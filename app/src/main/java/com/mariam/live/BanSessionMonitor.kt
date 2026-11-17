package com.mariam.live

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
                    prefs.edit().clear().apply()

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

    override fun onActivityResumed(activity: Activity) {
        foregroundActivities++
        if (activity !is AccountBannedActivity && activity !is LoginActivity) {
            checkCurrentSession()
        }
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, 15_000L)
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundActivities = (foregroundActivities - 1).coerceAtLeast(0)
        if (foregroundActivities == 0) handler.removeCallbacks(poll)
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
