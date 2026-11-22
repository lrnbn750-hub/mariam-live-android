package com.mariam.live

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

private const val ICON_BACK = 0
private const val ICON_SEND = 1
private const val ICON_REFRESH = 2

class DirectChatActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    // ───── الألوان: خلفية بيضاء + هوية خضراء ─────
    private val green = Color.rgb(34, 158, 92)
    private val greenSoft = Color.rgb(230, 245, 237)
    private val ink = Color.rgb(24, 32, 28)
    private val muted = Color.rgb(121, 131, 125)
    private val bubbleOther = Color.rgb(241, 243, 245)
    private val divider = Color.rgb(236, 239, 241)
    private val fieldBorder = Color.rgb(221, 226, 230)
    private val fieldBg = Color.rgb(247, 248, 250)

    private lateinit var root: LinearLayout
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: View
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var avatarView: TextView

    private var conversationId = ""
    private var otherUserId = ""
    private var otherName = "محادثة"
    private var loading = false
    private var reloadQueued = false
    private var lastMessagesSignature = ""
    private var lastBottomInset = 0
    private var contentWidthPx = 0
    private var directConversationValidated = false
    private var sending = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(color)
    }

    private fun ripple(content: Drawable?): Drawable =
        RippleDrawable(ColorStateList.valueOf(Color.argb(36, 0, 0, 0)), content, oval(Color.WHITE))

    /** أيقونات مرسومة بالكود (واضحة على كل الدقّات) */
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
        try {
            buildScreen()
        } catch (e: Throwable) {
            Toast.makeText(this, "خطأ: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun buildScreen() {
        conversationId = intent.getStringExtra("conversation_id").orEmpty()
        otherUserId = intent.getStringExtra("other_user_id").orEmpty()
        otherName = intent.getStringExtra("other_name")?.takeIf { it.isNotBlank() } ?: "محادثة"

        setupSystemBars()

        // أقصى عرض للمحتوى: يمنع تمدد الفقاعات على التابلت والشاشات الكبيرة
        contentWidthPx = minOf(resources.displayMetrics.widthPixels, dp(720))

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
        }
        applyInsets(root)

        // ───────── الهيدر ─────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.WHITE)
        }
        val back = Icon(ICON_BACK, ink).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            background = ripple(null)
            contentDescription = "رجوع"
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))

        avatarView = TextView(this).apply {
            gravity = Gravity.CENTER; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(green); background = oval(greenSoft)
        }
        header.addView(avatarView, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginStart = dp(2) })

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        titleView = TextView(this).apply {
            textSize = 16.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        subtitleView = TextView(this).apply {
            textSize = 12f; setTextColor(muted); maxLines = 1
            ellipsize = TextUtils.TruncateAt.END; visibility = View.GONE
        }
        titleColumn.addView(titleView)
        titleColumn.addView(subtitleView)
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(12); marginEnd = dp(8)
        })

        val refresh = Icon(ICON_REFRESH, green).apply {
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = ripple(null)
            contentDescription = "تحديث"
            setOnClickListener { loadMessages() }
        }
        header.addView(refresh, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(View(this).apply { setBackgroundColor(divider) }, LinearLayout.LayoutParams(-1, dp(1)))
        updateHeader()

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
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        scroll.addView(messages, FrameLayout.LayoutParams(contentWidthPx, -2, Gravity.CENTER_HORIZONTAL))
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
            textSize = 15f; minHeight = dp(44)
            setTextColor(ink)
            background = rounded(fieldBg, 22, fieldBorder)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(10))
            // زر "إرسال" على الكيبورد: لازم النوع بدون MULTI_LINE ثم نسمح بتعدد الأسطر يدوياً
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setHorizontallyScrolling(false)
            maxLines = 4
            setOnEditorActionListener { _, actionId, event ->
                val enterPressed = event != null &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    !event.isShiftPressed
                if (actionId == EditorInfo.IME_ACTION_SEND || enterPressed) {
                    sendMessage(); true
                } else false
            }
            setOnFocusChangeListener { _, focused ->
                background = rounded(Color.WHITE, 22, if (focused) green else fieldBorder)
                if (focused) scrollToBottom()
            }
            setOnClickListener { scrollToBottom() }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = Icon(ICON_SEND, Color.WHITE).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = ripple(oval(green))
            scaleX = -1f // السهم يتجه لليسار في الواجهة العربية
            contentDescription = "إرسال"
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
        root.requestApplyInsets()

        when {
            conversationId.isNotBlank() -> validateDirectConversation()
            otherUserId.isNotBlank() -> resolveConversation()
            else -> setStatus("معرّف المحادثة غير صالح")
        }
    }

    // ───────── الحواف الآمنة: لا شيء يلتصق بالشحن/الشبكة أو شريط التنقل ─────────
    private fun setupSystemBars() {
        try {
            window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE
            if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = flags
        } catch (_: Throwable) {
        }
    }

    private fun applyInsets(target: View) {
        target.setOnApplyWindowInsetsListener { v, insets ->
            try {
                @Suppress("DEPRECATION")
                val l = insets.systemWindowInsetLeft
                @Suppress("DEPRECATION")
                val t = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                val r = insets.systemWindowInsetRight
                @Suppress("DEPRECATION")
                val b = insets.systemWindowInsetBottom
                v.setPadding(l, t, r, b)
                if (b != lastBottomInset) {
                    lastBottomInset = b
                    scrollToBottom()
                }
            } catch (_: Throwable) {
            }
            insets
        }
    }

    private fun scrollToBottom() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    // ───────── حالة الواجهة ─────────
    private fun setStatus(text: String) {
        subtitleView.text = text
        subtitleView.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun updateHeader() {
        titleView.text = otherName
        avatarView.text = otherName.trim().firstOrNull()?.toString() ?: "؟"
    }

    private fun updateSendState() {
        val enabled = input.text.toString().isNotBlank()
        send.isEnabled = enabled && !sending && directConversationValidated
        send.alpha = if (send.isEnabled) 1f else 0.4f
    }

    // إذا وصل إشعار الرسالة بمعرّف المرسل فقط، ابحث عن محادثته وافتحها مباشرة.
    private fun resolveConversation() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        setStatus("جارٍ فتح المحادثة...")
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", token)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                var foundId = ""
                var foundName = ""
                val myId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "").orEmpty()
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: continue
                    if (item.optString("type") != "direct") continue
                    val members = item.optJSONArray("members") ?: JSONArray()
                    if (members.length() != 2) continue
                    var hasMe = false
                    var target: JSONObject? = null
                    for (j in 0 until members.length()) {
                        val member = members.optJSONObject(j) ?: continue
                        when (member.optString("user_id")) {
                            myId -> hasMe = true
                            otherUserId -> target = member
                        }
                    }
                    if (hasMe && target != null) {
                        foundId = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("conversation_id")
                        foundName = target.optString("display_name").takeIf { it.isNotBlank() && it != "null" }
                            ?: target.optString("username")
                        break
                    }
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
                runOnUiThread { setStatus("تعذر فتح المحادثة") }
            }
        }
    }

    // Confirm a private chat contains exactly the signed-in user and the intended recipient.
    private fun validateDirectConversation() {
        if (conversationId.isBlank()) return
        val prefs = getSharedPreferences("MariamLive", MODE_PRIVATE)
        val token = prefs.getString("token", null) ?: return
        val myId = prefs.getString("user_id", "").orEmpty()
        if (myId.isBlank()) { setStatus("تعذر التحقق من حسابك"); return }
        setStatus("جارٍ التحقق من خصوصية المحادثة...")
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/conversations", token)
                val rows = result.body.optJSONArray("conversations") ?: JSONArray()
                var valid = false
                var recipientId = ""
                var recipientName = ""
                if (result.status in 200..299) {
                    for (i in 0 until rows.length()) {
                        val item = rows.optJSONObject(i) ?: continue
                        if (item.optString("id") != conversationId || item.optString("type") != "direct") continue
                        val members = item.optJSONArray("members") ?: JSONArray()
                        if (members.length() != 2) break
                        var hasMe = false
                        var hasRecipient = false
                        for (j in 0 until members.length()) {
                            val member = members.optJSONObject(j) ?: continue
                            val uid = member.optString("user_id")
                            if (uid == myId) hasMe = true else {
                                recipientId = uid
                                recipientName = member.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: member.optString("username")
                                if (otherUserId.isBlank() || uid == otherUserId) hasRecipient = true
                            }
                        }
                        valid = hasMe && hasRecipient && recipientId.isNotBlank()
                        break
                    }
                }
                runOnUiThread {
                    if (valid) {
                        otherUserId = recipientId
                        if (otherName == "محادثة" && recipientName.isNotBlank()) otherName = recipientName
                        directConversationValidated = true
                        updateHeader()
                        updateSendState()
                        loadMessages()
                    } else {
                        directConversationValidated = false
                        setStatus("تم إيقاف المحادثة: أطرافها غير صحيحة أو تضم أكثر من شخص")
                        updateSendState()
                    }
                }
            } catch (_: Exception) { runOnUiThread { setStatus("تعذر التحقق من خصوصية المحادثة") } }
        }
    }
    private fun loadMessages() {
        if (conversationId.isBlank() || !directConversationValidated) return
        if (loading) { reloadQueued = true; return }
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        loading = true
        if (lastMessagesSignature.isEmpty()) setStatus("جارٍ التحميل...")
        executor.execute {
            try {
                val result = AuthApi.get("/api/messages/$conversationId", token)
                val rows = result.body.optJSONArray("messages") ?: JSONArray()
                runOnUiThread {
                    sending = false
                    updateSendState()
                    if (result.status in 200..299) {
                        val signature = rows.toString()
                        if (signature != lastMessagesSignature) {
                            renderMessages(rows)
                            lastMessagesSignature = signature
                            scrollToBottom()
                        }
                        setStatus("")
                    } else setStatus(result.body.optString("error", "تعذر تحميل الرسائل"))
                    finishLoading()
                }
            } catch (_: Exception) {
                runOnUiThread { setStatus("تعذر الاتصال بالخادم"); finishLoading() }
            }
        }
    }

    private fun finishLoading() {
        loading = false
        if (reloadQueued) { reloadQueued = false; loadMessages() }
    }

    // ───────── عرض الرسائل (فواصل أيام + تجميع رسائل نفس المرسل) ─────────
    private fun renderMessages(rows: JSONArray) {
        messages.removeAllViews()
        if (rows.length() == 0) {
            messages.addView(TextView(this).apply {
                text = "ابدأ أول محادثة بينكما"; textSize = 14f
                setTextColor(muted); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(64) })
            return
        }
        val ownId = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("user_id", "")
        var prevSender: String? = null
        var prevDay: String? = null
        for (i in 0 until rows.length()) {
            val item = rows.optJSONObject(i) ?: continue
            val sender = item.optString("sender_id")
            val date = parseDate(item.optString("created_at"))
            val day = date?.let { dayLabel(it) }

            if (day != null && day != prevDay) {
                addDaySeparator(day)
                prevSender = null
            }
            val nextSender = rows.optJSONObject(i + 1)?.optString("sender_id")
            val nextDay = rows.optJSONObject(i + 1)?.let { n ->
                parseDate(n.optString("created_at"))?.let { dayLabel(it) }
            }
            val isLastInGroup = nextSender != sender || (day != null && nextDay != day)
            val topGap = if (prevSender == sender) 2 else 8

            addBubble(
                content = item.optString("content"),
                mine = sender == ownId,
                time = date?.let { formatClock(it) } ?: "",
                tail = isLastInGroup,
                topGap = topGap
            )
            prevSender = sender
            if (day != null) prevDay = day
        }
    }

    private fun addDaySeparator(label: String) {
        messages.addView(TextView(this).apply {
            text = label; textSize = 11.5f; setTextColor(muted)
            background = rounded(bubbleOther, 12)
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }, LinearLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(14); bottomMargin = dp(6)
        })
    }

    private fun addBubble(content: String, mine: Boolean, time: String, tail: Boolean, topGap: Int): View {
        val big = dp(18).toFloat()
        val small = if (tail) dp(5).toFloat() else big
        val shape = GradientDrawable().apply {
            setColor(if (mine) green else bubbleOther)
            // الأركان: tl, tr, br, bl — الركن الصغير جهة المرسل في آخر رسالة بالمجموعة
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
            text = content; textSize = 15f
            setTextColor(if (mine) Color.WHITE else ink)
            setLineSpacing(0f, 1.15f)
            maxWidth = (contentWidthPx * 0.78f).toInt() - dp(26)
            setTextIsSelectable(false)
        })
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
            addView(bubble)
        }
        messages.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topGap) })
        return row
    }

    // ───────── التواريخ (الخادم بتوقيت UTC) ─────────
    private fun parseDate(raw: String): Date? {
        if (raw.isBlank() || raw == "null") return null
        return try {
            val clean = raw.substringBefore('.').substringBefore('Z').substringBefore('+').replace(' ', 'T')
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(clean)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatClock(d: Date): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(d)

    private fun dayLabel(d: Date): String {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { time = d }
        fun same(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        if (same(c, now)) return "اليوم"
        now.add(Calendar.DAY_OF_YEAR, -1)
        if (same(c, now)) return "أمس"
        return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(d)
    }

    // ───────── الإرسال (زر الإرسال أو زر الكيبورد) ─────────
    private fun sendMessage() {
        if (!directConversationValidated) { Toast.makeText(this, "لم يتم التأكد من خصوصية المحادثة", Toast.LENGTH_LONG).show(); return }
        if (sending) return
        val content = input.text.toString().trim()
        if (content.isBlank()) return
        if (content.length > 5000) {
            Toast.makeText(this, "الرسالة طويلة جداً", Toast.LENGTH_SHORT).show()
            return
        }
        if (conversationId.isBlank()) return
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return

        sending = true
        updateSendState()
        // إظهار الرسالة فوراً وإبقاء الكيبورد مفتوحاً لكتابة التالية
        input.setText("")
        val pending = addBubble(content, mine = true, time = "", tail = true, topGap = 8)
        scrollToBottom()

        executor.execute {
            try {
                val result = AuthApi.post(
                    "/api/messages/$conversationId",
                    JSONObject().put("content", content),
                    token
                )
                runOnUiThread {
                    if (result.status in 200..299) {
                        loadMessages()
                    } else {
                        messages.removeView(pending)
                        restoreDraft(content)
                        Toast.makeText(this, result.body.optString("error", "تعذر إرسال الرسالة"), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    sending = false
                    updateSendState()
                    messages.removeView(pending)
                    restoreDraft(content)
                    Toast.makeText(this, "تعذر الاتصال بالخادم", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun restoreDraft(content: String) {
        if (input.text.isNullOrBlank()) {
            input.setText(content)
            input.setSelection(input.text.length)
        }
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}