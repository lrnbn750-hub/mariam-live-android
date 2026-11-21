package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class AccountSecurityActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    // ───────── Palette ─────────
    private val bg = Color.rgb(245, 247, 250)
    private val ink = Color.rgb(17, 24, 39)
    private val muted = Color.rgb(107, 114, 128)
    private val divider = Color.rgb(238, 240, 243)
    private val tealDark = Color.rgb(14, 135, 126)
    private val okBg = Color.rgb(222, 246, 242)
    private val rose = Color.rgb(196, 58, 90)
    private val roseBg = Color.rgb(253, 236, 240)
    private val neutralBg = Color.rgb(238, 240, 243)

    // ───────── Typography ─────────
    // لاستخدام خط احترافي: ضع ملفات الخط في res/font بهذه الأسماء بالضبط
    // app_font_regular / app_font_medium / app_font_bold  (مثلاً IBM Plex Sans Arabic أو Tajawal)
    // وإن لم تكن موجودة يستخدم التطبيق خط النظام تلقائياً.
    private val fRegular by lazy { face("app_font_regular", Typeface.create("sans-serif", Typeface.NORMAL)) }
    private val fMedium by lazy { face("app_font_medium", Typeface.create("sans-serif-medium", Typeface.NORMAL)) }
    private val fBold by lazy { face("app_font_bold", Typeface.create("sans-serif", Typeface.BOLD)) }

    private fun face(name: String, fallback: Typeface): Typeface {
        if (Build.VERSION.SDK_INT >= 26) {
            val id = resources.getIdentifier(name, "font", packageName)
            if (id != 0) {
                try { return resources.getFont(id) } catch (_: Exception) {}
            }
        }
        return fallback
    }

    private fun label(size: Float, face: Typeface, color: Int) = TextView(this).apply {
        textSize = size
        typeface = face
        setTextColor(color)
    }

    private val totalMethods = 2 // الهاتف + البريد

    private lateinit var phoneRow: AccountRow
    private lateinit var emailRow: AccountRow
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var heroCount: TextView
    private val segments = mutableListOf<View>()
    private var linkedEmail: String? = null

    private enum class Tone { OK, WARN, NEUTRAL }

    private class AccountRow(val root: View, val subtitle: TextView, val chip: TextView)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
        }
        root.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(60)))

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(28))
            clipChildren = false
            clipToPadding = false
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(body)
        }

        body.addView(buildHero())

        body.addView(label(13.5f, fMedium, muted).apply {
            text = "وسائل الربط"
            setPadding(dp(6), 0, dp(6), 0)
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(26); bottomMargin = dp(10) })

        phoneRow = accountRow(
            "الهاتف", Glyph.PHONE,
            Color.rgb(52, 211, 196), Color.rgb(16, 160, 150), Color.WHITE, Color.rgb(16, 160, 150), false
        ) { startActivity(Intent(this, PhoneSecurityActivity::class.java)) }

        emailRow = accountRow(
            "البريد الإلكتروني", Glyph.MAIL,
            Color.rgb(96, 165, 250), Color.rgb(37, 99, 235), Color.WHITE, Color.rgb(37, 99, 235), false
        ) { onEmailClick() }

        val googleRow = accountRow(
            "Google", Glyph.GOOGLE,
            Color.WHITE, Color.WHITE, Color.BLACK, Color.WHITE, true
        ) { comingSoon("Google") }

        val snapRow = accountRow(
            "Snapchat", Glyph.SNAPCHAT,
            Color.rgb(255, 252, 0), Color.rgb(255, 252, 0), Color.rgb(24, 24, 24), Color.WHITE, false
        ) { comingSoon("Snapchat") }

        applyChip(phoneRow.chip, "جارٍ التحقق", Tone.NEUTRAL)
        applyChip(emailRow.chip, "جارٍ التحقق", Tone.NEUTRAL)
        applyChip(googleRow.chip, "قريباً", Tone.NEUTRAL)
        applyChip(snapRow.chip, "قريباً", Tone.NEUTRAL)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 22)
            clipToOutline = true
            elevation = dp(2).toFloat()
        }
        val rows = listOf(phoneRow, emailRow, googleRow, snapRow)
        rows.forEachIndexed { i, r ->
            card.addView(r.root, LinearLayout.LayoutParams(-1, -2))
            if (i < rows.lastIndex) card.addView(
                View(this).apply { setBackgroundColor(divider) },
                LinearLayout.LayoutParams(-1, 1.coerceAtLeast(dp(1))).apply { marginStart = dp(76) }
            )
        }
        body.addView(card, LinearLayout.LayoutParams(-1, -2))

        body.addView(noteBanner("نستخدم هذه الوسائل لتأمين حسابك واستعادته إذا فقدت الوصول إليه."))

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        setHero("جارٍ فحص الحساب", "لحظات ونكمل التحقق", null)
    }

    override fun onResume() {
        super.onResume()
        loadAccountLinks()
    }

    // ───────── Header ─────────

    private fun buildHeader(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)

        val back = FrameLayout(this@AccountSecurityActivity).apply {
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(30, 0, 0, 0)), rounded(Color.WHITE, 22), null
            )
            elevation = dp(1).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "رجوع"
            setOnClickListener { finish() }
            addView(GlyphView(this@AccountSecurityActivity, Glyph.CHEVRON_RIGHT, ink, ink), FrameLayout.LayoutParams(-1, -1))
        }
        addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(label(18f, fBold, ink).apply {
            text = "إدارة الحساب"
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(View(this@AccountSecurityActivity), LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    // ───────── Hero ─────────

    private fun buildHero(): View {
        heroTitle = label(20f, fBold, Color.WHITE)
        heroSub = label(13.5f, fRegular, Color.argb(225, 255, 255, 255)).apply {
            setLineSpacing(0f, 1.25f)
            setPadding(0, dp(5), 0, 0)
        }
        heroCount = label(12.5f, fMedium, Color.argb(225, 255, 255, 255))

        val shield = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(50, 255, 255, 255)) }
            addView(GlyphView(this@AccountSecurityActivity, Glyph.SHIELD, Color.WHITE, Color.rgb(14, 126, 124)), FrameLayout.LayoutParams(-1, -1))
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(heroTitle)
            addView(heroSub)
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(shield, LinearLayout.LayoutParams(dp(54), dp(54)))
            addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
        }
        val bar = LinearLayout(this)
        for (i in 0 until totalMethods) {
            val seg = View(this)
            segments.add(seg)
            bar.addView(seg, LinearLayout.LayoutParams(0, dp(6), 1f).apply { if (i > 0) marginStart = dp(6) })
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(18, 158, 148), Color.rgb(10, 108, 112))
            ).apply { cornerRadius = dp(24).toFloat() }
            addView(top)
            addView(bar, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(20) })
            addView(heroCount, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })
        }
    }

    private fun setHero(title: String, sub: String, linked: Int?) {
        heroTitle.text = title
        heroSub.text = sub
        segments.forEachIndexed { i, v ->
            val filled = linked != null && i < linked
            v.background = rounded(if (filled) Color.WHITE else Color.argb(80, 255, 255, 255), 3)
        }
        heroCount.text = if (linked == null) "" else "مرتبط $linked من $totalMethods"
    }

    // ───────── Rows ─────────

    private fun accountRow(
        title: String, glyph: Glyph,
        tileStart: Int, tileEnd: Int, tint: Int, detail: Int, outlined: Boolean,
        click: () -> Unit
    ): AccountRow {
        val titleView = label(16f, fMedium, ink).apply { text = title }
        val subtitle = label(13f, fRegular, muted).apply {
            visibility = View.GONE
            setPadding(0, dp(3), 0, 0)
        }
        val chip = label(12f, fMedium, muted).apply {
            setPadding(dp(11), dp(5), dp(11), dp(5))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleView); addView(subtitle)
        }
        val tile = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(tileStart, tileEnd)).apply {
                cornerRadius = dp(14).toFloat()
                if (outlined) setStroke(dp(1), Color.rgb(226, 229, 234))
            }
            addView(GlyphView(this@AccountSecurityActivity, glyph, tint, detail), FrameLayout.LayoutParams(-1, -1))
        }
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(78)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(38, 43, 202, 190)), ColorDrawable(Color.WHITE), null
            )
            addView(tile, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(column, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
            addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
            addView(
                GlyphView(this@AccountSecurityActivity, Glyph.CHEVRON_LEFT, Color.rgb(170, 176, 186), Color.rgb(170, 176, 186)),
                LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginStart = dp(4) }
            )
            setOnClickListener { click() }
        }
        return AccountRow(row, subtitle, chip)
    }

    private fun applyChip(chip: TextView, text: String, tone: Tone) {
        val (fg, bgColor) = when (tone) {
            Tone.OK -> tealDark to okBg
            Tone.WARN -> rose to roseBg
            Tone.NEUTRAL -> muted to neutralBg
        }
        chip.text = text
        chip.setTextColor(fg)
        chip.background = rounded(bgColor, 20)
    }

    private fun setSubtitle(row: AccountRow, text: String?) {
        if (text.isNullOrBlank()) {
            row.subtitle.visibility = View.GONE
        } else {
            row.subtitle.text = text
            row.subtitle.visibility = View.VISIBLE
        }
    }

    private fun noteBanner(message: String) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = rounded(Color.rgb(232, 246, 245), 16)
        addView(
            GlyphView(this@AccountSecurityActivity, Glyph.INFO, Color.rgb(24, 168, 158), Color.WHITE),
            LinearLayout.LayoutParams(dp(24), dp(24))
        )
        addView(label(13f, fRegular, Color.rgb(52, 84, 84)).apply {
            text = message
            setLineSpacing(0f, 1.25f)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
    }

    // ───────── Actions ─────────

    private fun comingSoon(provider: String) {
        Toast.makeText(this, "ربط $provider سيكون متاحاً قريباً", Toast.LENGTH_SHORT).show()
    }

    private fun onEmailClick() {
        val email = linkedEmail
        if (email.isNullOrBlank()) {
            comingSoon("البريد الإلكتروني")
        } else {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("البريد الإلكتروني")
                .setMessage("بريدك مرتبط بالحساب:\n" + maskEmail(email))
                .setPositiveButton("حسناً", null)
                .show()
        }
    }

    // ───────── Data ─────────

    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        return if (at <= 1) email else "\u200E" + email.take(1) + "•••" + email.substring(at)
    }

    private fun loadAccountLinks() {
        val auth = token() ?: run {
            applyChip(phoneRow.chip, "سجّل الدخول", Tone.WARN)
            applyChip(emailRow.chip, "غير مربوط", Tone.WARN)
            setHero("سجّل الدخول", "سجّل الدخول لإدارة وسائل ربط حسابك", 0)
            return
        }
        executor.execute {
            try {
                val response = AuthApi.get("/api/auth/me", auth)
                if (response.status !in 200..299) throw Exception("تعذر تحميل بيانات الحساب")
                val user = response.body.optJSONObject("user")
                val phone = user?.optString("phone")?.takeUnless { it.isNullOrBlank() || it == "null" }
                val email = user?.optString("email")?.takeUnless { it.isNullOrBlank() || it == "null" }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    var linked = 0

                    if (!phone.isNullOrBlank()) {
                        linked++
                        val digits = phone.filter { it.isDigit() }
                        val masked = if (digits.length >= 7) digits.take(2) + "******" + digits.takeLast(4) else phone
                        setSubtitle(phoneRow, "\u200E$masked")
                        applyChip(phoneRow.chip, "مرتبط", Tone.OK)
                    } else {
                        setSubtitle(phoneRow, "أضف رقماً لاستعادة حسابك")
                        applyChip(phoneRow.chip, "ربط الآن", Tone.WARN)
                    }

                    linkedEmail = email
                    if (!email.isNullOrBlank()) {
                        linked++
                        setSubtitle(emailRow, maskEmail(email))
                        applyChip(emailRow.chip, "مرتبط", Tone.OK)
                    } else {
                        setSubtitle(emailRow, null)
                        applyChip(emailRow.chip, "غير مربوط", Tone.WARN)
                    }

                    when (linked) {
                        0 -> setHero("حسابك يحتاج حماية", "اربط رقم هاتفك لتأمين الحساب واستعادته", linked)
                        1 -> setHero("حماية جيدة", "أضف وسيلة ثانية لتصبح أكثر أماناً", linked)
                        else -> setHero("حسابك محمي", "وسائل الاستعادة جاهزة", linked)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    setSubtitle(phoneRow, "اضغط لإدارة رقم الهاتف")
                    applyChip(phoneRow.chip, "إدارة", Tone.NEUTRAL)
                    applyChip(emailRow.chip, "تعذر التحقق", Tone.WARN)
                    setHero("تعذر فحص الحساب", "تحقق من الاتصال بالإنترنت ثم أعد فتح الصفحة", null)
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

// ───────── Icons: مرسومة بالكود، شكل مصمت (Filled) بنمط التطبيقات العالمية ─────────

private enum class Glyph { PHONE, MAIL, GOOGLE, SNAPCHAT, SHIELD, INFO, CHEVRON_LEFT, CHEVRON_RIGHT }

/** tint = لون الشكل الأساسي، detail = لون التفاصيل الداخلية (عادةً لون الخلفية) */
private class GlyphView(
    context: Context,
    private val glyph: Glyph,
    private val tint: Int,
    private val detail: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun fill(c: Int) { paint.style = Paint.Style.FILL; paint.color = c }
    private fun stroke(c: Int, wd: Float, cap: Paint.Cap = Paint.Cap.ROUND) {
        paint.style = Paint.Style.STROKE; paint.color = c; paint.strokeWidth = wd; paint.strokeCap = cap
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        paint.strokeJoin = Paint.Join.ROUND
        path.reset()

        when (glyph) {
            Glyph.PHONE -> {
                fill(tint)
                canvas.drawRoundRect(RectF(w * .31f, h * .17f, w * .69f, h * .83f), w * .1f, w * .1f, paint)
                fill(detail)
                canvas.drawRoundRect(RectF(w * .355f, h * .235f, w * .645f, h * .69f), w * .035f, w * .035f, paint)
                fill(detail)
                canvas.drawCircle(cx, h * .76f, w * .028f, paint)
            }
            Glyph.MAIL -> {
                fill(tint)
                canvas.drawRoundRect(RectF(w * .2f, h * .29f, w * .8f, h * .71f), w * .08f, w * .08f, paint)
                path.moveTo(w * .25f, h * .37f)
                path.lineTo(cx, h * .56f)
                path.lineTo(w * .75f, h * .37f)
                stroke(detail, w * .05f)
                canvas.drawPath(path, paint)
            }
            Glyph.GOOGLE -> {
                val r = w * .24f
                val box = RectF(cx - r, cy - r, cx + r, cy + r)
                val sw = w * .12f
                stroke(Color.rgb(234, 67, 53), sw, Paint.Cap.BUTT)
                canvas.drawArc(box, 222f, 100f, false, paint)
                stroke(Color.rgb(251, 188, 5), sw, Paint.Cap.BUTT)
                canvas.drawArc(box, 132f, 90f, false, paint)
                stroke(Color.rgb(52, 168, 83), sw, Paint.Cap.BUTT)
                canvas.drawArc(box, 48f, 84f, false, paint)
                stroke(Color.rgb(66, 133, 244), sw, Paint.Cap.BUTT)
                canvas.drawArc(box, 0f, 48f, false, paint)
                canvas.drawLine(cx, cy, cx + r + sw / 2, cy, paint)
            }
            Glyph.SNAPCHAT -> {
                path.moveTo(cx - w * .17f, h * .42f)
                path.arcTo(RectF(cx - w * .17f, h * .2f, cx + w * .17f, h * .64f), 180f, 180f)
                path.lineTo(cx + w * .17f, h * .7f)
                path.quadTo(cx + w * .16f, h * .79f, cx + w * .09f, h * .77f)
                path.quadTo(cx + w * .04f, h * .74f, cx, h * .77f)
                path.quadTo(cx - w * .04f, h * .74f, cx - w * .09f, h * .77f)
                path.quadTo(cx - w * .16f, h * .79f, cx - w * .17f, h * .7f)
                path.close()
                fill(detail)
                canvas.drawPath(path, paint)
                stroke(tint, w * .05f)
                canvas.drawPath(path, paint)
                fill(tint)
                canvas.drawCircle(cx - w * .06f, h * .44f, w * .026f, paint)
                canvas.drawCircle(cx + w * .06f, h * .44f, w * .026f, paint)
            }
            Glyph.SHIELD -> {
                path.moveTo(cx, h * .18f)
                path.lineTo(w * .75f, h * .29f)
                path.lineTo(w * .75f, h * .5f)
                path.quadTo(w * .75f, h * .72f, cx, h * .83f)
                path.quadTo(w * .25f, h * .72f, w * .25f, h * .5f)
                path.lineTo(w * .25f, h * .29f)
                path.close()
                fill(tint)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(w * .39f, h * .5f)
                path.lineTo(w * .465f, h * .58f)
                path.lineTo(w * .62f, h * .41f)
                stroke(detail, w * .065f)
                canvas.drawPath(path, paint)
            }
            Glyph.INFO -> {
                fill(tint)
                canvas.drawCircle(cx, cy, w * .5f, paint)
                fill(detail)
                canvas.drawCircle(cx, h * .3f, w * .07f, paint)
                stroke(detail, w * .12f)
                canvas.drawLine(cx, h * .46f, cx, h * .72f, paint)
            }
            Glyph.CHEVRON_LEFT -> {
                path.moveTo(w * .6f, h * .28f)
                path.lineTo(w * .4f, cy)
                path.lineTo(w * .6f, h * .72f)
                stroke(tint, w * .09f)
                canvas.drawPath(path, paint)
            }
            Glyph.CHEVRON_RIGHT -> {
                path.moveTo(w * .4f, h * .28f)
                path.lineTo(w * .6f, cy)
                path.lineTo(w * .4f, h * .72f)
                stroke(tint, w * .08f)
                canvas.drawPath(path, paint)
            }
        }
    }
}