package com.mariam.live

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Checks the authenticated session while any app screen is in the foreground.
 * A server-side ban is therefore enforced even if the user is already inside a room.
 */
class BanSessionMonitor : Application(), Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val checking = AtomicBoolean(false)
    @Volatile private var foregroundActivities = 0

    private val poll = object : Runnable {
        override fun run() {
            if (foregroundActivities > 0) checkCurrentSession()
            if (foregroundActivities > 0) handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    private fun checkCurrentSession() {
        val prefs = getSharedPreferences("MariamLive", MODE_PRIVATE)
        val token = prefs.getString("token", null)?.takeIf { it.isNotBlank() } ?: return
        if (!checking.compareAndSet(false, true)) return

        executor.execute {
            try {
                val result = AuthApi.get("/api/auth/me", token)
                if (result.status == 403 || result.status == 401) {
                    val banned = result.body.optBoolean("banned") ||
                        result.body.optString("code") == "ACCOUNT_BANNED"
                    prefs.edit().remove("token").remove("user_id").remove("username")
                        .remove("phone").remove("email").remove("active_room_id").apply()

                    handler.post {
                        if (banned) {
                            val intent = Intent(this, AccountBannedActivity::class.java).apply {
                                putExtra("reason", result.body.optString("reason"))
                                putExtra("ban_type", result.body.optString("ban_type"))
                                putExtra("started_at", result.body.optString("created_at"))
                                putExtra("expires_at", result.body.optString("expires_at"))
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, LoginActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            startActivity(intent)
                        }
                    }
                }
            } catch (_: Exception) {
                // Network failures must not log out a user.
            } finally {
                checking.set(false)
            }
        }
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    /** A small in-app dock keeps the active room one tap away on every screen. */
    private fun showRoomReturnButton(activity: Activity) {
        if (activity is RoomActivity || activity is LoginActivity || activity is AccountBannedActivity) return
        val roomId = getSharedPreferences("MariamLive", MODE_PRIVATE)
            .getString("active_room_id", null)?.takeIf { it.isNotBlank() } ?: return
        val decor = activity.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>("bak_live_room_return") != null) return

        val button = TextView(activity).apply {
            tag = "bak_live_room_return"
            text = "🎙  الرجوع للغرفة"
            textSize = 12.5f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(activity, 15), 0, dp(activity, 15), 0)
            elevation = dp(activity, 8).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(82, 65, 125), Color.rgb(42, 35, 72))
            ).apply {
                cornerRadius = dp(activity, 24).toFloat()
                setStroke(dp(activity, 1), Color.argb(110, 255, 255, 255))
            }
            contentDescription = "العودة إلى الغرفة الصوتية مع الاحتفاظ بالمقعد"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val currentRoomId = getSharedPreferences("MariamLive", MODE_PRIVATE)
                    .getString("active_room_id", null)?.takeIf { it.isNotBlank() } ?: roomId
                val intent = Intent(activity, RoomActivity::class.java).apply {
                    putExtra("room_id", currentRoomId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                activity.startActivity(intent)
            }
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 42), Gravity.TOP or Gravity.END
        ).apply {
            topMargin = dp(activity, 58)
            marginEnd = dp(activity, 12)
        }
        decor.addView(button, params)
    }

    private fun removeRoomReturnButton(activity: Activity) {
        val decor = activity.window?.decorView as? ViewGroup ?: return
        decor.findViewWithTag<View>("bak_live_room_return")?.let { decor.removeView(it) }
    }

    override fun onActivityResumed(activity: Activity) {
        foregroundActivities++
        if (activity !is AccountBannedActivity && activity !is LoginActivity) {
            checkCurrentSession()
        }
        showRoomReturnButton(activity)
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, 15_000L)
    }

    override fun onActivityPaused(activity: Activity) {
        removeRoomReturnButton(activity)
        foregroundActivities = (foregroundActivities - 1).coerceAtLeast(0)
        if (foregroundActivities == 0) handler.removeCallbacks(poll)
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
