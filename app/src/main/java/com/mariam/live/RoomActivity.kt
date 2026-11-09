package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.concurrent.Executors

class RoomActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var leaving = false
    private val backgroundColor = Color.rgb(17, 13, 29)
    private val purple = Color.rgb(151, 91, 255)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

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
        val state = TextView(this).apply {
            text = "البث الصوتي غير متاح حالياً"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        stateCard.addView(state)
        val details = TextView(this).apply {
            text = "لم يتم ربط خدمة الصوت المباشر وخدمة الغرف بالخادم بعد. لن نعرض مقاعد أو مستمعين أو رسائل وهمية، ولن ندّعي أن المايك يعمل قبل اكتمال الربط."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        stateCard.addView(details)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(26)
        root.addView(stateCard, params)

        val close = TextView(this).apply {
            text = "العودة"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(purple)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { leaveRoom() }
        }
        val closeParams = LinearLayout.LayoutParams(-1, dp(50))
        closeParams.topMargin = dp(20)
        root.addView(close, closeParams)

        setContentView(root)
    }

    private fun leaveRoom() {
        if (leaving) return
        val roomId = intent.getStringExtra("room_id")
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (roomId.isNullOrBlank() || token.isNullOrBlank()) {
            finish()
            return
        }
        leaving = true
        executor.execute {
            try {
                val result = AuthApi.post("/api/voice-rooms/$roomId/leave", JSONObject(), token)
                runOnUiThread {
                    if (result.status !in 200..299 && result.status != 404) {
                        Toast.makeText(this, "تعذر مغادرة الغرفة من الخادم", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "انقطع الاتصال؛ أُغلقت الشاشة فقط", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        leaveRoom()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
