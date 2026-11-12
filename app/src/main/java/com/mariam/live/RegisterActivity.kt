package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class RegisterActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var otpRequested = false

    private fun completeLogin(body: JSONObject) {
        val token = body.optString("token")
        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
        val user = body.optJSONObject("user") ?: JSONObject()
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("user_id", user.optString("id"))
            .putString("username", user.optString("username"))
            .putString("phone", user.optString("phone")).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = AuthUi.page(this)
        page.addView(AuthUi.brand(this, "انضم إلى مجتمع BAK LIVE"))
        val card = AuthUi.card(this)
        card.addView(AuthUi.heading(this, "أنشئ حسابك"))
        card.addView(AuthUi.hint(this, "أنشئ حسابك برقم هاتفك وتحقق منه عبر واتساب"))

        card.addView(AuthUi.label(this, "رقم الهاتف"))
        val phone = AuthUi.field(this, "+964 7XX XXX XXXX", InputType.TYPE_CLASS_PHONE).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        card.addView(phone)

        card.addView(AuthUi.label(this, "اسم المستخدم"))
        val username = AuthUi.field(this, "مثال: bak_123", InputType.TYPE_CLASS_TEXT)
        card.addView(username)

        card.addView(AuthUi.label(this, "الاسم الظاهر"))
        val displayName = AuthUi.field(this, "الاسم الذي سيظهر للآخرين", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        card.addView(displayName)

        card.addView(AuthUi.label(this, "كلمة المرور"))
        val password = AuthUi.field(this, "8 أحرف على الأقل", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(password)

        card.addView(AuthUi.label(this, "تأكيد كلمة المرور"))
        val confirm = AuthUi.field(this, "أعد كتابة كلمة المرور", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(confirm)

        val otpLabel = AuthUi.label(this, "رمز التحقق المرسل عبر واتساب").apply { visibility = View.GONE }
        card.addView(otpLabel)
        val otp = AuthUi.field(this, "أدخل رمز التحقق المكوّن من 6 أرقام", InputType.TYPE_CLASS_NUMBER).apply {
            visibility = View.GONE
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
        }
        card.addView(otp)

        val show = CheckBox(this).apply {
            text = "إظهار كلمات المرور"
            textSize = 12f
            setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val type = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                val p = password.selectionStart
                val c = confirm.selectionStart
                password.inputType = type
                confirm.inputType = type
                password.setSelection(p.coerceAtLeast(0))
                confirm.setSelection(c.coerceAtLeast(0))
            }
        }
        card.addView(show)

        val message = AuthUi.message(this)
        card.addView(message)
        val submit = AuthUi.primaryButton(this, "إرسال كود واتساب")
        card.addView(submit)

        submit.setOnClickListener {
            val number = phone.text.toString().trim().replace(" ", "").replace("-", "")
            val name = username.text.toString().trim().lowercase()
            val display = displayName.text.toString().trim()
            val pass = password.text.toString()
            val repeated = confirm.text.toString()

            if (!otpRequested) {
                val validation = when {
                    !Regex("^\\+?[1-9][0-9]{7,14}$").matches(number) -> "أدخل رقم هاتف دولي صحيحاً، مثال +9647XXXXXXXXX"
                    !Regex("^[a-z0-9_]{3,30}$").matches(name) -> "اسم المستخدم: 3–30 حرفاً إنجليزياً أو رقماً أو _"
                    display.isBlank() || display.length > 60 -> "أدخل اسماً ظاهراً لا يتجاوز 60 حرفاً"
                    pass.length < 8 || pass.toByteArray(StandardCharsets.UTF_8).size > 72 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
                    pass != repeated -> "كلمتا المرور غير متطابقتين"
                    else -> null
                }
                if (validation != null) { AuthUi.showMessage(message, validation); return@setOnClickListener }
            } else if (!Regex("^[0-9]{6}$").matches(otp.text.toString().trim())) {
                AuthUi.showMessage(message, "أدخل رمز التحقق المكوّن من 6 أرقام")
                return@setOnClickListener
            }

            submit.isEnabled = false
            submit.text = if (otpRequested) "جارٍ التحقق وإنشاء الحساب..." else "جارٍ إرسال الرمز..."
            message.visibility = View.GONE
            executor.execute {
                try {
                    val result = if (!otpRequested) {
                        AuthApi.post("/api/auth/phone/register/request-otp", JSONObject().apply { put("phone", number) })
                    } else {
                        AuthApi.post("/api/auth/phone/register/verify-otp", JSONObject().apply {
                            put("phone", number)
                            put("otp", otp.text.toString().trim())
                            put("username", name)
                            put("displayName", display)
                            put("password", pass)
                        })
                    }
                    if (result.status in 200..299) {
                        if (!otpRequested) {
                            otpRequested = true
                            runOnUiThread {
                                otpLabel.visibility = View.VISIBLE
                                otp.visibility = View.VISIBLE
                                phone.isEnabled = false
                                username.isEnabled = false
                                displayName.isEnabled = false
                                password.isEnabled = false
                                confirm.isEnabled = false
                                show.isEnabled = false
                                submit.isEnabled = true
                                submit.text = "تأكيد الرمز وإنشاء الحساب"
                                AuthUi.showMessage(message, "تم إرسال الرمز. افحص واتساب وأدخل الكود هنا.", true)
                            }
                        } else {
                            completeLogin(result.body)
                            runOnUiThread {
                                Toast.makeText(this, "تم إنشاء حسابك بنجاح 🎉", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, HomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                                finish()
                            }
                        }
                    } else {
                        val friendly = when (result.status) {
                            409 -> "رقم الهاتف أو اسم المستخدم مستخدم مسبقاً"
                            429 -> "طلبات كثيرة. انتظر قليلاً ثم حاول مجدداً"
                            400, 401 -> result.body.optString("error", "البيانات أو رمز التحقق غير صحيح")
                            503 -> "خدمة إرسال واتساب غير مفعّلة على الخادم حالياً"
                            else -> result.body.optString("error", "تعذر إكمال العملية")
                        }
                        runOnUiThread {
                            AuthUi.showMessage(message, friendly)
                            submit.isEnabled = true
                            submit.text = if (otpRequested) "تأكيد الرمز وإنشاء الحساب" else "إرسال كود واتساب"
                        }
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. حاول مرة أخرى.")
                        submit.isEnabled = true
                        submit.text = if (otpRequested) "تأكيد الرمز وإنشاء الحساب" else "إرسال كود واتساب"
                    }
                }
            }
        }

        val back = AuthUi.link(this, "لديك حساب بالفعل؟ سجّل الدخول")
        back.setOnClickListener { finish() }
        page.addView(card)
        page.addView(back)
        AuthUi.background(this, AuthUi.scroll(this, page))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
