package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountBannedActivity : Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun formatDate(value: String): String {
        if (value.isBlank() || value == "null") return "غير متوفر"
        return try {
            val instant = java.time.Instant.parse(value)
            SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date.from(instant))
        } catch (_: Exception) { value }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .remove("token").remove("user_id").remove("username").remove("phone").remove("email")
            .remove("active_room_id").apply()

        val reason = intent.getStringExtra("reason").orEmpty().ifBlank { "مخالفة معايير المجتمع وشروط الاستخدام" }
        val type = intent.getStringExtra("ban_type").orEmpty()
        val startedRaw = intent.getStringExtra("started_at").orEmpty()
        val expiresRaw = intent.getStringExtra("expires_at").orEmpty()
        val permanent = type.equals("permanent", true) || expiresRaw.isBlank() || expiresRaw == "null"
        val started = formatDate(startedRaw)
        val expires = if (permanent) "حظر دائم — لا يوجد تاريخ انتهاء" else formatDate(expiresRaw)
        val duration = if (permanent) "حظر دائم" else {
            try {
                val start = java.time.Instant.parse(startedRaw)
                val end = java.time.Instant.parse(expiresRaw)
                val hours = java.time.Duration.between(start, end).toHours().coerceAtLeast(1)
                when {
                    hours < 24 -> "$hours ساعة"
                    hours % 24 == 0L -> "${hours / 24} يوم"
                    else -> "$hours ساعة"
                }
            } catch (_: Exception) { "حظر مؤقت" }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(Color.rgb(246, 247, 251))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(26))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(230, 230, 238))
            }
            elevation = dp(4).toFloat()
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(7), 0, dp(7))
            }
        fun detail(label: String, value: String) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(9), dp(14), dp(9))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(247, 248, 252))
                    cornerRadius = dp(13).toFloat()
                }
            }
            box.addView(text(label, 12f, Color.rgb(119, 126, 143), true))
            box.addView(text(value, 15f, Color.rgb(35, 39, 53), true))
            card.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }

        card.addView(text("BAK LIVE", 15f, Color.rgb(88, 67, 164), true))
        card.addView(text("تم تقييد حسابك", 25f, Color.rgb(184, 48, 59), true))
        card.addView(text(
            "نود إشعارك بأنه تم اتخاذ إجراء بحق حسابك نتيجة مخالفة معايير المجتمع وشروط الاستخدام في BAK LIVE.",
            15f, Color.rgb(75, 80, 94)
        ))
        detail("سبب المخالفة", reason)
        detail("نوع الإجراء", if (permanent) "حظر دائم" else "حظر مؤقت")
        detail("مدة الحظر", duration)
        detail("تاريخ بدء الحظر", started)
        detail("تاريخ انتهاء الحظر", expires)

        card.addView(text(
            "خلال فترة الحظر، لن تتمكن من استخدام حسابك أو الميزات المشمولة بهذا الإجراء. نرجو الالتزام بقوانين التطبيق واحترام جميع المستخدمين لتجنب تكرار المخالفات واتخاذ إجراءات إضافية.",
            14f, Color.rgb(82, 87, 101)
        ).apply { setPadding(0, dp(16), 0, dp(8)) })
        card.addView(text(
            "إذا كنت تعتقد أن هذا القرار صدر عن طريق الخطأ، يمكنك التواصل مع فريق دعم BAK LIVE لطلب مراجعة القرار.",
            14f, Color.rgb(82, 87, 101)
        ))
        card.addView(text("BAK LIVE — مجتمعنا يجمعنا بالاحترام", 12f, Color.rgb(110, 101, 151), true)
            .apply { setPadding(0, dp(18), 0, 0) })

        scroll.addView(card, ScrollView.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val back = TextView(this).apply {
            text = "العودة إلى تسجيل الدخول"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(87, 64, 180), Color.rgb(27, 155, 139))
            ).apply { cornerRadius = dp(15).toFloat() }
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setOnClickListener { finish() }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        setContentView(root)
    }

    override fun onBackPressed() {
        finish()
    }
}
