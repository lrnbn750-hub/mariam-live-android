package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Private member hub. Access is granted only while the server reports an active SVIP membership. */
class SvipMemberActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val ink = Color.rgb(19, 24, 37)
    private val panel = Color.rgb(31, 40, 59)
    private val gold = Color.rgb(239, 215, 166)
    private val muted = Color.rgb(164, 175, 194)
    private val green = Color.rgb(82, 211, 157)
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) { finish(); return }
        window.statusBarColor = ink
        window.navigationBarColor = ink
        showLoading()
        loadMembership()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = true
        }

    private fun showLoading() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ink)
            addView(label("جارٍ التحقق من عضوية SVIP…", 16f, gold, true))
        }
        setContentView(root)
    }

    private fun loadMembership() {
        io.execute {
            try {
                val member = AuthApi.get("/api/svip/me", token)
                if (member.status !in 200..299 || !member.body.optBoolean("success", false)) {
                    throw IllegalStateException(member.body.optString("error", "تعذر التحقق من العضوية"))
                }
                val subscription = member.body.optJSONObject("subscription")
                val active = subscription != null &&
                    subscription.optBoolean("active", true) &&
                    subscription.optInt("level", 0) in 1..7
                if (!active) {
                    runOnUiThread {
                        if (!isFinishing) {
                            Toast.makeText(this, "هذه الصفحة مخصصة لأعضاء SVIP الفعّالين", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, SvipActivity::class.java))
                            finish()
                        }
                    }
                    return@execute
                }
                val account = AuthApi.get("/api/auth/me", token)
                val user = account.body.optJSONObject("user") ?: account.body
                val publicId = listOf("public_id", "account_id", "id")
                    .firstNotNullOfOrNull { key -> user.optString(key).takeIf { it.isNotBlank() && it != "null" } }
                    .orEmpty()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        renderMemberPage(subscription!!, publicId)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, e.message ?: "تعذر تحميل مركز SVIP", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun renderMemberPage(subscription: JSONObject, publicId: String) {
        val level = subscription.optInt("level", 1).coerceIn(1, 7)
        val expiry = subscription.optString("expires_at", "")
        val date = try {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(expiry.take(19))
            parsed?.let { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) .format(it) } ?: "—"
        } catch (_: Exception) { expiry.take(10).ifBlank { "—" } }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(ink)
            isFillViewport = true
            clipToPadding = false
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(16), dp(18), dp(30))
        }
        scroll.addView(page, ViewGroup.LayoutParams(-1, -2))
        setContentView(scroll)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = label("‹", 34f, gold, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        top.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(label("مركز أعضاء كبار الشخصيات", 18f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        page.addView(top)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg(panel, 22, 0xff5d5360.toInt())
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }
        val crown = label("♛", 48f, gold, true).apply { gravity = Gravity.CENTER }
        hero.addView(crown, LinearLayout.LayoutParams(-1, dp(58)))
        hero.addView(label("SVIP $level", 27f, gold, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("عضويتك مفعّلة", 14f, green, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("رقم الحساب للتوثيق: ${publicId.ifBlank { "غير متوفر" }}", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        hero.addView(label("صالحة حتى $date", 12f, muted).apply { gravity = Gravity.CENTER })
        page.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        page.addView(sectionTitle("امتيازاتك المفعّلة"))
        val perks = listOf(
            1 to "مؤثرات الدخول",
            1 to "شارة SVIP الخاصة بمستواك",
            1 to "زيادة نسبة المتابعة",
            2 to "براويز مميزة",
            2 to "مزيد من الأصدقاء",
            2 to "دعم متخصص",
            2 to "ألوان دردشة مميزة",
            3 to "السيارة المميزة",
            3 to "فقاعات دردشة",
            3 to "بطاقة تعريف مميزة",
            3 to "منع الطرد",
            3 to "منع الكتم من التعليقات",
            4 to "رسائل بارزة في الدردشة",
            6 to "أولوية الظهور",
            6 to "غرف SVIP حصرية"
        ).filter { it.first <= level }
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(panel, 18)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        perks.forEach { (_, name) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
                addView(label("✓", 18f, green, true), LinearLayout.LayoutParams(dp(34), dp(30)))
                addView(label(name, 14f, Color.WHITE), LinearLayout.LayoutParams(0, dp(30), 1f))
                addView(label("مفعّلة", 11f, green, true))
            }
            grid.addView(row)
            val divider = View(this).apply { setBackgroundColor(0xff3b455b.toInt()) }
            grid.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        }
        page.addView(grid, LinearLayout.LayoutParams(-1, -2))

        page.addView(sectionTitle("خدمة كبار الشخصيات"))
        val support = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(panel, 18, 0xff4b665f.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        support.addView(label("دعم واستفسارات أعضاء SVIP", 16f, Color.WHITE, true))
        support.addView(label("استقبال الاستفسارات عبر واتساب على مدار 24 ساعة", 12f, muted).apply {
            setPadding(0, dp(7), 0, dp(12))
        })
        support.addView(label("+964 771 558 8869", 20f, green, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        })
        val verify = label("توثيق رقم العضوية عبر واتساب", 14f, ink, true).apply {
            gravity = Gravity.CENTER
            background = bg(gold, 14)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { openSupportWhatsApp(publicId, level, expiry, true) }
        }
        support.addView(verify, LinearLayout.LayoutParams(-1, dp(48)))
        val contact = label("التواصل مع خدمة SVIP", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(0xff267c61.toInt(), 14)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { openSupportWhatsApp(publicId, level, expiry, false) }
        }
        support.addView(contact, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(10) })
        page.addView(support, LinearLayout.LayoutParams(-1, -2))

        page.addView(label("رقم الحساب أعلاه هو معرّفك داخل BAK LIVE. أرسله للدعم لطلب التحقق من عضويتك. الرد على الرسائل يعتمد على توفر فريق الخدمة.", 11f, muted).apply {
            setPadding(dp(4), dp(12), dp(4), 0)
        })
    }

    private fun sectionTitle(value: String) = label(value, 16f, gold, true).apply {
        setPadding(dp(2), dp(20), dp(2), dp(10))
    }

    private fun openSupportWhatsApp(publicId: String, level: Int, expiry: String, verify: Boolean) {
        val purpose = if (verify) "أرغب بتوثيق رقم حسابي والتحقق من عضوية SVIP." else "لدي استفسار بخصوص خدمة كبار الشخصيات SVIP."
        val message = "مرحباً فريق BAK LIVE،\n$purpose\nرقم الحساب: ${publicId.ifBlank { "غير متوفر" }}\nمستوى العضوية: SVIP $level\nتاريخ الانتهاء: ${expiry.ifBlank { "غير متوفر" }}\nيرجى مساعدتي، شكراً."
        try {
            val url = "https://wa.me/9647715588869?text=" + URLEncoder.encode(message, "UTF-8")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
