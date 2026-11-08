package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.content.res.ColorStateList

class RoomActivity : Activity() {

    private val purple = Color.rgb(112, 78, 255)
    private val background = Color.rgb(17, 15, 29)
    private var micOn = false

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = background
        window.navigationBarColor = background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(background)
        }

        // Header
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val roomTitle = TextView(this).apply {
            text = "MARIAM LIVE"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        header.addView(
            roomTitle,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val close = TextView(this).apply {
            text = "✕"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = shape(Color.rgb(43, 39, 59), 30)
            setOnClickListener { finish() }
        }

        header.addView(close, LinearLayout.LayoutParams(dp(45), dp(45)))
        root.addView(header)

        // Room information
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(18), dp(12), dp(18))
        }

        info.addView(
            text("🎙  غرفة الأصدقاء", 23f, Color.WHITE)
        )

        info.addView(
            text("غرفة عامة  •  8 مقاعد  •  🟢 مباشرة", 13f,
                Color.rgb(190, 180, 220))
        )

        root.addView(info)

        // Room seats
        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
        }

        val names = listOf(
            "أنت", "سارة", "أحمد", "ليلى",
            "محمد", "نور", "علي", "فارغ"
        )

        val avatars = listOf(
            "👑", "🌸", "😎", "🦋",
            "🎧", "🌷", "🔥", "+"
        )

        for (i in 0 until 8) {

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(Color.rgb(32, 28, 48), 18)
                setPadding(dp(3), dp(8), dp(3), dp(8))
            }

            val avatar = TextView(this).apply {
                text = avatars[i]
                textSize = 27f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.rgb(112, 78, 255),
                        Color.rgb(154, 96, 255)
                    )
                ).apply {
                    shape = GradientDrawable.OVAL
                }
            }

            card.addView(
                avatar,
                LinearLayout.LayoutParams(dp(58), dp(58))
            )

            val name = text(names[i], 12f, Color.WHITE)
            name.setPadding(0, dp(8), 0, 0)
            card.addView(name)

            val status = text(
                if (i == 7) "＋ انضمام" else "🎤",
                11f,
                Color.rgb(190, 180, 220)
            )
            card.addView(status)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(125)
                columnSpec = GridLayout.spec(i % 4, 1f)
                rowSpec = GridLayout.spec(i / 4)
                setMargins(dp(3), dp(5), dp(3), dp(5))
            }

            grid.addView(card, params)

            if (i == 7) {
                card.setOnClickListener {
                    Toast.makeText(
                        this,
                        "طلب الانضمام للمقعد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        root.addView(grid)

        // Chat preview
        val chatTitle = text(
            "المحادثة المباشرة",
            17f,
            Color.WHITE
        )
        chatTitle.gravity = Gravity.CENTER_VERTICAL
        chatTitle.setPadding(0, dp(16), 0, dp(8))
        root.addView(chatTitle)

        val chat = TextView(this).apply {
            text = "🌸 سارة: أهلاً بالجميع في الغرفة 💜"
            textSize = 14f
            setTextColor(Color.rgb(220, 215, 240))
            setPadding(dp(14), dp(15), dp(14), dp(15))
            background = shape(Color.rgb(32, 28, 48), 15)
        }

        root.addView(
            chat,
            LinearLayout.LayoutParams(-1, dp(55))
        )

        // Bottom controls
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(8))
        }

        fun addControl(
            label: String,
            color: Int,
            action: () -> Unit
        ) {
            val button = TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = shape(color, 18)
                setOnClickListener { action() }
            }

            val params = LinearLayout.LayoutParams(
                0, dp(58), 1f
            )
            params.setMargins(dp(4), 0, dp(4), 0)
            controls.addView(button, params)
        }

        addControl("🎤 المايك", purple) {
            micOn = !micOn
            Toast.makeText(
                this,
                if (micOn) "تم تشغيل المايك" else "تم إيقاف المايك",
                Toast.LENGTH_SHORT
            ).show()
        }

        addControl("🎁 هدايا", Color.rgb(72, 49, 110)) {
            Toast.makeText(this, "الهدايا قريباً", Toast.LENGTH_SHORT).show()
        }

        addControl("💬 دردشة", Color.rgb(72, 49, 110)) {
            Toast.makeText(this, "الدردشة قريباً", Toast.LENGTH_SHORT).show()
        }

        addControl("خروج", Color.rgb(175, 48, 75)) {
            finish()
        }

        root.addView(controls)

        setContentView(root)
    }
}
