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
                val rows = ArrayList<String>()
                if (users != null) for (i in 0 until users.length()) {
                    val user = users.optJSONObject(i) ?: continue
                    rows.add("${user.optString("username")}  •  ${user.optString("public_id")}\nالحالة: ${user.optString("status")}")
                }
                runOnUiThread {
                    usersBox.removeAllViews()
                    if (rows.isEmpty()) usersBox.addView(line("لا توجد نتائج", 14, false, muted))
                    rows.forEach { value ->
                        usersBox.addView(line(value, 14).apply {
                            setPadding(dp(12), dp(12), dp(12), dp(12))
                            background = rounded(Color.WHITE)
                        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { usersBox.removeAllViews(); usersBox.addView(line(e.message ?: "فشل البحث", 14, false, Color.RED)) }
            }
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
