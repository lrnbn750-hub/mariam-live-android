package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

class AccountBannedActivity : Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun formatDate(value: String): String {
        if (value.isBlank() || value == "null") return "غير متوفر"
        return try {
            val instant = java.time.Instant.parse(value)
            SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(java.util.Date.from(instant))
        } catch (_: Exception) { value }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .remove("token").remove("user_id").remove("username").remove("phone").remove("email")
            .remove("active_room_id").apply()

        val reason = intent.getStringExtra("reason").orEmpty().ifBlank { "لم يذكر سبب من الإدارة" }
        val type = intent.getStringExtra("ban_type").orEmpty()
        val started = formatDate(intent.getStringExtra("started_at").orEmpty())
        val expiresRaw = intent.getStringExtra("expires_at").orEmpty()
        val expires = if (type == "permanent" || expiresRaw.isBlank() || expiresRaw == "null") "حظر دائم" else formatDate(expiresRaw)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
            setBackgroundColor(Color.rgb(247, 247, 251))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(30), dp(24), dp(26))
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
                setPadding(0, dp(8), 0, dp(8))
            }

        card.addView(text("تم حظر حسابك", 26f, Color.rgb(185, 45, 55), true))
        card.addView(text("لا يمكنك استخدام BAK LIVE خلال فترة الحظر.", 15f, Color.DKGRAY))
        card.addView(text("سبب الحظر", 17f, Color.BLACK, true))
        card.addView(text(reason, 16f, Color.DKGRAY))
        card.addView(text("بداية الحظر: $started", 14f, Color.DKGRAY))
        card.addView(text("انتهاء الحظر: $expires", 14f, Color.DKGRAY))
        root.addView(card, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    override fun onBackPressed() {
        // The banned account remains signed out; returning to login is allowed.
        finish()
    }
}
