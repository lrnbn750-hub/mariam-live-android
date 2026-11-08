
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

class RoomActivity : Activity() {

    private val purple = Color.rgb(112, 78, 255)
    private val dark = Color.rgb(24, 20, 42)
    private val cardColor = Color.rgb(39, 33, 61)

    private fun makeBackground(color: Int, radius: Float): GradientDrawable {
        val shape = GradientDrawable()
        shape.setColor(color)
        shape.cornerRadius = radius
        return shape
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        val view = TextView(this)
        view.text = value
        view.textSize = size
        view.setTextColor(color)
        view.gravity = Gravity.CENTER

        if (bold) {
            view.typeface = Typeface.DEFAULT_BOLD
        }

        return view
    }

    private fun makeButton(
        value: String,
        color: Int,
        action: (TextView) -> Unit
    ): TextView {
        val view = TextView(this)
        view.text = value
        view.textSize = 15f
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.setPadding(16, 14, 16, 14)
        view.background = makeBackground(color, 28f)

        view.setOnClickListener {
            action(view)
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(dark)

        val scroll = ScrollView(this)

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(18, 18, 18, 24)

        // Header
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val title = makeText("MARIAM LIVE", 23f, Color.WHITE, true)

        header.addView(
            title,
            LinearLayout.LayoutParams(0, 60, 1f)
        )

        header.addView(
            makeButton("✕", Color.rgb(75, 48, 90)) {
                finish()
            }
        )

        content.addView(header)

        // Room information
        val roomInfo = LinearLayout(this)
        roomInfo.orientation = LinearLayout.VERTICAL
        roomInfo.gravity = Gravity.CENTER
        roomInfo.setPadding(15, 20, 15, 20)
        roomInfo.background = makeBackground(cardColor, 20f)

        roomInfo.addView(
            makeText("🎙 الغرفة الصوتية الرئيسية", 20f, Color.WHITE, true)
        )

        roomInfo.addView(
            makeText("👤 المضيف: Mariam", 14f, Color.LTGRAY)
        )

        roomInfo.addView(
            makeText("🟢 الغرفة مفتوحة", 13f, Color.GREEN)
        )

        content.addView(
            roomInfo,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = 20
            }
        )

        // Seats
        content.addView(
            makeText("مقاعد المتحدثين", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 5, 5, 15)
            }
        )

        val names = listOf(
            "المضيف", "مقعد 2", "مقعد 3", "مقعد 4",
            "مقعد 5", "مقعد 6", "مقعد 7", "مقعد 8"
        )

        val avatars = listOf(
            "👑", "🎧", "🎤", "🎵",
            "🎧", "🎤", "🎵", "🎧"
        )

        for (row in 0 until 4) {

            val seatRow = LinearLayout(this)
            seatRow.orientation = LinearLayout.HORIZONTAL
            seatRow.gravity = Gravity.CENTER

            for (column in 0 until 2) {

                val index = row * 2 + column

                val seat = LinearLayout(this)
                seat.orientation = LinearLayout.VERTICAL
                seat.gravity = Gravity.CENTER
                seat.setPadding(8, 15, 8, 15)
                seat.background = makeBackground(cardColor, 18f)

                seat.addView(
                    makeText(avatars[index], 32f, Color.WHITE)
                )

                seat.addView(
                    makeText(names[index], 14f, Color.WHITE, true)
                )

                seat.addView(
                    makeText(
                        if (index == 0) "المتحدث" else "فارغ",
                        11f,
                        if (index == 0) Color.GREEN else Color.LTGRAY
                    )
                )

                val params = LinearLayout.LayoutParams(0, 115, 1f)
                params.setMargins(5, 5, 5, 5)

                seatRow.addView(seat, params)
            }

            content.addView(seatRow)
        }

        // Chat
        content.addView(
            makeText("المحادثة", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 20, 0, 12)
            }
        )

        val chat = LinearLayout(this)
        chat.orientation = LinearLayout.VERTICAL
        chat.setPadding(14, 12, 14, 12)
        chat.background = makeBackground(cardColor, 18f)

        chat.addView(
            makeText("👑 Mariam: أهلاً وسهلاً بالجميع ❤️", 14f, Color.WHITE).apply {
                gravity = Gravity.RIGHT
            }
        )

        chat.addView(
            makeText("🎧 مرحباً بالجميع!", 14f, Color.LTGRAY).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 12, 0, 0)
            }
        )

        content.addView(chat)

        // Message input
        val messageRow = LinearLayout(this)
        messageRow.orientation = LinearLayout.HORIZONTAL
        messageRow.gravity = Gravity.CENTER_VERTICAL
        messageRow.setPadding(0, 12, 0, 12)

        val messageInput = EditText(this)
        messageInput.hint = "اكتب رسالة..."
        messageInput.textSize = 14f
        messageInput.setTextColor(Color.WHITE)
        messageInput.setHintTextColor(Color.LTGRAY)
        messageInput.setSingleLine(true)
        messageInput.setPadding(15, 10, 15, 10)
        messageInput.background = makeBackground(cardColor, 25f)

        messageRow.addView(
            messageInput,
            LinearLayout.LayoutParams(0, 50, 1f)
        )

        messageRow.addView(
            makeButton("إرسال", purple) {
                val message = messageInput.text.toString().trim()

                if (message.isNotEmpty()) {
                    Toast.makeText(
                        this,
                        "الدردشة التجريبية غير متصلة بالسيرفر",
                        Toast.LENGTH_SHORT
                    ).show()

                    messageInput.text.clear()
                }
            }
        )

        content.addView(messageRow)

        // Controls
        content.addView(
            makeText("أدوات التحكم", 18f, Color.WHITE, true).apply {
                gravity = Gravity.RIGHT
                setPadding(0, 15, 0, 15)
            }
        )

        val controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER

        val micButton = makeButton("🎤 تشغيل المايك", purple) { button ->
            button.text = if (button.text == "🎤 تشغيل المايك") {
                "🔇 إيقاف المايك"
            } else {
                "🎤 تشغيل المايك"
            }

            Toast.makeText(
                this,
                "زر تجريبي فقط، الصوت الحقيقي غير مفعّل",
                Toast.LENGTH_SHORT
            ).show()
        }

        val speakerButton = makeButton("🔊 السماعة", cardColor) { button ->
            button.text = if (button.text == "🔊 السماعة") {
                "🔇 السماعة"
            } else {
                "🔊 السماعة"
            }
        }

        val micParams = LinearLayout.LayoutParams(0, 55, 1f)
        micParams.setMargins(4, 4, 4, 4)

        val speakerParams = LinearLayout.LayoutParams(0, 55, 1f)
        speakerParams.setMargins(4, 4, 4, 4)

        controls.addView(micButton, micParams)
        controls.addView(speakerButton, speakerParams)

        content.addView(controls)

        content.addView(
            makeButton("🎁 إرسال هدية", Color.rgb(190, 70, 145)) {
                Toast.makeText(
                    this,
                    "نظام الهدايا قيد التطوير",
                    Toast.LENGTH_SHORT
                ).show()
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
