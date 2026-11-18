package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.view.View

/**
 * Agency dashboard UI shell. Data and invitation actions will be connected to the
 * backend during the server phase; no financial values are authoritative here.
 */
class AgencyDashboardActivity : Activity() {
    private val bg = Color.rgb(12, 10, 20)
    private val panel = Color.rgb(28, 22, 42)
    private val ink = Color.WHITE
    private val muted = Color.rgb(167, 157, 185)
    private val accent = Color.rgb(172, 104, 245)
    private lateinit var body: LinearLayout
    private lateinit var inviteInput: EditText
    private lateinit var inviteList: LinearLayout
    private val pendingIds = linkedSetOf<String>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun card(color: Int = panel, radius: Int = 18) = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat(); setColor(color); setStroke(dp(1), Color.rgb(58, 43, 76))
    }
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun sectionTitle(title: String, subtitle: String = "") {
        body.addView(text(title, 17f, ink, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20); bottomMargin = dp(5) })
        if (subtitle.isNotBlank()) body.addView(text(subtitle, 12f, muted))
    }
    private fun stat(title: String, value: String, note: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(15), dp(15), dp(15)); background = card()
        addView(text(title, 12f, muted, true))
        addView(text(value, 22f, ink, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addView(text(note, 11f, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
    }
    private fun actionButton(title: String, click: () -> Unit) = TextView(this).apply {
        text = title; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        setTextColor(Color.WHITE); setPadding(dp(12), dp(13), dp(12), dp(13))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(145, 69, 214), Color.rgb(91, 45, 149))).apply { cornerRadius = dp(14).toFloat() }
        setOnClickListener { click() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        header.addView(text("‹", 32f, ink, true).apply { gravity = Gravity.CENTER; setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(42), dp(48)))
        header.addView(text("لوحة الوكالة", 19f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(text("BAK", 12f, accent, true))
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(28)) }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        render()
    }

    private fun render() {
        body.removeAllViews()
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(107, 47, 158), Color.rgb(36, 22, 58))).apply { cornerRadius = dp(24).toFloat() }
        }
        hero.addView(text("AGENCY • PARTNER DASHBOARD", 10f, Color.rgb(228, 204, 248), true))
        hero.addView(text("إدارة وكالتك وصُنّاع المحتوى", 19f, Color.WHITE, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        hero.addView(text("تابع دعوات المبدعين وأداء الوكالة من مكان واحد", 12f, Color.rgb(225, 210, 239)), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        body.addView(hero)
        sectionTitle("ملخص الشهر", "ستظهر البيانات الحقيقية بعد ربط خدمات الوكالة")
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(stat("صُنّاع الوكالة", "—", "عدد المرتبطين"), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        stats.addView(stat("ألماس الشهر", "— 💎", "إجمالي الاستلام"), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        body.addView(stats)
        val earnings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = card() }
        earnings.addView(text("حصة الوكالة", 13f, muted, true))
        earnings.addView(text("12%", 30f, Color.rgb(203, 151, 255), true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        earnings.addView(text("المذيع 50%  •  الوكالة 12%  •  التطبيق 38%", 12f, ink))
        earnings.addView(text("الأرباح الشهرية —", 12f, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        body.addView(earnings, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        sectionTitle("دعوة صانع محتوى", "أدخل المعرّف العام للمستخدم فقط")
        inviteInput = EditText(this).apply {
            hint = "معرّف المستخدم (ID)"; singleLine = true; textSize = 15f
            setTextColor(ink); setHintTextColor(muted); gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10)); background = card(Color.rgb(36, 28, 52))
        }
        body.addView(inviteInput, LinearLayout.LayoutParams(-1, dp(52)))
        body.addView(actionButton("إرسال دعوة الوكالة") { addLocalInvitation() }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(9) })
        inviteList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(inviteList, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        renderInvites()

        sectionTitle("تقارير صُنّاع المحتوى", "التفاصيل الشهرية لكل صانع ومجموع الألماس والأرباح")
        val report = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(18), dp(24), dp(18), dp(24)); background = card() }
        report.addView(text("📊", 28f, accent, true).apply { gravity = Gravity.CENTER })
        report.addView(text("لا توجد تقارير متصلة بعد", 14f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        report.addView(text("سيتم عرض أسماء المبدعين، ألماس الشهر، وحصة كل مبدع هنا.", 12f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        body.addView(report)
        val notice = TextView(this).apply {
            text = "هذه واجهة أولية. الدعوات والبيانات المعروضة محلية مؤقتاً ولا تُرسل للخادم أو تُحتسب كأرباح فعلية."
            textSize = 11f; setTextColor(Color.rgb(237, 194, 158)); gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(14)); background = card(Color.rgb(45, 34, 35), 14)
        }
        body.addView(notice, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    }

    private fun addLocalInvitation() {
        val id = inviteInput.text.toString().trim()
        if (id.length < 3) { inviteInput.error = "أدخل معرّداً صحيحاً"; return }
        if (!pendingIds.add(id)) { Toast.makeText(this, "هذه الدعوة موجودة بالقائمة", Toast.LENGTH_SHORT).show(); return }
        inviteInput.setText("")
        renderInvites()
        Toast.makeText(this, "أُضيفت للقائمة التجريبية — لم تُرسل بعد", Toast.LENGTH_LONG).show()
    }
    private fun renderInvites() {
        if (!::inviteList.isInitialized) return
        inviteList.removeAllViews()
        if (pendingIds.isEmpty()) {
            inviteList.addView(text("لا توجد دعوات حالياً", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12) })
            return
        }
        pendingIds.forEach { id ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); background = card() }
            row.addView(text("ID  $id", 14f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text("قيد الإرسال", 11f, Color.rgb(215, 171, 255)))
            row.addView(text("  ×", 18f, Color.rgb(255, 125, 135), true).apply { setOnClickListener { pendingIds.remove(id); renderInvites() } })
            inviteList.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }
}
