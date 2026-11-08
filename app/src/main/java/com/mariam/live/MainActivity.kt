package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Mariam Live"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(112, 78, 255))
        }

        setContentView(title)
    }
}
