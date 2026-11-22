package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/**
 * BAK LIVE — redesigned private chat.
 * Consecutive emoji-only messages from one sender are displayed together,
 * while text messages remain separate and keep their original order.
 */
class DirectChatActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val green = Color.rgb(30, 155, 91)
    private val greenDark = Color.rgb(24, 132, 77)
    private val canvas = Color.rgb(250, 251, 250)
    private val ink = Color.rgb(30, 39, 34)
    private val muted = Color.rgb(137, 147, 141)
    private val incoming = Color.rgb(241, 244, 242)

    private lateinit var chatList: LinearLayout
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var headerName: TextView
    private lateinit var headerAvatar: ImageView
    private lateinit var avatarFallback: TextView
    private lateinit var status: TextView

    private var conversationId = ""
    private var otherUserId = ""
    private var otherName = "محادثة"
    private var verified = false
    private var loading = false
    private var sending = false
    private var refreshQueued = false
    private var lastSignature = ""
    private var destroyed = false
    private var avatarUrlLoaded = false

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = intent.getStringExtra("conversation_id").orEmpty()
        otherUserId = intent.getStringExtra("other_user_id").orEmpty()
        otherName = intent.getStringExtra("other_name")?.takeIf { it.isNotBlank() } ?: "محادثة"
        buildNewScreen()
        if (conversationId.isNotBlank()) verifyConversation()
        else if (otherUserId.isNotBlank()) resolveConversation()
        else showStatus("معرّف المحادثة غير صالح")
    }

    private fun buildNewScreen() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            (if (android.os.Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(canvas)
            fitsSystemWindows = true
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        val back = TextView(this).apply {
            text = "‹"; textSize = 34f; gravity = Gravity.CENTER
            setTextColor(ink); setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(48)))
        val avatarFrame = FrameLayout(this)
        avatarFallback = TextView(this).apply {
            text = otherName.trim().firstOrNull()?.toString() ?: "؟"
            textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(greenDark)
            background = shape(Color.rgb(229, 245, 236), 30)
        }
        headerAvatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = shape(Color.rgb(229, 245, 236), 30)
            clipToOutline = true
            contentDescription = "الصورة الشخصية"
        }
        avatarFrame.addView(avatarFallback, FrameLayout.LayoutParams(-1, -1))
        avatarFrame.addView(headerAvatar, FrameLayout.LayoutParams(-1, -1))
        header.addView(avatarFrame, LinearLayout.LayoutParams(dp(44), dp(44)))
        val nameColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        headerName = TextView(this).apply {
            text = otherName; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink); maxLines = 1
        }
        status = TextView(this).apply {
            text = "محادثة خاصة"; textSize = 11f; setTextColor(muted)
        }
        nameColumn.addView(headerName)
        nameColumn.addView(status)
        header.addView(nameColumn, LinearLayout.LayoutParams(0, -2, 1f))
        if (otherUserId.isNotBlank()) loadHeaderAvatar()
        val refresh = TextView(this).apply {
            text = "↻"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(green); setOnClickListener { loadMessages() }
        }
        header.addView(refresh, LinearLayout.LayoutParams(dp(42), dp(48)))
        root.addView(header)
        root.addView(View(this).apply { setBackgroundColor(Color.rgb(235, 239, 236)) },
            LinearLayout.LayoutParams(-1, dp(1)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(12))
        }
        chatList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(14), dp(8), dp(14), dp(12))
        }
        scroll.addView(chatList, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(View(this).apply { setBackgroundColor(Color.rgb(235, 239, 236)) },
            LinearLayout.LayoutParams(-1, dp(1)))
        // Composer follows the compact rounded style used in voice-room chat.
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(30, 22, 24), Color.rgb(18, 15, 17)))
        }
        input = EditText(this).apply {
            hint = "اكتب رسالة..."
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(155, 255, 255, 255))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(48, 48, 48), Color.rgb(25, 25, 25))).apply { cornerRadius = dp(24).toFloat() }
            setPadding(dp(16), 0, dp(16), 0)
            minHeight = dp(48)
            maxLines = 1
            setSingleLine(true)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        sendButton = TextView(this).apply {
            text = "إرسال"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(18), 0, dp(18), 0)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(255, 64, 140), Color.rgb(255, 140, 60))).apply { cornerRadius = dp(23).toFloat() }
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(-2, dp(46)))
        root.addView(composer, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) input.postDelayed({
                if (!destroyed) (input.parent?.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
            }, 180)
        }
        updateSendButton()
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSendButton() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadHeaderAvatar() {
        if (avatarUrlLoaded || otherUserId.isBlank() || destroyed) return
        val auth = token() ?: return
        avatarUrlLoaded = true
        worker.execute {
            try {
                val profile = AuthApi.get("/api/users/$otherUserId", auth)
                val user = profile.body.optJSONObject("user")
                    ?: profile.body.optJSONObject("profile")
                    ?: profile.body
                val url = user.optString("avatar_url").takeIf {
                    it.startsWith("https://") || it.startsWith("http://")
                } ?: user.optString("profile_image").takeIf {
                    it.startsWith("https://") || it.startsWith("http://")
                } ?: user.optString("image_url").takeIf {
                    it.startsWith("https://") || it.startsWith("http://")
                }
                if (url != null) {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            instanceFollowRedirects = true
                        }
                        val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                        if (bitmap != null) runOnUiThread {
                            if (!destroyed && ::headerAvatar.isInitialized) {
                                headerAvatar.setImageBitmap(bitmap)
                                avatarFallback.visibility = View.GONE
                            }
                        }
                    } finally {
                        connection?.disconnect()
                    }
                }
            } catch (_: Exception) {
                avatarUrlLoaded = false
            }
        }
    }

    private fun updateSendButton() {
        if (!::sendButton.isInitialized) return
        val enabled = input.text.toString().isNotBlank() && verified && !sending
        sendButton.isEnabled = enabled
        sendButton.alpha = if (enabled) 1f else 0.45f
    }

    private fun showStatus(message: String) {
        if (!destroyed && ::status.isInitialized) status.text = message
    }

    private fun token(): String? =
        getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    private fun myId(): String =
        getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "").orEmpty()

    private fun verifyConversation() {
        val auth = token() ?: run { showStatus("سجّل الدخول أولاً"); return }
        val self = myId()
        if (self.isBlank()) { showStatus("تعذر التحقق من الحساب"); return }
        showStatus("جارٍ التحقق من المحادثة...")
        worker.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", auth)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                var ok = false
                var recipient = ""
                var recipientName = ""
                if (result.status in 200..299) {
                    for (i in 0 until rows.length()) {
                        val item = rows.optJSONObject(i) ?: continue
                        val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("conversation_id")
                        if (id != conversationId) continue
                        val members = item.optJSONArray("members") ?: JSONArray()
                        if (members.length() != 2) break
                        var selfFound = false
                        var targetFound = false
                        for (j in 0 until members.length()) {
                            val member = members.optJSONObject(j) ?: continue
                            val uid = member.optString("user_id")
                            if (uid == self) selfFound = true
                            else {
                                recipient = uid
                                recipientName = member.optString("display_name").takeIf { it.isNotBlank() && it != "null" }
                                    ?: member.optString("username")
                                if (otherUserId.isBlank() || uid == otherUserId) targetFound = true
                            }
                        }
                        ok = selfFound && targetFound && recipient.isNotBlank()
                        break
                    }
                }
                runOnUiThread {
                    if (destroyed) return@runOnUiThread
                    verified = ok
                    if (ok) {
                        otherUserId = recipient
                        if (otherName == "محادثة" && recipientName.isNotBlank()) otherName = recipientName
                        headerName.text = otherName
                        avatarFallback.text = otherName.trim().firstOrNull()?.toString() ?: "؟"
                        loadHeaderAvatar()
                        showStatus("محادثة خاصة")
                        updateSendButton()
                        loadMessages()
                    } else {
                        showStatus("تعذر فتح المحادثة: تحقق من أطرافها")
                        updateSendButton()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { showStatus("تعذر التحقق من المحادثة") }
            }
        }
    }

    private fun resolveConversation() {
        val auth = token() ?: run { showStatus("سجّل الدخول أولاً"); return }
        val self = myId()
        worker.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", auth)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                var found = ""
                var foundName = ""
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: continue
                    val members = item.optJSONArray("members") ?: continue
                    if (members.length() != 2) continue
                    var hasSelf = false
                    var target: JSONObject? = null
                    for (j in 0 until members.length()) {
                        val m = members.optJSONObject(j) ?: continue
                        when (m.optString("user_id")) {
                            self -> hasSelf = true
                            otherUserId -> target = m
                        }
                    }
                    if (hasSelf && target != null) {
                        found = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("conversation_id")
                        foundName = target.optString("display_name").takeIf { it.isNotBlank() && it != "null" }
                            ?: target.optString("username")
                        break
                    }
                }
                runOnUiThread {
                    if (destroyed) return@runOnUiThread
                    if (found.isNotBlank()) {
                        conversationId = found
                        if (foundName.isNotBlank()) {
                            otherName = foundName
                            headerName.text = foundName
                        }
                        verifyConversation()
                    } else showStatus("لم يتم العثور على محادثة مع هذا المستخدم")
                }
            } catch (_: Exception) {
                runOnUiThread { showStatus("تعذر فتح المحادثة") }
            }
        }
    }

    private fun loadMessages() {
        if (!verified || conversationId.isBlank()) return
        if (loading) { refreshQueued = true; return }
        val auth = token() ?: return
        loading = true
        worker.execute {
            try {
                val result = AuthApi.get("/api/messages/$conversationId", auth)
                val rows = result.body.optJSONArray("messages") ?: JSONArray()
                runOnUiThread {
                    if (!destroyed) {
                        if (result.status in 200..299) {
                            val signature = rows.toString()
                            if (signature != lastSignature) {
                                renderMessages(rows)
                                lastSignature = signature
                            }
                            showStatus("محادثة خاصة")
                        } else showStatus(result.body.optString("error", "تعذر تحميل الرسائل"))
                        loading = false
                        if (refreshQueued) { refreshQueued = false; loadMessages() }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!destroyed) {
                        loading = false
                        showStatus("تعذر الاتصال بالخادم")
                        if (refreshQueued) { refreshQueued = false; loadMessages() }
                    }
                }
            }
        }
    }

    private fun renderMessages(rows: JSONArray) {
        chatList.removeAllViews()
        var i = 0
        while (i < rows.length()) {
            val first = rows.optJSONObject(i)
            if (first == null) { i++; continue }
            val sender = first.optString("sender_id")
            val date = parseDate(first.optString("created_at"))
            val day = date?.let { dayKey(it) }
            if (i == 0 || day != null && day != previousDay(rows, i)) addDayChip(day ?: "")
            val firstText = first.optString("content")
            val group = ArrayList<String>()
            group.add(firstText)
            var lastDate = date
            var next = i + 1

            // Emoji sent as separate rapid messages become one expressive bubble.
            if (isEmojiOnly(firstText)) {
                while (next < rows.length()) {
                    val candidate = rows.optJSONObject(next) ?: break
                    val candidateDate = parseDate(candidate.optString("created_at"))
                    val candidateDay = candidateDate?.let { dayKey(it) }
                    if (candidate.optString("sender_id") != sender ||
                        !isEmojiOnly(candidate.optString("content")) ||
                        (day != null && candidateDay != day)) break
                    group.add(candidate.optString("content"))
                    if (candidateDate != null) lastDate = candidateDate
                    next++
                }
            }

            val content = group.joinToString(" ")
            val mine = sender == myId()
            addMessageBubble(content, mine, lastDate)
            i = if (next > i + 1) next else i + 1
        }
        (chatList.parent as? ScrollView)?.post { (chatList.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun previousDay(rows: JSONArray, index: Int): String? {
        if (index <= 0) return null
        return rows.optJSONObject(index - 1)?.let {
            parseDate(it.optString("created_at"))?.let { d -> dayKey(d) }
        }
    }

    private fun dayKey(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun addDayChip(key: String) {
        val label = when (key) {
            "" -> ""
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) -> "اليوم"
            else -> key
        }
        if (label.isBlank()) return
        val chip = TextView(this).apply {
            text = label; textSize = 11f; setTextColor(muted)
            gravity = Gravity.CENTER; background = shape(Color.rgb(242, 244, 243), 22)
            setPadding(dp(15), dp(7), dp(15), dp(7))
        }
        chatList.addView(chip, LinearLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(12); bottomMargin = dp(12)
        })
    }

    private fun addMessageBubble(text: String, mine: Boolean, date: Date?) {
        val emoji = isEmojiOnly(text)
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (mine) Gravity.END else Gravity.START
            background = if (emoji) null else shape(if (mine) green else incoming, 22)
            setPadding(if (emoji) dp(5) else dp(14), if (emoji) dp(2) else dp(10),
                if (emoji) dp(5) else dp(14), if (emoji) dp(2) else dp(7))
        }
        val body = TextView(this).apply {
            this.text = text
            textSize = if (emoji) 31f else 15.5f
            setTextColor(if (mine) Color.WHITE else ink)
            setLineSpacing(0f, 1.12f)
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        bubble.addView(body)
        if (date != null) {
            bubble.addView(TextView(this).apply {
                this.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                textSize = 9.5f
                setTextColor(if (mine) Color.argb(190, 255, 255, 255) else muted)
                gravity = Gravity.END
            })
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (mine) Gravity.END else Gravity.START
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            addView(bubble)
        }
        chatList.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(if (emoji) 3 else 5)
        })
    }

    private fun isEmojiOnly(value: String): Boolean {
        val text = value.trim()
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            val type = Character.getType(cp)
            val allowed = type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.MODIFIER_SYMBOL.toInt() ||
                type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() ||
                cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E || cp == 0x20E3 ||
                cp in 0x1F3FB..0x1F3FF || Character.isWhitespace(cp)
            if (!allowed) return false
            i += Character.charCount(cp)
        }
        return true
    }

    private fun parseDate(raw: String): Date? {
        if (raw.isBlank() || raw == "null") return null
        val normalized = raw.replace(" ", "T")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                return SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun sendMessage() {
        if (!verified || sending) return
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        if (text.length > 5000) {
            Toast.makeText(this, "الرسالة طويلة جداً", Toast.LENGTH_SHORT).show()
            return
        }
        val auth = token() ?: return
        sending = true
        updateSendButton()
        worker.execute {
            try {
                val result = AuthApi.post(
                    "/api/messages/$conversationId",
                    JSONObject().put("content", text),
                    auth
                )
                runOnUiThread {
                    if (destroyed) return@runOnUiThread
                    sending = false
                    if (result.status in 200..299) {
                        input.setText("")
                        loadMessages()
                    } else {
                        Toast.makeText(this, result.body.optString("error", "تعذر إرسال الرسالة"),
                            Toast.LENGTH_LONG).show()
                    }
                    updateSendButton()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!destroyed) {
                        sending = false
                        updateSendButton()
                        Toast.makeText(this, "تعذر الاتصال بالخادم", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (verified) loadMessages()
    }

    override fun onDestroy() {
        destroyed = true
        worker.shutdownNow()
        super.onDestroy()
    }
}
