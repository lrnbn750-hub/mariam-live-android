package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import livekit.org.webrtc.RendererCommon
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** Admin-only, subscribe-only live review. No microphone/camera permission or publish calls. */
class AdminLiveMonitorActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var room: Room
    private lateinit var videoHost: FrameLayout
    private lateinit var status: TextView
    private lateinit var adminToken: String
    private lateinit var sessionId: String
    private lateinit var roomId: String
    private var serverUrl = ""
    private var participantToken = ""
    private var renderer: TextureViewRenderer? = null
    private var currentVideo: VideoTrack? = null
    private var ending = false
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminToken = intent.getStringExtra("admin_token").orEmpty()
        sessionId = intent.getStringExtra("session_id").orEmpty()
        roomId = intent.getStringExtra("room_id").orEmpty()
        serverUrl = intent.getStringExtra("server_url").orEmpty()
        participantToken = intent.getStringExtra("participant_token").orEmpty()
        if (listOf(adminToken, sessionId, roomId, serverUrl, participantToken).any { it.isBlank() }) {
            Toast.makeText(this, "بيانات جلسة المراقبة غير مكتملة", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        window.statusBarColor = Color.rgb(18, 18, 22)
        window.navigationBarColor = Color.BLACK
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 18, 22))
            setPadding(dp(14), dp(18), dp(14), dp(18))
        }
        val header = TextView(this).apply {
            text = "مراقبة مباشرة • للمشرف فقط"
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))
        status = TextView(this).apply {
            text = "جارٍ الاتصال بالبث..."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(42)))
        videoHost = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(videoHost, LinearLayout.LayoutParams(-1, 0, 1f))
        val note = TextView(this).apply {
            text = "وضع مشاهدة فقط: لا يتم تشغيل الميكروفون أو الكاميرا ولا حجز مقعد."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(note, LinearLayout.LayoutParams(-1, dp(56)))
        val end = TextView(this).apply {
            text = "إنهاء المراقبة"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(165, 45, 55))
            setOnClickListener { endMonitoring(true) }
        }
        root.addView(end, LinearLayout.LayoutParams(-1, dp(50)))
        setContentView(root)
        room = LiveKit.create(applicationContext)
        connectMonitor()
        refreshJob = scope.launch {
            while (isActive && !ending) {
                delay(8 * 60 * 1000L)
                refreshMonitorToken()
            }
        }
    }

    private fun connectMonitor() {
        scope.launch {
            try {
                room.connect(serverUrl, participantToken)
                // The server-issued grant is hidden + subscribe-only. Never publish local tracks.
                status.text = "متصل بالبث • صوت وصورة • مشاهدة فقط"
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val video = event.track as? VideoTrack
                            if (video != null) runOnUiThread { attachVideo(video) }
                        }
                        is RoomEvent.TrackUnsubscribed -> {
                            val video = event.track as? VideoTrack
                            if (video != null) runOnUiThread { detachVideo(video) }
                        }
                        else -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!ending) status.text = e.message ?: "تعذر الاتصال بالبث"
            }
        }
    }

    private fun attachVideo(track: VideoTrack) {
        if (ending) return
        try {
            if (renderer == null) {
                renderer = TextureViewRenderer(this).also {
                    room.initVideoRenderer(it)
                    it.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    videoHost.addView(it, FrameLayout.LayoutParams(-1, -1))
                }
            }
            if (currentVideo !== track) {
                try { currentVideo?.removeRenderer(renderer!!) } catch (_: Exception) {}
                track.addRenderer(renderer!!)
                currentVideo = track
            }
        } catch (e: Exception) {
            status.text = "تعذر عرض صورة البث: ${e.message}"
        }
    }

    private fun detachVideo(track: VideoTrack) {
        if (currentVideo === track) {
            try { track.removeRenderer(renderer!!) } catch (_: Exception) {}
            currentVideo = null
        }
    }

    private suspend fun refreshMonitorToken() {
        if (ending) return
        try {
            val result = withContext(Dispatchers.IO) {
                AuthApi.post("/api/admin/v2/reports/watch/$sessionId/token", JSONObject(), adminToken)
            }
            if (result.status !in 200..299) throw Exception(result.body.optString("error", "انتهت صلاحية جلسة المراقبة"))
            val nextToken = result.body.optString("participantToken")
            if (nextToken.isNotBlank()) {
                participantToken = nextToken
                withContext(Dispatchers.IO) { room.disconnect() }
                room.connect(serverUrl, participantToken)
                status.text = "تم تجديد اتصال المراقبة"
            }
        } catch (e: Exception) {
            status.text = e.message ?: "تعذر تجديد جلسة المراقبة"
        }
    }

    private fun endMonitoring(showMessage: Boolean) {
        if (ending) return
        ending = true
        refreshJob?.cancel()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AuthApi.post(
                        "/api/admin/v2/reports/watch/$sessionId/end",
                        JSONObject().put("admin_notes", "Ended from Android admin monitor").put("outcome", "reviewed"),
                        adminToken
                    )
                }
            } catch (_: Exception) { }
            try { currentVideo?.removeRenderer(renderer!!) } catch (_: Exception) {}
            try { renderer?.release() } catch (_: Exception) {}
            renderer = null
            try { room.disconnect() } catch (_: Exception) {}
            if (showMessage) Toast.makeText(this@AdminLiveMonitorActivity, "تم إنهاء المراقبة", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onBackPressed() {
        endMonitoring(false)
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        scope.cancel()
        try { currentVideo?.removeRenderer(renderer!!) } catch (_: Exception) {}
        try { renderer?.release() } catch (_: Exception) {}
        try { room.disconnect() } catch (_: Exception) {}
        executor.shutdown()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
