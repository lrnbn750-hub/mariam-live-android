package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class HomeActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val server = "http://185.174.252.118:3000"
    private val purple = Color.rgb(112, 78, 255)
    private val dark = Color.rgb(35, 35, 50)
    private val gray = Color.rgb(125, 127, 143)
    private lateinit var status: TextView
    private lateinit var account: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun panel() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(18).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 247, 251)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            setBackgroundColor(Color.rgb(247, 247, 251))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = label("Mariam Live", 25f, purple, true)
        header.addView(brand, LinearLayout.LayoutParams(0, dp(54), 1f))
        header.addView(label("الحساب", 14f, gray))
        root.addView(header)

        root.addView(label("مرحباً بك", 24f, dark, true).apply {
            setPadding(0, dp(28), 0, dp(5))
        })
        account = label("جارٍ التحقق من حسابك...", 14f, gray)
        root.addView(account)

        val roomsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel()
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        roomsCard.addView(label("الغرف الصوتية", 19f, dark, true))
        status = label("لم يتم تحميل الغرف بعد.", 14f, gray)
        status.setPadding(0, dp(12), 0, dp(12))
        roomsCard.addView(status)
        roomsCard.addView(label(
            "لن نعرض غرفاً أو مستخدمين تجريبيين. ستظهر الغرف هنا بعد ربط واجهة الغرف الحقيقية بالخادم.",
            13f, gray
        ))
        val cardParams = LinearLayout.LayoutParams(-1, -2)
        cardParams.topMargin = dp(22)
        root.addView(roomsCard, cardParams)

        val refresh = Button(this).apply {
            text = "إعادة التحقق من الحساب"
            setTextColor(Color.WHITE)
            setBackgroundColor(purple)
            setOnClickListener { verifyAccount() }
        }
        val refreshParams = LinearLayout.LayoutParams(-1, dp(52))
        refreshParams.topMargin = dp(18)
        root.addView(refresh, refreshParams)

        val logout = TextView(this).apply {
            text = "تسجيل الخروج"
            textSize = 14f
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
            setOnClickListener {
                getSharedPreferences("MariamLive", MODE_PRIVATE).edit().remove("token").apply()
                val i = Intent(this@HomeActivity, LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                finish()
            }
        }
        root.addView(logout)
        setContentView(root)
        verifyAccount()
    }

    private fun verifyAccount() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrBlank()) {
            account.text = "لا توجد جلسة دخول صالحة."
            return
        }
        account.text = "جارٍ الاتصال بالخادم..."
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$server/api/auth/me").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("Authorization", "Bearer $token")
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (code in 200..299) {
                    val json = JSONObject(body)
                    val user = json.optJSONObject("user") ?: json
                    val name = user.optString("displayName",
                        user.optString("username", "مستخدم Mariam Live"))
                    runOnUiThread { account.text = "تم التحقق من الحساب: $name" }
                } else {
                    runOnUiThread { account.text = "تعذر التحقق من الجلسة (HTTP $code). سجّل الدخول مجدداً إذا انتهت صلاحيتها." }
                }
            } catch (e: Exception) {
                runOnUiThread { account.text = "الخادم غير متاح حالياً: ${e.message ?: "خطأ اتصال"}" }
            } finally {
                connection?.disconnect()
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
