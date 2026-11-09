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

    private val purple = Color.rgb(151, 91, 255)
    private val pink = Color.rgb(255, 91, 174)
    private val background = Color.rgb(24, 16, 42)
    private val cardColor = Color.rgb(43, 31, 67)
    private val white = Color.WHITE
    private val gray = Color.rgb(185, 174, 205)

    private var microphoneOn = false
    private var handRaised = false

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int,
        stroke: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(151, 91, 255),
                Color.rgb(88, 43, 170),
                Color.rgb(55, 30, 100)
            )
        ).apply {
            cornerRadius = dp(20).toFloat()
        }
    }

    private fun makeText(
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
            typeface = if (bold)
                Typeface.DEFAULT_BOLD
            else
                Typeface.DEFAULT
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = background
        window.navigationBarColor = background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
        }

        val back = makeText("‹", 34f)
        header.addView(back, LinearLayout.LayoutParams(dp(35), dp(45)))

        back.setOnClickListener { finish() }

        val roomInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }

        roomInfo.addView(
            makeText("سهرات مريم 🌙", 17f, white, true)
        )

        roomInfo.addView(
            makeText("ID: 100086  •  ● مباشر", 10f, gray)
        )

        header.addView(
            roomInfo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val share = makeText("↗", 24f)
        header.addView(share, LinearLayout.LayoutParams(dp(42), dp(42)))

        share.setOnClickListener {
            toast("مشاركة الغرفة قريباً")
        }

        val more = makeText("⋮", 25f)
        header.addView(more, LinearLayout.LayoutParams(dp(35), dp(42)))

        more.setOnClickListener {
            toast("إعدادات الغرفة قريباً")
        }

        root.addView(header)

        // SCROLL AREA
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(8), dp(15), dp(15))
        }

        // ROOM BANNER
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient()
            setPadding(dp(15), dp(17), dp(15), dp(17))
        }

        banner.addView(
            makeText("💜", 35f)
        )

        banner.addView(
            makeText("أهلاً بكم في غرفة مريم", 18f, white, true)
        )

        banner.addView(
            makeText("استمتعوا بأجمل الأوقات مع الأصدقاء", 12f, white)
        )

        val bannerParams = LinearLayout.LayoutParams(-1, -2)
        bannerParams.bottomMargin = dp(20)
        content.addView(banner, bannerParams)

        // SEATS HEADER
        val seatsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        seatsHeader.addView(
            makeText("🎙️  المقاعد الصوتية", 17f, white, true),
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        seatsHeader.addView(
            makeText("8 / 8 مقاعد", 11f, gray)
        )

        content.addView(seatsHeader)

        // EIGHT SEATS
        val seats = listOf(
            "مريم" to "م",
            "أحمد" to "أ",
            "سارة" to "س",
            "علي" to "ع",
            "نور" to "ن",
            "محمد" to "م",
            "ليلى" to "ل",
            "ضيف" to "+"
        )

        val seatColors = listOf(
            0xFFFFC4DF.toInt(),
            0xFFD7C5FF.toInt(),
            0xFFFFD8A8.toInt(),
            0xFFBDE7FF.toInt(),
            0xFFD0F5D9.toInt(),
            0xFFFFD0D0.toInt(),
            0xFFE7D0FF.toInt(),
            0xFF44335F.toInt()
        )

        val seatsGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            alignmentMode = GridLayout.ALIGN_MARGINS
            useDefaultMargins = false
        }

        seats.forEachIndexed { index, seat ->

            val seatLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(9), dp(3), dp(9))
            }

            val avatar = TextView(this).apply {
                text = seat.second
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    if (index == 7) white else purple
                )
                gravity = Gravity.CENTER
                background = shape(
                    seatColors[index],
                    50,
                    if (index == 0) pink else 0
                )
            }

            seatLayout.addView(
                avatar,
                LinearLayout.LayoutParams(dp(57), dp(57))
            )

            if (index == 0) {
                val host = makeText("👑 المالك", 9f, pink, true)
                seatLayout.addView(
                    host,
                    LinearLayout.LayoutParams(-1, dp(20))
                )
            } else {
                val mic = makeText(
                    if (index == 7) "+" else "🎤",
                    12f,
                    gray
                )
                seatLayout.addView(
                    mic,
                    LinearLayout.LayoutParams(-1, dp(20))
                )
            }

            seatLayout.addView(
                makeText(
                    seat.first,
                    11f,
                    white,
                    true
                )
            )

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(112)
                columnSpec = GridLayout.spec(
                    index % 4,
                    1f
                )
                rowSpec = GridLayout.spec(index / 4)
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }

            seatsGrid.addView(seatLayout, params)

            seatLayout.setOnClickListener {
                if (index == 7) {
                    toast("طلب الانضمام إلى المقعد قريباً")
                } else {
                    toast("ملف المستخدم: ${seat.first}")
                }
            }
        }

        val gridParams = LinearLayout.LayoutParams(-1, -2)
        gridParams.bottomMargin = dp(12)
        content.addView(seatsGrid, gridParams)

        // AUDIENCE
        val audienceCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(cardColor, 15)
            setPadding(dp(13), dp(13), dp(13), dp(13))
        }

        audienceCard.addView(
            makeText("👥", 22f),
            LinearLayout.LayoutParams(dp(35), dp(35))
        )

        audienceCard.addView(
            makeText("المستمعون", 14f, white, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        audienceCard.addView(
            makeText("24 شخصاً  ›", 12f, gray)
        )

        val audienceParams = LinearLayout.LayoutParams(-1, -2)
        audienceParams.bottomMargin = dp(18)
        content.addView(audienceCard, audienceParams)

        audienceCard.setOnClickListener {
            toast("قائمة المستمعين قريباً")
        }

        // CHAT HEADER
        content.addView(
            makeText(
                "💬  المحادثة",
                16f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        // CHAT MESSAGES
        val chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(cardColor, 15)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        chat.addView(
            makeText(
                "مريم: أهلاً وسهلاً بالجميع 💜",
                12f,
                Color.rgb(255, 199, 225)
            ).apply {
                gravity = Gravity.START
            }
        )

        chat.addView(
            makeText(
                "أحمد: نورتوا الغرفة 🌹",
                12f,
                white
            ).apply {
                gravity = Gravity.START
                setPadding(0, dp(9), 0, 0)
            }
        )

        chat.addView(
            makeText(
                "سارة: مساء الخير ❤️",
                12f,
                white
            ).apply {
                gravity = Gravity.START
                setPadding(0, dp(9), 0, 0)
            }
        )

        val chatParams = LinearLayout.LayoutParams(-1, -2)
        chatParams.bottomMargin = dp(15)
        content.addView(chat, chatParams)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // BOTTOM CONTROLS
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = ColorDrawableCompat(Color.rgb(34, 24, 52))
            setPadding(dp(8), dp(10), dp(8), dp(12))
        }

        fun control(
            icon: String,
            label: String,
            click: () -> Unit
        ): LinearLayout {

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val button = TextView(this).apply {
                text = icon
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(white)
                background = shape(cardColor, 50)
            }

            item.addView(
                button,
                LinearLayout.LayoutParams(dp(49), dp(49))
            )

            item.addView(
                makeText(label, 10f, gray).apply {
                    setPadding(0, dp(5), 0, 0)
                }
            )

            item.setOnClickListener { click() }

            return item
        }

        val micControl = control("🎙️", "المايك") {
            microphoneOn = !microphoneOn
            toast(
                if (microphoneOn)
                    "تم تشغيل المايك (واجهة تجريبية)"
                else
                    "تم إيقاف المايك (واجهة تجريبية)"
            )
        }

        val handControl = control("✋", "رفع اليد") {
            handRaised = !handRaised
            toast(
                if (handRaised)
                    "تم إرسال طلب رفع اليد (تجريبي)"
                else
                    "تم إلغاء رفع اليد"
            )
        }

        val giftControl = control("🎁", "هدايا") {
            toast("الهدايا قريباً")
        }

        val chatControl = control("💬", "دردشة") {
            scroll.post {
                scroll.smoothScrollTo(0, content.bottom)
            }
        }

        val leaveControl = control("🚪", "خروج") {
            finish()
        }

        val items = listOf(
            micControl,
            handControl,
            giftControl,
            chatControl,
            leaveControl
        )

        items.forEach {
            controls.addView(
                it,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
        }

        root.addView(
            controls,
            LinearLayout.LayoutParams(-1, -2)
        )

        setContentView(root)
    }

    private fun ColorDrawableCompat(color: Int) =
        android.graphics.drawable.ColorDrawable(color)
}
