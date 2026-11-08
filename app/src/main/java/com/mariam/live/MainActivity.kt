package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val purple = Color.rgb(112, 78, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val header = TextView(this).apply {
            text = "Mariam Live"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 35, 24, 35)
            setBackgroundColor(purple)
        }

        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        fun addSection(
            title: String,
            description: String,
            action: (() -> Unit)? = null
        ) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundColor(Color.WHITE)
                if (action != null) {
                    setOnClickListener { action() }
                }
            }

            val name = TextView(this).apply {
                text = title
                textSize = 21f
                setTextColor(purple)
            }

            val info = TextView(this).apply {
                text = description
                textSize = 15f
                setTextColor(Color.DKGRAY)
                setPadding(0, 10, 0, 10)
            }

            card.addView(name)
            card.addView(info)

            content.addView(card)

            content.addView(
                TextView(this).apply { text = " " }
            )
        }

        addSection(
            "غرف صوتية",
            "اضغط للدخول إلى الغرفة الصوتية"
        ) {
            startActivity(
                Intent(this, RoomActivity::class.java)
            )
        }

        addSection(
            "الرسائل",
            "محادثاتك الخاصة"
        )

        addSection(
            "الأصدقاء",
            "ابحث عن أصدقاء جدد"
        )

        addSection(
            "الهدايا والعملات",
            "رصيدك وهداياك"
        )

        addSection(
            "ملفي الشخصي",
            "إعدادات حسابك"
        )

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        setContentView(root)
    }
}
