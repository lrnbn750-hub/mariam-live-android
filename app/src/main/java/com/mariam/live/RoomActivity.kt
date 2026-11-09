
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView

class RoomActivity : Activity() {

    private val bg = Color.rgb(13, 7, 24)
    private val purple = Color.rgb(145, 65, 255)
    private val pink = Color.rgb(218, 55, 155)
    private val gold = Color.rgb(255, 215, 0)
    private val panel = Color.rgb(42, 30, 61)

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun shape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(85, 35, 145), Color.rgb(35, 18, 58))
        ).apply {
            cornerRadius = dp(22).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = Color.WHITE,
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
        icon: String,
        label: String,
        color: Int,
        action: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(color, 18)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(text(icon, 23f))
            addView(text(label, 12f))
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(20), dp(14), dp(20))
        }

        // الشريط العلوي
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val roomTitle = text("MARIAM LIVE", 20f, Color.WHITE, true)
        header.addView(roomTitle, LinearLayout.LayoutParams(0, dp(55), 1f))

        val close = text("✕", 25f).apply {
            background = shape(Color.rgb(55, 45, 65), 50)
            setOnClickListener { finish() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)

        // معلومات الغرفة
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = gradient()
            setPadding(dp(14), dp(15), dp(14), dp(15))
        }

        info.addView(text("🔥 32.6K", 13f, gold))
        info.addView(text("  💎 09:28  ", 13f, gold))
        info.addView(text("🏆 الغرفة الرئيسية", 14f))
        root.addView(info, LinearLayout.LayoutParams(-1, dp(58)).apply {
            topMargin = dp(12)
        })

        // المسرح
        val stage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient()
            setPadding(dp(12), dp(20), dp(12), dp(18))
        }

        val versus = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val host = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        host.addView(text("👑", 38f))
        host.addView(text("Mariam", 15f, Color.WHITE, true))
        host.addView(text("🎁 3.2M", 12f, gold))

        val opponent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        opponent.addView(text("🛡️", 38f))
        opponent.addView(text("أمير", 15f, Color.WHITE, true))
        opponent.addView(text("🎁 2.1M", 12f, gold))

        versus.addView(host, LinearLayout.LayoutParams(0, -2, 1f))
        versus.addView(text("VS", 20f, gold, true))
        versus.addView(opponent, LinearLayout.LayoutParams(0, -2, 1f))
        stage.addView(versus)

        stage.addView(text("مقاعد المتحدثين", 16f, Color.WHITE, true).apply {
            setPadding(0, dp(24), 0, dp(12))
        })

        // ثمانية مقاعد
        val seats = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
        }

        for (i in 1..8) {
            val locked = i == 4 || i == 6 || i == 8

            val seat = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(
                    if (locked) Color.rgb(27, 19, 42) else panel,
                    18
                )
                setPadding(dp(4), dp(12), dp(4), dp(12))

                addView(text(if (locked) "🔒" else "＋", 25f))
                addView(text("مقعد $i", 11f, Color.LTGRAY))
                setOnClickListener {
                    Toast.makeText(
                        this@RoomActivity,
                        if (locked) "المقعد مغلق" else "طلب الانضمام للمقعد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(92)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            seats.addView(seat, params)
        }

        stage.addView(seats)
        root.addView(stage, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(14)
        })

        // الإعلان
        val announcement = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.rgb(65, 35, 100), 16)
            setPadding(dp(15), dp(12), dp(15), dp(12))
            addView(text("📢 إعلان الغرفة", 13f, gold, true))
            addView(text("أهلاً وسهلاً بكم في Mariam Live", 14f))
        }
        root.addView(announcement, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(14)
        })

        // المحادثة
        root.addView(text("المحادثة", 18f, Color.WHITE, true).apply {
            gravity = Gravity.START
            setPadding(0, dp(20), 0, dp(10))
        })

        val chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.rgb(31, 23, 46), 16)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        chat.addView(text("👑 Mariam: أهلاً وسهلاً بالجميع ❤️", 14f))
        chat.addView(text("🎧 مرحباً بالجميع!", 14f, Color.LTGRAY).apply {
            setPadding(0, dp(10), 0, dp(4))
        })
        root.addView(chat)

        // حقل الرسالة
        val messageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val input = EditText(this).apply {
            hint = "اكتب رسالة..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            background = shape(panel, 30)
            setPadding(dp(15), 0, dp(15), 0)
        }

        messageRow.addView(input, LinearLayout.LayoutParams(0, dp(50), 1f))

        val send = text("إرسال", 14f, Color.WHITE, true).apply {
            background = shape(purple, 30)
            setPadding(dp(15), dp(14), dp(15), dp(14))
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    Toast.makeText(
                        this@RoomActivity,
                        "تم تجهيز الرسالة للربط بالسيرفر",
                        Toast.LENGTH_SHORT
                    ).show()
                    input.text.clear()
                }
            }
        }
        messageRow.addView(send, LinearLayout.LayoutParams(-2, -2).apply {
            marginStart = dp(8)
        })
        root.addView(messageRow, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
        })

        // أدوات التحكم
        root.addView(text("أدوات التحكم", 18f, Color.WHITE, true).apply {
            gravity = Gravity.START
            setPadding(0, dp(22), 0, dp(12))
        })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val mic = button("🎙️", "الميكروفون", pink) {
            Toast.makeText(this@RoomActivity, "الميكروفون يحتاج ربط صوت حقيقي", Toast.LENGTH_SHORT).show()
        }

        val sound = button("🔊", "الصوت", panel) {
            Toast.makeText(this@RoomActivity, "زر الصوت", Toast.LENGTH_SHORT).show()
        }

        val gift = button("🎁", "إرسال هدية", purple) {
            Toast.makeText(this@RoomActivity, "نظام الهدايا يحتاج ربط السيرفر", Toast.LENGTH_SHORT).show()
        }

        controls.addView(mic, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })
        controls.addView(sound, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })
        controls.addView(gift, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })

        root.addView(controls)

        scroll.addView(root)
        setContentView(scroll)
    }
}
