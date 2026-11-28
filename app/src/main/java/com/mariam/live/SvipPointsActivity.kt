package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** SVIP loyalty points, self-service temporary unban, permanent-ban review and room trend support. */
class SvipPointsActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val bg = Color.rgb(13, 16, 24)
    private val panel = Color.rgb(25, 30, 44)
    private val line = Color.rgb(48, 56, 75)
    private val gold = Color.rgb(239, 207, 139)
    private val ink = Color.rgb(37, 30, 17)
    private val primary = Color.rgb(245, 242, 234)
    private val muted = Color.rgb(158, 168, 188)
    private val green = Color.rgb(91, 218, 164)
    private val red = Color.rgb(255, 126, 137)
    private lateinit var token: String
    private lateinit var balanceView: TextView
    private lateinit var historyBox: LinearLayout
    private lateinit var ownPublicId: EditText
    private lateinit var roomOwnerId: EditText
    private lateinit var statusView: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun fmt(v: Long) = NumberFormat.getNumberInstance(Locale.US).format(v)

    private fun formatBanDate(value: String, fallback: String = "غير متوفر"): String {
        val raw = value.trim()
        if (raw.isBlank() || raw == "null") return fallback
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val date: Date = parser.parse(raw) ?: continue
                return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date)
            } catch (_: Exception) {
                // Try the next ISO-8601 variant.
            }
        }
        return raw
    }
    private fun panelBg(radius: Int = 18) = GradientDrawable().apply {
        setColor(panel); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), line)
    }
    private fun tv(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
    private fun col() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
    private fun section(title: String, description: String): LinearLayout {
        val box = col().apply { background = panelBg(); setPadding(dp(16), dp(14), dp(16), dp(14)) }
        box.addView(tv(title, 16f, gold, true), lp(-1, -2))
        box.addView(tv(description, 12f, muted).apply { setLineSpacing(dp(2).toFloat(), 1.1f) },
            lp(-1, -2).apply { topMargin = dp(6) })
        return box
    }
    private fun input(hintText: String): EditText = EditText(this).apply {
        hint = hintText; setHintTextColor(muted); setTextColor(primary); textSize = 14f
        maxLines = 1; textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        setPadding(dp(12), 0, dp(12), 0)
        background = GradientDrawable().apply {
            setColor(Color.rgb(18, 22, 33)); cornerRadius = dp(12).toFloat(); setStroke(dp(1), line)
        }
    }
    private fun actionButton(title: String, action: () -> Unit) = tv(title, 13.5f, ink, true).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(gold, Color.rgb(203, 158, 82))).apply { cornerRadius = dp(13).toFloat() }
        setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { action() }
    }
    private fun showMessage(message: String, error: Boolean = false) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        statusView.text = message
        statusView.setTextColor(if (error) red else green)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_LONG).show()
            finish(); return
        }
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildPage()
        loadPoints()
    }

    private fun buildPage() {
        val root = col().apply { setBackgroundColor(bg); setPadding(dp(16), dp(14), dp(16), dp(18)) }
        val top = row()
        top.addView(tv("‹", 30f, primary, true).apply {
            gravity = Gravity.CENTER; setOnClickListener { finish() }
        }, lp(dp(42), dp(44)))
        top.addView(tv("نقاط SVIP", 20f, primary, true), LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(tv("تحديث", 12f, gold, true).apply {
            gravity = Gravity.CENTER; setOnClickListener { loadPoints() }
        }, lp(dp(54), dp(44)))
        root.addView(top)

        val balance = col().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(54, 43, 31), Color.rgb(29, 31, 43))).apply {
                cornerRadius = dp(20).toFloat(); setStroke(dp(1), Color.rgb(128, 103, 61))
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        balance.addView(tv("رصيد نقاط المكافآت", 13f, muted, true))
        balanceView = tv("…", 34f, gold, true)
        balance.addView(balanceView, lp(-1, dp(54)))
        balance.addView(tv("النقاط دائمة ولا تتحول إلى عملات أو ألماس", 11.5f, muted))
        root.addView(balance, lp(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(12) })

        val scroll = ScrollView(this).apply { isFillViewport = false; isVerticalScrollBarEnabled = false }
        val content = col()

        val unban = section("فك الحظر المؤقت",
            "أدخل Public ID لأي حساب محظور مؤقتاً. افحص تفاصيل الحظر ورصيدك أولاً، ثم أكّد فك الحظر.")
        ownPublicId = input("Public ID للحساب المراد فك حظره")
        unban.addView(ownPublicId, lp(-1, dp(48)).apply { topMargin = dp(12) })
        unban.addView(actionButton("معاينة الحظر والتكلفة", ::previewUnban),
            lp(-1, dp(48)).apply { topMargin = dp(10) })
        content.addView(unban, lp(-1, -2).apply { bottomMargin = dp(10) })

        val review = section("مراجعة الحظر الدائم",
            "يمكنك تقديم طلب مراجعة لحظر دائم عن طريق Public ID. لا تُخصم 75 نقطة عند الإرسال؛ الخصم فقط بعد موافقة المشرف.")
        review.addView(actionButton("معاينة الحظر وإرسال مراجعة", ::previewBanReview),
            lp(-1, dp(48)).apply { topMargin = dp(12) })
        content.addView(review, lp(-1, -2).apply { bottomMargin = dp(10) })

        val trend = section("دعم ترند الغرف",
            "أدخل Public ID لمالك الغرفة النشطة. التكلفة 20 نقطة ويستمر الدعم ساعتين. لا يمكنك دعم غرفتك.")
        roomOwnerId = input("Public ID لصاحب الغرفة")
        trend.addView(roomOwnerId, lp(-1, dp(48)).apply { topMargin = dp(12) })
        trend.addView(actionButton("دعم الترند بـ 20 نقطة", ::requestTrendSupport),
            lp(-1, dp(48)).apply { topMargin = dp(10) })
        content.addView(trend, lp(-1, -2).apply { bottomMargin = dp(10) })

        val history = section("سجل النقاط", "آخر 50 حركة على حسابك")
        historyBox = col()
        history.addView(historyBox, lp(-1, -2).apply { topMargin = dp(10) })
        content.addView(history, lp(-1, -2).apply { bottomMargin = dp(10) })

        statusView = tv("", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)) }
        content.addView(statusView, lp(-1, -2))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun hideKeyboard() {
        try {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (_: Exception) { }
    }

    private fun runAction(path: String, payload: JSONObject, success: (JSONObject) -> String) {
        hideKeyboard()
        statusView.text = "جارٍ تنفيذ الطلب…"
        statusView.setTextColor(muted)
        io.execute {
            try {
                val result = AuthApi.post(path, payload, token)
                val ok = result.status in 200..299 && result.body.optBoolean("success", false)
                val message = if (ok) success(result.body)
                    else result.body.optString("error", "تعذر تنفيذ الطلب (HTTP " + result.status + ")")
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        showMessage(message, !ok)
                        if (ok) loadPoints()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) showMessage(e.message ?: "تعذر الاتصال بالخادم", true)
                }
            }
        }
    }

    private fun previewUnban() = previewBanAction(permanent = false)

    private fun previewBanReview() = previewBanAction(permanent = true)

    /** Preview is read-only: it never deducts points or changes the ban. */
    private fun previewBanAction(permanent: Boolean) {
        val id = ownPublicId.text.toString().trim()
        if (id.isBlank()) { showMessage("أدخل Public ID للحساب أولاً", true); return }
        hideKeyboard()
        statusView.text = "جارٍ فحص الحظر والرصيد…"
        statusView.setTextColor(muted)
        io.execute {
            try {
                val encoded = java.net.URLEncoder.encode(id, "UTF-8")
                val result = AuthApi.get("/api/svip/points/unban-preview?public_id=" + encoded, token)
                if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                    val error = result.body.optString("error", "تعذر جلب تفاصيل الحظر")
                    runOnUiThread { if (!isFinishing && !isDestroyed) showMessage(error, true) }
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
                val created = formatBanDate(ban.optString("created_at"))
                val expires = formatBanDate(
                    ban.optString("expires_at"),
                    if (isPermanent) "دائم / بدون تاريخ انتهاء" else "غير متوفر"
                )
                val targetName = target.optString("username").ifBlank { id }
                val targetId = target.optString("public_id").ifBlank { id }
                val sufficient = body.optBoolean("points_sufficient", available >= required)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (banId.isBlank()) {
                        showMessage("لم يتم العثور على حظر نشط لهذا الحساب", true)
                        return@runOnUiThread
                    }
                    val details = "الحساب: $targetName ($targetId)\n" +
                        "نوع الحظر: $type\n" +
                        "السبب: $reason\n" +
                        "تاريخ الحظر: $created\n" +
                        "الانتهاء: $expires\n\n" +
                        "النقاط المطلوبة: ${fmt(required)}\n" +
                        "رصيدك الحالي: ${fmt(available)}\n\n" +
                        if (isPermanent)
                            "سيُرسل طلب مراجعة فقط. لا يوجد خصم عند الإرسال؛ تُخصم 75 نقطة فقط إذا وافق المشرف."
                        else
                            "لن يتم الخصم أو فك الحظر عند المعاينة. الخصم وفك الحظر يحدثان فقط بعد تأكيدك."
                    val dialog = AlertDialog.Builder(this)
                        .setTitle(if (isPermanent) "معاينة مراجعة الحظر الدائم" else "معاينة فك الحظر")
                        .setMessage(details)
                        .setNegativeButton("إلغاء", null)
                    if (!sufficient) {
                        dialog.setPositiveButton("رصيد غير كافٍ", null)
                    } else if (isPermanent) {
                        dialog.setPositiveButton("إرسال طلب المراجعة") { _, _ ->
                            runAction("/api/svip/points/ban-review",
                                JSONObject().put("public_id", targetId).put("ban_id", banId)) {
                                "تم إرسال طلب المراجعة. لا تُخصم النقاط إلا بعد موافقة المشرف."
                            }
                        }
                    } else {
                        dialog.setPositiveButton("تأكيد فك الحظر وخصم النقاط") { _, _ ->
                            runAction("/api/svip/points/unban",
                                JSONObject().put("public_id", targetId).put("ban_id", banId)) {
                                "تم فك الحظر. النقاط المستخدمة: " + it.optString("points_spent", fmt(required))
                            }
                        }
                    }
                    dialog.show()
                    if (!sufficient) showMessage("رصيدك غير كافٍ لإتمام هذا الإجراء", true)
                    else showMessage("تم عرض تفاصيل الحظر دون خصم نقاط", false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) showMessage(e.message ?: "تعذر الاتصال بالخادم", true)
                }
            }
        }
    }

    private fun requestTrendSupport() {
        val id = roomOwnerId.text.toString().trim()
        if (id.isBlank()) { showMessage("أدخل رقم صاحب الغرفة أولاً", true); return }
        runAction("/api/svip/points/trend-support", JSONObject().put("owner_public_id", id)) {
            "تم دعم الترند لمدة ساعتين. النقاط المستخدمة: 20"
        }
    }

    private fun loadPoints() {
        io.execute {
            try {
                val result = AuthApi.get("/api/svip/points", token)
                if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                    val error = result.body.optString("error", "تعذر تحميل نقاط SVIP")
                    runOnUiThread { if (!isFinishing && !isDestroyed) showMessage(error, true) }
                    return@execute
                }
                val balance = result.body.optLong("balance", 0)
                val history = result.body.optJSONArray("history") ?: JSONArray()
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    balanceView.text = fmt(balance)
                    historyBox.removeAllViews()
                    if (history.length() == 0) {
                        historyBox.addView(tv("لا توجد حركات نقاط حتى الآن", 12f, muted).apply {
                            gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12))
                        }, lp(-1, -2))
                    } else {
                        for (i in 0 until history.length()) {
                            val item = history.optJSONObject(i) ?: continue
                            val delta = item.optLong("delta")
                            val type = item.optString("transaction_type", "حركة نقاط")
                            val title = when (type) {
                                "purchase_reward" -> "مكافأة شراء SVIP"
                                "unban" -> "فك حظر مؤقت"
                                "ban_review_approved" -> "مراجعة حظر - موافقة"
                                "trend_support" -> "دعم ترند غرفة"
                                else -> type
                            }
                            val rr = row().apply { setPadding(0, dp(9), 0, dp(9)) }
                            rr.addView(tv(title, 12.5f, primary), LinearLayout.LayoutParams(0, -2, 1f))
                            rr.addView(tv((if (delta > 0) "+" else "") + fmt(delta), 13f,
                                if (delta > 0) green else red, true))
                            historyBox.addView(rr, lp(-1, -2))
                            if (i != history.length() - 1) historyBox.addView(View(this).apply {
                                setBackgroundColor(line)
                            }, lp(-1, dp(1)))
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { if (!isFinishing && !isDestroyed) showMessage(e.message ?: "تعذر تحميل الرصيد", true) }
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
