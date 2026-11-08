
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class RoomActivity : Activity() {

    private val purple = Color.rgb(112, 78, 255)
    private val dark = Color.rgb(24, 20, 42)
    private val cardColor = Color.rgb(39, 33, 61)
    private var micEnabled = false
    private var speakerEnabled = true

    private fun background(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
    }

    private fun button(
        value: String,
        color: Int,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(16, 14, 16, 14)
            background = background(color, 28f)
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = text("MARIAM LIVE", 23f, Color.WHITE, true)
        header.addView(
            title,
            LinearLayout.LayoutParams(0, 60, 1f)
        )

        header.addView(
            button("✕", Color.rgb(75, 48, 90)) {
                finish()
            }
        )

        content.addView(header)

        // Room information
        val roomInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(15, 20, 15, 20)
            background = background(cardColor, 20f)
        }

        roomInfo.addView(
            text("🎙 الغرفة الصوتية الرئيسية", 20f, Color.WHITE, true)
        )

        roomInfo.addView(
            text("👤 المضيف: Mariam", 14f, Color.LTGRAY)
        )

        roomInfo.addView(
            text("🟢 الغرفة مفتوحة", 13f, Color.GREEN)
        )

        content.addView(
            roomInfo,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = 20
            }
        )

        // Seats title
        content.addView(
            text("مقاعد المتحدثين", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 5, 5, 15)
            }
        )

        // Eight seats
        val names = listOf(
            "المضيف", "مقعد 2", "مقعد 3", "مقعد 4",
            "مقعد 5", "مقعد 6", "مقعد 7", "مقعد 8"
        )

        val avatars = listOf("👑", "🎧", "🎤", "🎵",
            "🎧", "🎤", "🎵", "🎧")

        for (row in 0 until 4) {
            val seatRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            for (column in 0 until 2) {
                val index = row * 2 + column

                val seat = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(8, 15, 8, 15)
                    background = background(cardColor, 18f)
                }

                seat.addView(
                    text(avatars[index], 32f, Color.WHITE)
                )

                seat.addView(
                    text(names[index], 14f, Color.WHITE, true)
                )

                seat.addView(
                    text(
                        if (index == 0) "المتحدث" else "فارغ",
                        11f,
                        if (index == 0) Color.GREEN else Color.LTGRAY
                    )
                )

                seatRow.addView(
                    seat,
                    LinearLayout.LayoutParams(0, 115, 1f).apply {
                        setMargins(5, 5, 5, 5)
                    }
                )
            }

            content.addView(seatRow)
        }

        // Chat section
        content.addView(
            text("المحادثة", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 20, 5, 12)
            }
        )

        val chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = background(cardColor, 18f)
        }

        chat.addView(
            text("👑 Mariam: أهلاً وسهلاً بالجميع ❤️", 14f, Color.WHITE).apply {
                gravity = Gravity.RIGHT
            }
        )

        chat.addView(
            text("🎧 مرحباً بالجميع!", 14f, Color.LTGRAY).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 12, 0, 0)
            }
        )

        content.addView(chat)

        // Message input
        val messageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val messageInput = EditText(this).apply {
            hint = "اكتب رسالة..."
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            background = background(cardColor, 25f)
            setPadding(15, 10, 15, 10)
        }

        messageRow.addView(
            messageInput,
            LinearLayout.LayoutParams(0, 50, 1f)
        )

        messageRow.addView(
            button("إرسال", purple) {
                val message = messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    Toast.makeText(
                        this,
                        "واجهة تجريبية: لم تُرسل الرسالة إلى مستخدمين",
                        Toast.LENGTH_SHORT
                    ).show()
                    messageInput.text.clear()
                }
            }.apply {
                setPadding(14, 12, 14, 12)
            }
        )

        content.addView(messageRow)

        // Controls
        content.addView(
            text("أدوات التحكم", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 15, 5, 15)
            }
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val micButton = button("🎤 تشغيل المايك", purple) {
            micEnabled = !micEnabled
            (it as TextView).text =
                if (micEnabled) "🔇 إيقاف المايك" else "🎤 تشغيل المايك"
            Toast.makeText(
                this@RoomActivity,
                "هذا زر تجريبي فقط، الصوت الحقيقي غير مفعّل",
                Toast.LENGTH_SHORT
            ).show()
        }

        val speakerButton = button("🔊 السماعة", cardColor) {
            speakerEnabled = !speakerEnabled
            (it as TextView).text =
                if (speakerEnabled) "🔊 السماعة" else "🔇 السماعة"
        }

        controls.addView(
            micButton,
            LinearLayout.LayoutParams(0, 55, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        controls.addView(
            speakerButton,
            LinearLayout.LayoutParams(0, 55, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        content.addView(controls)

        content.addView(
            button("🎁 إرسال هدية", Color.rgb(190, 70, 145)) {
                Toast.makeText(
                    this,
                    "نظام الهدايا قيد التطوير",
                    Toast.LENGTH_SHORT
                ).show()
            }.apply {
                setPadding(15, 16, 15, 16)
            }
        )

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }
}
