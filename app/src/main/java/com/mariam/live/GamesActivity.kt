package com.mariam.live

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Garden Wheel - entertainment game (original design).
 * - Uses FREE entertainment points only (local, not purchasable, not transferable).
 * - The real wallet is only displayed, read-only. Nothing is deducted from or added to it.
 */
class GamesActivity : Activity() {

    private data class Item(val emoji: String, val name: String, val mult: Int, val color: Int)

    private val items = listOf(
        Item("🍅", "طماطم", 5, 0xFFFF6B6B.toInt()),
        Item("🥕", "جزر", 5, 0xFFFFA94D.toInt()),
        Item("🌽", "ذرة", 5, 0xFFFFD43B.toInt()),
        Item("🥬", "ملفوف", 5, 0xFF69DB7C.toInt()),
        Item("🌶️", "فلفل", 10, 0xFFFF8787.toInt()),
        Item("🍆", "باذنجان", 15, 0xFFB197FC.toInt()),
        Item("🥒", "خيار", 25, 0xFF38D9A9.toInt()),
        Item("🍄", "فطر", 45, 0xFF74C0FC.toInt())
    )
    private val chips = longArrayOf(2, 10, 50, 100, 1000)
    private val bets = LongArray(8)
    private val history = ArrayDeque<Int>()

    private var chipIndex = 1
    private var points = 2000L
    private var todayWin = 0L
    private var bettingOpen = false
    private var timer: CountDownTimer? = null
    private val io = Executors.newSingleThreadExecutor()

    private lateinit var wheel: WheelView
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var pointsText: TextView
    private lateinit var winText: TextView
    private lateinit var walletText: TextView
    private lateinit var historyRow: LinearLayout
    private lateinit var chipRow: LinearLayout
    private lateinit var grid: GridLayout

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

    private fun rounded(fill: Int, radius: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 30, 34)
        window.navigationBarColor = Color.rgb(7, 30, 34)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(15, 61, 62), Color.rgb(7, 30, 34))
            )
        }

        // ---------- Header ----------
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        header.addView(label("‹", 32f, Color.WHITE, true).apply { setOnClickListener { finish() } },
            LinearLayout.LayoutParams(dp(42), dp(44)))
        header.addView(label("عجلة الحديقة", 20f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(42), dp(44)))
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(4), dp(14), dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---------- Stats row ----------
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pointsText = label("", 16f, Color.rgb(255, 224, 130), true)
        winText = label("", 16f, Color.rgb(128, 237, 153), true)
        stats.addView(statCard("نقاطي", pointsText), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        stats.addView(statCard("ربح اليوم", winText), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        body.addView(stats, LinearLayout.LayoutParams(-1, -2))

        walletText = label("جارٍ تحميل المحفظة…", 11f, Color.rgb(160, 190, 190))
        body.addView(walletText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        // ---------- Wheel ----------
        val wheelBox = FrameLayout(this)
        wheel = WheelView()
        wheelBox.addView(wheel, FrameLayout.LayoutParams(-1, -1))
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(11, 44, 48))
                setStroke(dp(4), Color.rgb(255, 214, 102))
            }
        }
        statusText = label("اختر", 12f, Color.rgb(200, 225, 225))
        timerText = label("25", 28f, Color.WHITE, true)
        center.addView(statusText)
        center.addView(timerText)
        wheelBox.addView(center, FrameLayout.LayoutParams(dp(96), dp(96), Gravity.CENTER))
        wheelBox.addView(label("▼", 28f, Color.rgb(255, 214, 102), true),
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        body.addView(wheelBox, LinearLayout.LayoutParams(dp(310), dp(310)).apply { topMargin = dp(6) })

        // ---------- Betting grid ----------
        grid = GridLayout(this).apply { columnCount = 4 }
        items.forEachIndexed { i, _ ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener { placeBet(i) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = -2
                columnSpec = GridLayout.spec(i % 4, 1f)
                rowSpec = GridLayout.spec(i / 4)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            grid.addView(card, lp)
        }
        body.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // ---------- Chips ----------
        chipRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        chips.forEachIndexed { i, v ->
            val chip = label(fmt(v), if (v >= 1000) 12f else 14f, Color.rgb(60, 40, 0), true).apply {
                setOnClickListener { chipIndex = i; refreshChips() }
            }
            chipRow.addView(chip, LinearLayout.LayoutParams(dp(54), dp(54)).apply { setMargins(dp(5), 0, dp(5), 0) })
        }
        body.addView(chipRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        // ---------- History ----------
        body.addView(label("آخر النتائج", 12f, Color.rgb(160, 190, 190)),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        historyRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(historyRow) }
        body.addView(hs, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(4) })

        body.addView(label("لعبة ترفيهية بنقاط مجانية فقط؛ لا يتم خصم أو إضافة أي رصيد من محفظتك.", 11f,
            Color.rgb(130, 160, 160)), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        setContentView(root)
        refreshAll()
        loadWallet()
        startBetting()
    }

    private fun statCard(title: String, value: TextView): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = rounded(Color.argb(40, 255, 255, 255), 16)
            addView(label(title, 11f, Color.rgb(170, 200, 200)))
            addView(value, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        }

    // ---------- Game flow ----------
    private fun startBetting() {
        bets.fill(0)
        bettingOpen = true
        statusText.text = "راهن"
        refreshAll()
        timer?.cancel()
        timer = object : CountDownTimer(25_000, 1_000) {
            override fun onTick(ms: Long) { timerText.text = (ms / 1000L).toString() }
            override fun onFinish() { timerText.text = "0"; spin() }
        }.start()
    }

    private fun placeBet(i: Int) {
        if (!bettingOpen) return
        val amount = chips[chipIndex]
        if (points < amount) {
            Toast.makeText(this, "نقاطك غير كافية", Toast.LENGTH_SHORT).show()
            return
        }
        points -= amount
        bets[i] += amount
        refreshAll()
    }

    private fun spin() {
        bettingOpen = false
        statusText.text = "تدور…"
        val winning = Random.nextInt(items.size)
        // Sector i is centred at (i*45 - 67.5) deg; pointer is at -90 deg.
        val current = ((wheel.rotation % 360f) + 360f) % 360f
        val target = ((-22.5f - winning * 45f - current) % 360f + 360f) % 360f
        val delta = 360f * 5f + target
        ObjectAnimator.ofFloat(wheel, View.ROTATION, wheel.rotation, wheel.rotation + delta).apply {
            duration = 4200
            interpolator = DecelerateInterpolator(2.2f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { showResult(winning) }
            })
            start()
        }
    }

    private fun showResult(winning: Int) {
        val win = bets[winning] * items[winning].mult
        points += win
        todayWin += win
        history.addFirst(winning)
        while (history.size > 8) history.removeLast()
        statusText.text = items[winning].emoji
        timerText.text = if (win > 0) "+${fmt(win)}" else "—"
        timerText.textSize = if (win > 0) 18f else 28f
        refreshAll()
        window.decorView.postDelayed({
            if (!isFinishing) {
                timerText.textSize = 28f
                startBetting()
            }
        }, 3000)
    }

    // ---------- UI refresh ----------
    private fun refreshAll() {
        pointsText.text = fmt(points)
        winText.text = fmt(todayWin)
        items.forEachIndexed { i, item ->
            val card = grid.getChildAt(i) as LinearLayout
            card.removeAllViews()
            card.background = if (bets[i] > 0)
                rounded(Color.argb(70, 255, 214, 102), 14, Color.rgb(255, 214, 102), 2)
            else rounded(Color.argb(35, 255, 255, 255), 14)
            card.addView(label(item.emoji, 26f, Color.WHITE))
            card.addView(label("x${item.mult}", 13f, item.color, true))
            card.addView(label(if (bets[i] > 0) fmt(bets[i]) else item.name, 11f,
                if (bets[i] > 0) Color.rgb(255, 224, 130) else Color.rgb(190, 215, 215), bets[i] > 0))
        }
        refreshChips()
        historyRow.removeAllViews()
        history.forEach { idx ->
            historyRow.addView(label(items[idx].emoji, 20f, Color.WHITE).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(50, 255, 255, 255))
                }
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
    }

    private fun refreshChips() {
        for (i in 0 until chipRow.childCount) {
            val c = chipRow.getChildAt(i)
            c.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 205, 77))
                if (i == chipIndex) setStroke(dp(4), Color.WHITE) else setStroke(dp(2), Color.rgb(190, 140, 20))
            }
            c.scaleX = if (i == chipIndex) 1.12f else 1f
            c.scaleY = c.scaleX
        }
    }

    private fun loadWallet() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) { walletText.text = ""; return }
        io.execute {
            try {
                val response = AuthApi.get("/api/wallet", token)
                val wallet = response.body.optJSONObject("wallet") ?: response.body
                if (response.status in 200..299) {
                    val coins = wallet.optLong("recharge_coins")
                    val dia = wallet.optLong("earned_diamonds")
                    runOnUiThread {
                        if (!isFinishing) walletText.text = "محفظتك (للعرض فقط): ${fmt(coins)} عملة  •  ${fmt(dia)} ألماس"
                    }
                } else runOnUiThread { if (!isFinishing) walletText.text = "" }
            } catch (_: Exception) {
                runOnUiThread { if (!isFinishing) walletText.text = "" }
            }
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    // ---------- Wheel drawing (sectors only; rotated as a whole) ----------
    private inner class WheelView : View(this@GamesActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val side = minOf(width, height).toFloat()
            val pad = dp(14).toFloat()
            bounds.set((width - side) / 2 + pad, (height - side) / 2 + pad,
                (width + side) / 2 - pad, (height + side) / 2 - pad)

            // outer ring
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(255, 214, 102)
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2 + dp(7), paint)

            for (i in 0..7) {
                paint.style = Paint.Style.FILL
                paint.color = items[i].color
                canvas.drawArc(bounds, i * 45f - 90f, 45f, true, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2).toFloat()
                paint.color = Color.rgb(11, 44, 48)
                canvas.drawArc(bounds, i * 45f - 90f, 45f, true, paint)

                val angle = Math.toRadians(i * 45.0 - 67.5)
                val r = bounds.width() * 0.34f
                val cx = bounds.centerX() + kotlin.math.cos(angle).toFloat() * r
                val cy = bounds.centerY() + kotlin.math.sin(angle).toFloat() * r
                canvas.save()
                canvas.rotate((i * 45f - 67.5f) + 90f, cx, cy)
                paint.style = Paint.Style.FILL
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = dp(28).toFloat()
                canvas.drawText(items[i].emoji, cx, cy + dp(6), paint)
                paint.color = Color.rgb(11, 44, 48)
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = dp(13).toFloat()
                canvas.drawText("x${items[i].mult}", cx, cy + dp(26), paint)
                paint.typeface = Typeface.DEFAULT
                canvas.restore()
            }
        }
    }
}