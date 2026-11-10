package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class RegisterActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = AuthUi.page(this)
        page.addView(AuthUi.brand(this, "انضم إلى مجتمع Mariam Live"))
        val card = AuthUi.card(this)
        card.addView(AuthUi.heading(this, "أنشئ حسابك ✨"))
        card.addView(AuthUi.hint(this, "خطوات بسيطة وتكون جزءاً من مجتمعنا"))
        card.addView(AuthUi.label(this, "اسم المستخدم"))
        val username = AuthUi.field(this, "مثال: mariam_123", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        card.addView(username)
        card.addView(AuthUi.hint(this, "3 إلى 30 حرفاً: أحرف إنجليزية صغيرة وأرقام وشرطة سفلية فقط"))
        card.addView(AuthUi.label(this, "الاسم الظاهر"))
        val displayName = AuthUi.field(this, "الاسم الذي سيظهر للآخرين", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        card.addView(displayName)
        card.addView(AuthUi.label(this, "البريد الإلكتروني"))
        val email = AuthUi.field(this, "name@example.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        card.addView(email)
        card.addView(AuthUi.label(this, "كلمة المرور"))
        val password = AuthUi.field(this, "8 أحرف على الأقل", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(password)
        card.addView(AuthUi.label(this, "تأكيد كلمة المرور"))
        val confirm = AuthUi.field(this, "أعد كتابة كلمة المرور", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(confirm)
        val show = CheckBox(this).apply {
            text = "إظهار كلمات المرور"; textSize = 12f; setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val type = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                val p = password.selectionStart; val c = confirm.selectionStart
                password.inputType = type; confirm.inputType = type
                password.setSelection(p.coerceAtLeast(0)); confirm.setSelection(c.coerceAtLeast(0))
            }
        }
        card.addView(show)
        val message = AuthUi.message(this); card.addView(message)
        val submit = AuthUi.primaryButton(this, "إنشاء حساب جديد"); card.addView(submit)
        submit.setOnClickListener {
            val name = username.text.toString().trim().lowercase()
            val display = displayName.text.toString().trim()
            val mail = email.text.toString().trim().lowercase()
            val pass = password.text.toString()
            val repeated = confirm.text.toString()
            val validation = when {
                !Regex("^[a-z0-9_]{3,30}$").matches(name) -> "اسم المستخدم يجب أن يكون 3–30 حرفاً إنجليزياً صغيراً أو رقماً أو _"
                display.isBlank() || display.length > 60 -> "أدخل اسماً ظاهراً لا يتجاوز 60 حرفاً"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> "أدخل بريداً إلكترونياً صحيحاً"
                pass.length < 8 || pass.toByteArray(StandardCharsets.UTF_8).size > 72 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل وبحد أقصى 72 بايت"
                pass != repeated -> "كلمتا المرور غير متطابقتين"
                else -> null
            }
            if (validation != null) { AuthUi.showMessage(message, validation); return@setOnClickListener }
            submit.isEnabled = false; submit.text = "جارٍ إنشاء حسابك..."; message.visibility = View.GONE
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/register", JSONObject().apply {
                        put("username", name); put("displayName", display); put("email", mail); put("password", pass)
                    })
                    if (result.status in 200..299) {
                        val token = result.body.optString("token")
                        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
                        val data = result.body.optJSONObject("user") ?: JSONObject()
                        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
                            .putString("token", token).putString("user_id", data.optString("id"))
                            .putString("username", data.optString("username")).putString("email", data.optString("email")).apply()
                        runOnUiThread {
                            Toast.makeText(this, "أهلاً بك في Mariam Live 🎉", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                            finish()
                        }
                    } else {
                        val raw = result.body.optString("error", "تحقق من البيانات المدخلة")
                        val friendly = when (result.status) {
                            409 -> "اسم المستخدم أو البريد الإلكتروني مستخدم مسبقاً"
                            400 -> when (raw) {
                                "Username must be 3-30 characters" -> "اسم المستخدم غير صالح"
                                "Invalid email address" -> "البريد الإلكتروني غير صحيح"
                                "Password must be 8-72 bytes" -> "كلمة المرور يجب أن تكون بين 8 و72 بايت"
                                else -> raw
                            }
                            else -> raw
                        }
                        runOnUiThread { AuthUi.showMessage(message, friendly); submit.isEnabled = true; submit.text = "إنشاء حساب جديد" }
                    }
                } catch (_: Exception) {
                    runOnUiThread { AuthUi.showMessage(message, "تعذر الاتصال بالخادم. حاول مرة أخرى."); submit.isEnabled = true; submit.text = "إنشاء حساب جديد" }
                }
            }
        }
        val back = AuthUi.link(this, "لديك حساب بالفعل؟ سجّل الدخول")
        back.setOnClickListener { finish() }
        page.addView(card); page.addView(back)
        AuthUi.background(this, AuthUi.scroll(this, page))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
