package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView

class MalakWelcomeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val continueToHome = Runnable {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val image = ImageView(this).apply {
            setImageResource(R.drawable.malak_splash)
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
            animate().alpha(1f).setDuration(350L).start()
            contentDescription = "مرحباً بك حبيبتي ملاك"
        }
        setContentView(image)
        handler.postDelayed(continueToHome, 5000L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(continueToHome)
        super.onDestroy()
    }
}
