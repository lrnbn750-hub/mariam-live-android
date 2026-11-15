package com.mariam.live
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class LevelActivity : Activity() {
    companion object { private const val TAG = "BAKWealthLevel" }
    private val executor = Executors.newSingleThreadExecutor()
    private var level = 1
    private var totalXp = 0L
    private var currentXp = 0L
    private var nextXp = 100L
    private var progressValue = 0.0
    private lateinit var heroSummary: TextView
    private lateinit var xpText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var percentText: TextView
    private lateinit var footerText: TextView
    private lateinit var tierRows: List<TextView>
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER; includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun cardBackground() = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF3D164D.toInt(), 0xFF0D1628.toInt())).apply {
        cornerRadius = dp(28).toFloat(); setStroke(dp(1).coerceAtLeast(1), 0xFFBEA064.toInt())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 15, 30); window.navigationBarColor = Color.rgb(7, 15, 30)
        buildScreen(); renderLevel(); loadLevel()
    }
    private fun buildScreen() {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF070F1E.toInt()) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(4), dp(10), dp(4)) }
        val back = text("‹", 36f); bar.addView(back, LinearLayout.LayoutParams(dp(48), dp(48))); back.setOnClickListener { finish() }
        bar.addView(text("مستوى الثراء", 19f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        bar.addView(View(this), LinearLayout.LayoutParams(dp(48), dp(48))); page.addView(bar)
        val scroll = ScrollView(this).apply { setFillViewport(true); clipToPadding = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(30)) }
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(20), dp(22), dp(20), dp(22)); background = cardBackground() }
        hero.addView(text("WEALTH RANK  •  رتبة الثراء", 12f, 0xFFD5C9E8.toInt(), true))
        hero.addView(text("💎", 100f), LinearLayout.LayoutParams(-1, dp(140)))
        heroSummary = text("", 24f, Color.WHITE, true); hero.addView(heroSummary)
        xpText = text("", 14f, 0xFFB8C2D5.toInt()); hero.addView(xpText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val divider = View(this).apply { setBackgroundColor(0x26FFFFFF) }; hero.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(20) })
        val progressHead = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        progressHead.addView(text("التقدم للمستوى التالي", 13f, 0xFFB8C2D5.toInt()), LinearLayout.LayoutParams(0, dp(42), 1f))
        percentText = text("0%", 14f, Color.WHITE, true); progressHead.addView(percentText, LinearLayout.LayoutParams(dp(55), dp(42))); hero.addView(progressHead)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000 }
        hero.addView(progressBar, LinearLayout.LayoutParams(-1, dp(9)))
        footerText = text("", 14f, 0xFF55DCCB.toInt(), true); hero.addView(footerText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        body.addView(hero, LinearLayout.LayoutParams(-1, -2))
        val pathHead = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(26), dp(4), dp(8)) }
        pathHead.addView(text("مسار الرتب", 22f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(42), 1f)); pathHead.addView(text("10 مراحل", 13f, 0xFF8FA0BC.toInt())); body.addView(pathHead)
        body.addView(text("كل مرحلة تفتح هوية ومكانة جديدة لحسابك", 13f, 0xFF74829D.toInt()).apply { gravity = Gravity.START; setPadding(dp(4), 0, 0, dp(14)) })
        val rows = ArrayList<TextView>()
        for (i in 0 until 10) {
            val row = text("", 16f, Color.WHITE, true).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18)); minHeight = dp(82); background = GradientDrawable().apply { setColor(0xFF142033.toInt()); cornerRadius = dp(20).toFloat(); setStroke(dp(1), 0xFF28364B.toInt()) } }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { if (i > 0) topMargin = dp(10) }); rows.add(row)
        }
        tierRows = rows; scroll.addView(body); page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(page)
    }
    private fun renderLevel() {
        if (!::heroSummary.isInitialized) return
        val idx = LevelTiers.indexOf(level); val tier = LevelTiers.ALL[idx]
        heroSummary.text = "LV $level\\n" + tier.displayName(this)
        xpText.text = "إجمالي نقاط الخبرة  •  " + String.format(Locale.US, "%,d", totalXp) + " XP"
        val p = if (level >= 100) 1f else progressValue.toFloat().coerceIn(0f, 1f)
        percentText.text = "${(p * 100).toInt()}%"; progressBar.progress = (p * 1000).toInt()
        footerText.text = if (level >= 100) "وصلت إلى أعلى رتبة في BAK LIVE" else "متبقي " + String.format(Locale.US, "%,d", (nextXp - currentXp).coerceAtLeast(0)) + " XP للمستوى التالي"
        tierRows.forEachIndexed { i, row ->
            val first = LevelTiers.firstLevel(i); val last = LevelTiers.lastLevel(i); val t = LevelTiers.ALL[i]
            val status = when { level > last -> "مكتملة"; level >= first -> "رتبتك الحالية"; else -> "مقفلة" }
            row.text = "LV $first — LV $last\\n${t.displayName(this)}     •     $status"
            row.alpha = if (level < first) 0.72f else 1f
            row.setOnClickListener { Toast.makeText(this, "${t.displayName(this)} • LV $first–$last • $status", Toast.LENGTH_SHORT).show() }
        }
    }
    private fun loadLevel() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        executor.execute {
            try {
                val result = AuthApi.get("/api/levels/me", token)
                if (result.status !in 200..299) throw Exception("تعذر تحميل بيانات المستوى")
                val data: JSONObject = result.body
                val l = data.optInt("level", 1).coerceIn(1, 100); val xp = data.optLong("total_xp", 0).coerceAtLeast(0)
                val cxp = data.optLong("current_level_xp", 0).coerceAtLeast(0); val nxp = data.optLong("next_level_xp", 100).coerceAtLeast(1)
                val prog = data.optDouble("progress", 0.0).coerceIn(0.0, 1.0)
                runOnUiThread { if (!isFinishing && !isDestroyed) { level = l; totalXp = xp; currentXp = cxp; nextXp = nxp; progressValue = prog; renderLevel() } }
            } catch (e: Exception) { Log.e(TAG, "Failed to load wealth level", e); runOnUiThread { if (!isFinishing && !isDestroyed) Toast.makeText(this, "تعذر تحديث بيانات المستوى، تحقق من الاتصال", Toast.LENGTH_LONG).show() } }
        }
    }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}