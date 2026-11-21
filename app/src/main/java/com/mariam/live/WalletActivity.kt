package com.mariam.live

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class WalletActivity : Activity() {
    private val io = Executors.newFixedThreadPool(4)

    // ───────── Palette ─────────
    private val ink = 0xFFFFFFFF.toInt()
    private val textMain = 0xFF1E1433.toInt()
    private val soft = 0xFFF5F0FC.toInt()
    private val softStroke = 0xFFE4DAF3.toInt()
    private val panel = 0xFFFFFFFF.toInt()
    private val stroke = 0xFFE4DAF3.toInt()
    private val gold = 0xFFFFC857.toInt()
    private val goldDeep = 0xFFE8A317.toInt()
    private val darkText = 0xFF2A1450.toInt()
    private val muted = 0xFF7A6F92.toInt()
    private val mint = 0xFF12A863.toInt()
    private val red = 0xFFE5484D.toInt()
    private val pink = 0xFFFF4F8B.toInt()

    private var token = ""
    private var packages = JSONArray()
    private var wallet = JSONObject()
    private var history = JSONArray()
    private var anchor: View? = null
    private val pulses = ArrayList<ObjectAnimator>()
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView

    // ───────── Helpers ─────────
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fmt(v: Long) = String.format(Locale.US, "%,d", v)
    private fun money(cents: Long) = String.format(Locale.US, "%,.2f", cents / 100.0)
    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)

    private fun rr(fill: Int, r: Int, strokeColor: Int = 0, sw: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(r).toFloat()
            if (strokeColor != 0) setStroke(dp(sw), strokeColor)
        }

    private fun grad(
        c1: Int, c2: Int, r: Int, strokeColor: Int = 0,
        o: GradientDrawable.Orientation = GradientDrawable.Orientation.TL_BR
    ): GradientDrawable = GradientDrawable(o, intArrayOf(c1, c2)).apply {
        cornerRadius = dp(r).toFloat()
        if (strokeColor != 0) setStroke(dp(1), strokeColor)
    }

    private fun oval(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    private fun View.press() {
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    private fun LinearLayout.addIc(
        k: WalletIc, size: Int, tint: Int = Color.WHITE, mod: (LinearLayout.LayoutParams) -> Unit = {}
    ) {
        val v = ImageView(context).apply { setImageDrawable(IconDrawable(k, tint)) }
        addView(v, lp(dp(size), dp(size)).also(mod))
    }

    private fun circleBtn(k: WalletIc, tint: Int, size: Int, click: () -> Unit): FrameLayout =
        FrameLayout(this).apply {
            background = rr(soft, size / 2, softStroke, 1)
            isClickable = true
            setOnClickListener { click() }
            press()
            addView(
                ImageView(context).apply { setImageDrawable(IconDrawable(k, tint)) },
                FrameLayout.LayoutParams(dp(size * 45 / 100), dp(size * 45 / 100), Gravity.CENTER)
            )
        }

    private fun pillButton(text: String, ic: WalletIc?, primary: Boolean, click: () -> Unit): LinearLayout {
        val tint = if (primary) darkText else textMain
        val b = LinearLayout(this).apply {
            orientation = 0; gravity = Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = if (primary) grad(0xFFFFD36B.toInt(), goldDeep, 16)
            else rr(soft, 16, softStroke, 1)
            isClickable = true
            setOnClickListener { click() }
            press()
        }
        if (ic != null) b.addIc(ic, 18, tint) { it.marginEnd = dp(8) }
        b.addView(label(text, 14f, tint, true))
        return b
    }

    // ───────── Lifecycle ─────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (android.os.Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags
        val root = LinearLayout(this).apply {
            orientation = 1
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFF8F4FE.toInt())
            )
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        header.addView(circleBtn(WalletIc.BACK, textMain, 44) { finish() }, lp(dp(44), dp(44)))
        header.addView(
            label("المحفظة", 20f, textMain, true).apply { gravity = Gravity.CENTER },
            lp(0, -2).also { it.weight = 1f }
        )
        header.addView(circleBtn(WalletIc.HELP, 0xFF7B3FE4.toInt(), 44) { showHelp() }, lp(dp(44), dp(44)))
        root.addView(header)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        content = LinearLayout(this).apply {
            orientation = 1
            setPadding(dp(18), dp(6), dp(18), dp(32))
        }
        scroll.addView(content)
        root.addView(scroll, lp(-1, 0).also { it.weight = 1f })
        setContentView(root)
        loading(); loadWallet()
    }

    override fun onDestroy() {
        stopPulses()
        io.shutdownNow()
        super.onDestroy()
    }

    private fun stopPulses() {
        pulses.forEach { it.cancel() }
        pulses.clear()
    }

    // ───────── Data ─────────
    private fun loadWallet() {
        if (token.isBlank()) { showError("انتهت جلسة الدخول"); return }
        loading()
        io.execute {
            try {
                // Load balance, packages and transaction history in parallel.
                val walletFuture = io.submit { AuthApi.get("/api/wallet", token) }
                val packagesFuture = io.submit { AuthApi.get("/api/wallet/packages", token) }
                val historyFuture = io.submit { AuthApi.get("/api/wallet/transactions", token) }
                val w = walletFuture.get()
                val p = packagesFuture.get()
                val h = historyFuture.get()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (w.status !in 200..299) {
                        showError(w.body.optString("error", "تعذر تحميل المحفظة")); return@runOnUiThread
                    }
                    wallet = w.body.optJSONObject("wallet") ?: w.body
                    packages = p.body.optJSONArray("packages") ?: JSONArray()
                    history = h.body.optJSONArray("transactions") ?: JSONArray()
                    render()
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing) showError(e.message ?: "تعذر الاتصال بالخادم") }
            }
        }
    }

    // ───────── States ─────────
    private fun loading() {
        stopPulses(); content.removeAllViews()
        fun sk(h: Int, r: Int, top: Int = 0) {
            val v = View(this).apply { background = rr(0xFFEDE7F7.toInt(), r) }
            content.addView(v, lp(-1, dp(h)).apply { topMargin = dp(top) })
            pulses.add(ObjectAnimator.ofFloat(v, "alpha", .35f, .9f).apply {
                duration = 800; repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE; start()
            })
        }
        sk(200, 26, 8); sk(52, 16, 14); sk(22, 8, 28)
        sk(130, 20, 14); sk(130, 20, 12); sk(130, 20, 12)
    }

    private fun showError(message: String) {
        stopPulses(); content.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER
            setPadding(dp(20), dp(70), dp(20), dp(20))
        }
        val chip = FrameLayout(this).apply { background = oval(0x22E5484D) }
        chip.addView(
            ImageView(this).apply { setImageDrawable(IconDrawable(WalletIc.WARN, red)) },
            FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
        )
        box.addView(chip, lp(dp(84), dp(84)))
        box.addView(
            label("تعذر تحميل المحفظة", 17f, textMain, true),
            lp(-2, -2).apply { topMargin = dp(18) }
        )
        box.addView(
            label(message, 13f, muted).apply { gravity = Gravity.CENTER },
            lp(-2, -2).apply { topMargin = dp(8) }
        )
        box.addView(
            pillButton("إعادة المحاولة", null, true) { loadWallet() },
            lp(dp(200), -2).apply { topMargin = dp(24) }
        )
        content.addView(box, lp(-1, -2))
    }

    // ───────── Main render ─────────
    private fun render() {
        stopPulses(); content.removeAllViews()
        content.addView(hero(), lp(-1, -2).apply { topMargin = dp(8) })

        val actions = LinearLayout(this).apply { orientation = 0 }
        actions.addView(
            pillButton("اشحن الآن", WalletIc.WALLET, true) {
                anchor?.let { a -> scroll.post { scroll.smoothScrollTo(0, a.top - dp(8)) } }
            },
            lp(0, -2).apply { weight = 1f; marginEnd = dp(6) }
        )
        actions.addView(
            pillButton("سجل العمليات", WalletIc.HISTORY, false) { showHistory() },
            lp(0, -2).apply { weight = 1f; marginStart = dp(6) }
        )
        content.addView(actions, lp(-1, -2).apply { topMargin = dp(16) })

        anchor = section("باقات شحن العملات", "اختر الباقة المناسبة لك")

        val n = packages.length()
        if (n == 0) {
            content.addView(emptyPackages(), lp(-1, -2))
        } else {
            var best = -1
            var bestPct = 0L
            for (i in 0 until n) {
                val pc = pct(packages.optJSONObject(i))
                if (pc > bestPct) { bestPct = pc; best = i }
            }
            var i = 0
            while (i < n) {
                val row = LinearLayout(this).apply { orientation = 0 }
                for (j in 0..1) {
                    val idx = i + j
                    val p = if (idx < n) packages.optJSONObject(idx) else null
                    val params = lp(0, -2).apply {
                        weight = 1f
                        if (j == 0) marginEnd = dp(6) else marginStart = dp(6)
                    }
                    if (p != null) row.addView(packageCard(p, idx, n, idx == best), params)
                    else row.addView(View(this), params)
                }
                content.addView(row, lp(-1, -2).apply { bottomMargin = dp(12) })
                i += 2
            }
        }

        val foot = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rr(soft, 14, softStroke, 1)
        }
        foot.addIc(WalletIc.SHIELD, 20, mint) { it.marginEnd = dp(10) }
        foot.addView(label("تُضاف العملات بعد تأكيد الدفع من الخادم فقط.", 11.5f, muted))
        content.addView(foot, lp(-1, -2).apply { topMargin = dp(4) })

        for (k in 0 until content.childCount) {
            val v = content.getChildAt(k)
            v.alpha = 0f; v.translationY = dp(18).toFloat()
            v.animate().alpha(1f).translationY(0f).setStartDelay(k * 60L).setDuration(320).start()
        }
    }

    private fun pct(p: JSONObject?): Long {
        if (p == null) return 0
        val c = p.optLong("coins"); val b = p.optLong("bonus_coins")
        return if (c > 0) b * 100 / c else 0
    }

    private fun hero(): LinearLayout {
        val h = LinearLayout(this).apply {
            orientation = 1
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = HeroDrawable(dp(26).toFloat(), dp(1).toFloat())
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addIc(WalletIc.WALLET, 16, 0xCCFFFFFF.toInt()) { it.marginEnd = dp(6) }
        top.addView(label("رصيدك الحالي", 12f, 0xCCFFFFFF.toInt()))
        h.addView(top)

        val bal = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bal.addIc(WalletIc.COIN, 44)
        bal.addView(
            label(fmt(wallet.optLong("recharge_coins")), 38f, Color.WHITE, true),
            lp(-2, -2).apply { marginStart = dp(12) }
        )
        h.addView(bal, lp(-2, -2).apply { topMargin = dp(12) })
        h.addView(label("عملات الشحن", 13f, gold, true), lp(-2, -2).apply { topMargin = dp(6) })

        h.addView(View(this).apply { setBackgroundColor(0x26FFFFFF) },
            lp(-1, dp(1)).apply { topMargin = dp(16); bottomMargin = dp(14) })

        val dia = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        dia.addIc(WalletIc.GEM, 28) { it.marginEnd = dp(12) }
        val col = LinearLayout(this).apply { orientation = 1 }
        col.addView(label("الألماس المكتسب", 11.5f, 0xCCFFFFFF.toInt()))
        col.addView(
            label(fmt(wallet.optLong("earned_diamonds")), 20f, Color.WHITE, true),
            lp(-2, -2).apply { topMargin = dp(3) }
        )
        dia.addView(col, lp(0, -2).apply { weight = 1f })
        h.addView(dia)
        return h
    }

    private fun section(title: String, sub: String = ""): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            View(this).apply { background = grad(gold, goldDeep, 3) },
            lp(dp(4), dp(34)).apply { marginEnd = dp(10) }
        )
        val col = LinearLayout(this).apply { orientation = 1 }
        col.addView(label(title, 18f, textMain, true))
        if (sub.isNotBlank()) col.addView(label(sub, 11.5f, muted), lp(-2, -2).apply { topMargin = dp(4) })
        row.addView(col)
        content.addView(row, lp(-1, -2).apply { topMargin = dp(26); bottomMargin = dp(14) })
        return row
    }

    private fun emptyPackages(): View {
        val empty = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER
            setPadding(dp(16), dp(28), dp(16), dp(28))
            background = rr(panel, 20, stroke, 1)
        }
        empty.addIc(WalletIc.COIN, 46)
        empty.addView(
            label("لا توجد باقات متاحة حالياً", 13f, muted),
            lp(-2, -2).apply { topMargin = dp(12) }
        )
        return empty
    }

    private fun packageCard(p: JSONObject, idx: Int, n: Int, best: Boolean): View {
        val coins = p.optLong("coins")
        val bonus = p.optLong("bonus_coins")
        val total = coins + bonus
        val frame = FrameLayout(this).apply {
            background = if (best) grad(0xFFFFF7E0.toInt(), 0xFFFFFFFF.toInt(), 22, gold)
            else grad(0xFFFFFFFF.toInt(), 0xFFF8F4FE.toInt(), 22, stroke)
            elevation = dp(3).toFloat()
            isClickable = true
            setOnClickListener { onBuy(p) }
            press()
        }
        val col = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(16), dp(12), dp(14))
        }
        val tier = (idx * 3 / n + 1).coerceIn(1, 3)
        col.addView(
            ImageView(this).apply { setImageDrawable(CoinPile(tier)) },
            lp(dp(72), dp(72))
        )
        col.addView(
            label(fmt(total), 22f, textMain, true),
            lp(-2, -2).apply { topMargin = dp(8) }
        )
        col.addView(label("عملة", 11f, muted), lp(-2, -2).apply { topMargin = dp(3) })
        col.addView(
            label(if (bonus > 0) "شامل مكافأة +" + fmt(bonus) else " ", 10.5f, mint, true).apply {
                visibility = if (bonus > 0) View.VISIBLE else View.INVISIBLE
            },
            lp(-2, -2).apply { topMargin = dp(8) }
        )
        col.addView(
            TextView(this).apply {
                text = "$" + money(p.optLong("price_usd_cents"))
                textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText); gravity = Gravity.CENTER
                setPadding(0, dp(11), 0, dp(11))
                background = grad(0xFFFFD36B.toInt(), goldDeep, 14)
            },
            lp(-1, -2).apply { topMargin = dp(12) }
        )
        frame.addView(col, FrameLayout.LayoutParams(-1, -2))

        fun badge(t: String, bg: GradientDrawable, fg: Int, g: Int) {
            frame.addView(
                TextView(this).apply {
                    text = t; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(fg); background = bg
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                },
                FrameLayout.LayoutParams(-2, -2, g).apply { setMargins(dp(10), dp(10), dp(10), 0) }
            )
        }
        if (bonus > 0 && coins > 0) {
            badge("+" + (bonus * 100 / coins) + "%", grad(pink, 0xFFD6246E.toInt(), 10), Color.WHITE, Gravity.TOP or Gravity.START)
        }
        if (best) {
            badge("الأفضل", grad(0xFFFFD36B.toInt(), goldDeep, 10), darkText, Gravity.TOP or Gravity.END)
        }
        return frame
    }

    /** نقطة الربط مع الدفع (Google Play Billing / بوابة دفع). حالياً رسالة "قريباً". */
    private fun onBuy(p: JSONObject) {
        Toast.makeText(this, "الشراء سيتاح قريباً", Toast.LENGTH_SHORT).show()
    }

    // ───────── Bottom sheets ─────────
    private fun sheet(title: String, build: (LinearLayout, Dialog) -> Unit) {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val r = dp(28).toFloat()
        val box = LinearLayout(this).apply {
            orientation = 1
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(10), dp(20), dp(22))
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke(dp(1), stroke)
            }
        }
        box.addView(
            View(this).apply { background = rr(0xFFD9CFEA.toInt(), 3) },
            lp(dp(42), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(14) }
        )
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(label(title, 19f, textMain, true), lp(0, -2).apply { weight = 1f })
        head.addView(circleBtn(WalletIc.CLOSE, textMain, 36) { d.dismiss() }, lp(dp(36), dp(36)))
        box.addView(head, lp(-1, -2).apply { bottomMargin = dp(14) })
        build(box, d)
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(-1, -2)
            setGravity(Gravity.BOTTOM)
            setDimAmount(.45f)
        }
        d.show()
    }

    private fun showHelp() {
        sheet("عن المحفظة") { box, d ->
            fun item(k: WalletIc, tint: Int, t: String, desc: String) {
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = rr(soft, 16, softStroke, 1)
                }
                val chip = FrameLayout(this).apply { background = oval(0xFFEDE7F7.toInt()) }
                chip.addView(
                    ImageView(this).apply { setImageDrawable(IconDrawable(k, tint)) },
                    FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
                )
                row.addView(chip, lp(dp(44), dp(44)).apply { marginEnd = dp(12) })
                val col = LinearLayout(this).apply { orientation = 1 }
                col.addView(label(t, 14f, textMain, true))
                col.addView(label(desc, 12f, muted).apply { setLineSpacing(0f, 1.15f) },
                    lp(-2, -2).apply { topMargin = dp(4) })
                row.addView(col, lp(0, -2).apply { weight = 1f })
                box.addView(row, lp(-1, -2).apply { bottomMargin = dp(10) })
            }
            item(WalletIc.COIN, gold, "عملات الشحن", "تُستخدم لإرسال الهدايا داخل التطبيق.")
            item(WalletIc.GEM, textMain, "الألماس", "هو ما تكسبه من الهدايا.")
            item(WalletIc.WALLET, mint, "الأرباح القابلة للسحب", "رصيد منفصل بالدولار.")
            item(WalletIc.SHIELD, pink, "المبالغ المحجوزة", "مرتبطة بطلبات السحب قيد المراجعة.")
            box.addView(pillButton("فهمت", null, true) { d.dismiss() }, lp(-1, -2).apply { topMargin = dp(8) })
        }
    }

    private fun showHistory() {
        sheet("سجل العمليات") { box, _ ->
            val list = LinearLayout(this).apply { orientation = 1 }
            var count = 0
            for (i in 0 until history.length()) {
                val t = history.optJSONObject(i) ?: continue
                list.addView(txRow(t), lp(-1, -2).apply { bottomMargin = dp(8) })
                count++
            }
            if (count == 0) {
                val e = LinearLayout(this).apply {
                    orientation = 1; gravity = Gravity.CENTER
                    setPadding(0, dp(36), 0, dp(36))
                }
                e.addIc(WalletIc.HISTORY, 44, muted)
                e.addView(
                    label("لا توجد عمليات حتى الآن", 13f, muted),
                    lp(-2, -2).apply { topMargin = dp(12) }
                )
                box.addView(e, lp(-1, -2))
            } else {
                val sv = ScrollView(this).apply {
                    isVerticalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    addView(list)
                }
                box.addView(sv, lp(-1, if (count > 5) dp(430) else -2))
            }
        }
    }

    private fun typeLabel(t: String): String {
        val k = t.lowercase(Locale.US)
        return when {
            k.isBlank() -> "عملية"
            k.contains("gift") && (k.contains("sent") || k.contains("send")) -> "هدية مُرسلة"
            k.contains("gift") -> "هدية مُستلمة"
            k.contains("withdraw") || k.contains("payout") -> "طلب سحب"
            k.contains("recharge") || k.contains("purchase") || k.contains("topup") -> "شحن عملات"
            else -> t.replace("_", " ")
        }
    }

    private fun statusInfo(s: String): Pair<String, Int> {
        val k = s.lowercase(Locale.US)
        return when {
            k in listOf("completed", "success", "succeeded", "paid", "approved") -> "مكتمل" to mint
            k in listOf("pending", "processing", "review", "in_review", "held") -> "قيد المراجعة" to gold
            k in listOf("failed", "rejected", "cancelled", "canceled", "declined") -> "مرفوض" to red
            else -> (if (s.isBlank()) "—" else s) to muted
        }
    }

    private fun txRow(t: JSONObject): View {
        val type = t.optString("transaction_type")
        val cur = t.optString("currency")
        val amt = t.optLong("amount")
        val k = type.lowercase(Locale.US)
        val out = amt < 0 || k.contains("send") || k.contains("withdraw") || k.contains("spend") || k.contains("debit")
        val accent = if (out) red else mint
        val isUsd = cur.equals("usd_cents", true) || cur.equals("usd", true)
        val abs = Math.abs(amt)
        val valueText = (if (out) "−" else "+") + (if (isUsd) "$" + money(abs) else fmt(abs))
        val unit = when {
            isUsd -> "دولار"
            cur.contains("diamond", true) -> "ألماس"
            cur.contains("coin", true) || cur.isBlank() -> "عملة"
            else -> cur
        }

        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rr(soft, 16, softStroke, 1)
        }
        val chip = FrameLayout(this).apply { background = oval(Color.argb(40, Color.red(accent), Color.green(accent), Color.blue(accent))) }
        chip.addView(
            ImageView(this).apply { setImageDrawable(IconDrawable(if (out) WalletIc.UP else WalletIc.DOWN, accent)) },
            FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        )
        row.addView(chip, lp(dp(42), dp(42)).apply { marginEnd = dp(12) })

        val mid = LinearLayout(this).apply { orientation = 1 }
        mid.addView(label(typeLabel(type), 14f, textMain, true))
        val date = t.optString("created_at").replace("T", " ").take(16)
        if (date.isNotBlank()) mid.addView(label(date, 10.5f, muted), lp(-2, -2).apply { topMargin = dp(4) })
        val (stText, stColor) = statusInfo(t.optString("status"))
        mid.addView(
            TextView(this).apply {
                text = stText; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(stColor)
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = rr(Color.argb(34, Color.red(stColor), Color.green(stColor), Color.blue(stColor)), 8)
            },
            lp(-2, -2).apply { topMargin = dp(6) }
        )
        row.addView(mid, lp(0, -2).apply { weight = 1f })

        val end = LinearLayout(this).apply { orientation = 1; gravity = Gravity.END }
        end.addView(label(valueText, 16f, accent, true))
        end.addView(label(unit, 10.5f, muted), lp(-2, -2).apply { topMargin = dp(4) })
        row.addView(end)
        return row
    }
}

// ═════════════════════ Drawables (no XML / no image assets needed) ═════════════════════

private enum class WalletIc { COIN, GEM, HISTORY, HELP, BACK, WALLET, SHIELD, UP, DOWN, CLOSE, WARN, STAR }

private abstract class Dr : Drawable() {
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private class IconDrawable(private val k: WalletIc, private val tint: Int = Color.WHITE) : Dr() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun line(w: Float = 2f) {
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND; p.color = tint
    }

    private fun solid(c: Int) { p.shader = null; p.style = Paint.Style.FILL; p.color = c }

    private fun star(cx: Float, cy: Float, ro: Float, ri: Float): Path {
        val s = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) ro else ri
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = (cx + r * Math.cos(a)).toFloat()
            val y = (cy + r * Math.sin(a)).toFloat()
            if (i == 0) s.moveTo(x, y) else s.lineTo(x, y)
        }
        s.close()
        return s
    }

    override fun draw(c: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate(b.left + (b.width() - s) / 2f, b.top + (b.height() - s) / 2f)
        c.scale(s / 24f, s / 24f)
        when (k) {
            WalletIc.COIN -> {
                p.style = Paint.Style.FILL
                p.shader = RadialGradient(
                    9f, 8f, 17f,
                    intArrayOf(0xFFFFF0B0.toInt(), 0xFFFFC857.toInt(), 0xFFC98A12.toInt()),
                    floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP
                )
                c.drawCircle(12f, 12f, 11f, p)
                p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f; p.color = 0x66A8700A
                c.drawCircle(12f, 12f, 8.3f, p)
                solid(0x99B8780C.toInt())
                c.drawPath(star(12f, 12.3f, 4.8f, 2.1f), p)
            }
            WalletIc.GEM -> {
                val g = Path().apply {
                    moveTo(6f, 3f); lineTo(18f, 3f); lineTo(23f, 9f); lineTo(12f, 22f); lineTo(1f, 9f); close()
                }
                p.style = Paint.Style.FILL
                p.shader = LinearGradient(
                    0f, 3f, 24f, 22f,
                    intArrayOf(0xFFB5F0FF.toInt(), 0xFF45B0F5.toInt(), 0xFF2467D6.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                c.drawPath(g, p)
                p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 1.1f
                p.strokeJoin = Paint.Join.ROUND; p.color = 0x80FFFFFF.toInt()
                c.drawLine(1f, 9f, 23f, 9f, p)
                val f = Path().apply {
                    moveTo(6f, 3f); lineTo(9f, 9f); lineTo(12f, 22f)
                    moveTo(18f, 3f); lineTo(15f, 9f); lineTo(12f, 22f)
                    moveTo(9f, 9f); lineTo(12f, 3f); lineTo(15f, 9f)
                }
                c.drawPath(f, p)
            }
            WalletIc.HISTORY -> {
                line(); c.drawCircle(12f, 12f, 9f, p)
                val h = Path().apply { moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f) }
                c.drawPath(h, p)
            }
            WalletIc.HELP -> {
                line(); c.drawCircle(12f, 12f, 9.5f, p)
                solid(tint); p.textSize = 14f; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER
                c.drawText("?", 12f, 17.2f, p)
            }
            WalletIc.BACK -> {
                line(2.4f)
                c.drawPath(Path().apply { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }, p)
            }
            WalletIc.WALLET -> {
                line(1.9f)
                c.drawRoundRect(RectF(3f, 6f, 21f, 20f), 3.5f, 3.5f, p)
                c.drawPath(Path().apply { moveTo(3.5f, 9.5f); lineTo(3.5f, 5f); lineTo(17f, 4f); lineTo(17f, 6f) }, p)
                solid(tint); c.drawCircle(16.5f, 13f, 1.5f, p)
            }
            WalletIc.SHIELD -> {
                line(1.9f)
                val sh = Path().apply {
                    moveTo(12f, 2f); lineTo(20f, 5.5f); lineTo(20f, 12f)
                    cubicTo(20f, 17f, 16f, 20.5f, 12f, 22f)
                    cubicTo(8f, 20.5f, 4f, 17f, 4f, 12f); lineTo(4f, 5.5f); close()
                }
                c.drawPath(sh, p)
                c.drawPath(Path().apply { moveTo(8.5f, 12f); lineTo(11f, 14.5f); lineTo(15.5f, 9.5f) }, p)
            }
            WalletIc.UP -> {
                line(2.2f)
                c.drawLine(12f, 19f, 12f, 5f, p)
                c.drawPath(Path().apply { moveTo(6f, 11f); lineTo(12f, 5f); lineTo(18f, 11f) }, p)
            }
            WalletIc.DOWN -> {
                line(2.2f)
                c.drawLine(12f, 5f, 12f, 19f, p)
                c.drawPath(Path().apply { moveTo(6f, 13f); lineTo(12f, 19f); lineTo(18f, 13f) }, p)
            }
            WalletIc.CLOSE -> {
                line(2.4f)
                c.drawLine(6f, 6f, 18f, 18f, p); c.drawLine(18f, 6f, 6f, 18f, p)
            }
            WalletIc.WARN -> {
                line(2f)
                c.drawPath(Path().apply { moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close() }, p)
                c.drawLine(12f, 9.5f, 12f, 14f, p)
                solid(tint); c.drawCircle(12f, 17f, 1.2f, p)
            }
            WalletIc.STAR -> {
                solid(tint); c.drawPath(star(12f, 12.5f, 10f, 4.4f), p)
            }
        }
        c.restore()
    }
}

private class CoinPile(private val tier: Int) : Dr() {
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coin = IconDrawable(WalletIc.COIN)

    override fun draw(c: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()).toFloat()
        val cx = b.exactCenterX(); val cy = b.exactCenterY()
        glow.shader = RadialGradient(
            cx, cy, s / 2f, intArrayOf(0x55FFC857, 0x00FFC857), null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, s / 2f, glow)
        val cs: Float
        val pos: List<Pair<Float, Float>>
        when (tier) {
            1 -> { cs = s * .62f; pos = listOf(.19f to .19f) }
            2 -> { cs = s * .52f; pos = listOf(.06f to .34f, .42f to .16f) }
            else -> { cs = s * .48f; pos = listOf(.03f to .40f, .49f to .40f, .26f to .08f) }
        }
        for ((fx, fy) in pos) {
            val l = b.left + fx * s
            val t = b.top + fy * s
            coin.setBounds(l.toInt(), t.toInt(), (l + cs).toInt(), (t + cs).toInt())
            coin.draw(c)
        }
    }
}

private class HeroDrawable(private val radius: Float, private val sw: Float) : Dr() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ov = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun draw(c: Canvas) {
        val b = RectF(bounds)
        fill.shader = LinearGradient(
            b.left, b.top, b.right, b.bottom,
            intArrayOf(0xFF7B3FE4.toInt(), 0xFF4B2290.toInt(), 0xFF2A1450.toInt()),
            floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(b, radius, radius, fill)
        c.save()
        c.clipPath(Path().apply { addRoundRect(b, radius, radius, Path.Direction.CW) })
        ov.color = 0x22FFFFFF
        c.drawCircle(b.right - b.width() * .1f, b.top + b.height() * .08f, b.width() * .38f, ov)
        ov.color = 0x1AFFC857
        c.drawCircle(b.left + b.width() * .05f, b.bottom, b.width() * .30f, ov)
        c.restore()
        edge.strokeWidth = sw * 2f
        edge.shader = LinearGradient(
            b.left, b.top, b.right, b.bottom,
            intArrayOf(0xAAFFC857.toInt(), 0x22FFFFFF, 0xAAFFC857.toInt()),
            null, Shader.TileMode.CLAMP
        )
        val inset = sw
        c.drawRoundRect(
            RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset),
            radius, radius, edge
        )
    }
}