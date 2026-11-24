package com.mariam.live

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * First safe release of the BAK LIVE entertainment game.
 * Wallet balances are read from the existing wallet API and displayed read-only.
 * Game rounds award non-purchasable, non-transferable entertainment score only.
 */
class GamesActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val foods = listOf("🍅 طماطم", "🥕 جزر", "🌽 ذرة", "🥬 ملفوف", "🌶️ فلفل", "🍆 باذنجان", "🥒 خيار", "🍄 فطر")
    private var selected = -1
    private var seconds = 25
    private var score = 0
    private var roundRunning = false
    private var timer: CountDownTimer? = null
    private var walletCoins = 0L
    private var diamonds = 0L
    private lateinit var balanceText: TextView
    private lateinit var timerText: TextView
    private lateinit var resultText: TextView
    private lateinit var scoreText: TextView
    private lateinit var wheel: FoodWheelView
    private lateinit var startButton: Button
    private lateinit var options: GridLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fmt(v: Long) = NumberFormat.getInstance(Locale.US).format(v)
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(26, 18, 53)
        window.navigationBarColor = Color.WHITE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 246, 251))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.rgb(26, 18, 53))
        }
        val back = label("‹", 32f, Color.WHITE, true).apply {
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(44)))
        header.addView(label("الألعاب الترفيهية", 19f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)

        val scroll = android.widget.ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val balanceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(66, 43, 126), Color.rgb(31, 25, 68))
            ).apply { cornerRadius = dp(18).toFloat() }
        }
        balanceCard.addView(label("رصيد محفظتك الحالي", 13f, 0xFFDCD4F5.toInt()))
        balanceText = label("جارٍ تحميل الرصيد…", 23f, Color.WHITE, true)
        balanceCard.addView(balanceText, LinearLayout.LayoutParams(-1, dp(38)))
        balanceCard.addView(label("الرصيد للعرض فقط في النسخة الترفيهية", 11f, 0xFFC7BDE6.toInt()))
        body.addView(balanceCard, LinearLayout.LayoutParams(-1, -2))

        body.addView(label("لعبة الخضار", 22f, Color.rgb(36, 30, 58), true),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        body.addView(label("اختر أحد الخيارات الثمانية ثم شاهد نتيجة الجولة", 12f, Color.rgb(120, 116, 135)),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        timerText = label("25", 28f, Color.rgb(190, 102, 29), true)
        body.addView(timerText, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(8) })

        wheel = FoodWheelView()
        body.addView(wheel, LinearLayout.LayoutParams(dp(300), dp(300)).apply { topMargin = dp(4) })

        options = GridLayout(this).apply {
            columnCount = 2
            rowCount = 4
            useDefaultMargins = false
        }
        foods.forEachIndexed { index, food ->
            val b = Button(this).apply {
                text = food
                isAllCaps = false
                textSize = 14f
                setTextColor(Color.rgb(48, 39, 68))
                setOnClickListener {
                    if (!roundRunning) {
                        selected = index
                        wheel.selectedIndex = index
                        refreshOptions()
                        resultText.text = "اختيارك: ${foods[index]}"
                    }
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(52)
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            options.addView(b, params)
        }
        body.addView(options, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        resultText = label("اختر طعاماً لبدء الجولة", 14f, Color.rgb(72, 61, 96), true)
        body.addView(resultText, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })
        scoreText = label("نقاطك الترفيهية: 0", 13f, Color.rgb(107, 87, 153), true)
        body.addView(scoreText, LinearLayout.LayoutParams(-1, dp(28)))

        startButton = Button(this).apply {
            text = "ابدأ جولة مجانية"
            isAllCaps = false
            textSize = 15f
            setOnClickListener { startRound() }
        }
        body.addView(startButton, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
        body.addView(label("اللعب هنا ترفيهي بنقاط مجانية فقط؛ لا يتم خصم العملات أو إضافة مكاسب إلى المحفظة.", 11f, Color.rgb(132, 127, 145)),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        setContentView(root)
        loadWallet()
    }

    private fun loadWallet() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            balanceText.text = "تعذر تحميل المحفظة"
            return
        }
        io.execute {
            try {
                val response = AuthApi.get("/api/wallet", token)
                val wallet = response.body.optJSONObject("wallet") ?: response.body
                if (response.status in 200..299) {
                    walletCoins = wallet.optLong("recharge_coins")
                    diamonds = wallet.optLong("earned_diamonds")
                    runOnUiThread {
                        if (!isFinishing) balanceText.text = "${fmt(walletCoins)} عملة  •  ${fmt(diamonds)} ألماس"
                    }
                } else runOnUiThread { if (!isFinishing) balanceText.text = "تعذر تحميل الرصيد" }
            } catch (_: Exception) {
                runOnUiThread { if (!isFinishing) balanceText.text = "تعذر الاتصال بالمحفظة" }
            }
        }
    }

    private fun refreshOptions() {
        for (i in 0 until options.childCount) {
            val b = options.getChildAt(i) as? Button ?: continue
            b.alpha = if (i == selected) 1f else .78f
            b.isSelected = i == selected
        }
    }

    private fun startRound() {
        if (roundRunning) return
        if (selected !in foods.indices) {
            Toast.makeText(this, "اختر أحد الخيارات أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        roundRunning = true
        seconds = 25
        startButton.isEnabled = false
        startButton.text = "الجولة جارية…"
        timerText.text = seconds.toString()
        resultText.text = "تم تثبيت اختيارك: ${foods[selected]}"
        timer = object : CountDownTimer(25_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = (millisUntilFinished / 1000L).toInt()
                timerText.text = seconds.toString()
            }
            override fun onFinish() {
                timerText.text = "0"
                finishRound()
            }
        }.start()
    }

    private fun finishRound() {
        val winning = Random.nextInt(foods.size)
        val rotation = 360f * 5f + (winning * 45f) + 22.5f
        ObjectAnimator.ofFloat(wheel, View.ROTATION, wheel.rotation, wheel.rotation + rotation).apply {
            duration = 4200
            interpolator = DecelerateInterpolator(2.2f)
            start()
        }.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                resultText.text = "النتيجة: ${foods[winning]}"
                if (winning == selected) {
                    score += 10
                    resultText.append("  •  أحسنت! +10 نقاط ترفيهية")
                } else {
                    resultText.append("  •  جولة جديدة بانتظارك")
                }
                scoreText.text = "نقاطك الترفيهية: $score"
                roundRunning = false
                selected = -1
                wheel.selectedIndex = -1
                refreshOptions()
                startButton.isEnabled = true
                startButton.text = "ابدأ جولة مجانية"
            }
        })
    }

    override fun onDestroy() {
        timer?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    private inner class FoodWheelView : View(this@GamesActivity) {
        var selectedIndex = -1
            set(value) { field = value; invalidate() }
        private val colors = intArrayOf(
            0xFFFFD166.toInt(), 0xFFEF8354.toInt(), 0xFF8ECF88.toInt(), 0xFF72C5D8.toInt(),
            0xFFB69AE8.toInt(), 0xFFFF9FBA.toInt(), 0xFF9AC7A5.toInt(), 0xFFFFB45C.toInt()
        )
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds = RectF()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val side = minOf(width, height).toFloat()
            val pad = dp(9).toFloat()
            bounds.set((width-side)/2+pad, (height-side)/2+pad, (width+side)/2-pad, (height+side)/2-pad)
            for (i in 0..7) {
                paint.color = colors[i]
                paint.style = Paint.Style.FILL
                canvas.drawArc(bounds, i*45f-90f, 45f, true, paint)
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2).toFloat()
                canvas.drawArc(bounds, i*45f-90f, 45f, true, paint)
                val angle = Math.toRadians((i*45.0)-67.5)
                val cx = bounds.centerX() + kotlin.math.cos(angle).toFloat() * side * .30f
                val cy = bounds.centerY() + kotlin.math.sin(angle).toFloat() * side * .30f
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(45, 37, 58)
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                paint.textSize = dp(10).toFloat()
                canvas.drawText(foods[i].substringAfter(" "), cx, cy, paint)
            }
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), side*.105f, paint)
            paint.color = Color.rgb(61, 43, 111)
            paint.textSize = dp(13).toFloat()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("BAK", bounds.centerX(), bounds.centerY()+dp(5), paint)
            paint.color = Color.rgb(190, 73, 62)
            paint.style = Paint.Style.FILL
            val arrow = floatArrayOf(bounds.centerX(), bounds.top-dp(1).toFloat(), bounds.centerX()-dp(10), bounds.top+dp(18).toFloat(), bounds.centerX()+dp(10), bounds.top+dp(18).toFloat())
            val path = android.graphics.Path().apply { moveTo(arrow[0],arrow[1]); lineTo(arrow[2],arrow[3]); lineTo(arrow[4],arrow[5]); close() }
            canvas.drawPath(path, paint)
        }
    }
}
