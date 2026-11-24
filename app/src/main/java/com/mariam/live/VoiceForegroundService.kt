package com.mariam.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class VoiceForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var activeRoomId: String = ""
    private var authToken: String = ""

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BAK LIVE voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your voice room connected while using other apps"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPresenceAndService()
            return START_NOT_STICKY
        }

        val roomId = intent?.getStringExtra(EXTRA_ROOM_ID).orEmpty()
        val token = intent?.getStringExtra(EXTRA_AUTH_TOKEN).orEmpty()
        if (roomId.isBlank() || token.isBlank()) {
            Log.w(TAG, "Missing room credentials; refusing to run presence heartbeat")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        activeRoomId = roomId
        authToken = token

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("BAK LIVE")
            .setContentText("أنت متصل بغرفة صوتية — الصوت يعمل في الخلفية")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive && activeRoomId == roomId && authToken == token) {
                try {
                    val response = AuthApi.post(
                        "/api/voice-rooms/$roomId/presence/heartbeat",
                        JSONObject(),
                        token
                    )
                    if (response.status == 401 || response.status == 403 || response.status == 404) {
                        Log.w(TAG, "Room presence rejected (HTTP ${response.status}); stopping service")
                        stopPresenceAndService()
                        break
                    }
                } catch (_: CancellationException) {
                    break
                } catch (error: Exception) {
                    Log.w(TAG, "Room presence heartbeat failed; will retry", error)
                }
                delay(2500L)
            }
        }

        // Never resurrect an audio session after Android kills the app process.
        return START_NOT_STICKY
    }

    private fun stopPresenceAndService() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        activeRoomId = ""
        authToken = ""
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Removing BAK LIVE from Recents ends the session; ordinary minimization keeps this service alive.
        stopPresenceAndService()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.mariam.live.action.STOP_VOICE"
        const val EXTRA_ROOM_ID = "com.mariam.live.extra.ROOM_ID"
        const val EXTRA_AUTH_TOKEN = "com.mariam.live.extra.AUTH_TOKEN"
        private const val CHANNEL_ID = "mariam_live_voice"
        private const val NOTIFICATION_ID = 4103
        private const val TAG = "BAK_VOICE"
    }
}
