package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = getSharedPreferences(
            "MariamLive",
            MODE_PRIVATE
        ).getString("token", null)

        val destination = if (!token.isNullOrBlank()) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }

        val launchIntent = Intent(this, destination).apply {
            intent.extras?.let { putExtras(it) }
            action = intent.action
        }
        startActivity(launchIntent)
        finish()
    }
}
