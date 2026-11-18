package com.mariam.live

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.concurrent.thread

/* ───────────────────────── Vector icons (crisp at any density) ───────────────────────── */

private class IconView(ctx: Context, private val kind: String, private val tint: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.9f
    }

    private fun dot(c: Canvas, x: Float, y: Float) {
        p.style = Paint.Style.FILL
        c.drawCircle(x, y, 1.25f, p)
        p.style = Paint.Style.STROKE
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        c.scale(s / 24f, s / 24f)
        p.color = tint
        when (kind) {
            "back" -> c.drawPath(Path().apply { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }, p)
            "close" -> { c.drawLine(6f, 6f, 18f, 18f, p); c.drawLine(18f, 6f, 6f, 18f, p) }
            "check" -> c.drawPath(Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, p)
            "diamond" -> {
                c.drawPath(Path().apply {
                    moveTo(6f, 3f); lineTo(18f, 3f); lineTo(22f, 9f); lineTo(12f, 21f); lineTo(2f, 9f); close()
                }, p)
                c.drawLine(2f, 9f, 22f, 9f, p)
                c.drawPath(Path().apply { moveTo(9.5f, 3f); lineTo(8f, 9f); lineTo(12f, 21f) }, p)
                c.drawPath(Path().apply { moveTo(14.5f, 3f); lineTo(16f, 9f); lineTo(12f, 21f) }, p)
            }
            "building" -> {
                c.drawRoundRect(RectF(5f, 3f, 19f, 21f), 2f, 2f, p)
                dot(c, 9f, 8f); dot(c, 15f, 8f); dot(c, 9f, 12f); dot(c, 15f, 12f)
                c.drawPath(Path().apply { moveTo(10f, 21f); lineTo(10f, 17f); lineTo(14f, 17f); lineTo(14f, 21f) }, p)
            }
            "wallet" -> {
                c.drawRoundRect(RectF(3f, 6f, 21f, 19f), 3.5f, 3.5f, p)
                c.drawLine(3f, 10f, 21f, 10f, p)
                dot(c, 16.5f, 14.5f)
            }
            "gift" -> {
                c.drawRoundRect(RectF(3.5f, 8f, 20.5f, 12f), 1.2f, 1.2f, p)
                c.drawRoundRect(RectF(5f, 12f, 19f, 21f), 1.5f, 1.5f, p)
                c.drawLine(12f, 8f, 12f, 21f, p)
                c.drawPath(Path().apply {
                    moveTo(12f, 8f); cubicTo(10f, 8f, 7.5f, 7.2f, 7.5f, 5.2f); cubicTo(7.5f, 3.2f, 10.5f, 3.4f, 12f, 8f)
                }, p)
                c.drawPath(Path().apply {
                    moveTo(12f, 8f); cubicTo(14f, 8f, 16.5f, 7.2f, 16.5f, 5.2f); cubicTo(16.5f, 3.2f, 13.5f, 3.4f, 12f, 8f)
                }, p)
            }
            "trend" -> {
                c.drawPath(Path().apply { moveTo(3f, 17f); lineTo(9f, 11f); lineTo(13f, 15f); lineTo(21f, 7f) }, p)
                c.drawPath(Path().apply { moveTo(15f, 7f); lineTo(21f, 7f); lineTo(21f, 13f) }, p)
            }
            "upright" -> {
                c.drawLine(7f, 17f, 17f, 7f, p)
                c.drawPath(Path().apply { moveTo(8f, 7f); lineTo(17f, 7f); lineTo(17f, 16f) }, p)
            }
            "alert" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawLine(12f, 7.5f, 12f, 13f, p)
                dot(c, 12f, 16.5f)
            }
        }
        c.restore()
    }
}

/* Hero card with soft decorative circles */
private class HeroLayout(ctx: Context) : LinearLayout(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        clipToOutline = true
    }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.color = Color.argb(30, 255, 255, 255)
        canvas.drawCircle(w * 0.96f, h * 0.02f, h * 0.55f, p)
        p.color = Color.argb(20, 255, 255, 255)
        canvas.drawCircle(w * 0.04f, h * 1.02f, h * 0.45f, p)
        super.dispatchDraw(canvas)
    }
}

/* ───────────────────────────────────── Screen ───────────────────────────────────── */

@Suppress("DEPRECATION")
class CreatorCenterActivity : Activity() {

    // Palette — clean white surface, violet brand, emerald for money
    private val ink = Color.parseColor("#0F172A")
    private val muted = Color.parseColor("#64748B")
    private val hairline = Color.parseColor("#E9ECF3")
    private val surface = Color.parseColor("#F7F8FC")
    private val brand = Color.parseColor("#6D28D9")
    private val brandSoft = Color.parseColor("#F1EAFE")
    private val green = Color.parseColor("#059669")
    private val greenSoft = Color.parseColor("#E6F7F0")
    private val danger = Color.parseColor("#E11D48")
    private val dangerSoft = Color.parseColor("#FFF1F2")

    private var token = ""
    private var wallet = JSONObject()
    private var history = JSONArray()
    private var incomingInvitations = JSONArray()
    private var invitationLoadError = ""
    private var agencyMembership = JSONObject()
    private lateinit var content: LinearLayout

    /* ─────────── helpers ─────────── */

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Int) = v * resources.displayMetrics.density
    private fun fmt(c: Long) = String.format(Locale.US, "%,.2f", c / 100.0)
    private fun fmtDate(s: String) = if (s.isBlank() || s == "null") "" else s.replace('T', ' ').take(16)

    private fun rect(fill: Int, stroke: Int = Color.TRANSPARENT, r: Int = 16, sw: Int = 1) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dpf(r)
            if (stroke != Color.TRANSPARENT) setStroke(dp(sw), stroke)
        }

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
        setLineSpacing(0f, 1.15f)
    }

    private fun icon(kind: String, tint: Int) = IconView(this, kind, tint)

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

    private fun button(s: String, primary: Boolean = true, click: () -> Unit) = TextView(this).apply {
        text = s
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = dp(50)
        setPadding(dp(16), 0, dp(16), 0)
        isClickable = true
        if (primary) {
            setTextColor(Color.WHITE)
            val g = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#5B21B6"))
            ).apply { cornerRadius = dpf(14) }
            background = ripple(g, 14, Color.argb(60, 255, 255, 255))
        } else {
            setTextColor(brand)
            background = ripple(rect(Color.WHITE, Color.parseColor("#D9D2EC"), 14), 14, brandSoft)
        }
        setOnClickListener { click() }
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.parseColor("#94A3B8"))
        setTextColor(ink)
        textSize = 15f
        setSingleLine()
        minHeight = dp(52)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), rect(Color.WHITE, brand, 14, 2))
            addState(intArrayOf(), rect(surface, hairline, 14))
        }
    }

    private fun sheet(build: (LinearLayout, Dialog) -> Unit): Dialog {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rect(Color.WHITE, r = 26)
        }
        build(box, d)
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return d
    }

    private fun notice(msg: String, onOk: (() -> Unit)? = null) {
        sheet { box, d ->
            box.gravity = Gravity.CENTER_HORIZONTAL
            box.addView(badge("alert", brand, brandSoft, 26, 20), sz(52))
            box.addView(
                label(msg, 15f, ink, 1).apply { gravity = Gravity.CENTER },
                lp(t = 14)
            )
            box.addView(button("حسناً") { d.dismiss(); onOk?.invoke() }, lp(t = 18))
        }.show()
    }

    /* ─────────── lifecycle ─────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= 23) {
            var f = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26) f = f or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = f
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setOnApplyWindowInsetsListener { v, ins ->
                v.setPadding(0, ins.systemWindowInsetTop, 0, ins.systemWindowInsetBottom)
                ins
            }
        }

        // Header
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        val back = FrameLayout(this).apply {
            background = ripple(rect(surface, r = 14), 14, brandSoft)
            isClickable = true
            setOnClickListener { finish() }
            addView(icon("back", ink), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        }
        header.addView(back, sz(42))
        header.addView(
            label("مركز صُنّاع المحتوى", 18f, ink, 2).apply { gravity = Gravity.CENTER },
            lp(0, -2, wt = 1f)
        )
        header.addView(
            label("BAK", 11f, brand, 2).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rect(brandSoft, r = 10)
                letterSpacing = 0.08f
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        root.addView(header)
        root.addView(View(this).apply { setBackgroundColor(hairline) }, LinearLayout.LayoutParams(-1, dp(1)))

        // Body
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(32))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        load()
    }

    /* ─────────── data ─────────── */

    private fun load() {
        content.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(120), 0, 0)
        }
        box.addView(ProgressBar(this).apply { indeterminateTintList = ColorStateList.valueOf(brand) }, sz(36))
        box.addView(label("جارٍ تحميل مركز الأرباح...", 14f, muted, 1), lp(t = 14))
        content.addView(box)

        thread {
            try {
                val w = AuthApi.get("/api/wallet", token)
                val h = AuthApi.get("/api/wallet/transactions?limit=100", token)
                val invites = AuthApi.get("/api/agency/invitations/incoming", token)
                val agency = AuthApi.get("/api/agency/membership", token)
                runOnUiThread {
                    if (w.status !in 200..299) {
                        showError(w.body.optString("error", "تعذر تحميل البيانات"))
                        return@runOnUiThread
                    }
                    wallet = w.body.optJSONObject("wallet") ?: w.body
                    history = h.body.optJSONArray("transactions") ?: JSONArray()
                    incomingInvitations =
                        if (invites.status in 200..299) invites.body.optJSONArray("invitations") ?: JSONArray()
                        else JSONArray()
                    invitationLoadError =
                        if (invites.status in 200..299) ""
                        else invites.body.optString("error", "تعذر تحميل دعوات الوكالة")
                    agencyMembership =
                        if (agency.status in 200..299) agency.body.optJSONObject("membership") ?: JSONObject()
                        else JSONObject()
                    render()
                }
            } catch (e: Exception) {
                runOnUiThread { showError(e.message ?: "تعذر الاتصال بالخادم") }
            }
        }
    }

    private fun showError(s: String) {
        content.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(100), dp(8), 0)
        }
        box.addView(badge("alert", danger, dangerSoft, 30, 22), sz(60))
        box.addView(label("حدث خطأ", 17f, ink, 2), lp(t = 16))
        box.addView(label(s, 13.5f, muted).apply { gravity = Gravity.CENTER }, lp(t = 6))
        box.addView(button("إعادة المحاولة") { load() }, lp(t = 22))
        content.addView(box)
    }

    /* ─────────── UI ─────────── */

    private fun section(title: String, subtitle: String = "") {
        content.addView(label(title, 17f, ink, 2), lp(t = 28))
        if (subtitle.isNotBlank()) content.addView(label(subtitle, 12.5f, muted), lp(t = 3, b = 12))
    }

    private fun stat(kind: String, tint: Int, soft: Int, title: String, value: String): LinearLayout =
        card(14).apply {
            addView(badge(kind, tint, soft, 20, 12), sz(38))
            addView(label(title, 12.5f, muted, 1), lp(t = 12))
            addView(label(value, 20f, ink, 2).apply { textDirection = View.TEXT_DIRECTION_LTR }, lp(t = 4))
        }

    private fun render() {
        content.removeAllViews()

        // ── Hero: balance
        val hero = HeroLayout(this).apply {
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#5B21B6"), Color.parseColor("#3B1580"))
            ).apply { cornerRadius = dpf(28) }
            elevation = dpf(6)
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(badge("wallet", Color.WHITE, Color.argb(45, 255, 255, 255), 20, 12), sz(38))
        top.addView(label("أرباح صانع المحتوى", 14f, Color.parseColor("#EDE4FF"), 1), lp(0, -2, s = 10, wt = 1f))
        top.addView(
            label("BAK LIVE", 10f, Color.WHITE, 2).apply {
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = rect(Color.argb(45, 255, 255, 255), r = 10)
                letterSpacing = 0.1f
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        hero.addView(top)
        hero.addView(label("الرصيد المتاح للسحب", 12.5f, Color.parseColor("#D9C8F5"), 1), lp(t = 24))

        val available = wallet.optLong("withdrawable_usd_cents")
        val txt = fmt(available)
        val dotAt = txt.indexOf('.')
        val amountText = SpannableString("$" + txt).apply {
            if (dotAt >= 0) setSpan(RelativeSizeSpan(0.55f), dotAt + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        hero.addView(
            label(amountText, 42f, Color.WHITE, 2).apply { textDirection = View.TEXT_DIRECTION_LTR },
            lp(t = 4)
        )

        val withdrawBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(52)
            isClickable = true
            background = ripple(rect(Color.WHITE, r = 16), 16, Color.argb(50, 109, 40, 217))
            setOnClickListener { showWithdrawal() }
            addView(icon("upright", brand), sz(18))
            addView(label("طلب سحب الأرباح", 15f, brand, 2), lp(s = 8))
        }
        hero.addView(withdrawBtn, lp(t = 22))
        content.addView(hero)

        // ── Agency membership
        renderAgencyMembershipCard()

        // ── Stats
        val stats = LinearLayout(this)
        stats.addView(
            stat("diamond", brand, brandSoft, "الألماس المكتسب",
                String.format(Locale.US, "%,d", wallet.optLong("earned_diamonds"))),
            lp(0, -2, e = 6, wt = 1f)
        )
        stats.addView(
            stat("trend", green, greenSoft, "إجمالي الأرباح",
                "$" + fmt(wallet.optLong("lifetime_earned_usd_cents"))),
            lp(0, -2, s = 6, wt = 1f)
        )
        content.addView(stats, lp(t = 14))

        // ── Agency invitations (hidden for active members)
        if (agencyMembership.length() == 0) {
            section("دعوات الوكالة", "راجع الدعوات المرسلة إليك من الوكالات")
            if (invitationLoadError.isNotBlank()) {
                val c = card(18).apply { gravity = Gravity.CENTER_HORIZONTAL }
                c.addView(badge("alert", danger, dangerSoft, 24, 18), sz(48))
                c.addView(label("تعذر تحميل الدعوات", 15f, ink, 2), lp(t = 12))
                c.addView(label(invitationLoadError, 12.5f, muted).apply { gravity = Gravity.CENTER }, lp(t = 4))
                c.addView(button("إعادة تحميل الدعوات", false) { load() }, lp(t = 14))
                content.addView(c)
            } else if (incomingInvitations.length() == 0) {
                val c = card(22).apply { gravity = Gravity.CENTER_HORIZONTAL }
                c.addView(badge("building", brand, brandSoft, 26, 18), sz(52))
                c.addView(label("لا توجد دعوات وكالة حالياً", 15f, ink, 2), lp(t = 12))
                c.addView(
                    label("عند وصول دعوة، ستظهر تفاصيل الوكالة هنا ويمكنك قبولها أو رفضها.", 12.5f, muted)
                        .apply { gravity = Gravity.CENTER },
                    lp(t = 4)
                )
                content.addView(c)
            } else {
                for (i in 0 until incomingInvitations.length()) {
                    val invite = incomingInvitations.optJSONObject(i) ?: continue
                    val inviteId = invite.optLong("id")
                    val agencyName = invite.optString("agency_name").ifBlank { "وكالة BAK LIVE" }
                    val created = fmtDate(invite.optString("created_at"))

                    val c = card(16)
                    val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                    head.addView(badge("building", brand, brandSoft, 22, 14), sz(46))
                    val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    info.addView(label(agencyName, 16f, ink, 2))
                    info.addView(label("دعوة للانضمام إلى الوكالة", 12.5f, muted), lp(t = 3))
                    if (created.isNotBlank()) info.addView(label(created, 11f, muted), lp(t = 3))
                    head.addView(info, lp(0, -2, s = 12, wt = 1f))
                    c.addView(head)

                    val actions = LinearLayout(this)
                    actions.addView(button("قبول الدعوة") { respondInvitation(inviteId, "accept") }, lp(0, -2, e = 6, wt = 1f))
                    actions.addView(button("رفض", false) { respondInvitation(inviteId, "reject") }, lp(0, -2, s = 6, wt = 1f))
                    c.addView(actions, lp(t = 14))
                    content.addView(c, lp(b = 10))
                }
            }
        }

        // ── Gift earnings
        section("أرباح الهدايا", "آخر العمليات التي حققت لك ألماساً وأرباحاً")
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rect(Color.WHITE, hairline, 20)
            elevation = dpf(2)
        }
        var found = 0
        for (i in 0 until history.length()) {
            val t = history.optJSONObject(i) ?: continue
            if (t.optString("transaction_type") != "gift_received") continue
            if (found > 0) {
                list.addView(
                    View(this).apply { setBackgroundColor(hairline) },
                    LinearLayout.LayoutParams(-1, dp(1)).apply { marginStart = dp(70) }
                )
            }
            found++
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            row.addView(badge("gift", green, greenSoft, 22, 14), sz(42))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(label("هدية مستلمة", 14.5f, ink, 2))
            info.addView(label(fmtDate(t.optString("created_at")), 11.5f, muted), lp(t = 3))
            row.addView(info, lp(0, -2, s = 12, wt = 1f))
            val amt = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            amt.addView(label("+" + String.format(Locale.US, "%,d", t.optLong("amount")), 15.5f, green, 2))
            amt.addView(icon("diamond", green), LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginStart = dp(5) })
            row.addView(amt, LinearLayout.LayoutParams(-2, -2))
            list.addView(row)
        }
        if (found == 0) {
            val empty = card(24).apply { gravity = Gravity.CENTER_HORIZONTAL }
            empty.addView(badge("diamond", brand, brandSoft, 26, 18), sz(52))
            empty.addView(label("لا توجد هدايا مكتسبة حتى الآن", 14f, muted, 1), lp(t = 12))
            content.addView(empty)
        } else {
            content.addView(list)
        }

        // ── Footer
        content.addView(button("تحديث البيانات", false) { load() }, lp(t = 26))
        content.addView(
            label("BAK LIVE  •  Creator Studio", 10.5f, Color.parseColor("#94A3B8"), 1)
                .apply { gravity = Gravity.CENTER; letterSpacing = 0.08f },
            lp(t = 18)
        )
    }

    private fun renderAgencyMembershipCard() {
        if (agencyMembership.length() == 0) return
        val agencyName = agencyMembership.optString("agency_name").ifBlank { "وكالة" }
        val ownerName = agencyMembership.optString("owner_name").ifBlank { "غير متوفر" }
        val ownerId = agencyMembership.optString("owner_public_id").ifBlank { "—" }
        val joined = agencyMembership.optString("joined_at").takeIf { it.isNotBlank() && it != "null" }?.take(10) ?: "—"

        val c = card(18)
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(badge("building", brand, brandSoft, 22, 14), sz(46))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(label("عضوية الوكالة", 12f, muted, 1))
        info.addView(label(agencyName, 17f, ink, 2), lp(t = 2))
        head.addView(info, lp(0, -2, s = 12, wt = 1f))
        head.addView(
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(12), dp(6))
                background = rect(greenSoft, r = 20)
                addView(icon("check", green), sz(14))
                addView(label("نشطة", 11.5f, green, 2), lp(s = 4))
            },
            LinearLayout.LayoutParams(-2, -2)
        )
        c.addView(head)
        c.addView(View(this).apply { setBackgroundColor(hairline) }, lp(-1, dp(1), t = 16, b = 14))

        fun infoRow(title: String, value: String, last: Boolean = false) {
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(label(title, 13f, muted), lp(0, -2, wt = 1f))
            row.addView(label(value, 13.5f, ink, 2).apply { textDirection = View.TEXT_DIRECTION_LOCALE })
            c.addView(row, lp(b = if (last) 0 else 12))
        }
        infoRow("صاحب الوكالة", ownerName)
        infoRow("ID الوكيل", ownerId)
        infoRow("تاريخ الانضمام", joined, true)
        content.addView(c, lp(t = 14))
    }

    /* ─────────── withdrawal ─────────── */

    private fun showWithdrawal() {
        val available = wallet.optLong("withdrawable_usd_cents")
        if (available <= 0) {
            notice("لا يوجد رصيد قابل للسحب حالياً.")
            return
        }
        val methods = listOf("zain_cash", "asia_hawala", "usdt")
        val names = listOf("زين كاش", "آسيا حوالة", "USDT")
        var selected = 0

        sheet { box, d ->
            // Title row
            val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            head.addView(label("طلب سحب", 19f, ink, 2), lp(0, -2, wt = 1f))
            head.addView(
                FrameLayout(this).apply {
                    background = ripple(rect(surface, r = 12), 12, brandSoft)
                    isClickable = true
                    setOnClickListener { d.dismiss() }
                    addView(icon("close", muted), FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
                },
                sz(36)
            )
            box.addView(head)

            // Available balance
            val avail = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rect(greenSoft, r = 14)
            }
            avail.addView(label("المتاح للسحب", 13f, green, 1), lp(0, -2, wt = 1f))
            avail.addView(
                label("$" + fmt(available), 17f, green, 2).apply { textDirection = View.TEXT_DIRECTION_LTR }
            )
            box.addView(avail, lp(t = 16))

            // Amount
            val amountHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            amountHead.addView(label("المبلغ (USD)", 13f, muted, 1), lp(0, -2, wt = 1f))
            val amount = field("مثال 10.50").apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                textDirection = View.TEXT_DIRECTION_LTR
            }
            amountHead.addView(
                label("الأقصى", 13f, brand, 2).apply {
                    isClickable = true
                    setOnClickListener { amount.setText(fmt(available).replace(",", "")) }
                }
            )
            box.addView(amountHead, lp(t = 18, b = 8))
            box.addView(amount, lp())

            // Method chips
            box.addView(label("طريقة الاستلام", 13f, muted, 1), lp(t = 18, b = 8))
            val chips = mutableListOf<TextView>()
            fun paint() {
                chips.forEachIndexed { i, c ->
                    val on = i == selected
                    c.setTextColor(if (on) Color.WHITE else ink)
                    c.background = if (on) rect(brand, r = 12) else rect(surface, hairline, 12)
                }
            }
            val chipRow = LinearLayout(this)
            names.forEachIndexed { i, n ->
                val chip = label(n, 13.5f, ink, 1).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(6), dp(13), dp(6), dp(13))
                    isClickable = true
                    setOnClickListener { selected = i; paint() }
                }
                chips.add(chip)
                chipRow.addView(chip, lp(0, -2, s = if (i == 0) 0 else 8, wt = 1f))
            }
            paint()
            box.addView(chipRow)

            // Destination
            box.addView(label("رقم المحفظة أو عنوان الاستلام", 13f, muted, 1), lp(t = 18, b = 8))
            val destination = field("أدخل رقم المحفظة أو العنوان")
            box.addView(destination, lp())

            val err = label("", 12.5f, danger, 1).apply { visibility = View.GONE }
            box.addView(err, lp(t = 12))

            // Actions
            val actions = LinearLayout(this)
            actions.addView(button("إلغاء", false) { d.dismiss() }, lp(0, -2, e = 6, wt = 1f))
            actions.addView(
                button("إرسال الطلب") {
                    val dollars = amount.text.toString().trim().replace(",", ".").toBigDecimalOrNull()
                    val cents = try { dollars?.movePointRight(2)?.longValueExact() } catch (_: Exception) { null }
                    val dest = destination.text.toString().trim()
                    if (cents == null || cents <= 0 || cents > available || dest.isBlank()) {
                        err.text = "أدخل مبلغاً صحيحاً وعنوان استلام، ولا يتجاوز الرصيد المتاح."
                        err.visibility = View.VISIBLE
                    } else {
                        d.dismiss()
                        submitWithdrawal(
                            JSONObject()
                                .put("amount_usd_cents", cents)
                                .put("method", methods[selected])
                                .put("destination", dest)
                        )
                    }
                },
                lp(0, -2, s = 6, wt = 1f)
            )
            box.addView(actions, lp(t = 22))
        }.show()
    }

    private fun submitWithdrawal(payload: JSONObject) {
        thread {
            try {
                val r = AuthApi.post("/api/wallet/withdrawals", payload, token)
                runOnUiThread {
                    val msg = if (r.status in 200..299) r.body.optString("message", "تم إرسال طلب السحب للمراجعة")
                    else r.body.optString("error", "تعذر تنفيذ الطلب")
                    notice(msg) { load() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر الاتصال", Toast.LENGTH_LONG).show() }
            }
        }
    }

    /* ─────────── agency invitations ─────────── */

    private fun respondInvitation(invitationId: Long, action: String) {
        if (invitationId <= 0) return
        thread {
            try {
                val response = AuthApi.post(
                    "/api/agency/invitations/$invitationId/respond",
                    JSONObject().put("action", action),
                    token
                )
                runOnUiThread {
                    if (response.status in 200..299) {
                        Toast.makeText(
                            this,
                            if (action == "accept") "تم قبول دعوة الوكالة" else "تم رفض الدعوة",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            response.body.optString("error", "تعذر معالجة الدعوة"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    load()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, e.message ?: "تعذر الاتصال بالخادم", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}