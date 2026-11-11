package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class MessagesActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val green = Color.rgb(34, 158, 92)
    private val ink = Color.rgb(35, 45, 39)
    private val muted = Color.rgb(116, 130, 120)
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var loading = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(248, 250, 248))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val back = TextView(this).apply {
            text = "‹"; textSize = 32f; gravity = Gravity.CENTER
            setTextColor(ink); setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(48)))
        val title = TextView(this).apply {
            text = "الرسائل"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val refresh = TextView(this).apply {
            text = "تحديث"; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(green); setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(Color.WHITE, 14)
            setOnClickListener { loadConversations() }
        }
        header.addView(refresh)
        root.addView(header)

        status = TextView(this).apply {
            text = "محادثاتك الخاصة"; textSize = 12f; setTextColor(muted)
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(38)))

        val scroll = ScrollView(this).apply { clipToPadding = false }
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadConversations()
    }

    private fun loadConversations() {
        if (loading) return
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrBlank()) {
            status.text = "سجّل الدخول أولاً"
            return
        }
        loading = true
        status.text = "جارٍ تحميل المحادثات..."
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", token)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                runOnUiThread {
                    list.removeAllViews()
                    if (result.status !in 200..299) {
                        status.text = result.body.optString("error", "تعذر تحميل الرسائل")
                    } else if (rows.length() == 0) {
                        status.text = "لا توجد محادثات بعد"
                        showEmpty()
                    } else {
                        status.text = "محادثاتك الخاصة • ${rows.length()}"
                        for (i in 0 until rows.length()) {
                            val item = rows.optJSONObject(i)
                            if (item != null) addConversation(item)
                        }
                    }
                    loading = false
                }
            } catch (_: Exception) {
                runOnUiThread { status.text = "تعذر الاتصال بالخادم"; loading = false }
            }
        }
    }

    private fun showEmpty() {
        val empty = TextView(this).apply {
            text = "💬\n\nهنا ستظهر رسائلك الخاصة\nيمكنك مراسلة أي مستخدم من ملفه الشخصي"
            textSize = 16f; gravity = Gravity.CENTER; setTextColor(muted)
            setPadding(dp(20), dp(70), dp(20), dp(70))
        }
        list.addView(empty, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addConversation(item: JSONObject) {
        val members = item.optJSONArray("members") ?: JSONArray()
        val myId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "")
        var other = JSONObject()
        for (i in 0 until members.length()) {
            val member = members.optJSONObject(i) ?: continue
            if (member.optString("user_id") != myId) { other = member; break }
        }
        val name = other.optString("display_name").takeIf { it.isNotBlank() && it != "null" }
            ?: other.optString("username", "مستخدم")
        val preview = item.optString("last_message").takeIf { it.isNotBlank() && it != "null" } ?: "ابدأ المحادثة"
        val conversationId = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
            ?: item.optString("conversation_id")
        val userId = other.optString("user_id")
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 18)
            isClickable = true
            elevation = dp(1).toFloat()
            setOnClickListener {
                if (conversationId.isNotBlank()) {
                    startActivity(Intent(this@MessagesActivity, DirectChatActivity::class.java)
                        .putExtra("conversation_id", conversationId)
                        .putExtra("other_name", name)
                        .putExtra("other_user_id", userId))
                }
            }
        }
        val avatar = TextView(this).apply {
            text = name.take(1).uppercase(); textSize = 20f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = rounded(green, 28)
        }
        card.addView(avatar, LinearLayout.LayoutParams(dp(52), dp(52)))
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), 0, 0, 0)
        }
        details.addView(TextView(this).apply {
            text = name; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink); maxLines = 1
        })
        details.addView(TextView(this).apply {
            text = preview; textSize = 12f; setTextColor(muted); maxLines = 1
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        card.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) loadConversations() }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
