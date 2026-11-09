package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class LoginActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = AuthUi.page(this)
        page.addView(AuthUi.brand(this, "عالمك، أصدقاؤك، وصوتك في مكان واحد"))
        val card = AuthUi.card(this)
        card.addView(AuthUi.heading(this, "أهلاً بعودتك 👋"))
        card.addView(AuthUi.hint(this, "سجّل دخولك وارجع لأصدقائك وغرفك المفضلة"))
        card.addView(AuthUi.label(this, "اسم المستخدم أو البريد الإلكتروني"))
        val identifier = AuthUi.field(this, "أدخل اسم المستخدم أو البريد", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        card.addView(identifier)
        card.addView(AuthUi.label(this, "كلمة المرور"))
        val password = AuthUi.field(this, "أدخل كلمة المرور", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        card.addView(password)
        val show = CheckBox(this).apply {
            text = "إظهار كلمة المرور"; textSize = 12f; setTextColor(0xFF77748B.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7957F6.toInt())
            setOnCheckedChangeListener { _, checked ->
                val pos = password.selectionStart
                password.inputType = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.setSelection(pos.coerceAtLeast(0))
            }
        }
        card.addView(show)
        val message = AuthUi.message(this); card.addView(message)
        val login = AuthUi.primaryButton(this, "تسجيل الدخول"); card.addView(login)
        login.setOnClickListener {
            val user = identifier.text.toString().trim()
            val pass = password.text.toString()
            if (user.isBlank() || pass.isBlank()) {
                AuthUi.showMessage(message, "يرجى إدخال اسم المستخدم وكلمة المرور"); return@setOnClickListener
            }
            login.isEnabled = false; login.text = "جارٍ التحقق من بياناتك..."; message.visibility = View.GONE
            executor.execute {
                try {
                    val result = AuthApi.post("/api/auth/login", JSONObject().apply { put("identifier", user); put("password", pass) })
                    if (result.status in 200..299) {
                        val token = result.body.optString("token")
                        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
                        val data = result.body.optJSONObject("user") ?: JSONObject()
                        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
                            .putString("token", token).putString("user_id", data.optString("id"))
                            .putString("username", data.optString("username")).putString("email", data.optString("email")).apply()
                        runOnUiThread { startActivity(Intent(this, HomeActivity::class.java)); finish() }
                    } else {
                        val friendly = when (result.status) {
                            401 -> "اسم المستخدم أو كلمة المرور غير صحيحة"
                            403 -> "هذا الحساب غير نشط، تواصل مع الدعم"
                            else -> result.body.optString("error", "تعذر تسجيل الدخول")
                        }
                        runOnUiThread { AuthUi.showMessage(message, friendly); login.isEnabled = true; login.text = "تسجيل الدخول" }
                    }
                } catch (_: Exception) {
                    runOnUiThread { AuthUi.showMessage(message, "تعذر الاتصال بالخادم. تحقق من الإنترنت وحاول مجدداً."); login.isEnabled = true; login.text = "تسجيل الدخول" }
                }
            }
        }
        val register = AuthUi.link(this, "مستخدم جديد؟ أنشئ حسابك الآن")
        register.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        page.addView(card); page.addView(register)
        AuthUi.background(this, AuthUi.scroll(this, page))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
