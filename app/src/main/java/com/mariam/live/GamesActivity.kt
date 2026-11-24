package com.mariam.live

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import kotlin.math.ceil
import kotlin.random.Random

private val BG_TOP = 0xFF0A0F2C.toInt()
private val BG_BOTTOM = 0xFF1C1247.toInt()
private val GOLD_LIGHT = 0xFFFFE9A8.toInt()
private val GOLD = 0xFFF5C451.toInt()
private val GOLD_DARK = 0xFFB7791F.toInt()
private val PANEL = 0xFF121544.toInt()

/**
 * Garden Wheel - premium entertainment game (original design).
 * - Uses FREE entertainment points only (local, not purchasable, not transferable).
 * - The real wallet is only displayed, read-only. Nothing is deducted from or added to it.
 */
class GamesActivity : Activity() {

    private data class Item(val emoji: String, val name: String, val mult: Int, val color: Int)

    private val items = listOf(
        Item("🍅", "طماطم", 5, 0xFFFF5D73.toInt()),
        Item("🥕", "جزر", 5, 0xFFFF9F43.toInt()),
        Item("🌽", "ذرة", 5, 0xFFFFC93C.toInt()),
        Item("🥬", "ملفوف", 5, 0xFF4CD97B.toInt()),
        Item("🌶️", "فلفل", 10, 0xFFE8505B.toInt()),
        Item("🍆", "باذنجان", 15, 0xFF9B6BFF.toInt()),
        Item("🥒", "خيار", 25, 0xFF1FC8A5.toInt()),
        Item("🍄", "فطر", 45, 0xFF4DA6FF.toInt())
    )
    private val chipValues = longArrayOf(2, 10, 50, 100, 1000)
    private val bets = LongArray(8)
    private val history = ArrayList<Int>()
    private val coinViews = ArrayList<CoinView>()

    private var chipIndex = 1
    private var points = 2000L
    private var todayWin = 0L
    private var bettingOpen = false
    private var lastWin = -1
    private var round = 221
    private var timer: CountDownTimer? = null
    private val io = Executors.newSingleThreadExecutor()

    private lateinit var wheel: WheelView
    private lateinit var center: CenterView
    private lateinit var pointsText: TextView
    private lateinit var winText: TextView
    private lateinit var betText: TextView
    private lateinit var walletText: TextView
    private lateinit var historyRow: LinearLayout
    private lateinit var grid: GridLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Float) = v * resources.displayMetrics.density
    private fun fmt(v: Long) = NumberFormat.getInstance(Locale.US).format(v)

    private fun blend(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun panel(radius: Int, strokeColor: Int, strokeDp: Int, vararg colors: Int) =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG_TOP
        window.navigationBarColor = BG_BOTTOM

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(BG_TOP, BG_BOTTOM))
        }

        // ---------- Header ----------
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        val back = label("›", 26f, GOLD, true).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(40, 255, 255, 255))
                setStroke(dp(1), GOLD_DARK)
            }
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(40), dp(40)))
        header.addView(label("عجلة الحديقة", 21f, GOLD_LIGHT, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(40), dp(40)))
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(2), dp(14), dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---------- Stats ----------
        pointsText = label("", 16f, GOLD_LIGHT, true)
        winText = label("", 16f, Color.rgb(120, 240, 160), true)
        betText = label("", 16f, Color.WHITE, true)
        val stats = LinearLayout(this)
        stats.addView(statCard("نقاطي", pointsText), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        stats.addView(statCard("رهان الجولة", betText), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        stats.addView(statCard("ربح اليوم", winText), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        body.addView(stats, LinearLayout.LayoutParams(-1, -2))

        walletText = label("", 11f, Color.rgb(150, 160, 200))
        body.addView(walletText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        // ---------- Wheel ----------
        val wheelBox = FrameLayout(this)
        wheel = WheelView()
        wheelBox.addView(wheel, FrameLayout.LayoutParams(-1, -1))
        center = CenterView()
        wheelBox.addView(center, FrameLayout.LayoutParams(dp(108), dp(108), Gravity.CENTER))
        wheelBox.addView(PointerView(), FrameLayout.LayoutParams(dp(38), dp(48), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        body.addView(wheelBox, LinearLayout.LayoutParams(dp(330), dp(330)).apply { topMargin = dp(4) })

        // ---------- Betting grid ----------
        grid = GridLayout(this).apply { columnCount = 4 }
        items.forEachIndexed { i, _ ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(8), dp(2), dp(8))
                setOnClickListener { placeBet(i, it) }
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
        body.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        // ---------- Basket (group) bets ----------
        val baskets = LinearLayout(this)
        baskets.addView(basketCard("🧺 سلة الخضار", 0, 3),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4); marginStart = dp(4) })
        baskets.addView(basketCard("🍖 سلة اللحوم", 4, 7),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        body.addView(baskets, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })

        // ---------- Chips tray ----------
        val tray = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = panel(22, GOLD_DARK, 1, Color.argb(70, 0, 0, 0), Color.argb(120, 0, 0, 0))
        }
        chipValues.forEachIndexed { i, v ->
            val coin = CoinView(v).apply { setOnClickListener { chipIndex = i; refreshChips() } }
            coinViews.add(coin)
            tray.addView(coin, LinearLayout.LayoutParams(dp(58), dp(58)).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        body.addView(tray, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val clear = label("مسح الرهان", 12f, GOLD_LIGHT, true).apply {
            background = panel(16, GOLD, 1, Color.argb(30, 255, 255, 255), Color.argb(30, 255, 255, 255))
            setPadding(dp(18), dp(8), dp(18), dp(8))
            setOnClickListener { clearBets() }
        }
        body.addView(clear, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })

        // ---------- History ----------
        body.addView(label("آخر النتائج", 12f, Color.rgb(150, 160, 200)),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        historyRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(historyRow) }
        body.addView(hs, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(4) })

        body.addView(label("لعبة ترفيهية بنقاط مجانية فقط؛ لا يتم خصم أو إضافة أي رصيد من محفظتك.", 11f,
            Color.rgb(120, 130, 170)), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        setContentView(root)
        refreshAll()
        loadWallet()
        startBetting()
    }

    private fun statCard(title: String, value: TextView): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(10))
            background = panel(16, Color.argb(120, 245, 196, 81), 1, Color.argb(60, 255, 255, 255), Color.argb(20, 255, 255, 255))
            addView(label(title, 11f, Color.rgb(160, 170, 210)))
            addView(value, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        }

    // ---------- Game flow ----------
    private fun startBetting() {
        bets.fill(0)
        bettingOpen = true
        lastWin = -1
        wheel.winIndex = -1
        refreshAll()
        timer?.cancel()
        timer = object : CountDownTimer(25_000, 100) {
            override fun onTick(ms: Long) {
                val s = ceil(ms / 1000.0).toInt()
                center.set("اختر الوقت", s.toString(), 30f, ms / 25_000f)
            }
            override fun onFinish() { spin() }
        }.start()
    }

    private fun placeBet(i: Int, v: View) {
        if (!bettingOpen) return
        val amount = chipValues[chipIndex]
        if (points < amount) {
            Toast.makeText(this, "نقاطك غير كافية", Toast.LENGTH_SHORT).show()
            return
        }
        points -= amount
        bets[i] += amount
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        v.animate().scaleX(.93f).scaleY(.93f).setDuration(70).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        }.start()
        refreshAll()
    }

    /** Payout multiplier when betting a whole basket: 1 / sum(1/mult). */
    private fun basketMult(from: Int, to: Int): Double =
        1.0 / (from..to).sumOf { 1.0 / items[it].mult }

    private fun basketCard(title: String, from: Int, to: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(10))
            background = panel(16, GOLD, 1, Color.argb(70, 245, 196, 81), Color.argb(25, 245, 196, 81))
            addView(label(title, 14f, GOLD_LIGHT, true))
            addView(label("x" + String.format(Locale.US, "%.2f", basketMult(from, to)), 13f, Color.WHITE, true),
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
            setOnClickListener { placeBasket(from, to, it) }
        }

    /** Splits the chip across the basket so every item in it pays the same total. */
    private fun placeBasket(from: Int, to: Int, v: View) {
        if (!bettingOpen) return
        val amount = chipValues[chipIndex]
        if (points < amount) {
            Toast.makeText(this, "نقاطك غير كافية", Toast.LENGTH_SHORT).show()
            return
        }
        val weights = (from..to).map { 1.0 / items[it].mult }
        val total = weights.sum()
        val parts = weights.map { (amount * it / total).toLong() }
        val spent = parts.sum()
        if (spent <= 0L) {
            Toast.makeText(this, "اختر فيشة أكبر لرهان السلة", Toast.LENGTH_SHORT).show()
            return
        }
        parts.forEachIndexed { k, p -> bets[from + k] += p }
        points -= spent
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        refreshAll()
    }

    private fun clearBets() {
        if (!bettingOpen) return
        points += bets.sum()
        bets.fill(0)
        refreshAll()
    }

    private fun spin() {
        bettingOpen = false
        center.set("تدور", "•••", 24f, 1f)
        // Probability proportional to 1/multiplier -> fair odds (small built-in margin).
        val weights = items.map { 1.0 / it.mult }
        var pick = Random.nextDouble() * weights.sum()
        var winning = items.lastIndex
        for (k in weights.indices) {
            pick -= weights[k]
            if (pick <= 0) { winning = k; break }
        }
        // Sector i is centred at (i*45 - 67.5) deg; the pointer sits at -90 deg.
        val current = ((wheel.rotation % 360f) + 360f) % 360f
        val target = ((-22.5f - winning * 45f - current) % 360f + 360f) % 360f
        val delta = 360f * 5f + target
        ObjectAnimator.ofFloat(wheel, View.ROTATION, wheel.rotation, wheel.rotation + delta).apply {
            duration = 4400
            interpolator = DecelerateInterpolator(2.4f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { showResult(winning) }
            })
            start()
        }
    }

    private fun showResult(winning: Int) {
        val win = bets[winning] * items[winning].mult
        showRoundDialog(winning, win, bets.sum())
        round++
        points += win
        todayWin += win
        history.add(0, winning)
        while (history.size > 8) history.removeAt(history.lastIndex)
        lastWin = winning
        wheel.winIndex = winning
        center.set(items[winning].name, if (win > 0) "+${fmt(win)}" else "—", if (win > 0) 20f else 28f, 1f)
        refreshAll()
        window.decorView.postDelayed({ if (!isFinishing) startBetting() }, 3200)
    }

    private fun showRoundDialog(winning: Int, win: Long, staked: Long) {
        if (isFinishing) return
        val d = android.app.Dialog(this)
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(30), dp(22), dp(30), dp(22))
            background = panel(26, GOLD, 2, PANEL, BG_BOTTOM)
        }
        box.addView(label(items[winning].emoji, 64f, Color.WHITE))
        box.addView(label("نتيجة الجولة رقم $round", 15f, GOLD_LIGHT, true),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        box.addView(label("الفائز: ${items[winning].name}  •  x${items[winning].mult}", 13f, Color.rgb(185, 195, 225)),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        box.addView(label("رهانك: ${fmt(staked)}", 14f, Color.WHITE),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        box.addView(label("ربحك: ${fmt(win)}", 20f, if (win > 0) Color.rgb(120, 240, 160) else Color.rgb(150, 160, 200), true),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        d.setContentView(box)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.show()
        box.postDelayed({ if (!isFinishing && d.isShowing) d.dismiss() }, 2800)
    }

    // ---------- UI refresh ----------
    private fun refreshAll() {
        pointsText.text = fmt(points)
        winText.text = fmt(todayWin)
        betText.text = fmt(bets.sum())
        items.forEachIndexed { i, item ->
            val card = grid.getChildAt(i) as LinearLayout
            card.removeAllViews()
            val base = blend(item.color, BG_TOP, .72f)
            card.background = when {
                lastWin == i -> panel(16, Color.WHITE, 2, blend(item.color, BG_TOP, .25f), blend(item.color, BG_TOP, .55f))
                bets[i] > 0 -> panel(16, GOLD, 2, blend(base, GOLD, .18f), base)
                else -> panel(16, Color.argb(90, 245, 196, 81), 1, blend(base, Color.WHITE, .08f), base)
            }
            val bubble = label(item.emoji, 24f, Color.WHITE).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(55, 255, 255, 255))
                }
            }
            card.addView(bubble, LinearLayout.LayoutParams(dp(44), dp(44)))
            card.addView(label("x${item.mult}", 13f, blend(item.color, Color.WHITE, .45f), true),
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            card.addView(label(if (bets[i] > 0) fmt(bets[i]) else item.name, 11f,
                if (bets[i] > 0) GOLD_LIGHT else Color.rgb(185, 195, 225), bets[i] > 0))
        }
        refreshChips()
        historyRow.removeAllViews()
        history.forEachIndexed { n, idx ->
            val chip = label(items[idx].emoji, 20f, Color.WHITE).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(50, 255, 255, 255))
                    setStroke(dp(if (n == 0) 2 else 1), if (n == 0) GOLD else Color.argb(70, 255, 255, 255))
                }
            }
            historyRow.addView(chip, LinearLayout.LayoutParams(dp(38), dp(38)).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
    }

    private fun refreshChips() {
        coinViews.forEachIndexed { i, c -> c.active = (i == chipIndex) }
    }

    private fun loadWallet() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) return
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
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    // =====================================================================
    // Custom views
    // =====================================================================

    /** Rotating wheel: gold frame, blinking lights, glossy sectors. */
    private inner class WheelView : View(this@GamesActivity) {
        var winIndex = -1
            set(value) { field = value; invalidate() }
        private var phase = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val sec = RectF()
        private val blinker = object : Runnable {
            override fun run() { phase = 1 - phase; invalidate(); postDelayed(this, 450) }
        }

        override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(blinker, 450) }
        override fun onDetachedFromWindow() { removeCallbacks(blinker); super.onDetachedFromWindow() }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val outerR = minOf(width, height) / 2f - dpf(4f)
            val sr = outerR - dpf(20f)

            // soft shadow
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(90, 0, 0, 0)
            canvas.drawCircle(cx, cy + dpf(4f), outerR, paint)

            // gold frame
            paint.shader = SweepGradient(cx, cy,
                intArrayOf(GOLD_LIGHT, GOLD_DARK, GOLD_LIGHT, GOLD_DARK, GOLD_LIGHT), null)
            canvas.drawCircle(cx, cy, outerR, paint)
            paint.shader = null
            paint.color = 0xFF14103A.toInt()
            canvas.drawCircle(cx, cy, outerR - dpf(14f), paint)

            // lights
            for (k in 0 until 24) {
                val a = Math.toRadians(k * 15.0)
                val bx = cx + Math.cos(a).toFloat() * (outerR - dpf(7f))
                val by = cy + Math.sin(a).toFloat() * (outerR - dpf(7f))
                val on = (k + phase) % 2 == 0
                if (on) {
                    paint.color = Color.argb(90, 255, 244, 190)
                    canvas.drawCircle(bx, by, dpf(5.5f), paint)
                }
                paint.color = if (on) Color.WHITE else 0xFF8A6A1F.toInt()
                canvas.drawCircle(bx, by, dpf(3f), paint)
            }

            // sectors
            sec.set(cx - sr, cy - sr, cx + sr, cy + sr)
            for (i in 0..7) {
                val c = items[i].color
                paint.style = Paint.Style.FILL
                paint.shader = RadialGradient(cx, cy, sr,
                    intArrayOf(blend(c, Color.WHITE, .35f), c, blend(c, Color.BLACK, .30f)),
                    floatArrayOf(0f, .62f, 1f), Shader.TileMode.CLAMP)
                canvas.drawArc(sec, i * 45f - 90f, 45f, true, paint)
                paint.shader = null
                if (i == winIndex) {
                    paint.color = Color.argb(80, 255, 255, 255)
                    canvas.drawArc(sec, i * 45f - 90f, 45f, true, paint)
                }
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dpf(2f)
                paint.color = Color.argb(200, 255, 233, 168)
                canvas.drawArc(sec, i * 45f - 90f, 45f, true, paint)

                // content (upright toward the outside)
                val mid = i * 45f - 67.5f
                val r = sr * 0.66f
                val ex = cx + Math.cos(Math.toRadians(mid.toDouble())).toFloat() * r
                val ey = cy + Math.sin(Math.toRadians(mid.toDouble())).toFloat() * r
                canvas.save()
                canvas.rotate(mid + 90f, ex, ey)
                paint.style = Paint.Style.FILL
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.DEFAULT
                paint.textSize = dpf(30f)
                canvas.drawText(items[i].emoji, ex, ey + dpf(2f), paint)
                paint.color = Color.argb(170, 10, 15, 44)
                canvas.drawRoundRect(ex - dpf(21f), ey + dpf(10f), ex + dpf(21f), ey + dpf(29f), dpf(9f), dpf(9f), paint)
                paint.color = GOLD_LIGHT
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = dpf(12f)
                canvas.drawText("x${items[i].mult}", ex, ey + dpf(24f), paint)
                paint.typeface = Typeface.DEFAULT
                canvas.restore()
            }

            // hub
            paint.style = Paint.Style.FILL
            paint.color = 0xFF14103A.toInt()
            canvas.drawCircle(cx, cy, dpf(58f), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpf(2f)
            paint.color = GOLD_DARK
            canvas.drawCircle(cx, cy, dpf(58f), paint)
        }
    }

    /** Static centre disc with circular countdown progress. */
    private inner class CenterView : View(this@GamesActivity) {
        private var caption = ""
        private var value = ""
        private var valueSize = 28f
        private var progress = 1f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arc = RectF()

        fun set(caption: String, value: String, valueSize: Float, progress: Float) {
            this.caption = caption
            this.value = value
            this.valueSize = valueSize
            this.progress = progress
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) / 2f - dpf(2f)
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), GOLD_LIGHT, GOLD_DARK, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            paint.color = 0xFF0C1030.toInt()
            canvas.drawCircle(cx, cy, r - dpf(7f), paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpf(5f)
            paint.strokeCap = Paint.Cap.ROUND
            arc.set(cx - r + dpf(14f), cy - r + dpf(14f), cx + r - dpf(14f), cy + r - dpf(14f))
            paint.color = Color.argb(50, 255, 255, 255)
            canvas.drawArc(arc, 0f, 360f, false, paint)
            paint.color = GOLD
            canvas.drawArc(arc, -90f, 360f * progress, false, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.rgb(170, 180, 215)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = dpf(11f)
            canvas.drawText(caption, cx, cy - dpf(10f), paint)
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = dpf(valueSize)
            canvas.drawText(value, cx, cy + dpf(valueSize * 0.55f) - dpf(4f), paint)
        }
    }

    /** Gold pointer at the top of the wheel. */
    private inner class PointerView : View(this@GamesActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            path.reset()
            path.moveTo(w / 2f, h)
            path.lineTo(dpf(3f), dpf(10f))
            path.lineTo(w - dpf(3f), dpf(10f))
            path.close()
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(0f, 0f, w, h, GOLD_LIGHT, GOLD_DARK, Shader.TileMode.CLAMP)
            canvas.drawPath(path, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpf(2f)
            paint.color = 0xFF0C1030.toInt()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFFE8505B.toInt()
            canvas.drawCircle(w / 2f, dpf(15f), dpf(5f), paint)
        }
    }

    /** Gold betting coin. */
    private inner class CoinView(val value: Long) : View(this@GamesActivity) {
        var active = false
            set(v) {
                field = v
                scaleX = if (v) 1.12f else 1f
                scaleY = scaleX
                invalidate()
            }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) / 2f - dpf(5f)
            paint.style = Paint.Style.FILL
            if (active) {
                paint.color = Color.argb(110, 255, 233, 168)
                canvas.drawCircle(cx, cy, r + dpf(4f), paint)
            }
            paint.shader = RadialGradient(cx - r * .3f, cy - r * .3f, r * 1.5f,
                intArrayOf(GOLD_LIGHT, GOLD, GOLD_DARK), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpf(3f)
            paint.color = Color.argb(210, 255, 255, 255)
            paint.pathEffect = DashPathEffect(floatArrayOf(dpf(5f), dpf(5f)), 0f)
            canvas.drawCircle(cx, cy, r - dpf(3f), paint)
            paint.pathEffect = null
            paint.strokeWidth = dpf(1.5f)
            paint.color = GOLD_DARK
            canvas.drawCircle(cx, cy, r - dpf(9f), paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = 0xFF4A3000.toInt()
            paint.textSize = if (value >= 1000) dpf(12f) else dpf(15f)
            canvas.drawText(value.toString(), cx, cy - (paint.ascent() + paint.descent()) / 2f, paint)
        }
    }
}