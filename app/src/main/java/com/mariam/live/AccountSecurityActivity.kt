package com.mariam.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
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

    // Palette
    private val bg = Color.rgb(245, 247, 250)
    private val ink = Color.rgb(26, 32, 44)
    private val muted = Color.rgb(110, 118, 130)
    private val divider = Color.rgb(238, 240, 243)
    private val tealDark = Color.rgb(14, 135, 126)
    private val okBg = Color.rgb(225, 247, 244)
    private val rose = Color.rgb(196, 58, 90)
    private val roseBg = Color.rgb(253, 236, 240)
    private val neutralBg = Color.rgb(238, 240, 243)

    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val totalMethods = 2 // الهاتف + البريد

    private lateinit var phoneRow: AccountRow
    private lateinit var emailRow: AccountRow
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var heroCount: TextView
    private val segments = mutableListOf<View>()

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

        body.addView(TextView(this).apply {
            text = "وسائل الربط"
            textSize = 14f
            typeface = medium
            setTextColor(muted)
            setPadding(dp(6), 0, dp(6), 0)
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(26); bottomMargin = dp(10) })

        phoneRow = accountRow("الهاتف", Glyph.PHONE, tealDark, Color.rgb(226, 248, 246)) {
            startActivity(Intent(this, PhoneSecurityActivity::class.java))
        }
        emailRow = accountRow("البريد الإلكتروني", Glyph.MAIL, Color.rgb(92, 106, 132), Color.rgb(236, 240, 247)) {
            comingSoon("البريد الإلكتروني")
        }
        val googleRow = accountRow("Google", Glyph.GOOGLE, Color.BLACK, Color.rgb(235, 242, 255)) { comingSoon("Google") }
        val snapRow = accountRow("Snapchat", Glyph.SNAPCHAT, Color.rgb(30, 30, 30), Color.rgb(255, 252, 0)) { comingSoon("Snapchat") }

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
            if (i < rows.lastIndex) card.addView(separator(), LinearLayout.LayoutParams(-1, 1.coerceAtLeast(dp(1))).apply {
                marginStart = dp(76)
            })
        }
        body.addView(card, LinearLayout.LayoutParams(-1, -2))

        body.addView(noteBanner("i", "نستخدم هذه الوسائل لتأمين حسابك واستعادته إذا فقدت الوصول إليه.", Color.rgb(232, 246, 245), Color.rgb(43, 202, 190)))

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        setHero("جارٍ فحص الحساب", "لحظات ونكمل التحقق", null)
    }

    override fun onResume() {
        super.onResume()
        loadAccountLinks() // يتحدث تلقائياً عند الرجوع من صفحة الهاتف
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
            addView(GlyphView(this@AccountSecurityActivity, Glyph.CHEVRON_RIGHT, ink), FrameLayout.LayoutParams(-1, -1))
        }
        addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(TextView(this@AccountSecurityActivity).apply {
            text = "إدارة الحساب"
            textSize = 18f
            typeface = medium
            setTextColor(ink)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(View(this@AccountSecurityActivity), LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    // ───────── Hero (حالة الحماية) ─────────

    private fun buildHero(): View {
        heroTitle = TextView(this).apply { textSize = 19f; typeface = medium; setTextColor(Color.WHITE) }
        heroSub = TextView(this).apply {
            textSize = 13.5f
            setTextColor(Color.argb(225, 255, 255, 255))
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(4), 0, 0)
        }
        heroCount = TextView(this).apply {
            textSize = 12.5f
            typeface = medium
            setTextColor(Color.argb(225, 255, 255, 255))
        }

        val shield = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(56, 255, 255, 255)) }
            addView(GlyphView(this@AccountSecurityActivity, Glyph.SHIELD, Color.WHITE), FrameLayout.LayoutParams(-1, -1))
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(heroTitle)
            addView(heroSub)
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(shield, LinearLayout.LayoutParams(dp(52), dp(52)))
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

    private fun separator() = View(this).apply { setBackgroundColor(divider) }

    private fun accountRow(title: String, glyph: Glyph, glyphColor: Int, tileColor: Int, click: () -> Unit): AccountRow {
        val titleView = TextView(this).apply {
            text = title; textSize = 16f; typeface = medium; setTextColor(ink)
        }
        val subtitle = TextView(this).apply {
            textSize = 13f; setTextColor(muted); visibility = View.GONE
            setPadding(0, dp(2), 0, 0)
        }
        val chip = TextView(this).apply {
            textSize = 12.5f; typeface = medium
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleView); addView(subtitle)
        }
        val tile = FrameLayout(this).apply {
            background = rounded(tileColor, 14)
            addView(GlyphView(this@AccountSecurityActivity, glyph, glyphColor), FrameLayout.LayoutParams(-1, -1))
        }
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(76)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(38, 43, 202, 190)), ColorDrawable(Color.WHITE), null
            )
            addView(tile, LinearLayout.LayoutParams(dp(46), dp(46)))
            addView(column, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
            addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
            addView(
                GlyphView(this@AccountSecurityActivity, Glyph.CHEVRON_LEFT, Color.rgb(176, 181, 189)),
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

    private fun noteBanner(icon: String, message: String, backgroundColor: Int, iconColor: Int) =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(backgroundColor, 16)
            addView(TextView(this@AccountSecurityActivity).apply {
                text = icon; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(iconColor) }
            }, LinearLayout.LayoutParams(dp(26), dp(26)))
            addView(TextView(this@AccountSecurityActivity).apply {
                text = message; textSize = 13f
                setTextColor(Color.rgb(62, 92, 92))
                setLineSpacing(0f, 1.2f)
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
        }

    private fun comingSoon(provider: String) {
        Toast.makeText(this, "ربط $provider سيكون متاحاً قريباً", Toast.LENGTH_SHORT).show()
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

// ───────── Vector-style icons drawn with Canvas (no emoji, no extra assets) ─────────

private enum class Glyph { PHONE, MAIL, GOOGLE, SNAPCHAT, SHIELD, CHEVRON_LEFT, CHEVRON_RIGHT }

private class GlyphView(context: Context, private val glyph: Glyph, private val tint: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = w * 0.07f
        paint.color = tint
        path.reset()

        when (glyph) {
            Glyph.PHONE -> {
                canvas.drawRoundRect(RectF(w * .31f, h * .2f, w * .69f, h * .8f), w * .09f, w * .09f, paint)
                canvas.drawLine(w * .44f, h * .7f, w * .56f, h * .7f, paint)
            }
            Glyph.MAIL -> {
                canvas.drawRoundRect(RectF(w * .22f, h * .3f, w * .78f, h * .7f), w * .07f, w * .07f, paint)
                path.moveTo(w * .25f, h * .35f)
                path.lineTo(cx, h * .54f)
                path.lineTo(w * .75f, h * .35f)
                canvas.drawPath(path, paint)
            }
            Glyph.GOOGLE -> {
                paint.style = Paint.Style.FILL
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = h * 0.6f
                paint.shader = SweepGradient(
                    cx, cy,
                    intArrayOf(
                        Color.rgb(234, 67, 53), Color.rgb(251, 188, 5),
                        Color.rgb(52, 168, 83), Color.rgb(66, 133, 244), Color.rgb(234, 67, 53)
                    ),
                    null
                )
                canvas.drawText("G", cx, cy - (paint.ascent() + paint.descent()) / 2, paint)
            }
            Glyph.SNAPCHAT -> {
                path.moveTo(cx - w * .17f, h * .4f)
                path.arcTo(RectF(cx - w * .17f, h * .2f, cx + w * .17f, h * .6f), 180f, 180f)
                path.lineTo(cx + w * .17f, h * .7f)
                path.lineTo(cx + w * .11f, h * .77f)
                path.lineTo(cx, h * .71f)
                path.lineTo(cx - w * .11f, h * .77f)
                path.lineTo(cx - w * .17f, h * .7f)
                path.close()
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.STROKE
                paint.color = tint
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx - w * .06f, h * .43f, w * .025f, paint)
                canvas.drawCircle(cx + w * .06f, h * .43f, w * .025f, paint)
            }
            Glyph.SHIELD -> {
                paint.strokeWidth = w * 0.06f
                path.moveTo(cx, h * .2f)
                path.lineTo(w * .74f, h * .3f)
                path.lineTo(w * .74f, h * .52f)
                path.quadTo(w * .74f, h * .72f, cx, h * .82f)
                path.quadTo(w * .26f, h * .72f, w * .26f, h * .52f)
                path.lineTo(w * .26f, h * .3f)
                path.close()
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(w * .4f, h * .5f)
                path.lineTo(w * .47f, h * .58f)
                path.lineTo(w * .61f, h * .42f)
                canvas.drawPath(path, paint)
            }
            Glyph.CHEVRON_LEFT -> {
                paint.strokeWidth = w * 0.09f
                path.moveTo(w * .6f, h * .28f)
                path.lineTo(w * .4f, cy)
                path.lineTo(w * .6f, h * .72f)
                canvas.drawPath(path, paint)
            }
            Glyph.CHEVRON_RIGHT -> {
                paint.strokeWidth = w * 0.08f
                path.moveTo(w * .4f, h * .28f)
                path.lineTo(w * .6f, cy)
                path.lineTo(w * .4f, h * .72f)
                canvas.drawPath(path, paint)
            }
        }
    }
}
