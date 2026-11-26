package com.mariam.live

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class InventoryActivity : Activity() {
    private val apiExecutor = Executors.newSingleThreadExecutor()
    private val assetExecutor = Executors.newFixedThreadPool(3)
    private val assetCache = ConcurrentHashMap<String, ByteArray>()

    // ───────── Palette ─────────
    private val purple = Color.rgb(112, 75, 246)
    private val purpleEnd = Color.rgb(148, 106, 255)
    private val purpleSoft = Color.rgb(246, 243, 255)
    private val ink = Color.rgb(24, 22, 38)
    private val muted = Color.rgb(112, 108, 130)
    private val border = Color.rgb(232, 229, 242)
    private val tile = Color.rgb(246, 244, 252)

    // ───────── Typography (ضع خطوطك في res/font بهذه الأسماء، وإلا يُستخدم خط النظام) ─────────
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    // ───────── Model ─────────
    private data class Item(
        val id: String, val name: String, val type: String,
        val url: String, val owned: Boolean, val equipped: Boolean,
        val priceCoins: Long, val expiresAt: String, val remainingSeconds: Long?
    )

    private val tabs = listOf(
        "all" to "الكل",
        "frame" to "الإطارات",
        "background" to "الخلفيات",
        "chat_bubble" to "الفقاعات"
    )

    private var items: List<Item> = emptyList()
    private var selectedTab = "all"
    private var token = ""

    private lateinit var chipsRow: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView

    private fun typeOf(i: Item) = when (i.type) {
        "frame" -> "frame"
        "background", "profile_background" -> "background"
        "chat_bubble" -> "chat_bubble"
        else -> "other"
    }

    private fun sectionTitle(t: String) = when (t) {
        "frame" -> "الإطارات"
        "background" -> "الخلفيات"
        "chat_bubble" -> "فقاعات الدردشة"
        else -> "عناصر أخرى"
    }

    private fun sectionHint(t: String) = when (t) {
        "frame" -> "تظهر حول صورتك الشخصية"
        "background" -> "تظهر في خلفية ملفك الشخصي"
        "chat_bubble" -> "تظهر في رسائلك داخل الغرف"
        else -> "عناصر تجميلية لحسابك"
    }

    private fun itemKind(t: String) = when (t) {
        "frame" -> "إطار صورة"
        "background" -> "خلفية ملف شخصي"
        "chat_bubble" -> "فقاعة دردشة"
        else -> "عنصر تجميلي"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags

        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()
        if (token.isBlank()) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(60)))

        chipsRow = LinearLayout(this).apply { setPadding(dp(16), dp(4), dp(16), dp(10)) }
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chipsRow)
        }, LinearLayout.LayoutParams(-1, -2))
        root.addView(View(this).apply { setBackgroundColor(Color.rgb(240, 238, 246)) }, LinearLayout.LayoutParams(-1, 1.coerceAtLeast(dp(1))))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(28))
        }
        scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        renderChips()
        loadInventory(silent = false)
    }

    // ───────── Header & chips ─────────

    private fun buildHeader(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)
        val back = FrameLayout(this@InventoryActivity).apply {
            background = RippleDrawable(ColorStateList.valueOf(Color.argb(30, 0, 0, 0)), rounded(purpleSoft, 22), null)
            isClickable = true
            isFocusable = true
            contentDescription = "رجوع"
            setOnClickListener { finish() }
            addView(InventoryIconView(this@InventoryActivity, Icon.BACK, purple), FrameLayout.LayoutParams(-1, -1))
        }
        addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(label(19f, fBold, ink).apply {
            text = "حقيبتي"
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(View(this@InventoryActivity), LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun renderChips() {
        chipsRow.removeAllViews()
        for ((key, title) in tabs) {
            val count = if (key == "all") items.size else items.count { typeOf(it) == key }
            val selected = key == selectedTab
            val chip = label(13.5f, fMedium, if (selected) Color.WHITE else ink).apply {
                text = if (items.isEmpty()) title else "$title $count"
                setPadding(dp(16), dp(9), dp(16), dp(9))
                background = if (selected) {
                    GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(purple, purpleEnd))
                        .apply { cornerRadius = dp(22).toFloat() }
                } else rounded(Color.rgb(243, 242, 248), 22)
                isClickable = true
                setOnClickListener {
                    if (selectedTab != key) {
                        selectedTab = key
                        renderChips()
                        render()
                        scroll.scrollTo(0, 0)
                    }
                }
            }
            chipsRow.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        }
    }

    // ───────── Data ─────────

    private fun parseItems(arr: JSONArray): List<Item> {
        val out = ArrayList<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Item(
                    o.optString("item_id"), o.optString("name", "عنصر"), o.optString("item_type"),
                    o.optString("asset_url"), o.optBoolean("owned"), o.optBoolean("equipped"),
                    o.optLong("price_coins", 0L), o.optString("expires_at"),
                    if (o.isNull("remaining_seconds")) null else o.optLong("remaining_seconds")
                )
            )
        }
        return out
    }

    private fun loadInventory(silent: Boolean) {
        if (!silent) showLoading()
        apiExecutor.execute {
            try {
                val response = AuthApi.get("/api/inventory", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل الحقيبة"))
                val parsed = parseItems(response.body.optJSONArray("items") ?: JSONArray())
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    items = parsed
                    renderChips()
                    render()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (silent) toast(e.message ?: "تعذر الاتصال بالخادم")
                    else showError(e.message ?: "تعذر الاتصال بالخادم")
                }
            }
        }
    }

    private fun purchaseItem(item: Item, button: TextView) {
        button.isEnabled = false
        button.text = "جارٍ الشراء..."
        apiExecutor.execute {
            try {
                val response = AuthApi.post(
                    "/api/inventory/purchase",
                    JSONObject().put("item_id", item.id),
                    token
                )
                if (response.status !in 200..299) {
                    throw Exception(response.body.optString("error", "تعذر إتمام عملية الشراء"))
                }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    toast("تم شراء العنصر بنجاح، صالح لمدة 30 يوماً")
                    loadInventory(silent = true)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    toast(e.message ?: "تعذر الاتصال بالخادم")
                    render()
                }
            }
        }
    }

    private fun toggleItem(item: Item, button: TextView) {
        button.isEnabled = false
        button.text = "جارٍ التنفيذ"
        apiExecutor.execute {
            try {
                val response = if (item.equipped) {
                    AuthApi.post("/api/inventory/unequip", JSONObject().put("item_type", item.type), token)
                } else {
                    AuthApi.post("/api/inventory/equip", JSONObject().put("item_id", item.id), token)
                }
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحديث العنصر"))
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    toast(if (item.equipped) "تم إلغاء التركيب" else "تم التركيب بنجاح")
                    loadInventory(silent = true)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    toast(e.message ?: "تعذر الاتصال بالخادم")
                    render()
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ───────── Screens: loading / error / empty ─────────

    private fun showLoading() {
        content.removeAllViews()
        content.addView(ProgressBar(this).apply {
            indeterminateTintList = ColorStateList.valueOf(purple)
        }, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(80)
        })
    }

    private fun showError(message: String) {
        content.removeAllViews()
        content.addView(stateView(Icon.BAG, "تعذر تحميل الحقيبة", message))
        content.addView(label(14.5f, fMedium, Color.WHITE).apply {
            text = "إعادة المحاولة"
            gravity = Gravity.CENTER
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(60, 255, 255, 255)), rounded(purple, 14), rounded(Color.WHITE, 14)
            )
            isClickable = true
            setOnClickListener { loadInventory(silent = false) }
        }, LinearLayout.LayoutParams(dp(180), dp(46)).apply { gravity = Gravity.CENTER_HORIZONTAL })
    }

    private fun stateView(icon: Icon, title: String, desc: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(56), dp(24), dp(24))
        addView(FrameLayout(this@InventoryActivity).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(purpleSoft) }
            addView(InventoryIconView(this@InventoryActivity, icon, purple), FrameLayout.LayoutParams(-1, -1).apply {
                setMargins(dp(22), dp(22), dp(22), dp(22))
            })
        }, LinearLayout.LayoutParams(dp(92), dp(92)))
        addView(label(17f, fBold, ink).apply { text = title; gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(18) })
        addView(label(13.5f, fRegular, muted).apply {
            text = desc; gravity = Gravity.CENTER; setLineSpacing(0f, 1.3f)
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8); bottomMargin = dp(22) })
    }

    private fun emptyState(): View {
        val (title, desc) = when (selectedTab) {
            "frame" -> "لا توجد إطارات بعد" to "الإطارات التي تحصل عليها من الإدارة أو العروض أو المتجر ستظهر هنا."
            "background" -> "لا توجد خلفيات بعد" to "خلفيات الحساب التي تحصل عليها من الإدارة أو العروض أو المتجر ستظهر هنا."
            "chat_bubble" -> "لا توجد فقاعات بعد" to "فقاعات الدردشة التي تحصل عليها من الإدارة أو العروض أو المتجر ستظهر هنا."
            else -> "حقيبتك فارغة حالياً" to "ستظهر هنا الإطارات والخلفيات والفقاعات التي تحصل عليها من الإدارة أو العروض أو المتجر."
        }
        return stateView(Icon.BAG, title, desc)
    }

    // ───────── Content (sections + grid) ─────────

    private fun render() {
        content.removeAllViews()
        val order = listOf("frame", "background", "chat_bubble", "other")
        val visible = if (selectedTab == "all") order else listOf(selectedTab)
        var any = false
        for (t in visible) {
            val group = items.filter { typeOf(it) == t }
                .sortedWith(compareByDescending<Item> { it.equipped }.thenByDescending { it.owned })
            if (group.isEmpty()) continue
            any = true
            if (selectedTab == "all") content.addView(sectionHeader(t, group.size))
            else content.addView(View(this), LinearLayout.LayoutParams(-1, dp(12)))
            addGrid(group, t)
        }
        if (!any) content.addView(emptyState())
    }

    private fun sectionHeader(t: String, count: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val titleRow = LinearLayout(this@InventoryActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(label(18f, fBold, ink).apply { text = sectionTitle(t) })
            addView(label(13f, fMedium, muted).apply { text = "$count" },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
        addView(titleRow)
        addView(label(13f, fRegular, muted).apply { text = sectionHint(t); setPadding(0, dp(3), 0, 0) })
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20); bottomMargin = dp(12) }
    }

    private fun addGrid(group: List<Item>, t: String) {
        val gap = dp(12)
        val cardW = (resources.displayMetrics.widthPixels - dp(32) - gap) / 2
        for (pair in group.chunked(2)) {
            val row = LinearLayout(this)
            pair.forEachIndexed { idx, item ->
                row.addView(itemCard(item, t, cardW), LinearLayout.LayoutParams(cardW, -2).apply {
                    if (idx > 0) marginStart = gap
                })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = gap })
        }
    }

    private fun itemCard(item: Item, t: String, cardW: Int): View {
        val pad = dp(10)
        val previewW = cardW - pad * 2
        val previewH = if (t == "background") (previewW * 1.25f).toInt() else previewW
        val mode = when (t) {
            "frame" -> Mode.FRAME
            "background" -> Mode.CROP
            "chat_bubble" -> Mode.BUBBLE
            else -> Mode.FIT
        }

        val preview = AssetPreview(this, mode).apply {
            background = rounded(tile, 14)
            clipToOutline = true
        }
        if (t == "chat_bubble") preview.setBubbleStyle(item.id)
        else if (item.url.startsWith("https://")) loadAsset(item, preview)

        val holder = FrameLayout(this)
        holder.addView(preview, FrameLayout.LayoutParams(-1, -1))

        if (!item.owned) {
            holder.addView(View(this).apply { background = rounded(Color.argb(130, 255, 255, 255), 14) },
                FrameLayout.LayoutParams(-1, -1))
            holder.addView(FrameLayout(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(210, 40, 38, 56)) }
                addView(InventoryIconView(this@InventoryActivity, Icon.LOCK, Color.WHITE), FrameLayout.LayoutParams(-1, -1).apply {
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                })
            }, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.START).apply {
                topMargin = dp(8); marginStart = dp(8)
            })
        }
        if (item.equipped) {
            holder.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(4), dp(10), dp(4))
                background = rounded(purple, 12)
                addView(InventoryIconView(this@InventoryActivity, Icon.CHECK, Color.WHITE), LinearLayout.LayoutParams(dp(12), dp(12)))
                addView(label(11.5f, fMedium, Color.WHITE).apply { text = "مركّب" },
                    LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(4) })
            }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
                topMargin = dp(8); marginStart = dp(8)
            })
        }

        val button = label(13f, fMedium, Color.WHITE).apply {
            gravity = Gravity.CENTER
            when {
                item.equipped -> {
                    text = "إلغاء التركيب"
                    setTextColor(purple)
                    background = RippleDrawable(
                        ColorStateList.valueOf(Color.argb(40, 112, 75, 246)),
                        GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), purple) },
                        rounded(Color.WHITE, 12)
                    )
                }
                item.owned -> {
                    text = "تركيب"
                    background = RippleDrawable(
                        ColorStateList.valueOf(Color.argb(60, 255, 255, 255)),
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(purple, purpleEnd))
                            .apply { cornerRadius = dp(12).toFloat() },
                        rounded(Color.WHITE, 12)
                    )
                }
                item.priceCoins > 0L -> {
                    text = "شراء • ${item.priceCoins} عملة"
                    background = RippleDrawable(
                        ColorStateList.valueOf(Color.argb(60, 255, 255, 255)),
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(18, 160, 112), Color.rgb(28, 190, 132)))
                            .apply { cornerRadius = dp(12).toFloat() },
                        rounded(Color.WHITE, 12)
                    )
                }
                else -> {
                    text = "غير متوفر حالياً"
                    setTextColor(Color.rgb(150, 146, 168))
                    background = rounded(Color.rgb(241, 240, 246), 12)
                }
            }
            isEnabled = item.owned || item.priceCoins > 0L
            when {
                item.owned -> setOnClickListener { toggleItem(item, this) }
                item.priceCoins > 0L -> setOnClickListener { purchaseItem(item, this) }
            }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), border)
            }
            addView(holder, LinearLayout.LayoutParams(previewW, previewH))
            addView(label(14f, fMedium, ink).apply {
                text = item.name; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(2), dp(10), dp(2), 0)
            })
            addView(label(12f, fRegular, muted).apply {
                text = itemKind(t); maxLines = 1
                setPadding(dp(2), dp(2), dp(2), dp(4))
            })
            if (!item.owned && item.priceCoins > 0L) {
                addView(label(11.5f, fMedium, Color.rgb(24, 145, 105)).apply {
                    text = "الصلاحية بعد الشراء: 30 يوماً"
                    maxLines = 1
                    setPadding(dp(2), 0, dp(2), dp(8))
                })
            }
            if (item.remainingSeconds != null) {
                addView(label(11.5f, fMedium, Color.rgb(190, 75, 55)).apply {
                    val days = item.remainingSeconds / 86400
                    val hours = (item.remainingSeconds % 86400) / 3600
                    text = if (days > 0) "متبقي $days يوم و$hours ساعة" else "متبقي $hours ساعة"
                    maxLines = 1
                    setPadding(dp(2), 0, dp(2), dp(8))
                })
            } else {
                addView(View(this@InventoryActivity), LinearLayout.LayoutParams(1, dp(8)))
            }
            addView(button, LinearLayout.LayoutParams(-1, dp(38)))
        }
    }

    // ───────── Assets ─────────

    private fun loadAsset(item: Item, preview: AssetPreview) {
        assetExecutor.execute {
            try {
                val bytes = assetCache[item.url] ?: URL(item.url).openConnection().apply {
                    connectTimeout = 12000; readTimeout = 15000
                }.getInputStream().use { it.readBytes() }.also {
                    if (it.size < 1_500_000) assetCache[item.url] = it
                }
                val isGif = bytes.size > 3 && bytes[0].toInt() == 0x47 && bytes[1].toInt() == 0x49 && bytes[2].toInt() == 0x46
                if (isGif) {
                    val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
                    runOnUiThread { if (!isFinishing) preview.setMovie(movie) }
                } else {
                    val bmp = decodeSampled(bytes, 640)
                    runOnUiThread { if (!isFinishing) preview.setBitmap(bmp) }
                }
            } catch (e: Exception) {
                android.util.Log.e("InventoryPreview", "Asset load failed for ${item.id}", e)
            }
        }
    }

    private fun decodeSampled(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim && bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    override fun onDestroy() {
        apiExecutor.shutdownNow()
        assetExecutor.shutdownNow()
        super.onDestroy()
    }
}

// ───────── Preview view: GIF / صورة ثابتة / فقاعة، مع معاينة الإطار على صورة افتراضية ─────────

private enum class Mode { FRAME, CROP, BUBBLE, FIT }

private class AssetPreview(context: Context, private val mode: Mode) : View(context) {
    private var movie: Movie? = null
    private var bitmap: Bitmap? = null
    private var bubbleStyle: ChatBubbleStyleDrawable? = null
    private var startedAt = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clip = Path()

    fun setBubbleStyle(styleId: String) {
        movie = null; bitmap = null
        bubbleStyle = ChatBubbleStyleDrawable(styleId).also { it.callback = this }
        invalidate()
    }

    fun setMovie(m: Movie?) {
        if (m == null) return
        bubbleStyle = null; bitmap = null
        movie = m
        startedAt = SystemClock.uptimeMillis()
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Movie لا يتحرك دائماً مع تسريع العتاد
        invalidate()
    }

    fun setBitmap(b: Bitmap?) {
        if (b == null) return
        bubbleStyle = null; movie = null
        bitmap = b
        invalidate()
    }

    private fun scaleFor(cw: Float, ch: Float): Float {
        val sx = width / cw.coerceAtLeast(1f)
        val sy = height / ch.coerceAtLeast(1f)
        return if (mode == Mode.CROP) maxOf(sx, sy) else minOf(sx, sy)
    }

    private fun drawAvatar(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.3f
        canvas.save()
        clip.reset()
        clip.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(clip)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(228, 224, 240)
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.rgb(200, 194, 222)
        canvas.drawCircle(cx, cy - r * 0.18f, r * 0.34f, paint)
        canvas.drawOval(RectF(cx - r * 0.7f, cy + r * 0.28f, cx + r * 0.7f, cy + r * 1.3f), paint)
        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        if (mode == Mode.FRAME) drawAvatar(canvas)

        val style = bubbleStyle
        if (style != null) {
            val padX = (width * 0.12f).toInt()
            style.bounds = Rect(padX, (height * 0.3f).toInt(), width - padX, (height * 0.7f).toInt())
            style.draw(canvas)
            postInvalidateOnAnimation()
            return
        }

        val bmp = bitmap
        if (bmp != null) {
            val scale = scaleFor(bmp.width.toFloat(), bmp.height.toFloat())
            val save = canvas.save()
            canvas.translate((width - bmp.width * scale) / 2f, (height - bmp.height * scale) / 2f)
            canvas.scale(scale, scale)
            canvas.drawBitmap(bmp, 0f, 0f, paint)
            canvas.restoreToCount(save)
            return
        }

        val gif = movie ?: return
        val duration = gif.duration().takeIf { it > 0 } ?: 1000
        gif.setTime(((SystemClock.uptimeMillis() - startedAt) % duration).toInt())
        val scale = scaleFor(gif.width().toFloat(), gif.height().toFloat())
        val save = canvas.save()
        canvas.translate((width - gif.width() * scale) / 2f, (height - gif.height() * scale) / 2f)
        canvas.scale(scale, scale)
        gif.draw(canvas, 0f, 0f, paint)
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }
}

// ───────── Icons مرسومة بالكود ─────────

private enum class Icon { BACK, CHECK, LOCK, BAG }

private class InventoryIconView(context: Context, private val icon: Icon, private val color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        path.reset()

        when (icon) {
            Icon.BACK -> {
                path.moveTo(w * .4f, h * .28f)
                path.lineTo(w * .6f, h * .5f)
                path.lineTo(w * .4f, h * .72f)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = w * .08f
                canvas.drawPath(path, paint)
            }
            Icon.CHECK -> {
                path.moveTo(w * .2f, h * .52f)
                path.lineTo(w * .42f, h * .72f)
                path.lineTo(w * .8f, h * .3f)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = w * .16f
                canvas.drawPath(path, paint)
            }
            Icon.LOCK -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = w * .11f
                canvas.drawArc(RectF(w * .32f, h * .12f, w * .68f, h * .56f), 180f, 180f, false, paint)
                canvas.drawLine(w * .32f, h * .34f, w * .32f, h * .5f, paint)
                canvas.drawLine(w * .68f, h * .34f, w * .68f, h * .5f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(w * .2f, h * .46f, w * .8f, h * .88f), w * .1f, w * .1f, paint)
            }
            Icon.BAG -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = w * .07f
                canvas.drawArc(RectF(w * .34f, h * .1f, w * .66f, h * .55f), 180f, 180f, false, paint)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(w * .16f, h * .34f, w * .84f, h * .88f), w * .12f, w * .12f, paint)
            }
        }
    }
}