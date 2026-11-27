package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/** Native SVIP page. No WebView/HTML startup cost. */
class SvipActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val ink = Color.rgb(22, 27, 39)
    private val deep = Color.rgb(27, 33, 48)
    private val panel = Color.rgb(35, 41, 56)
    private val line = Color.rgb(58, 65, 84)
    private val cream = Color.rgb(239, 223, 192)
    private val muted = Color.rgb(127, 136, 155)
    private val green = Color.rgb(95, 214, 164)
    private var token = ""
    private var balance = 0L
    private var owned = 0
    private var expiresAt = ""
    private var selected = 1
    private var loading = false
    private var packages = mutableListOf<SvipPackage>()
    private lateinit var page: LinearLayout
    private lateinit var tabs: LinearLayout
    private lateinit var badge: BadgeView
    private lateinit var eliteCard: LinearLayout
    private lateinit var perksGrid: LinearLayout
    private lateinit var priceDetails: TextView
    private lateinit var bottomPrice: TextView
    private lateinit var bottomState: TextView
    private lateinit var balanceView: TextView
    private lateinit var buyButton: TextView
    private lateinit var memberButton: TextView
    private lateinit var scroll: ScrollView

    private data class SvipPackage(val level: Int, val name: String, val price: Long, val active: Boolean)

    private val perks = listOf(
        Triple("⚡", 1, "مؤثرات الدخول"),
        Triple("◎", 2, "براويز مميزة"),
        Triple("✦", 1, "شارة SVIP"),
        Triple("♡", 1, "المتابعة *%d%%"),
        Triple("♧", 2, "مزيد من الأصدقاء"),
        Triple("🚘", 3, "سيارة مميزة"),
        Triple("♧", 2, "دعم متخصص"),
        Triple("▤", 3, "فقاعات دردشة"),
        Triple("▣", 3, "بطاقة تعريف مميزة"),
        Triple("◉", 2, "ألوان دردشة مميزة"),
        Triple("✉", 4, "رسائل بارزة في الدردشة"),
        Triple("⬡", 3, "منع الطرد"),
        Triple("⊘", 3, "منع الكتم من التعليقات"),
        Triple("★", 6, "أولوية الظهور"),
        Triple("⌂", 6, "غرف SVIP حصرية")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        window.statusBarColor = ink
        window.navigationBarColor = ink
        window.decorView.systemUiVisibility = 0
        buildPage()
        applyCachedData()
        loadData()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fmt(v: Long) = NumberFormat.getNumberInstance(Locale.US).format(v)
    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }

    private fun lp(w: Int, h: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight)

    private fun buildPage() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        val back = text("‹", 34f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(back, lp(dp(38), dp(42)))
        header.addView(text("SVIP", 18f, Color.WHITE, true), lp(0, dp(42), 1f))
        val wallet = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(deep, 22, line)
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }
        wallet.addView(text("◆", 15f, Color.rgb(127, 216, 255), true))
        balanceView = text("—", 12f, Color.WHITE, true)
        wallet.addView(balanceView, lp(-2, -2).apply { marginStart = dp(6) })
        header.addView(wallet)
        memberButton = text("♛ عضويتي", 12f, cream, true).apply {
            background = rounded(Color.rgb(59, 52, 64), 20, Color.rgb(128, 109, 72))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            visibility = View.GONE
            setOnClickListener { startActivity(Intent(this@SvipActivity, SvipMemberActivity::class.java)) }
        }
        header.addView(memberButton, lp(-2, -2).apply { marginStart = dp(6) })
        root.addView(header)

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(5), dp(2), dp(5), dp(4))
        }
        tabScroll.addView(tabs)
        root.addView(tabScroll, lp(-1, dp(43)))

        scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(16))
        }
        scroll.addView(page)
        root.addView(scroll, lp(-1, 0, 1f))

        badge = BadgeView(this).apply { setOnTouchListener(SwipeTouchListener { delta -> select(selected + delta) }) }
        page.addView(badge, lp(-1, dp(160)))
        val perksTitle = text("✦     الامتيازات     ✦", 15f, cream, true).apply { gravity = Gravity.CENTER }
        page.addView(perksTitle, lp(-1, dp(36)))

        eliteCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(33, 39, 54), 14, Color.rgb(86, 94, 114))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        eliteCard.addView(text("امتيازات النخبة", 13f, cream, true).apply { gravity = Gravity.CENTER })
        eliteCard.addView(text("◉", 30f, muted, true).apply { gravity = Gravity.CENTER }, lp(-1, dp(48)))
        eliteCard.addView(text("دخول رجل غامض الغرفة للتو.", 11f, muted).apply { gravity = Gravity.CENTER })
        eliteCard.setOnClickListener { showEliteInfo() }
        page.addView(eliteCard, lp(-1, -2).apply { bottomMargin = dp(12) })

        perksGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(perksGrid, lp(-1, -2))

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 13, line)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        details.addView(text("تفاصيل العضوية", 14f, Color.WHITE, true), lp(-1, dp(30)))
        val priceRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        priceRow.addView(text("سعر الباقة", 12f, muted), lp(0, dp(30), 1f))
        priceDetails = text("—", 13f, Color.WHITE, true)
        priceRow.addView(priceDetails)
        details.addView(priceRow)
        val duration = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        duration.addView(text("مدة العضوية", 12f, muted), lp(0, dp(30), 1f))
        duration.addView(text("30 يوماً", 13f, Color.WHITE, true))
        details.addView(duration)
        details.addView(text("يتم التحقق من السعر والرصيد وتنفيذ الاشتراك من خادم BAK LIVE.", 11f, muted).apply {
            setLineSpacing(dp(2).toFloat(), 1.15f)
        }, lp(-1, -2).apply { topMargin = dp(6) })
        page.addView(details, lp(-1, -2).apply { topMargin = dp(14) })

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(deep, 0, line)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        val priceCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val priceLine = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bottomPrice = text("—", 16f, Color.WHITE, true)
        priceLine.addView(bottomPrice)
        priceLine.addView(text(" ◆ / 30 يوم", 11f, muted), lp(-2, -2).apply { marginStart = dp(5) })
        priceCol.addView(priceLine)
        bottomState = text("جارٍ تحميل الباقات…", 11f, muted)
        priceCol.addView(bottomState, lp(-1, -2).apply { topMargin = dp(3) })
        bottom.addView(priceCol, lp(0, -2, 1f))
        buyButton = text("شراء", 13f, Color.rgb(45, 36, 17), true).apply {
            gravity = Gravity.CENTER
            background = rounded(cream, 22)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener { confirmPurchase() }
        }
        bottom.addView(buyButton, lp(-2, dp(40)).apply { marginStart = dp(10) })
        root.addView(bottom)
        setContentView(root)
    }

    private fun renderTabs() {
        tabs.removeAllViews()
        packages.sortedBy { it.level }.forEach { p ->
            val t = text("SVIP ${p.level}", 13f, if (p.level == selected) Color.WHITE else muted, p.level == selected).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                if (p.level == owned) setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                setOnClickListener { select(p.level) }
                if (p.level == selected) background = rounded(Color.rgb(53, 58, 73), 10)
            }
            tabs.addView(t, lp(-2, dp(38)).apply { marginStart = dp(3); marginEnd = dp(3) })
        }
    }

    private fun select(level: Int) {
        if (packages.none { it.level == level }) return
        selected = level
        render()
        renderTabs()
    }

    private fun render() {
        val p = packages.firstOrNull { it.level == selected }
        if (p == null) {
            buyButton.isEnabled = false
            return
        }
        badge.level = selected
        badge.invalidate()
        balanceView.text = fmt(balance)
        memberButton.visibility = if (owned > 0) View.VISIBLE else View.GONE
        val isElite = selected >= 4
        eliteCard.alpha = if (isElite) 1f else .62f
        val rows = mutableListOf<List<Triple<String, Int, String>>>()
        perks.chunked(3).forEach { rows.add(it) }
        perksGrid.removeAllViews()
        rows.forEach { rowItems ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            rowItems.forEach { item ->
                val active = selected >= item.second
                val col = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(6), dp(3), dp(6))
                    alpha = if (active) 1f else .48f
                }
                val circle = TextView(this).apply {
                    text = item.first
                    textSize = 25f
                    gravity = Gravity.CENTER
                    setTextColor(if (active) cream else muted)
                    background = rounded(if (active) Color.rgb(55, 55, 68) else Color.rgb(38, 44, 59), 40,
                        if (active) Color.rgb(125, 111, 86) else line)
                }
                col.addView(circle, lp(dp(58), dp(58)))
                val title = if (item.first == "♡") "المتابعة *${selected * 100}%" else item.third
                col.addView(text(title, 10f, if (active) Color.rgb(210, 216, 228) else muted).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                }, lp(-1, dp(34)).apply { topMargin = dp(4) })
                if (!active) col.addView(text("من SVIP ${item.second}", 9f, muted).apply { gravity = Gravity.CENTER })
                row.addView(col, lp(0, -2, 1f))
            }
            repeat(3 - rowItems.size) { row.addView(View(this), lp(0, 1, 1f)) }
            perksGrid.addView(row, lp(-1, -2).apply { bottomMargin = dp(5) })
        }
        priceDetails.text = fmt(p.price) + " ◆"
        bottomPrice.text = fmt(p.price)
        val date = if (owned == selected && expiresAt.isNotBlank()) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(expiresAt.take(19))
                d?.let { " · تنتهي " + SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(it) }.orEmpty()
            } catch (_: Exception) { "" }
        } else ""
        bottomState.text = if (owned == selected) "عضويتك فعّالة$date" else "أنت لست SVIP $selected"
        bottomState.setTextColor(if (owned == selected) green else muted)
        buyButton.text = if (owned == selected) "تجديد" else "شراء"
        buyButton.isEnabled = p.active && p.price > 0
        buyButton.alpha = if (buyButton.isEnabled) 1f else .45f
    }

    private fun confirmPurchase() {
        val p = packages.firstOrNull { it.level == selected } ?: run {
            toast("لم يتم تحميل باقات SVIP بعد"); return
        }
        val cashback = (p.price * 40) / 100
        val enough = balance >= p.price && p.active
        val action = if (owned == p.level) "تجديد" else "شراء"
        val message = buildString {
            append("رصيدك الحالي ${fmt(balance)} ماسة. كل ترقية تُحاسب بالسعر الكامل للمستوى الجديد وتبدأ مدة 30 يوماً جديدة.\n\n")
            append("سعر الباقة: ${fmt(p.price)} ◆\n")
            append("مردود المحفظة (40%): +${fmt(cashback)} ◆\n")
            append("الصافي بعد المردود: ${fmt(p.price - cashback)} ◆\n")
            append("رصيدك بعد الشراء: ${if (enough) fmt(balance - p.price + cashback) else "—"} ◆\n")
            append("مدة العضوية: 30 يوماً")
            if (!enough) append("\n\nرصيدك لا يكفي. تحتاج إلى ${fmt((p.price - balance).coerceAtLeast(0))} ماسة إضافية.")
        }
        AlertDialog.Builder(this)
            .setTitle("$action SVIP ${p.level}")
            .setMessage(message)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد الشراء", if (enough) { _, _ -> purchase(p.level) } else null)
            .show()
    }

    private fun showEliteInfo() {
        AlertDialog.Builder(this)
            .setTitle("امتيازات النخبة")
            .setMessage("تبدأ من SVIP 4. عند دخولك الغرفة يظهر اسمك كرجل غامض بدل ظهور الإشعار المعتاد، فتدخل دون لفت الانتباه.")
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun purchase(level: Int) {
        buyButton.isEnabled = false
        io.execute {
            try {
                val result = AuthApi.post("/api/svip/purchase", JSONObject().put("level", level), token)
                if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                    val message = result.body.optString("error", result.body.optString("message", "تعذر إتمام شراء SVIP"))
                    runOnUiThread { toast(message); render() }
                    return@execute
                }
                val message = result.body.optString("message", "تم تفعيل عضوية SVIP $level بنجاح")
                loadData(message)
            } catch (e: Exception) {
                runOnUiThread { toast(e.message ?: "تعذر الاتصال بالخادم"); render() }
            }
        }
    }

    private fun applyCachedData() {
        try {
            val cached = getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE).getString("page_data", null) ?: return
            applyData(JSONObject(cached))
        } catch (_: Exception) { }
    }

    private fun loadData(successMessage: String? = null) {
        if (loading && successMessage == null) return
        loading = true
        io.execute {
            try {
                val levelsFuture = CompletableFuture.supplyAsync { AuthApi.get("/api/svip/levels", token) }
                val meFuture = CompletableFuture.supplyAsync { AuthApi.get("/api/svip/me", token) }
                val walletFuture = CompletableFuture.supplyAsync { AuthApi.get("/api/wallet", token) }
                val levelsResult = levelsFuture.get()
                val meResult = meFuture.get()
                val walletResult = walletFuture.get()
                if (levelsResult.status !in 200..299 || !levelsResult.body.optBoolean("success", false))
                    throw IllegalStateException(levelsResult.body.optString("error", "تعذر تحميل مستويات SVIP"))
                if (meResult.status !in 200..299 || !meResult.body.optBoolean("success", false))
                    throw IllegalStateException(meResult.body.optString("error", "تعذر تحميل عضويتك الحالية"))
                if (walletResult.status !in 200..299)
                    throw IllegalStateException(walletResult.body.optString("error", "تعذر تحميل رصيد المحفظة"))
                val raw = levelsResult.body.optJSONArray("levels") ?: levelsResult.body.optJSONArray("packages") ?: JSONArray()
                val pkgs = JSONArray()
                for (i in 0 until raw.length()) {
                    val item = raw.optJSONObject(i) ?: continue
                    val level = item.optInt("level", item.optInt("vip_level", 0))
                    val price = item.optLong("price_coins", item.optLong("price_diamonds", item.optLong("price", 0)))
                    val active = item.optBoolean("active", true) && item.optBoolean("is_active", true) && price > 0
                    if (level in 1..7) pkgs.put(JSONObject().put("level", level).put("name", item.optString("name", "SVIP $level")).put("price", price).put("active", active))
                }
                val sub = meResult.body.optJSONObject("subscription") ?: JSONObject()
                val wallet = walletResult.body.optJSONObject("wallet") ?: walletResult.body
                val data = JSONObject()
                    .put("packages", pkgs)
                    .put("membership", JSONObject()
                        .put("vip_level", sub.optInt("level", 0))
                        .put("expires_at", sub.optString("expires_at", "")))
                    .put("balance", wallet.optLong("recharge_coins", 0))
                getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE).edit()
                    .putString("page_data", data.toString())
                    .putLong("saved_at", System.currentTimeMillis()).apply()
                runOnUiThread {
                    loading = false
                    if (!isFinishing && !isDestroyed) {
                        applyData(data)
                        if (!successMessage.isNullOrBlank()) toast(successMessage)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false
                    if (!isFinishing && !isDestroyed) toast(e.message ?: "تعذر تحميل بيانات SVIP")
                }
            }
        }
    }

    private fun applyData(data: JSONObject) {
        val raw = data.optJSONArray("packages") ?: JSONArray()
        val next = mutableListOf<SvipPackage>()
        for (i in 0 until raw.length()) {
            val p = raw.optJSONObject(i) ?: continue
            val level = p.optInt("level", p.optInt("vip_level", 0))
            val price = p.optLong("price", p.optLong("price_diamonds", 0))
            if (level in 1..7 && price > 0) next.add(SvipPackage(level, p.optString("name", "SVIP $level"), price, p.optBoolean("active", true)))
        }
        packages = next.sortedBy { it.level }.toMutableList()
        balance = data.optLong("balance", 0)
        val membership = data.optJSONObject("membership") ?: JSONObject()
        owned = membership.optInt("vip_level", membership.optInt("level", 0))
        expiresAt = membership.optString("expires_at", "")
        if (packages.isNotEmpty()) {
            selected = if (packages.any { it.level == owned }) owned else if (packages.any { it.level == selected }) selected else packages.first().level
            renderTabs()
            render()
        } else {
            bottomState.text = "لا توجد مستويات SVIP متاحة حالياً"
            buyButton.isEnabled = false
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private inner class SwipeTouchListener(val onSwipe: (Int) -> Unit) : View.OnTouchListener {
        private var down = 0f
        override fun onTouch(v: View, e: android.view.MotionEvent): Boolean {
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> { down = e.x; return true }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = e.x - down
                    if (kotlin.math.abs(dx) > dp(45)) onSwipe(if (dx > 0) 1 else -1)
                    return true
                }
            }
            return true
        }
    }

    private class BadgeView(context: android.content.Context) : View(context) {
        var level: Int = 1
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            val cx = w / 2f; val cy = h / 2f
            val scale = minOf(w / 240f, h / 272f) * .88f
            canvas.save()
            canvas.translate(cx - 120f * scale, cy - 136f * scale)
            canvas.scale(scale, scale)
            val metals = intArrayOf(0xffc98a54.toInt(),0xffc3ccd8.toInt(),0xffe9b94a.toInt(),0xffefc15a.toInt(),0xfff2c660.toInt(),0xfff4cb68.toInt(),0xfff6d070.toInt())
            val gems = intArrayOf(0xffff9a4a.toInt(),0xff5ec8ff.toInt(),0xff3ddc97.toInt(),0xff4aa3ff.toInt(),0xffb36bff.toInt(),0xffff5fa8.toInt(),0xffff3b4a.toInt())
            val metal = metals[(level - 1).coerceIn(0, 6)]
            val gem = gems[(level - 1).coerceIn(0, 6)]
            p.style = Paint.Style.FILL
            p.shader = android.graphics.LinearGradient(80f, 40f, 160f, 235f, intArrayOf(Color.WHITE, metal, darken(metal), darken(metal)), null, android.graphics.Shader.TileMode.CLAMP)
            val shield = Path().apply {
                moveTo(120f, 46f); lineTo(178f, 70f); lineTo(178f, 144f)
                cubicTo(178f, 182f, 150f, 208f, 120f, 234f)
                cubicTo(90f, 208f, 62f, 182f, 62f, 144f); lineTo(62f, 70f); close()
            }
            canvas.drawPath(shield, p)
            p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = darken(metal)
            canvas.drawPath(shield, p)
            p.style = Paint.Style.FILL
            p.color = gem
            val diamond = Path().apply { moveTo(120f, 88f); lineTo(152f, 130f); lineTo(120f, 172f); lineTo(88f, 130f); close() }
            canvas.drawPath(diamond, p)
            p.color = Color.argb(90, 255, 255, 255); p.strokeWidth = 2f; p.style = Paint.Style.STROKE
            canvas.drawPath(diamond, p)
            p.style = Paint.Style.FILL; p.color = creamColor()
            val crown = Path().apply {
                moveTo(90f, 54f); lineTo(88f, 26f); lineTo(102f, 41f); lineTo(110f, 20f)
                lineTo(120f, 37f); lineTo(130f, 20f); lineTo(138f, 41f); lineTo(152f, 26f); lineTo(150f, 54f); close()
            }
            canvas.drawPath(crown, p)
            p.color = Color.WHITE; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create("serif", Typeface.BOLD); p.textSize = 20f
            canvas.drawText("SVIP $level", 120f, 199f, p)
            p.color = metal
            for (i in 0 until level.coerceAtMost(7)) {
                val x = if (i % 2 == 0) 47f + i * 7f else 193f - i * 5f
                val y = 74f + (i % 3) * 34f
                canvas.drawCircle(x, y, 2.8f, p)
            }
            canvas.restore()
        }
        private fun darken(color: Int): Int = Color.rgb((Color.red(color) * .48).toInt(), (Color.green(color) * .48).toInt(), (Color.blue(color) * .48).toInt())
        private fun creamColor() = Color.rgb(255, 235, 190)
    }
}
