package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class RoomActivity : Activity() {

    private var micOn = false
    private val purple = Color.rgb(112, 78, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(18, 25, 18, 20)
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val title = TextView(this).apply {
            text = "غرفة Mariam Live"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(purple)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "الغرفة العامة • 8 مقاعد"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 25)
        }
        root.addView(subtitle)

        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 4
        }

        for (i in 1..8) {
            val seat = TextView(this).apply {
                text = "🎤\nالمقعد $i\nفارغ"
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(purple)
                setBackgroundColor(Color.WHITE)
                setPadding(15, 22, 15, 22)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 170
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                )
                setMargins(6, 6, 6, 6)
            }

            grid.addView(seat, params)
        }

        root.addView(grid)

        val micButton = Button(this).apply {
            text = "تشغيل المايكروفون"
            setOnClickListener {
                micOn = !micOn
                text = if (micOn)
                    "إيقاف المايكروفون"
                else
                    "تشغيل المايكروفون"
            }
        }

        root.addView(micButton)

        val exitButton = Button(this).apply {
            text = "مغادرة الغرفة"
            setOnClickListener { finish() }
        }

        root.addView(exitButton)

        setContentView(root)
    }
}
