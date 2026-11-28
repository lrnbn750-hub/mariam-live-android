package com.mariam.live

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * SVIP loyalty points: balance, temporary unban, permanent-ban review and room trend support.
 * Premium dark/gold design: tinted hero balance card, segmented actions, bottom-sheet confirmations,
 * iconified history. All endpoints and business rules are unchanged.
 */
class SvipPointsActivity : Activity() {

    // ───────────────────────── Design tokens ─────────────────────────
    private val bgInk = 0xFF0D1018.toInt()
    private val surface = 0xFF161A26.toInt()
    private val surfaceHi = 0xFF1D2231.toInt()
    private val fieldBg = 0xFF11151F.toInt()
    private val stroke = 0xFF2A3043.toInt()
    private val gold = 0xFFE9CE92.toInt()
    private val goldDeep = 0xFFC79D4F.toInt()
    private val onGold = 0xFF2D2411.toInt()
    private val textPrimary = 0xFFF5F1E8.toInt()
    private val textSecondary = 0xFF9BA4B9.toInt()
    private val textTertiary = 0xFF636C82.toInt()
    private val green = Color.rgb(91, 218, 164)
    private val red = Color.rgb(255, 126, 137)

    // ───────────────────────── State ─────────────────────────
    private val io = Executors.newSingleThreadExecutor()
    private var token = ""
    private var balance = 0L
    private var balanceLoaded = false
    private var shownBalance = 0L
    private var busy = false
    private var refreshing = false
    private var tab = 0
    private var topInset = 0
    private var botInset = 0
    private var balAnim: ValueAnimator? = null
    private var spinAnim: ValueAnimator? = null
    private val buttons = mutableListOf<Pair<TextView, String>>()

    // ───────────────────────── Views ─────────────────────────
    private lateinit var rootView: FrameLayout
    private lateinit var scroll: ScrollView
    private lateinit var toolbar: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var heroSpacer: View
    private lateinit var balanceView: TextView
    private lateinit var refreshIcon: IconView
    private lateinit var segRow: LinearLayout
    private lateinit var panelHolder: FrameLayout
    private lateinit var historyBox: LinearLayout
    private lateinit var unbanId: EditText
    private lateinit var reviewId: EditText
    private lateinit var roomOwnerId: EditText
    private val panels = arrayOfNulls<View>(3)
    private var toastView: TextView? = null
    private val hideToast = Runnable { toastView?.animate()?.alpha(0f)?.setDuration(200)?.start() }

    // ───────────────────────── Lifecycle ─────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_LONG).show()
            finish(); return
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bgInk
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        buildPage()
        loadPoints()
    }

    override fun onDestroy() {
        balAnim?.cancel(); spinAnim?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    // ───────────────────────── UI helpers ─────────────────────────
    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()
    private fun dp(v: Int) = dp(v.toFloat())
    private fun fmt(v: Long) = NumberFormat.getNumberInstance(Locale.US).format(v)
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)
    private fun alphaOf(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun shape(fill: Int, radius: Float, strokeColor: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(radius).toFloat()
        if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
    }

    private fun oval(fill: Int, strokeColor: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(fill)
        if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
    }

    private fun gradient(
        colors: IntArray, radius: Float,
        o: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
        strokeColor: Int = Color.TRANSPARENT
    ) = GradientDrawable(o, colors).apply {
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
    }

    private fun ripple(content: Drawable, radius: Float): Drawable =
        RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), content, shape(Color.WHITE, radius))

    private fun View.pressable() {
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
    }

    private fun tv(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun col() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }

    private fun iconCircle(kind: String, size: Int, tint: Int, fillAlpha: Int = 30): FrameLayout =
        FrameLayout(this).apply {
            background = oval(alphaOf(tint, fillAlpha), alphaOf(tint, 110))
            addView(IconView(this@SvipPointsActivity, kind).apply { this.tint = tint },
                FrameLayout.LayoutParams(dp(size * 0.58f), dp(size * 0.58f), Gravity.CENTER))
        }

    private fun chip(text: String, color: Int) = tv(text, 11f, color, true).apply {
        gravity = Gravity.CENTER; setPadding(dp(10), dp(4), dp(10), dp(4))
        background = shape(alphaOf(color, 26), 12f, alphaOf(color, 90))
    }

    // ───────────────────────── Page construction ─────────────────────────
    private fun buildPage() {
        val root = FrameLayout(this).apply { setBackgroundColor(bgInk); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        rootView = root
        val pad = dp(16)

        scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER; isVerticalScrollBarEnabled = false
            setOnScrollChangeListener { _, _, y, _, _ ->
                val a = (y / dp(110f).toFloat()).coerceIn(0f, 1f)
                toolbar.setBackgroundColor(Color.argb((a * 245).toInt(), 13, 16, 24)); toolbarTitle.alpha = a
            }
        }
        val content = col().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(alphaOf(gold, 38), bgInk, bgInk))
        }
        heroSpacer = View(this); content.addView(heroSpacer, lp(-1, dp(56)))

        // ── Large title ──
        content.addView(tv("نقاط SVIP", 28f, textPrimary, true).apply { setPadding(pad, dp(4), pad, dp(12)) }, lp(-1, -2))

        // ── Balance card ──
        val card = FrameLayout(this).apply {
            background = gradient(intArrayOf(0xFF3A2E20.toInt(), 0xFF1B1E2B.toInt()), 24f, GradientDrawable.Orientation.TL_BR, alphaOf(gold, 120))
            clipToOutline = true; elevation = dp(6).toFloat()
        }
        card.addView(HaloView(this), FrameLayout.LayoutParams(-1, -1))
        val inner = col().apply { setPadding(dp(20), dp(18), dp(20), dp(16)) }
        val cap = row()
        cap.addView(iconCircle("star", 28, gold), lp(dp(28), dp(28)))
        cap.addView(tv("رصيد نقاط المكافآت", 13f, textSecondary, true), lp(-2, -2).apply { marginStart = dp(8) })
        inner.addView(cap)
        balanceView = tv("…", 44f, gold, true).apply { setShadowLayer(dp(10).toFloat(), 0f, 0f, alphaOf(gold, 90)) }
        inner.addView(balanceView, lp(-1, dp(66)).apply { topMargin = dp(4) })
        val note = row()
        note.addView(IconView(this, "info").apply { tint = textTertiary }, lp(dp(14), dp(14)))
        note.addView(tv("النقاط دائمة ولا تتحول إلى عملات أو ألماس", 11.5f, textSecondary), lp(-2, -2).apply { marginStart = dp(6) })
        inner.addView(note)
        inner.addView(View(this).apply { setBackgroundColor(alphaOf(gold, 40)) }, lp(-1, 1).apply { topMargin = dp(14); bottomMargin = dp(12) })
        val costs = row()
        costs.addView(chip("مراجعة الحظر الدائم · 75", gold), lp(-2, -2))
        costs.addView(chip("دعم الترند · 20", gold), lp(-2, -2).apply { marginStart = dp(8) })
        inner.addView(costs)
        card.addView(inner, FrameLayout.LayoutParams(-1, -2))
        content.addView(card, lp(-1, -2).apply { marginStart = pad; marginEnd = pad })

        // ── Segmented control ──
        segRow = row().apply { background = shape(surface, 18f, stroke); setPadding(dp(4), dp(4), dp(4), dp(4)) }
        content.addView(segRow, lp(-1, dp(52)).apply { marginStart = pad; marginEnd = pad; topMargin = dp(18) })

        // ── Panels ──
        panelHolder = FrameLayout(this)
        content.addView(panelHolder, lp(-1, -2).apply { marginStart = pad; marginEnd = pad; topMargin = dp(12) })
        panels[0] = buildUnbanPanel(); panels[1] = buildReviewPanel(); panels[2] = buildTrendPanel()

        // ── History ──
        val history = col().apply { background = shape(surface, 20f, stroke); setPadding(dp(16), dp(16), dp(16), dp(8)) }
        val hh = row()
        hh.addView(iconCircle("clock", 36, textSecondary, 24), lp(dp(36), dp(36)))
        val hc = col()
        hc.addView(tv("سجل النقاط", 16f, textPrimary, true))
        hc.addView(tv("آخر 50 حركة على حسابك", 11.5f, textSecondary))
        hh.addView(hc, lp(0, -2, 1f).apply { marginStart = dp(12) })
        history.addView(hh)
        historyBox = col()
        historyBox.addView(tv("جارٍ التحميل…", 12f, textTertiary).apply { gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(24)) }, lp(-1, -2))
        history.addView(historyBox, lp(-1, -2).apply { topMargin = dp(10) })
        content.addView(history, lp(-1, -2).apply { marginStart = pad; marginEnd = pad; topMargin = dp(18); bottomMargin = dp(28) })

        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        // ── Toolbar ──
        toolbar = row().apply { setPadding(dp(12), 0, dp(12), 0) }
        val back = TextView(this).apply {
            text = "›"; textSize = 26f; setTextColor(textPrimary); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(3))
            background = ripple(oval(0x55000000), 20f); setOnClickListener { finish() }; pressable()
        }
        toolbar.addView(back, lp(dp(40), dp(40)))
        toolbarTitle = tv("نقاط SVIP", 17f, textPrimary, true).apply { gravity = Gravity.CENTER; alpha = 0f }
        toolbar.addView(toolbarTitle, lp(0, dp(40), 1f))
        refreshIcon = IconView(this, "refresh").apply { tint = gold }
        val refresh = FrameLayout(this).apply {
            background = ripple(oval(0x55000000), 20f); setOnClickListener { if (!refreshing) loadPoints() }; pressable()
            addView(refreshIcon, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        }
        toolbar.addView(refresh, lp(dp(40), dp(40)))
        root.addView(toolbar, FrameLayout.LayoutParams(-1, dp(56), Gravity.TOP))

        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { _, ins ->
            topInset = ins.systemWindowInsetTop; botInset = ins.systemWindowInsetBottom
            toolbar.setPadding(dp(12), topInset, dp(12), 0)
            toolbar.layoutParams = (toolbar.layoutParams as FrameLayout.LayoutParams).apply { height = dp(56) + topInset }
            heroSpacer.layoutParams = heroSpacer.layoutParams.apply { height = dp(56) + topInset }
            scroll.setPadding(0, 0, 0, botInset); scroll.clipToPadding = false
            ins
        }
        setContentView(root)
        root.requestApplyInsets()
        renderSegments(); showPanel(0, animate = false)
    }

    // ───────────────────────── Segmented control ─────────────────────────
    private val segTitles = arrayOf("فك الحظر" to "unlock", "مراجعة" to "shield", "ترند الغرف" to "trend")

    private fun renderSegments() {
        segRow.removeAllViews()
        segTitles.forEachIndexed { i, (title, icon) ->
            val sel = i == tab
            val seg = row().apply {
                gravity = Gravity.CENTER
                background = if (sel) gradient(intArrayOf(gold, goldDeep), 14f) else null
                setOnClickListener { if (i != tab) { tab = i; renderSegments(); showPanel(i, true) } }
                if (!sel) setBackgroundColor(Color.TRANSPARENT)
            }
            seg.addView(IconView(this, icon).apply { tint = if (sel) onGold else textSecondary }, lp(dp(16), dp(16)))
            seg.addView(tv(title, 12.5f, if (sel) onGold else textSecondary, sel), lp(-2, -2).apply { marginStart = dp(5) })
            segRow.addView(seg, lp(0, -1, 1f))
        }
    }

    private fun showPanel(i: Int, animate: Boolean) {
        panelHolder.removeAllViews()
        val p = panels[i] ?: return
        panelHolder.addView(p, FrameLayout.LayoutParams(-1, -2))
        if (animate) { p.alpha = 0f; p.translationY = dp(10).toFloat(); p.animate().alpha(1f).translationY(0f).setDuration(220).start() }
    }

    // ───────────────────────── Panels ─────────────────────────
    private fun panelCard(icon: String, title: String, desc: String, facts: List<String>): LinearLayout {
        val box = col().apply { background = shape(surface, 20f, stroke); setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val head = row()
        head.addView(iconCircle(icon, 44, gold), lp(dp(44), dp(44)))
        head.addView(tv(title, 16.5f, textPrimary, true), lp(0, -2, 1f).apply { marginStart = dp(12) })
        box.addView(head)
        box.addView(tv(desc, 12.5f, textSecondary).apply { setLineSpacing(dp(3).toFloat(), 1.1f) }, lp(-1, -2).apply { topMargin = dp(10) })
        val fr = row()
        facts.forEachIndexed { i, f -> fr.addView(chip(f, textSecondary), lp(-2, -2).apply { if (i > 0) marginStart = dp(6) }) }
        box.addView(fr, lp(-1, -2).apply { topMargin = dp(12) })
        return box
    }

    private fun fieldRow(hint: String): Pair<LinearLayout, EditText> {
        val wrap = row().apply { background = shape(fieldBg, 14f, stroke); setPadding(dp(12), 0, dp(6), 0) }
        val et = EditText(this).apply {
            this.hint = hint; setHintTextColor(textTertiary); setTextColor(textPrimary); textSize = 14.5f
            setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT; imeOptions = EditorInfo.IME_ACTION_DONE
            textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.CENTER_VERTICAL or Gravity.START
            background = null; setPadding(0, 0, 0, 0)
            onFocusChangeListener = View.OnFocusChangeListener { _, f -> wrap.background = shape(fieldBg, 14f, if (f) gold else stroke) }
        }
        wrap.addView(et, lp(0, dp(52), 1f))
        val paste = row().apply {
            gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6))
            background = ripple(shape(alphaOf(gold, 24), 10f), 10f)
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this@SvipPointsActivity)?.toString().orEmpty().trim()
                if (t.isNotEmpty()) { et.setText(t); et.setSelection(t.length) } else showMessage("الحافظة فارغة", true)
            }
            addView(IconView(this@SvipPointsActivity, "paste").apply { tint = gold }, lp(dp(15), dp(15)))
            addView(tv("لصق", 12f, gold, true), lp(-2, -2).apply { marginStart = dp(4) })
        }
        wrap.addView(paste)
        return wrap to et
    }

    private fun primaryButton(title: String, action: () -> Unit): TextView {
        val b = tv(title, 14.5f, onGold, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0)
            background = ripple(gradient(intArrayOf(gold, goldDeep), 16f), 16f)
            setOnClickListener { action() }; pressable()
        }
        buttons.add(b to title)
        return b
    }

    private fun updateButtons() {
        buttons.forEach { (b, title) -> b.text = if (busy) "جارٍ التنفيذ…" else title; b.isEnabled = !busy; b.alpha = if (busy) .6f else 1f }
    }

    private fun buildUnbanPanel(): View {
        val box = panelCard("unlock", "فك الحظر المؤقت",
            "أدخل Public ID لأي حساب محظور مؤقتاً. افحص تفاصيل الحظر ورصيدك أولاً، ثم أكّد فك الحظر.",
            listOf("معاينة بدون خصم", "تأكيد منك أولاً"))
        val (wrap, et) = fieldRow("Public ID للحساب المراد فك حظره"); unbanId = et
        box.addView(wrap, lp(-1, dp(52)).apply { topMargin = dp(14) })
        box.addView(primaryButton("معاينة الحظر والتكلفة") { previewBanAction(false, unbanId) }, lp(-1, dp(52)).apply { topMargin = dp(12) })
        return box
    }

    private fun buildReviewPanel(): View {
        val box = panelCard("shield", "مراجعة الحظر الدائم",
            "يمكنك تقديم طلب مراجعة لحظر دائم عن طريق Public ID. لا تُخصم 75 نقطة عند الإرسال؛ الخصم فقط بعد موافقة المشرف.",
            listOf("لا خصم عند الإرسال", "75 نقطة عند الموافقة"))
        val (wrap, et) = fieldRow("Public ID للحساب المحظور"); reviewId = et
        box.addView(wrap, lp(-1, dp(52)).apply { topMargin = dp(14) })
        box.addView(primaryButton("معاينة الحظر وإرسال مراجعة") { previewBanAction(true, reviewId) }, lp(-1, dp(52)).apply { topMargin = dp(12) })
        return box
    }

    private fun buildTrendPanel(): View {
        val box = panelCard("trend", "دعم ترند الغرف",
            "أدخل Public ID لمالك الغرفة النشطة. التكلفة 20 نقطة ويستمر الدعم ساعتين. لا يمكنك دعم غرفتك.",
            listOf("20 نقطة", "ساعتان", "ليست غرفتك"))
        val (wrap, et) = fieldRow("Public ID لصاحب الغرفة"); roomOwnerId = et
        box.addView(wrap, lp(-1, dp(52)).apply { topMargin = dp(14) })
        box.addView(primaryButton("دعم الترند بـ 20 نقطة") { requestTrendSupport() }, lp(-1, dp(52)).apply { topMargin = dp(12) })
        return box
    }

    // ───────────────────────── Sheets & toast ─────────────────────────
    private fun showSheet(build: (LinearLayout, Dialog) -> Unit) {
        val d = Dialog(this); d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val r = dp(26f).toFloat()
        val box = col().apply {
            background = GradientDrawable().apply { setColor(surface); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
            setPadding(dp(18), dp(10), dp(18), dp(22) + botInset)
        }
        box.addView(View(this).apply { background = shape(0xFF464E63.toInt(), 5f) }, lp(dp(40), dp(4)).apply { gravity = Gravity.CENTER; bottomMargin = dp(14) })
        build(box, d)
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.BOTTOM); setDimAmount(.6f)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        d.show()
        box.translationY = dp(360).toFloat()
        box.animate().translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun sheetActions(box: LinearLayout, d: Dialog, confirmLabel: String, enabled: Boolean, onConfirm: () -> Unit) {
        val actions = row().apply { layoutDirection = View.LAYOUT_DIRECTION_LTR }
        val cancel = tv("إلغاء", 14f, 0xFFC4CBDA.toInt(), true).apply {
            gravity = Gravity.CENTER; background = ripple(shape(Color.TRANSPARENT, 25f, 0xFF4A5268.toInt()), 25f)
            setOnClickListener { d.dismiss() }; pressable()
        }
        val confirm = tv(confirmLabel, 14f, onGold, true).apply {
            gravity = Gravity.CENTER; alpha = if (enabled) 1f else .45f
            background = ripple(gradient(intArrayOf(gold, goldDeep), 25f), 25f)
            setOnClickListener { if (enabled) { d.dismiss(); onConfirm() } }; pressable()
        }
        actions.addView(cancel, lp(0, dp(50), 1f)); actions.addView(confirm, lp(0, dp(50), 1.6f).apply { marginStart = dp(10) })
        box.addView(actions, lp(-1, dp(50)).apply { topMargin = dp(18) })
    }

    private fun costBlock(box: LinearLayout, required: Long, available: Long, sufficient: Boolean) {
        val c = col().apply { background = shape(surfaceHi, 16f, stroke); setPadding(dp(14), dp(14), dp(14), dp(14)) }
        val top = row()
        top.addView(tv("النقاط المطلوبة", 12.5f, textSecondary), lp(0, -2, 1f)); top.addView(tv(fmt(required), 14f, textPrimary, true))
        c.addView(top)
        val mid = row()
        mid.addView(tv("رصيدك الحالي", 12.5f, textSecondary), lp(0, -2, 1f))
        mid.addView(tv(fmt(available), 14f, if (sufficient) green else red, true))
        c.addView(mid, lp(-1, -2).apply { topMargin = dp(8) })
        val ratio = if (required <= 0) 1f else (available.toFloat() / required).coerceIn(0f, 1f)
        val track = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; background = shape(fieldBg, 4f) }
        track.addView(View(this).apply { background = shape(if (sufficient) green else red, 4f) }, lp(0, -1, ratio))
        track.addView(View(this), lp(0, -1, 1f - ratio))
        c.addView(track, lp(-1, dp(6)).apply { topMargin = dp(12) })
        if (!sufficient) c.addView(tv("ينقصك ${fmt((required - available).coerceAtLeast(0))} نقطة", 11.5f, red, true), lp(-1, -2).apply { topMargin = dp(8) })
        box.addView(c, lp(-1, -2).apply { topMargin = dp(12) })
    }

    private fun detailLine(card: LinearLayout, title: String, value: String, last: Boolean = false) {
        val r = row().apply { setPadding(dp(14), dp(11), dp(14), dp(11)) }
        r.addView(tv(title, 12f, textSecondary), lp(dp(92), -2))
        r.addView(tv(value, 12.5f, textPrimary, true).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.START }, lp(0, -2, 1f))
        card.addView(r)
        if (!last) card.addView(View(this).apply { setBackgroundColor(stroke) }, lp(-1, 1).apply { marginStart = dp(14); marginEnd = dp(14) })
    }

    private fun showMessage(message: String, error: Boolean = false) {
        val tvw = toastView ?: TextView(this).also {
            toastView = it
            rootView.addView(it, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        }
        val accent = if (error) red else green
        tvw.text = message; tvw.textSize = 13f; tvw.maxWidth = dp(340); tvw.gravity = Gravity.CENTER
        tvw.setTextColor(accent)
        tvw.background = shape(surfaceHi, 16f, alphaOf(accent, 160))
        tvw.setPadding(dp(18), dp(12), dp(18), dp(12)); tvw.elevation = dp(10).toFloat()
        tvw.layoutParams = (tvw.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topInset + dp(64) }
        tvw.removeCallbacks(hideToast); tvw.animate().cancel()
        tvw.alpha = 0f; tvw.translationY = -dp(24).toFloat()
        tvw.animate().alpha(1f).translationY(0f).setDuration(240).start()
        tvw.postDelayed(hideToast, 3400)
    }

    // ───────────────────────── Actions (logic unchanged) ─────────────────────────
    private fun hideKeyboard() {
        try {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (_: Exception) { }
    }

    private fun setBusy(b: Boolean) { busy = b; updateButtons() }

    private fun runAction(path: String, payload: JSONObject, success: (JSONObject) -> String) {
        hideKeyboard(); setBusy(true)
        io.execute {
            try {
                val result = AuthApi.post(path, payload, token)
                val ok = result.status in 200..299 && result.body.optBoolean("success", false)
                val message = if (ok) success(result.body)
                    else result.body.optString("error", "تعذر تنفيذ الطلب (HTTP " + result.status + ")")
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        setBusy(false); showMessage(message, !ok)
                        if (ok) loadPoints()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) { setBusy(false); showMessage(e.message ?: "تعذر الاتصال بالخادم", true) }
                }
            }
        }
    }

    /** Preview is read-only: it never deducts points or changes the ban. */
    private fun previewBanAction(permanent: Boolean, field: EditText) {
        val id = field.text.toString().trim()
        if (id.isBlank()) { showMessage("أدخل Public ID للحساب أولاً", true); return }
        hideKeyboard(); setBusy(true)
        io.execute {
            try {
                val encoded = java.net.URLEncoder.encode(id, "UTF-8")
                val result = AuthApi.get("/api/svip/points/unban-preview?public_id=" + encoded, token)
                if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                    val error = result.body.optString("error", "تعذر جلب تفاصيل الحظر")
                    runOnUiThread { if (!isFinishing && !isDestroyed) { setBusy(false); showMessage(error, true) } }
                    return@execute
                }
                val body = result.body
                val ban = body.optJSONObject("ban") ?: JSONObject()
                val target = body.optJSONObject("target") ?: JSONObject()
                val action = body.optString("action", if (permanent) "review" else "unban")
                val isPermanent = action == "review" || permanent
                val required = body.optLong("required_points", if (isPermanent) 75 else 0)
                val available = body.optLong("available_points", 0)
                val banId = ban.optString("id", body.optString("ban_id", "")).takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                val type = ban.optString("ban_type", if (isPermanent) "permanent" else "temporary")
                val reason = ban.optString("reason").ifBlank { "لم يُذكر سبب" }
                val created = ban.optString("created_at").ifBlank { "غير متوفر" }
                val expires = ban.optString("expires_at").ifBlank { if (isPermanent) "دائم / بدون تاريخ انتهاء" else "غير متوفر" }
                val targetName = target.optString("username").ifBlank { id }
                val targetId = target.optString("public_id").ifBlank { id }
                val sufficient = body.optBoolean("points_sufficient", available >= required)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    setBusy(false)
                    if (banId.isBlank()) { showMessage("لم يتم العثور على حظر نشط لهذا الحساب", true); return@runOnUiThread }
                    showBanSheet(isPermanent, targetName, targetId, type, reason, created, expires, required, available, sufficient, banId)
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing && !isDestroyed) { setBusy(false); showMessage(e.message ?: "تعذر الاتصال بالخادم", true) } }
            }
        }
    }

    private fun showBanSheet(
        isPermanent: Boolean, name: String, id: String, type: String, reason: String,
        created: String, expires: String, required: Long, available: Long, sufficient: Boolean, banId: String
    ) = showSheet { box, d ->
        val head = row()
        head.addView(iconCircle(if (isPermanent) "shield" else "unlock", 48, gold), lp(dp(48), dp(48)))
        val hc = col()
        hc.addView(tv(if (isPermanent) "معاينة مراجعة الحظر الدائم" else "معاينة فك الحظر", 17f, textPrimary, true))
        hc.addView(tv("$name  ·  $id", 12f, textSecondary).apply { textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.START })
        head.addView(hc, lp(0, -2, 1f).apply { marginStart = dp(12) })
        val permanentType = type.equals("permanent", true) || isPermanent
        head.addView(chip(if (permanentType) "حظر دائم" else "حظر مؤقت", if (permanentType) red else gold))
        box.addView(head)

        val card = col().apply { background = shape(surfaceHi, 16f, stroke) }
        detailLine(card, "السبب", reason); detailLine(card, "تاريخ الحظر", created); detailLine(card, "الانتهاء", expires, last = true)
        box.addView(card, lp(-1, -2).apply { topMargin = dp(14) })
        costBlock(box, required, available, sufficient)
        box.addView(
            tv(if (isPermanent) "سيُرسل طلب مراجعة فقط. لا يوجد خصم عند الإرسال؛ تُخصم 75 نقطة فقط إذا وافق المشرف."
            else "لن يتم الخصم أو فك الحظر عند المعاينة. الخصم وفك الحظر يحدثان فقط بعد تأكيدك.", 11.5f, textTertiary)
                .apply { setLineSpacing(dp(2).toFloat(), 1.1f) }, lp(-1, -2).apply { topMargin = dp(12) })

        val label = when { !sufficient -> "رصيد غير كافٍ"; isPermanent -> "إرسال طلب المراجعة"; else -> "تأكيد فك الحظر وخصم النقاط" }
        sheetActions(box, d, label, sufficient) {
            if (isPermanent) runAction("/api/svip/points/ban-review", JSONObject().put("public_id", id).put("ban_id", banId)) {
                "تم إرسال طلب المراجعة. لا تُخصم النقاط إلا بعد موافقة المشرف."
            } else runAction("/api/svip/points/unban", JSONObject().put("public_id", id).put("ban_id", banId)) {
                "تم فك الحظر. النقاط المستخدمة: " + it.optString("points_spent", fmt(required))
            }
        }
    }

    private fun requestTrendSupport() {
        val id = roomOwnerId.text.toString().trim()
        if (id.isBlank()) { showMessage("أدخل رقم صاحب الغرفة أولاً", true); return }
        hideKeyboard()
        val cost = 20L
        val enough = !balanceLoaded || balance >= cost
        showSheet { box, d ->
            val head = row()
            head.addView(iconCircle("trend", 48, gold), lp(dp(48), dp(48)))
            val hc = col()
            hc.addView(tv("دعم ترند الغرفة", 17f, textPrimary, true))
            hc.addView(tv("صاحب الغرفة · $id", 12f, textSecondary).apply { textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.START })
            head.addView(hc, lp(0, -2, 1f).apply { marginStart = dp(12) })
            box.addView(head)
            val card = col().apply { background = shape(surfaceHi, 16f, stroke) }
            detailLine(card, "مدة الدعم", "ساعتان"); detailLine(card, "ملاحظة", "لا يمكنك دعم غرفتك", last = true)
            box.addView(card, lp(-1, -2).apply { topMargin = dp(14) })
            costBlock(box, cost, balance, enough)
            sheetActions(box, d, if (enough) "تأكيد الدعم بـ 20 نقطة" else "رصيد غير كافٍ", enough) {
                runAction("/api/svip/points/trend-support", JSONObject().put("owner_public_id", id)) {
                    "تم دعم الترند لمدة ساعتين. النقاط المستخدمة: 20"
                }
            }
        }
    }

    // ───────────────────────── Data ─────────────────────────
    private fun setRefreshing(on: Boolean) {
        refreshing = on
        spinAnim?.cancel()
        if (on) {
            spinAnim = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 900; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener { refreshIcon.rotation = it.animatedValue as Float }
                start()
            }
        } else refreshIcon.rotation = 0f
    }

    private fun animateBalance(to: Long) {
        val from = shownBalance
        balAnim?.cancel()
        balAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800; interpolator = DecelerateInterpolator()
            addUpdateListener {
                shownBalance = (from + (to - from) * (it.animatedValue as Float).toDouble()).toLong()
                balanceView.text = fmt(shownBalance)
            }
            start()
        }
    }

    private fun loadPoints() {
        setRefreshing(true)
        io.execute {
            try {
                val result = AuthApi.get("/api/svip/points", token)
                if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                    val error = result.body.optString("error", "تعذر تحميل نقاط SVIP")
                    runOnUiThread { if (!isFinishing && !isDestroyed) { setRefreshing(false); showMessage(error, true) } }
                    return@execute
                }
                val bal = result.body.optLong("balance", 0)
                val history = result.body.optJSONArray("history") ?: JSONArray()
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    setRefreshing(false)
                    balance = bal; balanceLoaded = true; animateBalance(bal)
                    renderHistory(history)
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing && !isDestroyed) { setRefreshing(false); showMessage(e.message ?: "تعذر تحميل الرصيد", true) } }
            }
        }
    }

    private fun renderHistory(history: JSONArray) {
        historyBox.removeAllViews()
        if (history.length() == 0) {
            val empty = col().apply { gravity = Gravity.CENTER; setPadding(0, dp(22), 0, dp(22)) }
            empty.addView(iconCircle("clock", 56, textTertiary, 20), lp(dp(56), dp(56)))
            empty.addView(tv("لا توجد حركات نقاط حتى الآن", 12.5f, textSecondary).apply { gravity = Gravity.CENTER }, lp(-1, -2).apply { topMargin = dp(10) })
            historyBox.addView(empty, lp(-1, -2)); return
        }
        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue
            val delta = item.optLong("delta")
            val type = item.optString("transaction_type", "حركة نقاط")
            val (title, icon) = when (type) {
                "purchase_reward" -> "مكافأة شراء SVIP" to "gift"
                "unban" -> "فك حظر مؤقت" to "unlock"
                "ban_review_approved" -> "مراجعة حظر - موافقة" to "shield"
                "trend_support" -> "دعم ترند غرفة" to "trend"
                else -> type to "star"
            }
            val date = item.optString("created_at").take(10).replace("-", "/")
            val positive = delta > 0
            val r = row().apply { setPadding(0, dp(11), 0, dp(11)) }
            r.addView(iconCircle(icon, 40, if (positive) green else textSecondary, 24), lp(dp(40), dp(40)))
            val c = col()
            c.addView(tv(title, 13.5f, textPrimary, true))
            if (date.isNotBlank()) c.addView(tv(date, 11f, textTertiary))
            r.addView(c, lp(0, -2, 1f).apply { marginStart = dp(12) })
            r.addView(chip((if (positive) "+" else "") + fmt(delta), if (positive) green else red).apply { textSize = 12.5f })
            historyBox.addView(r, lp(-1, -2))
            if (i != history.length() - 1) historyBox.addView(View(this).apply { setBackgroundColor(stroke) }, lp(-1, 1).apply { marginStart = dp(52) })
        }
    }

    // ───────────────────────── Custom drawn views ─────────────────────────
    /** Decorative glow + concentric rings for the balance card. */
    private class HaloView(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val cx = w * .12f; val cy = h * .1f
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(cx, cy, w * .65f, intArrayOf(Color.argb(70, 233, 206, 146), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, w * .65f, p)
            p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = resources.displayMetrics.density
            for ((i, r) in floatArrayOf(.22f, .38f, .54f).withIndex()) {
                p.color = Color.argb(34 - i * 9, 233, 206, 146); c.drawCircle(cx, cy, w * r, p)
            }
        }
    }

    private class IconView(c: Context, private val kind: String) : View(c) {
        var tint = 0xfff1ecdd.toInt()
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val s = minOf(width, height).toFloat()
            c.save(); c.translate((width - s) / 2, (height - s) / 2); c.scale(s / 24f, s / 24f)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.7f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND; p.color = tint
            val path = Path()
            when (kind) {
                "unlock" -> { c.drawRoundRect(5f, 10.5f, 19f, 20.5f, 2.5f, 2.5f, p); path.moveTo(8f, 10.5f); path.lineTo(8f, 7.5f); path.cubicTo(8f, 2.5f, 16f, 2.5f, 16f, 6.5f) }
                "shield" -> {
                    path.moveTo(12f, 3f); path.lineTo(19f, 6f); path.lineTo(19f, 11.5f); path.quadTo(19f, 18f, 12f, 21f)
                    path.quadTo(5f, 18f, 5f, 11.5f); path.lineTo(5f, 6f); path.close()
                    path.moveTo(9f, 12f); path.lineTo(11f, 14f); path.lineTo(15f, 9.5f)
                }
                "trend" -> { path.moveTo(3f, 17f); path.lineTo(9f, 11f); path.lineTo(13f, 15f); path.lineTo(21f, 7f); path.moveTo(15f, 7f); path.lineTo(21f, 7f); path.lineTo(21f, 13f) }
                "refresh" -> { c.drawArc(RectF(4f, 4f, 20f, 20f), 30f, 290f, false, p); path.moveTo(14.6f, 7.0f); path.lineTo(18.6f, 7.6f); path.lineTo(19.4f, 3.6f) }
                "gift" -> {
                    c.drawRoundRect(3.5f, 9f, 20.5f, 20.5f, 2.5f, 2.5f, p)
                    path.moveTo(3.5f, 13f); path.lineTo(20.5f, 13f); path.moveTo(12f, 9f); path.lineTo(12f, 20.5f)
                    path.moveTo(12f, 9f); path.quadTo(8f, 3.5f, 7f, 7f); path.quadTo(7f, 9f, 12f, 9f)
                    path.quadTo(16f, 3.5f, 17f, 7f); path.quadTo(17f, 9f, 12f, 9f)
                }
                "clock" -> { c.drawCircle(12f, 12f, 9f, p); path.moveTo(12f, 7f); path.lineTo(12f, 12f); path.lineTo(15.5f, 14f) }
                "paste" -> { c.drawRoundRect(6f, 5f, 18f, 21f, 2.5f, 2.5f, p); c.drawRoundRect(9f, 3f, 15f, 7f, 1.5f, 1.5f, p) }
                "info" -> { c.drawCircle(12f, 12f, 9f, p); path.moveTo(12f, 11f); path.lineTo(12f, 16.5f); c.drawCircle(12f, 7.8f, .5f, p) }
                else -> { // star
                    for (i in 0..9) {
                        val a = (-90 + i * 36) * Math.PI / 180; val r = if (i % 2 == 0) 9.5 else 4.3
                        val x = (12 + Math.cos(a) * r).toFloat(); val y = (12 + Math.sin(a) * r).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
            }
            c.drawPath(path, p); c.restore()
        }
    }
}