package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlinx.coroutines.*

class WatchTogetherActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var player: WebView
    private lateinit var searchInput: EditText
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private var lastVideoTitle = ""
    private var roomId = ""
    private var token = ""
    private var isOwner = false
    private var playerReady = false
    private var lastAppliedUpdate = -1L
    private var lastVideoId = ""
    private var polling: Job? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = intent.getStringExtra("room_id").orEmpty()
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (roomId.isBlank() || token.isBlank()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.rgb(13, 16, 25))
        }
        val heading = TextView(this).apply {
            text = "المشاهدة الجماعية"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(heading, LinearLayout.LayoutParams(-1, -2))

        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        searchInput = EditText(this).apply {
            hint = "ابحث عن فيديو في YouTube"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.rgb(35, 40, 54))
        }
        searchRow.addView(searchInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        val searchButton = Button(this).apply {
            text = "بحث"
            setOnClickListener { searchYouTube() }
        }
        searchRow.addView(searchButton, LinearLayout.LayoutParams(dp(82), dp(48)).apply { marginStart = dp(8) })
        root.addView(searchRow, LinearLayout.LayoutParams(-1, -2))

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
        root.addView(player, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(12); bottomMargin = dp(10) })

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        playButton = Button(this).apply {
            text = "تشغيل"
            isEnabled = false
            setOnClickListener { sendOwnerCommand("playing") }
        }
        controls.addView(playButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        pauseButton = Button(this).apply {
            text = "إيقاف"
            isEnabled = false
            setOnClickListener { sendOwnerCommand("paused") }
        }
        pauseButton.isEnabled = false
        controls.addView(pauseButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
        root.addView(controls, LinearLayout.LayoutParams(-1, -2))

        status = TextView(this).apply {
            text = "جارٍ الاتصال بالغرفة..."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2))
        val close = Button(this).apply { text = "العودة إلى الغرفة"; setOnClickListener { finish() } }
        root.addView(close, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(6) })
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
        function currentTime(){return ytPlayer?(ytPlayer.getCurrentTime()||0):0;}
        var tag=document.createElement('script');tag.src='https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
        </script></body></html>
    """.trimIndent()

    private inner class PlayerBridge {
        @JavascriptInterface fun onReady() { runOnUiThread { playerReady = true; status.text = if (isOwner) "أنت صاحب الغرفة: يمكنك اختيار الفيديو والتحكم بالتشغيل" else "المشاهدة متزامنة مع صاحب الغرفة"; playButton.isEnabled = isOwner; pauseButton.isEnabled = isOwner } }
        @JavascriptInterface fun onCommand(action: String, seconds: Double) { runOnUiThread { persistState(action, seconds, lastVideoId, lastVideoTitle) } }
        @JavascriptInterface fun onVideoSelected(videoId: String) { runOnUiThread { lastVideoId = videoId; persistState("paused", 0.0, videoId, lastVideoTitle) } }
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
                playButton.isEnabled = isOwner && playerReady
                pauseButton.isEnabled = isOwner && playerReady
                val state = result.body.optJSONObject("state") ?: JSONObject()
                val updated = state.optLong("updated_at", 0L)
                val videoId = state.optString("video_id", "")
                lastVideoTitle = state.optString("video_title", lastVideoTitle)
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
                val labels = Array(items.length()) { i ->
                    val item = items.optJSONObject(i) ?: JSONObject()
                    item.optString("title", "فيديو") + "\n" + item.optString("channel", "")
                }
                AlertDialog.Builder(this@WatchTogetherActivity)
                    .setTitle("اختر فيديو للمشاهدة")
                    .setItems(labels) { _, which ->
                        val selected = items.optJSONObject(which) ?: return@setItems
                        val id = selected.optString("video_id")
                        val title = selected.optString("title")
                        if (id.matches(Regex("[A-Za-z0-9_-]{11}"))) {
                            lastVideoId = id
                            lastVideoTitle = title
                            player.evaluateJavascript("setVideo(" + JSONObject.quote(id) + ")", null)
                            persistState("paused", 0.0, id, title)
                            status.text = title
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            } catch (error: Exception) {
                status.text = error.message ?: "تعذر البحث"
            }
        }
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
