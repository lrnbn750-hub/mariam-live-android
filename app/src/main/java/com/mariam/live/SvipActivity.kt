package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Royal SVIP membership screen. Prices and assets are always loaded from the
 * backend; the client never invents a price or grants a membership locally.
 */
class SvipActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val gold = Color.rgb(226, 184, 91)
    private val dark = Color.rgb(13, 12, 19)
    private val panel = Color.rgb(29, 26, 36)
    private val muted = Color.rgb(183, 177, 165)
    private lateinit var token: String
    private lateinit var content: LinearLayout
    private lateinit var statusText: TextView
    private var activeSubscription = false
    private var currentLevel = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            finish()
            return
        }
        window.statusBarColor = dark
        window.navigationBarColor = dark
        buildScreen()
        loadMembership()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        val back = label("‹", 34f, gold, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(44)))
        header.addView(label("عضوية ROYAL SVIP", 19f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(label("1 — 7", 13f, gold, true))
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(28))
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(65, 48, 26), Color.rgb(24, 22, 29), Color.rgb(12, 12, 18))
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(139, 107, 53))
            }
        }
        hero.addView(label("✦", 34f, gold, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("امتيازات ملكية لمدة 30 يوماً", 17f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(5))
        })
        hero.addView(label("إطار • شارة • فقاعة دردشة • دخول مميز", 12f, muted).apply {
            gravity = Gravity.CENTER
        })
        content.addView(hero, LinearLayout.LayoutParams(-1, -2))
        statusText = label("جارٍ تحميل عضويتك...", 13f, gold, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(15), dp(12), dp(15))
            background = rounded(panel, 14, Color.rgb(73, 61, 43))
        }
        content.addView(statusText, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
            bottomMargin = dp(16)
        })
        content.addView(label("اختر مستوى SVIP", 18f, Color.WHITE, true).apply {
            setPadding(dp(2), dp(2), dp(2), dp(10))
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun loadMembership() {
        statusText.text = "جارٍ تحميل المستويات والاشتراك..."
        worker.execute {
            try {
                val levelsResult = AuthApi.get("/api/svip/levels", token)
                if (levelsResult.status !in 200..299) {
                    throw Exception(levelsResult.body.optString("error", "تعذر تحميل مستويات SVIP"))
                }
                val meResult = AuthApi.get("/api/svip/me", token)
                if (meResult.status !in 200..299) {
                    throw Exception(meResult.body.optString("error", "تعذر تحميل حالة الاشتراك"))
                }
                val levels = levelsResult.body.optJSONArray("levels")
                    ?: levelsResult.body.optJSONArray("items")
                    ?: levelsResult.body.optJSONArray("data")
                    ?: JSONArray()
                val subscription = extractSubscription(meResult.body)
                val level = subscription?.optInt("level",
                    subscription.optInt("svip_level", 0)) ?: 0
                val status = subscription?.optString("status").orEmpty()
                val active = subscription != null && level in 1..7 &&
                    if (subscription.has("active")) subscription.optBoolean("active")
                    else status.equals("active", true)
                val expiry = subscription?.optString("expires_at").orEmpty()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    activeSubscription = active
                    currentLevel = if (active) level else 0
                    statusText.text = if (active) {
                        "عضويتك الحالية: SVIP " + level +
                            if (expiry.isNotBlank() && expiry != "null") "\nتنتهي: " + expiry
                            else "\nالاشتراك فعال"
                    } else {
                        "لا يوجد اشتراك SVIP فعال حالياً"
                    }
                    renderLevels(levels)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        statusText.text = error.message ?: "تعذر الاتصال بالخادم"
                        Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun extractSubscription(body: JSONObject): JSONObject? {
        val keys = listOf("subscription", "active_subscription", "current_subscription", "membership", "svip")
        for (key in keys) {
            val item = body.optJSONObject(key)
            if (item != null) return item
        }
        return if (body.has("level") || body.has("svip_level")) body else null
    }

    private fun renderLevels(levels: JSONArray) {
        if (content.childCount > 2) {
            content.removeViews(2, content.childCount - 2)
        }
        if (levels.length() == 0) {
            content.addView(label("لا توجد مستويات متاحة حالياً. يرجى المحاولة لاحقاً.", 13f, muted))
            return
        }
        for (index in 0 until levels.length()) {
            val item = levels.optJSONObject(index) ?: continue
            val level = item.optInt("level", index + 1)
            if (level !in 1..7) continue
            val name = item.optString("name").takeIf { it.isNotBlank() && it != "null" }
                ?: "SVIP " + level
            val price = if (item.isNull("price_coins")) 0L else item.optLong("price_coins", 0L)
            val enabled = item.optBoolean("active", true)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = rounded(panel, 17,
                    if (level == currentLevel) gold else Color.rgb(58, 52, 43))
            }
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            top.addView(label("✦  " + name, 16f, gold, true),
                LinearLayout.LayoutParams(0, dp(30), 1f))
            top.addView(label("المستوى " + level, 11f, muted, true))
            card.addView(top)
            card.addView(label(
                if (price > 0) NumberFormat.getNumberInstance(Locale.US).format(price) + " عملة"
                else "السعر يحدده فريق الإدارة",
                13f, Color.WHITE, true
            ).apply { setPadding(0, dp(5), 0, dp(9)) })
            val action = TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(
                    if (activeSubscription || !enabled || price <= 0L) Color.rgb(65, 62, 61)
                    else Color.rgb(139, 103, 43), 12)
                setTextColor(if (activeSubscription || !enabled || price <= 0L) muted else Color.WHITE)
                text = when {
                    level == currentLevel && activeSubscription -> "اشتراكك الحالي"
                    activeSubscription -> "متاح بعد انتهاء اشتراكك"
                    !enabled -> "غير متاح حالياً"
                    price <= 0L -> "قريباً"
                    else -> "اشترك لمدة 30 يوماً بالعملات"
                }
                isEnabled = !activeSubscription && enabled && price > 0L
                setOnClickListener { confirmPurchase(level, name, price) }
            }
            card.addView(action, LinearLayout.LayoutParams(-1, dp(42)))
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(11)
            })
        }
    }

    private fun confirmPurchase(level: Int, name: String, price: Long) {
        if (activeSubscription) {
            Toast.makeText(this, "يمكنك شراء مستوى جديد بعد انتهاء اشتراكك الحالي", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("تأكيد اشتراك SVIP")
            .setMessage("هل تريد الاشتراك في " + name + " لمدة 30 يوماً مقابل " +
                NumberFormat.getNumberInstance(Locale.US).format(price) + " عملة؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد") { _, _ -> purchase(level) }
            .show()
    }

    private fun purchase(level: Int) {
        statusText.text = "جارٍ تنفيذ الاشتراك..."
        worker.execute {
            try {
                val result = AuthApi.post("/api/svip/purchase",
                    JSONObject().put("level", level), token)
                if (result.status !in 200..299) {
                    throw Exception(result.body.optString("error", "تعذر إتمام الاشتراك"))
                }
                runOnUiThread {
                    if (!isFinishing) {
                        Toast.makeText(this, "تم تفعيل عضوية SVIP بنجاح", Toast.LENGTH_LONG).show()
                        loadMembership()
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        statusText.text = error.message ?: "فشل الاشتراك"
                        Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
