package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.graphics.drawable.GradientDrawable
import org.json.JSONObject
import java.util.concurrent.Executors

class LoginActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    private fun rounded(color: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = AuthUi.dp(this@LoginActivity, 15).toFloat()
            if (stroke != 0) setStroke(AuthUi.dp(this@LoginActivity, 1), stroke)
        }

    private fun saveSession(result: JSONObject) {
        val token = result.optString("token")
        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
        val user = result.optJSONObject("user") ?: JSONObject()
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("user_id", user.optString("id"))
            .putString("username", user.optString("username"))
            .putString("phone", user.optString("phone"))
            .putString("email", user.optString("email")).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = AuthUi.page(this).apply { background = ColorDrawable(Color.TRANSPARENT) }
        page.addView(AuthUi.brand(this, "صوتك وأصدقاؤك وعالمك يبدأ من هنا"))

        val card = AuthUi.card(this).apply {
            background = GradientDrawable().apply {
                setColor(0xCC17152B.toInt())
                cornerRadius = AuthUi.dp(this@LoginActivity, 26).toFloat()
                setStroke(AuthUi.dp(this@LoginActivity, 1), 0x44FFFFFF)
            }
        }
        card.addView(AuthUi.heading(this, "أهلاً بعودتك").apply { setTextColor(Color.WHITE) })
        card.addView(AuthUi.hint(this, "سجّل دخولك إلى مجتمع BAK LIVE").apply { setTextColor(0xFFD8D3E8.toInt()) })

        val phoneTab = TextView(this).apply {
            text = "رقم الهاتف"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF7957F6.toInt())
            background = rounded(0xFFF1ECFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, AuthUi.dp(this@LoginActivity, 46), 1f)
        }
        val emailTab = TextView(this).apply {
            text = "البريد الإلكتروني"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF77748B.toInt())
            background = rounded(0xFFF3F1F8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, AuthUi.dp(this@LoginActivity, 46), 1f).apply {
                marginStart = AuthUi.dp(this@LoginActivity, 8)
            }
        }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(phoneTab); tabs.addView(emailTab); card.addView(tabs)

        card.addView(AuthUi.label(this, "رقم الهاتف").apply { setTextColor(Color.WHITE) })
        val phone = AuthUi.field(this, "+964 7XX XXX XXXX", InputType.TYPE_CLASS_PHONE)
        phone.textDirection = View.TEXT_DIRECTION_LTR
        phone.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        card.addView(phone)

        card.addView(AuthUi.label(this, "كلمة المرور").apply { setTextColor(Color.WHITE) })
        val password = AuthUi.field(this, "أدخل كلمة المرور", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        password.textDirection = View.TEXT_DIRECTION_LTR
        card.addView(password)

        val show = CheckBox(this).apply {
            text = "إظهار كلمة المرور"
            textSize = 12f
            setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val pos = password.selectionStart
                password.inputType = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.setSelection(pos.coerceAtLeast(0))
            }
        }
        card.addView(show)

        val emailPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        emailPanel.addView(AuthUi.label(this, "البريد الإلكتروني").apply { setTextColor(Color.WHITE) })
        val identifier = AuthUi.field(this, "البريد الإلكتروني أو اسم المستخدم", InputType.TYPE_CLASS_TEXT)
        emailPanel.addView(identifier)
        card.addView(emailPanel)

        val message = AuthUi.message(this)
        card.addView(message)
        val submit = AuthUi.primaryButton(this, "تسجيل الدخول")
        card.addView(submit)

        var usePhone = true
        fun selectPhone(enabled: Boolean) {
            usePhone = enabled
            phone.visibility = if (enabled) View.VISIBLE else View.GONE
            password.visibility = View.VISIBLE
            emailPanel.visibility = if (enabled) View.GONE else View.VISIBLE
            phoneTab.background = rounded(if (enabled) 0xFFF1ECFF.toInt() else 0xFFF3F1F8.toInt())
            emailTab.background = rounded(if (!enabled) 0xFFF1ECFF.toInt() else 0xFFF3F1F8.toInt())
            phoneTab.setTextColor(if (enabled) 0xFF7957F6.toInt() else 0xFF77748B.toInt())
            emailTab.setTextColor(if (!enabled) 0xFF7957F6.toInt() else 0xFF77748B.toInt())
            message.visibility = View.GONE
        }
        phoneTab.setOnClickListener { selectPhone(true) }
        emailTab.setOnClickListener { selectPhone(false) }

        submit.setOnClickListener {
            val id = if (usePhone) phone.text.toString().trim().replace(" ", "").replace("-", "") else identifier.text.toString().trim()
            val pass = password.text.toString()
            if (id.isBlank() || pass.isBlank()) {
                AuthUi.showMessage(message, "أدخل رقم الهاتف وكلمة المرور")
                return@setOnClickListener
            }
            if (usePhone && !Regex("^\\+?[1-9][0-9]{7,14}$").matches(id)) {
                AuthUi.showMessage(message, "أدخل رقم هاتف دولي صحيحاً، مثال +9647XXXXXXXXX")
                return@setOnClickListener
            }
            submit.isEnabled = false
            submit.text = "جارٍ تسجيل الدخول..."
            message.visibility = View.GONE
            executor.execute {
                try {
                    val endpoint = if (usePhone) "/api/auth/phone/login" else "/api/auth/login"
                    val payload = JSONObject().apply {
                        if (usePhone) put("phone", id) else put("identifier", id)
                        put("password", pass)
                    }
                    val result = AuthApi.post(endpoint, payload)
                    if (result.status in 200..299) {
                        saveSession(result.body)
                        runOnUiThread {
                            startActivity(Intent(this, MalakWelcomeActivity::class.java))
                            finish()
                        }
                    } else {
                        val error = when (result.status) {
                            401 -> "رقم الهاتف أو كلمة المرور غير صحيحة"
                            403 -> "هذا الحساب غير نشط"
                            404 -> "الحساب غير موجود"
                            else -> result.body.optString("error", "تعذر تسجيل الدخول")
                        }
                        runOnUiThread { AuthUi.showMessage(message, error); submit.isEnabled = true; submit.text = "تسجيل الدخول" }
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. تأكد من اتصال الإنترنت.")
                        submit.isEnabled = true
                        submit.text = "تسجيل الدخول"
                    }
                }
            }
        }

        val create = AuthUi.link(this, "ليس لديك حساب؟ أنشئ حساباً جديداً")
        create.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        val forgot = AuthUi.link(this, "نسيت كلمة المرور؟")
        forgot.setOnClickListener { Toast.makeText(this, "استعادة كلمة المرور ستُضاف بعد ربط التحقق من الهاتف", Toast.LENGTH_LONG).show() }

        page.addView(card)
        page.addView(create.apply { setTextColor(Color.WHITE) })
        page.addView(forgot.apply { setTextColor(0xFFD8D3E8.toInt()) })
        val root = FrameLayout(this)
        val videoId = resources.getIdentifier("login_background", "raw", packageName)
        if (videoId != 0) {
            val video = VideoView(this)
            root.addView(video, FrameLayout.LayoutParams(-1, -1))
            video.setVideoURI(Uri.parse("android.resource://$packageName/$videoId"))
            video.setOnPreparedListener { player ->
                player.isLooping = true
                player.setVolume(0f, 0f)
                video.start()
            }
            video.setOnErrorListener { _, _, _ -> true }
        } else {
            val fallback = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                val imageId = resources.getIdentifier("malak_splash", "drawable", packageName)
                if (imageId != 0) setImageResource(imageId)
                else setBackgroundColor(0xFF17152B.toInt())
            }
            root.addView(fallback, FrameLayout.LayoutParams(-1, -1))
        }
        root.addView(View(this).apply { setBackgroundColor(0x990D0B1B.toInt()) },
            FrameLayout.LayoutParams(-1, -1))
        val content = AuthUi.scroll(this, page).apply { background = ColorDrawable(Color.TRANSPARENT) }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        window.statusBarColor = 0xFF17152B.toInt()
        window.navigationBarColor = 0xFF17152B.toInt()
        window.decorView.systemUiVisibility = 0
        setContentView(root)
        selectPhone(true)
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
