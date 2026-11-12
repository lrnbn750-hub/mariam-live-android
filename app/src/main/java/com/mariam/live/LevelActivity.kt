package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.math.min

class LevelActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var level = 1
    private var currentXp = 0L
    private var nextXp = 100L
    private var totalXp = 0L
    private var progressValue = 0.0
    private lateinit var hero: LinearLayout
    private lateinit var levelText: TextView
    private lateinit var rankText: TextView
    private lateinit var totalText: TextView
    private lateinit var progressText: TextView
    private lateinit var remainingText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var gem: GemView
    private lateinit var tierList: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    private fun panel(colors: IntArray, radius: Int, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun palette(lv: Int): IntArray = when (lv) {
        in 1..10 -> intArrayOf(Color.rgb(93,57,35), Color.rgb(192,125,76), Color.rgb(255,207,155), Color.rgb(255,235,210))
        in 11..20 -> intArrayOf(Color.rgb(48,66,91), Color.rgb(115,151,190), Color.rgb(193,222,255), Color.WHITE)
        in 21..30 -> intArrayOf(Color.rgb(91,61,13), Color.rgb(194,145,36), Color.rgb(255,215,91), Color.rgb(255,245,198))
        in 31..40 -> intArrayOf(Color.rgb(8,75,62), Color.rgb(26,166,132), Color.rgb(93,245,198), Color.rgb(213,255,240))
        in 41..50 -> intArrayOf(Color.rgb(9,48,98), Color.rgb(37,132,220), Color.rgb(105,203,255), Color.rgb(224,246,255))
        in 51..60 -> intArrayOf(Color.rgb(62,25,105), Color.rgb(153,79,210), Color.rgb(225,163,255), Color.rgb(249,230,255))
        in 61..70 -> intArrayOf(Color.rgb(91,20,48), Color.rgb(199,49,91), Color.rgb(255,137,170), Color.rgb(255,230,237))
        in 71..80 -> intArrayOf(Color.rgb(8,73,89), Color.rgb(32,169,184), Color.rgb(111,244,239), Color.rgb(222,255,255))
        in 81..90 -> intArrayOf(Color.rgb(43,22,111), Color.rgb(119,77,220), Color.rgb(202,177,255), Color.rgb(246,239,255))
        in 91..99 -> intArrayOf(Color.rgb(90,20,43), Color.rgb(191,48,79), Color.rgb(255,199,111), Color.rgb(255,244,208))
        else -> intArrayOf(Color.rgb(71,20,89), Color.rgb(173,54,145), Color.rgb(255,224,124), Color.rgb(255,249,218))
    }
    private fun rank(lv: Int) = when (lv) {
        in 1..10 -> "البرونزي"; in 11..20 -> "الفضي"; in 21..30 -> "الذهبي"
        in 31..40 -> "الزمردي"; in 41..50 -> "الياقوتي"; in 51..60 -> "الماسي"
        in 61..70 -> "الملكي"; in 71..80 -> "الإمبراطوري"; in 81..90 -> "النخبوي"
        in 91..99 -> "الأسطوري"; else -> "الإمبراطور الأعظم"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5,12,27)
        window.navigationBarColor = Color.rgb(5,12,27)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(intArrayOf(Color.rgb(7,14,30), Color.rgb(5,18,34), Color.rgb(7,10,24)), 0)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(30))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("‹", 34f, Color.WHITE).apply { setOnClickListener { finish() } },
            LinearLayout.LayoutParams(dp(48), dp(52)))
        header.addView(label("مستوى الثراء", 21f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(label("✧", 25f, Color.rgb(214,190,255)), LinearLayout.LayoutParams(dp(42), dp(52)))
        body.addView(header)

        hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = panel(intArrayOf(Color.rgb(48,27,91), Color.rgb(20,27,51), Color.rgb(8,18,34)), 28)
        }
        hero.addView(label("WEALTH RANK  •  رتبة الثراء", 11f, Color.rgb(197,184,226), true),
            LinearLayout.LayoutParams(-1, dp(25)))
        levelText = label("LV 1", 36f, Color.WHITE, true)
        hero.addView(levelText, LinearLayout.LayoutParams(-1, dp(52)))
        gem = GemView()
        hero.addView(gem, LinearLayout.LayoutParams(-1, dp(158)))
        rankText = label("الرتبة البرونزية", 21f, Color.WHITE, true)
        hero.addView(rankText, LinearLayout.LayoutParams(-1, dp(38)))
        totalText = label("إجمالي نقاط الخبرة  0 XP", 13f, Color.rgb(199,205,220))
        hero.addView(totalText, LinearLayout.LayoutParams(-1, dp(28)))
        hero.addView(View(this).apply { setBackgroundColor(Color.argb(35,255,255,255)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(0, dp(14), 0, dp(12)) })
        val progressHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        progressHeader.addView(label("التقدم للمستوى التالي", 12f, Color.rgb(195,203,220)),
            LinearLayout.LayoutParams(0, dp(26), 1f))
        progressText = label("0%", 13f, Color.WHITE, true)
        progressHeader.addView(progressText, LinearLayout.LayoutParams(dp(55), dp(26)))
        hero.addView(progressHeader, LinearLayout.LayoutParams(-1, dp(28)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
            progressTintList = ColorStateList.valueOf(Color.rgb(203,169,255))
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(57,59,82))
        }
        hero.addView(progress, LinearLayout.LayoutParams(-1, dp(9)).apply { setMargins(0, dp(5), 0, dp(10)) })
        remainingText = label("باقي 100 نقطة للمستوى التالي", 13f, Color.rgb(114,235,216), true)
        hero.addView(remainingText, LinearLayout.LayoutParams(-1, dp(30)))
        body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12); bottomMargin = dp(22)
        })

        val section = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        section.addView(label("مسار الرتب", 20f, Color.WHITE, true).apply { gravity = Gravity.RIGHT },
            LinearLayout.LayoutParams(0, dp(42), 1f))
        section.addView(label("10 مراحل", 11f, Color.rgb(165,174,195)), LinearLayout.LayoutParams(dp(78), dp(42)))
        body.addView(section)
        body.addView(label("كل مرحلة تفتح هوية ومكانة جديدة لحسابك", 12f, Color.rgb(145,158,181)).apply {
            gravity = Gravity.RIGHT; setPadding(0, 0, 0, dp(12))
        })
        tierList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(tierList)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
        loadLevel()
    }

    private fun render() {
        val c = palette(level)
        hero.background = panel(intArrayOf(c[0], Color.rgb(20,27,48), Color.rgb(7,17,32)), 28, c[2])
        gem.setAccent(c[2], c[1])
        levelText.text = "LV $level"
        rankText.text = "رتبة ${rank(level)}"
        totalText.text = "إجمالي نقاط الخبرة  •  ${totalXp} XP"
        val pct = if (level >= 100) 100 else (progressValue * 100).toInt().coerceIn(0,100)
        progressText.text = "$pct%"
        progress.progress = pct * 10
        remainingText.text = if (level >= 100) "وصلت إلى أعلى رتبة في مريم لايف"
            else "باقي ${(nextXp-currentXp).coerceAtLeast(0)} XP للمستوى التالي"

        tierList.removeAllViews()
        val bands = listOf(1..10,11..20,21..30,31..40,41..50,51..60,61..70,71..80,81..90,91..100)
        bands.forEach { range ->
            val active = level in range
            val unlocked = level > range.last
            val colors = palette(range.first)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = panel(
                    if (active) intArrayOf(Color.rgb(42,45,70), Color.rgb(23,34,52))
                    else intArrayOf(Color.rgb(25,37,53), Color.rgb(17,28,43)),
                    20, if (active) colors[2] else Color.rgb(42,57,75))
            }
            val iconWrap = FrameLayout(this).apply {
                background = panel(intArrayOf(Color.argb(70,colors[1]), Color.argb(25,colors[2])), 16)
            }
            iconWrap.addView(label(if (unlocked) "✓" else if (active) "✦" else "◇", 27f, colors[2], true),
                FrameLayout.LayoutParams(-1, -1))
            card.addView(iconWrap, LinearLayout.LayoutParams(dp(54), dp(54)))
            val names = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
            }
            names.addView(label("LV ${range.first}  —  LV ${range.last}", 15f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
            })
            names.addView(label(rank(range.first), 12f, colors[2]).apply {
                gravity = Gravity.RIGHT; setPadding(0, dp(3), 0, 0)
            })
            card.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
            val state = when { active -> "رتبتك الحالية"; unlocked -> "مكتملة"; else -> "مقفلة" }
            card.addView(label(state, 10f, if (active) colors[2] else Color.rgb(157,169,188), active),
                LinearLayout.LayoutParams(dp(82), dp(42)))
            if (active) card.elevation = dp(5).toFloat()
            card.setOnClickListener {
                val msg = when { active -> "أنت حالياً ضمن رتبة ${rank(level)} • LV $level"
                    unlocked -> "لقد أكملت هذه المرحلة"
                    else -> "اجمع نقاط الخبرة لفتح رتبة ${rank(range.first)}" }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            tierList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
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

    private inner class GemView : View(this@LevelActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var main = Color.rgb(203,169,255)
        private var dark = Color.rgb(119,77,220)
        fun setAccent(primary: Int, secondary: Int) { main = primary; dark = secondary; invalidate() }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val s = min(width * 0.30f, height * 0.43f)
            paint.shader = RadialGradient(cx, cy, s * 1.5f,
                intArrayOf(Color.argb(85, Color.red(main), Color.green(main), Color.blue(main)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, s * 1.5f, paint)
            paint.shader = null
            val left = PointF(cx-s*1.15f, cy-s*.42f)
            val right = PointF(cx+s*1.15f, cy-s*.42f)
            val bottom = PointF(cx, cy+s)
            val crown = Path().apply {
                moveTo(left.x,left.y); lineTo(cx-s*.62f,cy-s*.88f)
                lineTo(cx+s*.62f,cy-s*.88f); lineTo(right.x,right.y); lineTo(bottom.x,bottom.y); close()
            }
            paint.shader = LinearGradient(cx-s,cy-s,cx+s,cy+s,
                intArrayOf(Color.WHITE, main, dark), null, Shader.TileMode.CLAMP)
            canvas.drawPath(crown, paint)
            paint.shader = null
            val facets = listOf(
                Path().apply { moveTo(left.x,left.y); lineTo(cx-s*.62f,cy-s*.88f); lineTo(cx,cy); close() },
                Path().apply { moveTo(cx-s*.62f,cy-s*.88f); lineTo(cx+s*.62f,cy-s*.88f); lineTo(cx,cy); close() },
                Path().apply { moveTo(cx+s*.62f,cy-s*.88f); lineTo(right.x,right.y); lineTo(cx,cy); close() },
                Path().apply { moveTo(left.x,left.y); lineTo(cx,cy); lineTo(bottom.x,bottom.y); close() },
                Path().apply { moveTo(right.x,right.y); lineTo(cx,cy); lineTo(bottom.x,bottom.y); close() }
            )
            val shades = intArrayOf(Color.argb(220,255,255,255), main, dark,
                Color.argb(210,Color.red(dark),Color.green(dark),Color.blue(dark)),
                Color.argb(180,Color.red(main),Color.green(main),Color.blue(main)))
            facets.forEachIndexed { i, path -> paint.color = shades[i]; canvas.drawPath(path, paint) }
            paint.color = Color.argb(190,255,255,255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            canvas.drawPath(crown, paint)
            paint.style = Paint.Style.FILL
            val star = Path().apply {
                moveTo(cx,cy-s*.55f)
                cubicTo(cx+s*.12f,cy-s*.12f,cx+s*.35f,cy-s*.10f,cx+s*.55f,cy)
                cubicTo(cx+s*.30f,cy+s*.10f,cx+s*.12f,cy+s*.15f,cx,cy+s*.55f)
                cubicTo(cx-s*.12f,cy+s*.15f,cx-s*.30f,cy+s*.10f,cx-s*.55f,cy)
                cubicTo(cx-s*.35f,cy-s*.10f,cx-s*.12f,cy-s*.12f,cx,cy-s*.55f)
                close()
            }
            paint.shader = LinearGradient(cx-s*.5f,cy-s*.5f,cx+s*.5f,cy+s*.5f,
                intArrayOf(Color.WHITE, main, dark), null, Shader.TileMode.CLAMP)
            canvas.drawPath(star, paint)
            paint.shader = null
        }
    }
}
