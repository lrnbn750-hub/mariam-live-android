package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class LevelActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var level = 1
    private var currentXp = 0L
    private var nextXp = 100L
    private var totalXp = 0L
    private var progressValue = 0.0
    private lateinit var levelText: TextView
    private lateinit var rankText: TextView
    private lateinit var xpText: TextView
    private lateinit var remainingText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var jewel: TextView
    private lateinit var hero: LinearLayout
    private lateinit var milestones: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }
    private fun palette(lv: Int): IntArray = when (lv) {
        in 1..10 -> intArrayOf(Color.rgb(76,48,31),Color.rgb(153,95,54),Color.rgb(255,205,151),Color.rgb(255,229,198))
        in 11..20 -> intArrayOf(Color.rgb(35,48,66),Color.rgb(104,127,155),Color.rgb(190,218,246),Color.WHITE)
        in 21..30 -> intArrayOf(Color.rgb(78,49,9),Color.rgb(177,124,22),Color.rgb(255,211,83),Color.rgb(255,244,190))
        in 31..40 -> intArrayOf(Color.rgb(8,63,53),Color.rgb(25,145,119),Color.rgb(91,241,201),Color.rgb(211,255,240))
        in 41..50 -> intArrayOf(Color.rgb(8,39,83),Color.rgb(28,111,194),Color.rgb(89,190,255),Color.rgb(219,243,255))
        in 51..60 -> intArrayOf(Color.rgb(52,17,91),Color.rgb(139,58,188),Color.rgb(218,143,255),Color.rgb(246,221,255))
        in 61..70 -> intArrayOf(Color.rgb(83,15,38),Color.rgb(183,37,74),Color.rgb(255,123,151),Color.rgb(255,224,231))
        in 71..80 -> intArrayOf(Color.rgb(8,63,78),Color.rgb(27,151,169),Color.rgb(104,239,244),Color.rgb(220,255,255))
        in 81..90 -> intArrayOf(Color.rgb(37,19,91),Color.rgb(104,65,190),Color.rgb(190,166,255),Color.rgb(239,230,255))
        in 91..99 -> intArrayOf(Color.rgb(81,16,35),Color.rgb(175,35,60),Color.rgb(255,196,100),Color.rgb(255,242,198))
        else -> intArrayOf(Color.rgb(56,14,72),Color.rgb(157,36,123),Color.rgb(255,218,112),Color.rgb(255,248,205))
    }
    private fun rank(lv: Int) = when (lv) {
        in 1..10 -> "البرونزي"
        in 11..20 -> "الفضي"
        in 21..30 -> "الذهبي"
        in 31..40 -> "الزمردي"
        in 41..50 -> "الياقوتي"
        in 51..60 -> "الماسي"
        in 61..70 -> "الملكي"
        in 71..80 -> "الإمبراطوري"
        in 81..90 -> "النخبوي"
        in 91..99 -> "الأسطوري"
        else -> "الإمبراطور الأعظم"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20,17,31)
        window.navigationBarColor = Color.rgb(4,17,29)
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(4,17,29))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(28))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("‹", 34f, Color.WHITE).apply { setOnClickListener { finish() } },
            LinearLayout.LayoutParams(dp(48), dp(52)))
        top.addView(label("مستوى الثراء", 21f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(52), 1f))
        top.addView(label("ⓘ", 24f, Color.WHITE).apply {
            setOnClickListener {
                android.app.AlertDialog.Builder(this@LevelActivity)
                    .setTitle("نظام المستويات")
                    .setMessage("يرتفع مستواك مع جمع نقاط الخبرة. شريط التقدم يوضح ما أنجزته وما تبقى للمستوى التالي.")
                    .setPositiveButton("حسناً", null).show()
            }
        }, LinearLayout.LayoutParams(dp(42), dp(52)))
        body.addView(top)

        hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(22), dp(16), dp(22))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(55,35,21), Color.rgb(21,26,43), Color.rgb(4,17,29))).apply {
                cornerRadius = dp(24).toFloat()
            }
        }
        hero.addView(label("نقاط الثراء", 14f, Color.LTGRAY), LinearLayout.LayoutParams(-1, dp(25)))
        levelText = label("LV 1", 25f, Color.WHITE, true)
        hero.addView(levelText, LinearLayout.LayoutParams(-1, dp(42)))
        jewel = label("✦", 112f, Color.rgb(255,207,133), true)
        hero.addView(jewel, LinearLayout.LayoutParams(-1, dp(150)))
        rankText = label("البرونزي", 18f, Color.WHITE, true)
        hero.addView(rankText, LinearLayout.LayoutParams(-1, dp(34)))
        xpText = label("0 / 100 XP", 14f, Color.LTGRAY)
        hero.addView(xpText, LinearLayout.LayoutParams(-1, dp(28)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
        }
        hero.addView(progress, LinearLayout.LayoutParams(-1, dp(12)).apply {
            setMargins(dp(4), dp(8), dp(4), dp(8))
        })
        remainingText = label("متبقي 100 نقطة للمستوى التالي", 14f, Color.rgb(102,235,215), true)
        hero.addView(remainingText, LinearLayout.LayoutParams(-1, dp(32)))
        body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        body.addView(label("مراحل المستويات", 19f, Color.WHITE, true).apply {
            gravity = Gravity.RIGHT
        }, LinearLayout.LayoutParams(-1, dp(58)))
        milestones = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(milestones)
        body.addView(label("استمر بجمع نقاط الخبرة لفتح الرتب والمزايا الأعلى", 13f,
            Color.rgb(160,170,183)).apply { setPadding(0, dp(20), 0, 0) },
            LinearLayout.LayoutParams(-1, dp(54)))

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
        loadLevel()
    }

    private fun render() {
        val c = palette(level)
        hero.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(c[0], Color.rgb(18,25,39), Color.rgb(4,17,29))).apply {
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), c[2])
        }
        jewel.setTextColor(c[2])
        levelText.text = "LV $level"
        rankText.text = "رتبة ${rank(level)}"
        xpText.text = if (level >= 100) "MAX LEVEL • $totalXp XP"
            else "$currentXp / $nextXp XP  •  المجموع $totalXp"
        progress.progress = if (level >= 100) 1000 else (progressValue * 1000).toInt()
        remainingText.text = if (level >= 100) "وصلت إلى أعلى مستوى"
            else "باقي ${(nextXp-currentXp).coerceAtLeast(0)} نقطة للمستوى التالي"

        milestones.removeAllViews()
        val bands = listOf(1..10,11..20,21..30,31..40,41..50,51..60,61..70,71..80,81..90,91..100)
        bands.forEach { range ->
            val active = level in range
            val unlocked = level > range.last
            val color = palette(range.first).let { it[2] }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(if (active) Color.rgb(37,48,62) else Color.rgb(25,37,49),
                    16, if (active) color else Color.rgb(39,55,68))
            }
            val emblem = label(if (unlocked) "✓" else if (active) "✦" else "◆",
                27f, color, true)
            card.addView(emblem, LinearLayout.LayoutParams(dp(48), dp(48)))
            val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            names.addView(label("LV ${range.first} – LV ${range.last}", 16f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
            })
            names.addView(label(rank(range.first), 12f, Color.LTGRAY).apply {
                gravity = Gravity.RIGHT
            })
            card.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(label(when { active -> "مستواك الحالي"; unlocked -> "مكتمل"; else -> "مقفل" },
                11f, if (active) color else Color.LTGRAY, active),
                LinearLayout.LayoutParams(dp(92), dp(42)))
            milestones.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            })
        }
    }

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
                runOnUiThread { if (!isFinishing) render() }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing) Toast.makeText(this, e.message ?: "تعذر الاتصال", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
