package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.cardview.widget.CardView

class RoomActivity : Activity() {

    // الألوان المستخدمة في التصميم
    private val bgColor = Color.parseColor("#0D0714")
    private val purpleGlow = Color.parseColor("#9141FF")
    private val pinkGlow = Color.parseColor("#DA379B")
    private val goldColor = Color.parseColor("#FFD700")
    private val darkBox = Color.parseColor("#2A1E3D")
    private val lightText = Color.parseColor("#E0E0E0")

    private fun createGradient(color1: Int, color2: Int, radius: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(color1, color2)).apply {
            cornerRadius = radius
            if (strokeColor != null) setStroke(2, strokeColor)
        }
    }

    private fun createSolidShape(color: Int, radius: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeColor != null) setStroke(2, strokeColor)
        }
    }

    private fun createText(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // الحاوية الرئيسية
        val root = ConstraintLayout(this).apply {
            setBackgroundColor(bgColor)
            setPadding(24, 40, 24, 24)
        }

        // 1. الشريط العلوي (Header)
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            id = View.generateViewId()
        }

        val userInfo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createGradient(Color.parseColor("#4A148C"), Color.parseColor("#7B1FA2"), 50f)
            setPadding(16, 8, 16, 8)
        }
        
        val crownIcon = createText("👑", 20f)
        userInfo.addView(crownIcon)
        
        val userDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 0, 0, 0)
        }
        userDetails.addView(createText("MARIAM LIVE ✔", 14f, Color.WHITE, true))
        userDetails.addView(createText("ID: 100886", 10f, lightText))
        userInfo.addView(userDetails)

        headerLayout.addView(userInfo, LinearLayout.LayoutParams(0, -2, 1f))

        val topIcons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topIcons.addView(createText("👥 123", 14f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#332244"), 20f)
            setPadding(16, 8, 16, 8)
        })
        
        val closeBtn = createText("✕", 20f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#332244"), 50f)
            setPadding(16, 8, 16, 8)
            setOnClickListener { finish() }
        }
        val closeParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = 8 }
        topIcons.addView(closeBtn, closeParams)

        headerLayout.addView(topIcons)
        root.addView(headerLayout)

        // 2. معلومات الغرفة (Room Info)
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createSolidShape(Color.parseColor("#1A1128"), 25f)
            setPadding(16, 8, 16, 8)
            id = View.generateViewId()
        }
        
        infoLayout.addView(createText("🔥 32.6K", 12f, Color.parseColor("#FF5252")))
        
        val timer = createText("💎 09:28", 12f, goldColor).apply {
            setPadding(16, 0, 16, 0)
        }
        infoLayout.addView(timer)
        
        infoLayout.addView(createText("🏆 متصدر الغرفة", 12f, lightText))

        // 3. المسرح (Stage) - يحتوي على الصورة والخلفية
        val stageLayout = ConstraintLayout(this).apply {
            id = View.generateViewId()
            background = createGradient(Color.parseColor("#2D143D"), Color.parseColor("#0D0714"), 30f, Color.parseColor("#5A2D6B"))
            setPadding(16, 16, 16, 16)
        }

        // محاكاة صورة السيارة في المنتصف (استخدم صورة حقيقية هنا)
        val carImagePlaceholder = View(this).apply {
            // في حال وجود صورة: setBackgroundResource(R.drawable.your_car_image)
            background = createGradient(Color.parseColor("#FF5722"), Color.parseColor("#9C27B0"), 20f)
            alpha = 0.3f
        }
        stageLayout.addView(carImagePlaceholder, ConstraintLayout.LayoutParams(0, 200).apply {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        })

        // المضيف (الملكة)
        val hostLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            id = View.generateViewId()
        }
        
        val hostAvatar = TextView(this).apply {
            text = "👩🏻"
            textSize = 40f
            gravity = Gravity.CENTER
            background = createGradient(purpleGlow, pinkGlow, 100f, goldColor)
            val params = LinearLayout.LayoutParams(100, 100)
            layoutParams = params
        }
        hostLayout.addView(hostAvatar)
        
        val hostBadge = createText("المضيف", 12f, Color.WHITE).apply {
            background = createSolidShape(purpleGlow, 20f)
            setPadding(16, 4, 16, 4)
            val params = LinearLayout.LayoutParams(-2, -2).apply { topMargin = -10 }
            layoutParams = params
        }
        hostLayout.addView(hostBadge)
        hostLayout.addView(createText("Mariam 💜", 14f, Color.WHITE, true))
        hostLayout.addView(createText("🎁 3.2M", 12f, goldColor))

        stageLayout.addView(hostLayout, ConstraintLayout.LayoutParams(-2, -2).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 16
        })

        // المنافس (الدرع)
        val opponentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        opponentLayout.addView(createText("🛡️", 60f)) // استبدلها بصورة الدرع
        opponentLayout.addView(createText("✨ أمير", 14f, Color.WHITE, true))
        opponentLayout.addView(createText("🎁 2.1M", 12f, goldColor))

        stageLayout.addView(opponentLayout, ConstraintLayout.LayoutParams(-2, -2).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 16
            marginEnd = 16
        })

        // 4. شبكة المقاعد (المقاعد 3 إلى 10)
        val seatsContainer = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = true
            id = View.generateViewId()
        }

        for (i in 3..10) {
            val isLocked = (i == 4 || i == 6 || i == 8 || i == 10)
            
            val seatCard = CardView(this).apply {
                radius = 50f
                cardElevation = 0f
                setCardBackgroundColor(if (isLocked) Color.parseColor("#1A1128") else Color.parseColor("#2A1E3D"))
                
                val seatContent = LinearLayout(this@RoomActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    
                    if (isLocked) {
                        addView(createText("🔒", 24f, lightText))
                        if (i == 4) { // علامة التاج للمقعد الرابع
                             addView(createText("👑", 12f, purpleGlow).apply { translationY = -10f })
                        }
                    } else {
                        addView(createText("➕", 24f, Color.WHITE))
                    }
                    addView(createText("$i", 12f, lightText))
                }
                addView(seatContent)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 150
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            seatsContainer.addView(seatCard, params)
        }

        stageLayout.addView(seatsContainer, ConstraintLayout.LayoutParams(0, -2).apply {
            topToBottom = hostLayout.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 32
        })

        // 5. شريط الإعلانات (Announcement)
        val announcementLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createGradient(Color.parseColor("#4A148C"), Color.parseColor("#7B1FA2"), 25f)
            setPadding(24, 12, 24, 12)
            id = View.generateViewId()
        }
        announcementLayout.addView(createText("📢 إعلان الغرفة", 12f, goldColor, true).apply { gravity = Gravity.START })
        announcementLayout.addView(createText("🤍 مرحباً بكم في غرفة Mariam Live 🤍", 14f, Color.WHITE).apply { gravity = Gravity.START })
        
        // 6. الدردشة (Chat)
        val chatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            id = View.generateViewId()
        }
        
        chatLayout.addView(createChatMessage("👩🏻 Mariam: أهلاً وسهلاً بالجميع ❤️", Color.parseColor("#9141FF")))
        chatLayout.addView(createChatMessage("👨🏻 أحمد: مرحباً بالجميع!", Color.parseColor("#00BCD4")))
        chatLayout.addView(createChatMessage("👩🏻 سارة: شو أخباركم اليوم؟", Color.parseColor("#00BCD4")))

        // 7. شريط الرسائل (Message Bar)
        val messageBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createSolidShape(Color.parseColor("#1A1128"), 30f)
            setPadding(16, 8, 16, 8)
            id = View.generateViewId()
        }
        
        val menuIcon = createText("☰", 24f, Color.WHITE)
        messageBar.addView(menuIcon)
        
        val editText = EditText(this).apply {
            hint = "اكتب رسالة..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setSingleLine(true)
            val params = LinearLayout.LayoutParams(0, -2, 1f)
            params.marginStart = 16
            params.marginEnd = 16
            layoutParams = params
        }
        messageBar.addView(editText)
        
        messageBar.addView(createText("😊", 24f, Color.WHITE))
        messageBar.addView(createText("🎁", 24f, pinkGlow).apply { setPadding(16, 0, 16, 0) })
        messageBar.addView(createText("⊞", 24f, Color.WHITE))

        // 8. أزرار التحكم السفلية (أدوات الصوت)
        val bottomControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            id = View.generateViewId()
        }

        val micBtn = createText("🎙️", 24f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#DA379B"), 50f)
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                Toast.makeText(this@RoomActivity, "المايك مكتوم", Toast.LENGTH_SHORT).show()
            }
        }
        
        val soundBtn = createText("🔊", 24f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#1A1128"), 50f)
            setPadding(24, 24, 24, 24)
        }

        val giftBtn = createText("🎁", 24f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#9141FF"), 50f)
            setPadding(24, 24, 24, 24)
        }

        bottomControls.addView(micBtn)
        bottomControls.addView(soundBtn, LinearLayout.LayoutParams(-2, -2).apply { marginStart = 16, marginEnd = 16 })
        bottomControls.addView(giftBtn)


        // ===== إضافة العناصر إلى ConstraintLayout الرئيسي =====
        root.addView(infoLayout, ConstraintLayout.LayoutParams(0, -2).apply {
            topToBottom = headerLayout.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 16
        })

        root.addView(stageLayout, ConstraintLayout.LayoutParams(0, 0).apply {
            topToBottom = infoLayout.id
            bottomToTop = messageBar.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 16
            bottomMargin = 16
        })

        root.addView(announcementLayout, ConstraintLayout.LayoutParams(0, -2).apply {
            bottomToTop = chatLayout.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = 8
        })

        root.addView(chatLayout, ConstraintLayout.LayoutParams(0, -2).apply {
            bottomToTop = messageBar.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = 8
        })

        root.addView(messageBar, ConstraintLayout.LayoutParams(0, 60).apply {
            bottomToTop = bottomControls.id
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = 16
        })

        root.addView(bottomControls, ConstraintLayout.LayoutParams(-2, -2).apply {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        })

        setContentView(root)
    }

    // دالة مساعدة لإنشاء رسالة دردشة
    private fun createChatMessage(text: String, badgeColor: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
        }
        
        val avatar = createText("👤", 14f, Color.WHITE).apply {
            background = createSolidShape(badgeColor, 50f)
            setPadding(8, 4, 8, 4)
        }
        
        val messageText = createText(text, 14f, Color.WHITE).apply {
            background = createSolidShape(Color.parseColor("#1A1128"), 20f)
            setPadding(16, 8, 16, 8)
            gravity = Gravity.START
        }
        
        row.addView(avatar)
        row.addView(messageText, LinearLayout.LayoutParams(-2, -2).apply { marginStart = 8 })
        
        return row
    }
}
