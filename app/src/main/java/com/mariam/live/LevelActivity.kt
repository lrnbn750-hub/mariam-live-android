package com.mariam.live

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Redesigned wealth-level screen.
 * The existing /api/levels/me contract and XP calculations are intentionally preserved.
 * All level values come from the authenticated API; this activity does not create or modify XP.
 */
class LevelActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var level = 1
    private var totalXp = 0L
    private var currentXp = 0L
    private var nextXp = 100L
    private var progressValue = 0.0
    private lateinit var wealthContent: WealthLevelContentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 15, 30)
        window.navigationBarColor = android.graphics.Color.rgb(7, 15, 30)
        try {
            setContentView(R.layout.wealth_level_screen)
        } catch (error: Throwable) {
            // Keep the app alive if Android cannot inflate the wealth screen or a custom view.
            Log.e("BAKWealthLevel", "Failed to inflate wealth_level_screen", error)
            val fallback = android.widget.TextView(this).apply {
                text = "مستوى الثراء\\n\\nتعذر تحميل التصميم.\\n\\nالخطأ: " + error.javaClass.name + "\\n" + (error.message ?: "لا توجد تفاصيل") + "\\n\\nأرسل صورة هذه الرسالة للمطور."
                textSize = 20f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.rgb(7, 15, 30))
                setPadding(32, 32, 32, 32)
                setOnClickListener { finish() }
            }
            setContentView(fallback)
            return
        }

        val toolbar = findViewById<View>(R.id.wealthToolbar)
        toolbar.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }
        toolbar.requestApplyInsets()
        findViewById<View>(R.id.wealthBack).setOnClickListener { finish() }

        wealthContent = findViewById(R.id.wealthContent)
        wealthContent.onTierClick = { tierIndex ->
            val tier = LevelTiers.ALL.getOrNull(tierIndex)
            if (tier != null) {
                val first = LevelTiers.firstLevel(tierIndex)
                val last = LevelTiers.lastLevel(tierIndex)
                val status = when {
                    level in first..last -> "رتبتك الحالية"
                    level > last -> "مرحلة مكتملة"
                    else -> "مرحلة مقفلة"
                }
                Toast.makeText(this, "${tier.displayName(this)} • LV $first–$last • $status", Toast.LENGTH_SHORT).show()
            }
        }

        renderLevel()
        loadLevel()
    }

    private fun renderLevel() {
        if (!::wealthContent.isInitialized) return
        val remaining = if (level >= 100) null else (nextXp - currentXp).coerceAtLeast(0L)
        wealthContent.bind(
            level = level,
            totalXp = totalXp,
            progress = if (level >= 100) 1f else progressValue.toFloat().coerceIn(0f, 1f),
            remainingXp = remaining,
            animate = true
        )
    }

    /** Existing API integration retained unchanged: GET /api/levels/me. */
    private fun loadLevel() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        executor.execute {
            try {
                val result = AuthApi.get("/api/levels/me", token)
                if (result.status !in 200..299) throw Exception("تعذر تحميل بيانات المستوى")
                val data: JSONObject = result.body
                level = data.optInt("level", 1).coerceIn(1, 100)
                totalXp = data.optLong("total_xp", 0).coerceAtLeast(0)
                currentXp = data.optLong("current_level_xp", 0).coerceAtLeast(0)
                nextXp = data.optLong("next_level_xp", 100).coerceAtLeast(1)
                progressValue = data.optDouble("progress", 0.0).coerceIn(0.0, 1.0)
                runOnUiThread { if (!isFinishing && !isDestroyed) renderLevel() }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, e.message ?: "تعذر الاتصال بالخادم", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}