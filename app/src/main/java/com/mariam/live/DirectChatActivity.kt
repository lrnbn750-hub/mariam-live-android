package com.mariam.live

import android.app.Activity
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

class DirectChatActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val green = Color.rgb(34, 158, 92)
    private val ink = Color.rgb(35, 45, 39)
    private val muted = Color.rgb(116, 130, 120)
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var conversationId = ""
    private var otherName = "محادثة"
    private var loading = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = intent.getStringExtra("conversation_id").orEmpty()
        otherName = intent.getStringExtra("other_name") ?: "محادثة"
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(248, 250, 248)); setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val back = TextView(this).apply {
            text = "‹"; textSize = 32f; gravity = Gravity.CENTER
            setTextColor(ink); setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(48)))
        header.addView(TextView(this).apply {
            text = otherName; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val refresh = TextView(this).apply {
            text = "↻"; textSize = 24f; gravity = Gravity.CENTER; setTextColor(green)
            setOnClickListener { loadMessages() }
        }
        header.addView(refresh, LinearLayout.LayoutParams(dp(42), dp(48)))
        root.addView(header)

        status = TextView(this).apply { text = ""; textSize = 11f; setTextColor(muted) }
        root.addView(status)
        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = true }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(2), dp(10), dp(2), dp(10))
        }
        scroll.addView(messages, ScrollView.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(0, dp(8), 0, dp(4))
        }
        input = EditText(this).apply {
            hint = "اكتب رسالتك..."; textSize = 14f; maxLines = 4
            setTextColor(ink); background = rounded(Color.WHITE, 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        val send = TextView(this).apply {
            text = "إرسال"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = rounded(green, 16)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            setOnClickListener { sendMessage() }
        }
        composer.addView(send, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        root.addView(composer)
        setContentView(root)
        if (conversationId.isBlank()) {
            status.text = "معرّف المحادثة غير صالح"
        } else loadMessages()
    }

    private fun loadMessages() {
        if (loading || conversationId.isBlank()) return
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        loading = true
        status.text = "جارٍ تحميل الرسائل..."
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/$conversationId", token)
                val rows = result.body.optJSONArray("messages") ?: JSONArray()
                runOnUiThread {
                    messages.removeAllViews()
                    if (result.status in 200..299) {
                        for (i in 0 until rows.length()) {
                            val message = rows.optJSONObject(i)
                            if (message != null) addMessage(message)
                        }
                        status.text = if (rows.length() == 0) "ابدأ أول محادثة بينكما" else " "
                    } else status.text = result.body.optString("error", "تعذر تحميل الرسائل")
                    loading = false
                }
            } catch (_: Exception) {
                runOnUiThread { status.text = "تعذر الاتصال بالخادم"; loading = false }
            }
        }
    }

    private fun addMessage(item: JSONObject) {
        val ownId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "")
        val mine = item.optString("sender_id") == ownId
        val bubble = TextView(this).apply {
            text = item.optString("content"); textSize = 14f
            setTextColor(if (mine) Color.WHITE else ink)
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(if (mine) green else Color.WHITE, 16)
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (mine) Gravity.START else Gravity.END
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(3), dp(3), dp(3), dp(3))
            addView(bubble)
        }
        messages.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendMessage() {
        val content = input.text.toString().trim()
        if (content.isBlank()) return
        if (content.length > 5000) {
            Toast.makeText(this, "الرسالة طويلة جداً", Toast.LENGTH_SHORT).show()
            return
        }
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        input.isEnabled = false
        executor.execute {
            try {
                val result = AuthApi.post(
                    "/api/messages/$conversationId",
                    JSONObject().put("content", content),
                    token
                )
                runOnUiThread {
                    input.isEnabled = true
                    if (result.status in 200..299) {
                        input.setText("")
                        loadMessages()
                    } else Toast.makeText(this, result.body.optString("error", "تعذر إرسال الرسالة"), Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    input.isEnabled = true
                    Toast.makeText(this, "تعذر الاتصال بالخادم", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
