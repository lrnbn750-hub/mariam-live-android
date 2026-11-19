package com.mariam.live

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

/* ═════════════════════════ Vector icons ═════════════════════════ */

private class SetupIconView(ctx: Context, private val kind: String, private val tint: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.9f
    }

    private fun dot(c: Canvas, x: Float, y: Float, r: Float = 1.9f) {
        p.style = Paint.Style.FILL
        c.drawCircle(x, y, r, p)
        p.style = Paint.Style.STROKE
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        c.scale(s / 24f, s / 24f)
        p.color = tint
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.9f
        when (kind) {
            "close" -> { c.drawLine(6f, 6f, 18f, 18f, p); c.drawLine(18f, 6f, 6f, 18f, p) }
            "check" -> c.drawPath(Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, p)
            "edit" -> {
                c.drawPath(Path().apply {
                    moveTo(4f, 20f); lineTo(8f, 19f); lineTo(19.5f, 7.5f); lineTo(16.5f, 4.5f); lineTo(5f, 16f); close()
                }, p)
                c.drawLine(14f, 6.5f, 17.5f, 10f, p)
            }
            "mic" -> {
                c.drawRoundRect(RectF(9f, 3f, 15f, 14f), 3f, 3f, p)
                c.drawPath(Path().apply { moveTo(5.5f, 11f); cubicTo(5.5f, 17.5f, 18.5f, 17.5f, 18.5f, 11f) }, p)
                c.drawLine(12f, 17.2f, 12f, 21f, p)
                c.drawLine(8.5f, 21f, 15.5f, 21f, p)
            }
            "eye" -> {
                c.drawPath(Path().apply {
                    moveTo(2f, 12f); cubicTo(5f, 6.5f, 19f, 6.5f, 22f, 12f); cubicTo(19f, 17.5f, 5f, 17.5f, 2f, 12f)
                }, p)
                c.drawCircle(12f, 12f, 3f, p)
            }
            "users" -> {
                c.drawCircle(9f, 8f, 3.5f, p)
                c.drawPath(Path().apply {
                    moveTo(2.5f, 20f); cubicTo(2.5f, 15.5f, 5.5f, 14f, 9f, 14f); cubicTo(12.5f, 14f, 15.5f, 15.5f, 15.5f, 20f)
                }, p)
                c.drawCircle(17f, 8.5f, 2.6f, p)
                c.drawPath(Path().apply { moveTo(17f, 13.8f); cubicTo(19.8f, 14f, 21.5f, 15.6f, 21.5f, 19.5f) }, p)
            }
            "broadcast" -> {
                dot(c, 12f, 12f, 2.1f)
                c.drawArc(RectF(7.5f, 7.5f, 16.5f, 16.5f), -45f, 90f, false, p)
                c.drawArc(RectF(7.5f, 7.5f, 16.5f, 16.5f), 135f, 90f, false, p)
                c.drawArc(RectF(3.5f, 3.5f, 20.5f, 20.5f), -45f, 90f, false, p)
                c.drawArc(RectF(3.5f, 3.5f, 20.5f, 20.5f), 135f, 90f, false, p)
            }
            "shield" -> {
                c.drawPath(Path().apply {
                    moveTo(12f, 3f); lineTo(19.5f, 6f); lineTo(19.5f, 12f)
                    cubicTo(19.5f, 16.5f, 16.5f, 19.5f, 12f, 21f)
                    cubicTo(7.5f, 19.5f, 4.5f, 16.5f, 4.5f, 12f)
                    lineTo(4.5f, 6f); close()
                }, p)
                c.drawPath(Path().apply { moveTo(8.5f, 12f); lineTo(11f, 14.5f); lineTo(15.5f, 9.5f) }, p)
            }
        }
        c.restore()
    }
}

/* Full-screen backdrop — deep violet gradient, soft colour glows and a few sparkles */
private class SetupBackdrop(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val d = resources.displayMetrics.density
    private val colors = intArrayOf(Color.parseColor("#0F0722"), Color.parseColor("#1C0B40"), Color.parseColor("#0D0720"))

    private fun star(c: Canvas, x: Float, y: Float, r: Float) {
        path.reset()
        path.moveTo(x, y - r); path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r); path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        c.drawPath(path, p)
    }

    private fun glow(c: Canvas, cx: Float, cy: Float, r: Float, r8: Int, g8: Int, b8: Int, a: Int) {
        p.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.argb(a, r8, g8, b8), Color.argb(0, r8, g8, b8)),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        p.shader = LinearGradient(0f, 0f, 0f, h, colors, floatArrayOf(0f, 0.38f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        glow(c, w * 0.92f, h * 0.10f, w * 0.95f, 124, 58, 237, 120)
        glow(c, w * 0.02f, h * 0.38f, w * 0.80f, 192, 38, 211, 62)
        glow(c, w * 0.80f, h * 0.88f, w * 0.70f, 245, 166, 35, 22)
        p.color = Color.argb(190, 255, 255, 255)
        star(c, w * 0.12f, h * 0.075f, 5.5f * d)
        star(c, w * 0.88f, h * 0.20f, 4f * d)
        star(c, w * 0.20f, h * 0.30f, 3f * d)
        p.color = Color.argb(150, 255, 224, 140)
        star(c, w * 0.66f, h * 0.06f, 4.5f * d)
        star(c, w * 0.94f, h * 0.36f, 3f * d)
    }
}

/* Rotating gradient ring around the avatar */
private class SetupRingView(ctx: Context, private val c1: Int, private val c2: Int, private val c3: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val cx = w / 2f
        val sw = 3.5f * resources.displayMetrics.density
        p.strokeWidth = sw
        p.shader = SweepGradient(cx, cx, intArrayOf(c1, c2, c3, c1), null)
        c.drawCircle(cx, cx, cx - sw, p)
    }
}

/* Mini seat layout preview (rows of dots) */
private class SetupSeatDots(ctx: Context, private val count: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var color = Color.BLACK
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val rows = if (count <= 4) 1 else 2
        val per = (count + rows - 1) / rows
        val w = width.toFloat()
        val h = height.toFloat()
        val cellW = w / per
        val cellH = h / rows
        val rad = minOf(cellW, cellH) * 0.34f
        p.color = color
        for (row in 0 until rows) {
            val inRow = if (row == rows - 1) count - per * (rows - 1) else per
            val startX = (w - inRow * cellW) / 2f
            for (k in 0 until inRow) {
                c.drawCircle(startX + cellW * (k + 0.5f), cellH * (row + 0.5f), rad, p)
            }
        }
    }
}

/* Live stage preview — the numbered speaker seats; new seats pop in when the count changes */
private class SetupStageView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * d }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private var count = 8
    private var from = 8
    private var pop = 1f
    private var anim: ValueAnimator? = null

    fun setSeats(n: Int) {
        if (n == count) return
        from = count
        count = n
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 460
            interpolator = OvershootInterpolator(2.2f)
            addUpdateListener { pop = it.animatedValue as Float; invalidate() }
            start()
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val rows = if (count <= 4) 1 else 2
        val per = (count + rows - 1) / rows
        val cellW = w / 4f
        val cellH = h / 2f
        val r = minOf(cellW * 0.36f, 23f * d)
        for (i in 0 until count) {
            val row = i / per
            val inRow = if (row == rows - 1) count - per * (rows - 1) else per
            val k = i - row * per
            val startX = (w - inRow * cellW) / 2f
            val cx = startX + cellW * (k + 0.5f)
            val cy = if (rows == 1) h / 2f else cellH * (row + 0.5f)
            val sc = if (i >= from) pop else 0.88f + 0.12f * pop
            val rr = r * sc.coerceAtLeast(0f)
            glowPaint.color = Color.argb(34, 217, 70, 239)
            c.drawCircle(cx, cy, rr * 1.16f, glowPaint)
            fill.color = Color.argb(30, 255, 255, 255)
            c.drawCircle(cx, cy, rr, fill)
            line.color = Color.argb(70, 255, 255, 255)
            c.drawCircle(cx, cy, rr, line)
            txt.color = Color.argb(170, 255, 255, 255)
            txt.textSize = 13f * d * sc.coerceAtLeast(0f)
            c.drawText("${i + 1}", cx, cy - (txt.ascent() + txt.descent()) / 2f, txt)
        }
    }
}

/* Primary action container with a slow light sweep across it */
private class SetupShineLayout(ctx: Context, private val radiusPx: Float) : LinearLayout(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private var t = -1f
    private var anim: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                t = if (f < 0.42f) f / 0.42f else -1f
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (t < 0f || !isEnabled) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w, h), radiusPx, radiusPx, Path.Direction.CW)
        val band = w * 0.34f
        val x = -band + (w + band * 2f) * t
        canvas.save()
        canvas.clipPath(clip)
        p.shader = LinearGradient(
            x, 0f, x + band, h * 0.5f,
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, p)
        p.shader = null
        canvas.restore()
    }
}

/* ═══════════════════════════════════════ Screen ═══════════════════════════════════════ */

@Suppress("DEPRECATION")
class RoomSetupActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val animators = mutableListOf<Animator>()

    private lateinit var titleInput: EditText
    private lateinit var avatarPreview: ImageView
    private lateinit var startButton: LinearLayout
    private lateinit var previewName: TextView
    private lateinit var previewSub: TextView
    private lateinit var stage: SetupStageView
    private lateinit var nameCheck: View
    private var nameCheckShown = false
    private var profileAvatarUrl = ""
    private var profileAvatar: Bitmap? = null
    private var selectedCapacity = 8
    private var selectedBroadcastMode = "audio"

    // Palette
    private val ink = Color.parseColor("#F6F2FF")
    private val muted = Color.parseColor("#A99FCB")
    private val hairline = Color.argb(44, 255, 255, 255)
    private val surface = Color.argb(18, 255, 255, 255)
    private val brand = Color.parseColor("#7C3AED")
    private val brandLight = Color.parseColor("#B79CFF")
    private val pink = Color.parseColor("#C026D3")
    private val brandSoft = Color.argb(56, 139, 92, 246)
    private val pinkSoft = Color.argb(48, 217, 70, 239)
    private val hintColor = Color.parseColor("#7F75A3")
    private val gold1 = Color.parseColor("#FFD76A")
    private val gold2 = Color.parseColor("#F5A623")
    private val goldInk = Color.parseColor("#4A2E00")
    private val liveRed1 = Color.parseColor("#FF4D7D")
    private val liveRed2 = Color.parseColor("#F0143F")
    private val statusBarColor = Color.parseColor("#0F0722")
    private val barColor = Color.parseColor("#0D0720")

    /* ─────────── helpers ─────────── */

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Int) = v * resources.displayMetrics.density

    private fun rect(fill: Int, stroke: Int = Color.TRANSPARENT, r: Int = 16, sw: Int = 1) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dpf(r)
            if (stroke != Color.TRANSPARENT) setStroke(dp(sw), stroke)
        }

    private fun grad(c1: Int, c2: Int, r: Int, o: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT) =
        GradientDrawable(o, intArrayOf(c1, c2)).apply { cornerRadius = dpf(r) }

    private fun ripple(content: Drawable, r: Int, color: Int) =
        RippleDrawable(ColorStateList.valueOf(color), content, rect(Color.WHITE, r = r))

    private fun lp(w: Int = -1, h: Int = -2, t: Int = 0, b: Int = 0, s: Int = 0, e: Int = 0, wt: Float = 0f) =
        LinearLayout.LayoutParams(w, h, wt).apply {
            topMargin = dp(t); bottomMargin = dp(b); marginStart = dp(s); marginEnd = dp(e)
        }

    private fun sz(n: Int) = LinearLayout.LayoutParams(dp(n), dp(n))

    /** w: 0 = regular, 1 = medium, 2 = bold */
    private fun label(s: CharSequence, size: Float, color: Int, w: Int = 0) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        typeface = when (w) {
            2 -> Typeface.create("sans-serif", Typeface.BOLD)
            1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.create("sans-serif", Typeface.NORMAL)
        }
        setLineSpacing(0f, 1.2f)
    }

    private fun icon(kind: String, tint: Int) = SetupIconView(this, kind, tint)

    private fun badge(kind: String, tint: Int, soft: Int, ic: Int = 22, r: Int = 14) = FrameLayout(this).apply {
        background = rect(soft, r = r)
        addView(icon(kind, tint), FrameLayout.LayoutParams(dp(ic), dp(ic), Gravity.CENTER))
    }

    /** Soft press feedback: shrinks on touch, springs back on release. Does not consume the touch. */
    private fun View.pressable(scale: Float = 0.96f) {
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(scale).scaleY(scale).setDuration(110)
                        .setInterpolator(DecelerateInterpolator()).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(240)
                        .setInterpolator(OvershootInterpolator(2.4f)).start()
            }
            false
        }
    }

    private fun bounce(v: View) {
        v.animate().cancel()
        v.scaleX = 0.92f
        v.scaleY = 0.92f
        v.animate().scaleX(1f).scaleY(1f).setDuration(420)
            .setInterpolator(OvershootInterpolator(3f)).start()
    }

    private fun goldButton(s: String, kind: String, click: () -> Unit): LinearLayout {
        val r = 20
        val face = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FFE9A6"), gold1, gold2)
        ).apply { cornerRadius = dpf(r) }
        return SetupShineLayout(this, dpf(r)).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(60)
            isClickable = true
            background = ripple(face, r, Color.argb(70, 255, 255, 255))
            elevation = dpf(8)
            if (Build.VERSION.SDK_INT >= 28) {
                outlineSpotShadowColor = gold2
                outlineAmbientShadowColor = gold2
            }
            pressable(0.97f)
            setOnClickListener { click() }
            addView(icon(kind, goldInk), sz(24))
            addView(label(s, 17f, goldInk, 2), lp(-2, -2, s = 10))
        }
    }

    private fun section(parent: LinearLayout, title: String, subtitle: String = "", top: Int = 26) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            View(this).apply { background = grad(brandLight, pink, 3, GradientDrawable.Orientation.TOP_BOTTOM) },
            LinearLayout.LayoutParams(dp(4), dp(18))
        )
        row.addView(label(title, 16.5f, ink, 2), lp(-2, -2, s = 8))
        parent.addView(row, lp(t = top))
        if (subtitle.isNotBlank()) parent.addView(label(subtitle, 12.5f, muted), lp(t = 4))
    }

    private fun placeholderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 512f, 512f,
            intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#C026D3")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        paint.shader = null
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 190f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("B", 256f, 325f, paint)
        return bitmap
    }

    private fun setStart(enabled: Boolean, text: String) {
        startButton.isEnabled = enabled
        startButton.alpha = if (enabled) 1f else 0.6f
        (startButton.getChildAt(1) as TextView).text = text
    }

    private fun updatePreview() {
        val name = titleInput.text.toString().trim()
        if (name.isEmpty()) {
            previewName.text = "اسم غرفتك هنا"
            previewName.alpha = 0.55f
        } else {
            previewName.text = name
            previewName.alpha = 1f
        }
        previewSub.text = when(selectedBroadcastMode) { "camera4" -> "بث كاميرا مباشر • 4 مقاعد"; "camera6" -> "بث كاميرا مباشر • 6 مقاعد"; else -> "بث صوتي مباشر • 8 مقاعد" }
        if (::stage.isInitialized) stage.setSeats(selectedCapacity)
        if (::nameCheck.isInitialized) {
            val ok = name.length >= 2
            if (ok != nameCheckShown) {
                nameCheckShown = ok
                nameCheck.animate().cancel()
                nameCheck.animate()
                    .alpha(if (ok) 1f else 0f)
                    .scaleX(if (ok) 1f else 0.5f)
                    .scaleY(if (ok) 1f else 0.5f)
                    .setDuration(260)
                    .setInterpolator(if (ok) OvershootInterpolator(2.6f) else DecelerateInterpolator())
                    .start()
            }
        }
    }

    /* ─────────── seat selector ─────────── */

    private inner class SeatCard(val count: Int, val box: LinearLayout, val dots: SetupSeatDots, val num: TextView, val cap: TextView)

    private fun seatCard(count: Int, onPick: () -> Unit): SeatCard {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(16), dp(8), dp(14))
            isClickable = true
            if (Build.VERSION.SDK_INT >= 28) {
                outlineSpotShadowColor = pink
                outlineAmbientShadowColor = pink
            }
            pressable(0.95f)
        }
        val dots = SetupSeatDots(this, count)
        box.addView(dots, lp(dp(50), dp(32)))
        val num = label("$count", 22f, ink, 2).apply { gravity = Gravity.CENTER }
        val cap = label("مقاعد", 11.5f, muted, 1).apply { gravity = Gravity.CENTER }
        box.addView(num, lp(t = 8))
        box.addView(cap, lp(t = 1))
        box.setOnClickListener { selectedCapacity = count; onPick(); bounce(box) }
        return SeatCard(count, box, dots, num, cap)
    }

    /* ─────────── lifecycle ─────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = statusBarColor
        window.navigationBarColor = barColor
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()

        val root = FrameLayout(this).apply {
            setBackgroundColor(barColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(SetupBackdrop(this), FrameLayout.LayoutParams(-1, -1))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ── Top bar
        val topBar = FrameLayout(this).apply { setPadding(dp(16), dp(6), dp(16), dp(6)) }
        topBar.addView(
            FrameLayout(this).apply {
                background = ripple(rect(Color.argb(30, 255, 255, 255), Color.argb(50, 255, 255, 255), 21), 21, Color.argb(60, 255, 255, 255))
                isClickable = true
                pressable(0.92f)
                setOnClickListener { finish() }
                addView(icon("close", Color.WHITE), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
            },
            FrameLayout.LayoutParams(dp(42), dp(42), Gravity.START or Gravity.CENTER_VERTICAL)
        )
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        titleCol.addView(label("إعداد البث المباشر", 17.5f, Color.WHITE, 2).apply { gravity = Gravity.CENTER })
        val titleSub = LinearLayout(this).apply { gravity = Gravity.CENTER }
        titleSub.addView(
            View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(liveRed1) } },
            sz(6)
        )
        titleSub.addView(label("جهّز غرفتك قبل البث", 11.5f, muted, 1), lp(-2, -2, s = 6))
        titleCol.addView(titleSub, lp(t = 2))
        topBar.addView(titleCol, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        topBar.addView(
            label("BAK", 11f, goldInk, 2).apply {
                setPadding(dp(11), dp(6), dp(11), dp(6))
                background = grad(gold1, gold2, 10)
                letterSpacing = 0.08f
            },
            FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL)
        )
        column.addView(topBar, LinearLayout.LayoutParams(-1, -2))

        // ── Scroll
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ── Hero: live room card preview (gradient hairline border + dark glass body)
        val heroOuter = FrameLayout(this).apply {
            setPadding(dp(1), dp(1), dp(1), dp(1))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(210, 255, 215, 106), Color.argb(150, 192, 38, 211), Color.argb(110, 124, 58, 237))
            ).apply { cornerRadius = dpf(30) }
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            setPadding(dp(18), dp(16), dp(18), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#2C1360"), Color.parseColor("#170A36"))
            ).apply { cornerRadius = dpf(29) }
        }
        heroOuter.addView(hero, FrameLayout.LayoutParams(-1, -2))

        val chip = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(12), dp(5))
            background = rect(Color.argb(30, 255, 255, 255), r = 20)
        }
        chip.addView(icon("eye", gold1), sz(14))
        chip.addView(label("هكذا ستظهر غرفتك", 11.5f, muted, 1), lp(-2, -2, s = 6))
        hero.addView(chip, LinearLayout.LayoutParams(-2, -2))

        val avatarWrap = FrameLayout(this).apply { clipChildren = false }
        val glow = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(70, 217, 70, 239)) }
        }
        avatarWrap.addView(glow, FrameLayout.LayoutParams(dp(116), dp(116), Gravity.CENTER))
        val ring = SetupRingView(this, gold1, pink, brand)
        avatarWrap.addView(ring, FrameLayout.LayoutParams(dp(124), dp(124), Gravity.CENTER))
        avatarPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#2C1360")) }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            setImageBitmap(placeholderBitmap())
        }
        avatarWrap.addView(avatarPreview, FrameLayout.LayoutParams(dp(102), dp(102), Gravity.CENTER))

        val live = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(11), dp(4))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(liveRed1, liveRed2)).apply {
                cornerRadius = dpf(12)
                setStroke(dp(2), Color.parseColor("#2C1360"))
            }
            elevation = dpf(4)
        }
        live.addView(
            View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) } },
            sz(6)
        )
        live.addView(label("LIVE", 10.5f, Color.WHITE, 2).apply { letterSpacing = 0.08f }, lp(-2, -2, s = 5))
        avatarWrap.addView(live, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        hero.addView(avatarWrap, lp(dp(140), dp(140), t = 6))

        previewName = label("اسم غرفتك هنا", 23f, Color.WHITE, 2).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        hero.addView(previewName, lp(t = 8, s = 8, e = 8))
        val subRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        subRow.addView(icon("mic", gold1), sz(15))
        previewSub = label("", 13f, Color.argb(225, 255, 255, 255), 1)
        subRow.addView(previewSub, lp(-2, -2, s = 6))
        hero.addView(subRow, lp(t = 6))

        hero.addView(View(this).apply { setBackgroundColor(Color.argb(30, 255, 255, 255)) }, lp(-1, dp(1), t = 16))
        stage = SetupStageView(this)
        hero.addView(stage, lp(-1, dp(104), t = 8))
        content.addView(heroOuter, lp(t = 6, s = 18, e = 18))

        // Animations: spinning ring + soft pulse
        animators.add(ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f).apply {
            duration = 7000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        })
        animators.add(ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1900
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val f = it.animatedValue as Float
                glow.scaleX = 1f + 0.2f * f
                glow.scaleY = 1f + 0.2f * f
                glow.alpha = 0.6f * (1f - f)
            }
            start()
        })

        // ── Settings body
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), dp(28))
        }
        content.addView(body, lp(-1, -2, wt = 1f))

        // Room name
        section(body, "اسم الغرفة", "سيظهر في بطاقة البث بالصفحة الرئيسية", top = 20)
        val nameBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(58)
            background = rect(surface, hairline, 18)
        }
        nameBox.addView(icon("edit", brandLight), sz(20))
        titleInput = EditText(this).apply {
            setText(intent.getStringExtra("room_name").orEmpty())
            hint = "مثلاً: سهرة الأصدقاء"
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(hintColor)
            highlightColor = Color.argb(90, 183, 156, 255)
            setSingleLine(true)
            background = null
            setPadding(dp(10), dp(14), dp(10), dp(14))
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= 29) {
                textCursorDrawable = GradientDrawable().apply { setColor(gold1); setSize(dp(2), -1) }
            }
            onFocusChangeListener = View.OnFocusChangeListener { _, f ->
                nameBox.background =
                    if (f) rect(Color.argb(34, 139, 92, 246), brandLight, 18, 2)
                    else rect(surface, hairline, 18)
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { updatePreview() }
            })
        }
        nameBox.addView(titleInput, lp(0, -2, wt = 1f))
        nameCheck = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(gold1, gold2)).apply {
                shape = GradientDrawable.OVAL
            }
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            addView(icon("check", goldInk), FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER))
        }
        nameBox.addView(nameCheck, sz(24))
        body.addView(nameBox, lp(t = 14))

        // Broadcast type selector: audio 8, camera 4, camera 6.
        val modeTitle = label("نوع البث", 15f, ink, 2)
        body.addView(modeTitle, lp(t = 18))
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        fun modeCard(title: String, subtitle: String, mode: String, capacity: Int): LinearLayout {
            val selected = selectedBroadcastMode == mode
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(7), dp(13), dp(7), dp(13))
                background = rect(if(selected) Color.argb(45, 245, 190, 88) else surface,
                    if(selected) gold1 else hairline, 16, if(selected) 2 else 1)
                addView(label(title, 12.5f, if(selected) gold1 else ink, 2).apply { gravity = Gravity.CENTER })
                addView(label(subtitle, 10.5f, muted, 1).apply { gravity = Gravity.CENTER }, lp(t = 4))
                setOnClickListener {
                    selectedBroadcastMode = mode
                    selectedCapacity = capacity
                    for(i in 0 until modeRow.childCount) {
                        val child = modeRow.getChildAt(i)
                        child.background = rect(surface, hairline, 16)
                        if(child is LinearLayout && child.childCount > 0 && child.getChildAt(0) is TextView)
                            (child.getChildAt(0) as TextView).setTextColor(ink)
                    }
                    background = rect(Color.argb(45, 245, 190, 88), gold1, 16, 2)
                    (getChildAt(0) as TextView).setTextColor(gold1)
                    updatePreview()
                }
            }
        }
        val modes = listOf(
            modeCard("صوتي", "8 مقاعد", "audio", 8),
            modeCard("كاميرا", "4 مقاعد", "camera4", 4),
            modeCard("كاميرا", "6 مقاعد", "camera6", 6)
        )
        modes.forEach { modeRow.addView(it, LinearLayout.LayoutParams(0, dp(76), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }) }
        body.addView(modeRow, lp())
        
        // Info tip
        val tip = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(46, 124, 58, 237), Color.argb(34, 192, 38, 211))
            ).apply { cornerRadius = dpf(20); setStroke(dp(1), Color.argb(64, 183, 156, 255)) }
        }
        tip.addView(badge("eye", gold1, brandSoft, 22, 14), sz(44))
        val tipText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tipText.addView(label("ماذا سيظهر للمستخدمين؟", 14f, ink, 2))
        tipText.addView(
            label("صورتك الشخصية واسم الغرفة في بطاقة البث بالصفحة الرئيسية. لا تحتاج إلى اختيار خلفية أو رفع صورة إضافية.", 12.5f, muted, 1),
            lp(t = 4)
        )
        tip.addView(tipText, lp(0, -2, s = 12, wt = 1f))
        body.addView(tip, lp(t = 24))

        // Profile image row
        val prof = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rect(surface, hairline, 18)
        }
        prof.addView(badge("users", brandLight, brandSoft, 22, 14), sz(42))
        val profText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        profText.addView(label("صورة الغرفة", 14.5f, ink, 2))
        profText.addView(label("تُستخدم صورة ملفك الشخصي تلقائياً", 12f, muted), lp(t = 3))
        prof.addView(profText, lp(0, -2, s = 12, wt = 1f))
        prof.addView(
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(12), dp(6))
                background = rect(Color.argb(44, 16, 185, 129), r = 20)
                addView(icon("check", Color.parseColor("#34D399")), sz(14))
                addView(label("تلقائي", 11.5f, Color.parseColor("#34D399"), 2), lp(-2, -2, s = 4))
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        body.addView(prof, lp(t = 12))

        body.addView(
            label("الخلفية اختيارية بالكامل، وسيتم استخدام تصميم الغرفة الافتراضي.", 11.5f, hintColor, 1)
                .apply { gravity = Gravity.CENTER },
            lp(t = 20)
        )

        // ── Fixed bottom action bar
        column.addView(View(this).apply { setBackgroundColor(Color.argb(26, 255, 255, 255)) }, LinearLayout.LayoutParams(-1, dp(1)))
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(barColor)
        }
        startButton = goldButton("ابدأ البث المباشر", "broadcast") { createAndStart(token) }
        bottomBar.addView(startButton, lp())
        column.addView(bottomBar, lp())
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        // Edge-to-edge safety (Android 15+)
        root.setOnApplyWindowInsetsListener { _, ins ->
            topBar.setPadding(dp(16), ins.systemWindowInsetTop + dp(6), dp(16), dp(6))
            column.setPadding(0, 0, 0, ins.systemWindowInsetBottom)
            ins
        }

        setContentView(root)
        updatePreview()
        loadProfileAvatar(token)

        // Entrance: hero first, then each settings block rises into place
        val reveal = mutableListOf<View>(heroOuter)
        for (i in 0 until body.childCount) reveal.add(body.getChildAt(i))
        reveal.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = dpf(18)
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(50L + minOf(i, 8) * 45L)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()
        }
    }

    /* ─────────── data ─────────── */

    private fun loadProfileAvatar(token: String) {
        if (token.isBlank()) return
        executor.execute {
            try {
                val response = AuthApi.get("/api/profile", token)
                if (response.status !in 200..299) return@execute
                val profile = response.body.optJSONObject("profile") ?: JSONObject()
                profileAvatarUrl = profile.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                if (profileAvatarUrl.isNotBlank()) {
                    val bitmap = URL(profileAvatarUrl).openConnection()
                        .apply { connectTimeout = 10000; readTimeout = 15000 }
                        .getInputStream().use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null) {
                        profileAvatar = bitmap
                        runOnUiThread { if (!isFinishing) avatarPreview.setImageBitmap(bitmap) }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun createAndStart(token: String) {
        val name = titleInput.text.toString().trim()
        if (name.length < 2) {
            Toast.makeText(this, "اكتب اسم الغرفة (حرفان على الأقل)", Toast.LENGTH_SHORT).show(); return
        }
        if (token.isBlank()) {
            Toast.makeText(this, "سجّل الدخول أولاً", Toast.LENGTH_SHORT).show(); finish(); return
        }
        setStart(false, "جارٍ تجهيز غرفتك...")
        executor.execute {
            try {
                val source = profileAvatar ?: placeholderBitmap()
                val maxSide = 1280
                val scale = minOf(1f, maxSide.toFloat() / maxOf(source.width, source.height))
                val resized = if (scale < 1f)
                    Bitmap.createScaledBitmap(
                        source,
                        (source.width * scale).toInt().coerceAtLeast(1),
                        (source.height * scale).toInt().coerceAtLeast(1),
                        true
                    ) else source
                val bytes = ByteArrayOutputStream()
                var quality = 85
                do {
                    bytes.reset(); resized.compress(Bitmap.CompressFormat.JPEG, quality, bytes); quality -= 10
                } while (bytes.size() > 3 * 1024 * 1024 && quality >= 45)
                if (bytes.size() > 3 * 1024 * 1024) throw Exception("تعذر تجهيز صورة الغرفة")
                val payload = JSONObject().put("name", name).put("max_members", selectedCapacity)
                    .put("room_mode", if(selectedBroadcastMode == "audio") "audio" else "camera")
                    .put("camera_seat_count", if(selectedBroadcastMode == "camera4") 4 else if(selectedBroadcastMode == "camera6") 6 else 0)
                    .put("image_mime", "image/jpeg")
                    .put("image_base64", android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP))
                if (resized !== source) resized.recycle()
                if (source !== profileAvatar && !source.isRecycled) source.recycle()
                val result = AuthApi.post("/api/voice-rooms", payload, token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إنشاء الغرفة"))
                val room = result.body.optJSONObject("room") ?: throw Exception("استجابة الخادم غير مكتملة")
                val roomId = room.optString("id")
                runOnUiThread {
                    startActivity(Intent(this, RoomActivity::class.java).putExtra("room_id", roomId).putExtra("room_name", name))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setStart(true, "ابدأ البث المباشر")
                    Toast.makeText(this, e.message ?: "تعذر إنشاء الغرفة", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        animators.forEach { it.cancel() }
        executor.shutdownNow()
        super.onDestroy()
    }
}