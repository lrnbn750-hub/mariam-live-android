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
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
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

/* Header backdrop — vertical gradient so it blends with a solid status bar */
private class SetupHeaderBg(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val d = resources.displayMetrics.density
    private val colors = intArrayOf(Color.parseColor("#22093F"), Color.parseColor("#5B21B6"), Color.parseColor("#B02FC9"))

    private fun star(c: Canvas, x: Float, y: Float, r: Float) {
        path.reset()
        path.moveTo(x, y - r); path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r); path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        c.drawPath(path, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, 0f, h, colors, floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = Color.argb(34, 255, 255, 255)
        c.drawCircle(w * 0.96f, h * 0.16f, h * 0.30f, p)
        p.color = Color.argb(24, 255, 190, 255)
        c.drawCircle(w * 0.02f, h * 0.66f, h * 0.27f, p)
        p.color = Color.argb(210, 255, 255, 255)
        star(c, w * 0.13f, h * 0.20f, 6f * d)
        star(c, w * 0.86f, h * 0.42f, 4.5f * d)
        star(c, w * 0.26f, h * 0.55f, 3.5f * d)
        p.color = Color.argb(160, 255, 224, 140)
        star(c, w * 0.70f, h * 0.15f, 5f * d)
        star(c, w * 0.93f, h * 0.66f, 3.5f * d)
    }
}

/* Rotating gradient ring around the avatar */
private class SetupRingView(ctx: Context, private val c1: Int, private val c2: Int, private val c3: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val cx = w / 2f
        val sw = 4f * resources.displayMetrics.density
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
    private var profileAvatarUrl = ""
    private var profileAvatar: Bitmap? = null
    private var selectedCapacity = 8

    // Palette
    private val ink = Color.parseColor("#17112B")
    private val muted = Color.parseColor("#6B6485")
    private val hairline = Color.parseColor("#ECE8F6")
    private val surface = Color.parseColor("#F8F6FD")
    private val brand = Color.parseColor("#7C3AED")
    private val pink = Color.parseColor("#C026D3")
    private val brandSoft = Color.parseColor("#F3ECFF")
    private val pinkSoft = Color.parseColor("#FCE8F8")
    private val gold1 = Color.parseColor("#FFD76A")
    private val gold2 = Color.parseColor("#F5A623")
    private val goldInk = Color.parseColor("#4A2E00")
    private val liveRed1 = Color.parseColor("#FF4D7D")
    private val liveRed2 = Color.parseColor("#F0143F")
    private val statusBarColor = Color.parseColor("#22093F")

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

    private fun goldButton(s: String, kind: String, click: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        minimumHeight = dp(56)
        isClickable = true
        background = ripple(grad(gold1, gold2, 18), 18, Color.argb(70, 255, 255, 255))
        elevation = dpf(4)
        setOnClickListener { click() }
        addView(icon(kind, goldInk), sz(22))
        addView(label(s, 16.5f, goldInk, 2), lp(s = 8))
    }

    private fun section(parent: LinearLayout, title: String, subtitle: String = "", top: Int = 26) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            View(this).apply { background = grad(brand, pink, 3, GradientDrawable.Orientation.TOP_BOTTOM) },
            LinearLayout.LayoutParams(dp(4), dp(18))
        )
        row.addView(label(title, 17f, ink, 2), lp(s = 8))
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
        previewSub.text = "بث صوتي مباشر  •  $selectedCapacity مقاعد"
    }

    /* ─────────── seat selector ─────────── */

    private inner class SeatCard(val count: Int, val box: LinearLayout, val dots: SetupSeatDots, val num: TextView, val cap: TextView)

    private fun seatCard(count: Int, onPick: () -> Unit): SeatCard {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(14), dp(8), dp(14))
            isClickable = true
        }
        val dots = SetupSeatDots(this, count)
        box.addView(dots, lp(dp(46), dp(30)))
        val num = label("$count", 20f, ink, 2).apply { gravity = Gravity.CENTER }
        val cap = label("مقاعد", 11.5f, muted, 1).apply { gravity = Gravity.CENTER }
        box.addView(num, lp(t = 8))
        box.addView(cap, lp(t = 1))
        box.setOnClickListener { selectedCapacity = count; onPick() }
        return SeatCard(count, box, dots, num, cap)
    }

    /* ─────────── lifecycle ─────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = statusBarColor
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val bg = SetupHeaderBg(this)
        root.addView(bg, FrameLayout.LayoutParams(-1, dp(420)))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ── Top bar
        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        topBar.addView(
            FrameLayout(this).apply {
                background = ripple(rect(Color.argb(45, 255, 255, 255), Color.argb(70, 255, 255, 255), 14), 14, Color.argb(60, 255, 255, 255))
                isClickable = true
                setOnClickListener { finish() }
                addView(icon("close", Color.WHITE), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
            },
            sz(42)
        )
        topBar.addView(label("إعداد البث المباشر", 18f, Color.WHITE, 2).apply { gravity = Gravity.CENTER }, lp(0, -2, wt = 1f))
        topBar.addView(
            label("BAK", 11f, goldInk, 2).apply {
                setPadding(dp(11), dp(6), dp(11), dp(6))
                background = grad(gold1, gold2, 10)
                letterSpacing = 0.08f
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        column.addView(topBar)

        // ── Scroll
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ── Hero: live room card preview
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(4), dp(24), dp(56))
        }
        val avatarWrap = FrameLayout(this)
        val glow = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(70, 255, 255, 255)) }
        }
        avatarWrap.addView(glow, FrameLayout.LayoutParams(dp(128), dp(128), Gravity.CENTER))
        val ring = SetupRingView(this, gold1, pink, brand)
        avatarWrap.addView(ring, FrameLayout.LayoutParams(dp(132), dp(132), Gravity.CENTER))
        avatarPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(surface) }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            setImageBitmap(placeholderBitmap())
        }
        avatarWrap.addView(avatarPreview, FrameLayout.LayoutParams(dp(110), dp(110), Gravity.CENTER))

        val live = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(11), dp(4))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(liveRed1, liveRed2)).apply {
                cornerRadius = dpf(12)
                setStroke(dp(2), Color.WHITE)
            }
            elevation = dpf(4)
        }
        live.addView(
            View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) } },
            sz(6)
        )
        live.addView(label("LIVE", 10.5f, Color.WHITE, 2).apply { letterSpacing = 0.08f }, lp(s = 5))
        avatarWrap.addView(live, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(0) })
        top.addView(avatarWrap, LinearLayout.LayoutParams(dp(140), dp(140)))

        previewName = label("اسم غرفتك هنا", 22f, Color.WHITE, 2).apply { gravity = Gravity.CENTER }
        top.addView(previewName, lp(t = 14))
        val subRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        subRow.addView(icon("mic", gold1), sz(15))
        previewSub = label("", 13f, Color.argb(220, 255, 255, 255), 1)
        subRow.addView(previewSub, lp(s = 6))
        top.addView(subRow, lp(t = 6))
        content.addView(top, lp())

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

        // ── White sheet
        val r = dpf(30)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        content.addView(body, lp(-1, -2, t = -30, wt = 1f))

        // Room name
        section(body, "اسم الغرفة", "سيظهر في بطاقة البث بالصفحة الرئيسية", top = 20)
        val nameBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(56)
            background = rect(surface, hairline, 16)
        }
        nameBox.addView(icon("edit", muted), sz(20))
        titleInput = EditText(this).apply {
            setText(intent.getStringExtra("room_name").orEmpty())
            hint = "مثلاً: سهرة الأصدقاء"
            textSize = 15.5f
            setTextColor(ink)
            setHintTextColor(Color.parseColor("#A29BB8"))
            setSingleLine(true)
            background = null
            setPadding(dp(10), dp(14), dp(10), dp(14))
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            onFocusChangeListener = View.OnFocusChangeListener { _, f ->
                nameBox.background = if (f) rect(Color.WHITE, brand, 16, 2) else rect(surface, hairline, 16)
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { updatePreview() }
            })
        }
        nameBox.addView(titleInput, lp(0, -2, wt = 1f))
        body.addView(nameBox, lp(t = 14))

        // Seats
        section(body, "عدد مقاعد المتحدثين", "اختر عدد من يمكنهم التحدث معك على المنصة")
        val seatRow = LinearLayout(this)
        val cards = mutableListOf<SeatCard>()
        fun paintSeats() {
            cards.forEach { c ->
                val on = c.count == selectedCapacity
                c.box.background =
                    if (on) grad(brand, pink, 18, GradientDrawable.Orientation.TL_BR)
                    else rect(Color.WHITE, hairline, 18, 1)
                c.box.elevation = if (on) dpf(5) else 0f
                c.dots.color = if (on) Color.WHITE else brand
                c.num.setTextColor(if (on) Color.WHITE else ink)
                c.cap.setTextColor(if (on) Color.argb(220, 255, 255, 255) else muted)
            }
            updatePreview()
        }
        listOf(4, 6, 8).forEachIndexed { i, count ->
            val c = seatCard(count) { paintSeats() }
            cards.add(c)
            seatRow.addView(c.box, lp(0, -2, s = if (i == 0) 0 else 5, e = if (i == 2) 0 else 5, wt = 1f))
        }
        paintSeats()
        body.addView(seatRow, lp(t = 14))

        // Info tip
        val tip = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#F4ECFF"), Color.parseColor("#FDEEFB"))
            ).apply { cornerRadius = dpf(20); setStroke(dp(1), Color.parseColor("#E4D6FA")) }
        }
        tip.addView(badge("eye", Color.WHITE, brand, 22, 14), sz(44))
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
            background = rect(Color.WHITE, hairline, 18)
            elevation = dpf(2)
        }
        prof.addView(badge("users", brand, brandSoft, 22, 14), sz(42))
        val profText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        profText.addView(label("صورة الغرفة", 14.5f, ink, 2))
        profText.addView(label("تُستخدم صورة ملفك الشخصي تلقائياً", 12f, muted), lp(t = 3))
        prof.addView(profText, lp(0, -2, s = 12, wt = 1f))
        prof.addView(
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(12), dp(6))
                background = rect(Color.parseColor("#E6F7F0"), r = 20)
                addView(icon("check", Color.parseColor("#059669")), sz(14))
                addView(label("تلقائي", 11.5f, Color.parseColor("#059669"), 2), lp(s = 4))
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        body.addView(prof, lp(t = 12))

        body.addView(
            label("الخلفية اختيارية بالكامل، وسيتم استخدام تصميم الغرفة الافتراضي.", 11.5f, Color.parseColor("#A29BB8"), 1)
                .apply { gravity = Gravity.CENTER },
            lp(t = 20)
        )

        // ── Fixed bottom action bar
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(Color.WHITE)
            elevation = dpf(10)
        }
        startButton = goldButton("ابدأ البث المباشر", "broadcast") { createAndStart(token) }
        bottomBar.addView(startButton, lp())
        column.addView(bottomBar, lp())
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        // Edge-to-edge safety (Android 15+)
        root.setOnApplyWindowInsetsListener { _, ins ->
            topBar.setPadding(dp(16), ins.systemWindowInsetTop + dp(8), dp(16), dp(8))
            (bg.layoutParams as ViewGroup.LayoutParams).height = dp(420) + ins.systemWindowInsetTop
            bg.requestLayout()
            column.setPadding(0, 0, 0, ins.systemWindowInsetBottom)
            ins
        }

        setContentView(root)
        updatePreview()
        loadProfileAvatar(token)
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