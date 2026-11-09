package com.mariam.live

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import org.json.JSONObject
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomActivity : Activity() {
    private lateinit var room: Room
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var leaving = false
    private var connected = false
    private var microphoneEnabled = true
    private var roomId: String = ""
    private var authToken: String = ""

    private val backgroundColor = Color.rgb(17, 13, 29)
    private val purple = Color.rgb(151, 91, 255)
    private lateinit var state: TextView
    private lateinit var micButton: TextView
    private lateinit var leaveButton: TextView

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        roomId = intent.getStringExtra("room_id").orEmpty()
        authToken = getSharedPreferences("MariamLive", MODE_PRIVATE)
            .getString("token", null).orEmpty()

        val roomName = intent.getStringExtra("room_name")
            ?.takeIf { it.isNotBlank() } ?: "غرفة صوتية"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(backgroundColor)
        }

        val title = TextView(this).apply {
            text = roomName
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val stateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = GradientDrawable().apply {
                setColor(Color.rgb(34, 26, 51))
                cornerRadius = dp(18).toFloat()
            }
        }
        state = TextView(this).apply {
            text = "جارٍ تجهيز الاتصال الصوتي..."
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        stateCard.addView(state)
        val details = TextView(this).apply {
            text = "الصوت يعمل عبر خادم Mariam Live الخاص"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        stateCard.addView(details)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(26)
        root.addView(stateCard, params)

        micButton = actionButton("🎙 إيقاف المايك")
        micButton.setOnClickListener { toggleMicrophone() }
        val micParams = LinearLayout.LayoutParams(-1, dp(52))
        micParams.topMargin = dp(22)
        root.addView(micButton, micParams)

        leaveButton = actionButton("مغادرة الغرفة")
        leaveButton.setOnClickListener { leaveRoom() }
        val leaveParams = LinearLayout.LayoutParams(-1, dp(50))
        leaveParams.topMargin = dp(12)
        root.addView(leaveButton, leaveParams)

        setContentView(root)

        if (roomId.isBlank() || authToken.isBlank()) {
            state.text = "بيانات الغرفة أو تسجيل الدخول غير متوفرة"
            micButton.isEnabled = false
            return
        }

        room = LiveKit.create(applicationContext)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            connectToVoiceRoom()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
        }
    }

    private fun actionButton(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(purple)
            cornerRadius = dp(14).toFloat()
        }
    }

    private fun connectToVoiceRoom() {
        state.text = "جارٍ الاتصال بخادم الصوت..."
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    AuthApi.get("/api/voice-rooms/$roomId/token", authToken)
                }
                if (response.status !in 200..299) {
                    throw IllegalStateException(response.body.optString("error", "تعذر إصدار رمز دخول الصوت"))
                }

                val serverUrl = response.body.optString("serverUrl")
                val participantToken = response.body.optString("participantToken")
                if (serverUrl.isBlank() || participantToken.isBlank()) {
                    throw IllegalStateException("استجابة رمز الصوت غير مكتملة")
                }

                room.connect(serverUrl, participantToken)
                room.localParticipant.setMicrophoneEnabled(true)
                connected = true
                microphoneEnabled = true
                state.text = "أنت متصل بالغرفة الصوتية"
                micButton.text = "🎙 إيقاف المايك"
            } catch (error: Exception) {
                state.text = error.message ?: "تعذر الاتصال الصوتي"
                micButton.isEnabled = false
                Toast.makeText(this@RoomActivity, state.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleMicrophone() {
        if (!connected) return
        scope.launch {
            try {
                microphoneEnabled = !microphoneEnabled
                room.localParticipant.setMicrophoneEnabled(microphoneEnabled)
                micButton.text = if (microphoneEnabled) "🎙 إيقاف المايك" else "🔇 تشغيل المايك"
                state.text = if (microphoneEnabled) "المايك يعمل" else "المايك متوقف"
            } catch (error: Exception) {
                microphoneEnabled = !microphoneEnabled
                Toast.makeText(this@RoomActivity, "تعذر تغيير حالة المايك", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToVoiceRoom()
            } else {
                state.text = "يجب السماح باستخدام المايك للدخول الصوتي"
                Toast.makeText(this, "اسمح للتطبيق باستخدام المايك", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun leaveRoom() {
        if (leaving) return
        leaving = true
        scope.launch {
            try {
                if (connected) room.disconnect()
                withContext(Dispatchers.IO) {
                    AuthApi.post("/api/voice-rooms/$roomId/leave", JSONObject(), authToken)
                }
            } catch (_: Exception) {
                // Close locally even if the API is temporarily unreachable.
            } finally {
                connected = false
                finish()
            }
        }
    }

    override fun onBackPressed() {
        leaveRoom()
    }

    override fun onDestroy() {
        if (!leaving && connected) {
            room.disconnect()
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MICROPHONE = 4102
    }
}
