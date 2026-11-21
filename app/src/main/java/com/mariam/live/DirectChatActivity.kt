package com.mariam.live

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

private const val ICON_BACK = 0
private const val ICON_SEND = 1
private const val ICON_REFRESH = 2

class DirectChatActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    // ألوان الهوية — خلفية بيضاء نظيفة مع لمسات خضراء
    private val green = Color.rgb(34, 158, 92)
    private val greenSoft = Color.rgb(230, 245, 237)
    private val ink = Color.rgb(24, 32, 28)
    private val muted = Color.rgb(121, 131, 125)
    private val bubbleOther = Color.rgb(241, 243, 245)
    private val divider = Color.rgb(236, 239, 241)
    private val fieldBg = Color.rgb(244, 246, 248)

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: View
    private lateinit var status: TextView
    private lateinit var titleView: TextView
    private lateinit var avatarView: TextView
    private var conversationId = ""
    private var otherUserId = ""
    private var otherName = "محادثة"
    private var loading = false
    private var sending = false
    private var lastMessagesSignature = ""

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(color)
    }

    private fun ripple(content: Drawable?): Drawable =
        RippleDrawable(ColorStateList.valueOf(Color.argb(36, 0, 0, 0)), content, null)

    /** أيقونات مرسومة بالكود (بدون ملفات صور) */
    private inner class Icon(private val kind: Int, private val tint: Int) : View(this@DirectChatActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val w = width - paddingLeft - paddingRight
            val h = height - paddingTop - paddingBottom
            c.save()
            c.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            c.scale(w / 24f, h / 24f)
            paint.color = tint
            when (kind) {
                ICON_BACK -> {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.4f
                    paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
                    val p = Path().apply { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
                    c.drawPath(p, paint)
                }
                ICON_SEND -> {
                    paint.style = Paint.Style.FILL
                    val p = Path().apply {
                        moveTo(2f, 21f); lineTo(23f, 12f); lineTo(2f, 3f)
                        lineTo(2f, 10f); lineTo(17f, 12f); lineTo(2f, 14f); close()
                    }
                    c.drawPath(p, paint)
                }
                ICON_REFRESH -> {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.2f
                    paint.strokeCap = Paint.Cap.ROUND
                    c.drawArc(RectF(5f, 5f, 19f, 19f), 40f, 280f, false, paint)
                    val a = Math.toRadians(-40.0)
                    val ex = (12 + 7 * Math.cos(a)).toFloat()
                    val ey = (12 + 7 * Math.sin(a)).toFloat()
                    val tx = (-Math.sin(a)).toFloat(); val ty = Math.cos(a).toFloat()
                    val rx = Math.cos(a).toFloat(); val ry = Math.sin(a).toFloat()
                    paint.style = Paint.Style.FILL
                    val head = Path().apply {
                        moveTo(ex + tx * 3f, ey + ty * 3f)
                        lineTo(ex + rx * 3f, ey + ry * 3f)
                        lineTo(ex - rx * 3f, ey - ry * 3f)
                        close()
                    }
                    c.drawPath(head, paint)
                }
            }
            c.restore()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = intent.getStringExtra("conversation_id").orEmpty()
        otherUserId = intent.getStringExtra("other_user_id").orEmpty()
        otherName = intent.getStringExtra("other_name")?.takeIf { it.isNotBlank() } ?: "محادثة"

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
        }

        // ───────── الهيدر ─────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(8), 0, dp(8), 0)
            setBackgroundColor(Color.WHITE)
        }
        val back = Icon(ICON_BACK, ink).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            background = ripple(null)
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))

        avatarView = TextView(this).apply {
            gravity = Gravity.CENTER; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(green); background = oval(greenSoft)
        }
        header.addView(avatarView, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(4) })

        titleView = TextView(this).apply {
            textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        header.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(12); marginEnd = dp(8)
        })

        val refresh = Icon(ICON_REFRESH, green).apply {
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = ripple(null)
            setOnClickListener { loadMessages() }
        }
        header.addView(refresh, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(60)))
        root.addView(View(this).apply { setBackgroundColor(divider) }, LinearLayout.LayoutParams(-1, dp(1)))
        updateHeader()

        // ───────── سطر الحالة ─────────
        status = TextView(this).apply {
            textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8)); visibility = View.GONE
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2))

        // ───────── الرسائل ─────────
        scroll = ScrollView(this).apply {
            clipToPadding = false; isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        scroll.addView(messages, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ───────── حقل الكتابة ─────────
        root.addView(View(this).apply { setBackgroundColor(divider) }, LinearLayout.LayoutParams(-1, dp(1)))
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        input = EditText(this).apply {
            hint = "اكتب رسالتك..."; setHintTextColor(muted)
            textSize = 15f; maxLines = 4; minHeight = dp(44)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(ink); background = rounded(fieldBg, 22)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(10))
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = Icon(ICON_SEND, Color.WHITE).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = ripple(oval(green))
            // السهم يتجه لليسار في الواجهة العربية
            scaleX = -1f
            setOnClickListener { sendMessage() }
        }
        composer.addView(send, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) })
        root.addView(composer, LinearLayout.LayoutParams(-1, -2))

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updateSendState()
        })
        updateSendState()

        setContentView(root)
        when {
            conversationId.isNotBlank() -> loadMessages()
            otherUserId.isNotBlank() -> resolveConversation()
            else -> setStatus("معرّف المحادثة غير صالح")
        }
    }

    private fun setStatus(text: String) {
        status.text = text
        status.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun updateHeader() {
        titleView.text = otherName
        avatarView.text = otherName.trim().firstOrNull()?.toString() ?: "؟"
    }

    private fun updateSendState() {
        val enabled = input.text.toString().isNotBlank() && !sending
        send.isEnabled = enabled
        send.alpha = if (enabled) 1f else 0.4f
    }

    // إذا وصل إشعار الرسالة بمعرّف المرسل فقط، ابحث عن محادثته وافتحها مباشرة.
    private fun resolveConversation() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        setStatus("جارٍ فتح محادثة المرسل...")
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", token)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                var foundId = ""
                var foundName = ""
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: continue
                    val members = item.optJSONArray("members") ?: JSONArray()
                    for (j in 0 until members.length()) {
                        val member = members.optJSONObject(j) ?: continue
                        if (member.optString("user_id") == otherUserId) {
                            foundId = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                                ?: item.optString("conversation_id")
                            foundName = member.optString("display_name").takeIf { it.isNotBlank() && it != "null" }
                                ?: member.optString("username")
                            break
                        }
                    }
                    if (foundId.isNotBlank()) break
                }
                runOnUiThread {
                    if (foundId.isNotBlank()) {
                        conversationId = foundId
                        if (otherName == "محادثة" && foundName.isNotBlank()) {
                            otherName = foundName
                            updateHeader()
                        }
                        loadMessages()
                    } else {
                        setStatus("لم يتم العثور على محادثة مع هذا المستخدم")
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { setStatus("تعذر فتح محادثة المرسل") }
            }
        }
    }

    private fun loadMessages() {
        if (loading || conversationId.isBlank()) return
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        loading = true
        if (lastMessagesSignature.isEmpty()) setStatus("جارٍ تحميل الرسائل...")
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/$conversationId", token)
                val rows = result.body.optJSONArray("messages") ?: JSONArray()
                runOnUiThread {
                    if (result.status in 200..299) {
                        val signature = rows.toString()
                        if (signature != lastMessagesSignature) {
                            messages.removeAllViews()
                            for (i in 0 until rows.length()) {
                                val message = rows.optJSONObject(i)
                                if (message != null) addMessage(message)
                            }
                            lastMessagesSignature = signature
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                        }
                        setStatus(if (rows.length() == 0) "ابدأ أول محادثة بينكما" else "")
                    } else setStatus(result.body.optString("error", "تعذر تحميل الرسائل"))
                    loading = false
                }
            } catch (_: Exception) {
                runOnUiThread { setStatus("تعذر الاتصال بالخادم"); loading = false }
            }
        }
    }

    /** يحوّل وقت الخادم (UTC) إلى وقت الجهاز، ويرجع نصاً فارغاً إذا لم يتوفر الحقل */
    private fun formatTime(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        return try {
            val clean = raw.substringBefore('.').substringBefore('Z').substringBefore('+').replace(' ', 'T')
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(clean) ?: return ""
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            ""
        }
    }

    private fun addMessage(item: JSONObject) {
        val ownId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "")
        val mine = item.optString("sender_id") == ownId

        val big = dp(18).toFloat()
        val small = dp(5).toFloat()
        val shape = GradientDrawable().apply {
            setColor(if (mine) green else bubbleOther)
            // ركن صغير جهة المرسل لإعطاء شكل "ذيل" احترافي
            cornerRadii = if (mine)
                floatArrayOf(big, big, big, big, big, big, small, small)
            else
                floatArrayOf(big, big, big, big, small, small, big, big)
        }

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape
            setPadding(dp(13), dp(9), dp(13), dp(7))
        }
        bubble.addView(TextView(this).apply {
            text = item.optString("content"); textSize = 15f
            setTextColor(if (mine) Color.WHITE else ink)
            setLineSpacing(0f, 1.15f)
            maxWidth = (resources.displayMetrics.widthPixels * 0.74f).toInt()
        })
        val time = formatTime(item.optString("created_at"))
        if (time.isNotEmpty()) {
            bubble.addView(TextView(this).apply {
                text = time; textSize = 10f
                setTextColor(if (mine) Color.argb(190, 255, 255, 255) else muted)
            }, LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.END; topMargin = dp(3)
            })
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (mine) Gravity.START else Gravity.END
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(3), 0, dp(3))
            addView(bubble)
        }
        messages.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendMessage() {
        val content = input.text.toString().trim()
        if (content.isBlank() || sending) return
        if (content.length > 5000) {
            Toast.makeText(this, "الرسالة طويلة جداً", Toast.LENGTH_SHORT).show()
            return
        }
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        sending = true
        input.isEnabled = false
        updateSendState()
        executor.execute {
            try {
                val result = AuthApi.post(
                    "/api/messages/$conversationId",
                    JSONObject().put("content", content),
                    token
                )
                runOnUiThread {
                    sending = false
                    input.isEnabled = true
                    if (result.status in 200..299) {
                        input.setText("")
                        loadMessages()
                    } else Toast.makeText(this, result.body.optString("error", "تعذر إرسال الرسالة"), Toast.LENGTH_LONG).show()
                    updateSendState()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    sending = false
                    input.isEnabled = true
                    updateSendState()
                    Toast.makeText(this, "تعذر الاتصال بالخادم", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}