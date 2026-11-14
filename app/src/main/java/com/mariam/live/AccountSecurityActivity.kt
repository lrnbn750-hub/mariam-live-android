package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class AccountSecurityActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val green = Color.rgb(20, 145, 83)
    private val ink = Color.rgb(27, 42, 34)
    private val muted = Color.rgb(112, 128, 118)
    private val page = Color.rgb(246, 249, 247)
    private lateinit var phoneValue: TextView
    private lateinit var status: TextView
    private var selectedCountry = Country("IQ", "العراق", "+964", "🇮🇶")
    private var countryDialog: AlertDialog? = null
    private data class Country(val iso: String, val name: String, val dial: String, val flag: String)
    private val codes = """AF:93 AL:355 DZ:213 AS:1684 AD:376 AO:244 AI:1264 AG:1268 AR:54 AM:374 AW:297 AU:61 AT:43 AZ:994 BS:1242 BH:973 BD:880 BB:1246 BY:375 BE:32 BZ:501 BJ:229 BM:1441 BT:975 BO:591 BA:387 BW:267 BR:55 BN:673 BG:359 BF:226 BI:257 KH:855 CM:237 CA:1 CV:238 KY:1345 CF:236 TD:235 CL:56 CN:86 CO:57 KM:269 CG:242 CD:243 CR:506 CI:225 HR:385 CU:53 CY:357 CZ:420 DK:45 DJ:253 DM:1767 DO:1809 EC:593 EG:20 SV:503 GQ:240 ER:291 EE:372 SZ:268 ET:251 FJ:679 FI:358 FR:33 GA:241 GM:220 GE:995 DE:49 GH:233 GR:30 GD:1473 GU:1671 GT:502 GN:224 GW:245 GY:592 HT:509 HN:504 HK:852 HU:36 IS:354 IN:91 ID:62 IR:98 IQ:964 IE:353 IL:972 IT:39 JM:1876 JP:81 JO:962 KZ:7 KE:254 KI:686 KP:850 KR:82 KW:965 KG:996 LA:856 LV:371 LB:961 LS:266 LR:231 LY:218 LI:423 LT:370 LU:352 MO:853 MG:261 MW:265 MY:60 MV:960 ML:223 MT:356 MH:692 MR:222 MU:230 MX:52 FM:691 MD:373 MC:377 MN:976 ME:382 MA:212 MZ:258 MM:95 NA:264 NR:674 NP:977 NL:31 NZ:64 NI:505 NE:227 NG:234 MK:389 NO:47 OM:968 PK:92 PW:680 PS:970 PA:507 PG:675 PY:595 PE:51 PH:63 PL:48 PT:351 PR:1787 QA:974 RO:40 RU:7 RW:250 KN:1869 LC:1758 VC:1784 WS:685 SM:378 ST:239 SA:966 SN:221 RS:381 SC:248 SL:232 SG:65 SK:421 SI:386 SB:677 SO:252 ZA:27 SS:211 ES:34 LK:94 SD:249 SR:597 SE:46 CH:41 SY:963 TW:886 TJ:992 TZ:255 TH:66 TL:670 TG:228 TO:676 TT:1868 TN:216 TR:90 TM:993 TV:688 UG:256 UA:380 AE:971 GB:44 US:1 UY:598 UZ:998 VU:678 VA:39 VE:58 VN:84 YE:967 ZM:260 ZW:263 XK:383 CW:599 SX:1721 BQ:599 GG:441481 IM:441624 JE:441534 FO:298 GL:299 GI:350 GP:590 MQ:596 RE:262 YT:262 GF:594 PF:689 NC:687 PM:508 WF:681""".split(" ").mapNotNull {
        val p = it.split(":"); if (p.size == 2) p[0] to p[1] else null
    }.toMap()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun flag(iso: String): String {
        val s = iso.uppercase(Locale.US)
        return if (s.length == 2) String(Character.toChars(0x1F1E6 + (s[0].code - 'A'.code))) +
            String(Character.toChars(0x1F1E6 + (s[1].code - 'A'.code))) else "🌐"
    }
    private fun countries(): List<Country> = Locale.getISOCountries().mapNotNull { iso ->
        val dial = codes[iso] ?: return@mapNotNull null
        Country(iso, Locale("", iso).getDisplayCountry(Locale("ar")), "+$dial", flag(iso))
    }.sortedBy { it.name }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = page
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = 1; setBackgroundColor(page) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(7), dp(18), dp(7)); setBackgroundColor(Color.WHITE) }
        header.addView(TextView(this).apply { text = "‹"; textSize = 34f; setTextColor(green); gravity = Gravity.CENTER; setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(44), dp(50)))
        header.addView(TextView(this).apply { text = "الأمان والخصوصية"; textSize = 18f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(50), 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(44), dp(50)))
        root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = 1; setPadding(dp(18), dp(24), dp(18), dp(28)) }
        scroll.addView(body)
        body.addView(TextView(this).apply { text = "حماية حسابك"; textSize = 24f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END })
        body.addView(TextView(this).apply { text = "تحكّم بطرق تسجيل الدخول وحافظ على أمان BAK LIVE."; textSize = 14f; setTextColor(muted); gravity = Gravity.END; setPadding(0, dp(7), 0, dp(24)) })

        val phoneCard = card()
        heading(phoneCard, "رقم الهاتف", "📱", "استقبل رموز التحقق على واتساب")
        phoneValue = TextView(this).apply { text = "جارٍ تحميل الرقم..."; textSize = 14f; setTextColor(muted); gravity = Gravity.END; setPadding(0, dp(14), 0, dp(16)) }
        phoneCard.addView(phoneValue)
        phoneCard.addView(actionButton("ربط أو تغيير رقم الهاتف") { showPhoneDialog() })
        body.addView(phoneCard)

        val passCard = card()
        heading(passCard, "كلمة المرور", "🔐", "حدّث كلمة المرور بشكل دوري")
        passCard.addView(TextView(this).apply { text = "اختر كلمة مرور قوية لا تستخدمها في حسابات أخرى."; textSize = 13f; setTextColor(muted); gravity = Gravity.END; setPadding(0, dp(14), 0, dp(16)) })
        passCard.addView(actionButton("تغيير كلمة المرور") { showPasswordDialog() })
        body.addView(passCard)
        status = TextView(this).apply { textSize = 13f; setTextColor(green); gravity = Gravity.CENTER; setPadding(dp(8), dp(14), dp(8), dp(8)) }
        body.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadSecurityInfo()
    }

    private fun shape(color: Int, radius: Int, stroke: Int = color) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke) }
    private fun card() = LinearLayout(this).apply {
        orientation = 1; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = shape(Color.WHITE, 22, Color.rgb(232, 238, 234))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
    }
    private fun heading(parent: LinearLayout, title: String, symbol: String, subtitle: String) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { text = symbol; textSize = 19f; gravity = Gravity.CENTER; background = shape(Color.rgb(232, 247, 238), 14) }, LinearLayout.LayoutParams(dp(46), dp(46)))
        val texts = LinearLayout(this).apply { orientation = 1; gravity = Gravity.END; setPadding(dp(12), 0, 0, 0) }
        texts.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END })
        texts.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(muted); gravity = Gravity.END; setPadding(0, dp(4), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f)); parent.addView(row)
    }
    private fun actionButton(label: String, click: () -> Unit) = TextView(this).apply {
        text = label; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(dp(16), dp(15), dp(16), dp(15)); background = shape(green, 15); setOnClickListener { click() }
    }
    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    private fun loadSecurityInfo() {
        val auth = token() ?: run { phoneValue.text = "يرجى تسجيل الدخول مجدداً"; return }
        executor.execute {
            try {
                val r = AuthApi.get("/api/auth/me", auth)
                if (r.status !in 200..299) throw Exception("load failed")
                val phone = r.body.optJSONObject("user")?.optString("phone")?.takeUnless { it.isNullOrBlank() || it == "null" }
                runOnUiThread { if (!isFinishing) phoneValue.text = phone ?: "لا يوجد رقم هاتف مرتبط" }
            } catch (_: Exception) { runOnUiThread { if (!isFinishing) phoneValue.text = "تعذر تحميل رقم الهاتف" } }
        }
    }

    private fun showPhoneDialog() {
        val panel = LinearLayout(this).apply { orientation = 1; setPadding(dp(22), dp(8), dp(22), dp(4)) }
        panel.addView(TextView(this).apply { text = "اختر الدولة ثم أدخل رقم الهاتف"; textSize = 14f; setTextColor(muted); gravity = Gravity.END; setPadding(0, 0, 0, dp(12)) })
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = shape(Color.rgb(247, 250, 248), 13, Color.rgb(225, 233, 227)); setPadding(dp(8), dp(4), dp(8), dp(4)) }
        val countryButton = TextView(this).apply {
            text = selectedCountry.flag + "  " + selectedCountry.dial + "  ▾"; textSize = 15f; setTextColor(ink); gravity = Gravity.CENTER; setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        val input = EditText(this).apply {
            hint = "رقم الهاتف"; textSize = 16f; inputType = android.text.InputType.TYPE_CLASS_PHONE; setTextColor(ink); setHintTextColor(muted)
            background = null; gravity = Gravity.CENTER_VERTICAL or Gravity.END; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        countryButton.setOnClickListener { showCountryPicker { c -> selectedCountry = c; countryButton.text = c.flag + "  " + c.dial + "  ▾" } }
        row.addView(countryButton, LinearLayout.LayoutParams(-2, dp(52)))
        row.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        panel.addView(row)
        panel.addView(TextView(this).apply { text = "سيتم إرسال رمز تحقق إلى الرقم عبر واتساب."; textSize = 12f; setTextColor(muted); gravity = Gravity.END; setPadding(0, dp(12), 0, 0) })
        val dialog = AlertDialog.Builder(this).setTitle("ربط رقم الهاتف").setView(panel).setNegativeButton("إلغاء", null).setPositiveButton("إرسال رمز التحقق", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setTextColor(green); dialog.getButton(-2).setTextColor(muted)
            dialog.getButton(-1).setOnClickListener {
                val digits = input.text.toString().filter { it.isDigit() }.trimStart('0')
                if (digits.length < 6) { input.error = "أدخل رقم هاتف صحيح"; return@setOnClickListener }
                val full = selectedCountry.dial + digits
                dialog.dismiss(); requestPhoneCode(full)
            }
        }
        dialog.show()
    }

    private fun showCountryPicker(onSelected: (Country) -> Unit) {
        val all = countries()
        var filtered = all
        val box = LinearLayout(this).apply { orientation = 1; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        val search = EditText(this).apply { hint = "ابحث عن الدولة أو رمز الاتصال"; textSize = 14f; setSingleLine(); setTextColor(ink); background = shape(Color.rgb(246, 249, 247), 12, Color.rgb(225, 233, 227)); setPadding(dp(14), dp(10), dp(14), dp(10)) }
        val list = ListView(this).apply { divider = android.graphics.drawable.ColorDrawable(Color.rgb(237, 241, 238)); dividerHeight = dp(1); isFastScrollEnabled = true }
        box.addView(search, LinearLayout.LayoutParams(-1, dp(48))); box.addView(list, LinearLayout.LayoutParams(-1, dp(420)))
        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = filtered.size
            override fun getItem(position: Int) = filtered[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val c = getItem(position)
                return (convertView as? TextView ?: TextView(this@AccountSecurityActivity).apply { textSize = 15f; setTextColor(ink); gravity = Gravity.CENTER_VERTICAL or Gravity.END; setPadding(dp(12), 0, dp(12), 0) }).apply {
                    text = c.flag + "     " + c.name + "       " + c.dial; height = dp(52)
                }
            }
        }
        list.adapter = adapter
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().trim().lowercase(Locale("ar"))
                filtered = all.filter { it.name.lowercase(Locale("ar")).contains(q) || it.dial.contains(q) || it.iso.lowercase().contains(q) }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        countryDialog = AlertDialog.Builder(this).setTitle("اختر الدولة").setView(box).setNegativeButton("إغلاق", null).create()
        list.setOnItemClickListener { _, _, position, _ -> onSelected(filtered[position]); countryDialog?.dismiss() }
        countryDialog?.show()
    }

    private fun requestPhoneCode(phone: String) {
        val auth = token() ?: return
        status.text = "جارٍ إرسال رمز التحقق..."
        executor.execute {
            try {
                val r = AuthApi.post("/api/account/phone/request", JSONObject().put("phone", phone), auth)
                if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إرسال الرمز"))
                runOnUiThread { if (!isFinishing) showPhoneCodeDialog(phone) }
            } catch (e: Exception) { runOnUiThread { if (!isFinishing) status.text = e.message ?: "تعذر إرسال الرمز" } }
        }
    }
    private fun showPhoneCodeDialog(phone: String) {
        val input = EditText(this).apply { hint = "رمز التحقق المكوّن من 6 أرقام"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setTextColor(ink) }
        AlertDialog.Builder(this).setTitle("تأكيد رقم الهاتف").setMessage("أدخل الرمز الذي وصلك عبر واتساب.").setView(input)
            .setNegativeButton("إلغاء", null).setPositiveButton("تأكيد") { _, _ -> verifyPhone(phone, input.text.toString().trim()) }.show()
    }
    private fun verifyPhone(phone: String, code: String) {
        val auth = token() ?: return
        status.text = "جارٍ تأكيد الرقم..."
        executor.execute {
            try {
                val r = AuthApi.post("/api/account/phone/verify", JSONObject().put("phone", phone).put("verificationCode", code), auth)
                if (r.status !in 200..299) throw Exception(r.body.optString("error", "فشل تأكيد الرقم"))
                runOnUiThread { if (!isFinishing) { status.text = "تم تحديث رقم الهاتف بنجاح"; loadSecurityInfo() } }
            } catch (e: Exception) { runOnUiThread { if (!isFinishing) status.text = e.message ?: "فشل تأكيد الرقم" } }
        }
    }
    private fun showPasswordDialog() {
        val panel = LinearLayout(this).apply { orientation = 1; setPadding(dp(22), dp(8), dp(22), 0) }
        fun field(h: String) = EditText(this).apply { hint = h; textSize = 15f; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; setTextColor(ink); setPadding(dp(4), dp(12), dp(4), dp(12)) }
        val current = field("كلمة المرور الحالية"); val next = field("كلمة المرور الجديدة (8 أحرف على الأقل)"); val confirm = field("تأكيد كلمة المرور الجديدة")
        panel.addView(current); panel.addView(next); panel.addView(confirm)
        val d = AlertDialog.Builder(this).setTitle("تغيير كلمة المرور").setView(panel).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create()
        d.setOnShowListener {
            d.getButton(-1).setTextColor(green); d.getButton(-2).setTextColor(muted)
            d.getButton(-1).setOnClickListener {
                val n = next.text.toString()
                if (current.text.isBlank() || n.length < 8 || n != confirm.text.toString()) { Toast.makeText(this, "تحقق من كلمة المرور الحالية والجديدة والتأكيد", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                d.dismiss(); changePassword(current.text.toString(), n)
            }
        }
        d.show()
    }
    private fun changePassword(current: String, next: String) {
        val auth = token() ?: return
        status.text = "جارٍ تحديث كلمة المرور..."
        executor.execute {
            try {
                val r = AuthApi.post("/api/account/password/change", JSONObject().put("currentPassword", current).put("newPassword", next), auth)
                if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر تغيير كلمة المرور"))
                runOnUiThread { if (!isFinishing) status.text = "تم تغيير كلمة المرور بنجاح" }
            } catch (e: Exception) { runOnUiThread { if (!isFinishing) status.text = e.message ?: "تعذر تغيير كلمة المرور" } }
        }
    }
    override fun onDestroy() { countryDialog?.dismiss(); executor.shutdownNow(); super.onDestroy() }
}
