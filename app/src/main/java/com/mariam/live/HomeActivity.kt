
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class HomeActivity : Activity() {

    private val purple = Color.rgb(112, 78, 255)
    private val darkPurple = Color.rgb(78, 48, 210)
    private val backgroundColor = Color.rgb(246, 247, 251)
    private val dark = Color.rgb(35, 35, 50)
    private val gray = Color.rgb(135, 137, 153)

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(purple, darkPurple)
        ).apply {
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = dark,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun makeButton(
        value: String,
        color: Int,
        textColor: Int
    ): TextView {
        return text(value, 14f, textColor, true).apply {
            gravity = Gravity.CENTER
            background = rounded(color, 14)
            setPadding(dp(15), dp(10), dp(15), dp(10))
        }
    }

    // فتح غرفة الصوت
    private fun openRoom(roomName: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("room_name", roomName)
        startActivity(intent)
    }

    private fun showProfile(scroll: ScrollView) {
        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(24), dp(20), dp(30))
        }

        profile.addView(text("الملف الشخصي", 24f, dark, true))

        val subtitle = text("إدارة معلومات حسابك في Mariam Live", 13f, gray)
        subtitle.setPadding(0, dp(6), 0, dp(22))
        profile.addView(subtitle)

        val avatar = TextView(this).apply {
            text = "👤"
            textSize = 42f
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(235, 228, 255), 60)
        }
        val avatarParams = LinearLayout.LayoutParams(dp(100), dp(100))
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL
        profile.addView(avatar, avatarParams)

        val prefs = getSharedPreferences("MariamLive", MODE_PRIVATE)
        val displayName = EditText(this).apply {
            hint = "الاسم الظاهر"
            setSingleLine()
            setText(prefs.getString("profile_display_name", ""))
            textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 12)
        }
        val nameParams = LinearLayout.LayoutParams(-1, dp(54))
        nameParams.topMargin = dp(22)
        profile.addView(displayName, nameParams)

        val bio = EditText(this).apply {
            hint = "نبذة عنك"
            minLines = 3
            maxLines = 4
            gravity = Gravity.TOP or Gravity.RIGHT
            setText(prefs.getString("profile_bio", ""))
            textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 12)
        }
        val bioParams = LinearLayout.LayoutParams(-1, dp(110))
        bioParams.topMargin = dp(12)
        profile.addView(bio, bioParams)

        val save = TextView(this).apply {
            text = "حفظ التعديلات"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = gradient()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val saveParams = LinearLayout.LayoutParams(-1, dp(52))
        saveParams.topMargin = dp(18)
        profile.addView(save, saveParams)

        save.setOnClickListener {
            val nameValue = displayName.text.toString().trim()
            val bioValue = bio.text.toString().trim()
            if (nameValue.length > 30 || bioValue.length > 150) {
                Toast.makeText(
                    this,
                    "الاسم بحد أقصى 30 حرفاً والنبذة 150 حرفاً",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("profile_display_name", nameValue)
                .putString("profile_bio", bioValue)
                .apply()
            Toast.makeText(this, "تم حفظ التعديلات على هذا الجهاز", Toast.LENGTH_SHORT).show()
        }

        // تسجيل الخروج أسفل رابط/زر حفظ التعديلات مباشرة
        val logout = TextView(this).apply {
            text = "تسجيل الخروج"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(210, 45, 65))
            background = rounded(Color.rgb(255, 235, 239), 12)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val logoutParams = LinearLayout.LayoutParams(-1, dp(52))
        logoutParams.topMargin = dp(12)
        profile.addView(logout, logoutParams)

        logout.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد تسجيل الخروج من حسابك؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("خروج") { _, _ -> performLogout() }
                .show()
        }

        scroll.removeAllViews()
        scroll.addView(profile)
        scroll.smoothScrollTo(0, 0)
    }

    private fun performLogout() {
        getSharedPreferences("MariamLive", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = Color.WHITE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(20))
        }

        // رأس الصفحة
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "M"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = gradient()
        }

        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        brand.addView(text("Mariam Live", 21f, purple, true))
        brand.addView(text("عالمك.. أصدقاء.. وأصوات", 11f, gray))

        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val notification = TextView(this).apply {
            text = "♧"
            textSize = 25f
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 15)
        }

        header.addView(notification, LinearLayout.LayoutParams(dp(46), dp(46)))

        notification.setOnClickListener {
            Toast.makeText(this, "الإشعارات قريباً", Toast.LENGTH_SHORT).show()
        }

        content.addView(header)

        // الترحيب
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(28), 0, dp(18))
        }

        welcome.addView(text("مرحباً بك في مريم لايف ❤️", 22f, dark, true))
        welcome.addView(
            text("اكتشف أصدقاء جدد وشاركهم أجمل الأوقات", 13f, gray)
        )

        content.addView(welcome)

        // البطاقة الرئيسية
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient()
            setPadding(dp(20), dp(19), dp(20), dp(19))
        }

        banner.addView(text("✨ عالمك يبدأ من هنا", 21f, Color.WHITE, true))

        val bannerDescription = text(
            "ادخل الغرف الصوتية وتعرّف على أشخاص جدد",
            13f,
            Color.WHITE
        )

        bannerDescription.setPadding(0, dp(8), 0, dp(15))
        banner.addView(bannerDescription)

        val discoverButton = makeButton(
            "اكتشف الغرف ←",
            Color.WHITE,
            darkPurple
        )

        banner.addView(
            discoverButton,
            LinearLayout.LayoutParams(dp(145), dp(43))
        )

        discoverButton.setOnClickListener {
            scroll.smoothScrollTo(0, dp(650))
        }

        val bannerParams = LinearLayout.LayoutParams(-1, -2)
        bannerParams.bottomMargin = dp(25)
        content.addView(banner, bannerParams)

        // المستخدمون النشطون
        val activeHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        activeHeader.addView(
            text("🟢 متصلون الآن", 18f, dark, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        activeHeader.addView(text("عرض الكل", 12f, purple, true))
        content.addView(activeHeader)

        val usersScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val usersRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(15), 0, dp(22))
        }

        val users = listOf(
            "أحمد" to "أ",
            "سارة" to "س",
            "علي" to "ع",
            "نور" to "ن",
            "محمد" to "م",
            "ليلى" to "ل"
        )

        val avatarColors = listOf(
            0xFFFFD8E5.toInt(),
            0xFFE2D8FF.toInt(),
            0xFFD6F2FF.toInt(),
            0xFFFFE8C8.toInt(),
            0xFFD8F5E5.toInt(),
            0xFFFFDCCD.toInt()
        )

        users.forEachIndexed { index, user ->
            val userColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val avatar = TextView(this).apply {
                text = user.second
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(purple)
                gravity = Gravity.CENTER
                background = rounded(avatarColors[index], 50)
            }

            userColumn.addView(
                avatar,
                LinearLayout.LayoutParams(dp(57), dp(57))
            )

            userColumn.addView(
                text(user.first, 12f, dark, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                }
            )

            usersRow.addView(
                userColumn,
                LinearLayout.LayoutParams(dp(68), -2)
            )
        }

        usersScroll.addView(usersRow)
        content.addView(usersScroll)

        // عنوان الغرف
        val roomHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        roomHeader.addView(
            text("🎙️ الغرف الصوتية", 19f, dark, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        roomHeader.addView(text("عرض الكل ←", 12f, purple, true))
        content.addView(roomHeader)

        // الغرف الصوتية
        val rooms = listOf(
            Triple("سهرات مريم 🌙", "دردشة وأصدقاء من كل مكان", "12"),
            Triple("قهوة الصباح ☕", "تعرف على أصدقاء جدد", "8"),
            Triple("عالم الموسيقى 🎵", "أجواء جميلة ومحادثات", "6"),
            Triple("أصدقاء للأبد 💜", "شاركنا الحديث والضحك", "15")
        )

        rooms.forEachIndexed { index, room ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(Color.WHITE, 19)
                setPadding(dp(15), dp(14), dp(15), dp(14))
            }

            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val roomIcon = TextView(this).apply {
                text = listOf("🌙", "☕", "🎵", "💜")[index]
                textSize = 25f
                gravity = Gravity.CENTER
                background = rounded(
                    listOf(
                        0xFFF0EAFF.toInt(),
                        0xFFFFF0DD.toInt(),
                        0xFFE6F5FF.toInt(),
                        0xFFFFEAF2.toInt()
                    )[index],
                    14
                )
            }

            top.addView(
                roomIcon,
                LinearLayout.LayoutParams(dp(53), dp(53))
            )

            val roomInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }

            roomInfo.addView(text(room.first, 15f, dark, true))

            roomInfo.addView(
                text(room.second, 11f, gray).apply {
                    setPadding(0, dp(5), 0, 0)
                }
            )

            top.addView(roomInfo, LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(text("● مباشر", 11f, Color.rgb(235, 65, 100), true))

            card.addView(top)

            val separator = View(this).apply {
                setBackgroundColor(Color.rgb(240, 240, 246))
            }

            val sepParams = LinearLayout.LayoutParams(-1, dp(1))
            sepParams.topMargin = dp(13)
            sepParams.bottomMargin = dp(11)
            card.addView(separator, sepParams)

            val bottom = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            bottom.addView(
                text("👥 ${room.third} مشارك", 12f, gray),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val join = makeButton("دخول الغرفة", purple, Color.WHITE)
            bottom.addView(join)

            // التعديل المهم: فتح غرفة الصوت بدلاً من رسالة قريباً
            join.setOnClickListener {
                openRoom(room.first)
            }

            card.addView(bottom)

            val cardParams = LinearLayout.LayoutParams(-1, -2)
            cardParams.topMargin = dp(12)
            content.addView(card, cardParams)
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // شريط التنقل السفلي
        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(12).toFloat()
            setPadding(0, dp(9), 0, dp(8))
        }

        val navItems = listOf(
            "⌂" to "الرئيسية",
            "◉" to "اكتشف",
            "▤" to "الرسائل",
            "♡" to "الأصدقاء",
            "♙" to "حسابي"
        )

        navItems.forEachIndexed { index, item ->

            val selected = index == 0

            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, dp(3))
            }

            itemLayout.addView(
                text(
                    item.first,
                    25f,
                    if (selected) purple else gray,
                    true
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            itemLayout.addView(
                text(
                    item.second,
                    10f,
                    if (selected) purple else gray,
                    selected
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(3), 0, 0)
                }
            )

            itemLayout.setOnClickListener {
                if (index == 0) {
                    scroll.removeAllViews()
                    scroll.addView(content)
                    scroll.smoothScrollTo(0, 0)
                } else if (index == 4) {
                    showProfile(scroll)
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "${item.second} قريباً",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            navigation.addView(
                itemLayout,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
        }

        root.addView(
            navigation,
            LinearLayout.LayoutParams(-1, dp(67))
        )

        setContentView(root)
    }
}
