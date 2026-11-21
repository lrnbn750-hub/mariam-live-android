package com.mariam.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.util.concurrent.Executors

object PushTokenManager {
    private val executor = Executors.newSingleThreadExecutor()
    private const val PREFS = "MariamLive"
    private const val TOKEN_KEY = "fcm_token"
    private const val PERMISSION_REQUEST = 7402

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST)
        }
    }

    fun sync(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val authToken = prefs.getString("token", null).orEmpty()
        if (authToken.isBlank()) return

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            if (fcmToken.isNullOrBlank()) return@addOnSuccessListener
            val previous = prefs.getString(TOKEN_KEY, null)
            prefs.edit().putString(TOKEN_KEY, fcmToken).apply()
            if (previous == fcmToken && prefs.getBoolean("fcm_token_registered", false)) return@addOnSuccessListener

            executor.execute {
                try {
                    val result = AuthApi.post(
                        "/api/notifications/devices/register",
                        JSONObject().put("device_token", fcmToken).put("platform", "android"),
                        authToken
                    )
                    if (result.status in 200..299) {
                        prefs.edit().putBoolean("fcm_token_registered", true).apply()
                    } else {
                        prefs.edit().putBoolean("fcm_token_registered", false).apply()
                    }
                } catch (_: Exception) {
                    prefs.edit().putBoolean("fcm_token_registered", false).apply()
                }
            }
        }
    }

    fun onTokenRefresh(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).putBoolean("fcm_token_registered", false).apply()
        sync(context)
    }
}
