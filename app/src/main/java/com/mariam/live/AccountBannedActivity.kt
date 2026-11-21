package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale

class AccountBannedActivity : Activity() {

    // ===== الألوان =====
    private val cBg = Color.rgb(244, 245, 250)
    private val cPurple = Color.rgb(87, 64, 180)
    private val cTeal = Color.rgb(27, 155, 139)
    private val cRed = Color.rgb(200, 52, 64)
    private val cOrange = Color.rgb(224, 138, 30)
    private val cGreen = Color.rgb(32, 160, 98)
    private val cTitle = Color.rgb(30, 34, 48)
    private val cBody = Color.rgb(82, 87, 101)
    private val cMuted = Color.rgb(119, 126, 143)

    // ===== حالة الحظر =====
    private var permanent = false
    private var startMillis: Long? = null
    private var endMillis: Long = 0L
    private var expired = false

    // ===== عناصر تتحدث كل ثانية =====
    private lateinit var tvDays: TextView
    private lateinit var tvHours: TextView
    private lateinit var tvMinutes: TextView
    private lateinit var tvSeconds: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvButton: TextView
    private lateinit var progressFill: View
    private lateinit var progressRest: View

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (updateCountdown()) handler.postDelayed(this, 1000)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    // ===== أدوات التاريخ والوقت =====
    private fun parseMillis(value: String): Long? {
        if (value.isBlank() || value == "null") return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (_: Exception) {
                value.toLongOrNull()?.let { if (it < 100_000_000_000L) it * 1000 else it }
            }
        }
    }

    private fun formatDate(millis: Long?): String {
        if (millis == null) return "غير متوفر"
        return SimpleDateFormat("yyyy/MM/dd  •  HH:mm", Locale.ENGLISH).format(Date(millis))
    }

    private fun arUnit(n: Long, one: String, two: String, few: String, many: String) = when {
        n == 1L -> one
        n == 2L -> two
        n in 3..10 -> "$n $few"
        else -> "$n $many"
    }

    private fun durationText(ms: Long): String {
        val totalMin = ms / 60_000
        val days = totalMin / 1440
        val hours = (totalMin % 1440) / 60
        val mins = totalMin % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts += arUnit(days, "يوم", "يومان", "أيام", "يوماً")
        if (hours > 0) parts += arUnit(hours, "ساعة", "ساعتان", "ساعات", "ساعة")
        if (days == 0L && mins > 0) parts += arUnit(mins, "دقيقة", "دقيقتان", "دقائق", "دقيقة")
        return if (parts.isEmpty()) "أقل من دقيقة" else parts.joinToString(" و ")
    }

    private fun two(n: Long) = String.format(Locale.US, "%02d", n)

    // ===== أدوات الواجهة =====
    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.CENTER
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        this.gravity = gravity
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.15f)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun chip(value: String, color: Int) = TextView(this).apply {
        text = value
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(5), dp(14), dp(5))
        background = rounded(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), 20)
    }

    private fun setChip(tv: TextView, value: String, color: Int) {
        tv.text = value
        tv.setTextColor(color)
        tv.background = rounded(
            Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), 20
        )
    }

    private fun timeBox(caption: String, parent: LinearLayout): TextView {
        val number = TextView(this).apply {
            text = "00"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val cap = TextView(this).apply {
            text = caption
            textSize = 11f
            setTextColor(Color.argb(210, 255, 255, 255))
            gravity = Gravity.CENTER
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(cPurple, Color.rgb(64, 96, 190))
            ).apply { cornerRadius = dp(14).toFloat() }
            addView(number)
            addView(cap)
        }
        parent.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(4); marginEnd = dp(4)
        })
        return number
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تسجيل الخروج ومسح بيانات الجلسة
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .remove("token").remove("user_id").remove("username").remove("phone").remove("email")
            .remove("active_room_id").apply()

        window.statusBarColor = cBg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // ===== قراءة البيانات =====
        val reason = intent.getStringExtra("reason").orEmpty()
            .ifBlank { "مخالفة معايير المجتمع وشروط الاستخدام" }
        val type = intent.getStringExtra("ban_type").orEmpty()
        val startedRaw = intent.getStringExtra("started_at").orEmpty()
        val expiresRaw = intent.getStringExtra("expires_at").orEmpty()

        startMillis = parseMillis(startedRaw)
        val endParsed = parseMillis(expiresRaw)
        permanent = type.equals("permanent", true) || endParsed == null
        endMillis = endParsed ?: 0L

        val totalDuration = if (permanent) "حظر دائم" else {
            val s = startMillis
            if (s != null && endMillis > s) durationText(endMillis - s) else "حظر مؤقت"
        }

        // ===== الهيكل الأساسي =====
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(cBg)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(26), dp(20), dp(22))
            background = rounded(Color.WHITE, 26, Color.rgb(232, 233, 241))
            elevation = dp(4).toFloat()
        }

        // ===== الرأس =====
        card.addView(label("BAK LIVE", 13f, cPurple, true))

        val iconCircle = TextView(this).apply {
            text = "!"
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cRed)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(253, 236, 238))
                setStroke(dp(2), Color.rgb(246, 200, 205))
            }
        }
        card.addView(iconCircle, LinearLayout.LayoutParams(dp(78), dp(78)).apply {
            topMargin = dp(12)
        })

        tvTitle = label("تم تقييد حسابك", 24f, cRed, true)
        card.addView(tvTitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        tvStatus = chip(if (permanent) "حظر دائم" else "حظر مؤقت", if (permanent) cRed else cOrange)
        card.addView(tvStatus, LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = dp(4); bottomMargin = dp(6)
        })

        card.addView(
            label(
                "تم اتخاذ إجراء بحق حسابك بسبب مخالفة معايير المجتمع وشروط الاستخدام في BAK LIVE.",
                14f, cBody
            )
        )

        // ===== قسم العد التنازلي =====
        val countdownBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(16), dp(12), dp(16))
            background = rounded(Color.rgb(247, 248, 253), 20, Color.rgb(233, 235, 245))
        }
        if (permanent) {
            countdownBox.addView(label("لا يوجد موعد لفك الحظر", 16f, cRed, true))
            countdownBox.addView(
                label(
                    "هذا الحظر دائم. يمكنك التواصل مع فريق الدعم لطلب مراجعة القرار.",
                    13f, cBody
                )
            )
        } else {
            countdownBox.addView(label("الوقت المتبقي لفك الحظر", 13f, cMuted, true))
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                weightSum = 4f
            }
            tvDays = timeBox("يوم", row)
            tvHours = timeBox("ساعة", row)
            tvMinutes = timeBox("دقيقة", row)
            tvSeconds = timeBox("ثانية", row)
            countdownBox.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })

            // شريط التقدم
            val track = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                weightSum = 100f
                background = rounded(Color.rgb(226, 228, 240), 6)
                clipToOutline = true
            }
            progressFill = View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.RIGHT_LEFT,
                    intArrayOf(cPurple, cTeal)
                )
            }
            progressRest = View(this)
            track.addView(progressFill, LinearLayout.LayoutParams(0, -1, 0f))
            track.addView(progressRest, LinearLayout.LayoutParams(0, -1, 100f))
            countdownBox.addView(track, LinearLayout.LayoutParams(-1, dp(8)).apply {
                topMargin = dp(16)
            })
            tvProgress = label("", 12f, cMuted)
            countdownBox.addView(tvProgress)
        }
        card.addView(countdownBox, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(14)
        })

        // ===== التفاصيل =====
        fun detail(title: String, value: String, valueColor: Int = cTitle) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                background = rounded(Color.rgb(247, 248, 252), 14)
            }
            box.addView(label(title, 12f, cMuted, true, Gravity.START))
            box.addView(label(value, 15f, valueColor, true, Gravity.START))
            card.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
        card.addView(View(this), LinearLayout.LayoutParams(-1, dp(6)))
        detail("سبب الحظر", reason)
        detail("مدة الحظر", totalDuration)
        detail("تاريخ الحظر", formatDate(startMillis))
        detail(
            "تاريخ انتهاء الحظر",
            if (permanent) "لا يوجد — حظر دائم" else formatDate(endMillis)
        )

        // ===== الملاحظات =====
        card.addView(
            label(
                "خلال فترة الحظر لن تتمكن من استخدام حسابك. نرجو الالتزام بقوانين التطبيق واحترام جميع المستخدمين لتجنب إجراءات إضافية.",
                13f, cBody
            ).apply { setPadding(0, dp(16), 0, dp(6)) }
        )
        card.addView(
            label(
                "إذا كنت تعتقد أن القرار صدر عن طريق الخطأ، تواصل مع فريق دعم BAK LIVE لطلب المراجعة.",
                13f, cBody
            )
        )
        card.addView(
            label("BAK LIVE — مجتمعنا يجمعنا بالاحترام", 12f, Color.rgb(110, 101, 151), true)
                .apply { setPadding(0, dp(14), 0, 0) }
        )

        scroll.addView(card, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ===== الزر =====
        tvButton = TextView(this).apply {
            text = "العودة إلى تسجيل الدخول"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cPurple, cTeal)
            ).apply { cornerRadius = dp(16).toFloat() }
            setOnClickListener { finish() }
        }
        root.addView(tvButton, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(12)
        })

        setContentView(root)
    }

    /** يحدّث العد التنازلي. يرجع true إذا لازم يكمل التحديث كل ثانية. */
    private fun updateCountdown(): Boolean {
        if (permanent) return false

        val now = System.currentTimeMillis()
        val remaining = endMillis - now
        if (remaining <= 0) {
            showExpired()
            return false
        }

        val total = remaining / 1000
        tvDays.text = two(total / 86_400)
        tvHours.text = two((total % 86_400) / 3_600)
        tvMinutes.text = two((total % 3_600) / 60)
        tvSeconds.text = two(total % 60)

        val s = startMillis
        val pct = if (s != null && endMillis > s) {
            ((now - s) * 100 / (endMillis - s)).coerceIn(0, 100)
        } else 0L
        (progressFill.layoutParams as LinearLayout.LayoutParams).weight = pct.toFloat()
        (progressRest.layoutParams as LinearLayout.LayoutParams).weight = (100 - pct).toFloat()
        progressFill.requestLayout()
        tvProgress.text = "انقضى $pct% من مدة الحظر"
        return true
    }

    private fun showExpired() {
        if (expired) return
        expired = true
        tvDays.text = "00"; tvHours.text = "00"; tvMinutes.text = "00"; tvSeconds.text = "00"
        (progressFill.layoutParams as LinearLayout.LayoutParams).weight = 100f
        (progressRest.layoutParams as LinearLayout.LayoutParams).weight = 0f
        progressFill.requestLayout()
        tvProgress.text = "انتهت مدة الحظر"
        tvTitle.text = "انتهت مدة الحظر"
        tvTitle.setTextColor(cGreen)
        setChip(tvStatus, "يمكنك الآن تسجيل الدخول", cGreen)
        tvButton.text = "تسجيل الدخول"
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(ticker)
        if (updateCountdown()) handler.postDelayed(ticker, 1000)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBackPressed() {
        finish()
    }
}