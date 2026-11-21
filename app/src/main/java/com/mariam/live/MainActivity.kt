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

        val launchIntent = Intent(this, destination).apply {\n            intent.extras?.let { putExtras(it) }\n            action = intent.action\n        }\n        startActivity(launchIntent)
        finish()
    }
}
