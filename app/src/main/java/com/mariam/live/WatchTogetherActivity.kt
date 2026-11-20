package com.mariam.live

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.*

class WatchTogetherActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var player: WebView
    private lateinit var searchInput: EditText
    private lateinit var status: TextView
    private lateinit var playButton: TextView
    private lateinit var pauseButton: TextView
    private lateinit var resultsContainer: LinearLayout
    private lateinit var selectedTitle: TextView
    private var lastVideoTitle = ""
    private var roomId = ""
    private var token = ""
    private var isOwner = false
    private var playerReady = false
    private var lastAppliedUpdate = -1L
    private var lastVideoId = ""
    private var polling: Job? = null

    private val bg = Color.rgb(12, 15, 24)
    private val panel = Color.rgb(25, 30, 43)
    private val muted = Color.rgb(166, 174, 190)
    private val accent = Color.rgb(232, 62, 91)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = intent.getStringExtra("room_id").orEmpty()
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (roomId.isBlank() || token.isBlank()) {
            finish()
            return
        }

        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = text("‹", 32f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(panel, 14)
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(44), dp(42)).apply { marginEnd = dp(10) })
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(text("المشاهدة الجماعية", 19f, Color.WHITE, true))
        titles.addView(text("شاهدوا الفيديو معاً داخل غرفة BAK LIVE", 11f, muted))
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        val live = text("● مباشر", 11f, Color.rgb(115, 225, 165), true).apply {
            background = rounded(Color.rgb(28, 62, 51), 18)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        header.addView(live)
        root.addView(header, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(12) })

        val roomHint = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(28, 37, 51), 13)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val roomDot = View(this).apply {
            background = rounded(Color.rgb(85, 205, 145), 20)
        }
        roomHint.addView(roomDot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(8) })
        roomHint.addView(text("صوت الغرفة مستمر أثناء المشاهدة", 11f, Color.rgb(210, 219, 232)))
        root.addView(roomHint, LinearLayout.LayoutParams(-1, dp(36)).apply { bottomMargin = dp(12) })

        val searchCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 15)
            setPadding(dp(8), dp(5), dp(8), dp(5))
        }
        searchInput = EditText(this).apply {
            hint = "ابحث عن فيديو أو أغنية"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(12), 0, dp(12), 0)
            background = null
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        searchCard.addView(searchInput, LinearLayout.LayoutParams(0, dp(46), 1f))
        val searchButton = text("بحث", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 12)
            setPadding(dp(18), 0, dp(18), 0)
            setOnClickListener { searchYouTube() }
        }
        searchCard.addView(searchButton, LinearLayout.LayoutParams(-2, dp(42)))
        root.addView(searchCard, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(12) })

        val playerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.BLACK, 17, Color.rgb(49, 55, 70))
            clipToOutline = true
        }
        player = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(PlayerBridge(), "BakWatch")
            loadDataWithBaseURL("https://nokiraq.duckdns.org/", playerHtml(), "text/html", "UTF-8", null)
        }
        playerCard.addView(player, LinearLayout.LayoutParams(-1, dp(205)))
        selectedTitle = text("اختر فيديو لبدء المشاهدة", 12f, Color.WHITE, true).apply {
            setPadding(dp(12), dp(9), dp(12), dp(9))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        playerCard.addView(selectedTitle, LinearLayout.LayoutParams(-1, dp(38)))
        root.addView(playerCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        playButton = control("▶  تشغيل", true) { sendOwnerCommand("playing") }
        pauseButton = control("Ⅱ  إيقاف مؤقت", false) { sendOwnerCommand("paused") }
        controls.addView(playButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(6) })
        controls.addView(pauseButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(6) })
        root.addView(controls, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        status = text("جارٍ الاتصال بالغرفة...", 11f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2))

        val resultsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        resultsHeader.addView(text("نتائج البحث", 14f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(30), 1f))
        resultsHeader.addView(text("اختر فيديو للمشاهدة", 10f, muted))
        root.addView(resultsHeader)

        val resultsScroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(6))
        }
        resultsScroll.addView(resultsContainer)
        root.addView(resultsScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val close = text("العودة إلى الغرفة", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(49, 56, 71), 13)
            setOnClickListener { finish() }
        }
        root.addView(close, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) })
        setContentView(root)

        scope.launch {
            loadState()
            polling = launch {
                while (isActive) {
                    delay(2200)
                    loadState()
                }
            }
        }
    }

    private fun control(label: String, primary: Boolean, action: () -> Unit) =
        text(label, 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(if (primary) accent else Color.rgb(48, 55, 69), 13)
            setOnClickListener { action() }
            isEnabled = false
            alpha = 0.48f
        }

    private fun setControlEnabled(view: TextView, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.48f
    }

    private fun playerHtml(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"><meta name="referrer" content="origin">
        <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
        <body><div id="player"></div>
        <script>
        var ytPlayer=null, pending=null;
        function onYouTubeIframeAPIReady(){
          ytPlayer=new YT.Player('player',{width:'100%',height:'100%',videoId:'',
            playerVars:{playsinline:1,rel:0,modestbranding:1,enablejsapi:1,origin:'https://nokiraq.duckdns.org'},
            events:{onReady:function(){BakWatch.onReady();if(pending)applyRemoteState(pending.id,pending.state,pending.time,pending.updated);}}});
        }
        function applyRemoteState(id,state,time,updated){
          if(!ytPlayer || !ytPlayer.loadVideoById){pending={id:id,state:state,time:time,updated:updated};return;}
          var current='';
          try{current=ytPlayer.getVideoData().video_id||'';}catch(e){}
          if(id && current!==id){ytPlayer.loadVideoById({videoId:id,startSeconds:Math.max(0,time||0)});}
          else if(id && Math.abs((ytPlayer.getCurrentTime()||0)-(time||0))>4){ytPlayer.seekTo(Math.max(0,time||0),true);}
          if(state==='playing') ytPlayer.playVideo(); else ytPlayer.pauseVideo();
          pending=null;
        }
        function ownerPlay(){if(ytPlayer){ytPlayer.playVideo();BakWatch.onCommand('playing',ytPlayer.getCurrentTime()||0);}}
        function ownerPause(){if(ytPlayer){ytPlayer.pauseVideo();BakWatch.onCommand('paused',ytPlayer.getCurrentTime()||0);}}
        function setVideo(id){if(ytPlayer){ytPlayer.cueVideoById(id);BakWatch.onVideoSelected(id);}}
        var tag=document.createElement('script');tag.src='https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
        </script></body></html>
    """.trimIndent()

    private inner class PlayerBridge {
        @JavascriptInterface fun onReady() {
            runOnUiThread {
                playerReady = true
                status.text = if (isOwner) "أنت صاحب الغرفة: اختر الفيديو وتحكم بالتشغيل" else "المشاهدة متزامنة مع صاحب الغرفة"
                setControlEnabled(playButton, isOwner)
                setControlEnabled(pauseButton, isOwner)
            }
        }
        @JavascriptInterface fun onCommand(action: String, seconds: Double) {
            runOnUiThread { persistState(action, seconds, lastVideoId, lastVideoTitle) }
        }
        @JavascriptInterface fun onVideoSelected(videoId: String) {
            runOnUiThread {
                lastVideoId = videoId
                persistState("paused", 0.0, videoId, lastVideoTitle)
            }
        }
    }

    private fun loadState() {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    AuthApi.get("/api/voice-rooms/$roomId/watch/state", token)
                }
                if (result.status !in 200..299) {
                    status.text = result.body.optString("error", "تعذر تحميل حالة المشاهدة")
                    return@launch
                }
                isOwner = result.body.optBoolean("is_owner", false)
                setControlEnabled(playButton, isOwner && playerReady)
                setControlEnabled(pauseButton, isOwner && playerReady)
                val state = result.body.optJSONObject("state") ?: JSONObject()
                val updated = state.optLong("updated_at", 0L)
                val videoId = state.optString("video_id", "")
                lastVideoTitle = state.optString("video_title", lastVideoTitle)
                if (lastVideoTitle.isNotBlank()) selectedTitle.text = lastVideoTitle
                if (updated > 0 && updated != lastAppliedUpdate) {
                    lastAppliedUpdate = updated
                    lastVideoId = videoId
                    if (playerReady && videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) {
                        val action = "applyRemoteState(" +
                            JSONObject.quote(videoId) + "," +
                            JSONObject.quote(state.optString("playback_state", "paused")) + "," +
                            state.optDouble("current_time", 0.0) + "," + updated + ")"
                        player.evaluateJavascript(action, null)
                    }
                }
                if (videoId.isBlank()) status.text = if (isOwner) "ابحث واختر فيديو لبدء المشاهدة" else "بانتظار اختيار صاحب الغرفة للفيديو"
            } catch (_: Exception) {
                status.text = "تعذر الاتصال بخدمة المشاهدة"
            }
        }
    }

    private fun searchYouTube() {
        if (!isOwner) {
            Toast.makeText(this, "اختيار الفيديو متاح لصاحب الغرفة فقط", Toast.LENGTH_SHORT).show()
            return
        }
        val query = searchInput.text.toString().trim()
        if (query.length < 2) {
            searchInput.error = "اكتب كلمتين على الأقل"
            return
        }
        scope.launch {
            try {
                status.text = "جارٍ البحث في YouTube..."
                resultsContainer.removeAllViews()
                val encoded = URLEncoder.encode(query, "UTF-8")
                val result = withContext(Dispatchers.IO) {
                    AuthApi.get("/api/watch/youtube/search?q=$encoded", token)
                }
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "فشل البحث"))
                val items = result.body.optJSONArray("results") ?: JSONArray()
                if (items.length() == 0) {
                    status.text = "لم نعثر على نتائج"
                    return@launch
                }
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    addSearchResult(item)
                }
                status.text = "اختر فيديو من النتائج لبدء المشاهدة"
            } catch (error: Exception) {
                status.text = error.message ?: "تعذر البحث حالياً"
            }
        }
    }

    private fun addSearchResult(item: JSONObject) {
        val id = item.optString("video_id")
        if (!id.matches(Regex("[A-Za-z0-9_-]{11}"))) return
        val title = item.optString("title", "فيديو")
        val channel = item.optString("channel", "")
        val thumbnail = item.optString("thumbnail", "")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = rounded(panel, 13, Color.rgb(48, 55, 70))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            setOnClickListener { selectVideo(id, title) }
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(42, 47, 60), 9)
        }
        row.addView(image, LinearLayout.LayoutParams(dp(112), dp(66)).apply { marginEnd = dp(10) })
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        labels.addView(text(title, 12f, Color.WHITE, true).apply {
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }, LinearLayout.LayoutParams(-1, -2))
        labels.addView(text(channel, 10f, muted).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }, LinearLayout.LayoutParams(-1, dp(20)))
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        resultsContainer.addView(row, LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(7) })

        if (thumbnail.startsWith("https://")) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try { URL(thumbnail).openStream().use { BitmapFactory.decodeStream(it) } }
                    catch (_: Exception) { null }
                }
                if (bitmap != null && !isFinishing) image.setImageBitmap(bitmap)
            }
        }
    }

    private fun selectVideo(id: String, title: String) {
        lastVideoId = id
        lastVideoTitle = title
        selectedTitle.text = title
        player.evaluateJavascript("setVideo(" + JSONObject.quote(id) + ")", null)
        persistState("paused", 0.0, id, title)
        status.text = "تم اختيار الفيديو — اضغط تشغيل"
    }

    private fun sendOwnerCommand(action: String) {
        if (!isOwner) return
        val js = if (action == "playing") "ownerPlay()" else "ownerPause()"
        player.evaluateJavascript(js, null)
    }

    private fun persistState(action: String, seconds: Double, videoId: String, title: String) {
        if (!isOwner || videoId.isBlank()) return
        scope.launch {
            try {
                val payload = JSONObject()
                    .put("video_id", videoId)
                    .put("video_title", title)
                    .put("playback_state", action)
                    .put("current_time", seconds.coerceIn(0.0, 86400.0))
                val result = withContext(Dispatchers.IO) {
                    AuthApi.put("/api/voice-rooms/$roomId/watch/state", payload, token)
                }
                if (result.status !in 200..299) status.text = result.body.optString("error", "تعذر مزامنة التشغيل")
            } catch (_: Exception) {
                status.text = "تعذر الاتصال بالخادم لمزامنة التشغيل"
            }
        }
    }

    override fun onDestroy() {
        polling?.cancel()
        scope.cancel()
        if (::player.isInitialized) {
            player.removeJavascriptInterface("BakWatch")
            player.destroy()
        }
        super.onDestroy()
    }
}
