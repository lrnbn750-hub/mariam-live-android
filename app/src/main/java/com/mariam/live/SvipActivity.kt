package com.mariam.live

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.math.*

/** [metal, gem, ribbon] colors for each of the 7 SVIP tiers. */
private val TIERS = arrayOf(
    intArrayOf(0xffc98a54.toInt(), 0xffff9a4a.toInt(), 0xff7a3b1a.toInt()),
    intArrayOf(0xffc3ccd8.toInt(), 0xff5ec8ff.toInt(), 0xff2c4a6b.toInt()),
    intArrayOf(0xffe9b94a.toInt(), 0xff3ddc97.toInt(), 0xff1f6b4a.toInt()),
    intArrayOf(0xffefc15a.toInt(), 0xff4aa3ff.toInt(), 0xff24407a.toInt()),
    intArrayOf(0xfff2c660.toInt(), 0xffb36bff.toInt(), 0xff5a2a8a.toInt()),
    intArrayOf(0xfff4cb68.toInt(), 0xffff5fa8.toInt(), 0xff8a1f55.toInt()),
    intArrayOf(0xfff6d070.toInt(), 0xffff3b4a.toInt(), 0xff9c1424.toInt())
)

/**
 * Fully native SVIP screen (no WebView).
 * Premium dark/gold design: tier-tinted hero with swipeable emblem, pill tabs,
 * perk cards, details card, sticky purchase bar and bottom-sheet confirmation.
 */
class SvipActivity : Activity() {

    // ───────────────────────── Design tokens ─────────────────────────
    private val bgInk = 0xFF0D1018.toInt()
    private val surface = 0xFF161A26.toInt()
    private val surfaceHi = 0xFF1D2231.toInt()
    private val stroke = 0xFF2A3043.toInt()
    private val gold = 0xFFE9CE92.toInt()
    private val goldDeep = 0xFFC79D4F.toInt()
    private val onGold = 0xFF2D2411.toInt()
    private val textPrimary = 0xFFF5F1E8.toInt()
    private val textSecondary = 0xFF9BA4B9.toInt()
    private val textTertiary = 0xFF636C82.toInt()
    private val ok = Color.rgb(95, 214, 164)
    private val danger = 0xFFFF7A86.toInt()

    // ───────────────────────── Models ─────────────────────────
    private data class Pack(val level: Int, val price: Long, val active: Boolean)
    private data class Perk(val icon: String, val min: Int, val label: (Int) -> String)

    private val perks = listOf(
        Perk("entry", 1) { "مؤثرات الدخول" }, Perk("frame", 2) { "براويز مميزة" },
        Perk("vip", 1) { "شارة SVIP$it" }, Perk("follow", 1) { "المتابعة *${it * 100}%" },
        Perk("friends", 2) { "مزيد من الاصدقاء" }, Perk("car", 3) { "احصل/ي على هذه السيارة" },
        Perk("support", 2) { "دعم متخصص" }, Perk("bubble", 3) { "فقاعات دردشة" },
        Perk("card", 3) { "بطاقة تعريف مميزة" }, Perk("palette", 2) { "ألوان دردشة مميزة" },
        Perk("highlight", 4) { "رسائل بارزة في الدردشة" }, Perk("shield", 3) { "منع الطرد" },
        Perk("mute", 3) { "منع الكتم من التعليقات" }, Perk("star", 6) { "أولوية الظهور" },
        Perk("door", 6) { "غرف SVIP حصرية" }
    )

    // ───────────────────────── State ─────────────────────────
    private val io = Executors.newSingleThreadExecutor()
    private var token = ""
    private var balance = 0L
    private var owned = 0
    private var expiresAt = ""
    private var selected = 1
    private var userPicked = false
    private var loading = false
    private var buying = false
    private var packs = mutableListOf<Pack>()
    private var topInset = 0
    private var botInset = 0
    private var heroTop = 0
    private var heroAnim: ValueAnimator? = null
    private var floatAnim: ValueAnimator? = null

    // ───────────────────────── Views ─────────────────────────
    private lateinit var rootView: FrameLayout
    private lateinit var scroll: ScrollView
    private lateinit var toolbar: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var heroSpacer: View
    private lateinit var footer: LinearLayout
    private lateinit var heroBg: GradientDrawable
    private lateinit var badge: BadgeView
    private lateinit var tierTitle: TextView
    private lateinit var tierSub: TextView
    private lateinit var dots: LinearLayout
    private lateinit var scrollTabs: HorizontalScrollView
    private lateinit var tabRow: LinearLayout
    private lateinit var balanceText: TextView
    private lateinit var memberEntry: TextView
    private lateinit var perksCounter: TextView
    private lateinit var grid: LinearLayout
    private lateinit var elite: LinearLayout
    private lateinit var eliteIcon: IconView
    private lateinit var eliteState: TextView
    private lateinit var priceDetail: TextView
    private lateinit var cashbackDetail: TextView
    private lateinit var bottomPrice: TextView
    private lateinit var bottomState: TextView
    private lateinit var bottomBack: TextView
    private lateinit var buy: TextView
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
        window.navigationBarColor = surface
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        buildNativePage()
        startFloating()
        applyCached()
        loadData()
    }

    override fun onDestroy() {
        floatAnim?.cancel(); heroAnim?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    // ───────────────────────── UI helpers ─────────────────────────
    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()
    private fun dp(v: Int) = dp(v.toFloat())
    private fun fmt(v: Long) = java.text.NumberFormat.getNumberInstance(Locale.US).format(v)
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    private fun mix(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
    )

    private fun alphaOf(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun shape(fill: Int, radius: Float, strokeColor: Int = Color.TRANSPARENT, strokeW: Int = 1) =
        GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(radius).toFloat()
            if (strokeColor != Color.TRANSPARENT) setStroke(dp(strokeW), strokeColor)
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
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
    }

    private fun label(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = true
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun startFloating() {
        floatAnim = ValueAnimator.ofFloat(0f, -dp(6).toFloat()).apply {
            duration = 2400; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { badge.translationY = it.animatedValue as Float }
            start()
        }
    }

    // ───────────────────────── Page construction ─────────────────────────
    private fun buildNativePage() {
        val root = FrameLayout(this).apply { setBackgroundColor(bgInk); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        rootView = root
        val main = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        scroll = ScrollView(this).apply {
            isFillViewport = false; overScrollMode = View.OVER_SCROLL_NEVER; isVerticalScrollBarEnabled = false
            setOnScrollChangeListener { _, _, y, _, _ ->
                val a = (y / dp(140f).toFloat()).coerceIn(0f, 1f)
                toolbar.setBackgroundColor(Color.argb((a * 245).toInt(), 13, 16, 24))
                toolbarTitle.alpha = a
            }
        }
        val content = column()

        // ── Hero ──
        heroBg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0, bgInk))
        val hero = column().apply { background = heroBg; gravity = Gravity.CENTER_HORIZONTAL }
        heroSpacer = View(this); hero.addView(heroSpacer, lp(-1, dp(56)))
        badge = BadgeView(this).apply {
            setOnTouchListener(object : View.OnTouchListener {
                var down = 0f
                override fun onTouch(v: View, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> down = e.x
                        MotionEvent.ACTION_UP -> {
                            val dx = e.x - down
                            if (abs(dx) > dp(45)) select(selected + if (dx > 0) 1 else -1)
                        }
                    }
                    return true
                }
            })
        }
        hero.addView(badge, lp(-1, dp(210)))
        tierTitle = label("SVIP 1", 26f, textPrimary, true).apply { gravity = Gravity.CENTER }
        hero.addView(tierTitle, lp(-1, -2).apply { topMargin = dp(2) })
        tierSub = label("", 12.5f, textSecondary).apply { gravity = Gravity.CENTER }
        hero.addView(tierSub, lp(-1, -2).apply { topMargin = dp(2) })
        dots = row().apply { gravity = Gravity.CENTER }
        hero.addView(dots, lp(-1, dp(18)).apply { topMargin = dp(10) })

        scrollTabs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        scrollTabs.addView(tabRow)
        hero.addView(scrollTabs, lp(-1, dp(48)).apply { topMargin = dp(8) })
        content.addView(hero, lp(-1, -2))

        // ── Perks header ──
        val sectionPad = dp(14)
        val perksHead = row().apply { setPadding(sectionPad, dp(10), sectionPad, dp(8)) }
        perksHead.addView(label("الامتيازات", 16f, textPrimary, true), lp(0, dp(30), 1f))
        perksCounter = label("", 11.5f, gold, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(10), dp(3), dp(10), dp(3))
            background = shape(alphaOf(gold, 28), 12f, alphaOf(gold, 90))
        }
        perksHead.addView(perksCounter, lp(-2, -2))
        content.addView(perksHead, lp(-1, -2))

        // ── Elite card ──
        elite = row().apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = ripple(gradient(intArrayOf(0xFF2A2433.toInt(), surface), 18f, GradientDrawable.Orientation.LEFT_RIGHT, alphaOf(gold, 110)), 18f)
            setOnClickListener { showEliteInfo() }; pressable()
        }
        val eliteCircle = FrameLayout(this).apply { background = oval(alphaOf(gold, 30), alphaOf(gold, 120)) }
        eliteIcon = IconView(this, "mask").apply { tint = gold }
        eliteCircle.addView(eliteIcon, FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
        elite.addView(eliteCircle, lp(dp(52), dp(52)))
        val eliteText = column()
        eliteText.addView(label("امتيازات النخبة", 14.5f, gold, true))
        eliteText.addView(label("دخول رجل غامض الغرفة للتو.", 11.5f, textSecondary).apply { setPadding(0, dp(2), 0, 0) })
        eliteState = label("", 11f, textTertiary).apply { setPadding(0, dp(3), 0, 0) }
        eliteText.addView(eliteState)
        elite.addView(eliteText, lp(0, -2, 1f).apply { marginStart = dp(12); marginEnd = dp(8) })
        val help = TextView(this).apply {
            text = "?"; textSize = 14f; setTextColor(gold); gravity = Gravity.CENTER
            background = oval(Color.TRANSPARENT, alphaOf(gold, 140)); setOnClickListener { showEliteInfo() }
        }
        elite.addView(help, lp(dp(26), dp(26)))
        content.addView(elite, lp(-1, -2).apply { marginStart = sectionPad; marginEnd = sectionPad; bottomMargin = dp(12) })

        // ── Perk grid ──
        grid = column().apply { setPadding(sectionPad - dp(4), 0, sectionPad - dp(4), 0) }
        content.addView(grid, lp(-1, -2))

        // ── Details card ──
        val details = column().apply { background = shape(surface, 18f, stroke) }
        details.addView(label("تفاصيل العضوية", 14.5f, textPrimary, true), lp(-1, dp(46)).apply { marginStart = dp(16); marginEnd = dp(16) })
        fun divider() = details.addView(View(this).apply { setBackgroundColor(stroke) }, lp(-1, 1).apply { marginStart = dp(16); marginEnd = dp(16) })
        fun infoRow(title: String, value: TextView) {
            divider()
            val r = row().apply { setPadding(dp(16), dp(13), dp(16), dp(13)) }
            r.addView(label(title, 12.5f, textSecondary), lp(0, -2, 1f)); r.addView(value); details.addView(r)
        }
        priceDetail = label("—", 13.5f, textPrimary, true)
        cashbackDetail = label("—", 13.5f, ok, true)
        infoRow("سعر الباقة", priceDetail)
        infoRow("مدة العضوية", label("30 يوماً", 13.5f, textPrimary, true))
        infoRow("مردود المحفظة (40%)", cashbackDetail)
        details.addView(label("يتم التحقق من السعر والرصيد وتنفيذ الاشتراك من خادم BAK LIVE. مدة العضوية 30 يوماً.", 11.5f, textTertiary).apply {
            setPadding(dp(16), dp(4), dp(16), dp(16)); setLineSpacing(dp(2).toFloat(), 1.15f)
        }, lp(-1, -2).apply { topMargin = dp(6) })
        content.addView(details, lp(-1, -2).apply { marginStart = sectionPad; marginEnd = sectionPad; topMargin = dp(8); bottomMargin = dp(24) })

        scroll.addView(content)
        main.addView(scroll, lp(-1, 0, 1f))

        // ── Sticky purchase bar ──
        main.addView(View(this).apply { setBackgroundColor(stroke) }, lp(-1, 1))
        footer = row().apply { setBackgroundColor(surface); setPadding(dp(16), dp(12), dp(16), dp(12)) }
        val priceCol = column()
        val priceLine = row()
        bottomPrice = label("—", 20f, textPrimary, true)
        priceLine.addView(bottomPrice)
        priceLine.addView(DiamondView(this), lp(dp(18), dp(18)).apply { marginStart = dp(6) })
        priceLine.addView(label("/ 30 يوم", 12.5f, textSecondary).apply { layoutDirection = View.LAYOUT_DIRECTION_LTR }, lp(-2, -2).apply { marginStart = dp(6) })
        priceCol.addView(priceLine)
        bottomState = label("جارٍ تحميل الباقات…", 12f, textTertiary)
        priceCol.addView(bottomState, lp(-1, -2).apply { topMargin = dp(2) })
        bottomBack = label("", 11.5f, ok)
        priceCol.addView(bottomBack)
        footer.addView(priceCol, lp(0, -2, 1f))
        buy = label("شراء", 15f, onGold, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(28), 0, dp(28), 0)
            background = ripple(gradient(intArrayOf(gold, goldDeep), 26f), 26f)
            setOnClickListener { confirmPurchase() }; pressable()
            isEnabled = false; alpha = .5f
        }
        footer.addView(buy, lp(-2, dp(50)).apply { marginStart = dp(12) })
        main.addView(footer, lp(-1, -2))
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        // ── Floating toolbar (transparent over hero, solid after scroll) ──
        toolbar = row().apply { setPadding(dp(12), 0, dp(12), 0) }
        val back = TextView(this).apply {
            text = "›"; textSize = 26f; setTextColor(textPrimary); gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(3))
            background = ripple(oval(0x55000000), 20f); setOnClickListener { finish() }; pressable()
        }
        toolbar.addView(back, lp(dp(40), dp(40)))
        toolbarTitle = label("SVIP", 17f, textPrimary, true).apply { gravity = Gravity.CENTER; alpha = 0f }
        toolbar.addView(toolbarTitle, lp(0, dp(40), 1f))
        val wallet = row().apply { background = shape(0xAA161A26.toInt(), 30f, stroke); setPadding(dp(12), dp(7), dp(12), dp(7)) }
        wallet.addView(DiamondView(this), lp(dp(16), dp(16)))
        balanceText = label("0", 12.5f, textPrimary, true)
        wallet.addView(balanceText, lp(-2, -2).apply { marginStart = dp(7) })
        toolbar.addView(wallet)
        memberEntry = label("♛  عضويتي", 12f, gold, true).apply {
            gravity = Gravity.CENTER
            background = ripple(gradient(intArrayOf(0xFF3B3440.toInt(), 0xFF292D3D.toInt()), 24f, GradientDrawable.Orientation.LEFT_RIGHT, 0xFF806D48.toInt()), 24f)
            setPadding(dp(12), dp(7), dp(12), dp(7)); visibility = View.GONE
            setOnClickListener { startActivity(Intent(this@SvipActivity, SvipMemberActivity::class.java)) }
        }
        toolbar.addView(memberEntry, lp(-2, -2).apply { marginStart = dp(6) })
        root.addView(toolbar, FrameLayout.LayoutParams(-1, dp(56), Gravity.TOP))

        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { _, ins ->
            topInset = ins.systemWindowInsetTop; botInset = ins.systemWindowInsetBottom
            applyInsets(); ins
        }
        setContentView(root)
        root.requestApplyInsets()
        renderPerks()
    }

    private fun applyInsets() {
        toolbar.setPadding(dp(12), topInset, dp(12), 0)
        toolbar.layoutParams = (toolbar.layoutParams as FrameLayout.LayoutParams).apply { height = dp(56) + topInset }
        heroSpacer.layoutParams = heroSpacer.layoutParams.apply { height = dp(56) + topInset }
        footer.setPadding(dp(16), dp(12), dp(16), dp(12) + botInset)
    }

    // ───────────────────────── Rendering ─────────────────────────
    private fun renderTabs() {
        tabRow.removeAllViews()
        packs.sortedBy { it.level }.forEach { p ->
            val sel = p.level == selected
            val t = label("SVIP ${p.level}", 13f, if (sel) onGold else textSecondary, sel).apply {
                gravity = Gravity.CENTER; setPadding(dp(18), 0, dp(18), 0)
                background = if (sel) gradient(intArrayOf(gold, goldDeep), 20f) else shape(surface, 20f, stroke)
                setOnClickListener { select(p.level) }; pressable()
            }
            tabRow.addView(t, lp(-2, dp(38)).apply { marginStart = dp(4); marginEnd = dp(4) })
            if (sel) tabRow.post { t.requestRectangleOnScreen(Rect(0, 0, t.width, t.height), false) }
        }
    }

    private fun renderDots() {
        dots.removeAllViews()
        packs.forEach { p ->
            val on = p.level == selected
            val d = View(this).apply { background = shape(if (on) gold else 0xFF3A4156.toInt(), 4f) }
            dots.addView(d, lp(dp(if (on) 18 else 6), dp(6)).apply { marginStart = dp(3); marginEnd = dp(3) })
        }
    }

    private fun select(level: Int) {
        if (level == selected || packs.none { it.level == level }) return
        userPicked = true; selected = level
        badge.animate().cancel()
        badge.alpha = .35f; badge.scaleX = .86f; badge.scaleY = .86f
        badge.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator(1.2f)).start()
        renderTabs(); render()
    }

    private fun tintHero(gem: Int) {
        val target = alphaOf(gem, 105)
        heroAnim?.cancel()
        heroAnim = ValueAnimator.ofObject(ArgbEvaluator(), heroTop, target).apply {
            duration = 380
            addUpdateListener { heroTop = it.animatedValue as Int; heroBg.setColors(intArrayOf(heroTop, bgInk)) }
            start()
        }
    }

    private fun renderPerks() {
        val tier = TIERS[(selected - 1).coerceIn(0, 6)]
        val gem = tier[1]
        val unlocked = perks.count { selected >= it.min }
        perksCounter.text = "$unlocked / ${perks.size} مفعّلة"
        val eliteOn = selected >= 4
        elite.alpha = if (eliteOn) 1f else .6f
        eliteIcon.active = eliteOn; eliteIcon.invalidate()
        eliteState.text = if (eliteOn) "مفعّلة في هذا المستوى" else "تبدأ من SVIP4"
        eliteState.setTextColor(if (eliteOn) ok else textTertiary)

        grid.removeAllViews()
        perks.chunked(3).forEach { chunk ->
            val r = row().apply { gravity = Gravity.CENTER }
            chunk.forEach { perk ->
                val active = selected >= perk.min
                val card = column().apply {
                    gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(4), dp(14), dp(4), dp(12))
                    background = if (active)
                        gradient(intArrayOf(surfaceHi, surface), 18f, GradientDrawable.Orientation.TOP_BOTTOM, mix(gem, bgInk, .65f))
                    else shape(surface, 18f, 0xFF222839.toInt())
                }
                val circle = FrameLayout(this).apply {
                    background = if (active) oval(alphaOf(gem, 38), alphaOf(gem, 120)) else oval(0xFF1A1F2C.toInt(), stroke)
                }
                circle.addView(
                    IconView(this, perk.icon).apply {
                        this.active = active; tint = if (active) mix(gem, Color.WHITE, .55f) else 0xFF5B6479.toInt()
                    },
                    FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER)
                )
                if (!active) {
                    val lock = FrameLayout(this).apply { background = oval(surface, stroke) }
                    lock.addView(IconView(this, "lock").apply { this.active = false; tint = 0xFF7A8296.toInt() },
                        FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER))
                    circle.addView(lock, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.BOTTOM or Gravity.START))
                }
                card.addView(circle, lp(dp(54), dp(54)))
                card.addView(
                    label(perk.label(selected), 11f, if (active) 0xFFC9D0DE.toInt() else 0xFF6A7389.toInt()).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; textAlignment = View.TEXT_ALIGNMENT_CENTER; maxLines = 2
                    }, lp(-1, dp(32)).apply { topMargin = dp(8) }
                )
                card.addView(
                    label(if (active) "مفعّل" else "من SVIP${perk.min}", 10f, if (active) ok else textTertiary, active)
                        .apply { gravity = Gravity.CENTER }, lp(-1, dp(16))
                )
                r.addView(card, lp(0, -1, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
            }
            repeat(3 - chunk.size) { r.addView(View(this), lp(0, 1, 1f).apply { marginStart = dp(4); marginEnd = dp(4) }) }
            grid.addView(r, lp(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun render() {
        val p = packs.firstOrNull { it.level == selected } ?: return
        val tier = TIERS[(selected - 1).coerceIn(0, 6)]
        badge.level = selected
        tierTitle.text = "SVIP $selected"
        tintHero(tier[1]); renderDots(); renderPerks()

        val date = if (owned == selected && expiresAt.isNotBlank()) try {
            val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(expiresAt.take(19))
            d?.let { " · تنتهي " + SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(it) }.orEmpty()
        } catch (_: Exception) { "" } else ""

        val cashback = p.price * 40 / 100
        balanceText.text = fmt(balance)
        memberEntry.visibility = if (owned > 0) View.VISIBLE else View.GONE
        priceDetail.text = fmt(p.price) + "  ◇"
        cashbackDetail.text = "+ " + fmt(cashback) + "  ◇"
        bottomPrice.text = fmt(p.price)
        bottomBack.text = "يرجعلك ${fmt(cashback)} عملة (40%)"
        val isOwned = owned == selected
        bottomState.text = if (isOwned) "عضويتك فعّالة$date" else "أنت لست SVIP$selected"
        bottomState.setTextColor(if (isOwned) ok else textTertiary)
        tierSub.text = if (isOwned) "عضويتك فعّالة$date" else "تفتح ${perks.count { selected >= it.min }} من ${perks.size} امتيازاً"
        tierSub.setTextColor(if (isOwned) ok else textSecondary)
        updateBuy()
    }

    private fun updateBuy() {
        val p = packs.firstOrNull { it.level == selected }
        val can = p != null && p.active && p.price > 0 && !buying
        buy.isEnabled = can; buy.alpha = if (can) 1f else .5f
        buy.text = when { buying -> "جارٍ التنفيذ…"; owned == selected -> "تجديد"; else -> "شراء" }
    }

    // ───────────────────────── Bottom sheets ─────────────────────────
    private fun showSheet(build: (LinearLayout, Dialog) -> Unit) {
        val d = Dialog(this); d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val r = dp(26f).toFloat()
        val box = column().apply {
            background = GradientDrawable().apply { setColor(surface); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
            setPadding(dp(18), dp(10), dp(18), dp(22) + botInset)
        }
        box.addView(View(this).apply { background = shape(0xFF464E63.toInt(), 5f) },
            lp(dp(40), dp(4)).apply { gravity = Gravity.CENTER; bottomMargin = dp(14) })
        build(box, d)
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.BOTTOM); setDimAmount(.6f)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        d.show()
        box.translationY = dp(320).toFloat()
        box.animate().translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun confirmPurchase() {
        val p = packs.firstOrNull { it.level == selected } ?: run { showToast("لم يتم تحميل باقات SVIP بعد", true); return }
        val back = p.price * 40 / 100
        val enough = balance >= p.price && p.active
        showSheet { box, d ->
            box.addView(BadgeView(this).apply { level = p.level }, lp(-1, dp(120)))
            box.addView(label("${if (owned == p.level) "تجديد" else "شراء"} SVIP ${p.level}", 19f, textPrimary, true).apply { gravity = Gravity.CENTER }, lp(-1, dp(34)))
            box.addView(
                label("رصيدك الحالي ${fmt(balance)} ماسة. كل ترقية تُحاسب بالسعر الكامل للمستوى الجديد وتبدأ مدة 30 يوماً جديدة.", 12.5f, textSecondary)
                    .apply { gravity = Gravity.CENTER; setLineSpacing(dp(2).toFloat(), 1.15f) },
                lp(-1, -2).apply { bottomMargin = dp(14) }
            )
            val card = column().apply { background = shape(surfaceHi, 16f, stroke) }
            fun line(title: String, value: String, color: Int = textPrimary, last: Boolean = false) {
                val rr = row().apply { setPadding(dp(14), dp(12), dp(14), dp(12)) }
                rr.addView(label(title, 12.5f, textSecondary), lp(0, -2, 1f)); rr.addView(label(value, 13.5f, color, true))
                card.addView(rr)
                if (!last) card.addView(View(this).apply { setBackgroundColor(stroke) }, lp(-1, 1).apply { marginStart = dp(14); marginEnd = dp(14) })
            }
            line("سعر الباقة", fmt(p.price) + " ◇")
            line("مردود المحفظة (40%)", "+ " + fmt(back) + " ◇", ok)
            line("الصافي بعد المردود", fmt(p.price - back) + " ◇")
            line("رصيدك بعد الشراء", if (enough) fmt(balance - p.price + back) + " ◇" else "—")
            line("مدة العضوية", "30 يوماً", last = true)
            box.addView(card, lp(-1, -2))
            if (!enough) box.addView(
                label("رصيدك لا يكفي. تحتاج إلى ${fmt((p.price - balance).coerceAtLeast(0))} ماسة إضافية.", 12.5f, danger).apply { gravity = Gravity.CENTER },
                lp(-1, -2).apply { topMargin = dp(12) }
            )
            val actions = row().apply { layoutDirection = View.LAYOUT_DIRECTION_LTR }
            val cancel = label("إلغاء", 14f, 0xFFC4CBDA.toInt(), true).apply {
                gravity = Gravity.CENTER
                background = ripple(shape(Color.TRANSPARENT, 25f, 0xFF4A5268.toInt()), 25f)
                setOnClickListener { d.dismiss() }; pressable()
            }
            val confirm = label("تأكيد الشراء", 14f, onGold, true).apply {
                gravity = Gravity.CENTER; alpha = if (enough) 1f else .45f
                background = ripple(gradient(intArrayOf(gold, goldDeep), 25f), 25f)
                setOnClickListener { if (enough) { d.dismiss(); purchase(p.level) } }; pressable()
            }
            actions.addView(cancel, lp(0, dp(50), 1f))
            actions.addView(confirm, lp(0, dp(50), 1.4f).apply { marginStart = dp(10) })
            box.addView(actions, lp(-1, dp(50)).apply { topMargin = dp(18) })
        }
    }

    private fun showEliteInfo() {
        showSheet { box, d ->
            val c = FrameLayout(this).apply { background = oval(alphaOf(gold, 30), alphaOf(gold, 120)) }
            c.addView(IconView(this, "mask").apply { tint = gold }, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER))
            box.addView(c, lp(dp(72), dp(72)).apply { gravity = Gravity.CENTER })
            box.addView(label("امتيازات النخبة", 19f, gold, true).apply { gravity = Gravity.CENTER }, lp(-1, dp(40)).apply { topMargin = dp(6) })
            box.addView(
                label("تبدأ من VIP4. عند دخولك الغرفة يظهر اسمك كرجل غامض بدل ظهور الإشعار المعتاد، فتدخل دون لفت الانتباه.", 13f, textSecondary)
                    .apply { gravity = Gravity.CENTER; setLineSpacing(dp(3).toFloat(), 1.15f) },
                lp(-1, -2).apply { bottomMargin = dp(18) }
            )
            val ok = label("حسناً", 14f, onGold, true).apply {
                gravity = Gravity.CENTER
                background = ripple(gradient(intArrayOf(gold, goldDeep), 25f), 25f)
                setOnClickListener { d.dismiss() }; pressable()
            }
            box.addView(ok, lp(-1, dp(50)))
        }
    }

    // ───────────────────────── In-app toast ─────────────────────────
    private fun showToast(s: String, error: Boolean = false) {
        val tv = toastView ?: TextView(this).also {
            toastView = it
            rootView.addView(it, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        }
        tv.text = s; tv.textSize = 13f; tv.maxWidth = dp(340); tv.gravity = Gravity.CENTER
        tv.setTextColor(if (error) danger else textPrimary)
        tv.background = shape(surfaceHi, 16f, if (error) alphaOf(danger, 150) else alphaOf(gold, 150))
        tv.setPadding(dp(18), dp(12), dp(18), dp(12)); tv.elevation = dp(10).toFloat()
        tv.layoutParams = (tv.layoutParams as FrameLayout.LayoutParams).apply { topMargin = topInset + dp(64) }
        tv.removeCallbacks(hideToast); tv.animate().cancel()
        tv.alpha = 0f; tv.translationY = -dp(24).toFloat()
        tv.animate().alpha(1f).translationY(0f).setDuration(240).start()
        tv.postDelayed(hideToast, 3200)
    }

    // ───────────────────────── Networking (unchanged logic) ─────────────────────────
    private fun purchase(level: Int) {
        buying = true; updateBuy()
        io.execute {
            try {
                val r = AuthApi.post("/api/svip/purchase", JSONObject().put("level", level), token)
                if (r.status !in 200..299 || !r.body.optBoolean("success", false)) {
                    val msg = r.body.optString("error", r.body.optString("message", "تعذر إتمام شراء SVIP"))
                    runOnUiThread { buying = false; showToast(msg, true); updateBuy() }; return@execute
                }
                loadData(r.body.optString("message", "تم تفعيل عضوية SVIP $level بنجاح"))
            } catch (e: Exception) {
                runOnUiThread { buying = false; showToast(e.message ?: "تعذر الاتصال بالخادم", true); updateBuy() }
            }
        }
    }

    private fun applyCached() {
        try {
            val c = getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE).getString("page_data", null) ?: return
            applyData(JSONObject(c))
        } catch (_: Exception) {}
    }

    private fun loadData(successMessage: String? = null) {
        if (loading && successMessage == null) return
        loading = true
        io.execute {
            try {
                val lf = CompletableFuture.supplyAsync { AuthApi.get("/api/svip/levels", token) }
                val mf = CompletableFuture.supplyAsync { AuthApi.get("/api/svip/me", token) }
                val wf = CompletableFuture.supplyAsync { AuthApi.get("/api/wallet", token) }
                val lr = lf.get(); val mr = mf.get(); val wr = wf.get()
                if (lr.status !in 200..299 || !lr.body.optBoolean("success", false)) throw IllegalStateException(lr.body.optString("error", "تعذر تحميل مستويات SVIP"))
                if (mr.status !in 200..299 || !mr.body.optBoolean("success", false)) throw IllegalStateException(mr.body.optString("error", "تعذر تحميل عضويتك الحالية"))
                if (wr.status !in 200..299) throw IllegalStateException(wr.body.optString("error", "تعذر تحميل رصيد المحفظة"))
                val raw = lr.body.optJSONArray("levels") ?: lr.body.optJSONArray("packages") ?: JSONArray()
                val arr = JSONArray()
                for (i in 0 until raw.length()) {
                    val x = raw.optJSONObject(i) ?: continue
                    val n = x.optInt("level", x.optInt("vip_level", 0))
                    val price = x.optLong("price_coins", x.optLong("price_diamonds", x.optLong("price", 0)))
                    val active = x.optBoolean("active", true) && x.optBoolean("is_active", true) && price > 0
                    if (n in 1..7) arr.put(JSONObject().put("level", n).put("price", price).put("active", active))
                }
                val sub = mr.body.optJSONObject("subscription") ?: JSONObject()
                val wallet = wr.body.optJSONObject("wallet") ?: wr.body
                val data = JSONObject().put("packages", arr)
                    .put("membership", JSONObject().put("vip_level", sub.optInt("level", 0)).put("expires_at", sub.optString("expires_at", "")))
                    .put("balance", wallet.optLong("recharge_coins", 0))
                getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE).edit()
                    .putString("page_data", data.toString()).putLong("saved_at", System.currentTimeMillis()).apply()
                runOnUiThread {
                    loading = false; buying = false
                    if (!isFinishing && !isDestroyed) {
                        applyData(data)
                        if (!successMessage.isNullOrBlank()) showToast(successMessage)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false; buying = false
                    if (!isFinishing && !isDestroyed) { showToast(e.message ?: "تعذر تحميل بيانات SVIP", true); updateBuy() }
                }
            }
        }
    }

    private fun applyData(data: JSONObject) {
        val raw = data.optJSONArray("packages") ?: JSONArray()
        val next = mutableListOf<Pack>()
        for (i in 0 until raw.length()) {
            val p = raw.optJSONObject(i) ?: continue
            val n = p.optInt("level", p.optInt("vip_level", 0))
            val price = p.optLong("price", p.optLong("price_diamonds", 0))
            if (n in 1..7 && price > 0) next.add(Pack(n, price, p.optBoolean("active", true)))
        }
        packs = next.sortedBy { it.level }.toMutableList()
        balance = data.optLong("balance", 0)
        val m = data.optJSONObject("membership") ?: JSONObject()
        owned = m.optInt("vip_level", m.optInt("level", 0)); expiresAt = m.optString("expires_at", "")
        if (packs.isNotEmpty()) {
            selected = when {
                !userPicked && packs.any { it.level == owned } -> owned
                packs.any { it.level == selected } -> selected
                else -> packs.first().level
            }
            renderTabs(); render()
        } else {
            bottomState.text = "لا توجد مستويات SVIP متاحة حالياً"; updateBuy()
        }
    }

    // ───────────────────────── Custom drawn views ─────────────────────────
    private class DiamondView(c: android.content.Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val path = Path().apply {
                moveTo(w * .25f, h * .12f); lineTo(w * .75f, h * .12f); lineTo(w * .96f, h * .42f); lineTo(w * .5f, h * .94f); lineTo(w * .04f, h * .42f); close()
                moveTo(w * .04f, h * .42f); lineTo(w * .96f, h * .42f)
                moveTo(w * .25f, h * .12f); lineTo(w * .37f, h * .42f); lineTo(w * .5f, h * .94f); lineTo(w * .63f, h * .42f); lineTo(w * .75f, h * .12f)
            }
            p.color = 0xff7fd8ff.toInt(); p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f * resources.displayMetrics.density; p.strokeJoin = Paint.Join.ROUND
            c.drawPath(path, p)
        }
    }

    private class IconView(c: android.content.Context, private val kind: String) : View(c) {
        var active = true
        var tint = 0xfff1ecdd.toInt()
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val s = min(width, height) * .78f
            c.save(); c.translate((width - s) / 2, (height - s) / 2); c.scale(s / 24f, s / 24f)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
            p.color = if (active) tint else 0xff8b93a6.toInt()
            val path = Path()
            when (kind) {
                "lock" -> { c.drawRoundRect(5f, 10.5f, 19f, 20.5f, 2.5f, 2.5f, p); path.moveTo(8f, 10.5f); path.lineTo(8f, 8f); path.cubicTo(8f, 2.5f, 16f, 2.5f, 16f, 8f); path.lineTo(16f, 10.5f) }
                "entry" -> { path.moveTo(13f, 3f); path.lineTo(5f, 13f); path.lineTo(11f, 13f); path.lineTo(10f, 21f); path.lineTo(18f, 10f); path.lineTo(12f, 10f) }
                "frame" -> { c.drawCircle(12f, 13.5f, 6.5f, p); c.drawCircle(12f, 13.5f, 2.8f, p); path.moveTo(8f, 4.5f); path.lineTo(10f, 6.5f); path.lineTo(14f, 6.5f); path.lineTo(16f, 4.5f) }
                "vip" -> { c.drawRoundRect(2.5f, 6f, 21.5f, 18f, 6f, 6f, p); p.style = Paint.Style.FILL; p.textSize = 6.6f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; c.drawText("VIP", 12f, 14.6f, p); p.style = Paint.Style.STROKE }
                "follow", "friends" -> {
                    c.drawCircle(10f, 8f, 3.6f, p)
                    path.moveTo(3.5f, 20f); path.cubicTo(3.5f, 12f, 16.5f, 12f, 16.5f, 20f)
                    if (kind == "follow") { path.moveTo(16f, 11.5f); path.lineTo(18f, 13.5f); path.lineTo(21.5f, 9.5f) }
                    else {
                        path.moveTo(18f, 20f); path.lineTo(15f, 17f); path.quadTo(14f, 14f, 17f, 15f); path.lineTo(18f, 16f)
                        path.lineTo(19f, 15f); path.quadTo(22f, 14f, 21f, 17f); path.close()
                    }
                }
                "car" -> { path.moveTo(3.5f, 16.5f); path.lineTo(3.5f, 12.5f); path.lineTo(6f, 8f); path.lineTo(18f, 8f); path.lineTo(20.5f, 12.5f); path.lineTo(20.5f, 16.5f); path.close(); c.drawCircle(7.5f, 16.8f, 1.9f, p); c.drawCircle(16.5f, 16.8f, 1.9f, p) }
                "support" -> { path.moveTo(4f, 14f); path.lineTo(4f, 12f); path.cubicTo(4f, 1f, 20f, 1f, 20f, 12f); path.lineTo(20f, 14f); c.drawRoundRect(3f, 13f, 7f, 19f, 1.5f, 1.5f, p); c.drawRoundRect(17f, 13f, 21f, 19f, 1.5f, 1.5f, p); path.moveTo(19f, 19f); path.quadTo(18f, 22f, 14f, 21f) }
                "bubble" -> { c.drawRoundRect(2.5f, 6f, 21.5f, 18f, 6f, 6f, p); path.moveTo(10f, 10f); path.lineTo(14f, 10f); path.moveTo(12f, 10f); path.lineTo(12f, 14f); path.moveTo(17f, 10f); path.lineTo(18.5f, 10f); path.moveTo(17f, 13.5f); path.lineTo(18.5f, 13.5f) }
                "card" -> { c.drawRoundRect(3f, 7.5f, 21f, 20f, 3f, 3f, p); path.moveTo(7f, 12.5f); path.lineTo(13f, 12.5f); path.moveTo(7f, 16f); path.lineTo(11f, 16f); path.moveTo(15f, 6f); path.lineTo(16.2f, 3.8f); path.lineTo(17.5f, 6f); path.lineTo(18.8f, 3.8f); path.lineTo(19.5f, 7f) }
                "palette" -> { c.drawCircle(12f, 12f, 9f, p); c.drawCircle(7.5f, 11f, 1f, p); c.drawCircle(10f, 7.5f, 1f, p); c.drawCircle(14.5f, 7.5f, 1f, p) }
                "highlight" -> { c.drawRoundRect(3f, 5.5f, 21f, 17f, 4f, 4f, p); path.moveTo(8f, 17f); path.lineTo(7f, 20.5f); path.lineTo(11.5f, 17f); path.moveTo(12f, 8.3f); path.lineTo(13f, 10.3f); path.lineTo(15.2f, 10.6f); path.lineTo(13.6f, 12.1f); path.lineTo(14f, 14.3f); path.lineTo(12f, 13.2f); path.lineTo(10f, 14.3f); path.lineTo(10.4f, 12.1f); path.lineTo(8.8f, 10.6f); path.lineTo(11f, 10.3f); path.close() }
                "shield" -> { path.moveTo(12f, 3f); path.lineTo(19f, 6f); path.lineTo(19f, 11.5f); path.quadTo(19f, 18f, 12f, 21f); path.quadTo(5f, 18f, 5f, 11.5f); path.close(); c.drawRoundRect(9f, 11f, 15f, 16f, 1f, 1f, p); path.moveTo(10f, 11f); path.lineTo(10f, 9.6f); path.quadTo(12f, 6f, 14f, 9.6f); path.lineTo(14f, 11f) }
                "mute" -> { c.drawRoundRect(3f, 5.5f, 21f, 17.5f, 5f, 5f, p); path.moveTo(8.5f, 17.5f); path.lineTo(7f, 21f); path.lineTo(11.5f, 17.5f); path.moveTo(4f, 3.5f); path.lineTo(20f, 20.5f) }
                "star" -> { for (i in 0..9) { val a = (-90 + i * 36) * Math.PI / 180; val r = if (i % 2 == 0) 9.5 else 4.3; val x = 12 + cos(a) * r; val y = 12 + sin(a) * r; if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat()) }; path.close() }
                "door" -> { c.drawRoundRect(5.5f, 3f, 16.5f, 21f, 1.5f, 1.5f, p); c.drawCircle(13f, 12f, .9f, p); path.moveTo(16.5f, 8f); path.lineTo(20f, 8f); path.moveTo(16.5f, 16f); path.lineTo(20f, 16f); path.moveTo(3f, 21f); path.lineTo(21f, 21f) }
                "mask" -> { path.moveTo(3f, 8.5f); path.cubicTo(6f, 6.5f, 9f, 6.5f, 12f, 8.5f); path.cubicTo(15f, 6.5f, 18f, 6.5f, 21f, 8.5f); path.cubicTo(21f, 14f, 18.5f, 17f, 15.5f, 17f); path.quadTo(13f, 17f, 12f, 15.8f); path.quadTo(10.2f, 17f, 8.5f, 17f); path.cubicTo(5.5f, 17f, 3f, 14f, 3f, 8.5f); path.moveTo(6.5f, 11.5f); path.quadTo(8.5f, 10.7f, 9.7f, 11.5f); path.moveTo(14.3f, 11.5f); path.quadTo(16.5f, 10.7f, 17.5f, 11.5f) }
            }
            c.drawPath(path, p); c.restore()
        }
    }

    /** Detailed metallic shield emblem, drawn natively (no SVG/WebView). */
    private class BadgeView(c: android.content.Context) : View(c) {
        var level = 1
            set(v) { field = v; invalidate() }
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun shade(c: Int, t: Float): Int {
            fun channel(v: Int): Int {
                val target = if (t > 0f) 255f else 0f
                return (v + (target - v) * abs(t)).roundToInt().coerceIn(0, 255)
            }
            return Color.rgb(channel(Color.red(c)), channel(Color.green(c)), channel(Color.blue(c)))
        }
        private fun fill(shader: Shader? = null, color: Int = Color.WHITE) { p.style = Paint.Style.FILL; p.shader = shader; p.color = color }
        private fun stroke(color: Int, w: Float) { p.style = Paint.Style.STROKE; p.shader = null; p.color = color; p.strokeWidth = w; p.strokeJoin = Paint.Join.ROUND; p.strokeCap = Paint.Cap.ROUND }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat(); val scale = min(w / 240f, h / 272f) * .92f
            canvas.save(); canvas.translate((w - 240 * scale) / 2, (h - 272 * scale) / 2); canvas.scale(scale, scale)
            val t = TIERS[(level - 1).coerceIn(0, 6)]; val metal = t[0]; val gem = t[1]; val rib = t[2]
            val hi = shade(metal, .62f); val lo = shade(metal, -.4f); val dk = shade(metal, -.68f)
            val gHi = shade(gem, .6f); val gLo = shade(gem, -.5f)
            // aura + tier rays
            fill(RadialGradient(120f, 138f, 108f, intArrayOf(Color.argb(150, Color.red(hi), Color.green(hi), Color.blue(hi)), Color.TRANSPARENT), null, Shader.TileMode.CLAMP))
            canvas.drawCircle(120f, 138f, 108f, p)
            if (level >= 4) {
                stroke(Color.argb(70, Color.red(hi), Color.green(hi), Color.blue(hi)), 5f)
                for (i in 0 until 6 + level) { val a = i * 2 * PI / (6 + level); canvas.drawLine(120f, 138f, 120f + 102f * cos(a).toFloat(), 138f + 102f * sin(a).toFloat(), p) }
            }
            // wings
            val wing = Path().apply { moveTo(128f, 172f); cubicTo(200f, 168f, 198f, 138f, 182f, 84f); cubicTo(168f, 114f, 150f, 112f, 128f, 118f); close() }
            fill(LinearGradient(120f, 80f, 195f, 180f, intArrayOf(hi, metal, lo, dk), null, Shader.TileMode.CLAMP)); canvas.drawPath(wing, p)
            canvas.save(); canvas.scale(-1f, 1f, 120f, 0f); canvas.drawPath(wing, p); canvas.restore()
            stroke(dk, 1.3f); canvas.drawPath(wing, p); canvas.save(); canvas.scale(-1f, 1f, 120f, 0f); canvas.drawPath(wing, p); canvas.restore()
            // crown
            val crown = Path().apply {
                if (level >= 7) { moveTo(90f, 54f); lineTo(88f, 26f); lineTo(102f, 41f); lineTo(110f, 20f); lineTo(120f, 37f); lineTo(130f, 20f); lineTo(138f, 41f); lineTo(152f, 26f); lineTo(150f, 54f) }
                else if (level >= 4) { moveTo(98f, 54f); lineTo(96f, 32f); lineTo(108f, 44f); lineTo(120f, 26f); lineTo(132f, 44f); lineTo(144f, 32f); lineTo(142f, 54f) }
                else { moveTo(104f, 54f); lineTo(120f, 28f); lineTo(136f, 54f) }
                close()
            }
            fill(LinearGradient(100f, 20f, 140f, 55f, intArrayOf(hi, metal, lo), null, Shader.TileMode.CLAMP)); canvas.drawPath(crown, p); stroke(dk, 1.3f); canvas.drawPath(crown, p)
            if (level >= 4) { fill(null, gem); listOf(120f to 30f, 110f to 20f, 130f to 20f).forEach { canvas.drawCircle(it.first, it.second, 3.4f, p) } }
            // shield layers
            val sh = Path().apply { moveTo(120f, 46f); lineTo(178f, 70f); lineTo(178f, 144f); cubicTo(178f, 182f, 150f, 208f, 120f, 234f); cubicTo(90f, 208f, 62f, 182f, 62f, 144f); lineTo(62f, 70f); close() }
            fill(LinearGradient(62f, 60f, 178f, 220f, intArrayOf(lo, hi, metal, lo), null, Shader.TileMode.CLAMP)); canvas.drawPath(sh, p)
            stroke(dk, 1.8f); canvas.drawPath(sh, p)
            canvas.save(); canvas.translate(120f, 140f); canvas.scale(.9f, .9f); canvas.translate(-120f, -140f)
            fill(LinearGradient(80f, 50f, 160f, 230f, intArrayOf(hi, metal, lo, dk), null, Shader.TileMode.CLAMP)); canvas.drawPath(sh, p); canvas.restore()
            canvas.save(); canvas.translate(120f, 140f); canvas.scale(.8f, .8f); canvas.translate(-120f, -140f)
            fill(LinearGradient(70f, 65f, 170f, 215f, intArrayOf(0xff232c40.toInt(), 0xff0e131f.toInt()), null, Shader.TileMode.CLAMP)); canvas.drawPath(sh, p); canvas.restore()
            // jewel
            fill(RadialGradient(120f, 130f, 48f, intArrayOf(Color.argb(150, Color.red(gem), Color.green(gem), Color.blue(gem)), Color.TRANSPARENT), null, Shader.TileMode.CLAMP))
            canvas.drawCircle(120f, 130f, 48f, p)
            val dia = Path().apply { moveTo(120f, 88f); lineTo(152f, 130f); lineTo(120f, 172f); lineTo(88f, 130f); close() }
            fill(LinearGradient(88f, 88f, 152f, 172f, intArrayOf(gHi, gem, gLo), null, Shader.TileMode.CLAMP)); canvas.drawPath(dia, p); stroke(hi, 1.5f); canvas.drawPath(dia, p)
            fill(null, Color.argb(45, 255, 255, 255)); val facet = Path().apply { moveTo(120f, 88f); lineTo(136f, 130f); lineTo(120f, 172f); close() }; canvas.drawPath(facet, p)
            stroke(Color.argb(150, 255, 255, 255), 1f); canvas.drawLine(88f, 130f, 152f, 130f, p)
            // ribbon
            fill(null, rib); val tail = Path().apply { moveTo(54f, 190f); lineTo(70f, 182f); lineTo(70f, 212f); lineTo(54f, 212f); lineTo(63f, 201f); close() }
            canvas.drawPath(tail, p); canvas.save(); canvas.scale(-1f, 1f, 120f, 0f); canvas.drawPath(tail, p); canvas.restore()
            fill(LinearGradient(66f, 178f, 66f, 208f, intArrayOf(shade(rib, .35f), rib, shade(rib, -.45f)), null, Shader.TileMode.CLAMP)); canvas.drawRect(66f, 178f, 174f, 208f, p)
            stroke(hi, 2f); canvas.drawRect(66f, 178f, 174f, 208f, p)
            stroke(hi, 1f); canvas.drawLine(71f, 183f, 169f, 183f, p); canvas.drawLine(71f, 203f, 169f, 203f, p)
            fill(null, 0xfffffbe6.toInt()); p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create("serif", Typeface.BOLD); p.textSize = 20f
            canvas.drawText("SVIP $level", 120f, 199.5f, p)
            // rivets + sparkles
            fill(null, hi); listOf(72f to 84f, 168f to 84f, 120f to 60f, 66f to 118f, 174f to 118f).forEach { canvas.drawCircle(it.first, it.second, 2.4f, p) }
            fill(null, gHi)
            val points = listOf(44f to 70f, 198f to 62f, 36f to 170f, 206f to 170f, 62f to 36f, 180f to 36f, 28f to 120f, 212f to 118f, 120f to 12f)
            points.take(3 + level / 2).forEach { pt ->
                val x = pt.first; val y = pt.second
                val sp = Path().apply { moveTo(x, y - 5); lineTo(x + 1.4f, y - 1.4f); lineTo(x + 5f, y); lineTo(x + 1.4f, y + 1.4f); lineTo(x, y + 5); lineTo(x - 1.4f, y + 1.4f); lineTo(x - 5f, y); lineTo(x - 1.4f, y - 1.4f); close() }
                canvas.drawPath(sp, p)
            }
            canvas.restore()
        }
    }
}