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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.net.URLEncoder
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
    private lateinit var withdrawalsBox: LinearLayout
    private lateinit var reportsBox: LinearLayout
    private var agencyStatus = "pending"
    private var withdrawalStatus = "pending"

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

        page.addView(title("بلاغات المستخدمين", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        val reportsRefresh = actionButton("تحديث البلاغات")
        page.addView(reportsRefresh, LinearLayout.LayoutParams(-1, dp(46)))
        reportsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(reportsBox)
        reportsRefresh.setOnClickListener { loadReports() }

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

        page.addView(title("طلبات سحب أرباح الوكالات", 18).apply { setPadding(0, dp(24), 0, dp(10)) })
        val withdrawalFilter = android.widget.Spinner(this)
        val withdrawalLabels = listOf("قيد المراجعة", "مقبولة", "مرفوضة", "مدفوعة", "كل الطلبات")
        val withdrawalKeys = listOf("pending", "approved", "rejected", "paid", "all")
        withdrawalFilter.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, withdrawalLabels)
        page.addView(withdrawalFilter, LinearLayout.LayoutParams(-1, dp(48)))
        withdrawalsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(withdrawalsBox)
        val refreshWithdrawals = actionButton("تحديث طلبات السحب")
        page.addView(refreshWithdrawals, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        refreshWithdrawals.setOnClickListener { loadAgencyWithdrawals() }
        withdrawalFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                withdrawalStatus = withdrawalKeys[position]
                loadAgencyWithdrawals()
            }
        }

        page.addView(title("سجل العمليات الإدارية", 18).apply { setPadding(0, dp(22), 0, dp(10)) })
        val auditButton = actionButton("عرض آخر العمليات")
        page.addView(auditButton, LinearLayout.LayoutParams(-1, dp(48)))
        auditButton.setOnClickListener { loadAudit() }
        auditBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(auditBox)
        loadDashboard()
        loadReports()
    }

    private fun loadReports() {
        if (!::reportsBox.isInitialized) return
        val token = tokenOrClose() ?: return
        reportsBox.removeAllViews()
        reportsBox.addView(line("جارٍ تحميل البلاغات…", 14, false, muted))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/reports?limit=30", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل البلاغات"))
                val array = response.body.optJSONArray("reports")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let(rows::add)
                runOnUiThread {
                    reportsBox.removeAllViews()
                    if (rows.isEmpty()) reportsBox.addView(line("لا توجد بلاغات حالياً", 14, false, muted))
                    rows.forEach { item ->
                        val id = item.optLong("id")
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(13), dp(11), dp(13), dp(11))
                            background = rounded(Color.WHITE)
                        }
                        card.addView(line("بلاغ #$id  •  ${item.optString("status", "new")}", 15, true))
                        card.addView(line("المبلّغ: ${item.optString("reporter_username", "—")}", 13, false, muted))
                        card.addView(line("ID المبلّغ: ${item.optString("reporter_public_id", item.optString("reporter_user_id", "غير متوفر"))}", 12, false, muted))
                        card.addView(line("المُبلّغ عليه: ${item.optString("reported_username", "—")}", 13, false, muted))
                        card.addView(line("ID المُبلّغ عليه: ${item.optString("reported_public_id", item.optString("reported_user_id", "غير متوفر"))}", 12, false, muted))
                        card.addView(line("السبب: ${item.optString("reason", "—")}", 13))
                        val details = item.optString("details")
                        if (details.isNotBlank() && details != "null") card.addView(line("التفاصيل: $details", 13))
                        card.addView(line("التاريخ: ${item.optString("created_at", "—")}", 11, false, muted))
                        val review = actionButton("مراجعة البلاغ")
                        card.addView(review, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(7) })
                        review.setOnClickListener { reviewReport(id, item.optString("status", "new")) }
                        val reportedPublicId = item.optString("reported_public_id", "")
                        val roomIdForWatch = item.optString("room_id", "")
                        if (reportedPublicId.isNotBlank() && reportedPublicId != "null") {
                            val ban = actionButton("حظر المستخدم المُبلّغ عليه")
                            card.addView(ban, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(6) })
                            ban.setOnClickListener { askBan(reportedPublicId) }
                        }
                        if (roomIdForWatch.isNotBlank() && roomIdForWatch != "null" && item.optString("status") !in listOf("rejected", "closed")) {
                            val watch = actionButton("مشاهدة البث بالصوت والصورة")
                            card.addView(watch, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(6) })
                            watch.setOnClickListener { startReportWatch(id) }
                        }
                        reportsBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { reportsBox.removeAllViews(); reportsBox.addView(line(e.message ?: "تعذر تحميل البلاغات", 14, false, Color.RED)) }
            }
        }
    }

    private fun startReportWatch(reportId: Long) {
        val token = tokenOrClose() ?: return
        executor.execute {
            try {
                val response = AuthApi.post("/api/admin/v2/reports/$reportId/watch", JSONObject(), token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر بدء مراقبة البث"))
                val intent = Intent(this, AdminLiveMonitorActivity::class.java)
                    .putExtra("admin_token", token)
                    .putExtra("session_id", response.body.optString("sessionId"))
                    .putExtra("room_id", response.body.optString("roomId"))
                    .putExtra("server_url", response.body.optString("serverUrl"))
                    .putExtra("participant_token", response.body.optString("participantToken"))
                runOnUiThread { startActivity(intent) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر بدء مراقبة البث", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun reviewReport(reportId: Long, currentStatus: String) {
        val labels = listOf("قيد المراجعة", "مقبول", "مرفوض", "مغلق")
        val statuses = listOf("reviewing", "accepted", "rejected", "closed")
        val group = android.widget.RadioGroup(this).apply { orientation = android.widget.RadioGroup.VERTICAL }
        var selected = statuses.indexOf(currentStatus).takeIf { it >= 0 } ?: 0
        labels.forEachIndexed { index, value ->
            group.addView(android.widget.RadioButton(this).apply {
                text = value
                id = View.generateViewId()
                tag = index
                if (index == selected) isChecked = true
            })
        }
        group.setOnCheckedChangeListener { g, checked ->
            selected = (0 until g.childCount).mapNotNull { g.getChildAt(it) as? android.widget.RadioButton }
                .firstOrNull { it.id == checked }?.tag as? Int ?: 0
        }
        val note = EditText(this).apply { hint = "ملاحظة المراجعة (مطلوبة عند القبول أو الرفض أو الإغلاق)" }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(group)
            addView(note)
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("مراجعة البلاغ #$reportId")
            .setView(content)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ القرار", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val memo = note.text.toString().trim()
                if (statuses[selected] != "reviewing" && memo.length < 3) {
                    note.error = "أدخل ملاحظة للمراجعة"
                    return@setOnClickListener
                }
                val token = tokenOrClose() ?: return@setOnClickListener
                val payload = JSONObject().put("status", statuses[selected]).put("note", memo)
                dialog.getButton(-1).isEnabled = false
                executor.execute {
                    try {
                        val result = AuthApi.post("/api/admin/v2/reports/$reportId/review", payload, token)
                        if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر حفظ القرار"))
                        runOnUiThread { dialog.dismiss(); Toast.makeText(this, "تم تحديث البلاغ", Toast.LENGTH_SHORT).show(); loadReports() }
                    } catch (e: Exception) {
                        runOnUiThread { dialog.getButton(-1).isEnabled = true; Toast.makeText(this, e.message ?: "تعذر تحديث البلاغ", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
        dialog.show()
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
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر الاتصال بلوحة الإدارة", Toast.LENGTH_LONG).show(); finish() }
            }
        }
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

    private fun loadAgencyWithdrawals() {
        val token = tokenOrClose() ?: return
        withdrawalsBox.removeAllViews()
        withdrawalsBox.addView(line("جارٍ تحميل طلبات السحب…", 14, false, muted))
        executor.execute {
            try {
                val path = "/api/admin/v2/agency-withdrawals?status=" + URLEncoder.encode(withdrawalStatus, "UTF-8") + "&limit=50&offset=0"
                val response = AuthApi.get(path, token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل طلبات السحب"))
                val array = response.body.optJSONArray("withdrawals") ?: response.body.optJSONArray("items")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) rows.add(array.optJSONObject(i) ?: continue)
                runOnUiThread {
                    withdrawalsBox.removeAllViews()
                    if (rows.isEmpty()) withdrawalsBox.addView(line("لا توجد طلبات سحب ضمن هذا التصنيف", 14, false, muted))
                    rows.forEach { item ->
                        val id = item.optString("id")
                        val status = item.optString("status")
                        val cents = item.optLong("amount_usd_cents", 0L)
                        val amount = String.format(java.util.Locale.US, "%.2f", cents / 100.0)
                        val method = when (item.optString("method").lowercase()) {
                            "zain_cash", "zaincash" -> "زين كاش"
                            "asia_hawala", "asia_hawala_wallet", "asiahawala" -> "آسيا حوالة"
                            "usdt" -> "USDT"
                            else -> item.optString("method", "غير محدد")
                        }
                        val state = when (status) { "pending" -> "قيد المراجعة"; "approved" -> "مقبول — بانتظار الدفع"; "rejected" -> "مرفوض"; "paid" -> "مدفوع"; else -> status }
                        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = rounded(Color.WHITE) }
                        card.addView(line("طلب سحب #$id • $state", 15, true))
                        card.addView(line("الوكالة: " + item.optString("agency_name", "—"), 14, true))
                        card.addView(line("المالك: " + item.optString("username", "—") + " • ID: " + item.optString("public_id", "—"), 12, false, muted))
                        card.addView(line("المبلغ: $" + amount + " USD", 16, true, Color.rgb(32, 126, 105)))
                        card.addView(line("طريقة الاستلام: $method", 13, false, muted))
                        card.addView(line("بيانات الاستلام: " + item.optString("destination", "—"), 13, false, muted))
                        card.addView(line("تاريخ الطلب: " + item.optString("created_at", "—"), 12, false, muted))
                        if (item.optString("admin_note").isNotBlank()) card.addView(line("ملاحظة الإدارة: " + item.optString("admin_note"), 12, false, muted))
                        if (item.optString("payment_reference").isNotBlank()) card.addView(line("مرجع الدفع: " + item.optString("payment_reference"), 12, false, muted))
                        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                        fun addAction(label: String, action: String) {
                            val button = actionButton(label)
                            actions.addView(button, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(5) })
                            button.setOnClickListener { askWithdrawalAction(id, action, label) }
                        }
                        when (status) { "pending" -> { addAction("موافقة", "approve"); addAction("رفض", "reject") }; "approved" -> addAction("تسجيل الدفع", "paid") }
                        if (actions.childCount > 0) card.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
                        withdrawalsBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
                    }
                }
            } catch (e: Exception) { runOnUiThread { withdrawalsBox.removeAllViews(); withdrawalsBox.addView(line(e.message ?: "تعذر تحميل طلبات السحب", 14, false, Color.RED)) } }
        }
    }

    private fun askWithdrawalAction(id: String, action: String, label: String) {
        val reason = EditText(this).apply { hint = if (action == "reject") "سبب الرفض (إلزامي)" else "ملاحظة الإدارة"; minLines = 2 }
        val reference = if (action == "paid") EditText(this).apply { hint = "رقم مرجع التحويل / الإيصال (إلزامي)"; isSingleLine = true } else null
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(reason); if (reference != null) addView(reference) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle("$label — طلب #$id").setView(content).setNegativeButton("إلغاء", null).setPositiveButton("تأكيد", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val why = reason.text.toString().trim()
                val ref = reference?.text?.toString()?.trim().orEmpty()
                if (action == "reject" && why.isBlank()) { reason.error = "سبب الرفض مطلوب"; return@setOnClickListener }
                if (action == "paid" && ref.length !in 3..200) { reference?.error = "أدخل رقم مرجع صحيحاً"; return@setOnClickListener }
                val token = tokenOrClose() ?: return@setOnClickListener
                val payload = JSONObject().put("reason", why)
                if (action == "paid") payload.put("payment_reference", ref)
                dialog.getButton(-1).isEnabled = false
                executor.execute {
                    try {
                        val response = AuthApi.post("/api/admin/v2/agency-withdrawals/$id/$action", payload, token)
                        if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تنفيذ الإجراء"))
                        runOnUiThread { dialog.dismiss(); Toast.makeText(this, "تم $label وتسجيل العملية", Toast.LENGTH_LONG).show(); loadAgencyWithdrawals() }
                    } catch (e: Exception) { runOnUiThread { dialog.getButton(-1).isEnabled = true; Toast.makeText(this, e.message ?: "تعذر تنفيذ الإجراء", Toast.LENGTH_LONG).show() } }
                }
            }
        }
        dialog.show()
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
