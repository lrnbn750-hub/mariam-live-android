package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class AccountSecurityActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val ink = Color.rgb(42, 45, 51)
    private val muted = Color.rgb(145, 149, 156)
    private val teal = Color.rgb(43, 202, 190)
    private val divider = Color.rgb(231, 232, 235)
    private lateinit var phoneStatus: TextView
    private lateinit var emailStatus: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = 1
            setBackgroundColor(Color.WHITE)
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(7), dp(18), dp(7))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(TextView(this).apply {
            text = "‹"; textSize = 34f; setTextColor(ink); gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(46), dp(52)))
        header.addView(TextView(this).apply {
            text = "إدارة الحساب"; textSize = 20f; setTextColor(ink)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(46), dp(52)))
        root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = 1; setBackgroundColor(Color.WHITE) }
        scroll.addView(body)

        val section = TextView(this).apply {
            text = "ربط الحساب"; textSize = 16f; setTextColor(ink)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(24), 0)
            setBackgroundColor(Color.rgb(245, 246, 250))
        }
        body.addView(section, LinearLayout.LayoutParams(-1, dp(72)))

        phoneStatus = TextView(this).apply {
            text = "جارٍ التحقق..."; textSize = 13f; setTextColor(muted)
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        body.addView(accountRow("الهاتف", "▯", phoneStatus) {
            startActivity(Intent(this, PhoneSecurityActivity::class.java))
        })
        body.addView(noteBanner("✓", "لقد قمت بربط حسابك برقم الهاتف، وأصبح عامل أمان الحساب أفضل.", Color.rgb(226, 250, 248), teal))
        body.addView(separator())

        emailStatus = TextView(this).apply {
            text = "في انتظار الربط"; textSize = 13f; setTextColor(Color.rgb(220, 103, 126))
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        body.addView(accountRow("البريد الإلكتروني", "✉", emailStatus) {
            Toast.makeText(this, "ربط البريد الإلكتروني سيكون متاحاً قريباً", Toast.LENGTH_SHORT).show()
        })
        body.addView(noteBanner("i", "قم بربط بريدك الإلكتروني لتلقي أخبار BAK LIVE وتحديثات الخصوصية والمكافآت.", Color.rgb(255, 250, 239), Color.rgb(239, 177, 52)))
        body.addView(separator())

        body.addView(accountRow("Facebook", "f", null) { comingSoon("Facebook") })
        body.addView(separator())
        body.addView(accountRow("Google", "G", null) { comingSoon("Google") })
        body.addView(separator())
        body.addView(accountRow("X", "𝕏", null) { comingSoon("X") })
        body.addView(separator())
        body.addView(accountRow("Snapchat", "♧", null) { comingSoon("Snapchat") })
        body.addView(separator())
        body.addView(accountRow("VK", "ᴠκ", null) { comingSoon("VK") })
        body.addView(separator())
        body.addView(accountRow("أبل", "●", null) { comingSoon("Apple") })
        body.addView(separator())

        val tipsTitle = TextView(this).apply {
            text = "نصائح للأمان"; textSize = 18f; setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END
            setPadding(dp(20), dp(28), dp(24), dp(12))
        }
        body.addView(tipsTitle)
        body.addView(TextView(this).apply {
            text = "1. لماذا يجب أن أقوم بربط هاتفي المحمول؟\nعندما تقوم بربط رقم هاتفك المحمول، يمكنك تلقي رمز التحقق عند تغيير إعدادات الحساب، كما يساعدك ذلك على استعادة حسابك عند الحاجة.\n\n2. حافظ على سرية كلمة المرور ورمز التحقق، ولا تشاركهما مع أي شخص."
            textSize = 15f; setTextColor(ink); gravity = Gravity.END
            setLineSpacing(dp(5).toFloat(), 1f)
            setPadding(dp(24), 0, dp(24), dp(30))
        })

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadAccountLinks()
    }

    private fun separator() = View(this).apply {
        setBackgroundColor(divider)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1))
    }

    private fun accountRow(title: String, symbol: String, state: TextView?, click: () -> Unit) =
        LinearLayout(this).apply {
            orientation = 0
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
            minimumHeight = dp(72)
            setBackgroundColor(Color.WHITE)

            addView(TextView(this@AccountSecurityActivity).apply {
                text = "‹"; textSize = 28f; setTextColor(Color.rgb(155, 158, 163))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(30), dp(64)))

            if (state != null) addView(state, LinearLayout.LayoutParams(0, dp(64), 1f))
            else addView(View(this@AccountSecurityActivity), LinearLayout.LayoutParams(0, dp(64), 1f))

            addView(TextView(this@AccountSecurityActivity).apply {
                text = title; textSize = 17f; setTextColor(ink)
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            }, LinearLayout.LayoutParams(-2, dp(64)))

            addView(TextView(this@AccountSecurityActivity).apply {
                text = symbol; textSize = 23f; setTextColor(if (title == "الهاتف") teal else muted)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(38), dp(64)))

            isClickable = true
            setOnClickListener { click() }
        }

    private fun noteBanner(icon: String, message: String, backgroundColor: Int, iconColor: Int) =
        LinearLayout(this).apply {
            orientation = 0
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(backgroundColor); cornerRadius = dp(7).toFloat()
            }
            val text = TextView(this@AccountSecurityActivity).apply {
                this.text = message; textSize = 13f; setTextColor(Color.rgb(133, 137, 144))
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            }
            addView(text, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@AccountSecurityActivity).apply {
                this.text = icon; textSize = 17f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(iconColor)
                }
            }, LinearLayout.LayoutParams(dp(26), dp(26)).apply { leftMargin = dp(6) })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(dp(18), dp(8), dp(18), dp(18))
            }
        }

    private fun comingSoon(provider: String) {
        Toast.makeText(this, "ربط $provider سيكون متاحاً قريباً", Toast.LENGTH_SHORT).show()
    }

    private fun loadAccountLinks() {
        val auth = token() ?: run {
            phoneStatus.text = "سجّل الدخول"
            emailStatus.text = "غير مربوط"
            return
        }
        executor.execute {
            try {
                val response = AuthApi.get("/api/auth/me", auth)
                if (response.status !in 200..299) throw Exception("تعذر تحميل بيانات الحساب")
                val user = response.body.optJSONObject("user")
                val phone = user?.optString("phone")?.takeUnless { it.isNullOrBlank() || it == "null" }
                val email = user?.optString("email")?.takeUnless { it.isNullOrBlank() || it == "null" }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (!phone.isNullOrBlank()) {
                        val digits = phone.filter { it.isDigit() }
                        val masked = if (digits.length >= 7) digits.take(2) + "******" + digits.takeLast(4) else phone
                        phoneStatus.text = masked
                        phoneStatus.setTextColor(Color.rgb(125, 132, 137))
                    } else {
                        phoneStatus.text = "غير مربوط • ربط الآن"
                        phoneStatus.setTextColor(Color.rgb(220, 103, 126))
                    }
                    emailStatus.text = if (!email.isNullOrBlank()) "مرتبط" else "في انتظار الربط"
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        phoneStatus.text = "اضغط لإدارة رقم الهاتف"
                        emailStatus.text = "تعذر التحقق"
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
