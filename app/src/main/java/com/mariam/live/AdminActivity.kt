package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.net.URLEncoder
import android.text.InputType
import java.util.concurrent.Executors

/** Native administration screen. All authority is checked by Admin V2 on the server. */
class AdminActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val bg = Color.rgb(244, 245, 250)
    private val ink = Color.rgb(25, 29, 42)
    private val muted = Color.rgb(120, 129, 146)
    private lateinit var statsBox: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var usersBox: LinearLayout
    private lateinit var auditBox: LinearLayout
    private lateinit var agenciesBox: LinearLayout
    private lateinit var svipBox: LinearLayout
    private var agencyStatus = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.rgb(24, 18, 49)
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(30))
        }
        scroll.addView(page, ViewGroup.LayoutParams(-1, -2))
        setContentView(scroll)

        val hero = TextView(this).apply {
            text = "مركز الإدارة\nBAK LIVE  •  لوحة تحكم آمنة"
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(37, 23, 76), Color.rgb(28, 112, 111))
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        page.addView(hero, LinearLayout.LayoutParams(-1, dp(112)))
        page.addView(title("نظرة عامة", 18).apply { setPadding(0, dp(20), 0, dp(10)) })
        statsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        page.addView(statsBox)

        page.addView(title("إدارة مستويات SVIP", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        val refreshSvip = actionButton("تحديث مستويات SVIP")
        page.addView(refreshSvip, LinearLayout.LayoutParams(-1, dp(46)))
        refreshSvip.setOnClickListener { loadSvipLevels() }
        svipBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(svipBox)

        page.addView(title("إدارة المستخدمين", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        searchInput = EditText(this).apply {
            hint = "ابحث بالمعرّف أو اسم المستخدم"
            textSize = 14f
            isSingleLine = true
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(Color.WHITE)
            setPadding(dp(14), 0, dp(14), 0)
        }
        page.addView(searchInput, LinearLayout.LayoutParams(-1, dp(52)))
        val searchButton = actionButton("بحث عن مستخدم")
        page.addView(searchButton, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        searchButton.setOnClickListener { searchUsers() }
        usersBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(usersBox)

        page.addView(title("إدارة الوكالات", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        val agencyFilters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("pending" to "قيد المراجعة", "approved" to "مقبولة", "blocked" to "محظورة", "all" to "الكل").forEach { (status, label) ->
            val filter = actionButton(label)
            agencyFilters.addView(filter, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(4) })
            filter.setOnClickListener { agencyStatus = status; loadAgencies() }
        }
        page.addView(agencyFilters)
        agenciesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(agenciesBox)
        val refreshAgencies = actionButton("تحديث طلبات الوكالات")
        page.addView(refreshAgencies, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        refreshAgencies.setOnClickListener { loadAgencies() }

        page.addView(title("سجل العمليات الإدارية", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        val auditButton = actionButton("عرض آخر العمليات")
        page.addView(auditButton, LinearLayout.LayoutParams(-1, dp(48)))
        auditButton.setOnClickListener { loadAudit() }
        auditBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(auditBox)
        loadDashboard()
    }

    private fun loadDashboard() {
        val token = tokenOrClose() ?: return
        executor.execute {
            try {
                val me = AuthApi.get("/api/admin/v2/me", token)
                if (me.status !in 200..299) throw Exception(me.body.optString("error", "لا توجد صلاحية إدارة"))
                val response = AuthApi.get("/api/admin/v2/dashboard", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل الإحصائيات"))
                val data = response.body.optJSONObject("statistics") ?: JSONObject()
                val admin = me.body.optJSONObject("admin")
                runOnUiThread {
                    statsBox.removeAllViews()
                    statsBox.addView(line("مرحباً ${admin?.optString("username", "مدير") ?: "مدير"}", 16, true))
                    statsBox.addView(line("الصلاحية: ${admin?.optString("role_name", "إدارة") ?: "إدارة"}", 13, false, muted))
                    statsBox.addView(line("إجمالي المستخدمين: ${data.optString("total_users", "—")}", 15))
                    statsBox.addView(line("المستخدمون النشطون: ${data.optString("active_users", "—")}", 15))
                    statsBox.addView(line("المديرون النشطون: ${data.optString("active_admins", "—")}", 15))
                    loadSvipLevels()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر الاتصال بلوحة الإدارة", Toast.LENGTH_LONG).show(); finish() }
            }
        }
    }

    private fun loadSvipLevels() {
        val token = tokenOrClose() ?: return
        svipBox.removeAllViews()
        svipBox.addView(line("جارٍ تحميل إعدادات SVIP...", 14, false, muted))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/svip/levels", token)
                if (response.status !in 200..299) {
                    throw Exception(response.body.optString("error", "لا توجد صلاحية لإدارة SVIP"))
                }
                val levels = response.body.optJSONArray("levels")
                    ?: response.body.optJSONArray("items")
                    ?: org.json.JSONArray()
                runOnUiThread {
                    if (!isFinishing) renderSvipLevels(levels, token)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        svipBox.removeAllViews()
                        svipBox.addView(line(error.message ?: "تعذر تحميل إعدادات SVIP", 13, false, Color.rgb(190, 55, 65)))
                    }
                }
            }
        }
    }

    private fun renderSvipLevels(levels: org.json.JSONArray, token: String) {
        svipBox.removeAllViews()
        if (levels.length() == 0) {
            svipBox.addView(line("لا توجد مستويات SVIP في الخادم.", 13, false, muted))
            return
        }
        for (index in 0 until levels.length()) {
            val item = levels.optJSONObject(index) ?: continue
            val level = item.optInt("level", index + 1)
            if (level !in 1..7) continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(Color.WHITE)
                setPadding(dp(14), dp(12), dp(14), dp(14))
            }
            card.addView(line("SVIP " + level, 17, true, Color.rgb(145, 103, 34)))
            val nameField = adminInput(item.optString("name").takeIf { it != "null" }.orEmpty(), "اسم المستوى")
            val priceValue = if (item.isNull("price_coins")) "" else item.optLong("price_coins").toString()
            val priceField = adminInput(priceValue, "السعر بالعملات", numeric = true)
            val badgeField = adminInput(item.optString("badge_asset_url").takeIf { it != "null" }.orEmpty(), "رابط شارة SVIP")
            val frameField = adminInput(item.optString("frame_asset_url").takeIf { it != "null" }.orEmpty(), "رابط إطار الصورة")
            val bubbleField = adminInput(item.optString("chat_bubble_asset_url").takeIf { it != "null" }.orEmpty(), "رابط فقاعة الدردشة")
            val entryField = adminInput(item.optString("entry_effect_asset_url").takeIf { it != "null" }.orEmpty(), "رابط مؤثر الدخول")
            listOf(nameField, priceField, badgeField, frameField, bubbleField, entryField).forEach { field ->
                card.addView(field, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) })
            }
            val active = CheckBox(this).apply {
                text = "المستوى متاح للشراء"
                isChecked = item.optBoolean("active", true)
                setTextColor(ink)
            }
            card.addView(active, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(5) })
            val save = actionButton("حفظ إعدادات SVIP " + level)
            card.addView(save, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(7) })
            save.setOnClickListener {
                val priceText = priceField.text.toString().trim()
                val price = if (priceText.isBlank()) null else priceText.toLongOrNull()
                if (priceText.isNotBlank() && (price == null || price <= 0L)) {
                    priceField.error = "أدخل سعراً صحيحاً أكبر من صفر أو اتركه فارغاً"
                    return@setOnClickListener
                }
                val payload = JSONObject()
                    .put("name", nameField.text.toString().trim().ifBlank { "SVIP " + level })
                    .put("active", active.isChecked)
                    .put("price_coins", price ?: JSONObject.NULL)
                    .put("badge_asset_url", badgeField.text.toString().trim().ifBlank { null } ?: JSONObject.NULL)
                    .put("frame_asset_url", frameField.text.toString().trim().ifBlank { null } ?: JSONObject.NULL)
                    .put("chat_bubble_asset_url", bubbleField.text.toString().trim().ifBlank { null } ?: JSONObject.NULL)
                    .put("entry_effect_asset_url", entryField.text.toString().trim().ifBlank { null } ?: JSONObject.NULL)
                save.isEnabled = false
                executor.execute {
                    try {
                        val result = AuthApi.put("/api/admin/v2/svip/levels/" + level, payload, token)
                        if (result.status !in 200..299) {
                            throw Exception(result.body.optString("error", "تعذر حفظ إعدادات SVIP"))
                        }
                        runOnUiThread {
                            if (!isFinishing) {
                                Toast.makeText(this, "تم حفظ SVIP " + level, Toast.LENGTH_SHORT).show()
                                loadSvipLevels()
                            }
                        }
                    } catch (error: Exception) {
                        runOnUiThread {
                            if (!isFinishing) {
                                save.isEnabled = true
                                Toast.makeText(this, error.message ?: "فشل الحفظ", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            svipBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun adminInput(value: String, hintText: String, numeric: Boolean = false) =
        EditText(this).apply {
            hint = hintText
            setText(value)
            textSize = 13f
            isSingleLine = true
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(Color.rgb(248, 249, 252))
            setPadding(dp(12), 0, dp(12), 0)
        }

    private fun searchUsers() {
        val query = searchInput.text.toString().trim()
        if (query.length < 2) { searchInput.error = "اكتب حرفين على الأقل"; return }
        val token = tokenOrClose() ?: return
        usersBox.removeAllViews()
        usersBox.addView(line("جارٍ البحث…", 14, false, muted))
        executor.execute {
            try {
                val path = "/api/admin/v2/users?q=" + URLEncoder.encode(query, "UTF-8")
                val response = AuthApi.get(path, token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشل البحث"))
                val users = response.body.optJSONArray("users")
                val rows = ArrayList<JSONObject>()
                if (users != null) for (i in 0 until users.length()) {
                    val user = users.optJSONObject(i) ?: continue
                    rows.add(user)
                }
                runOnUiThread {
                    usersBox.removeAllViews()
                    if (rows.isEmpty()) usersBox.addView(line("لا توجد نتائج", 14, false, muted))
                    rows.forEach { user ->
                        val publicId = user.optString("public_id")
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(12), dp(10), dp(12), dp(10))
                            background = rounded(Color.WHITE)
                        }
                        card.addView(line("${user.optString("username")}  •  $publicId", 15, true))
                        card.addView(line("الحالة: ${user.optString("status")}", 13, false, muted))
                        val activeBan = user.optJSONObject("active_ban")
                        if (activeBan != null) {
                            card.addView(line("الحظر: ${activeBan.optString("ban_type")}  •  ${activeBan.optString("expires_at", "دائم")}", 12, false, Color.rgb(180, 75, 55)))
                            val unban = actionButton("رفع الحظر")
                            card.addView(unban, LinearLayout.LayoutParams(-1, dp(42)))
                            unban.setOnClickListener { askUnban(publicId) }
                        } else {
                            val ban = actionButton("حظر المستخدم")
                            card.addView(ban, LinearLayout.LayoutParams(-1, dp(42)))
                            ban.setOnClickListener { askBan(publicId) }
                        }
                        usersBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { usersBox.removeAllViews(); usersBox.addView(line(e.message ?: "فشل البحث", 14, false, Color.RED)) }
            }
        }
    }

    private fun askBan(publicId: String) {
        val reason = EditText(this).apply { hint = "سبب الحظر (إلزامي)" }
        val duration = EditText(this).apply {
            hint = "مدة الحظر بالساعات (1 إلى 8760)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val type = android.widget.RadioGroup(this).apply {
            orientation = android.widget.RadioGroup.VERTICAL
            val temporary = android.widget.RadioButton(this@AdminActivity).apply { text = "حظر مؤقت"; id = View.generateViewId(); isChecked = true }
            val permanent = android.widget.RadioButton(this@AdminActivity).apply { text = "حظر دائم"; id = View.generateViewId() }
            addView(temporary); addView(permanent)
            tag = Pair(temporary.id, permanent.id)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(type); addView(duration); addView(reason)
        }
        type.setOnCheckedChangeListener { _, checked ->
            duration.visibility = if (checked == (type.tag as Pair<*, *>).first) View.VISIBLE else View.GONE
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("حظر $publicId")
            .setView(content)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد الحظر", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(-1).setOnClickListener {
                        val why = reason.text.toString().trim()
                        val temporary = type.checkedRadioButtonId == (type.tag as Pair<*, *>).first
                        val hours = duration.text.toString().toIntOrNull()
                        if (why.isBlank() || (temporary && (hours == null || hours !in 1..8760))) {
                            Toast.makeText(this, "أدخل السبب والمدة الصحيحة", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val payload = JSONObject().put("ban_type", if (temporary) "temporary" else "permanent").put("reason", why)
                        if (temporary) payload.put("duration_hours", hours)
                        submitBan(publicId, payload, dialog)
                    }
                }
                dialog.show()
            }
    }

    private fun submitBan(publicId: String, payload: JSONObject, dialog: android.app.AlertDialog) {
        val token = tokenOrClose() ?: return
        executor.execute {
            try {
                val response = AuthApi.post("/api/admin/v2/users/$publicId/ban", payload, token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشل الحظر"))
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "تم تنفيذ الحظر وتسجيل العملية", Toast.LENGTH_LONG).show()
                    searchUsers()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "فشل الحظر", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun askUnban(publicId: String) {
        val reason = EditText(this).apply { hint = "سبب رفع الحظر (إلزامي)" }
        android.app.AlertDialog.Builder(this)
            .setTitle("رفع الحظر عن $publicId")
            .setView(reason)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد") { _, _ ->
                val why = reason.text.toString().trim()
                if (why.isBlank()) {
                    Toast.makeText(this, "اكتب سبب رفع الحظر", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val token = tokenOrClose() ?: return@setPositiveButton
                executor.execute {
                    try {
                        val response = AuthApi.post("/api/admin/v2/users/$publicId/unban", JSONObject().put("reason", why), token)
                        if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشل رفع الحظر"))
                        runOnUiThread { Toast.makeText(this, "تم رفع الحظر", Toast.LENGTH_SHORT).show(); searchUsers() }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, e.message ?: "فشل رفع الحظر", Toast.LENGTH_LONG).show() }
                    }
                }
            }.show()
    }

    private fun loadAgencies() {
        val token = tokenOrClose() ?: return
        agenciesBox.removeAllViews()
        agenciesBox.addView(line("جارٍ تحميل الوكالات…", 14, false, muted))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/agencies?status=" + URLEncoder.encode(agencyStatus, "UTF-8"), token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل الوكالات"))
                val array = response.body.optJSONArray("agencies")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    rows.add(item)
                }
                runOnUiThread {
                    agenciesBox.removeAllViews()
                    if (rows.isEmpty()) agenciesBox.addView(line("لا توجد وكالات ضمن هذا التصنيف", 14, false, muted))
                    rows.forEach { agency ->
                        val id = agency.optString("id")
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(13), dp(10), dp(13), dp(10))
                            background = rounded(Color.WHITE)
                        }
                        card.addView(line(agency.optString("agency_name", "وكالة بدون اسم"), 16, true))
                        card.addView(line("صاحب الطلب: ${agency.optString("username")} • ID: ${agency.optString("public_id")}", 13, false, muted))
                        card.addView(line("الاسم القانوني: ${agency.optString("legal_name")} • الدولة: ${agency.optString("country_code")}", 13, false, muted))
                        card.addView(line("الحالة: ${agency.optString("status")} • تاريخ الطلب: ${agency.optString("created_at")}", 12, false, muted))
                        if (agency.optString("review_note").isNotBlank()) card.addView(line("ملاحظة: ${agency.optString("review_note")}", 12, false, muted))
                        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                        fun addAction(label: String, endpoint: String) {
                            val button = actionButton(label)
                            actions.addView(button, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(4) })
                            button.setOnClickListener { askAgencyAction(id, endpoint, label) }
                        }
                        when (agency.optString("status")) {
                            "pending" -> { addAction("قبول", "approve"); addAction("رفض", "reject") }
                            "approved" -> { addAction("حظر", "block"); addAction("إلغاء الوكالة", "revoke") }
                            "blocked" -> { addAction("رفع الحظر", "unblock"); addAction("إلغاء الوكالة", "revoke") }
                        }
                        if (actions.childCount > 0) card.addView(actions)
                        agenciesBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    agenciesBox.removeAllViews()
                    agenciesBox.addView(line(e.message ?: "فشل تحميل الوكالات", 14, false, Color.RED))
                }
            }
        }
    }

    private fun askAgencyAction(id: String, action: String, label: String) {
        val reason = EditText(this).apply { hint = "اكتب سبب $label (إلزامي)" }
        android.app.AlertDialog.Builder(this)
            .setTitle("$label — وكالة #$id")
            .setView(reason)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تأكيد", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(-1).setOnClickListener {
                        val why = reason.text.toString().trim()
                        if (why.isBlank()) {
                            Toast.makeText(this, "السبب إلزامي لتوثيق العملية", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val token = tokenOrClose() ?: return@setOnClickListener
                        executor.execute {
                            try {
                                val response = AuthApi.post("/api/admin/v2/agencies/$id/$action", JSONObject().put("reason", why), token)
                                if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشلت العملية"))
                                runOnUiThread {
                                    dialog.dismiss()
                                    Toast.makeText(this, "تم $label وتسجيل العملية", Toast.LENGTH_LONG).show()
                                    loadAgencies()
                                }
                            } catch (e: Exception) {
                                runOnUiThread { Toast.makeText(this, e.message ?: "فشلت العملية", Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun loadAudit() {
        val token = tokenOrClose() ?: return
        auditBox.removeAllViews()
        auditBox.addView(line("جارٍ تحميل السجل…", 14, false, muted))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/audit?limit=20", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل السجل"))
                val entries = response.body.optJSONArray("entries")
                val rows = ArrayList<String>()
                if (entries != null) for (i in 0 until entries.length()) {
                    val item = entries.optJSONObject(i) ?: continue
                    rows.add("${item.optString("action")}  •  ${item.optString("created_at")}\n${item.optString("reason")}")
                }
                runOnUiThread {
                    auditBox.removeAllViews()
                    if (rows.isEmpty()) auditBox.addView(line("لا توجد عمليات مسجلة بعد", 14, false, muted))
                    rows.forEach { auditBox.addView(line(it, 13).apply { setPadding(dp(8), dp(8), dp(8), dp(8)) }) }
                }
            } catch (e: Exception) {
                runOnUiThread { auditBox.removeAllViews(); auditBox.addView(line(e.message ?: "تعذر تحميل السجل", 14, false, Color.RED)) }
            }
        }
    }

    private fun tokenOrClose(): String? {
        val value = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (value.isNullOrBlank()) {
            Toast.makeText(this, "سجّل الدخول أولاً", Toast.LENGTH_LONG).show()
            finish()
            return null
        }
        return value
    }

    private fun title(value: String, size: Int) = line(value, size, true)
    private fun line(value: String, size: Int, bold: Boolean = false, color: Int = ink) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun actionButton(value: String) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.rgb(87, 64, 180), Color.rgb(27, 155, 139))
        ).apply { cornerRadius = dp(14).toFloat() }
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), Color.rgb(232, 234, 241))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
