package com.mariam.live

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
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/* ═════════════════════════ Vector icons ═════════════════════════ */

private class AgencyIconView(ctx: Context, private val kind: String, private val tint: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.9f
    }

    private fun dot(c: Canvas, x: Float, y: Float, r: Float = 1.25f) {
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
            "back" -> c.drawPath(Path().apply { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }, p)
            "chevronDown" -> c.drawPath(Path().apply { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }, p)
            "check" -> c.drawPath(Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, p)
            "alert" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawLine(12f, 7.5f, 12f, 13f, p)
                dot(c, 12f, 16.5f)
            }
            "clock" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawPath(Path().apply { moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f) }, p)
            }
            "building" -> {
                c.drawRoundRect(RectF(5f, 3f, 19f, 21f), 2f, 2f, p)
                dot(c, 9f, 8f); dot(c, 15f, 8f); dot(c, 9f, 12f); dot(c, 15f, 12f)
                c.drawPath(Path().apply { moveTo(10f, 21f); lineTo(10f, 17f); lineTo(14f, 17f); lineTo(14f, 21f) }, p)
            }
            "user" -> {
                c.drawCircle(12f, 8f, 4f, p)
                c.drawPath(Path().apply {
                    moveTo(4f, 21f); cubicTo(4f, 16f, 8f, 14f, 12f, 14f); cubicTo(16f, 14f, 20f, 16f, 20f, 21f)
                }, p)
            }
            "globe" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawOval(RectF(8f, 3f, 16f, 21f), p)
                c.drawLine(3f, 12f, 21f, 12f, p)
            }
            "idcard" -> {
                c.drawRoundRect(RectF(2.5f, 5f, 21.5f, 19f), 3f, 3f, p)
                c.drawCircle(8.5f, 11f, 2f, p)
                c.drawPath(Path().apply { moveTo(5.5f, 16f); quadTo(8.5f, 12.8f, 11.5f, 16f) }, p)
                c.drawLine(14f, 10f, 18.5f, 10f, p)
                c.drawLine(14f, 13.5f, 18.5f, 13.5f, p)
            }
            "camera" -> {
                c.drawRoundRect(RectF(3f, 7f, 21f, 20f), 3f, 3f, p)
                c.drawPath(Path().apply { moveTo(8f, 7f); lineTo(9.5f, 4.5f); lineTo(14.5f, 4.5f); lineTo(16f, 7f) }, p)
                c.drawCircle(12f, 13.5f, 3.5f, p)
            }
            "lock" -> {
                c.drawRoundRect(RectF(5f, 11f, 19f, 21f), 3f, 3f, p)
                c.drawPath(Path().apply {
                    moveTo(8f, 11f); lineTo(8f, 8f); cubicTo(8f, 2.5f, 16f, 2.5f, 16f, 8f); lineTo(16f, 11f)
                }, p)
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
            "send" -> {
                c.drawPath(Path().apply { moveTo(21f, 3f); lineTo(3f, 10.5f); lineTo(10.5f, 13.5f); lineTo(13.5f, 21f); close() }, p)
            }
            "crownFill" -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.6f
                c.drawPath(Path().apply {
                    moveTo(3f, 8f); lineTo(7.5f, 12.5f); lineTo(12f, 5f); lineTo(16.5f, 12.5f); lineTo(21f, 8f)
                    lineTo(19f, 19f); lineTo(5f, 19f); close()
                }, p)
                p.style = Paint.Style.FILL
                p.color = Color.argb(150, 255, 255, 255)
                c.drawRoundRect(RectF(6f, 15.5f, 18f, 17.3f), 0.9f, 0.9f, p)
                p.color = tint
                dot(c, 3f, 7.5f, 1.7f); dot(c, 12f, 4.5f, 1.7f); dot(c, 21f, 7.5f, 1.7f)
            }
        }
        c.restore()
    }
}

/* Header backdrop — vertical gradient so it blends with a solid status bar */
private class AgencyHeaderBg(ctx: Context) : View(ctx) {
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
        c.drawCircle(w * 0.95f, h * 0.18f, h * 0.30f, p)
        p.color = Color.argb(24, 255, 190, 255)
        c.drawCircle(w * 0.02f, h * 0.62f, h * 0.27f, p)
        p.color = Color.argb(210, 255, 255, 255)
        star(c, w * 0.14f, h * 0.20f, 6f * d)
        star(c, w * 0.84f, h * 0.38f, 4.5f * d)
        star(c, w * 0.30f, h * 0.50f, 3.5f * d)
        p.color = Color.argb(160, 255, 224, 140)
        star(c, w * 0.66f, h * 0.16f, 5f * d)
        star(c, w * 0.92f, h * 0.60f, 3.5f * d)
    }
}

/* Animated progress bar (gradient fill) */
private class AgencyBarView(ctx: Context, private val trackC: Int, private val c1: Int, private val c2: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var fraction = 0f
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        p.shader = null
        p.color = trackC
        c.drawRoundRect(0f, 0f, w, h, r, r, p)
        val fw = w * fraction.coerceIn(0f, 1f)
        if (fw > 0f) {
            p.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, maxOf(fw, h), h, r, r, p)
            p.shader = null
        }
    }
}

/* ═══════════════════════════════════════ Screen ═══════════════════════════════════════ */

@Suppress("DEPRECATION")
class AgencyActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val pickFront = 8101
    private val pickBack = 8102
    private val takeSelfie = 8103
    private val cameraPermissionRequest = 8104
    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var selfieUri: Uri? = null
    private var cameraFile: File? = null

    private lateinit var countryCodes: List<String>
    private lateinit var agencyName: EditText
    private lateinit var legalName: EditText
    private lateinit var country: Spinner
    private lateinit var consent: CheckBox
    private lateinit var submit: LinearLayout
    private lateinit var statusBox: LinearLayout
    private lateinit var dashboardButton: TextView
    private lateinit var agencyRoot: View
    private lateinit var tileFront: Tile
    private lateinit var tileBack: Tile
    private lateinit var tileSelfie: Tile
    private lateinit var bar: AgencyBarView
    private lateinit var progressText: TextView
    private lateinit var progressHint: TextView

    // Palette
    private val ink = Color.parseColor("#17112B")
    private val muted = Color.parseColor("#6B6485")
    private val hairline = Color.parseColor("#ECE8F6")
    private val surface = Color.parseColor("#F8F6FD")
    private val brand = Color.parseColor("#7C3AED")
    private val pink = Color.parseColor("#C026D3")
    private val brandSoft = Color.parseColor("#F3ECFF")
    private val gold1 = Color.parseColor("#FFD76A")
    private val gold2 = Color.parseColor("#F5A623")
    private val goldInk = Color.parseColor("#4A2E00")
    private val green = Color.parseColor("#059669")
    private val greenSoft = Color.parseColor("#E6F7F0")
    private val danger = Color.parseColor("#E11D48")
    private val dangerSoft = Color.parseColor("#FFF1F2")
    private val amberText = Color.parseColor("#B45309")
    private val amberSoft = Color.parseColor("#FFF4E0")
    private val statusBarColor = Color.parseColor("#22093F")

    private val totalSteps = 6

    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

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

    private fun icon(kind: String, tint: Int) = AgencyIconView(this, kind, tint)

    private fun badge(kind: String, tint: Int, soft: Int, ic: Int = 22, r: Int = 14) = FrameLayout(this).apply {
        background = rect(soft, r = r)
        addView(icon(kind, tint), FrameLayout.LayoutParams(dp(ic), dp(ic), Gravity.CENTER))
    }

    private fun card(pad: Int = 16) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        background = rect(Color.WHITE, hairline, 20)
        elevation = dpf(2)
    }

    private fun goldButton(s: String, kind: String, click: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        minimumHeight = dp(56)
        isClickable = true
        background = ripple(grad(gold1, gold2, 18), 18, Color.argb(70, 255, 255, 255))
        elevation = dpf(4)
        setOnClickListener { click() }
        addView(icon(kind, goldInk), sz(20))
        addView(label(s, 16f, goldInk, 2), lp(s = 8))
    }

    private fun purpleButton(s: String, click: () -> Unit) = TextView(this).apply {
        text = s
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(Color.WHITE)
        minHeight = dp(54)
        isClickable = true
        background = ripple(grad(brand, pink, 16), 16, Color.argb(60, 255, 255, 255))
        setOnClickListener { click() }
    }

    private fun section(parent: LinearLayout, title: String, subtitle: String = "") {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            View(this).apply { background = grad(brand, pink, 3, GradientDrawable.Orientation.TOP_BOTTOM) },
            LinearLayout.LayoutParams(dp(4), dp(18))
        )
        row.addView(label(title, 17f, ink, 2), lp(s = 8))
        parent.addView(row, lp(t = 26))
        if (subtitle.isNotBlank()) parent.addView(label(subtitle, 12.5f, muted), lp(t = 4))
    }

    private fun fieldLabel(s: String) = label(s, 13f, muted, 1)

    private fun inputBox(hintText: String, iconKind: String): Pair<LinearLayout, EditText> {
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(54)
            background = rect(surface, hairline, 16)
        }
        box.addView(icon(iconKind, muted), sz(20))
        val et = EditText(this).apply {
            hint = hintText
            setHintTextColor(Color.parseColor("#A29BB8"))
            setTextColor(ink)
            textSize = 15f
            setSingleLine(true)
            background = null
            setPadding(dp(10), dp(14), dp(10), dp(14))
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            onFocusChangeListener = View.OnFocusChangeListener { _, f ->
                box.background = if (f) rect(Color.WHITE, brand, 16, 2) else rect(surface, hairline, 16)
            }
        }
        box.addView(et, lp(0, -2, wt = 1f))
        return box to et
    }

    private fun flag(code: String): String =
        if (code.length == 2)
            String(Character.toChars(0x1F1E6 + (code[0] - 'A'))) + String(Character.toChars(0x1F1E6 + (code[1] - 'A')))
        else ""

    private fun styledAdapter(items: List<String>) =
        object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
            private fun style(v: View, vPad: Int): View {
                (v as TextView).apply {
                    setTextColor(ink)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(vPad), dp(10), dp(vPad))
                }
                return v
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getView(position, convertView, parent), 0)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getDropDownView(position, convertView, parent), 12)
        }

    /* ─────────── upload tile ─────────── */

    private inner class Tile(
        val root: FrameLayout,
        val center: LinearLayout,
        val thumb: ImageView,
        val caption: TextView,
        val check: View
    )

    private fun tile(title: String, subtitle: String, kind: String, h: Int, click: () -> Unit): Tile {
        val root = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            minimumHeight = dp(h)
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dpf(18)
                setStroke(dp(2), Color.parseColor("#CDBBF5"), dpf(7), dpf(5))
            }
            setOnClickListener { click() }
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(14), dp(10), dp(14))
        }
        center.addView(badge(kind, brand, brandSoft, 24, 16), sz(46))
        center.addView(label(title, 13.5f, ink, 2).apply { gravity = Gravity.CENTER }, lp(t = 10))
        center.addView(label(subtitle, 11.5f, muted).apply { gravity = Gravity.CENTER }, lp(t = 3))
        root.addView(center, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))

        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            background = rect(surface, r = 18)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        root.addView(thumb, FrameLayout.LayoutParams(-1, -1))

        val caption = label("", 12f, Color.WHITE, 2).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(Color.argb(150, 20, 8, 40))
                val r = dpf(18)
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
        }
        root.addView(caption, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        val check = FrameLayout(this).apply {
            visibility = View.GONE
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(green) }
            elevation = dpf(3)
            addView(icon("check", Color.WHITE), FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER))
        }
        root.addView(check, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.TOP or Gravity.START).apply {
            setMargins(dp(8), dp(8), dp(8), dp(8))
        })
        return Tile(root, center, thumb, caption, check)
    }

    private fun mark(t: Tile, uri: Uri, text: String) {
        t.caption.text = text
        t.caption.visibility = View.VISIBLE
        t.check.visibility = View.VISIBLE
        t.root.background = rect(Color.WHITE, green, 18, 2)
        showThumb(uri, t)
        refreshProgress()
    }

    private fun showThumb(uri: Uri, t: Tile) {
        try {
            worker.execute {
                try {
                    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, b) }
                    var s = 1
                    while (b.outWidth / s > 700 || b.outHeight / s > 700) s *= 2
                    val bmp = contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
                    }
                    if (bmp != null) runOnUiThread {
                        t.thumb.setImageBitmap(bmp)
                        t.thumb.visibility = View.VISIBLE
                        t.center.visibility = View.GONE
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    /* ─────────── status banner / progress ─────────── */

    /** kind: 0 info, 1 success, 2 pending, 3 error */
    private fun showStatus(msg: String, kind: Int) {
        statusBox.removeAllViews()
        if (msg.isBlank()) {
            statusBox.visibility = View.GONE
            return
        }
        val (bgc, fg, ic) = when (kind) {
            1 -> Triple(greenSoft, green, "check")
            2 -> Triple(amberSoft, amberText, "clock")
            3 -> Triple(dangerSoft, danger, "alert")
            else -> Triple(brandSoft, brand, "shield")
        }
        statusBox.background = rect(bgc, r = 16)
        statusBox.addView(icon(ic, fg), sz(22))
        statusBox.addView(label(msg, 13.5f, fg, 1), lp(0, -2, s = 10, wt = 1f))
        statusBox.visibility = View.VISIBLE
    }

    private fun setSubmit(enabled: Boolean, text: String = "إرسال طلب الوكالة") {
        submit.isEnabled = enabled
        submit.alpha = if (enabled) 1f else 0.5f
        (submit.getChildAt(1) as TextView).text = text
    }

    private fun refreshProgress() {
        if (!::bar.isInitialized) return
        var n = 0
        if (agencyName.text.toString().trim().length >= 2) n++
        if (legalName.text.toString().trim().length >= 3) n++
        if (frontUri != null) n++
        if (backUri != null) n++
        if (selfieUri != null) n++
        if (consent.isChecked) n++
        progressText.text = "$n / $totalSteps"
        progressHint.text = if (n == totalSteps) "كل شيء جاهز — يمكنك إرسال الطلب الآن" else "أكمل البيانات والصور والموافقة لإرسال الطلب"
        val target = n / totalSteps.toFloat()
        ValueAnimator.ofFloat(bar.fraction, target).apply {
            duration = 350
            addUpdateListener { bar.fraction = it.animatedValue as Float }
            start()
        }
    }

    private fun watch(et: EditText) {
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { refreshProgress() }
        })
    }

    /* ─────────── lifecycle ─────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = statusBarColor
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            visibility = View.INVISIBLE
        }
        agencyRoot = root
        val bg = AgencyHeaderBg(this)
        root.addView(bg, FrameLayout.LayoutParams(-1, dp(360)))

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
                addView(icon("back", Color.WHITE), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            },
            sz(42)
        )
        topBar.addView(label("مركز الوكالة", 18f, Color.WHITE, 2).apply { gravity = Gravity.CENTER }, lp(0, -2, wt = 1f))
        topBar.addView(View(this), sz(42))
        column.addView(topBar)

        // ── Scroll: hero + white sheet
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFocusableInTouchMode = false
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Hero
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(2), dp(24), dp(54))
        }
        val glow = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(38, 255, 255, 255)) }
        }
        val inner = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(60, 255, 255, 255))
                setStroke(dp(1), Color.argb(120, 255, 255, 255))
            }
            elevation = dpf(4)
            addView(icon("crownFill", gold1), FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER))
        }
        glow.addView(inner, FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER))
        top.addView(glow, sz(96))
        top.addView(label("افتح وكالتك في BAK LIVE", 23f, Color.WHITE, 2).apply { gravity = Gravity.CENTER }, lp(t = 10))
        top.addView(
            label("وثّق هويتك وقدّم طلبك، وستتم مراجعته من قِبل الإدارة", 13.5f, Color.argb(215, 255, 255, 255), 1)
                .apply { gravity = Gravity.CENTER },
            lp(t = 6)
        )
        val chips = LinearLayout(this).apply { gravity = Gravity.CENTER }
        fun chip(kind: String, text: String, first: Boolean) {
            chips.addView(
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(8), dp(14), dp(8))
                    background = rect(Color.argb(40, 255, 255, 255), Color.argb(85, 255, 255, 255), 20)
                    addView(icon(kind, gold1), sz(15))
                    addView(label(text, 12f, Color.WHITE, 1), lp(s = 6))
                },
                lp(-2, -2, s = if (first) 0 else 8)
            )
        }
        chip("lock", "سرّية تامة", true)
        chip("idcard", "هوية وطنية", false)
        top.addView(chips, lp(t = 18))
        content.addView(top, lp())

        // White sheet
        val r = dpf(30)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        content.addView(body, lp(-1, -2, t = -30, wt = 1f))

        // Status banner
        statusBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            visibility = View.GONE
        }
        body.addView(statusBox, lp(b = 14))

        // Progress card
        val prog = card(16)
        val progHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        progHead.addView(label("اكتمال الطلب", 14.5f, ink, 2), lp(0, -2, wt = 1f))
        progressText = label("0 / $totalSteps", 13.5f, brand, 2).apply { textDirection = View.TEXT_DIRECTION_LTR }
        progHead.addView(progressText)
        prog.addView(progHead)
        bar = AgencyBarView(this, Color.parseColor("#EEE8FA"), brand, pink).apply { scaleX = -1f }
        prog.addView(bar, lp(-1, dp(8), t = 12))
        progressHint = label("", 12f, muted)
        prog.addView(progressHint, lp(t = 10))
        body.addView(prog)

        // ── Agency details
        section(body, "بيانات الوكالة", "اختر اسماً مميزاً لوكالتك")
        val (nameBox, nameEt) = inputBox("اسم الوكالة", "building")
        val (legalBox, legalEt) = inputBox("الاسم الكامل كما يظهر في الهوية", "user")
        agencyName = nameEt
        legalName = legalEt
        body.addView(fieldLabel("اسم الوكالة"), lp(t = 16, b = 8))
        body.addView(nameBox)
        body.addView(fieldLabel("الاسم القانوني"), lp(t = 14, b = 8))
        body.addView(legalBox)
        body.addView(fieldLabel("بلد إصدار الهوية"), lp(t = 14, b = 8))

        val countries = Locale.getISOCountries().map { code ->
            code to (Locale("", code).getDisplayCountry(Locale("ar")).ifBlank { Locale("", code).displayCountry })
        }.sortedBy { it.second }
        countryCodes = countries.map { it.first }
        country = Spinner(this).apply {
            adapter = styledAdapter(countries.map { "${flag(it.first)}  ${it.second}" })
            background = null
            setPadding(0, 0, 0, 0)
            setPopupBackgroundDrawable(rect(Color.WHITE, hairline, 14))
        }
        val def = countryCodes.indexOf(Locale.getDefault().country)
        if (def >= 0) country.setSelection(def)
        val countryBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(54)
            background = rect(surface, hairline, 16)
        }
        countryBox.addView(icon("globe", muted), sz(20))
        countryBox.addView(country, lp(0, dp(54), wt = 1f))
        countryBox.addView(icon("chevronDown", muted), sz(18))
        body.addView(countryBox)

        // ── ID documents
        section(body, "توثيق الهوية الوطنية", "")
        val note = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rect(brandSoft, r = 14)
        }
        note.addView(icon("shield", brand), sz(18))
        note.addView(label("نقبل بطاقة الهوية الوطنية من جميع الدول. لا نقبل جواز السفر.", 12.5f, brand, 1), lp(0, -2, s = 8, wt = 1f))
        body.addView(note, lp(t = 8, b = 12))

        tileFront = tile("الوجه الأمامي", "اختر صورة واضحة", "idcard", 140) { choose(pickFront) }
        tileBack = tile("الوجه الخلفي", "اختر صورة واضحة", "idcard", 140) { choose(pickBack) }
        val idRow = LinearLayout(this)
        idRow.addView(tileFront.root, lp(0, dp(140), e = 6, wt = 1f))
        idRow.addView(tileBack.root, lp(0, dp(140), s = 6, wt = 1f))
        body.addView(idRow)

        // ── Selfie
        section(body, "صورة شخصية مباشرة", "")
        tileSelfie = tile("التقط صورة سيلفي بالكاميرا", "يجب أن يظهر وجه صاحب الطلب بوضوح", "camera", 128) { captureSelfie() }
        body.addView(tileSelfie.root, lp(-1, dp(128), t = 12))

        // ── Consent
        val consentCard = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(14), dp(10))
            background = rect(surface, hairline, 16)
        }
        consent = CheckBox(this).apply {
            buttonTintList = ColorStateList.valueOf(brand)
            setOnCheckedChangeListener { _, _ -> refreshProgress() }
        }
        consentCard.addView(consent, LinearLayout.LayoutParams(-2, -2))
        consentCard.addView(
            label("أوافق على معالجة صور الهوية والصورة الشخصية لغرض التحقق من طلب الوكالة، وأفهم أنها لن تكون ظاهرة للمستخدمين.", 12.5f, muted, 1),
            lp(0, -2, wt = 1f)
        )
        consentCard.isClickable = true
        consentCard.setOnClickListener { consent.isChecked = !consent.isChecked }
        body.addView(consentCard, lp(t = 22))

        // ── Dashboard (shown only when applicable)
        dashboardButton = purpleButton("فتح لوحة الوكالة الخاصة") {
            startActivity(Intent(this@AgencyActivity, AgencyDashboardActivity::class.java))
        }.apply { visibility = View.GONE }
        body.addView(dashboardButton, lp(t = 16))

        // ── Footer
        val foot = LinearLayout(this).apply { gravity = Gravity.CENTER }
        foot.addView(icon("lock", Color.parseColor("#A29BB8")), sz(14))
        foot.addView(label("بياناتك محمية ولا تُستخدم لأي غرض آخر", 11.5f, Color.parseColor("#A29BB8"), 1), lp(s = 6))
        body.addView(foot, lp(t = 22))

        // ── Fixed bottom action bar
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(Color.WHITE)
            elevation = dpf(10)
        }
        submit = goldButton("إرسال طلب الوكالة", "send") { sendApplication() }
        bottomBar.addView(submit, lp())
        column.addView(bottomBar, lp())

        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        // Edge-to-edge safety (Android 15+): keep content clear of system bars / keyboard
        root.setOnApplyWindowInsetsListener { _, ins ->
            topBar.setPadding(dp(16), ins.systemWindowInsetTop + dp(8), dp(16), dp(8))
            (bg.layoutParams as ViewGroup.LayoutParams).height = dp(360) + ins.systemWindowInsetTop
            bg.requestLayout()
            column.setPadding(0, 0, 0, ins.systemWindowInsetBottom)
            ins
        }

        watch(agencyName)
        watch(legalName)
        setContentView(root)
        refreshProgress()
        loadStatus()
    }

    /* ─────────── pickers ─────────── */

    private fun choose(code: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, code)
    }

    private fun captureSelfie() {
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), cameraPermissionRequest)
            return
        }
        try {
            val dir = File(cacheDir, "agency-capture").apply { mkdirs() }
            cameraFile = File(dir, "selfie-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", cameraFile!!)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, takeSelfie)
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                captureSelfie()
            } else {
                Toast.makeText(this, "يجب السماح باستخدام الكاميرا لالتقاط صورة السيلفي", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            pickFront -> {
                frontUri = data?.data
                frontUri?.let { mark(tileFront, it, "تم اختيار صورة الهوية الأمامية") }
            }
            pickBack -> {
                backUri = data?.data
                backUri?.let { mark(tileBack, it, "تم اختيار صورة الهوية الخلفية") }
            }
            takeSelfie -> {
                val f = cameraFile
                if (f != null && f.exists() && f.length() > 0) {
                    selfieUri = Uri.fromFile(f)
                    mark(tileSelfie, selfieUri!!, "تم التقاط صورة الوجه")
                } else Toast.makeText(this, "لم يتم حفظ صورة السيلفي", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ─────────── submit ─────────── */

    private fun imageBase64(uri: Uri): String {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("تعذر قراءة إحدى الصور")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw Exception("ملف صورة غير صالح")
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw Exception("صيغة الصورة غير مدعومة")
        val out = ByteArrayOutputStream()
        var quality = 86
        do {
            out.reset(); bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out); quality -= 7
        } while (out.size() > 1536 * 1024 && quality >= 58)
        bitmap.recycle()
        if (out.size() > 1536 * 1024) throw Exception("حجم الصورة أكبر من المسموح")
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun sendApplication() {
        val name = agencyName.text.toString().trim()
        val legal = legalName.text.toString().trim()
        val front = frontUri; val back = backUri; val selfie = selfieUri
        if (name.length < 2 || legal.length < 3 || front == null || back == null || selfie == null || !consent.isChecked) {
            Toast.makeText(this, "أكمل البيانات والصور الثلاثة والموافقة أولاً", Toast.LENGTH_LONG).show(); return
        }
        val countryCode = countryCodes.getOrNull(country.selectedItemPosition)
            ?: run { Toast.makeText(this, "اختر الدولة", Toast.LENGTH_SHORT).show(); return }
        val auth = token() ?: run { finish(); return }
        setSubmit(false, "جارٍ الإرسال...")
        showStatus("جارٍ تجهيز الصور وإرسال الطلب...", 0)
        worker.execute {
            try {
                val payload = JSONObject()
                    .put("agency_name", name)
                    .put("legal_name", legal)
                    .put("country_code", countryCode)
                    .put("id_front_base64", imageBase64(front))
                    .put("id_back_base64", imageBase64(back))
                    .put("selfie_base64", imageBase64(selfie))
                    .put("consent", true)
                val result = AuthApi.post("/api/agency/application", payload, auth)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إرسال الطلب"))
                runOnUiThread {
                    showStatus("تم استلام طلبك وهو قيد مراجعة الإدارة", 2)
                    setSubmit(false, "طلبك قيد المراجعة")
                    Toast.makeText(this, "تم إرسال طلب الوكالة بنجاح", Toast.LENGTH_LONG).show()
                }
                cameraFile?.delete()
            } catch (e: Exception) {
                runOnUiThread {
                    showStatus(e.message ?: "تعذر إرسال الطلب", 3)
                    setSubmit(true)
                }
            }
        }
    }

    private fun loadStatus() {
        val auth = token() ?: run { agencyRoot.visibility = View.VISIBLE; return }
        worker.execute {
            try {
                val result = AuthApi.get("/api/agency/application", auth)
                if (result.status in 200..299) {
                    val app = result.body.optJSONObject("application")
                    val applicationStatus = app?.optString("status", "").orEmpty()
                    runOnUiThread {
                        if (applicationStatus == "approved") {
                            startActivity(Intent(this@AgencyActivity, AgencyDashboardActivity::class.java))
                            finish()
                            return@runOnUiThread
                        }
                        agencyRoot.visibility = View.VISIBLE
                        when (applicationStatus) {
                            "pending" -> showStatus("طلبك قيد مراجعة الإدارة", 2)
                            "rejected" -> showStatus("تم رفض الطلب: " + (app?.optString("review_note") ?: ""), 3)
                            else -> showStatus("", 0)
                        }
                        if (applicationStatus == "pending") setSubmit(false, "طلبك قيد المراجعة")
                    }
                } else runOnUiThread { agencyRoot.visibility = View.VISIBLE }
            } catch (_: Exception) {
                runOnUiThread { agencyRoot.visibility = View.VISIBLE }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}