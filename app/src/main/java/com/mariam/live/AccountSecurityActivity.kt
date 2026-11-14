package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class AccountSecurityActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val green = Color.rgb(35, 157, 91)
    private val ink = Color.rgb(35, 45, 39)
    private val muted = Color.rgb(116, 130, 120)
    private lateinit var phoneValue: TextView
    private lateinit var status: TextView
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            setTextColor(green)
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(44), dp(48)))
        header.addView(TextView(this).apply {
            text = "الأمان والخصوصية"
            textSize = 19f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(44), dp(48)))
        root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }
        scroll.addView(body)
        body.addView(TextView(this).apply {
            text = "إدارة حسابك"
            textSize = 21f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(this).apply {
            text = "حدّث معلومات الدخول وحافظ على أمان حساب BAK LIVE."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(18))
        })

        val phoneCard = card()
        phoneCard.addView(TextView(this).apply {
            text = "رقم الهاتف"
            textSize = 16f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        phoneValue = TextView(this).apply {
            text = "جارٍ تحميل الرقم..."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(12))
        }
        phoneCard.addView(phoneValue)
        phoneCard.addView(actionButton("ربط / تغيير رقم الهاتف") {
            showPhoneDialog()
        })
        body.addView(phoneCard)

        val passwordCard = card()
        passwordCard.addView(TextView(this).apply {
            text = "كلمة المرور"
            textSize = 16f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        passwordCard.addView(TextView(this).apply {
            text = "استخدم كلمة مرور قوية ولا تشاركها مع أي شخص."
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(7), 0, dp(12))
        })
        passwordCard.addView(actionButton("تغيير كلمة المرور") { showPasswordDialog() })
        body.addView(passwordCard)

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }
        body.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadSecurityInfo()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.rgb(231, 237, 232))
        }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(14)
        layoutParams = params
    }

    private fun actionButton(label: String, click: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 14f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(green)
            cornerRadius = dp(12).toFloat()
        }
        setOnClickListener { click() }
    }

    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    private fun loadSecurityInfo() {
        val auth = token() ?: run { phoneValue.text = "يرجى تسجيل الدخول مجدداً"; return }
        executor.execute {
            try {
                val result = AuthApi.get("/api/auth/me", auth)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر تحميل الحساب"))
                val user = result.body.optJSONObject("user") ?: JSONObject()
                val phone = user.optString("phone").takeUnless { it.isBlank() || it == "null" }
                runOnUiThread {
                    if (!isFinishing) phoneValue.text = phone ?: "لا يوجد رقم هاتف مرتبط"
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing) phoneValue.text = "تعذر تحميل رقم الهاتف" }
            }
        }
    }

    private fun showPhoneDialog() {
        val input = EditText(this).apply {
            hint = "رقم الهاتف مع رمز الدولة، مثال +964..."
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setTextColor(ink)
        }
        AlertDialog.Builder(this)
            .setTitle("ربط أو تغيير رقم الهاتف")
            .setMessage("سيتم إرسال رمز تحقق إلى الرقم عبر واتساب.")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("متابعة") { _, _ ->
                val phone = input.text.toString().trim()
                if (phone.isBlank()) {
                    Toast.makeText(this, "أدخل رقم الهاتف", Toast.LENGTH_SHORT).show()
                } else {
                    requestPhoneCode(phone)
                }
            }.show()
    }

    private fun requestPhoneCode(phone: String) {
        val auth = token() ?: return
        status.text = "جارٍ طلب رمز التحقق..."
        executor.execute {
            try {
                val result = AuthApi.post("/api/account/phone/request", JSONObject().put("phone", phone), auth)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إرسال الرمز"))
                runOnUiThread { if (!isFinishing) showPhoneCodeDialog(phone) }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing) status.text = e.message ?: "تعذر إرسال الرمز" }
            }
        }
    }

    private fun showPhoneCodeDialog(phone: String) {
        val input = EditText(this).apply {
            hint = "رمز التحقق المكوّن من 6 أرقام"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(ink)
        }
        AlertDialog.Builder(this)
            .setTitle("تأكيد رقم الهاتف")
            .setMessage("أدخل الرمز الذي وصلك عبر واتساب.")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد") { _, _ -> verifyPhone(phone, input.text.toString().trim()) }
            .show()
    }

    private fun verifyPhone(phone: String, code: String) {
        val auth = token() ?: return
        status.text = "جارٍ تأكيد الرقم..."
        executor.execute {
            try {
                val result = AuthApi.post(
                    "/api/account/phone/verify",
                    JSONObject().put("phone", phone).put("verificationCode", code),
                    auth
                )
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "فشل تأكيد الرقم"))
                runOnUiThread {
                    if (!isFinishing) {
                        status.text = "تم تحديث رقم الهاتف بنجاح"
                        loadSecurityInfo()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing) status.text = e.message ?: "فشل تأكيد الرقم" }
            }
        }
    }

    private fun showPasswordDialog() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun passwordField(hintText: String) = EditText(this).apply {
            hint = hintText
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ink)
        }
        val current = passwordField("كلمة المرور الحالية")
        val next = passwordField("كلمة المرور الجديدة (8 أحرف على الأقل)")
        val confirm = passwordField("تأكيد كلمة المرور الجديدة")
        panel.addView(current)
        panel.addView(next)
        panel.addView(confirm)
        AlertDialog.Builder(this)
            .setTitle("تغيير كلمة المرور")
            .setView(panel)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val oldPassword = current.text.toString()
                val newPassword = next.text.toString()
                if (newPassword.length < 8 || newPassword != confirm.text.toString()) {
                    Toast.makeText(this, "تأكد من كلمة المرور الجديدة وتطابق التأكيد", Toast.LENGTH_LONG).show()
                } else {
                    changePassword(oldPassword, newPassword)
                }
            }.show()
    }

    private fun changePassword(current: String, next: String) {
        val auth = token() ?: return
        status.text = "جارٍ تحديث كلمة المرور..."
        executor.execute {
            try {
                val result = AuthApi.post(
                    "/api/account/password/change",
                    JSONObject().put("currentPassword", current).put("newPassword", next),
                    auth
                )
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر تغيير كلمة المرور"))
                runOnUiThread {
                    if (!isFinishing) status.text = "تم تغيير كلمة المرور بنجاح"
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing) status.text = e.message ?: "تعذر تغيير كلمة المرور" }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
