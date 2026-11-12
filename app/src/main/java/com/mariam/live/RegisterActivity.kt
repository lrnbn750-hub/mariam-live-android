package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject

class RegisterActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val phoneUtil by lazy { PhoneNumberUtil.getInstance() }
    private data class Country(val region: String, val name: String, val dial: String)
    private lateinit var card: LinearLayout
    private lateinit var message: TextView
    private lateinit var phone: EditText
    private lateinit var countryButton: Button
    private lateinit var submit: Button
    private var selectedCountry = Country("IQ", "العراق", "+964")
    private var normalizedPhone = ""
    private var otpRequested = false
    private var otp: EditText? = null
    private var displayName: EditText? = null
    private var password: EditText? = null

    private fun countries(): List<Country> = phoneUtil.supportedRegions
        .mapNotNull { region ->
            val code = phoneUtil.getCountryCodeForRegion(region)
            if (code <= 0) null else {
                val locale = Locale("ar", region)
                val name = locale.getDisplayCountry(Locale("ar"))
                    .ifBlank { Locale("", region).displayCountry }
                Country(region, name, "+$code")
            }
        }
        .distinctBy { it.region }
        .sortedBy { it.name }

    private fun chooseCountry() {
        val items = countries()
        val labels = items.map { "${it.name}  ${it.dial}" }.toTypedArray()
        val search = EditText(this).apply {
            hint = "ابحث عن الدولة أو رمز الاتصال"
            textSize = 15f
            setSingleLine(true)
            setPadding(28, 18, 28, 18)
        }
        val list = ListView(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels.toMutableList())
        list.adapter = adapter
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 12, 20, 8)
            addView(search)
            addView(list, LinearLayout.LayoutParams(-1, dp(360)))
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("اختر رمز الدولة")
            .setView(dialogView)
            .setNegativeButton("إلغاء", null)
            .create()
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.clear()
                adapter.addAll(items.filter { it.name.contains(s.toString(), true) || it.dial.contains(s.toString()) }
                    .map { "${it.name}  ${it.dial}" })
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        list.setOnItemClickListener { _, _, position, _ ->
            val chosenLabel = adapter.getItem(position)
            selectedCountry = items.first { "${it.name}  ${it.dial}" == chosenLabel }
            countryButton.text = "${selectedCountry.name}  ${selectedCountry.dial}  ▾"
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dp(v: Int) = AuthUi.dp(this, v)

    private fun completeLogin(body: JSONObject) {
        val token = body.optString("token")
        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
        val user = body.optJSONObject("user") ?: JSONObject()
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("user_id", user.optString("id"))
            .putString("username", user.optString("username"))
            .putString("phone", user.optString("phone"))
            .putString("email", user.optString("email")).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = AuthUi.page(this)
        page.addView(AuthUi.brand(this, "انضم إلى مجتمع BAK LIVE"))
        card = AuthUi.card(this)
        page.addView(card)
        message = AuthUi.message(this)
        val registrationMode = intent.getStringExtra("registration_mode") ?: "phone"
        if (registrationMode == "email") renderEmailStep() else renderPhoneStep()
        page.addView(message)
        AuthUi.background(this, AuthUi.scroll(this, page))
    }


    private fun renderEmailStep() {
        card.removeAllViews()
        card.addView(AuthUi.heading(this, "إنشاء حساب بالبريد الإلكتروني"))
        card.addView(AuthUi.hint(this, "أدخل بريدك واسمك الظاهر وكلمة المرور لإكمال التسجيل"))
        card.addView(AuthUi.label(this, "البريد الإلكتروني"))
        val email = AuthUi.field(this, "example@email.com",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        card.addView(email)
        card.addView(AuthUi.label(this, "الاسم الظاهر"))
        val display = AuthUi.field(this, "الاسم الذي سيظهر للآخرين",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        card.addView(display)
        card.addView(AuthUi.label(this, "كلمة المرور"))
        val pass = AuthUi.field(this, "8 أحرف على الأقل",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
            textDirection = View.TEXT_DIRECTION_LTR
        }
        card.addView(pass)
        val show = CheckBox(this).apply {
            text = "إظهار كلمة المرور"
            textSize = 12f
            setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val pos = pass.selectionStart
                pass.inputType = InputType.TYPE_CLASS_TEXT or if (checked)
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_TEXT_VARIATION_PASSWORD
                pass.setSelection(pos.coerceAtLeast(0))
            }
        }
        card.addView(show)
        submit = AuthUi.primaryButton(this, "إنشاء حساب")
        card.addView(submit)
        submit.setOnClickListener {
            val mail = email.text.toString().trim().lowercase(Locale.ROOT)
            val name = display.text.toString().trim()
            val secret = pass.text.toString()
            val error = when {
                !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(mail) -> "أدخل بريداً إلكترونياً صحيحاً"
                name.isBlank() || name.length > 60 -> "أدخل اسماً ظاهراً لا يتجاوز 60 حرفاً"
                secret.length < 8 || secret.toByteArray(Charsets.UTF_8).size > 72 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
                else -> null
            }
            if (error != null) {
                AuthUi.showMessage(message, error)
                return@setOnClickListener
            }
            val internalUsername = "bak_" + System.currentTimeMillis().toString().takeLast(10)
            submit.isEnabled = false
            submit.text = "جارٍ إنشاء الحساب..."
            message.visibility = View.GONE
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/register", JSONObject().apply {
                        put("username", internalUsername)
                        put("email", mail)
                        put("displayName", name)
                        put("password", secret)
                    })
                    if (result.status in 200..299) {
                        completeLogin(result.body)
                        runOnUiThread {
                            Toast.makeText(this, "تم إنشاء حسابك بنجاح 🎉", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                    } else runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "إنشاء حساب"
                        val errorText = when (result.status) {
                            409 -> "هذا البريد مسجل مسبقاً"
                            400 -> "تحقق من البريد وكلمة المرور والاسم"
                            else -> result.body.optString("error", "تعذر إنشاء الحساب")
                        }
                        AuthUi.showMessage(message, errorText)
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "إنشاء حساب"
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. حاول مرة أخرى")
                    }
                }
            }
        }
    }

    private fun renderPhoneStep() {
        otpRequested = false
        card.removeAllViews()
        card.addView(AuthUi.heading(this, "إنشاء حساب جديد"))
        card.addView(AuthUi.hint(this, "أدخل رقم هاتفك لنرسل لك رمز تحقق"))
        card.addView(AuthUi.label(this, "الدولة ورمز الاتصال"))
        countryButton = AuthUi.primaryButton(this, "${selectedCountry.name}  ${selectedCountry.dial}  ▾").apply {
            textSize = 14f
            setTextColor(0xFF29243D.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFF3F1F8.toInt())
                cornerRadius = dp(13).toFloat()
            }
            setOnClickListener { chooseCountry() }
        }
        card.addView(countryButton)
        card.addView(AuthUi.label(this, "رقم الهاتف"))
        phone = AuthUi.field(this, "اكتب الرقم بدون رمز الدولة", InputType.TYPE_CLASS_PHONE).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            inputType = InputType.TYPE_CLASS_PHONE
        }
        card.addView(phone)
        submit = AuthUi.primaryButton(this, "جلب رمز التحقق")
        card.addView(submit)
        submit.setOnClickListener {
            val raw = phone.text.toString().trim().replace(Regex("[^0-9+]"), "")
            val parsed = try { phoneUtil.parse(raw, selectedCountry.region) } catch (_: Exception) { null }
            if (parsed == null || !phoneUtil.isValidNumberForRegion(parsed, selectedCountry.region)) {
                AuthUi.showMessage(message, "رقم الهاتف غير صحيح. اختر الدولة واكتب الرقم بشكل صحيح")
                return@setOnClickListener
            }
            normalizedPhone = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            submit.isEnabled = false
            submit.text = "جارٍ إرسال رمز التحقق..."
            message.visibility = View.GONE
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/phone/register/request-otp",
                        JSONObject().put("phone", normalizedPhone))
                    runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "جلب رمز التحقق"
                        if (result.status in 200..299) renderOtpStep()
                        else AuthUi.showMessage(message, result.body.optString("error", "تعذر إرسال الرمز. حاول مجدداً"))
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "جلب رمز التحقق"
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. حاول مرة أخرى")
                    }
                }
            }
        }
    }

    private fun renderOtpStep() {
        otpRequested = true
        card.removeAllViews()
        card.addView(AuthUi.heading(this, "تأكيد رقم الهاتف"))
        card.addView(AuthUi.hint(this, "أدخل رمز التحقق المرسل إلى $normalizedPhone، ثم أكمل بيانات حسابك"))
        card.addView(AuthUi.label(this, "رمز التحقق"))
        otp = AuthUi.field(this, "رمز التحقق المكوّن من 6 أرقام", InputType.TYPE_CLASS_NUMBER).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        }
        card.addView(otp)
        card.addView(AuthUi.label(this, "الاسم الظاهر"))
        displayName = AuthUi.field(this, "الاسم الذي سيظهر للآخرين",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        card.addView(displayName)
        card.addView(AuthUi.label(this, "كلمة المرور"))
        password = AuthUi.field(this, "8 أحرف على الأقل", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(password)
        val show = CheckBox(this).apply {
            text = "إظهار كلمة المرور"
            textSize = 12f
            setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val type = InputType.TYPE_CLASS_TEXT or if (checked) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
                val pos = password?.selectionStart ?: 0
                password?.inputType = type
                password?.setSelection(pos.coerceAtLeast(0))
            }
        }
        card.addView(show)
        submit = AuthUi.primaryButton(this, "إنشاء حساب")
        card.addView(submit)
        val resend = AuthUi.link(this, "إعادة إرسال الرمز")
        card.addView(resend)
        resend.setOnClickListener {
            resend.isEnabled = false
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/phone/register/request-otp",
                        JSONObject().put("phone", normalizedPhone))
                    runOnUiThread {
                        resend.isEnabled = true
                        AuthUi.showMessage(message,
                            if (result.status in 200..299) "تم إرسال رمز جديد" else result.body.optString("error", "تعذر إعادة إرسال الرمز"),
                            result.status in 200..299)
                    }
                } catch (_: Exception) {
                    runOnUiThread { resend.isEnabled = true; AuthUi.showMessage(message, "تعذر الاتصال بالخادم") }
                }
            }
        }
        submit.setOnClickListener {
            val code = otp?.text?.toString()?.trim().orEmpty()
            val display = displayName?.text?.toString()?.trim().orEmpty()
            val pass = password?.text?.toString().orEmpty()
            val validation = when {
                !Regex("^[0-9]{6}$").matches(code) -> "أدخل رمز التحقق المكوّن من 6 أرقام"
                display.isBlank() || display.length > 60 -> "أدخل اسماً ظاهراً لا يتجاوز 60 حرفاً"
                pass.length < 8 || pass.toByteArray(Charsets.UTF_8).size > 72 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
                else -> null
            }
            if (validation != null) { AuthUi.showMessage(message, validation); return@setOnClickListener }
            // The backend currently requires a username; create a unique internal one without asking the user for it.
            val internalUsername = "bak_" + System.currentTimeMillis().toString().takeLast(10)
            submit.isEnabled = false
            submit.text = "جارٍ التحقق وإنشاء الحساب..."
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/phone/register/verify-otp", JSONObject().apply {
                        put("phone", normalizedPhone)
                        put("otp", code)
                        put("username", internalUsername)
                        put("displayName", display)
                        put("password", pass)
                    })
                    if (result.status in 200..299) {
                        completeLogin(result.body)
                        runOnUiThread {
                            Toast.makeText(this, "تم إنشاء حسابك بنجاح 🎉", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                    } else runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "إنشاء حساب"
                        val error = when (result.status) {
                            409 -> "هذا الرقم مسجل مسبقاً"
                            429 -> "طلبات كثيرة. انتظر قليلاً ثم حاول"
                            400, 401 -> result.body.optString("error", "رمز التحقق غير صحيح أو انتهت صلاحيته")
                            503 -> "خدمة إرسال واتساب غير مفعّلة حالياً"
                            else -> result.body.optString("error", "تعذر إنشاء الحساب")
                        }
                        AuthUi.showMessage(message, error)
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        submit.isEnabled = true
                        submit.text = "إنشاء حساب"
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. حاول مرة أخرى")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
