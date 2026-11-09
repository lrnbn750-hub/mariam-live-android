
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class RoomActivity : Activity() {

    private val bgColor = Color.rgb(17, 13, 29)
    private val panelColor = Color.rgb(34, 26, 51)
    private val purple = Color.rgb(151, 91, 255)
    private val white = Color.WHITE
    private var micEnabled = false
    private var handRaised = false
    private var isRoomOwner = false

    private lateinit var micButton: TextView
    private lateinit var handButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val roomName = intent.getStringExtra("room_name")
            ?.takeIf { it.isNotBlank() } ?: "غرفة مريم الصوتية"
        isRoomOwner = intent.getBooleanExtra("is_owner", false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val roomInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        roomInfo.addView(text(
            roomName,
            21,
            white,
            true
        ))

        roomInfo.addView(text(
            if (isRoomOwner) "أنت مالك هذه الغرفة" else "غرفة صوتية تجريبية",
            12,
            Color.LTGRAY
        ))

        header.addView(
            roomInfo,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        header.addView(text("🔴 LIVE", 13, Color.RED, true))
        header.addView(text("  ↗  ", 24, white))
        header.addView(text("  ⋮", 25, white))

        content.addView(header)

        // Welcome banner
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(15), dp(12), dp(15))
            background = rounded(
                Color.rgb(65, 38, 105),
                16
            )
        }

        banner.addView(text(
            "أهلاً بكم في غرفة مريم ❤️",
            17,
            white,
            true
        ))

        banner.addView(text(
            "احترموا الجميع واستمتعوا بوقتكم",
            12,
            Color.LTGRAY
        ))

        val bannerParams = LinearLayout.LayoutParams(
            -1,
            dp(85)
        )
        bannerParams.topMargin = dp(16)
        content.addView(banner, bannerParams)

        // Seats title
        val seatsTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        seatsTitle.addView(
            text("مقاعد المتحدثين", 17, white, true),
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        seatsTitle.addView(text("8 مقاعد", 12, Color.LTGRAY))

        content.addView(seatsTitle)

        // Seats grid
        val seats = listOf(
            "👑" to "المالك",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ",
            "＋" to "مقعد فارغ"
        )

        for (rowIndex in 0..1) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            for (columnIndex in 0..3) {
                val index = rowIndex * 4 + columnIndex
                val seat = seatView(
                    seats[index].first,
                    seats[index].second,
                    index == 0
                )

                val params = LinearLayout.LayoutParams(
                    0,
                    dp(112),
                    1f
                )
                params.setMargins(dp(3), dp(5), dp(3), dp(5))

                row.addView(seat, params)
            }

            content.addView(row)
        }

        // Audience information
        val audience = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(panelColor, 12)
        }

        audience.addView(
            text("👥  المستمعون", 14, white, true),
            LinearLayout.LayoutParams(0, dp(35), 1f)
        )

        audience.addView(text("24 شخص", 13, Color.LTGRAY))

        val audienceParams = LinearLayout.LayoutParams(
            -1,
            dp(55)
        )
        audienceParams.topMargin = dp(12)
        content.addView(audience, audienceParams)

        // Chat title
        val chatTitle = text(
            "المحادثة",
            17,
            white,
            true
        )
        val chatTitleParams = LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
        chatTitleParams.topMargin = dp(8)
        content.addView(chatTitle, chatTitleParams)

        // Chat panel
        val chatPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(panelColor, 14)
        }

        chatPanel.addView(chatMessage(
            "مريم",
            "أهلاً وسهلاً بالجميع ❤️",
            purple
        ))

        chatPanel.addView(chatMessage(
            "أحمد",
            "مساء الخير عليكم 🌹",
            Color.rgb(90, 180, 255)
        ))

        chatPanel.addView(chatMessage(
            "سارة",
            "نورتم الغرفة",
            Color.rgb(255, 150, 190)
        ))

        content.addView(
            chatPanel,
            LinearLayout.LayoutParams(-1, dp(145))
        )

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        // Bottom controls
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(5))
        }

        micButton = controlButton("🎤", "المايك")
        handButton = controlButton("✋", "رفع اليد")

        val giftButton = controlButton("🎁", "هدايا")
        val chatButton = controlButton("💬", "دردشة")
        val exitButton = controlButton("🚪", "خروج")

        val buttons = listOf(
            micButton,
            handButton,
            giftButton,
            chatButton,
            exitButton
        )

        buttons.forEach { button ->
            controls.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(65),
                    1f
                )
            )
        }

        micButton.setOnClickListener {
            micEnabled = !micEnabled
            micButton.text =
                if (micEnabled) "🔊\nالمايك مفتوح"
                else "🎤\nالمايك مغلق"

            Toast.makeText(
                this,
                if (micEnabled) "تم فتح المايك تجريبياً"
                else "تم إغلاق المايك",
                Toast.LENGTH_SHORT
            ).show()
        }

        handButton.setOnClickListener {
            handRaised = !handRaised
            handButton.text =
                if (handRaised) "✋\nتم الرفع"
                else "✋\nرفع اليد"

            Toast.makeText(
                this,
                if (handRaised) "تم إرسال طلب رفع اليد تجريبياً"
                else "تم إلغاء الطلب",
                Toast.LENGTH_SHORT
            ).show()
        }

        giftButton.setOnClickListener {
            Toast.makeText(
                this,
                "نظام الهدايا قيد التطوير 🎁",
                Toast.LENGTH_SHORT
            ).show()
        }

        chatButton.setOnClickListener {
            Toast.makeText(
                this,
                "الدردشة قيد التطوير",
                Toast.LENGTH_SHORT
            ).show()
        }

        exitButton.setOnClickListener {
            finish()
        }

        root.addView(controls)

        setContentView(root)
    }

    private fun seatView(
        symbol: String,
        label: String,
        owner: Boolean
    ): View {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(5), dp(3), dp(5))
            background = rounded(panelColor, 14)
        }

        val avatar = TextView(this).apply {
            text = symbol
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(
                if (owner) purple else Color.rgb(61, 48, 82),
                100
            )
        }

        container.addView(
            avatar,
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        val name = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(if (owner) white else Color.LTGRAY)
            gravity = Gravity.CENTER
            maxLines = 1
        }

        val nameParams = LinearLayout.LayoutParams(
            -1,
            dp(28)
        )
        nameParams.topMargin = dp(4)
        container.addView(name, nameParams)

        container.setOnClickListener {
            Toast.makeText(
                this,
                if (owner) "أنت داخل مقعد المالك"
                else "هذا المقعد متاح للانضمام",
                Toast.LENGTH_SHORT
            ).show()
        }

        return container
    }

    private fun chatMessage(
        username: String,
        message: String,
        nameColor: Int
    ): View {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val name = text("$username: ", 13, nameColor, true)
        row.addView(name)

        row.addView(text(message, 13, white))

        return row
    }

    private fun controlButton(
        emoji: String,
        label: String
    ): TextView {

        return TextView(this).apply {
            text = "$emoji\n$label"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(2), dp(5), dp(2), dp(5))
            background = rounded(panelColor, 13)
        }
    }

    private fun text(
        value: String,
        size: Int,
        color: Int,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
