package com.mariam.live

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * شاشة VIP — Kotlin Native بالكامل (بدون WebView).
 * منطق الـ API مطابق للنسخة السابقة:
 *   GET  /api/vip/packages   GET /api/wallet   GET /api/vip/me   POST /api/vip/purchase {vip_level}
 * ولا يوجد أي حساب مالي هنا: الخصم والإرجاع (35% / 65%) والاشتراك تنفَّذ كلها في الخادم،
 * والأرقام في الواجهة للعرض فقط.
 */
class VipActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private var token = ""
    @Volatile private var loading = false
    private var purchasing = false

    // ----- state -----
    private val packages = ArrayList<VipPackage>()
    private var balance = 0L
    private var selected = 1
    private var owned = 0
    private var renderedLevel = 0

    // ----- views -----
    private lateinit var root: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var buyBar: LinearLayout
    private lateinit var balanceText: TextView
    private lateinit var tabsScroll: HorizontalScrollView
    private lateinit var tabsRow: LinearLayout
    private val tabViews = HashMap<Int, VipTabView>()
    private lateinit var hero: VipHeroView
    private lateinit var eliteText: TextView
    private lateinit var eliteCirc: FrameLayout
    private lateinit var eliteIcon: VipIconView
    private lateinit var eliteLock: View
    private val cells = ArrayList<PvCell>()
    private lateinit var sPrice: TextView
    private lateinit var sUsd: TextView
    private lateinit var sFee: TextView
    private lateinit var sBack: TextView
    private lateinit var bPrice: TextView
    private lateinit var bState: TextView
    private lateinit var bUsd: TextView
    private lateinit var buyBtn: TextView
    private lateinit var sendBtn: TextView
    private lateinit var veil: FrameLayout
    private lateinit var sheetScroll: ScrollView
    private lateinit var sheetBox: LinearLayout
    private lateinit var toastView: TextView
    private var okBuyBtn: TextView? = null
    private var insetTop = 0
    private var insetBottom = 0

    private class PvCell(
        val circ: FrameLayout, val icon: VipIconView, val lock: View, val label: TextView, val small: TextView
    )

    private val hideToast = Runnable {
        toastView.animate().alpha(0f).translationY(dpf(20f)).setDuration(300).start()
    }

    // ===================================================================
    // Lifecycle
    // ===================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        window.statusBarColor = Color.rgb(22, 27, 39)
        window.navigationBarColor = Color.rgb(15, 19, 28)
        window.decorView.systemUiVisibility = 0

        setContentView(buildUi())
        rebuildTabs()
        updateTabs()
        render(false)
        hero.badge.popIn(true)
        loadVipData()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (veil.visibility == View.VISIBLE) closeSheet() else finish()
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }

    // ===================================================================
    // Data (نفس منطق النسخة السابقة)
    // ===================================================================
    private fun JSONObject.num(first: String, second: String): Double {
        val v = if (!isNull(first)) opt(first) else opt(second)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun loadVipData() {
        if (loading) return
        if (token.isBlank()) {
            showResult(false, "انتهت الجلسة، سجّل الدخول من جديد")
            return
        }
        loading = true
        worker.execute {
            try {
                val packagesResponse = AuthApi.get("/api/vip/packages", token)
                if (packagesResponse.status !in 200..299) {
                    throw Exception(packagesResponse.body.optString("error", "تعذر تحميل باقات VIP"))
                }
                val walletResponse = AuthApi.get("/api/wallet", token)
                if (walletResponse.status !in 200..299) {
                    throw Exception(walletResponse.body.optString("error", "تعذر تحميل المحفظة"))
                }
                val membershipResponse = AuthApi.get("/api/vip/me", token)
                val wallet = walletResponse.body.optJSONObject("wallet") ?: walletResponse.body
                val membership = membershipResponse.body.optJSONObject("membership")
                    ?: membershipResponse.body.optJSONObject("vip") ?: JSONObject()

                val arr = packagesResponse.body.optJSONArray("packages") ?: JSONArray()
                val list = ArrayList<VipPackage>()
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    val level = p.num("vip_level", "level").toInt()
                    val price = p.num("price_diamonds", "price").toLong()
                    val active = p.opt("active") != false
                    if (level in 1..VipConfig.MAX_LEVEL && price > 0) list.add(VipPackage(level, price, active))
                }
                val bal = wallet.optLong("recharge_coins", 0L)
                val lvl = membership.optInt("vip_level", 0)

                runOnUiThread {
                    loading = false
                    if (!isFinishing) applyData(list, bal, lvl)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false
                    showResult(false, e.message ?: "تعذر تحميل صفحة VIP")
                }
            }
        }
    }

    private fun applyData(list: List<VipPackage>, bal: Long, level: Int) {
        packages.clear(); packages.addAll(list)
        balance = bal
        owned = level
        if (packages.none { it.level == selected }) selected = packages.firstOrNull()?.level ?: 1
        rebuildTabs()
        updateTabs()
        render(false)
    }

    /** يعادل window.onNativePurchaseResult في الواجهة القديمة. */
    private fun showResult(ok: Boolean, message: String) {
        if (isFinishing) return
        if (ok) {
            closeSheet()
            toast(message.ifBlank { "تم تنفيذ العملية بنجاح" })
        } else {
            okBuyBtn?.let { setBtnEnabled(it, true) }
            toast(message.ifBlank { "تعذر إتمام الشراء" })
        }
    }

    private fun startPurchase(level: Int) {
        if (level !in 1..VipConfig.MAX_LEVEL) {
            showResult(false, "مستوى VIP غير صالح")
            return
        }
        if (purchasing) return
        purchase(level)
    }

    private fun purchase(level: Int) {
        if (token.isBlank()) {
            showResult(false, "سجّل الدخول أولاً")
            return
        }
        purchasing = true
        worker.execute {
            try {
                val response = AuthApi.post(
                    "/api/vip/purchase",
                    JSONObject().put("vip_level", level),
                    token
                )
                val success = response.status in 200..299 &&
                    response.body.optBoolean("success", true)
                val message = if (success) {
                    "تم تفعيل VIP $level لمدة ${VipConfig.DURATION_DAYS} يوماً"
                } else {
                    response.body.optString("error", "فشل شراء العضوية")
                }
                runOnUiThread {
                    purchasing = false
                    if (success) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        showResult(true, message)
                        loadVipData()
                    } else {
                        showResult(false, message)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    purchasing = false
                    showResult(false, e.message ?: "تعذر الاتصال بالخادم")
                }
            }
        }
    }

    private fun pkg(): VipPackage? = packages.firstOrNull { it.level == selected }

    // ===================================================================
    // UI construction
    // ===================================================================
    private fun buildUi(): View {
        root = FrameLayout(this).apply {
            setBackgroundColor(VipColors.PAGE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val width = Math.min(resources.displayMetrics.widthPixels, dp(480f))
        val app = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(VipColors.INK)
        }
        root.addView(app, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL))

        topBar = buildTopBar()
        app.addView(topBar, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        hero = VipHeroView(this).apply { setOnTouchListener(heroSwipe()) }
        hero.badge.setLevel(1)
        column.addView(hero, LinearLayout.LayoutParams(-1, dp(170f)))
        column.addView(buildPanel(), LinearLayout.LayoutParams(-1, -2, 1f).apply { topMargin = -dp(18f) })
        scroll.addView(column, ViewGroup.LayoutParams(-1, -2))
        app.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        buyBar = buildBuyBar()
        app.addView(buyBar, LinearLayout.LayoutParams(-1, -2))

        buildOverlays()
        applyInsets()
        return root
    }

    private fun buildTopBar(): LinearLayout {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(VipColors.INK) }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), 0, dp(12f), 0)
        }
        val back = FrameLayout(this).apply {
            contentDescription = "رجوع"
            isClickable = true
            setOnClickListener { finish() }
            addView(
                VipIconView(this@VipActivity).apply { icon = VipIcon.BACK; strokeColor = Color.WHITE; strokeUnits = 2f },
                FrameLayout.LayoutParams(dp(22f), dp(22f), Gravity.CENTER)
            )
        }
        head.addView(back, LinearLayout.LayoutParams(dp(34f), dp(34f)))
        head.addView(
            label("VIP", 17f, Color.WHITE, bold = true).apply { letterSpacing = 0.03f },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6f) }
        )
        head.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        balanceText = label("0", 13f, VipColors.TEXT, bold = true)
        val wallet = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "رصيد الألماس"
            background = roundedBg(this@VipActivity, VipColors.DEEP, 999f, VipColors.LINE, 1f)
            setPadding(dp(10f), dp(6f), dp(12f), dp(6f))
            addView(diamondIcon(16f, 1.6f), LinearLayout.LayoutParams(dp(16f), dp(16f)))
            addView(balanceText, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(7f) })
        }
        head.addView(wallet, LinearLayout.LayoutParams(-2, -2))
        bar.addView(head, LinearLayout.LayoutParams(-1, dp(48f)))

        tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(7f), dp(3f), dp(7f), dp(3f))
        }
        tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            contentDescription = "مستويات VIP"
            addView(tabsRow, ViewGroup.LayoutParams(-2, -2))
        }
        bar.addView(tabsScroll, LinearLayout.LayoutParams(-1, -2))
        return bar
    }

    private fun buildPanel(): View {
        val panel = VipPanelView(this).apply { setPadding(dp(14f), dp(18f), dp(14f), dp(20f)) }

        // عنوان "الامتيازات" بخطّين متدرجين
        fun line() = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(withAlpha(VipColors.CREAM_D, 0f), VipColors.CREAM_D, withAlpha(VipColors.CREAM_D, 0f))
            )
        }
        val title = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(line(), LinearLayout.LayoutParams(dp(46f), 1.coerceAtLeast(dp(1f))))
            addView(
                label("الامتيازات", 16f, Color.rgb(0xF3, 0xE7, 0xCF)),
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10f); marginEnd = dp(10f) }
            )
            addView(line(), LinearLayout.LayoutParams(dp(46f), 1.coerceAtLeast(dp(1f))))
        }
        panel.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(15f) })

        panel.addView(buildElite(), LinearLayout.LayoutParams(-1, -2))
        panel.addView(buildGrid(), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(26f) })
        panel.addView(buildSplitSection(), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20f) })
        return panel
    }

    // ----- Elite box -----
    private fun buildElite(): View {
        val box = VipEliteBox(this)

        eliteIcon = VipIconView(this).apply { icon = VipIcon.MASK; strokeColor = Color.rgb(0x8B, 0x93, 0xA6) }
        eliteLock = lockBadge(22f, VipColors.ELITE_BG, 12f)
        eliteCirc = FrameLayout(this).apply {
            clipChildren = false
            addView(eliteIcon, FrameLayout.LayoutParams(dp(32f), dp(32f), Gravity.CENTER))
            addView(
                eliteLock,
                FrameLayout.LayoutParams(dp(22f), dp(22f), Gravity.BOTTOM or Gravity.LEFT).apply {
                    leftMargin = -dp(2f); bottomMargin = -dp(2f)
                }
            )
        }
        eliteText = label("دخول رجل غامض الغرفة للتو.", 11f, Color.rgb(0x6F, 0x77, 0x8A)).apply {
            gravity = Gravity.CENTER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            setPadding(dp(12f), dp(36f), dp(12f), dp(14f))
            addView(eliteCirc, LinearLayout.LayoutParams(dp(58f), dp(58f)))
            addView(eliteText, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10f) })
        }
        box.addView(content, FrameLayout.LayoutParams(-1, -2))

        val ribbon = VipRibbon(this).apply {
            text = "امتيازات النخبة"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.rgb(0x3A, 0x2D, 0x12))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            gravity = Gravity.CENTER
            setPadding(dp(34f), dp(5f), dp(34f), dp(13f))
        }
        box.addView(ribbon, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        val q = TextView(this).apply {
            text = "?"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(VipColors.CREAM)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            contentDescription = "ما هي امتيازات النخبة؟"
            background = ovalBg(this@VipActivity, Color.TRANSPARENT, VipColors.CREAM, 1.6f)
            setOnClickListener { openInfo() }
        }
        box.addView(
            q,
            FrameLayout.LayoutParams(dp(28f), dp(28f), Gravity.TOP or Gravity.RIGHT).apply {
                topMargin = dp(10f); rightMargin = dp(10f)
            }
        )
        return box
    }

    // ----- Privileges grid (3 columns) -----
    private fun buildGrid(): View {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rowGap = Math.max(14f, Math.min(22f, resources.displayMetrics.widthPixels / resources.displayMetrics.density * 0.03f))
        var i = 0
        while (i < VIP_PRIVILEGES.size) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until 3) {
                val idx = i + c
                if (idx >= VIP_PRIVILEGES.size) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                    continue
                }
                row.addView(buildCell(VIP_PRIVILEGES[idx]), LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = if (c == 0) 0 else dp(3f); marginEnd = if (c == 2) 0 else dp(3f)
                })
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = if (i == 0) 0 else dp(rowGap) })
            i += 3
        }
        return grid
    }

    private fun buildCell(v: VipPrivilege): View {
        val icon = VipIconView(this).apply { this.icon = v.icon; strokeColor = Color.rgb(0x8B, 0x93, 0xA6) }
        val lock = lockBadge(20f, VipColors.PANEL, 11f)
        val circ = FrameLayout(this).apply {
            clipChildren = false
            addView(icon, FrameLayout.LayoutParams(dp(30f), dp(30f), Gravity.CENTER))
            addView(
                lock,
                FrameLayout.LayoutParams(dp(20f), dp(20f), Gravity.BOTTOM or Gravity.LEFT).apply {
                    leftMargin = -dp(2f); bottomMargin = -dp(2f)
                }
            )
        }
        val labelTv = label("", 11f, Color.rgb(0x59, 0x61, 0x7A)).apply {
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
        }
        val small = label("", 10.5f, Color.rgb(0x4D, 0x55, 0x6B)).apply { gravity = Gravity.CENTER }
        cells.add(PvCell(circ, icon, lock, labelTv, small))

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            addView(circ, LinearLayout.LayoutParams(dp(58f), dp(58f)))
            addView(labelTv, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7f) })
            addView(small, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3f) })
        }
    }

    private fun lockBadge(sizeDp: Float, bg: Int, iconDp: Float): FrameLayout = FrameLayout(this).apply {
        background = ovalBg(this@VipActivity, bg, VipColors.CIRC_BORDER, 1f)
        addView(
            VipIconView(this@VipActivity).apply {
                icon = VipIcon.LOCK; strokeColor = Color.rgb(0x7A, 0x82, 0x96); strokeUnits = 2f
            },
            FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER)
        )
    }

    // ----- "تفاصيل الخصم" -----
    private fun buildSplitSection(): View {
        sPrice = valueText(VipColors.TEXT); sUsd = valueText(VipColors.TEXT)
        sFee = valueText(VipColors.TEXT); sBack = valueText(VipColors.OK)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(this@VipActivity, VipColors.SPLIT_BG, 12f, VipColors.LINE, 1f)
        }
        card.addView(
            label("تفاصيل الخصم", 14.5f, VipColors.TEXT).apply {
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            }, LinearLayout.LayoutParams(-1, -2)
        )
        card.addView(divider(VipColors.LINE))
        card.addView(splitRow("سعر الباقة", withDiamond(sPrice), false))
        card.addView(splitRow("القيمة بالدولار", sUsd, true))
        card.addView(splitRow("حصة BAK LIVE \u2066(35%)\u2069", withDiamond(sFee), true))
        card.addView(splitRow("يعود إلى محفظتك \u2066(65%)\u2069", withDiamond(sBack), true))
        card.addView(splitRow("مدة العضوية", valueText(VipColors.TEXT).apply { text = "${VipConfig.DURATION_DAYS} يوماً" }, true))

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBg(this@VipActivity, VipColors.ROW_LINE, 5f)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            addView(View(this@VipActivity).apply { setBackgroundColor(Color.rgb(0x8D, 0x96, 0xAA)) }, LinearLayout.LayoutParams(0, -1, 35f))
            addView(View(this@VipActivity).apply { setBackgroundColor(VipColors.OK) }, LinearLayout.LayoutParams(0, -1, 65f))
        }
        card.addView(bar, LinearLayout.LayoutParams(-1, dp(8f)).apply {
            topMargin = dp(4f); marginStart = dp(16f); marginEnd = dp(16f); bottomMargin = dp(14f)
        })

        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16f), 0, dp(16f), dp(14f))
            addView(label("35% للتطبيق", 11f, VipColors.MUTED))
            addView(View(this@VipActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(label("65% لمحفظتك", 11f, VipColors.MUTED))
        }
        card.addView(legend, LinearLayout.LayoutParams(-1, -2))

        card.addView(
            label(
                "يُخصم سعر الباقة عند تأكيد الشراء، ثم تُعاد 65% منه إلى محفظتك. التنفيذ النهائي يتم من الخادم.",
                11.5f, VipColors.MUTED
            ).apply {
                setLineSpacing(0f, 1.5f)
                setPadding(dp(16f), 0, dp(16f), dp(16f))
            }, LinearLayout.LayoutParams(-1, -2)
        )
        return card
    }

    private fun valueText(color: Int): TextView = label("", 14f, color).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun withDiamond(tv: TextView): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        (tv.parent as? ViewGroup)?.removeView(tv)
        addView(tv)
        addView(diamondIcon(14f, 1.7f), LinearLayout.LayoutParams(dp(14f), dp(14f)).apply { marginStart = dp(5f) })
    }

    private fun splitRow(labelText: CharSequence, value: View, topDivider: Boolean): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (topDivider) wrap.addView(divider(VipColors.ROW_LINE))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            addView(label(labelText, 13f, Color.rgb(0xAA, 0xB2, 0xC3)))
            addView(View(this@VipActivity), LinearLayout.LayoutParams(dp(10f), 1, 1f))
            addView(value)
        }
        wrap.addView(row, LinearLayout.LayoutParams(-1, -2))
        return wrap
    }

    private fun divider(color: Int): View =
        View(this).apply {
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(-1, Math.max(1, dp(1f)))
        }

    // ----- Buy bar -----
    private fun buildBuyBar(): LinearLayout {
        bPrice = label("", 17f, VipColors.TEXT, bold = true)
        bState = label("", 12f, Color.rgb(0x6C, 0x74, 0x88)).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        bUsd = label("", 12f, Color.rgb(0x6C, 0x74, 0x88)).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }

        val l1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(bPrice)
            addView(diamondIcon(16f, 1.7f), LinearLayout.LayoutParams(dp(16f), dp(16f)).apply { marginStart = dp(5f) })
            addView(
                label("/ ${VipConfig.DURATION_DAYS} يوم", 13f, Color.rgb(0x8A, 0x93, 0xA6)),
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(5f) }
            )
        }
        val pr = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(l1)
            addView(bState, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3f) })
            addView(bUsd, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3f) })
        }

        sendBtn = pillButton("إرسال", PillStyle.SEND).apply {
            visibility = if (VipConfig.GIFT_ENABLED) View.VISIBLE else View.GONE // الإهداء مخفي كما في الأصل
            setOnClickListener { openSend() }
        }
        buyBtn = pillButton("شراء", PillStyle.MAIN).apply {
            minWidth = dp(76f)
            setOnClickListener { openBuy() }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(VipColors.DEEP)
            val inner = LinearLayout(this@VipActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
                addView(pr, LinearLayout.LayoutParams(0, -2, 1f))
                addView(sendBtn, LinearLayout.LayoutParams(-2, dp(38f)).apply { marginStart = dp(10f) })
                addView(buyBtn, LinearLayout.LayoutParams(-2, dp(38f)).apply { marginStart = dp(10f) })
            }
            addView(divider(VipColors.BUY_BORDER))
            addView(inner, LinearLayout.LayoutParams(-1, -2))
            inner.tag = "buy-inner"
        }
    }

    // ----- Overlays: sheet + toast -----
    private fun buildOverlays() {
        veil = FrameLayout(this).apply {
            setBackgroundColor(VipColors.VEIL)
            visibility = View.GONE
            isClickable = true
            setOnClickListener { closeSheet() }
        }
        sheetBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(10f), dp(18f), dp(20f))
        }
        val r = dpf(22f)
        sheetScroll = MaxHeightScroll(this, (resources.displayMetrics.heightPixels * 0.88f).toInt()).apply {
            isVerticalScrollBarEnabled = false
            isClickable = true // لا تمرّر النقرات إلى الستار
            background = GradientDrawable().apply {
                setColor(VipColors.SPLIT_BG)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke(Math.max(1, dp(1f)), VipColors.LINE)
            }
            addView(sheetBox, ViewGroup.LayoutParams(-1, -2))
        }
        val width = Math.min(resources.displayMetrics.widthPixels, dp(480f))
        veil.addView(sheetScroll, FrameLayout.LayoutParams(width, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        root.addView(veil, FrameLayout.LayoutParams(-1, -1))

        toastView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTextColor(Color.rgb(0x2A, 0x22, 0x10))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = roundedBg(this@VipActivity, Color.rgb(0xF4, 0xEC, 0xDB), 12f)
            setPadding(dp(18f), dp(11f), dp(18f), dp(11f))
            maxWidth = resources.displayMetrics.widthPixels - dp(32f)
            alpha = 0f
            visibility = View.GONE
            isClickable = false
        }
        root.addView(
            toastView,
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(96f) }
        )
    }

    private fun applyInsets() {
        root.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val i = insets.getInsets(WindowInsets.Type.systemBars())
                insetTop = i.top; insetBottom = i.bottom
            } else {
                @Suppress("DEPRECATION")
                run { insetTop = insets.systemWindowInsetTop; insetBottom = insets.systemWindowInsetBottom }
            }
            topBar.setPadding(0, insetTop, 0, 0)
            buyBar.findViewWithTag<View>("buy-inner")
                ?.setPadding(dp(16f), dp(12f), dp(16f), dp(12f) + insetBottom)
            sheetBox.setPadding(dp(18f), dp(10f), dp(18f), dp(20f) + insetBottom)
            insets
        }
    }

    // ===================================================================
    // Tabs / selection
    // ===================================================================
    private fun rebuildTabs() {
        tabsRow.removeAllViews()
        tabViews.clear()
        for (p in packages) {
            val t = VipTabView(this).apply {
                text = "VIP${p.level}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(6f), dp(10f), dp(9f))
                setOnClickListener { select(p.level) }
            }
            tabViews[p.level] = t
            tabsRow.addView(t, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = 1 })
        }
    }

    private fun updateTabs() {
        for ((level, t) in tabViews) {
            val sel = level == selected
            t.isSelected = sel
            t.owned = owned == level
            t.setTextColor(if (sel) Color.WHITE else Color.rgb(0x6B, 0x73, 0x85))
            t.typeface = if (sel) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
            t.invalidate()
        }
    }

    private fun centerTab(level: Int) {
        val t = tabViews[level] ?: return
        tabsScroll.post {
            val dx = (t.left + t.width / 2) - (tabsScroll.scrollX + tabsScroll.width / 2)
            tabsScroll.smoothScrollBy(dx, 0)
        }
    }

    private fun select(n: Int, scroll: Boolean = true) {
        val next = n.coerceIn(1, VipConfig.MAX_LEVEL)
        if (next == selected) {
            if (scroll) centerTab(next)
            return
        }
        if (packages.none { it.level == next }) return
        selected = next
        updateTabs()
        render(true)
        if (scroll) centerTab(selected)
    }

    /** السحب الأفقي على الشارة: (RTL) سحب لليمين = المستوى التالي. */
    private fun heroSwipe(): View.OnTouchListener {
        var x0 = 0f
        var tracking = false
        return View.OnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { x0 = e.x; tracking = true; true }
                MotionEvent.ACTION_UP -> {
                    if (tracking) {
                        val dx = e.x - x0
                        if (Math.abs(dx) > dpf(50f)) select(selected + (if (dx > 0) 1 else -1))
                    }
                    tracking = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> { tracking = false; true }
                else -> tracking
            }
        }
    }

    // ===================================================================
    // Render
    // ===================================================================
    private fun render(animate: Boolean) {
        val n = selected.coerceIn(1, VipConfig.MAX_LEVEL)
        val p = pkg()
        val tier = VIP_TIERS[n - 1]

        hero.setTier(tier.gem, animate)
        if (renderedLevel != n) {
            hero.badge.setLevel(n)
            if (animate && renderedLevel != 0) hero.badge.popIn(false)
            renderedLevel = n
        }

        // Elite
        val on = n >= VipConfig.ELITE_MIN
        eliteText.setTextColor(if (on) Color.rgb(0xAE, 0xB6, 0xC7) else Color.rgb(0x6F, 0x77, 0x8A))
        eliteCirc.background = ovalBg(this, VipColors.CIRC_BG, if (on) VipColors.CREAM_D else VipColors.CIRC_BORDER, 1f)
        eliteIcon.strokeColor = if (on) VipColors.CREAM else Color.rgb(0x8B, 0x93, 0xA6)
        eliteLock.visibility = if (on) View.GONE else View.VISIBLE

        // Privileges
        for ((i, v) in VIP_PRIVILEGES.withIndex()) {
            val cell = cells[i]
            val ok = n >= v.min
            cell.circ.background = if (ok) {
                ovalBg(this, mixTierFill(tier.gem), mixTierBorder(tier.gem), 1f)
            } else {
                ovalBg(this, VipColors.CIRC_BG, VipColors.CIRC_BORDER, 1f)
            }
            cell.icon.strokeColor = if (ok) Color.rgb(0xF1, 0xEC, 0xDD) else Color.rgb(0x8B, 0x93, 0xA6)
            cell.lock.visibility = if (ok) View.GONE else View.VISIBLE
            cell.label.text = v.label(n)
            cell.label.setTextColor(if (ok) Color.rgb(0xB4, 0xBC, 0xCB) else Color.rgb(0x59, 0x61, 0x7A))
            cell.small.text = if (ok) "" else "من VIP${v.min}"
        }

        // Price / split
        balanceText.text = fmtInt(balance)
        if (p != null) {
            val s = vipSplit(p.price)
            val usd = fmtUsd(vipUsd(p.price))
            sPrice.text = fmtInt(p.price)
            sUsd.text = "$$usd"
            sFee.text = fmtInt(s.fee)
            sBack.text = fmtInt(s.back)
            bPrice.text = fmtInt(p.price)
            bUsd.text = "ما يعادل $$usd"
        } else {
            sPrice.text = "—"; sUsd.text = "—"; sFee.text = "—"; sBack.text = "—"
            bPrice.text = "—"; bUsd.text = ""
        }
        if (owned == n) {
            bState.text = "عضويتك فعّالة · ${VipConfig.DURATION_DAYS} يوماً"
            bState.setTextColor(VipColors.OK)
        } else {
            bState.text = "أنت لست VIP$n"
            bState.setTextColor(Color.rgb(0x6C, 0x74, 0x88))
        }
        buyBtn.text = if (owned == n) "تجديد" else "شراء"
    }

    /** color-mix(in srgb, tier 55%, #fff 10%) => مجموع 65% فيصبح الناتج بشفافية .65 */
    private fun mixTierBorder(tier: Int): Int {
        fun ch(t: Int) = Math.round((t * 55f + 255f * 10f) / 65f)
        return Color.argb(166, ch(Color.red(tier)), ch(Color.green(tier)), ch(Color.blue(tier)))
    }

    /** color-mix(in srgb, tier 12%, #262c3b) */
    private fun mixTierFill(tier: Int): Int {
        val b = VipColors.CIRC_BG
        fun ch(t: Int, c: Int) = Math.round(t * .12f + c * .88f)
        return Color.rgb(
            ch(Color.red(tier), Color.red(b)), ch(Color.green(tier), Color.green(b)), ch(Color.blue(tier), Color.blue(b))
        )
    }

    // ===================================================================
    // Sheets
    // ===================================================================
    private fun openSheet(build: (LinearLayout) -> Unit) {
        sheetBox.removeAllViews()
        val grab = View(this).apply { background = roundedBg(this@VipActivity, Color.rgb(0x46, 0x4E, 0x63), 2f) }
        sheetBox.addView(grab, LinearLayout.LayoutParams(dp(38f), dp(4f)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(14f)
        })
        build(sheetBox)
        veil.visibility = View.VISIBLE
        sheetScroll.scrollTo(0, 0)
        sheetScroll.alpha = 0f
        sheetScroll.translationY = dpf(40f)
        sheetScroll.animate().alpha(1f).translationY(0f).setDuration(280)
            .setInterpolator(PathInterpolator(0.2f, 0.8f, 0.2f, 1f)).start()
    }

    private fun closeSheet() {
        veil.visibility = View.GONE
        okBuyBtn = null
    }

    private fun h2(text: String): View = label(text, 18f, VipColors.TEXT, bold = true).apply {
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(4f) }
    }

    private fun sub(text: String): View = label(text, 12.5f, VipColors.MUTED).apply {
        setLineSpacing(0f, 1.5f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14f) }
    }

    private fun err(text: String): TextView = label(text, 12.5f, VipColors.BAD).apply {
        setLineSpacing(0f, 1.5f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) }
    }

    private fun acts(vararg buttons: TextView): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16f) }
        for ((i, b) in buttons.withIndex()) {
            addView(b, LinearLayout.LayoutParams(0, dp(38f), 1f).apply { if (i > 0) marginStart = dp(10f) })
        }
    }

    private fun breakdownCard(p: VipPackage, s: VipSplit, afterBal: Long?): View {
        fun num(v: Long, color: Int = VipColors.TEXT) = valueText(color).apply { text = fmtInt(v) }
        val after: View = if (afterBal == null) valueText(VipColors.TEXT).apply { text = "—" } else withDiamond(num(afterBal))
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(this@VipActivity, VipColors.SPLIT_BG, 12f, VipColors.LINE, 1f)
            addView(splitRow("سعر الباقة", withDiamond(num(p.price)), false))
            addView(splitRow("حصة BAK LIVE \u2066(35%)\u2069", withDiamond(num(s.fee)), true))
            addView(splitRow("يعود إلى محفظتك \u2066(65%)\u2069", withDiamond(num(s.back, VipColors.OK)), true))
            addView(splitRow("مدة العضوية", valueText(VipColors.TEXT).apply { text = "${VipConfig.DURATION_DAYS} يوماً" }, true))
            addView(splitRow("رصيدك بعد العملية", after, true))
        }
    }

    private fun openBuy() {
        val p = pkg()
        if (p == null) { toast("لم يتم تحميل باقات VIP بعد"); return }
        val s = vipSplit(p.price)
        val enough = balance >= p.price && p.active
        val renew = owned == p.level
        openSheet { box ->
            box.addView(h2("${if (renew) "تجديد" else "شراء"} VIP ${p.level}"))
            box.addView(sub("رصيدك الحالي ${fmtInt(balance)} ماسة"))
            box.addView(breakdownCard(p, s, if (balance >= p.price) balance - s.fee else null))
            if (!enough) {
                box.addView(
                    err(
                        if (!p.active) "هذه الباقة غير متاحة حالياً."
                        else "رصيدك لا يكفي. تحتاج إلى ${fmtInt(p.price - balance)} ماسة إضافية."
                    )
                )
            }
            val cancel = pillButton("إلغاء", PillStyle.GHOST).apply { setOnClickListener { closeSheet() } }
            val ok = pillButton("تأكيد الشراء", PillStyle.MAIN)
            setBtnEnabled(ok, enough)
            ok.setOnClickListener {
                setBtnEnabled(ok, false)
                startPurchase(p.level)
            }
            okBuyBtn = ok
            box.addView(acts(cancel, ok))
        }
    }

    /** نافذة الإهداء — موجودة كما في الأصل لكنها مخفية (GIFT_ENABLED = false) حتى يتوفر endpoint في الخادم. */
    private fun openSend() {
        val p = pkg()
        if (p == null) { toast("لم يتم تحميل باقات VIP بعد"); return }
        val s = vipSplit(p.price)
        val enough = balance >= p.price && p.active
        openSheet { box ->
            box.addView(h2("إرسال VIP ${p.level} كهدية"))
            box.addView(sub("أدخل معرّف المستلم. تُحتسب النسب نفسها: 35% للتطبيق و65% تعود إلى محفظتك."))
            box.addView(label("معرّف المستخدم (ID)", 12.5f, Color.rgb(0xAA, 0xB2, 0xC3)).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(7f) }
            })
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "مثال: 10458237"
                setHintTextColor(VipColors.MUTED)
                setTextColor(VipColors.TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(14f), 0, dp(14f), 0)
                background = roundedBg(this@VipActivity, Color.rgb(0x18, 0x1D, 0x2A), 12f, Color.rgb(0x4A, 0x52, 0x68), 1f)
            }
            box.addView(input, LinearLayout.LayoutParams(-1, dp(46f)).apply { bottomMargin = dp(14f) })
            box.addView(breakdownCard(p, s, if (balance >= p.price) balance - s.fee else null))
            val errView = err(if (enough) "" else "رصيدك لا يكفي. تحتاج إلى ${fmtInt(p.price - balance)} ماسة إضافية.")
            errView.visibility = if (enough) View.GONE else View.VISIBLE
            box.addView(errView)

            val cancel = pillButton("إلغاء", PillStyle.GHOST).apply { setOnClickListener { closeSheet() } }
            val ok = pillButton("إرسال", PillStyle.MAIN)
            setBtnEnabled(ok, enough)
            ok.setOnClickListener {
                val id = input.text.toString().trim()
                if (!Regex("^\\d{4,}$").matches(id)) {
                    errView.visibility = View.VISIBLE
                    errView.text = "أدخل معرّفاً صحيحاً من 4 أرقام على الأقل."
                    input.requestFocus()
                    return@setOnClickListener
                }
                // لا يوجد endpoint إهداء في الخادم (كما في النسخة الأصلية: submitPurchase غير معرّفة).
                toast("تعذّر الإرسال. حاول مرة أخرى.")
                closeSheet()
            }
            box.addView(acts(cancel, ok))
        }
    }

    private fun openInfo() {
        openSheet { box ->
            box.addView(h2("امتيازات النخبة"))
            box.addView(
                sub(
                    "تبدأ من VIP${VipConfig.ELITE_MIN}. عند دخولك الغرفة يظهر اسمك كرجل غامض بدل ظهور الإشعار المعتاد، " +
                        "فتدخل دون لفت الانتباه."
                )
            )
            box.addView(acts(pillButton("حسناً", PillStyle.MAIN).apply { setOnClickListener { closeSheet() } }))
        }
    }

    // ===================================================================
    // Toast (داخل الواجهة ليظهر فوق الستار)
    // ===================================================================
    private fun toast(msg: String) {
        toastView.text = msg
        toastView.animate().cancel()
        toastView.visibility = View.VISIBLE
        toastView.alpha = 0f
        toastView.translationY = dpf(20f)
        toastView.animate().alpha(1f).translationY(0f).setDuration(300).start()
        ui.removeCallbacks(hideToast)
        ui.postDelayed(hideToast, 2600)
    }

    // ===================================================================
    // Small widgets
    // ===================================================================
    private fun label(text: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun diamondIcon(sizeDp: Float, strokeUnits: Float): View = VipIconView(this).apply {
        icon = VipIcon.DIAMOND; strokeColor = VipColors.DIAMOND; this.strokeUnits = strokeUnits
    }

    private enum class PillStyle { MAIN, SEND, GHOST }

    private fun pillButton(text: String, style: PillStyle): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(17f), 0, dp(17f), 0)
        isClickable = true
        when (style) {
            PillStyle.MAIN -> {
                setTextColor(VipColors.CTA_TEXT)
                background = roundedBg(this@VipActivity, VipColors.CREAM, 20f, VipColors.CREAM, 1.5f)
            }
            PillStyle.SEND -> {
                setTextColor(VipColors.CREAM)
                background = roundedBg(this@VipActivity, Color.TRANSPARENT, 20f, VipColors.CREAM, 1.5f)
            }
            PillStyle.GHOST -> {
                setTextColor(Color.rgb(0xC4, 0xCB, 0xDA))
                background = roundedBg(this@VipActivity, Color.TRANSPARENT, 20f, Color.rgb(0x4A, 0x52, 0x68), 1f)
            }
        }
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.96f).scaleY(.96f).setDuration(120).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    private fun setBtnEnabled(b: TextView, enabled: Boolean) {
        b.isEnabled = enabled
        b.isClickable = enabled
        b.alpha = if (enabled) 1f else .45f
    }
}

/* ===================== Custom views ===================== */

private class VipTabView(context: Context) : TextView(context) {
    var owned = false
    private val underline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VipColors.CREAM }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VipColors.OK }
    private val r = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isSelected) {
            val cx = width / 2f
            r.set(cx - context.dpf(7f), height - context.dpf(6f), cx + context.dpf(7f), height - context.dpf(2f))
            canvas.drawRoundRect(r, context.dpf(3f), context.dpf(3f), underline)
        }
        if (owned) canvas.drawCircle(width - context.dpf(9f), context.dpf(11f), context.dpf(3f), dot)
    }
}

/** لوحة الامتيازات: زوايا علوية بيضاوية (50% / 34px). */
private class VipPanelView(context: Context) : LinearLayout(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VipColors.PANEL }
    private val path = Path()

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = context.dpf(34f)
        path.reset()
        path.moveTo(0f, r)
        path.arcTo(RectF(0f, 0f, w.toFloat(), 2 * r), 180f, 180f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}

/** صندوق النخبة: خلفية + شبكة خطوط قطرية خفيفة + حد. */
private class VipEliteBox(context: Context) : FrameLayout(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VipColors.ELITE_BG }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = Math.max(1f, context.dpf(1f)); color = VipColors.CIRC_BORDER
    }
    private val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = Math.max(1f, context.dpf(1f)); color = Color.argb(5, 255, 255, 255)
    }
    private val clip = Path()
    private val rr = RectF()

    init {
        setWillNotDraw(false)
        clipChildren = false
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val rad = context.dpf(12f)
        rr.set(0.5f, 0.5f, w - 0.5f, h - 0.5f)
        canvas.drawRoundRect(rr, rad, rad, fill)
        canvas.save()
        clip.reset(); clip.addRoundRect(rr, rad, rad, Path.Direction.CW)
        canvas.clipPath(clip)
        val step = context.dpf(14f) * 1.4142f
        var x = -h
        while (x < w) {
            canvas.drawLine(x, h, x + h, 0f, hatch)
            canvas.drawLine(x, 0f, x + h, h, hatch)
            x += step
        }
        canvas.restore()
        canvas.drawRoundRect(rr, rad, rad, border)
    }
}

/** شريط "امتيازات النخبة": polygon(0 0, 100% 0, 93% 68%, 50% 100%, 7% 68%) بتدرج #f4e5c6 → #d8be8d. */
private class VipRibbon(context: Context) : TextView(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        p.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(), Color.rgb(0xF4, 0xE5, 0xC6), Color.rgb(0xD8, 0xBE, 0x8D), Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(0f, 0f); path.lineTo(w.toFloat(), 0f)
        path.lineTo(w * 0.93f, h * 0.68f); path.lineTo(w * 0.5f, h.toFloat()); path.lineTo(w * 0.07f, h * 0.68f)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, p)
        super.onDraw(canvas)
    }
}

/** ScrollView بحد أقصى للارتفاع (max-height: 88vh للنافذة السفلية). */
private class MaxHeightScroll(context: Context, private val maxH: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST))
    }
}