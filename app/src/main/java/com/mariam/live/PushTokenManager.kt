package com.mariam.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

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
        val userId = prefs.getString("user_id", null).orEmpty()

        if (authToken.isBlank()) {
            Log.w("BAK_PUSH", "Skip token registration: user is not signed in")
            return
        }

        // Always upload the current FCM token on app resume. The server may have
        // removed a token after FCM returned UNREGISTERED while local preferences
        // still said it was registered; trusting that flag would prevent recovery.
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken.isBlank()) {
                    Log.e("BAK_PUSH", "Firebase returned an empty FCM token")
                    return@addOnSuccessListener
                }

                prefs.edit().putString(TOKEN_KEY, fcmToken).apply()

                try {
                    executor.execute {
                        try {
                            val result = AuthApi.post(
                                "/api/notifications/devices/register",
                                JSONObject()
                                    .put("device_token", fcmToken)
                                    .put("platform", "android"),
                                authToken
                            )

                            if (result.status in 200..299) {
                                prefs.edit()
                                    .putBoolean("fcm_token_registered", true)
                                    .putString("fcm_registered_user_id", userId)
                                    .apply()
                                Log.i("BAK_PUSH", "Device token registered/refreshed successfully (HTTP ${result.status})")
                            } else {
                                prefs.edit().putBoolean("fcm_token_registered", false).apply()
                                Log.e(
                                    "BAK_PUSH",
                                    "Device registration failed (HTTP ${result.status}): ${result.body.optString("error")}"
                                )
                            }
                        } catch (error: Exception) {
                            prefs.edit().putBoolean("fcm_token_registered", false).apply()
                            Log.e("BAK_PUSH", "Device registration request failed", error)
                        }
                    }
                } catch (error: RejectedExecutionException) {
                    Log.e("BAK_PUSH", "Token registration executor is unavailable", error)
                }
            }
            .addOnFailureListener { error ->
                Log.e("BAK_PUSH", "Unable to obtain Firebase messaging token", error)
            }
    }

    fun onTokenRefresh(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).putBoolean("fcm_token_registered", false).apply()
        sync(context)
    }
}
