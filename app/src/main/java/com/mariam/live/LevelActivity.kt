package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Wealth screen host.
 *
 * Important: the page body is the supplied BAK LIVE design component
 * (WealthLevelContentView). The outer screen is created programmatically to avoid
 * the old wealth_level_screen.xml InflateException. Existing API/XP contract is unchanged.
 */
class LevelActivity : Activity() {
    companion object { private const val TAG = "BAKWealthLevel" }

    private val executor = Executors.newSingleThreadExecutor()
    private var level = 1
    private var totalXp = 0L
    private var currentXp = 0L
    private var nextXp = 100L
    private var progressValue = 0.0
    private lateinit var wealthContent: WealthLevelContentView

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 15, 30)
        window.navigationBarColor = Color.rgb(7, 15, 30)
        try {
            buildDesignedScreen()
            renderLevel()
            loadLevel()
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to create supplied wealth design", error)
            Toast.makeText(this, "تعذر فتح تصميم مستوى الثراء", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun buildDesignedScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 15, 30))
        }

        val toolbar = FrameLayout(this).apply {
            setPadding(dp(8), 0, dp(8), dp(4))
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 36f
            includeFontPadding = false
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = null
            contentDescription = "رجوع"
            setOnClickListener { finish() }
        }
        val backParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.START or Gravity.CENTER_VERTICAL)
        toolbar.addView(back, backParams)

        val title = TextView(this).apply {
            text = "مستوى الثراء"
            textSize = 19f
            includeFontPadding = false
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        toolbar.addView(title, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            dp(48),
            Gravity.CENTER
        ))
        toolbar.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }
        page.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        wealthContent = WealthLevelContentView(this)
        wealthContent.layoutParams = ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
        )
        wealthContent.onTierClick = { tierIndex ->
            val first = LevelTiers.firstLevel(tierIndex)
            val last = LevelTiers.lastLevel(tierIndex)
            val tier = LevelTiers.ALL[tierIndex]
            val status = when {
                level > last -> "مكتملة"
                level >= first -> "رتبتك الحالية"
                else -> "مقفلة"
            }
            Toast.makeText(
                this,
                "${tier.displayName(this)} • LV $first–$last • $status",
                Toast.LENGTH_SHORT
            ).show()
        }
        scroll.addView(wealthContent)
        page.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(page)
        toolbar.requestApplyInsets()
    }

    private fun renderLevel() {
        if (!::wealthContent.isInitialized) return
        val progress = if (level >= 100) 1f else progressValue.toFloat().coerceIn(0f, 1f)
        val remainingXp = if (level >= 100) null else (nextXp - currentXp).coerceAtLeast(0L)
        wealthContent.bind(
            level = level,
            totalXp = totalXp,
            progress = progress,
            remainingXp = remainingXp,
            animate = true
        )
    }

    /** Keep the existing authenticated endpoint and XP response fields unchanged. */
    private fun loadLevel() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE)
            .getString("token", null) ?: return

        executor.execute {
            try {
                val result = AuthApi.get("/api/levels/me", token)
                if (result.status !in 200..299) {
                    throw IllegalStateException("Level API returned HTTP ${result.status}")
                }
                val data: JSONObject = result.body
                val newLevel = data.optInt("level", 1).coerceIn(1, 100)
                val newTotalXp = data.optLong("total_xp", 0).coerceAtLeast(0)
                val newCurrentXp = data.optLong("current_level_xp", 0).coerceAtLeast(0)
                val newNextXp = data.optLong("next_level_xp", 100).coerceAtLeast(1)
                val newProgress = data.optDouble("progress", 0.0).coerceIn(0.0, 1.0)

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        level = newLevel
                        totalXp = newTotalXp
                        currentXp = newCurrentXp
                        nextXp = newNextXp
                        progressValue = newProgress
                        renderLevel()
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to load /api/levels/me", error)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this,
                            "تعذر تحديث بيانات الثروة، تظهر آخر بيانات متاحة",
                            Toast.LENGTH_LONG
                        ).show()
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
