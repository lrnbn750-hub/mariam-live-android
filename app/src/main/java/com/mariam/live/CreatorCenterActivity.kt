package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import kotlin.concurrent.thread

class CreatorCenterActivity : Activity() {
    private var token: String = ""
    private lateinit var content: LinearLayout

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun money(cents: Long): String = String.format(java.util.Locale.US, "%.2f", cents / 100.0)

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        window.statusBarColor = Color.rgb(18, 13, 28)
        window.navigationBarColor = Color.rgb(18, 13, 28)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 13, 28))
        }
        val header = TextView(this).apply {
            text = "‹          مركز صُنّاع المحتوى"
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { finish() }
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(56)))

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadCreatorData()
    }

    private fun loadCreatorData() {
        content.removeAllViews()
        content.addView(label("جارٍ تحميل بيانات الأرباح...", 14f, Color.LTGRAY))
        thread {
            try {
                val response = AuthApi.get("/api/wallet", token)
                runOnUiThread {
                    content.removeAllViews()
                    if (response.status !in 200..299) {
                        content.addView(label(response.body.optString("error", "تعذر تحميل الأرباح"), 14f, Color.WHITE))
                    } else {
                        val wallet = response.body.optJSONObject("wallet") ?: response.body
                        render(wallet)
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    content.removeAllViews()
                    content.addView(label(error.message ?: "تعذر الاتصال", 14f, Color.WHITE))
                }
            }
        }
    }

    private fun render(wallet: JSONObject) {
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(91, 38, 128), Color.rgb(42, 25, 61))
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        hero.addView(label("الأرباح المتاحة للسحب", 14f, Color.LTGRAY, true))
        hero.addView(label("$" + money(wallet.optLong("withdrawable_usd_cents")), 36f, Color.rgb(104, 232, 177), true))
        hero.addView(label("إجمالي الأرباح: $" + money(wallet.optLong("lifetime_earned_usd_cents")), 14f, Color.WHITE))
        content.addView(hero)

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(
            label("💎  الألماس المكتسب\n" + wallet.optLong("earned_diamonds"), 16f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(90), 1f)
        )
        stats.addView(
            label("50%\nحصة المذيع من الهدايا", 16f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(90), 1f)
        )
        content.addView(stats)
        content.addView(label("تقسيم الأرباح: 50% للمذيع و50% للمنصة", 13f, Color.rgb(220, 190, 240), true))
        content.addView(label("طلبات السحب تُدار من مركز صُنّاع المحتوى بعد تفعيل خدمة السحب على الخادم.", 13f, Color.LTGRAY))

        val refresh = TextView(this).apply {
            text = "تحديث البيانات"
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.rgb(105, 46, 145))
            }
            setOnClickListener { loadCreatorData() }
        }
        content.addView(refresh, LinearLayout.LayoutParams(-1, dp(50)))
    }
}