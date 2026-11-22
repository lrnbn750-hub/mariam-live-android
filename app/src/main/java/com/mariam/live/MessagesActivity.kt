package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// Conversation list with remote profile avatars and a letter fallback.
class MessagesActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(4)
    private val green = Color.rgb(34, 158, 92)
    private val ink = Color.rgb(35, 45, 39)
    private val muted = Color.rgb(116, 130, 120)
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var loading = false
    private val avatarCache = ConcurrentHashMap<String, Bitmap>()
    private val avatarRequests = ConcurrentHashMap.newKeySet<String>()

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
                        var shown = 0
                        val myId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "").orEmpty()
                        for (i in 0 until rows.length()) {
                            val item = rows.optJSONObject(i) ?: continue
                            if (item.optString("type") != "direct") continue
                            val members = item.optJSONArray("members") ?: continue
                            if (members.length() != 2) continue
                            var hasMe = false
                            var hasOther = false
                            for (j in 0 until members.length()) {
                                val uid = members.optJSONObject(j)?.optString("user_id") ?: continue
                                if (uid == myId) hasMe = true else if (uid.isNotBlank()) hasOther = true
                            }
                            if (hasMe && hasOther) {
                                addConversation(item)
                                shown++
                            }
                        }
                        status.text = if (shown == 0) "لا توجد محادثات خاصة صالحة بعد" else "محادثاتك الخاصة • $shown"
                        if (shown == 0) showEmpty()
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
        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(green, 28)
            clipToOutline = true
            contentDescription = "صورة $name"
        }
        val placeholder = TextView(this).apply {
            text = name.take(1).uppercase(); textSize = 20f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = rounded(green, 28)
        }
        val avatarFrame = FrameLayout(this)
        avatarFrame.addView(placeholder, FrameLayout.LayoutParams(-1, -1))
        avatarFrame.addView(avatar, FrameLayout.LayoutParams(-1, -1))
        card.addView(avatarFrame, LinearLayout.LayoutParams(dp(52), dp(52)))
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

        val directAvatarUrl = other.optString("avatar_url").takeIf { it.startsWith("https://") }
        if (directAvatarUrl != null) {
            loadAvatar(directAvatarUrl, avatar, placeholder)
        } else if (userId.isNotBlank()) {
            loadAvatarFromProfile(userId, avatar, placeholder)
        }
    }

    private fun loadAvatarFromProfile(userId: String, target: ImageView, placeholder: View) {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        executor.execute {
            try {
                val profile = AuthApi.get("/api/users/$userId", token)
                val user = profile.body.optJSONObject("user") ?: return@execute
                val avatarUrl = user.optString("avatar_url")
                if (avatarUrl.startsWith("https://")) {
                    runOnUiThread { if (!isFinishing && target.parent != null) loadAvatar(avatarUrl, target, placeholder) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadAvatar(url: String, target: ImageView, placeholder: View) {
        target.tag = url
        val cached = avatarCache[url]
        if (cached != null) {
            target.setImageBitmap(cached)
            placeholder.visibility = View.GONE
            return
        }
        if (!avatarRequests.add(url)) return
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                }
                connection.inputStream.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        avatarCache[url] = bitmap
                        runOnUiThread {
                            if (!isFinishing && target.tag == url) {
                                target.setImageBitmap(bitmap)
                                placeholder.visibility = View.GONE
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                connection?.disconnect()
                avatarRequests.remove(url)
            }
        }
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) loadConversations() }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
