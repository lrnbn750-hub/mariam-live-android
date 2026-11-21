package com.mariam.live

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Native administration screen. All authority is checked by Admin V2 on the server. */
class AdminActivity : Activity() {
    private companion object {
        const val PRIMARY = 0
        const val TONAL = 1
        const val DANGER = 2
        const val SUCCESS = 3
    }

    private val executor = Executors.newSingleThreadExecutor()

    // ===== الألوان (واجهة بيضاء) =====
    private val bg = Color.WHITE
    private val surface = Color.rgb(248, 249, 252)
    private val border = Color.rgb(228, 231, 240)
    private val ink = Color.rgb(25, 29, 42)
    private val body = Color.rgb(70, 76, 92)
    private val muted = Color.rgb(112, 121, 140)
    private val primary = Color.rgb(87, 64, 180)
    private val teal = Color.rgb(27, 155, 139)
    private val red = Color.rgb(200, 52, 64)
    private val orange = Color.rgb(217, 130, 20)
    private val green = Color.rgb(30, 150, 95)
    private val blue = Color.rgb(48, 108, 208)

    // ===== التبويبات =====
    private val tabNames = listOf("نظرة عامة", "البلاغات", "المستخدمون", "الوكالات", "طلبات السحب", "السجل")
    private val sections = ArrayList<LinearLayout>()
    private val tabViews = ArrayList<TextView>()
    private val loadedTabs = HashSet<Int>()
    private lateinit var contentScroll: ScrollView

    // ===== حاويات القوائم =====
    private lateinit var statsBox: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var usersBox: LinearLayout
    private lateinit var auditBox: LinearLayout
    private lateinit var agenciesBox: LinearLayout
    private lateinit var withdrawalsBox: LinearLayout
    private lateinit var reportsBox: LinearLayout

    private var agencyStatus = "pending"
    private var withdrawalStatus = "pending"
    private var reportFilter = "active"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setBackgroundColor(bg)

        root.addView(buildHeader())
        root.addView(buildTabs())
        val divider = View(this)
        divider.setBackgroundColor(border)
        root.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))

        contentScroll = ScrollView(this)
        contentScroll.isVerticalScrollBarEnabled = false
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(18), dp(18), dp(18), dp(40))
        listOf(
            buildOverview(), buildReports(), buildUsers(),
            buildAgencies(), buildWithdrawals(), buildAudit()
        ).forEach { section ->
            sections.add(section)
            content.addView(section, lp())
        }
        contentScroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(contentScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        selectTab(0)
        loadDashboard()
    }

    // ============================================================
    //  بناء الواجهة
    // ============================================================

    private fun buildHeader(): View {
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(18), dp(14), dp(18), dp(10))

        val logo = TextView(this)
        logo.text = "B"
        logo.textSize = 19f
        logo.typeface = Typeface.DEFAULT_BOLD
        logo.setTextColor(Color.WHITE)
        logo.gravity = Gravity.CENTER
        logo.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(primary, teal)
        ).apply { cornerRadius = dp(14).toFloat() }
        header.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))

        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(line("مركز الإدارة", 19, true))
        texts.addView(line("BAK LIVE  •  لوحة تحكم آمنة", 12, false, muted))
        header.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })

        val close = btn("إغلاق", TONAL)
        close.setOnClickListener { finish() }
        header.addView(close, LinearLayout.LayoutParams(-2, dp(38)))
        return header
    }

    private fun buildTabs(): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(18), dp(4), dp(18), dp(12))
        tabNames.forEachIndexed { index, name ->
            val tab = TextView(this)
            tab.text = name
            tab.textSize = 13f
            tab.typeface = Typeface.DEFAULT_BOLD
            tab.gravity = Gravity.CENTER
            tab.setPadding(dp(16), dp(9), dp(16), dp(9))
            tab.isClickable = true
            tab.setOnClickListener { selectTab(index) }
            tabViews.add(tab)
            row.addView(tab, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        }
        val scroller = HorizontalScrollView(this)
        scroller.isHorizontalScrollBarEnabled = false
        scroller.addView(row)
        return scroller
    }

    private fun selectTab(index: Int) {
        sections.forEachIndexed { i, s -> s.visibility = if (i == index) View.VISIBLE else View.GONE }
        tabViews.forEachIndexed { i, t -> styleChip(t, i == index) }
        contentScroll.scrollTo(0, 0)
        if (loadedTabs.add(index)) {
            when (index) {
                1 -> loadReports()
                3 -> loadAgencies()
                4 -> loadAgencyWithdrawals()
                5 -> loadAudit()
            }
        }
    }

    private fun buildOverview(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("نظرة عامة", "ملخص سريع لحالة المنصة", null))
        statsBox = LinearLayout(this)
        statsBox.orientation = LinearLayout.VERTICAL
        statsBox.addView(loadingView("جارٍ تحميل الإحصائيات…"))
        s.addView(statsBox, lp(top = 12))
        s.addView(line("اختصارات سريعة", 16, true), lp(top = 22))
        s.addView(shortcut("البلاغات", "مراجعة بلاغات المستخدمين واتخاذ القرار", 1), lp(top = 8))
        s.addView(shortcut("المستخدمون", "البحث عن مستخدم وحظره أو رفع الحظر", 2), lp(top = 8))
        s.addView(shortcut("الوكالات", "قبول الوكالات ورفضها وإدارة حالتها", 3), lp(top = 8))
        s.addView(shortcut("طلبات السحب", "الموافقة على طلبات أرباح الوكالات ودفعها", 4), lp(top = 8))
        s.addView(shortcut("السجل", "آخر العمليات الإدارية المسجلة", 5), lp(top = 8))
        return s
    }

    private fun buildReports(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("بلاغات المستخدمين", "راجع البلاغات واتخذ الإجراء المناسب") { loadReports() })
        s.addView(
            chipRow(listOf("active" to "الحالية", "completed" to "المكتملة", "rejected" to "المرفوضة"), reportFilter) {
                reportFilter = it
                loadReports()
            }, lp(top = 12)
        )
        reportsBox = LinearLayout(this)
        reportsBox.orientation = LinearLayout.VERTICAL
        s.addView(reportsBox, lp(top = 4))
        return s
    }

    private fun buildUsers(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("إدارة المستخدمين", "ابحث عن مستخدم لحظره أو رفع الحظر عنه", null))
        searchInput = input("ابحث بالمعرّف أو اسم المستخدم")
        searchInput.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { searchUsers(); true } else false
        }
        s.addView(searchInput, lp(-1, dp(52), top = 14))
        val searchButton = btn("بحث عن مستخدم", PRIMARY)
        searchButton.setOnClickListener { searchUsers() }
        s.addView(searchButton, lp(-1, dp(48), top = 10))
        usersBox = LinearLayout(this)
        usersBox.orientation = LinearLayout.VERTICAL
        s.addView(usersBox, lp(top = 6))
        return s
    }

    private fun buildAgencies(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("إدارة الوكالات", "طلبات الوكالات وحالاتها") { loadAgencies() })
        s.addView(
            chipRow(
                listOf("pending" to "قيد المراجعة", "approved" to "مقبولة", "blocked" to "محظورة", "all" to "الكل"),
                agencyStatus
            ) {
                agencyStatus = it
                loadAgencies()
            }, lp(top = 12)
        )
        agenciesBox = LinearLayout(this)
        agenciesBox.orientation = LinearLayout.VERTICAL
        s.addView(agenciesBox, lp(top = 4))
        return s
    }

    private fun buildWithdrawals(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("طلبات سحب أرباح الوكالات", "راجع الطلبات وسجّل الدفع") { loadAgencyWithdrawals() })
        s.addView(
            chipRow(
                listOf(
                    "pending" to "قيد المراجعة", "approved" to "مقبولة", "rejected" to "مرفوضة",
                    "paid" to "مدفوعة", "all" to "كل الطلبات"
                ),
                withdrawalStatus
            ) {
                withdrawalStatus = it
                loadAgencyWithdrawals()
            }, lp(top = 12)
        )
        withdrawalsBox = LinearLayout(this)
        withdrawalsBox.orientation = LinearLayout.VERTICAL
        s.addView(withdrawalsBox, lp(top = 4))
        return s
    }

    private fun buildAudit(): LinearLayout {
        val s = LinearLayout(this)
        s.orientation = LinearLayout.VERTICAL
        s.addView(sectionHeader("سجل العمليات الإدارية", "آخر 20 عملية مسجلة") { loadAudit() })
        auditBox = LinearLayout(this)
        auditBox.orientation = LinearLayout.VERTICAL
        s.addView(auditBox, lp(top = 10))
        return s
    }

    // ============================================================
    //  تحميل البيانات
    // ============================================================

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
                    val name = admin?.optString("username", "مدير") ?: "مدير"
                    val role = admin?.optString("role_name", "إدارة") ?: "إدارة"

                    val welcome = newCard(Color.rgb(246, 243, 254))
                    welcome.background = rounded(Color.rgb(246, 243, 254), 18, Color.rgb(228, 222, 250))
                    welcome.addView(line("مرحباً $name", 20, true))
                    val roleRow = LinearLayout(this)
                    roleRow.orientation = LinearLayout.HORIZONTAL
                    roleRow.gravity = Gravity.CENTER_VERTICAL
                    roleRow.addView(line("الصلاحية", 12, false, muted))
                    roleRow.addView(badge(role, primary), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
                    welcome.addView(roleRow, lp(top = 4))
                    statsBox.addView(welcome)

                    val tiles = LinearLayout(this)
                    tiles.orientation = LinearLayout.HORIZONTAL
                    val tileParams = { first: Boolean ->
                        LinearLayout.LayoutParams(0, -2, 1f).apply { if (!first) marginStart = dp(8) }
                    }
                    tiles.addView(statTile(data.optString("total_users", "—"), "إجمالي المستخدمين", primary), tileParams(true))
                    tiles.addView(statTile(data.optString("active_users", "—"), "المستخدمون النشطون", green), tileParams(false))
                    tiles.addView(statTile(data.optString("active_admins", "—"), "المديرون النشطون", blue), tileParams(false))
                    statsBox.addView(tiles, lp(top = 12))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, e.message ?: "تعذر الاتصال بلوحة الإدارة", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun loadReports() {
        if (!::reportsBox.isInitialized) return
        val token = tokenOrClose() ?: return
        val filterNow = reportFilter
        reportsBox.removeAllViews()
        reportsBox.addView(loadingView("جارٍ تحميل البلاغات…"))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/reports?limit=100", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل البلاغات"))
                val array = response.body.optJSONArray("reports")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let(rows::add)
                val filtered = rows.filter { item ->
                    when (filterNow) {
                        "completed" -> item.optString("status") in listOf("accepted", "closed")
                        "rejected" -> item.optString("status") == "rejected"
                        else -> item.optString("status") !in listOf("accepted", "closed", "rejected")
                    }
                }
                runOnUiThread {
                    if (filterNow != reportFilter) return@runOnUiThread
                    reportsBox.removeAllViews()
                    if (filtered.isEmpty()) {
                        reportsBox.addView(
                            emptyView(
                                when (filterNow) {
                                    "completed" -> "لا توجد بلاغات مكتملة حالياً"
                                    "rejected" -> "لا توجد بلاغات مرفوضة حالياً"
                                    else -> "لا توجد بلاغات قيد المعالجة حالياً"
                                }
                            )
                        )
                    }
                    filtered.forEach { item ->
                        val id = item.optLong("id")
                        val status = item.optString("status", "new")
                        val (statusText, statusColor) = reportBadge(status)
                        val c = newCard()
                        c.addView(headRow("بلاغ #$id", statusText, statusColor))
                        c.addView(spacer(8))
                        val reporterId = item.optString("reporter_public_id", item.optString("reporter_user_id", "غير متوفر"))
                        val reportedId = item.optString("reported_public_id", item.optString("reported_user_id", "غير متوفر"))
                        c.addView(infoRow("المبلّغ", "${item.optString("reporter_username", "—")}  •  ID: $reporterId"))
                        c.addView(infoRow("المُبلّغ عليه", "${item.optString("reported_username", "—")}  •  ID: $reportedId"))
                        c.addView(infoRow("السبب", item.optString("reason", "—"), ink, true))
                        val details = item.optString("details")
                        if (details.isNotBlank() && details != "null") c.addView(infoRow("التفاصيل", details))
                        c.addView(infoRow("التاريخ", fmtDate(item.optString("created_at", "")), muted))

                        val reportedPublicId = item.optString("reported_public_id", "")
                        val roomIdForWatch = item.optString("room_id", "")
                        if (filterNow == "active") {
                            val review = btn("مراجعة البلاغ", TONAL)
                            review.setOnClickListener { reviewReport(id, status) }
                            c.addView(review, lp(-1, dp(44), top = 12))
                            if (reportedPublicId.isNotBlank() && reportedPublicId != "null") {
                                val ban = btn("حظر المستخدم المُبلّغ عليه", DANGER)
                                ban.setOnClickListener { askBan(reportedPublicId, id) }
                                c.addView(ban, lp(-1, dp(44), top = 8))
                            }
                            if (roomIdForWatch.isNotBlank() && roomIdForWatch != "null") {
                                val watch = btn("مشاهدة البث بالصوت والصورة", PRIMARY)
                                watch.setOnClickListener { startReportWatch(id) }
                                c.addView(watch, lp(-1, dp(46), top = 8))
                            }
                        } else {
                            val note = item.optString("review_note", "")
                            if (note.isNotBlank() && note != "null") c.addView(infoRow("قرار الإدارة", note, body))
                        }
                        reportsBox.addView(c, lp(top = 10))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    reportsBox.removeAllViews()
                    reportsBox.addView(errorView(e.message ?: "تعذر تحميل البلاغات"))
                }
            }
        }
    }

    private fun searchUsers() {
        val query = searchInput.text.toString().trim()
        if (query.length < 2) { searchInput.error = "اكتب حرفين على الأقل"; return }
        val token = tokenOrClose() ?: return
        usersBox.removeAllViews()
        usersBox.addView(loadingView("جارٍ البحث…"))
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
                    if (rows.isEmpty()) usersBox.addView(emptyView("لا توجد نتائج مطابقة"))
                    rows.forEach { user ->
                        val publicId = user.optString("public_id")
                        val c = newCard()
                        val (statusText, statusColor) = userBadge(user.optString("status"))
                        c.addView(headRow(user.optString("username"), statusText, statusColor))
                        c.addView(spacer(6))
                        c.addView(infoRow("المعرّف", publicId))
                        val activeBan = user.optJSONObject("active_ban")
                        if (activeBan != null) {
                            val banType = if (activeBan.optString("ban_type") == "permanent") "حظر دائم" else "حظر مؤقت"
                            val expiresRaw = activeBan.optString("expires_at", "")
                            val until = if (expiresRaw.isBlank() || expiresRaw == "null") "دائم" else fmtDate(expiresRaw)
                            val reasonText = activeBan.optString("reason", "")
                            val note = line(
                                "$banType  •  ينتهي: $until" +
                                    if (reasonText.isNotBlank() && reasonText != "null") "\nالسبب: $reasonText" else "",
                                12, true, red
                            )
                            note.background = rounded(tint(red), 12)
                            note.setPadding(dp(12), dp(10), dp(12), dp(10))
                            c.addView(note, lp(top = 8))
                            val unban = btn("رفع الحظر", SUCCESS)
                            unban.setOnClickListener { askUnban(publicId) }
                            c.addView(unban, lp(-1, dp(44), top = 10))
                        } else {
                            val ban = btn("حظر المستخدم", DANGER)
                            ban.setOnClickListener { askBan(publicId) }
                            c.addView(ban, lp(-1, dp(44), top = 10))
                        }
                        usersBox.addView(c, lp(top = 10))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    usersBox.removeAllViews()
                    usersBox.addView(errorView(e.message ?: "فشل البحث"))
                }
            }
        }
    }

    private fun loadAgencies() {
        val token = tokenOrClose() ?: return
        val statusNow = agencyStatus
        agenciesBox.removeAllViews()
        agenciesBox.addView(loadingView("جارٍ تحميل الوكالات…"))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/agencies?status=" + URLEncoder.encode(statusNow, "UTF-8"), token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل الوكالات"))
                val array = response.body.optJSONArray("agencies")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    rows.add(item)
                }
                runOnUiThread {
                    if (statusNow != agencyStatus) return@runOnUiThread
                    agenciesBox.removeAllViews()
                    if (rows.isEmpty()) agenciesBox.addView(emptyView("لا توجد وكالات ضمن هذا التصنيف"))
                    rows.forEach { agency ->
                        val id = agency.optString("id")
                        val status = agency.optString("status")
                        val (statusText, statusColor) = agencyBadge(status)
                        val c = newCard()
                        c.addView(headRow(agency.optString("agency_name", "وكالة بدون اسم"), statusText, statusColor))
                        c.addView(spacer(8))
                        c.addView(infoRow("صاحب الطلب", "${agency.optString("username")}  •  ID: ${agency.optString("public_id")}"))
                        c.addView(infoRow("الاسم القانوني", agency.optString("legal_name")))
                        c.addView(infoRow("الدولة", agency.optString("country_code")))
                        c.addView(infoRow("تاريخ الطلب", fmtDate(agency.optString("created_at")), muted))
                        if (agency.optString("review_note").isNotBlank()) {
                            c.addView(infoRow("ملاحظة", agency.optString("review_note"), body))
                        }
                        val actions = actionRow()
                        when (status) {
                            "pending" -> {
                                actions.addAction("قبول", SUCCESS) { askAgencyAction(id, "approve", "قبول") }
                                actions.addAction("رفض", DANGER) { askAgencyAction(id, "reject", "رفض") }
                            }
                            "approved" -> {
                                actions.addAction("حظر", DANGER) { askAgencyAction(id, "block", "حظر") }
                                actions.addAction("إلغاء الوكالة", TONAL) { askAgencyAction(id, "revoke", "إلغاء الوكالة") }
                            }
                            "blocked" -> {
                                actions.addAction("رفع الحظر", SUCCESS) { askAgencyAction(id, "unblock", "رفع الحظر") }
                                actions.addAction("إلغاء الوكالة", TONAL) { askAgencyAction(id, "revoke", "إلغاء الوكالة") }
                            }
                        }
                        if (actions.childCount > 0) c.addView(actions, lp(top = 12))
                        agenciesBox.addView(c, lp(top = 10))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    agenciesBox.removeAllViews()
                    agenciesBox.addView(errorView(e.message ?: "فشل تحميل الوكالات"))
                }
            }
        }
    }

    private fun loadAgencyWithdrawals() {
        val token = tokenOrClose() ?: return
        val statusNow = withdrawalStatus
        withdrawalsBox.removeAllViews()
        withdrawalsBox.addView(loadingView("جارٍ تحميل طلبات السحب…"))
        executor.execute {
            try {
                val path = "/api/admin/v2/agency-withdrawals?status=" + URLEncoder.encode(statusNow, "UTF-8") + "&limit=50&offset=0"
                val response = AuthApi.get(path, token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل طلبات السحب"))
                val array = response.body.optJSONArray("withdrawals") ?: response.body.optJSONArray("items")
                val rows = ArrayList<JSONObject>()
                if (array != null) for (i in 0 until array.length()) rows.add(array.optJSONObject(i) ?: continue)
                runOnUiThread {
                    if (statusNow != withdrawalStatus) return@runOnUiThread
                    withdrawalsBox.removeAllViews()
                    if (rows.isEmpty()) withdrawalsBox.addView(emptyView("لا توجد طلبات سحب ضمن هذا التصنيف"))
                    rows.forEach { item ->
                        val id = item.optString("id")
                        val status = item.optString("status")
                        val cents = item.optLong("amount_usd_cents", 0L)
                        val amount = String.format(Locale.US, "%.2f", cents / 100.0)
                        val method = when (item.optString("method").lowercase()) {
                            "zain_cash", "zaincash" -> "زين كاش"
                            "asia_hawala", "asia_hawala_wallet", "asiahawala" -> "آسيا حوالة"
                            "usdt" -> "USDT"
                            else -> item.optString("method", "غير محدد")
                        }
                        val (stateText, stateColor) = when (status) {
                            "pending" -> "قيد المراجعة" to orange
                            "approved" -> "بانتظار الدفع" to blue
                            "rejected" -> "مرفوض" to red
                            "paid" -> "مدفوع" to green
                            else -> status to muted
                        }
                        val c = newCard()
                        c.addView(headRow("طلب سحب #$id", stateText, stateColor))
                        val amountView = line("\$$amount USD", 22, true, green)
                        c.addView(amountView, lp(top = 4))
                        c.addView(spacer(4))
                        c.addView(infoRow("الوكالة", item.optString("agency_name", "—"), ink, true))
                        c.addView(infoRow("المالك", "${item.optString("username", "—")}  •  ID: ${item.optString("public_id", "—")}"))
                        c.addView(infoRow("طريقة الاستلام", method))
                        c.addView(infoRow("بيانات الاستلام", item.optString("destination", "—")))
                        c.addView(infoRow("تاريخ الطلب", fmtDate(item.optString("created_at", "")), muted))
                        if (item.optString("admin_note").isNotBlank()) c.addView(infoRow("ملاحظة الإدارة", item.optString("admin_note"), body))
                        if (item.optString("payment_reference").isNotBlank()) c.addView(infoRow("مرجع الدفع", item.optString("payment_reference"), body))
                        val actions = actionRow()
                        when (status) {
                            "pending" -> {
                                actions.addAction("موافقة", SUCCESS) { askWithdrawalAction(id, "approve", "موافقة") }
                                actions.addAction("رفض", DANGER) { askWithdrawalAction(id, "reject", "رفض") }
                            }
                            "approved" -> actions.addAction("تسجيل الدفع", PRIMARY) { askWithdrawalAction(id, "paid", "تسجيل الدفع") }
                        }
                        if (actions.childCount > 0) c.addView(actions, lp(top = 12))
                        withdrawalsBox.addView(c, lp(top = 10))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    withdrawalsBox.removeAllViews()
                    withdrawalsBox.addView(errorView(e.message ?: "تعذر تحميل طلبات السحب"))
                }
            }
        }
    }

    private fun loadAudit() {
        val token = tokenOrClose() ?: return
        auditBox.removeAllViews()
        auditBox.addView(loadingView("جارٍ تحميل السجل…"))
        executor.execute {
            try {
                val response = AuthApi.get("/api/admin/v2/audit?limit=20", token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تحميل السجل"))
                val entries = response.body.optJSONArray("entries")
                val rows = ArrayList<Triple<String, String, String>>()
                if (entries != null) for (i in 0 until entries.length()) {
                    val item = entries.optJSONObject(i) ?: continue
                    rows.add(Triple(item.optString("action"), item.optString("created_at"), item.optString("reason")))
                }
                runOnUiThread {
                    auditBox.removeAllViews()
                    if (rows.isEmpty()) auditBox.addView(emptyView("لا توجد عمليات مسجلة بعد"))
                    rows.forEach { (action, date, reason) ->
                        val c = newCard()
                        c.addView(line(action, 14, true))
                        if (reason.isNotBlank() && reason != "null") c.addView(line(reason, 13, false, body))
                        c.addView(line(fmtDate(date), 11, false, muted))
                        auditBox.addView(c, lp(top = 8))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    auditBox.removeAllViews()
                    auditBox.addView(errorView(e.message ?: "تعذر تحميل السجل"))
                }
            }
        }
    }

    // ============================================================
    //  العمليات (نوافذ التأكيد)
    // ============================================================

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
        val group = RadioGroup(this)
        group.orientation = RadioGroup.VERTICAL
        val ids = ArrayList<Int>()
        var selected = statuses.indexOf(currentStatus).takeIf { it >= 0 } ?: 0
        labels.forEachIndexed { index, value ->
            val radio = RadioButton(this)
            radio.text = value
            radio.id = View.generateViewId()
            radio.setTextColor(ink)
            ids.add(radio.id)
            group.addView(radio)
            if (index == selected) radio.isChecked = true
        }
        group.setOnCheckedChangeListener { _, checked ->
            selected = ids.indexOf(checked).takeIf { it >= 0 } ?: 0
        }
        val note = input("ملاحظة المراجعة (مطلوبة عند القبول أو الرفض أو الإغلاق)", true)
        val content = formBox()
        content.addView(group)
        content.addView(note, lp(top = 8))
        showForm("مراجعة البلاغ #$reportId", null, content, "حفظ القرار") { dialog ->
            val memo = note.text.toString().trim()
            if (statuses[selected] != "reviewing" && memo.length < 3) {
                note.error = "أدخل ملاحظة للمراجعة"
                return@showForm
            }
            val token = tokenOrClose() ?: return@showForm
            val payload = JSONObject().put("status", statuses[selected]).put("note", memo)
            lock(dialog, true)
            executor.execute {
                try {
                    val result = AuthApi.post("/api/admin/v2/reports/$reportId/review", payload, token)
                    if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر حفظ القرار"))
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this, "تم تحديث البلاغ", Toast.LENGTH_SHORT).show()
                        loadReports()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        lock(dialog, false)
                        Toast.makeText(this, e.message ?: "تعذر تحديث البلاغ", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun askBan(publicId: String, reportId: Long? = null) {
        val reasons = listOf(
            "الإساءة إلى كبار الشخصيات (VIP)",
            "السب والشتم والإساءة اللفظية",
            "التهديد أو التحرش والمضايقة",
            "التخريب والإزعاج داخل الغرف",
            "نشر محتوى غير لائق",
            "مخالفة الضوابط الدينية",
            "مخالفة شروط وأحكام التطبيق"
        )
        val durations = listOf(
            "ساعة واحدة" to 1,
            "6 ساعات" to 6,
            "24 ساعة" to 24,
            "3 أيام" to 72,
            "7 أيام" to 168,
            "30 يوماً" to 720
        )
        val (reasonBox, reasonPicker) = spinnerBox(reasons)
        val (durationBox, durationPicker) = spinnerBox(durations.map { it.first })

        val temporary = RadioButton(this)
        temporary.text = "حظر مؤقت"
        temporary.id = View.generateViewId()
        temporary.isChecked = true
        temporary.setTextColor(ink)
        val permanent = RadioButton(this)
        permanent.text = "حظر دائم"
        permanent.id = View.generateViewId()
        permanent.setTextColor(ink)
        val type = RadioGroup(this)
        type.orientation = RadioGroup.VERTICAL
        type.addView(temporary)
        type.addView(permanent)

        val durationLabel = line("مدة الحظر المؤقت", 13, true)
        val content = formBox()
        content.addView(line("سبب الحظر", 13, true))
        content.addView(reasonBox, lp(top = 4))
        content.addView(line("نوع الحظر", 13, true), lp(top = 12))
        content.addView(type)
        content.addView(durationLabel, lp(top = 8))
        content.addView(durationBox, lp(top = 4))
        type.setOnCheckedChangeListener { _, checked ->
            val visibility = if (checked == temporary.id) View.VISIBLE else View.GONE
            durationLabel.visibility = visibility
            durationBox.visibility = visibility
        }
        showForm("تأكيد حظر المستخدم", "المستخدم: $publicId", content, "تأكيد الحظر") { dialog ->
            val isTemporary = type.checkedRadioButtonId == temporary.id
            val payload = JSONObject()
                .put("ban_type", if (isTemporary) "temporary" else "permanent")
                .put("reason", reasons[reasonPicker.selectedItemPosition])
            if (isTemporary) {
                payload.put("duration_hours", durations[durationPicker.selectedItemPosition].second)
            }
            submitBan(publicId, payload, dialog, reportId)
        }
    }

    private fun submitBan(publicId: String, payload: JSONObject, dialog: android.app.AlertDialog, reportId: Long? = null) {
        val token = tokenOrClose() ?: return
        lock(dialog, true)
        executor.execute {
            try {
                val response = AuthApi.post("/api/admin/v2/users/$publicId/ban", payload, token)
                if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشل الحظر"))
                if (reportId != null) {
                    val reviewPayload = JSONObject()
                        .put("status", "accepted")
                        .put("note", "تم اتخاذ إجراء حظر بحق المستخدم المُبلّغ عليه.")
                    val reviewResponse = AuthApi.post("/api/admin/v2/reports/$reportId/review", reviewPayload, token)
                    if (reviewResponse.status !in 200..299) throw Exception("تم الحظر لكن تعذر إكمال البلاغ؛ راجعه من قائمة البلاغات")
                }
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        if (reportId != null) "تم الحظر ونقل البلاغ إلى البلاغات المكتملة" else "تم تنفيذ الحظر وتسجيل العملية",
                        Toast.LENGTH_LONG
                    ).show()
                    if (searchInput.text.toString().trim().length >= 2) searchUsers()
                    loadReports()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    lock(dialog, false)
                    Toast.makeText(this, e.message ?: "فشل الحظر", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun askUnban(publicId: String) {
        val reason = input("سبب رفع الحظر (إلزامي)", true)
        val content = formBox()
        content.addView(reason)
        showForm("رفع الحظر عن $publicId", null, content, "تأكيد") { dialog ->
            val why = reason.text.toString().trim()
            if (why.isBlank()) { reason.error = "اكتب سبب رفع الحظر"; return@showForm }
            val token = tokenOrClose() ?: return@showForm
            lock(dialog, true)
            executor.execute {
                try {
                    val response = AuthApi.post("/api/admin/v2/users/$publicId/unban", JSONObject().put("reason", why), token)
                    if (response.status !in 200..299) throw Exception(response.body.optString("error", "فشل رفع الحظر"))
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this, "تم رفع الحظر", Toast.LENGTH_SHORT).show()
                        searchUsers()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        lock(dialog, false)
                        Toast.makeText(this, e.message ?: "فشل رفع الحظر", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun askAgencyAction(id: String, action: String, label: String) {
        val reason = input("اكتب سبب $label (إلزامي)", true)
        val content = formBox()
        content.addView(reason)
        showForm("$label — وكالة #$id", null, content, "تأكيد") { dialog ->
            val why = reason.text.toString().trim()
            if (why.isBlank()) {
                reason.error = "السبب إلزامي لتوثيق العملية"
                return@showForm
            }
            val token = tokenOrClose() ?: return@showForm
            lock(dialog, true)
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
                    runOnUiThread {
                        lock(dialog, false)
                        Toast.makeText(this, e.message ?: "فشلت العملية", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun askWithdrawalAction(id: String, action: String, label: String) {
        val reason = input(if (action == "reject") "سبب الرفض (إلزامي)" else "ملاحظة الإدارة", true)
        val reference = if (action == "paid") input("رقم مرجع التحويل / الإيصال (إلزامي)") else null
        val content = formBox()
        content.addView(reason)
        if (reference != null) content.addView(reference, lp(top = 10))
        showForm("$label — طلب #$id", null, content, "تأكيد") { dialog ->
            val why = reason.text.toString().trim()
            val ref = reference?.text?.toString()?.trim().orEmpty()
            if (action == "reject" && why.isBlank()) { reason.error = "سبب الرفض مطلوب"; return@showForm }
            if (action == "paid" && ref.length !in 3..200) { reference?.error = "أدخل رقم مرجع صحيحاً"; return@showForm }
            val token = tokenOrClose() ?: return@showForm
            val payload = JSONObject().put("reason", why)
            if (action == "paid") payload.put("payment_reference", ref)
            lock(dialog, true)
            executor.execute {
                try {
                    val response = AuthApi.post("/api/admin/v2/agency-withdrawals/$id/$action", payload, token)
                    if (response.status !in 200..299) throw Exception(response.body.optString("error", "تعذر تنفيذ الإجراء"))
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this, "تم $label وتسجيل العملية", Toast.LENGTH_LONG).show()
                        loadAgencyWithdrawals()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        lock(dialog, false)
                        Toast.makeText(this, e.message ?: "تعذر تنفيذ الإجراء", Toast.LENGTH_LONG).show()
                    }
                }
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

    // ============================================================
    //  مكوّنات الواجهة المشتركة
    // ============================================================

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun lp(w: Int = -1, h: Int = -2, top: Int = 0, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight).apply { topMargin = dp(top) }

    private fun tint(color: Int) = Color.argb(28, Color.red(color), Color.green(color), Color.blue(color))

    private fun rounded(color: Int, radius: Int = 16, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun line(value: String, size: Int, bold: Boolean = false, color: Int = ink) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        setPadding(0, dp(3), 0, dp(3))
        setLineSpacing(0f, 1.1f)
    }

    private fun spacer(heightDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp(heightDp))
    }

    private fun newCard(fill: Int = Color.WHITE) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(fill, 18, border)
        elevation = dp(1).toFloat()
    }

    private fun badge(value: String, color: Int) = TextView(this).apply {
        text = value
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = rounded(tint(color), 20)
    }

    private fun headRow(name: String, badgeText: String, badgeColor: Int): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(line(name, 16, true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(badge(badgeText, badgeColor), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        return row
    }

    private fun infoRow(label: String, value: String, valueColor: Int = body, bold: Boolean = false): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(line(label, 12, false, muted), LinearLayout.LayoutParams(dp(92), -2))
        row.addView(line(value, 13, bold, valueColor), LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    private fun statTile(value: String, label: String, color: Int): LinearLayout {
        val tile = LinearLayout(this)
        tile.orientation = LinearLayout.VERTICAL
        tile.gravity = Gravity.CENTER
        tile.setPadding(dp(6), dp(16), dp(6), dp(14))
        tile.background = rounded(surface, 16, border)
        val number = TextView(this)
        number.text = value
        number.textSize = 24f
        number.typeface = Typeface.DEFAULT_BOLD
        number.setTextColor(color)
        number.gravity = Gravity.CENTER
        val caption = TextView(this)
        caption.text = label
        caption.textSize = 11f
        caption.setTextColor(muted)
        caption.gravity = Gravity.CENTER
        tile.addView(number)
        tile.addView(caption)
        return tile
    }

    private fun shortcut(name: String, description: String, tabIndex: Int): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(13), dp(16), dp(13))
        row.background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(30, 87, 64, 180)),
            rounded(Color.WHITE, 16, border), null
        )
        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(line(name, 15, true))
        texts.addView(line(description, 12, false, muted))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val chevron = TextView(this)
        chevron.text = "‹"
        chevron.textSize = 26f
        chevron.setTextColor(muted)
        row.addView(chevron)
        row.isClickable = true
        row.setOnClickListener { selectTab(tabIndex) }
        return row
    }

    private fun sectionHeader(name: String, hint: String, onRefresh: (() -> Unit)?): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(line(name, 19, true))
        texts.addView(line(hint, 12, false, muted))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        if (onRefresh != null) {
            val refresh = btn("تحديث", TONAL)
            refresh.setOnClickListener { onRefresh() }
            row.addView(refresh, LinearLayout.LayoutParams(-2, dp(38)).apply { marginStart = dp(10) })
        }
        return row
    }

    private fun styleChip(view: TextView, selected: Boolean) {
        view.setTextColor(if (selected) Color.WHITE else ink)
        view.background = if (selected) rounded(primary, 22) else rounded(surface, 22, border)
    }

    private fun chipRow(options: List<Pair<String, String>>, initial: String, onSelect: (String) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val chips = ArrayList<Pair<String, TextView>>()
        options.forEach { (key, name) ->
            val chip = TextView(this)
            chip.text = name
            chip.textSize = 13f
            chip.typeface = Typeface.DEFAULT_BOLD
            chip.gravity = Gravity.CENTER
            chip.setPadding(dp(16), dp(8), dp(16), dp(8))
            chip.isClickable = true
            chips.add(key to chip)
            row.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            chip.setOnClickListener {
                chips.forEach { (k, v) -> styleChip(v, k == key) }
                onSelect(key)
            }
        }
        chips.forEach { (k, v) -> styleChip(v, k == initial) }
        val scroller = HorizontalScrollView(this)
        scroller.isHorizontalScrollBarEnabled = false
        scroller.addView(row)
        return scroller
    }

    private fun btn(value: String, kind: Int = PRIMARY): Button {
        val button = Button(this)
        button.text = value
        button.isAllCaps = false
        button.textSize = 14f
        button.typeface = Typeface.DEFAULT_BOLD
        button.stateListAnimator = null
        button.minHeight = 0
        button.minimumHeight = 0
        button.minWidth = 0
        button.minimumWidth = 0
        button.setPadding(dp(14), 0, dp(14), 0)
        val shape: GradientDrawable
        when (kind) {
            TONAL -> { button.setTextColor(primary); shape = rounded(Color.rgb(240, 237, 252), 12) }
            DANGER -> { button.setTextColor(red); shape = rounded(Color.rgb(253, 236, 238), 12) }
            SUCCESS -> { button.setTextColor(green); shape = rounded(Color.rgb(228, 246, 238), 12) }
            else -> {
                button.setTextColor(Color.WHITE)
                shape = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(primary, teal))
                shape.cornerRadius = dp(12).toFloat()
            }
        }
        button.background = RippleDrawable(ColorStateList.valueOf(Color.argb(50, 0, 0, 0)), shape, null)
        return button
    }

    private fun actionRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        return row
    }

    private fun LinearLayout.addAction(label: String, kind: Int, onClick: () -> Unit) {
        val button = btn(label, kind)
        button.setOnClickListener { onClick() }
        val params = LinearLayout.LayoutParams(0, dp(44), 1f)
        if (childCount > 0) params.marginStart = dp(8)
        addView(button, params)
    }

    private fun input(hintText: String, multiline: Boolean = false): EditText {
        val field = EditText(this)
        field.hint = hintText
        field.textSize = 14f
        field.setTextColor(ink)
        field.setHintTextColor(muted)
        field.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        field.background = rounded(surface, 14, border)
        field.setPadding(dp(14), dp(12), dp(14), dp(12))
        if (multiline) {
            field.minLines = 3
            field.gravity = Gravity.START or Gravity.TOP
        } else {
            field.isSingleLine = true
            field.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        return field
    }

    private fun spinnerBox(items: List<String>): Pair<FrameLayout, Spinner> {
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.background = ColorDrawable(Color.TRANSPARENT)
        val arrow = TextView(this)
        arrow.text = "▾"
        arrow.textSize = 16f
        arrow.setTextColor(muted)
        val box = FrameLayout(this)
        box.background = rounded(surface, 14, border)
        box.addView(spinner, FrameLayout.LayoutParams(-1, dp(50)))
        box.addView(
            arrow,
            FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(14) }
        )
        return box to spinner
    }

    private fun formBox(): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.layoutDirection = View.LAYOUT_DIRECTION_RTL
        box.setPadding(dp(20), dp(8), dp(20), 0)
        return box
    }

    private fun showForm(
        name: String,
        message: String?,
        content: View,
        positive: String,
        onConfirm: (android.app.AlertDialog) -> Unit
    ) {
        val builder = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        builder.setTitle(name)
        if (message != null) builder.setMessage(message)
        builder.setView(content)
        builder.setNegativeButton("إلغاء", null)
        builder.setPositiveButton(positive, null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { onConfirm(dialog) }
        }
        dialog.show()
    }

    private fun lock(dialog: android.app.AlertDialog, locked: Boolean) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = !locked
    }

    private fun loadingView(value: String) = line(value, 14, false, muted).apply {
        gravity = Gravity.CENTER
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        setPadding(0, dp(28), 0, dp(28))
    }

    private fun emptyView(value: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(dp(16), dp(30), dp(16), dp(30))
        box.background = rounded(surface, 18, border)
        val text = line(value, 14, false, muted)
        text.gravity = Gravity.CENTER
        text.textAlignment = View.TEXT_ALIGNMENT_CENTER
        box.addView(text)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(10)
        box.layoutParams = params
        return box
    }

    private fun errorView(value: String) = line(value, 13, true, red).apply {
        background = rounded(tint(red), 14)
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    // ===== تنسيقات الحالات والتواريخ =====

    private fun reportBadge(status: String): Pair<String, Int> = when (status) {
        "new" -> "جديد" to blue
        "reviewing" -> "قيد المراجعة" to orange
        "accepted", "closed" -> "مكتمل" to green
        "rejected" -> "مرفوض" to red
        else -> status to muted
    }

    private fun agencyBadge(status: String): Pair<String, Int> = when (status) {
        "pending" -> "قيد المراجعة" to orange
        "approved" -> "مقبولة" to green
        "blocked" -> "محظورة" to red
        "rejected" -> "مرفوضة" to red
        "revoked" -> "ملغاة" to muted
        else -> status to muted
    }

    private fun userBadge(status: String): Pair<String, Int> = when (status) {
        "active" -> "نشط" to green
        "banned" -> "محظور" to red
        "suspended" -> "موقوف" to orange
        else -> (status.ifBlank { "—" }) to muted
    }

    private fun fmtDate(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return "—"
        return try {
            val instant = try {
                Instant.parse(raw)
            } catch (_: Exception) {
                OffsetDateTime.parse(raw).toInstant()
            }
            SimpleDateFormat("yyyy/MM/dd  •  HH:mm", Locale.ENGLISH).format(Date.from(instant))
        } catch (_: Exception) {
            raw
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}