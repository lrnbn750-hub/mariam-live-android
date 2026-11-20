package com.mariam.live

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.view.animation.DecelerateInterpolator
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * مشغل المشاهدة الجماعية (YouTube) داخل الغرفة الصوتية نفسها.
 *
 * - مكان مخصص للمشغل فوق المقاعد: عند التشغيل تتصغر المقاعد قليلاً ويظهر الفيديو فوقها،
 *   وعند الإغلاق ترجع المقاعد لشكلها الطبيعي (بدون نافذة عائمة وبدون Activity مستقلة).
 * - تحكم بالصوت (كتم + شريط مستوى) لكل مستخدم محلياً دون التأثير على بقية الأعضاء.
 * - المالك فقط يختار الفيديو ويتحكم بالتشغيل/الإيقاف/التقديم.
 * - الأعضاء يشاهدون متزامنين مع المالك (نفس endpoints القديمة، بدون أي تغيير في الخادم).
 * - لا يلمس LiveKit ولا المقاعد ولا الدردشة ولا الهدايا.
 */
class RoomWatchPlayer(
    private val activity: Activity,
    /** المكان المخصص (FrameLayout ارتفاعه 0 داخل صفحة الغرفة، بين الهيدر والمقاعد). */
    private val host: FrameLayout,
    /** شبكة المقاعد: تُصغَّر قليلاً أثناء المشاهدة وتعود طبيعية عند الإغلاق. */
    private val anchorProvider: () -> View?,
    private val roomId: String,
    private val token: String,
    private val isOwnerHint: () -> Boolean
) {
    private companion object {
        val ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
        const val EMBED_ORIGIN = "https://nokiraq.duckdns.org"
        const val HEARTBEAT_MS = 5000L
        /** مقدار تصغير المقاعد أثناء المشاهدة (1.0 = بدون تصغير). */
        const val SEAT_SCALE = 0.8f
        /** ارتفاع أكبر ليستغل الفيديو عرض المساحة المتاحة بنسبة 16:9. */
        const val MAX_VIDEO_DP = 240
        const val VOLUME_ROW_DP = 32
        const val VOLUME_GAP_DP = 4
        const val PREF_VOLUME = "watch_volume"
        const val PREF_VOLUME_LAST = "watch_volume_last"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---- views ----
    private var panelWindow: LinearLayout? = null
    private var web: WebView? = null
    private var emptyView: TextView? = null
    private var topBar: LinearLayout? = null
    private var bottomBar: FrameLayout? = null
    private var titleView: TextView? = null
    private var volumeIcon: WatchGlyph? = null
    private var volumeSeek: SeekBar? = null
    private var volumeText: TextView? = null
    private var playBtn: WatchGlyph? = null
    private var backBtn: WatchGlyph? = null
    private var fwdBtn: WatchGlyph? = null
    private var searchBtn: WatchGlyph? = null
    private var progressFill: View? = null
    private var searchDialog: Dialog? = null

    // ---- state ----
    private var started = false
    private var released = false
    private var hostPaused = false
    private var poll: Job? = null
    private var controlsJob: Job? = null
    private var isOwner = false
    private var playerReady = false
    private var visible = false
    private var controlsShown = true
    private var initialSeen = false
    private var lastApplied = -1L
    private var videoId = ""
    private var videoTitle = ""
    private var desired = "paused"
    private var desiredTime = 0.0
    private var dismissedVideoId: String? = null
    private var playerCode = -1
    private var curTime = 0.0
    private var lastHeartbeat = 0L
    private var lastKick = 0L
    private var lastErrorToastAt = 0L
    private val thumbCache = HashMap<String, Bitmap>()

    // ---- volume (محلي لكل مستخدم ويُحفظ بين الجلسات)
    private val prefs = activity.getSharedPreferences("MariamLive", Context.MODE_PRIVATE)
    private var volume = prefs.getInt(PREF_VOLUME, 80).coerceIn(0, 100)
    private var lastVolume = prefs.getInt(PREF_VOLUME_LAST, 80).coerceIn(1, 100)

    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private val accentA = Color.rgb(255, 75, 90)
    private val accentB = Color.rgb(255, 64, 129)

    private fun accentGradient(radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(accentA, accentB)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun toast(msg: String) {
        if (!released && !activity.isFinishing) Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    // =====================================================================
    // Public API (يستدعيها RoomActivity)
    // =====================================================================

    fun start() {
        if (started || released) return
        started = true
        poll = scope.launch {
            while (isActive) {
                if (!hostPaused) refreshState()
                delay(if (visible) 2200L else 3500L)
            }
        }
    }

    /** زر "مشاهدة جماعية" في قائمة أدوات الغرفة. */
    fun open() {
        if (released) return
        start()
        scope.launch {
            refreshState()
            dismissedVideoId = null
            if (visible) {
                if (isOwner) showSearch() else {
                    showControls(true)
                    scheduleControlsHide()
                }
                return@launch
            }
            if (!isOwner && !ID_REGEX.matches(videoId)) {
                toast("لم يبدأ صاحب الغرفة مشاهدة أي فيديو بعد")
                return@launch
            }
            if (isOwner && !ID_REGEX.matches(videoId)) {
                showSearch()
                return@launch
            }
            show()
            pushToPlayer()
        }
    }

    fun containsTouch(rawX: Float, rawY: Float): Boolean {
        if (panelWindow == null || !visible || host.height <= 0) return false
        val loc = IntArray(2)
        host.getLocationOnScreen(loc)
        return rawX >= loc[0] && rawX <= loc[0] + host.width && rawY >= loc[1] && rawY <= loc[1] + host.height
    }

    fun onHostPause() {
        hostPaused = true
        if (visible) {
            // المالك: نوقف للجميع بدل أن يبقى الأعضاء يتقدمون بدونه.
            if (isOwner && desired == "playing") {
                desired = "paused"
                persistDetached("paused", curTime)
            }
            web?.evaluateJavascript("if(ytPlayer&&ytPlayer.pauseVideo)ytPlayer.pauseVideo()", null)
        }
        web?.onPause()
    }

    fun onHostResume() {
        hostPaused = false
        if (visible) web?.onResume()
        // إعادة مزامنة الأعضاء مع آخر حالة من الخادم.
        lastApplied = -1L
        if (started && !released) scope.launch { refreshState() }
    }

    fun release() {
        if (released) return
        released = true
        controlsJob?.cancel()
        poll?.cancel()
        scope.cancel()
        dockAnim?.cancel()
        try { applyDockFraction(0f) } catch (_: Exception) {}
        try { searchDialog?.dismiss() } catch (_: Exception) {}
        searchDialog = null
        web?.let { wv ->
            try {
                wv.removeJavascriptInterface("BakWatch")
                (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                wv.destroy()
            } catch (_: Exception) {}
        }
        web = null
        panelWindow?.let { host.removeView(it) }
        panelWindow = null
    }

    // =====================================================================
    // Server state / sync
    // =====================================================================

    private suspend fun refreshState() {
        try {
            val result = withContext(Dispatchers.IO) {
                AuthApi.get("/api/voice-rooms/$roomId/watch/state", token)
            }
            if (released || result.status !in 200..299) return
            val owner = if (result.body.has("is_owner")) result.body.optBoolean("is_owner", false) else isOwnerHint()
            val roleChanged = owner != isOwner
            isOwner = owner
            if (roleChanged && visible) applyRoleUi()

            val st = result.body.optJSONObject("state") ?: JSONObject()
            val updated = st.optLong("updated_at", 0L)
            val id = st.optString("video_id", "")
            val validId = ID_REGEX.matches(id)
            val pb = st.optString("playback_state", "paused")
            val time = st.optDouble("current_time", 0.0).let { if (it.isNaN()) 0.0 else it }
            val title = st.optString("video_title", "")
            if (title.isNotBlank() && title != "null") videoTitle = title

            if (!initialSeen) {
                // أول قراءة بعد دخول الغرفة: نُظهر المشغل تلقائياً فقط إذا كان هناك فيديو يعمل الآن.
                initialSeen = true
                lastApplied = updated
                videoId = if (validId) id else ""
                desired = pb
                desiredTime = time
                if (validId && pb == "playing") {
                    show()
                    pushToPlayer()
                }
                return
            }

            if (updated > 0 && updated != lastApplied) {
                lastApplied = updated
                if (pb == "stopped" || !validId) {
                    desired = "stopped"
                    if (!isOwner && visible) hide(localDismiss = false)
                    return
                }
                val newVideo = id != videoId
                videoId = id
                desired = pb
                desiredTime = time
                if (isOwner) { updateTitle(); return } // المالك هو مصدر الحقيقة
                if (!visible) {
                    if (dismissedVideoId == id && !newVideo) return
                    if (dismissedVideoId != null && dismissedVideoId != id) dismissedVideoId = null
                    show()
                }
                updateTitle()
                pushToPlayer()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // نتجاهل أخطاء الشبكة المؤقتة، المحاولة التالية خلال ثوانٍ.
        }
    }

    private fun pushToPlayer() {
        val wv = web ?: return
        if (!playerReady || !ID_REGEX.matches(videoId)) return
        val state = if (desired == "playing") "playing" else "paused"
        val js = "applyRemoteState(" + JSONObject.quote(videoId) + "," + JSONObject.quote(state) + "," +
            desiredTime + "," + lastApplied + ")"
        wv.evaluateJavascript(js, null)
    }

    private suspend fun putState(state: String, time: Double, id: String, title: String): Boolean {
        return try {
            val payload = JSONObject()
                .put("video_id", id)
                .put("video_title", title)
                .put("playback_state", state)
                .put("current_time", time.coerceIn(0.0, 86400.0))
            val r = withContext(Dispatchers.IO) {
                AuthApi.put("/api/voice-rooms/$roomId/watch/state", payload, token)
            }
            r.status in 200..299
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private fun persist(state: String, time: Double, id: String = videoId, title: String = videoTitle) {
        if (!isOwner || !ID_REGEX.matches(id)) return
        scope.launch {
            val ok = putState(state, time, id, title)
            if (!ok) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastErrorToastAt > 8000) {
                    lastErrorToastAt = now
                    toast("تعذر مزامنة التشغيل مع الأعضاء")
                }
            }
        }
    }

    /** يُرسل حتى لو أُغلقت الشاشة/الغرفة مباشرة بعده (لا يرتبط بـ scope الخاص بالمشغل). */
    private fun persistDetached(state: String, time: Double) {
        val id = videoId
        val title = videoTitle
        if (!isOwner || !ID_REGEX.matches(id)) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { putState(state, time, id, title) }
    }

    /** المالك يغلق المشغل: نحاول إيقاف المشاهدة عند الجميع. */
    private fun stopForEveryone() {
        val id = videoId
        val title = videoTitle
        val time = curTime
        desired = "stopped"
        if (!isOwner || !ID_REGEX.matches(id)) return
        scope.launch {
            // "stopped" يخفي المشغل عند الأعضاء. إن رفضه الخادم نرجع إلى "paused" (قيمة مدعومة أصلاً).
            if (!putState("stopped", time, id, title)) putState("paused", time, id, title)
        }
    }

    // =====================================================================
    // Docked window: مكان مخصص فوق المقاعد (المقاعد تصغر قليلاً وتعود عند الإغلاق)
    // =====================================================================

    private var dockAnim: ValueAnimator? = null
    private var dockFraction = 0f
    private var dockHeightPx = 0

    private fun availWidth(): Int {
        if (host.width > 0) return host.width
        val p = host.parent as? View ?: return 0
        return p.width - p.paddingLeft - p.paddingRight
    }

    /** مقاس الفيديو 16:9 بحد أقصى للارتفاع حتى لا تضيق الدردشة على الشاشات الصغيرة. */
    private fun videoSize(): Pair<Int, Int> {
        val maxH = minOf(dp(MAX_VIDEO_DP), (activity.resources.displayMetrics.heightPixels * 0.30f).toInt())
        val availW = availWidth().coerceAtLeast(dp(220))
        var w = availW
        var h = w * 9 / 16
        if (h > maxH) {
            h = maxH
            w = h * 16 / 9
        }
        return w to h
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun ensureWindow(): LinearLayout {
        panelWindow?.let { return it }
        val (vw, vh) = videoSize()
        val dock = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            alpha = 0f
        }

        val box = FrameLayout(activity).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = rounded(Color.BLACK, 18, Color.argb(70, 255, 255, 255))
            clipToOutline = true
        }

        val wv = WebView(activity).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // الأعضاء لا يضغطون على الفيديو، لذلك يجب السماح بالتشغيل التلقائي.
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(Bridge(), "BakWatch")
            loadDataWithBaseURL("$EMBED_ORIGIN/", playerHtml(), "text/html", "UTF-8", null)
        }
        web = wv
        box.addView(wv, FrameLayout.LayoutParams(-1, -1))

        val empty = TextView(activity).apply {
            gravity = Gravity.CENTER
            textSize = 12.5f
            setTextColor(Color.argb(215, 255, 255, 255))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(44, 24, 82), Color.rgb(14, 10, 30), Color.rgb(8, 26, 48))
            )
        }
        emptyView = empty
        box.addView(empty, FrameLayout.LayoutParams(-1, -1))

        // طبقة شفافة: تمنع لمس الفيديو (الأعضاء لا يتحكمون) والضغط عليها يُظهر/يُخفي الأزرار.
        val shield = View(activity)
        shield.setOnClickListener { toggleControls() }
        box.addView(shield, FrameLayout.LayoutParams(-1, -1))

        val track = FrameLayout(activity).apply { setBackgroundColor(Color.argb(64, 255, 255, 255)) }
        val fillView = View(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(accentA, accentB)
            )
            pivotX = 0f
            scaleX = 0f
        }
        progressFill = fillView
        track.addView(fillView, FrameLayout.LayoutParams(-1, -1))
        box.addView(track, FrameLayout.LayoutParams(-1, dp(3), Gravity.BOTTOM))

        // ---- الشريط العلوي: [إغلاق] ......... [العنوان]
        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(205, 8, 4, 20), Color.TRANSPARENT)
            )
            setPadding(dp(7), dp(6), dp(12), dp(16))
        }
        val closeBtn = glyphButton(WatchGlyph.CLOSE) { onClosePressed() }
        val titleTv = TextView(activity).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(4f, 0f, 1f, Color.argb(170, 0, 0, 0))
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }
        top.addView(closeBtn, LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(8) })
        top.addView(titleTv, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(top, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        topBar = top
        titleView = titleTv

        // ---- الشريط السفلي (للمالك فقط)
        val bottom = FrameLayout(activity).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(Color.argb(205, 8, 4, 20), Color.TRANSPARENT)
            )
            setPadding(dp(8), dp(16), dp(8), dp(10))
        }
        val center = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val backB = glyphButton(WatchGlyph.BACK) { ownerSeek(-10) }
        val playB = glyphButton(WatchGlyph.PLAY, accent = true) { ownerToggle() }
        val fwdB = glyphButton(WatchGlyph.FWD) { ownerSeek(10) }
        center.addView(backB, LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(14) })
        center.addView(playB, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(14) })
        center.addView(fwdB, LinearLayout.LayoutParams(dp(34), dp(34)))
        bottom.addView(center, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        val searchB = glyphButton(WatchGlyph.SEARCH) { showSearch() }
        bottom.addView(searchB, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.END or Gravity.CENTER_VERTICAL))
        box.addView(bottom, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        bottomBar = bottom
        backBtn = backB
        playBtn = playB
        fwdBtn = fwdB
        searchBtn = searchB

        dock.addView(box, LinearLayout.LayoutParams(vw, vh))
        dock.addView(
            buildVolumeRow(),
            LinearLayout.LayoutParams(vw, dp(VOLUME_ROW_DP)).apply { topMargin = dp(VOLUME_GAP_DP) }
        )

        dockHeightPx = vh + dp(VOLUME_GAP_DP) + dp(VOLUME_ROW_DP)
        host.addView(dock, FrameLayout.LayoutParams(-1, dockHeightPx, Gravity.TOP))
        panelWindow = dock
        return dock
    }

    // ---- صف التحكم بالصوت (محلي لكل مستخدم، لا يُزامن مع الخادم) ----
    private fun buildVolumeRow(): LinearLayout {
        val accent = Color.rgb(255, 75, 90)
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(120, 46, 35, 78), Color.argb(105, 16, 12, 32))
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(60, 255, 255, 255))
            }
            setPadding(dp(6), 0, dp(10), 0)
        }
        val icon = WatchGlyph(activity, volumeGlyphKind()).apply {
            isClickable = true
            setOnClickListener { toggleMute() }
        }
        volumeIcon = icon
        row.addView(icon, LinearLayout.LayoutParams(dp(32), dp(32)))

        val seek = SeekBar(activity).apply {
            max = 100
            progress = volume
            setPadding(dp(10), 0, dp(10), 0)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(Color.argb(90, 255, 255, 255))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    volume = p
                    if (p > 0) lastVolume = p
                    refreshVolumeUi(updateSeek = false)
                    applyVolume()
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) { saveVolume() }
            })
        }
        volumeSeek = seek
        row.addView(seek, LinearLayout.LayoutParams(0, dp(32), 1f))

        val pct = TextView(activity).apply {
            textSize = 11f
            setTextColor(Color.argb(230, 255, 255, 255))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            text = "$volume%"
        }
        volumeText = pct
        row.addView(pct, LinearLayout.LayoutParams(dp(40), -1))
        return row
    }

    private fun volumeGlyphKind() = if (volume == 0) WatchGlyph.MUTE else WatchGlyph.VOLUME

    private fun refreshVolumeUi(updateSeek: Boolean) {
        volumeText?.text = "$volume%"
        volumeIcon?.let {
            val k = volumeGlyphKind()
            if (it.kind != k) {
                it.kind = k
                it.invalidate()
            }
        }
        if (updateSeek) volumeSeek?.progress = volume
    }

    private fun toggleMute() {
        if (volume > 0) {
            lastVolume = volume
            volume = 0
        } else {
            volume = lastVolume.coerceAtLeast(30)
        }
        refreshVolumeUi(updateSeek = true)
        applyVolume()
        saveVolume()
    }

    private fun applyVolume() {
        web?.evaluateJavascript("setVol($volume)", null)
    }

    private fun saveVolume() {
        prefs.edit().putInt(PREF_VOLUME, volume).putInt(PREF_VOLUME_LAST, lastVolume).apply()
    }

    private fun glyphButton(kind: Int, accent: Boolean = false, onClick: () -> Unit): WatchGlyph =
        WatchGlyph(activity, kind).apply {
            background = if (accent) {
                GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(accentA, accentB)).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dp(1), Color.argb(120, 255, 255, 255))
                }
            } else {
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.argb(150, 46, 35, 78), Color.argb(140, 10, 8, 22))
                ).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dp(1), Color.argb(80, 255, 255, 255))
                }
            }
            setOnClickListener {
                onClick()
                scheduleControlsHide()
            }
        }

    /**
     * f = 0 → المقاعد بحجمها الطبيعي والمكان المخصص مغلق.
     * f = 1 → المكان مفتوح والمقاعد مصغّرة قليلاً (تحت المشغل).
     */
    private fun applyDockFraction(f: Float) {
        dockFraction = f
        (host.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.height = (dockHeightPx * f).toInt()
            lp.topMargin = (dp(8) * f).toInt()
            host.layoutParams = lp
        }
        panelWindow?.alpha = ((f - 0.25f) / 0.75f).coerceIn(0f, 1f)
        anchorProvider()?.let { seats ->
            val s = 1f - (1f - SEAT_SCALE) * f
            seats.pivotX = seats.width / 2f
            seats.pivotY = 0f
            seats.scaleX = s
            seats.scaleY = s
            // التصغير بصري فقط؛ نسترجع المساحة الفارغة حتى ترتفع الدردشة بدل أن تبقى فجوة.
            val h = if (seats.height > 0) seats.height else dp(236)
            (seats.layoutParams as? ViewGroup.MarginLayoutParams)?.let { glp ->
                val mb = -(h * (1f - s)).toInt()
                if (glp.bottomMargin != mb) {
                    glp.bottomMargin = mb
                    seats.layoutParams = glp
                }
            }
        }
    }

    private fun animateDock(open: Boolean) {
        dockAnim?.cancel()
        val to = if (open) 1f else 0f
        dockAnim = ValueAnimator.ofFloat(dockFraction, to).apply {
            duration = 280
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { applyDockFraction(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(a: Animator) { cancelled = true }
                override fun onAnimationEnd(a: Animator) {
                    if (cancelled || released) return
                    if (!open && !visible) web?.onPause()
                }
            })
            start()
        }
    }

    private fun show() {
        if (released) return
        if (availWidth() == 0) {
            host.post { show() }
            return
        }
        ensureWindow()
        if (!visible) {
            visible = true
            web?.onResume()
            animateDock(open = true)
            showControls(true)
            scheduleControlsHide()
        }
        applyRoleUi()
        updateEmpty()
        updateTitle()
    }

    private fun hide(localDismiss: Boolean) {
        if (panelWindow == null || !visible) return
        visible = false
        if (localDismiss && videoId.isNotBlank()) dismissedVideoId = videoId
        web?.evaluateJavascript("if(ytPlayer&&ytPlayer.pauseVideo)ytPlayer.pauseVideo()", null)
        controlsJob?.cancel()
        animateDock(open = false)
    }

    private fun onClosePressed() {
        if (isOwner) {
            AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("إغلاق المشغل")
                .setMessage("سيتوقف الفيديو لدى جميع أعضاء الغرفة. هل تريد المتابعة؟")
                .setPositiveButton("إغلاق") { _, _ ->
                    stopForEveryone()
                    hide(localDismiss = false)
                }
                .setNegativeButton("إلغاء", null)
                .show()
        } else {
            hide(localDismiss = true)
        }
    }

    private fun applyRoleUi() {
        val bar = bottomBar
        if (bar != null) {
            if (!isOwner) bar.visibility = View.GONE
            else if (controlsShown) bar.visibility = View.VISIBLE
        }
        updatePlayPauseIcon()
        updateEmpty()
    }

    private fun updateEmpty() {
        val tv = emptyView ?: return
        if (ID_REGEX.matches(videoId)) {
            tv.visibility = View.GONE
        } else {
            tv.visibility = View.VISIBLE
            tv.text = if (isOwner) "اضغط على البحث لاختيار فيديو" else "بانتظار صاحب الغرفة لاختيار الفيديو"
        }
    }

    private fun updateTitle() {
        titleView?.text = if (videoTitle.isNotBlank()) videoTitle else "المشاهدة الجماعية"
    }

    private fun updatePlayPauseIcon() {
        val b = playBtn ?: return
        val k = if (playerCode == 1 || playerCode == 3) WatchGlyph.PAUSE else WatchGlyph.PLAY
        if (b.kind != k) {
            b.kind = k
            b.invalidate()
        }
    }

    private fun toggleControls() = if (controlsShown) showControls(false) else {
        showControls(true)
        scheduleControlsHide()
    }

    private fun showControls(show: Boolean) {
        controlsShown = show
        val bars = listOfNotNull(topBar, if (isOwner) bottomBar else null)
        for (v in bars) {
            if (show) {
                v.visibility = View.VISIBLE
                v.animate().alpha(1f).setDuration(140).start()
            } else {
                v.animate().alpha(0f).setDuration(180).withEndAction {
                    if (!controlsShown) v.visibility = View.INVISIBLE
                }.start()
            }
        }
    }

    private fun scheduleControlsHide() {
        controlsJob?.cancel()
        controlsJob = scope.launch {
            delay(3200)
            if (visible) showControls(false)
        }
    }

    // =====================================================================
    // Owner controls
    // =====================================================================
    private fun ownerToggle() {
        if (!isOwner) return
        web?.evaluateJavascript("ownerToggle()", null)
    }

    private fun ownerSeek(delta: Int) {
        if (!isOwner) return
        web?.evaluateJavascript("ownerSeek($delta)", null)
    }

    private fun selectVideo(id: String, title: String) {
        if (!isOwner || !ID_REGEX.matches(id)) return
        videoId = id
        videoTitle = title
        desired = "playing"
        desiredTime = 0.0
        curTime = 0.0
        dismissedVideoId = null
        lastHeartbeat = SystemClock.elapsedRealtime()
        show()
        updateEmpty()
        updateTitle()
        if (playerReady) web?.evaluateJavascript("ownerLoad(" + JSONObject.quote(id) + ")", null)
        persist("playing", 0.0, id, title)
    }

    // =====================================================================
    // JS bridge
    // =====================================================================

    private inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            activity.runOnUiThread {
                if (released) return@runOnUiThread
                playerReady = true
                applyVolume()
                if (visible) pushToPlayer()
            }
        }

        @JavascriptInterface
        fun onCommand(action: String, seconds: Double) {
            activity.runOnUiThread {
                if (released) return@runOnUiThread
                lastHeartbeat = SystemClock.elapsedRealtime()
                desired = action
                desiredTime = seconds
                curTime = seconds
                persist(action, seconds)
            }
        }

        @JavascriptInterface
        fun onPlayerState(state: Int, seconds: Double) {
            activity.runOnUiThread {
                if (released) return@runOnUiThread
                playerCode = state
                updatePlayPauseIcon()
                if (state == 1) applyVolume() // المشغل أحياناً يعيد الصوت الافتراضي عند بدء فيديو جديد
                if (state == 0 && isOwner && visible) {
                    desired = "paused"
                    persist("paused", seconds)
                }
            }
        }

        @JavascriptInterface
        fun onTick(seconds: Double, duration: Double, state: Int) {
            activity.runOnUiThread {
                if (released) return@runOnUiThread
                playerCode = state
                curTime = seconds
                progressFill?.scaleX = if (duration > 0) (seconds / duration).toFloat().coerceIn(0f, 1f) else 0f
                updatePlayPauseIcon()
                val now = SystemClock.elapsedRealtime()
                if (isOwner) {
                    // نبض مزامنة: يصحح انحراف الأعضاء ويُدخل من انضم متأخراً في الوقت الصحيح.
                    if (state == 1 && now - lastHeartbeat >= HEARTBEAT_MS) {
                        lastHeartbeat = now
                        desired = "playing"
                        persist("playing", seconds)
                    }
                } else if (visible && now - lastKick > 3000) {
                    // العضو لا يتحكم؛ إن توقف مشغله من النظام نعيده لحالة المالك.
                    if (desired == "playing" && (state == 2 || state == 5 || state == -1)) {
                        lastKick = now
                        web?.evaluateJavascript("if(ytPlayer&&ytPlayer.playVideo)ytPlayer.playVideo()", null)
                    } else if (desired != "playing" && state == 1) {
                        lastKick = now
                        web?.evaluateJavascript("if(ytPlayer&&ytPlayer.pauseVideo)ytPlayer.pauseVideo()", null)
                    }
                }
            }
        }
    }

    private fun playerHtml(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"><meta name="referrer" content="origin">
        <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
        <body><div id="player"></div>
        <script>
        var ytPlayer=null, pending=null, tickTimer=null;
        function onYouTubeIframeAPIReady(){
          ytPlayer=new YT.Player('player',{width:'100%',height:'100%',videoId:'',
            playerVars:{playsinline:1,rel:0,modestbranding:1,enablejsapi:1,controls:0,disablekb:1,fs:0,iv_load_policy:3,origin:'$EMBED_ORIGIN'},
            events:{
              onReady:function(){
                BakWatch.onReady();
                startTick();
                if(pending){var p=pending;pending=null;if(p.owner){ownerLoad(p.id);}else{applyRemoteState(p.id,p.state,p.time,p.updated);}}
              },
              onStateChange:function(e){
                var t=0;try{t=ytPlayer.getCurrentTime()||0;}catch(x){}
                BakWatch.onPlayerState(e.data,t);
              }
            }});
        }
        function startTick(){
          if(tickTimer)return;
          tickTimer=setInterval(function(){
            if(!ytPlayer||!ytPlayer.getCurrentTime)return;
            var t=0,d=0,s=-1;
            try{t=ytPlayer.getCurrentTime()||0;d=ytPlayer.getDuration()||0;s=ytPlayer.getPlayerState();}catch(e){}
            BakWatch.onTick(t,d,s);
          },1000);
        }
        function applyRemoteState(id,state,time,updated){
          if(!ytPlayer || !ytPlayer.loadVideoById){pending={id:id,state:state,time:time,updated:updated};return;}
          var current='';
          try{current=ytPlayer.getVideoData().video_id||'';}catch(e){}
          var start=Math.max(0,time||0);
          if(id && current!==id){
            if(state==='playing'){ytPlayer.loadVideoById({videoId:id,startSeconds:start});}
            else{ytPlayer.cueVideoById({videoId:id,startSeconds:start});}
            return;
          }
          if(id && Math.abs((ytPlayer.getCurrentTime()||0)-start)>3.5){ytPlayer.seekTo(start,true);}
          if(state==='playing') ytPlayer.playVideo(); else ytPlayer.pauseVideo();
        }
        function setVol(v){
          if(!ytPlayer||!ytPlayer.setVolume)return;
          try{
            v=Math.max(0,Math.min(100,v|0));
            ytPlayer.setVolume(v);
            if(v===0){ytPlayer.mute();}
            else if(ytPlayer.isMuted&&ytPlayer.isMuted()){ytPlayer.unMute();}
          }catch(e){}
        }
        function ownerLoad(id){
          if(!ytPlayer||!ytPlayer.loadVideoById){pending={id:id,owner:true};return;}
          ytPlayer.loadVideoById(id);
        }
        function ownerToggle(){
          if(!ytPlayer)return;
          var s=ytPlayer.getPlayerState();
          if(s===1||s===3){ytPlayer.pauseVideo();BakWatch.onCommand('paused',ytPlayer.getCurrentTime()||0);}
          else{ytPlayer.playVideo();BakWatch.onCommand('playing',ytPlayer.getCurrentTime()||0);}
        }
        function ownerSeek(delta){
          if(!ytPlayer)return;
          var t=Math.max(0,(ytPlayer.getCurrentTime()||0)+delta);
          ytPlayer.seekTo(t,true);
          var s=ytPlayer.getPlayerState();
          BakWatch.onCommand((s===1||s===3)?'playing':'paused',t);
        }
        var tag=document.createElement('script');tag.src='https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
        </script></body></html>
    """.trimIndent()

    // =====================================================================
    // Search sheet (للمالك فقط)
    // =====================================================================

    @SuppressLint("SetTextI18n")
    private fun showSearch() {
        if (!isOwner || released || activity.isFinishing) return
        searchDialog?.dismiss()
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val muted = Color.argb(160, 255, 255, 255)
        val accent = Color.rgb(255, 75, 90)

        fun tv(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(activity).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(10), dp(16), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(40, 29, 70), Color.rgb(20, 15, 38), Color.rgb(12, 10, 24))
            ).apply {
                val r = dp(30).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke(dp(1), Color.argb(64, 255, 255, 255))
            }
        }
        root.addView(View(activity).apply {
            background = rounded(Color.argb(90, 255, 255, 255), 3)
        }, LinearLayout.LayoutParams(dp(40), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(12)
        })
        root.addView(tv("اختيار فيديو للمشاهدة الجماعية", 16f, Color.WHITE, true), LinearLayout.LayoutParams(-1, dp(34)))

        val searchCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(64, 255, 255, 255), Color.argb(30, 255, 255, 255))
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.argb(60, 255, 255, 255))
            }
            setPadding(dp(8), dp(5), dp(8), dp(5))
        }
        val input = EditText(activity).apply {
            hint = "ابحث عن فيديو أو أغنية"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(12), 0, dp(12), 0)
            background = null
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        searchCard.addView(input, LinearLayout.LayoutParams(0, dp(46), 1f))
        val goBtn = tv("بحث", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = accentGradient(14)
            setPadding(dp(18), 0, dp(18), 0)
        }
        searchCard.addView(goBtn, LinearLayout.LayoutParams(-2, dp(42)))
        root.addView(searchCard, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(6); bottomMargin = dp(8) })

        val status = tv("اكتب اسم الفيديو ثم اضغط بحث", 11f, muted).apply { gravity = Gravity.CENTER }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(26)))

        val results = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(6))
        }
        val scroll = ScrollView(activity).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        scroll.addView(results)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun addResult(item: JSONObject) {
            val id = item.optString("video_id")
            if (!ID_REGEX.matches(id)) return
            val title = item.optString("title", "فيديو")
            val channel = item.optString("channel", "")
            val thumb = item.optString("thumbnail", "")
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.argb(50, 255, 255, 255), Color.argb(22, 255, 255, 255))
                ).apply {
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.argb(50, 255, 255, 255))
                }
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                setOnClickListener {
                    dlg.dismiss()
                    selectVideo(id, title)
                }
            }
            val image = ImageView(activity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(Color.rgb(42, 47, 60), 9)
                clipToOutline = true
            }
            row.addView(image, LinearLayout.LayoutParams(dp(112), dp(66)).apply { marginEnd = dp(10) })
            val labels = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            labels.addView(tv(title, 12f, Color.WHITE, true).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            }, LinearLayout.LayoutParams(-1, -2))
            labels.addView(tv(channel, 10f, muted).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            }, LinearLayout.LayoutParams(-1, dp(20)))
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            results.addView(row, LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(7) })

            if (thumb.startsWith("https://")) {
                val cached = thumbCache[thumb]
                if (cached != null) {
                    image.setImageBitmap(cached)
                } else {
                    scope.launch {
                        val bmp = withContext(Dispatchers.IO) {
                            try { URL(thumb).openStream().use { BitmapFactory.decodeStream(it) } }
                            catch (_: Exception) { null }
                        }
                        if (bmp != null) {
                            thumbCache[thumb] = bmp
                            if (dlg.isShowing) image.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }

        fun runSearch() {
            val q = input.text.toString().trim()
            if (q.length < 2) {
                input.error = "اكتب حرفين على الأقل"
                return
            }
            status.text = "جارٍ البحث في YouTube..."
            results.removeAllViews()
            scope.launch {
                try {
                    val encoded = URLEncoder.encode(q, "UTF-8")
                    val r = withContext(Dispatchers.IO) {
                        AuthApi.get("/api/watch/youtube/search?q=$encoded", token)
                    }
                    if (r.status !in 200..299) throw Exception(r.body.optString("error", "فشل البحث"))
                    val items = r.body.optJSONArray("results") ?: JSONArray()
                    if (items.length() == 0) {
                        status.text = "لم نعثر على نتائج"
                        return@launch
                    }
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        addResult(item)
                    }
                    status.text = "اختر فيديو لبدء المشاهدة"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    status.text = e.message ?: "تعذر البحث حالياً"
                }
            }
        }
        goBtn.setOnClickListener { runSearch() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { runSearch(); true } else false
        }

        dlg.setContentView(root)
        dlg.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.62f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            setGravity(Gravity.BOTTOM)
        }
        dlg.show()
        dlg.window?.setLayout(-1, (activity.resources.displayMetrics.heightPixels * 0.8f).toInt())
        searchDialog = dlg
    }
}

/** أيقونات المشغل مرسومة برمجياً (بدون الحاجة لأي drawables جديدة). */
private class WatchGlyph(context: Context, var kind: Int) : View(context) {
    companion object {
        const val CLOSE = 0
        const val EXPAND = 1
        const val SHRINK = 2
        const val PLAY = 3
        const val PAUSE = 4
        const val BACK = 5
        const val FWD = 6
        const val SEARCH = 7
        const val VOLUME = 8
        const val MUTE = 9
    }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val s = minOf(w, h) * 0.30f
        stroke.strokeWidth = minOf(w, h) * 0.075f
        when (kind) {
            CLOSE -> {
                c.drawLine(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy + s * 0.8f, stroke)
                c.drawLine(cx - s * 0.8f, cy + s * 0.8f, cx + s * 0.8f, cy - s * 0.8f, stroke)
            }
            EXPAND -> {
                val a = s * 0.85f
                val k = s * 0.6f
                c.drawLine(cx - a, cy + a, cx + a, cy - a, stroke)
                path.reset()
                path.moveTo(cx + a - k, cy - a); path.lineTo(cx + a, cy - a); path.lineTo(cx + a, cy - a + k)
                path.moveTo(cx - a + k, cy + a); path.lineTo(cx - a, cy + a); path.lineTo(cx - a, cy + a - k)
                c.drawPath(path, stroke)
            }
            SHRINK -> {
                val a = s * 0.85f
                val d = s * 0.12f
                val k = s * 0.6f
                c.drawLine(cx - a, cy + a, cx + a, cy - a, stroke)
                path.reset()
                path.moveTo(cx + d + k, cy - d); path.lineTo(cx + d, cy - d); path.lineTo(cx + d, cy - d - k)
                path.moveTo(cx - d - k, cy + d); path.lineTo(cx - d, cy + d); path.lineTo(cx - d, cy + d + k)
                c.drawPath(path, stroke)
            }
            PLAY -> {
                path.reset()
                path.moveTo(cx - s * 0.55f, cy - s * 0.8f)
                path.lineTo(cx - s * 0.55f, cy + s * 0.8f)
                path.lineTo(cx + s * 0.85f, cy)
                path.close()
                c.drawPath(path, fill)
            }
            PAUSE -> {
                val r = s * 0.12f
                c.drawRoundRect(RectF(cx - s * 0.7f, cy - s * 0.8f, cx - s * 0.2f, cy + s * 0.8f), r, r, fill)
                c.drawRoundRect(RectF(cx + s * 0.2f, cy - s * 0.8f, cx + s * 0.7f, cy + s * 0.8f), r, r, fill)
            }
            BACK, FWD -> {
                val r = s * 0.95f
                val oval = RectF(cx - r, cy - r, cx + r, cy + r)
                val hs = s * 0.55f
                path.reset()
                if (kind == BACK) {
                    c.drawArc(oval, -70f, -290f, false, stroke)
                    // رأس السهم عند نهاية القوس (يمين) متجهاً للأعلى
                    val ex = cx + r
                    val ey = cy
                    path.moveTo(ex, ey - hs)
                    path.lineTo(ex - hs * 0.8f, ey + hs * 0.2f)
                    path.lineTo(ex + hs * 0.8f, ey + hs * 0.2f)
                } else {
                    c.drawArc(oval, -110f, 290f, false, stroke)
                    val ex = cx - r
                    val ey = cy
                    path.moveTo(ex, ey - hs)
                    path.lineTo(ex - hs * 0.8f, ey + hs * 0.2f)
                    path.lineTo(ex + hs * 0.8f, ey + hs * 0.2f)
                }
                path.close()
                c.drawPath(path, fill)
                text.textSize = s * 0.95f
                c.drawText("10", cx, cy + s * 0.33f, text)
            }
            VOLUME, MUTE -> {
                val ox = cx - s * 0.15f
                path.reset()
                path.moveTo(ox - s * 0.95f, cy - s * 0.42f)
                path.lineTo(ox - s * 0.4f, cy - s * 0.42f)
                path.lineTo(ox + s * 0.2f, cy - s * 0.9f)
                path.lineTo(ox + s * 0.2f, cy + s * 0.9f)
                path.lineTo(ox - s * 0.4f, cy + s * 0.42f)
                path.lineTo(ox - s * 0.95f, cy + s * 0.42f)
                path.close()
                c.drawPath(path, fill)
                if (kind == VOLUME) {
                    val r1 = s * 0.55f
                    val r2 = s * 1.0f
                    c.drawArc(RectF(ox + s * 0.2f - r1, cy - r1, ox + s * 0.2f + r1, cy + r1), -50f, 100f, false, stroke)
                    c.drawArc(RectF(ox + s * 0.2f - r2, cy - r2, ox + s * 0.2f + r2, cy + r2), -50f, 100f, false, stroke)
                } else {
                    val x0 = ox + s * 0.55f
                    val x1 = ox + s * 1.2f
                    c.drawLine(x0, cy - s * 0.4f, x1, cy + s * 0.4f, stroke)
                    c.drawLine(x0, cy + s * 0.4f, x1, cy - s * 0.4f, stroke)
                }
            }
            SEARCH -> {
                val r = s * 0.62f
                c.drawCircle(cx - s * 0.18f, cy - s * 0.18f, r, stroke)
                c.drawLine(cx + s * 0.28f, cy + s * 0.28f, cx + s * 0.9f, cy + s * 0.9f, stroke)
            }
        }
    }
}