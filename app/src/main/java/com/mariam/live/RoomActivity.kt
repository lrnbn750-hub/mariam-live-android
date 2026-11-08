
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

    private val purple = Color.rgb(145, 65, 255)
    private val pink = Color.rgb(218, 55, 155)
    private val white = Color.WHITE

    private fun shape(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun button(
        symbol: String,
        description: String,
        color: Int,
        action: (() -> Unit)? = null
    ): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(color, 28f)
            setPadding(8, 12, 8, 12)
            if (action != null) setOnClickListener { action() }
        }

        box.addView(text(symbol, 23f))
        box.addView(text(description, 10f, Color.LTGRAY))
        return box
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.rgb(10, 8, 17))
        }

        // Header
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val menu = TextView(this).apply {
            text = "⋮"
            textSize = 30f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = shape(Color.rgb(42, 40, 48), 60f)
        }
        header.addView(menu, LinearLayout.LayoutParams(52, 52))

        val roomTitle = text("MARIAM LIVE", 20f, white, true)
        header.addView(roomTitle, LinearLayout.LayoutParams(0, 52, 1f))

        val close = TextView(this).apply {
            text = "×"
            textSize = 32f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = shape(Color.rgb(42, 40, 48), 60f)
            setOnClickListener { finish() }
        }
        header.addView(close, LinearLayout.LayoutParams(52, 52))
        root.addView(header)

        // Room information
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 10)
            background = shape(Color.rgb(29, 22, 45), 20f)
        }

        info.addView(text("👑", 27f), LinearLayout.LayoutParams(45, 45))
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        details.addView(text("الغرفة الرئيسية", 17f, white, true))
        details.addView(text("المضيف: Mariam  •  🟢 مفتوحة", 12f, Color.LTGRAY))
        info.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        info.addView(text("👥  128", 13f, white, true))
        val infoParams = LinearLayout.LayoutParams(-1, 76)
        infoParams.topMargin = 18
        root.addView(info, infoParams)

        // Stage
        val stage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(49, 20, 75),
                    Color.rgb(17, 12, 29),
                    Color.rgb(65, 20, 65)
                )
            ).apply { cornerRadius = 22f }
            setPadding(8, 14, 8, 14)
        }

        stage.addView(text("✦  مسرح الغرفة  ✦", 15f, Color.LTGRAY, true))

        // Main speakers
        val mainSeats = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }

        fun seat(avatar: String, name: String, crown: Boolean = false): LinearLayout {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
            }
            val picture = TextView(this).apply {
                text = avatar
                textSize = 34f
                gravity = Gravity.CENTER
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(purple, pink)
                ).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(3, Color.rgb(255, 205, 120))
                }
            }
            item.addView(picture, LinearLayout.LayoutParams(66, 66))
            item.addView(text(if (crown) "👑 $name" else name, 12f, white, true))
            return item
        }

        mainSeats.addView(seat("👩🏻", "Mariam", true))
        mainSeats.addView(seat("👨🏻", "ضيف 2"))
        stage.addView(mainSeats)

        stage.addView(text("────────  المقاعد الصوتية  ────────", 11f, Color.LTGRAY))

        // Eight seats
        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = true
        }

        for (i in 1..8) {
            val occupied = i == 1 || i == 4
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(Color.rgb(48, 36, 65), 18f)
                setPadding(4, 10, 4, 10)
            }
            cell.addView(text(if (occupied) "🎙" else "＋", 25f))
            cell.addView(text(if (occupied) "متحدث" else "مقعد $i", 10f, Color.LTGRAY))
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 82
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 5, 4, 5)
            }
            grid.addView(cell, params)
        }
        stage.addView(grid)

        val stageParams = LinearLayout.LayoutParams(-1, 0, 1f)
        stageParams.topMargin = 12
        stageParams.bottomMargin = 12
        root.addView(stage, stageParams)

        // Chat area
        root.addView(text("المحادثة", 17f, white, true).apply {
            gravity = Gravity.RIGHT
            setPadding(0, 4, 8, 8)
        })

        val chat = TextView(this).apply {
            text = "👑 Mariam: أهلاً وسهلاً بالجميع ❤️\n\n🎧 مرحباً بالجميع!"
            textSize = 14f
            setTextColor(white)
            setPadding(14, 12, 14, 12)
            background = shape(Color.rgb(36, 28, 51), 18f)
        }
        root.addView(chat, LinearLayout.LayoutParams(-1, 75))

        // Message bar
        val messageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val message = EditText(this).apply {
            hint = "اكتب رسالة..."
            textSize = 14f
            setTextColor(white)
            setHintTextColor(Color.LTGRAY)
            singleLine = true
            background = shape(Color.rgb(39, 30, 55), 30f)
            setPadding(15, 4, 15, 4)
        }
        messageRow.addView(message, LinearLayout.LayoutParams(0, 48, 1f))

        val send = TextView(this).apply {
            text = "إرسال"
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = shape(purple, 30f)
            setOnClickListener {
                if (message.text.toString().isNotBlank()) {
                    Toast.makeText(this@RoomActivity, "واجهة تجريبية - الدردشة تحتاج سيرفر", Toast.LENGTH_SHORT).show()
                    message.text.clear()
                }
            }
        }
        val sendParams = LinearLayout.LayoutParams(75, 48)
        sendParams.marginStart = 8
        messageRow.addView(send, sendParams)
        val messageParams = LinearLayout.LayoutParams(-1, 52)
        messageParams.topMargin = 8
        root.addView(messageRow, messageParams)

        // Bottom controls
        root.addView(text("أدوات الغرفة", 16f, white, true).apply {
            gravity = Gravity.RIGHT
            setPadding(0, 12, 8, 8)
        })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controls.addView(button("🎙", "المايك", Color.rgb(104, 55, 210)) {
            Toast.makeText(this@RoomActivity, "الميكروفون يحتاج ربط خدمة صوتية", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, 58, 1f))

        val soundParams = LinearLayout.LayoutParams(0, 58, 1f)
        soundParams.marginStart = 8
        controls.addView(button("🔊", "السماعة", Color.rgb(43, 35, 59)) {
            Toast.makeText(this@RoomActivity, "زر السماعة تجريبي", Toast.LENGTH_SHORT).show()
        }, soundParams)

        root.addView(controls)

        val gift = TextView(this).apply {
            text = "🎁   إرسال هدية"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = shape(pink, 30f)
            setOnClickListener {
                Toast.makeText(this@RoomActivity, "نظام الهدايا يحتاج ربط السيرفر", Toast.LENGTH_SHORT).show()
            }
        }
        val giftParams = LinearLayout.LayoutParams(-1, 54)
        giftParams.topMargin = 8
        root.addView(gift, giftParams)

        setContentView(root)
    }
}
