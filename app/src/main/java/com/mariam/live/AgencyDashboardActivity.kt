package com.mariam.live

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class AgencyDashboardActivity : Activity() {

 private val worker = Executors.newSingleThreadExecutor()
 private val apiExecutor = Executors.newFixedThreadPool(4)

 // ───────────── Palette (white theme) ─────────────
 private val bg = Color.WHITE
 private val ink = Color.rgb(17, 24, 39)
 private val muted = Color.rgb(107, 114, 128)
 private val line = Color.rgb(229, 231, 235)
 private val fieldBg = Color.rgb(249, 250, 251)
 private val brand = Color.rgb(124, 58, 237)
 private val brandDark = Color.rgb(76, 29, 149)
 private val brandSoft = Color.rgb(243, 238, 255)
 private val green = Color.rgb(5, 150, 105)
 private val greenSoft = Color.rgb(236, 253, 245)
 private val amber = Color.rgb(217, 119, 6)
 private val amberSoft = Color.rgb(255, 247, 230)
 private val red = Color.rgb(220, 38, 38)
 private val redSoft = Color.rgb(254, 242, 242)
 private val blue = Color.rgb(37, 99, 235)
 private val blueSoft = Color.rgb(239, 246, 255)
 private val rippleBrand = Color.argb(30, 124, 58, 237)
 private val tones by lazy { listOf(brand to brandSoft, blue to blueSoft, green to greenSoft, amber to amberSoft) }

 // ───────────── Views ─────────────
 private lateinit var scroll: ScrollView
 private lateinit var body: LinearLayout
 private lateinit var heroName: TextView
 private lateinit var heroTotal: TextView
 private lateinit var heroAvail: TextView
 private lateinit var heroPaid: TextView
 private lateinit var earningsBox: LinearLayout
 private lateinit var membersEarningsBox: LinearLayout
 private lateinit var historyBox: LinearLayout
 private lateinit var withdrawalsBox: LinearLayout
 private lateinit var withdrawalsStatus: TextView
 private var withdrawalAvailableCents: Long = 0L
 private lateinit var agencyBox: LinearLayout
 private lateinit var invitesBox: LinearLayout
 private lateinit var membersBox: LinearLayout
 private lateinit var earningsStatus: TextView
 private lateinit var status: TextView
 private lateinit var statMembers: TextView
 private lateinit var statPending: TextView
 private lateinit var anAccepted: TextView
 private lateinit var anRejected: TextView
 private lateinit var anCancelled: TextView
 private lateinit var anTotal: TextView
 private lateinit var input: EditText
 private lateinit var memberSearch: EditText
 private lateinit var inviteSearch: EditText
 private lateinit var send: View
 private lateinit var agencyHeader: View
 private lateinit var earningsAnchor: View
 private lateinit var invitesAnchor: View
 private lateinit var membersAnchor: View

 private var invites = JSONArray()
 private var members = JSONArray()
 private var agencyInfo = JSONObject()
 private var agencyName = "وكالتك"

 // ───────────── Helpers ─────────────
 private val rtl: Boolean get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
 private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
 private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
 private fun lp(w: Int = -1, h: Int = -2) = LinearLayout.LayoutParams(w, h)
 private fun fmt(v: Long): String = String.format(java.util.Locale.US, "%,d", v)

 private fun vbox() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
 private fun hbox() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

 private fun shape(fill: Int, radius: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
  cornerRadius = dp(radius).toFloat()
  setColor(fill)
  if (stroke != 0) setStroke(dp(1), stroke)
 }

 private fun ripple(content: Drawable, radius: Int, color: Int = rippleBrand): Drawable =
  RippleDrawable(ColorStateList.valueOf(color), content, shape(Color.WHITE, radius))

 private fun card(radius: Int = 18): Drawable = shape(Color.WHITE, radius, line)

 private fun cardView(pad: Int = 14): LinearLayout = vbox().apply {
  setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
  background = card()
  elevation = dp(1).toFloat()
 }

 private fun t(s: String, z: Float = 14f, c: Int = ink, b: Boolean = false) = TextView(this).apply {
  text = s; textSize = z; setTextColor(c)
  if (b) typeface = Typeface.DEFAULT_BOLD
 }

 private fun ico(kind: Int, color: Int = ink, size: Int = 20) = ImageView(this).apply {
  setImageDrawable(AgencyIcon(kind, color, rtl))
  layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
 }

 private fun badge(kind: Int, tint: Int, soft: Int, box: Int = 40, icon: Int = 20, round: Boolean = false) =
  FrameLayout(this).apply {
   background = shape(soft, if (round) 999 else 12)
   addView(ico(kind, tint, icon), FrameLayout.LayoutParams(dp(icon), dp(icon), Gravity.CENTER))
   layoutParams = LinearLayout.LayoutParams(dp(box), dp(box))
  }

 private fun initials(name: String, size: Int = 44): TextView {
  val ch = name.trim().firstOrNull { it.isLetterOrDigit() }?.toString()?.uppercase() ?: "•"
  val (fg, sf) = tones[(name.hashCode() and 0x7fffffff) % tones.size]
  return TextView(this).apply {
   text = ch; textSize = size / 2.6f; setTextColor(fg); typeface = Typeface.DEFAULT_BOLD
   gravity = Gravity.CENTER; background = shape(sf, 999)
   layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
  }
 }

 private fun chip(s: String, fg: Int, soft: Int) = t(s, 11.5f, fg, true).apply {
  setPadding(dp(10), dp(4), dp(10), dp(4)); background = shape(soft, 999)
 }

 private fun usdText(cents: Long): String = String.format(java.util.Locale.US, "\$%,.2f", cents / 100.0)

  private fun usdLine(cents: Long, z: Float = 20f, c: Int = ink): LinearLayout = hbox().apply {
   addView(t(usdText(cents), z, c, true))
  }

 private fun button(label: String, kind: Int = 0, icon: Int = -1, action: () -> Unit): LinearLayout {
  val fg = when (kind) { 0 -> Color.WHITE; 2 -> red; else -> brand }
  val fill: Drawable = when (kind) {
   0 -> GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(brand, brandDark)).apply { cornerRadius = dp(14).toFloat() }
   2 -> shape(redSoft, 14)
   else -> shape(brandSoft, 14)
  }
  return LinearLayout(this).apply {
   orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
   setPadding(dp(14), dp(13), dp(14), dp(13)); minimumHeight = dp(48)
   background = ripple(fill, 14, if (kind == 0) Color.argb(60, 255, 255, 255) else rippleBrand)
   if (icon >= 0) addView(ico(icon, fg, 18), LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) })
   addView(t(label, 14.5f, fg, true))
   isClickable = true; isFocusable = true
   setOnClickListener { action() }
  }
 }

 private fun iconButton(kind: Int, desc: String, action: () -> Unit) = FrameLayout(this).apply {
  background = ripple(shape(Color.WHITE, 999, line), 999)
  contentDescription = desc; isClickable = true; isFocusable = true
  addView(ico(kind, brand, 18), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
  layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
  setOnClickListener { action() }
 }

 private fun section(title: String, sub: String = "", icon: Int = Ic.INFO, trailing: View? = null): View {
  val row = hbox()
  row.addView(badge(icon, brand, brandSoft, 38, 20), LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(12) })
  val col = vbox()
  col.addView(t(title, 17f, ink, true))
  if (sub.isNotBlank()) col.addView(t(sub, 12f, muted), lp().apply { topMargin = dp(2) })
  row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
  if (trailing != null) row.addView(trailing, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginStart = dp(8) })
  body.addView(row, lp().apply { topMargin = dp(26); bottomMargin = dp(12) })
  return row
 }

 private fun empty(icon: Int, msg: String) = vbox().apply {
  gravity = Gravity.CENTER; setPadding(dp(16), dp(22), dp(16), dp(22)); background = shape(fieldBg, 18)
  addView(badge(icon, muted, Color.WHITE, 44, 22, true), LinearLayout.LayoutParams(dp(44), dp(44)))
  addView(t(msg, 13f, muted).apply { gravity = Gravity.CENTER }, lp().apply { topMargin = dp(10) })
 }

 private fun statusLine(s: String) = t(s, 12f, muted).apply {
  gravity = Gravity.CENTER; setPadding(dp(12), dp(8), dp(12), dp(8)); background = shape(fieldBg, 999)
 }

 private fun inputBox(hint: String, icon: Int, onChange: (() -> Unit)? = null): Pair<LinearLayout, EditText> {
  val et = EditText(this).apply {
   this.hint = hint; setSingleLine(); textSize = 14.5f; setTextColor(ink)
   setHintTextColor(Color.rgb(156, 163, 175)); background = null; setPadding(0, 0, 0, 0)
  }
  val box = hbox().apply { setPadding(dp(14), 0, dp(14), 0); background = shape(fieldBg, 14, line) }
  box.addView(ico(icon, muted, 20), LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(10) })
  box.addView(et, LinearLayout.LayoutParams(0, -1, 1f))
  et.setOnFocusChangeListener { _, f -> box.background = shape(if (f) Color.WHITE else fieldBg, 14, if (f) brand else line) }
  if (onChange != null) et.addTextChangedListener(object : TextWatcher {
   override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
   override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { onChange() }
   override fun afterTextChanged(s: Editable?) {}
  })
  return box to et
 }

 private fun toneOf(s: String): Pair<Int, Int> = when (s) {
  "available", "accepted" -> green to greenSoft
  "pending" -> amber to amberSoft
  "paid" -> blue to blueSoft
  "reversed", "rejected" -> red to redSoft
  else -> muted to fieldBg
 }

 private fun inviteLabel(s: String) = when (s) {
  "pending" -> "بانتظار الرد"; "accepted" -> "مقبولة"; "rejected" -> "مرفوضة"; "cancelled" -> "ملغاة"; else -> s
 }

 private fun earnLabel(s: String) = when (s) {
  "available" -> "متاح"; "pending" -> "قيد الانتظار"; "paid" -> "مدفوع"; "reversed" -> "معكوس"; else -> if (s.isBlank()) "—" else s
 }

 private fun goTo(v: View) = scroll.smoothScrollTo(0, (v.top - dp(8)).coerceAtLeast(0))

 // ───────────── Bottom sheet / confirm ─────────────
 private fun sheet(build: (LinearLayout, Dialog) -> Unit) {
  val d = Dialog(this)
  d.requestWindowFeature(Window.FEATURE_NO_TITLE)
  val r = dp(28).toFloat()
  val box = vbox().apply {
   setPadding(dp(20), dp(12), dp(20), dp(20))
   background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
  }
  box.addView(View(this).apply { background = shape(line, 999) }, LinearLayout.LayoutParams(dp(40), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(16) })
  build(box, d)
  d.setContentView(box)
  d.window?.apply {
   setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
   setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
   setGravity(Gravity.BOTTOM)
  }
  d.show()
 }

 private fun confirm(title: String, msg: String, okLabel: String, icon: Int, onOk: () -> Unit) {
  sheet { box, d ->
   box.addView(badge(icon, red, redSoft, 56, 28, true), LinearLayout.LayoutParams(dp(56), dp(56)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(14) })
   box.addView(t(title, 18f, ink, true).apply { gravity = Gravity.CENTER }, lp())
   box.addView(t(msg, 13.5f, muted).apply { gravity = Gravity.CENTER }, lp().apply { topMargin = dp(8); bottomMargin = dp(20) })
   val row = hbox()
   row.addView(button("رجوع", 1) { d.dismiss() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
   row.addView(button(okLabel, 2) { d.dismiss(); onOk() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
   box.addView(row, lp())
  }
 }

 // ───────────── onCreate ─────────────
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = bg
  window.navigationBarColor = bg
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  if (Build.VERSION.SDK_INT >= 23) {
   var f = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
   if (Build.VERSION.SDK_INT >= 26) f = f or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
   window.decorView.systemUiVisibility = f
  }

  val root = vbox().apply { setBackgroundColor(bg) }

  // Top bar
  val bar = hbox().apply { setPadding(dp(16), dp(10), dp(16), dp(10)) }
  val back = FrameLayout(this)
  back.background = ripple(shape(Color.WHITE, 999, line), 999)
  back.isClickable = true; back.isFocusable = true; back.contentDescription = "رجوع"
  back.addView(ico(Ic.BACK, ink, 20), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
  back.setOnClickListener { finish() }
  bar.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))
  bar.addView(t("لوحة الوكالة", 18f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
  bar.addView(chip("BAK", brand, brandSoft).apply { minWidth = dp(42); gravity = Gravity.CENTER })
  root.addView(bar)
  root.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))

  scroll = ScrollView(this)
  scroll.isVerticalScrollBarEnabled = false
  body = vbox().apply { setPadding(dp(16), dp(16), dp(16), dp(32)) }
  scroll.addView(body)
  root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
  setContentView(root)

  // Hero
  val hero = vbox().apply {
   setPadding(dp(20), dp(20), dp(20), dp(20))
   background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(139, 92, 246), Color.rgb(109, 40, 217), Color.rgb(76, 29, 149))).apply { cornerRadius = dp(24).toFloat() }
   elevation = dp(4).toFloat()
  }
  val heroTop = hbox()
  heroTop.addView(badge(Ic.BUILDING, Color.WHITE, Color.argb(45, 255, 255, 255), 44, 22), LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(12) })
  val heroCol = vbox()
  heroCol.addView(t("شريك وكالة معتمد", 12f, Color.argb(210, 255, 255, 255)))
  heroName = t(agencyName, 20f, Color.WHITE, true)
  heroCol.addView(heroName, lp().apply { topMargin = dp(2) })
  heroTop.addView(heroCol, LinearLayout.LayoutParams(0, -2, 1f))
  hero.addView(heroTop)
  hero.addView(t("إجمالي أرباح الوكالة", 12.5f, Color.argb(210, 255, 255, 255)), lp().apply { topMargin = dp(20) })
  val totalRow = hbox()
  heroTotal = t("—", 34f, Color.WHITE, true)
  totalRow.addView(ico(Ic.WALLET, Color.WHITE, 26), LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(8) })
  totalRow.addView(heroTotal)
  hero.addView(totalRow, lp().apply { topMargin = dp(4) })
  heroAvail = t("—", 16f, Color.WHITE, true)
  heroPaid = t("—", 16f, Color.WHITE, true)
  val tiles = hbox()
  tiles.addView(glassTile("المتاحة", heroAvail), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) })
  tiles.addView(glassTile("تمت تسويتها", heroPaid), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5) })
  hero.addView(tiles, lp().apply { topMargin = dp(18) })
  body.addView(hero)

  // Quick actions
  val qrow = hbox()
  qrow.addView(quick(Ic.WALLET, "الأرباح") { goTo(earningsAnchor) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) })
  qrow.addView(quick(Ic.MAIL, "دعوة") { goTo(invitesAnchor) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5); marginEnd = dp(5) })
  qrow.addView(quick(Ic.USERS, "الأعضاء") { goTo(membersAnchor) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5) })
  body.addView(qrow, lp().apply { topMargin = dp(12) })

  // Summary
  section("ملخص الوكالة", "بيانات حقيقية من السيرفر", Ic.TREND, iconButton(Ic.REFRESH, "تحديث بيانات الوكالة") { loadData() })
  statMembers = t("—", 24f, ink, true)
  statPending = t("—", 24f, ink, true)
  val stats = hbox()
  stats.addView(statCard("الأعضاء", Ic.USERS, brand, brandSoft, statMembers), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) })
  stats.addView(statCard("دعوات معلّقة", Ic.MAIL, amber, amberSoft, statPending), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5) })
  body.addView(stats)
  anAccepted = t("—"); anRejected = t("—"); anCancelled = t("—"); anTotal = t("—")
  val analytics = cardView(12).apply { orientation = LinearLayout.HORIZONTAL }
  analytics.addView(miniStat("مقبولة", anAccepted, green), LinearLayout.LayoutParams(0, -2, 1f))
  analytics.addView(miniStat("مرفوضة", anRejected, red), LinearLayout.LayoutParams(0, -2, 1f))
  analytics.addView(miniStat("ملغاة", anCancelled, muted), LinearLayout.LayoutParams(0, -2, 1f))
  analytics.addView(miniStat("الإجمالي", anTotal, brand), LinearLayout.LayoutParams(0, -2, 1f))
  body.addView(analytics, lp().apply { topMargin = dp(10) })
  status = statusLine("جارٍ تحميل البيانات...")
  body.addView(status, lp().apply { topMargin = dp(10) })

  // Earnings
  earningsAnchor = section("أرباح الوكالة", "12% من إجمالي قيمة الهدايا بالدولار • مستقلة عن أرباح المذيع", Ic.WALLET, iconButton(Ic.REFRESH, "تحديث الأرباح") { loadEarnings() })
  earningsBox = vbox()
  body.addView(earningsBox)
  earningsStatus = statusLine("جارٍ تحميل الأرباح...")
  body.addView(earningsStatus, lp().apply { topMargin = dp(10) })

  section("سحب أرباح الوكالة", "اطلب تحويل الرصيد المتاح إلى وسيلة الدفع التي تختارها", Ic.WALLET, iconButton(Ic.REFRESH, "تحديث طلبات السحب") { loadWithdrawals() })
  withdrawalsBox = vbox()
  body.addView(withdrawalsBox)
  withdrawalsStatus = statusLine("جارٍ تحميل رصيد السحب...")
  body.addView(withdrawalsStatus, lp().apply { topMargin = dp(8) })
  body.addView(button("طلب سحب الأرباح", 0, Ic.WALLET) { openWithdrawalDialog() }, lp().apply { topMargin = dp(10) })

  section("عمولة الوكالة حسب العضو", "عمولة الوكالة الناتجة عن هدايا كل عضو • ليست أرباح المذيع", Ic.GIFT)
  membersEarningsBox = vbox()
  body.addView(membersEarningsBox)

  section("سجل الأرباح", "آخر عمليات العمولة المسجلة", Ic.CLOCK)
  historyBox = vbox()
  body.addView(historyBox)

  // Agency details (filled after the server responds)
  agencyHeader = section("بيانات الوكالة", "معلومات الوكالة من استجابة السيرفر", Ic.BUILDING)
  agencyHeader.visibility = View.GONE
  agencyBox = cardView(14).apply { visibility = View.GONE }
  body.addView(agencyBox)

  // Invite
  invitesAnchor = section("دعوة صانع محتوى", "أدخل ID المستخدم لإرسال دعوة حقيقية", Ic.MAIL)
  val inp = inputBox("معرّف المستخدم (ID)", Ic.USER)
  input = inp.second
  body.addView(inp.first, lp(h = dp(54)))
  send = button("إرسال دعوة الوكالة", 0, Ic.SEND) { sendInvitation() }
  body.addView(send, lp().apply { topMargin = dp(10) })

  section("دعوات الوكالة", "حالات الدعوات المحفوظة", Ic.CLOCK)
  val invSearch = inputBox("بحث: الاسم أو ID أو الحالة", Ic.SEARCH) { renderInvites() }
  inviteSearch = invSearch.second
  body.addView(invSearch.first, lp(h = dp(50)).apply { bottomMargin = dp(10) })
  invitesBox = vbox()
  body.addView(invitesBox)

  // Members
  membersAnchor = section("أعضاء الوكالة", "المستخدمون الذين قبلوا الدعوة", Ic.USERS)
  val memSearch = inputBox("بحث بالاسم أو ID", Ic.SEARCH) { renderMembers() }
  memberSearch = memSearch.second
  body.addView(memSearch.first, lp(h = dp(50)).apply { bottomMargin = dp(10) })
  membersBox = vbox()
  body.addView(membersBox)

  // Footnote
  val note = hbox().apply { setPadding(dp(14), dp(14), dp(14), dp(14)); background = shape(fieldBg, 16) }
  note.addView(ico(Ic.INFO, muted, 20), LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(10) })
  note.addView(t("عمولة الوكالة تُسجّل بالدولار وتُعرض هنا بالدولار، ولا تُضاف إلى رصيد كوينز أو ماسات المذيع.", 11.5f, muted), LinearLayout.LayoutParams(0, -2, 1f))
  body.addView(note, lp().apply { topMargin = dp(24) })

  loadData()
  loadEarnings()
  loadWithdrawals()
 }

 // ───────────── Reusable cards ─────────────
 private fun glassTile(label: String, v: TextView) = vbox().apply {
  setPadding(dp(14), dp(12), dp(14), dp(12))
  background = shape(Color.argb(38, 255, 255, 255), 16)
  addView(t(label, 11.5f, Color.argb(210, 255, 255, 255)))
  val r = hbox()
  r.addView(ico(Ic.DIAMOND, Color.WHITE, 14), LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(5) })
  r.addView(v)
  addView(r, lp().apply { topMargin = dp(5) })
 }

 private fun quick(icon: Int, label: String, go: () -> Unit) = vbox().apply {
  gravity = Gravity.CENTER_HORIZONTAL
  setPadding(dp(6), dp(14), dp(6), dp(12))
  background = ripple(card(), 18)
  isClickable = true; isFocusable = true
  addView(badge(icon, brand, brandSoft, 42, 22), LinearLayout.LayoutParams(dp(42), dp(42)))
  addView(t(label, 12.5f, ink, true).apply { gravity = Gravity.CENTER }, lp().apply { topMargin = dp(8) })
  setOnClickListener { go() }
 }

 private fun statCard(title: String, icon: Int, tint: Int, soft: Int, value: TextView) = cardView(14).apply {
  addView(badge(icon, tint, soft, 38, 20), LinearLayout.LayoutParams(dp(38), dp(38)))
  addView(value, lp().apply { topMargin = dp(12) })
  addView(t(title, 12.5f, muted), lp().apply { topMargin = dp(2) })
 }

 private fun miniStat(label: String, v: TextView, c: Int) = vbox().apply {
  gravity = Gravity.CENTER_HORIZONTAL
  addView(v.apply { textSize = 18f; setTextColor(c); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER })
  addView(t(label, 11f, muted).apply { gravity = Gravity.CENTER }, lp().apply { topMargin = dp(3) })
 }

 private fun earnCard(label: String, value: Long, icon: Int, tint: Int, soft: Int) = cardView(12).apply {
  addView(badge(icon, tint, soft, 34, 18), LinearLayout.LayoutParams(dp(34), dp(34)))
  addView(usdLine(value, 15f, ink), lp().apply { topMargin = dp(10) })
  addView(t(label, 11.5f, muted), lp().apply { topMargin = dp(3) })
 }

 private fun infoRow(icon: Int, label: String, value: String) = hbox().apply {
  setPadding(dp(4), dp(10), dp(4), dp(10))
  addView(badge(icon, brand, brandSoft, 34, 18), LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(12) })
  addView(t(label, 12.5f, muted), LinearLayout.LayoutParams(0, -2, 1f))
  addView(t(value, 14f, ink, true))
 }

 // ───────────── Earnings ─────────────
 private fun loadEarnings() {
  val tok = token() ?: run { if (::earningsStatus.isInitialized) earningsStatus.text = "يرجى تسجيل الدخول"; return }
  if (::earningsStatus.isInitialized) earningsStatus.text = "جارٍ تحميل تقرير الأرباح..."
  worker.execute {
   try {
    // Independent report endpoints are fetched together to reduce dashboard wait time.
    val overviewFuture = apiExecutor.submit { AuthApi.get("/api/agency/earnings", tok) }
    val membersFuture = apiExecutor.submit { AuthApi.get("/api/agency/earnings/members", tok) }
    val historyFuture = apiExecutor.submit { AuthApi.get("/api/agency/earnings/history?limit=30", tok) }
    val overview = overviewFuture.get()
    val perMember = membersFuture.get()
    val history = historyFuture.get()
    if (overview.status !in 200..299) throw Exception(overview.body.optString("error", "فشل تحميل الأرباح"))
    if (perMember.status !in 200..299) throw Exception(perMember.body.optString("error", "فشل تحميل أرباح الأعضاء"))
    if (history.status !in 200..299) throw Exception(history.body.optString("error", "فشل تحميل سجل الأرباح"))
    val o = overview.body.optJSONObject("earnings") ?: JSONObject()
    val m = perMember.body.optJSONArray("members") ?: JSONArray()
    val h = history.body.optJSONArray("history") ?: JSONArray()
    runOnUiThread { renderEarnings(o, m, h); earningsStatus.text = "تم تحديث الأرباح من الخادم" }
   } catch (e: Exception) {
    runOnUiThread { earningsStatus.text = e.message ?: "تعذر تحميل الأرباح. حاول تحديث التقرير." }
   }
  }
 }

 private fun renderEarnings(o: JSONObject, m: JSONArray, h: JSONArray) {
  if (!::earningsBox.isInitialized) return
  val total = o.optLong("total_cents", 0); val available = o.optLong("available_cents", 0); val paid = o.optLong("paid_cents", 0)
  val today = o.optLong("today_cents", 0); val week = o.optLong("last_7_days_cents", 0); val month = o.optLong("month_cents", 0)
  heroTotal.text = usdText(total); heroAvail.text = usdText(available); heroPaid.text = usdText(paid)

  earningsBox.removeAllViews()
  val row = hbox()
  row.addView(earnCard("أرباح اليوم", today, Ic.CLOCK, blue, blueSoft), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
  row.addView(earnCard("آخر 7 أيام", week, Ic.TREND, green, greenSoft), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
  row.addView(earnCard("هذا الشهر", month, Ic.CALENDAR, amber, amberSoft), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
  earningsBox.addView(row)

  membersEarningsBox.removeAllViews()
  if (m.length() == 0) membersEarningsBox.addView(empty(Ic.GIFT, "لا توجد بيانات أرباح أعضاء بعد"))
  for (i in 0 until m.length()) {
   val item = m.optJSONObject(i) ?: continue
   val id = item.optString("public_id", "—")
   val name = item.optString("display_name").ifBlank { item.optString("username").ifBlank { "ID $id" } }
   val totalMember = item.optLong("total_cents", 0); val availMember = item.optLong("available_cents", 0); val gifts = item.optLong("gifts_count", 0)
   val r = cardView(12).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
   r.addView(initials(name, 44), LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(12) })
   val col = vbox()
   col.addView(t(name, 14.5f, ink, true))
   col.addView(t("ID $id • هدايا: $gifts", 11.5f, muted), lp().apply { topMargin = dp(3) })
   r.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
   val end = vbox().apply { gravity = Gravity.END }
   end.addView(usdLine(totalMember, 14f, green))
   end.addView(t("متاح: ${usdText(availMember)}", 11f, muted), lp().apply { topMargin = dp(3) })
   r.addView(end)
   membersEarningsBox.addView(r, lp().apply { bottomMargin = dp(8) })
  }

  historyBox.removeAllViews()
  if (h.length() == 0) historyBox.addView(empty(Ic.CLOCK, "لا توجد عمليات مسجلة حتى الآن"))
  for (i in 0 until h.length()) {
   val item = h.optJSONObject(i) ?: continue
   val name = item.optString("display_name").ifBlank { item.optString("public_id", "عضو") }
   val amount = item.optLong("amount_usd_cents", 0)
   val st = item.optString("status")
   val (fg, sf) = toneOf(st)
   val date = item.optString("created_at", "").replace("T", " ").take(16)
   val r = cardView(12).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
   r.addView(initials(name, 40), LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
   val col = vbox()
   col.addView(t(name, 14f, ink, true))
   val meta = hbox()
   meta.addView(chip(earnLabel(st), fg, sf))
   meta.addView(t(date, 11f, muted), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
   col.addView(meta, lp().apply { topMargin = dp(5) })
   r.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
   r.addView(usdLine(amount, 14f, ink))
   historyBox.addView(r, lp().apply { bottomMargin = dp(7) })
  }
 }


 // ───────────── Agency withdrawals ─────────────
 private fun loadWithdrawals() {
  val tok = token() ?: run {
   if (::withdrawalsStatus.isInitialized) withdrawalsStatus.text = "يرجى تسجيل الدخول مجدداً"
   return
  }
  if (::withdrawalsStatus.isInitialized) withdrawalsStatus.text = "جارٍ تحديث رصيد السحب والطلبات..."
  worker.execute {
   try {
    val r = AuthApi.get("/api/agency/withdrawals?limit=30&offset=0", tok)
    if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر تحميل طلبات السحب"))
    val root = r.body
    val balances = root.optJSONObject("balances") ?: root.optJSONObject("earnings") ?: root
    val rows = root.optJSONArray("withdrawals") ?: root.optJSONArray("history") ?: JSONArray()
    val available = balances.optLong("available_cents", root.optLong("available_cents", 0L))
    val reserved = balances.optLong("reserved_cents", root.optLong("reserved_cents", 0L))
    runOnUiThread {
     withdrawalAvailableCents = available
     renderWithdrawals(rows, available, reserved)
     withdrawalsStatus.text = "تم تحديث بيانات السحب من الخادم"
    }
   } catch (e: Exception) {
    runOnUiThread {
     if (::withdrawalsStatus.isInitialized) withdrawalsStatus.text = e.message ?: "تعذر تحميل طلبات السحب"
    }
   }
  }
 }

 private fun withdrawalStatusLabel(value: String): String = when (value) {
  "pending" -> "قيد المراجعة"
  "approved" -> "تمت الموافقة — بانتظار التحويل"
  "rejected" -> "مرفوض"
  "paid" -> "تم الدفع"
  else -> value.ifBlank { "غير معروف" }
 }

 private fun withdrawalMethodLabel(value: String): String = when (value) {
  "zain_cash" -> "زين كاش"
  "asia_hawala" -> "آسيا حوالة"
  "usdt" -> "USDT"
  else -> value
 }

 private fun renderWithdrawals(rows: JSONArray, available: Long, reserved: Long) {
  if (!::withdrawalsBox.isInitialized) return
  withdrawalsBox.removeAllViews()
  val summary = hbox()
  summary.addView(earnCard("الرصيد المتاح للسحب", available, Ic.WALLET, green, greenSoft),
   LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) })
  summary.addView(earnCard("الرصيد المحجوز", reserved, Ic.CLOCK, amber, amberSoft),
   LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5) })
  withdrawalsBox.addView(summary)
  if (rows.length() == 0) {
   withdrawalsBox.addView(empty(Ic.WALLET, "لا توجد طلبات سحب حتى الآن"), lp().apply { topMargin = dp(8) })
   return
  }
  for (i in 0 until rows.length()) {
   val item = rows.optJSONObject(i) ?: continue
   val amount = item.optLong("amount_usd_cents", 0L)
   val method = withdrawalMethodLabel(item.optString("method"))
   val state = item.optString("status")
   val (fg, soft) = toneOf(state)
   val destination = item.optString("destination")
   val date = item.optString("created_at").replace("T", " ").take(16)
   val card = cardView(13)
   val top = hbox()
   val col = vbox()
   col.addView(t("طلب سحب #${item.optString("id", "—")}", 13f, muted, true))
   col.addView(t("$method • $date", 11.5f, muted), lp().apply { topMargin = dp(4) })
   top.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
   top.addView(usdLine(amount, 17f, ink))
   card.addView(top)
   card.addView(chip(withdrawalStatusLabel(state), fg, soft), lp().apply { topMargin = dp(10) })
   if (destination.isNotBlank()) card.addView(t("وجهة التحويل: $destination", 12f, muted), lp().apply { topMargin = dp(8) })
   val note = item.optString("admin_note")
   if (note.isNotBlank()) card.addView(t("ملاحظة الإدارة: $note", 12f, red), lp().apply { topMargin = dp(6) })
   val reference = item.optString("payment_reference")
   if (reference.isNotBlank()) card.addView(t("مرجع الدفع: $reference", 11.5f, green), lp().apply { topMargin = dp(6) })
   withdrawalsBox.addView(card, lp().apply { topMargin = dp(8) })
  }
 }

 private fun openWithdrawalDialog() {
  if (withdrawalAvailableCents <= 0L) {
   withdrawalsStatus.text = "لا يوجد رصيد متاح للسحب حالياً"
   return
  }
  sheet { box, dialog ->
   box.addView(t("طلب سحب أرباح الوكالة", 19f, ink, true), lp())
   box.addView(t("المتاح حالياً: ${usdText(withdrawalAvailableCents)}", 13f, green, true), lp().apply { topMargin = dp(7); bottomMargin = dp(14) })
   box.addView(t("المبلغ بالدولار", 12.5f, muted, true), lp())
   val amount = EditText(this).apply {
    hint = "مثال: 25.00"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    textSize = 16f; setTextColor(ink); background = shape(fieldBg, 12, line); setPadding(dp(13), dp(12), dp(13), dp(12))
   }
   box.addView(amount, lp(h = dp(52)).apply { topMargin = dp(6); bottomMargin = dp(12) })
   box.addView(t("طريقة الاستلام", 12.5f, muted, true), lp())
   val methods = listOf("زين كاش", "آسيا حوالة", "USDT")
   val methodKeys = listOf("zain_cash", "asia_hawala", "usdt")
   val spinner = android.widget.Spinner(this)
   spinner.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, methods)
   box.addView(spinner, lp(h = dp(50)).apply { topMargin = dp(5); bottomMargin = dp(12) })
   box.addView(t("رقم المحفظة أو عنوان الاستلام", 12.5f, muted, true), lp())
   val destination = EditText(this).apply {
    hint = "أدخل بيانات الاستلام"; textSize = 15f; setTextColor(ink)
    background = shape(fieldBg, 12, line); setPadding(dp(13), dp(12), dp(13), dp(12))
   }
   box.addView(destination, lp(h = dp(52)).apply { topMargin = dp(6); bottomMargin = dp(16) })
   val row = hbox()
   row.addView(button("إلغاء", 1) { dialog.dismiss() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) })
   row.addView(button("إرسال الطلب", 0, Ic.SEND) {
    val cents = try { amount.text.toString().trim().toBigDecimal().movePointRight(2).longValueExact() } catch (_: Exception) { 0L }
    val dest = destination.text.toString().trim()
    if (cents <= 0L || cents > withdrawalAvailableCents) {
     amount.error = "أدخل مبلغاً صحيحاً لا يتجاوز الرصيد المتاح"
     return@button
    }
    if (dest.length !in 3..300) {
     destination.error = "أدخل بيانات استلام صحيحة"
     return@button
    }
    dialog.dismiss()
    withdrawalsStatus.text = "جارٍ إرسال طلب السحب..."
    val tok = token() ?: run { withdrawalsStatus.text = "يرجى تسجيل الدخول مجدداً"; return@button }
    worker.execute {
     try {
      val payload = JSONObject()
       .put("amount_usd_cents", cents)
       .put("method", methodKeys[spinner.selectedItemPosition])
       .put("destination", dest)
      val result = AuthApi.post("/api/agency/withdrawals", payload, tok)
      if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إرسال طلب السحب"))
      runOnUiThread {
       withdrawalsStatus.text = "تم إرسال طلب السحب بنجاح، وهو الآن قيد المراجعة"
       loadWithdrawals()
       loadEarnings()
      }
     } catch (e: Exception) {
      runOnUiThread { withdrawalsStatus.text = e.message ?: "تعذر إرسال طلب السحب" }
     }
    }
   }, LinearLayout.LayoutParams(0, -2, 1.3f).apply { marginStart = dp(5) })
   box.addView(row)
  }
 }

 // ───────────── Agency details ─────────────
 private fun renderAgencyDetails() {
  if (!::agencyBox.isInitialized) return
  agencyBox.removeAllViews()
  val labels = mapOf(
   "agency_name" to "اسم الوكالة", "agency_id" to "رقم الوكالة", "status" to "الحالة",
   "created_at" to "تاريخ الإنشاء", "owner_name" to "المالك", "commission_rate" to "نسبة العمولة"
  )
  var shown = 0
  for (key in agencyInfo.keys().asSequence().toList()) {
   val raw = agencyInfo.opt(key)
   if (raw == null || raw == JSONObject.NULL) continue
   val value = raw.toString()
   if (value.isBlank() || value == "null") continue
   if (shown > 0) agencyBox.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))
   val r = hbox().apply { setPadding(0, dp(12), 0, dp(12)) }
   r.addView(t(labels[key] ?: key.replace("_", " "), 12.5f, muted), LinearLayout.LayoutParams(0, -2, 1f))
   r.addView(t(value, 14f, ink, true))
   agencyBox.addView(r)
   shown++
  }
  val vis = if (shown > 0) View.VISIBLE else View.GONE
  agencyBox.visibility = vis
  agencyHeader.visibility = vis
 }

 // ───────────── Data ─────────────
 private fun loadData(afterRemoval: Boolean = false) {
  val tok = token() ?: run { status.text = "يرجى تسجيل الدخول مجدداً"; return }
  if (!afterRemoval) status.text = "جارٍ تحديث البيانات..."
  worker.execute {
   try {
    // Member and invitation lists do not depend on each other.
    val membersFuture = apiExecutor.submit { AuthApi.get("/api/agency/members", tok) }
    val invitesFuture = apiExecutor.submit { AuthApi.get("/api/agency/invitations", tok) }
    val m = membersFuture.get()
    val i = invitesFuture.get()
    if (m.status !in 200..299) throw Exception(m.body.optString("error", "فشل تحميل الأعضاء"))
    if (i.status !in 200..299) throw Exception(i.body.optString("error", "فشل تحميل الدعوات"))
    val mm = m.body.optJSONArray("members") ?: JSONArray()
    val ii = i.body.optJSONArray("invitations") ?: JSONArray()
    val ag = m.body.optJSONObject("agency")
    runOnUiThread {
     members = mm; invites = ii; agencyInfo = ag ?: JSONObject()
     agencyName = ag?.optString("agency_name")?.takeIf { it.isNotBlank() } ?: agencyName
     status.text = "تم تحديث $agencyName"
     render()
    }
   } catch (e: Exception) {
    runOnUiThread {
     status.text = if (afterRemoval) "تمت إزالة العضو، لكن تعذر تحديث القائمة. اضغط تحديث." else "تعذر تحديث بيانات الوكالة. حاول مرة أخرى."
     render()
    }
   }
  }
 }

 private fun sendInvitation() {
  val id = input.text.toString().trim()
  if (id.isBlank() || id.length > 40) { input.error = "أدخل ID صحيح"; return }
  val tok = token() ?: run { status.text = "يرجى تسجيل الدخول"; return }
  send.isEnabled = false; send.alpha = 0.5f
  status.text = "جارٍ إرسال الدعوة..."
  worker.execute {
   try {
    val r = AuthApi.post("/api/agency/invitations", JSONObject().put("public_id", id), tok)
    if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إرسال الدعوة"))
    runOnUiThread {
     input.setText(""); status.text = "تم إرسال الدعوة إلى ID $id"
     send.isEnabled = true; send.alpha = 1f
     loadData()
    }
   } catch (e: Exception) {
    runOnUiThread { status.text = e.message ?: "تعذر إرسال الدعوة"; send.isEnabled = true; send.alpha = 1f }
   }
  }
 }

 // ───────────── Lists ─────────────
 private fun renderInvites() {
  if (!::invitesBox.isInitialized) return
  invitesBox.removeAllViews()
  val q = if (::inviteSearch.isInitialized) inviteSearch.text.toString().trim().lowercase() else ""
  val list = (0 until invites.length()).mapNotNull { invites.optJSONObject(it) }.filter { o ->
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   val st = o.optString("status")
   name.lowercase().contains(q) || id.lowercase().contains(q) || st.lowercase().contains(q) || inviteLabel(st).lowercase().contains(q)
  }
  if (list.isEmpty()) {
   invitesBox.addView(empty(Ic.MAIL, if (q.isNotBlank()) "لا توجد دعوات مطابقة" else "لا توجد دعوات"))
   return
  }
  for (o in list) {
   val uid = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $uid" }
   val st = o.optString("status")
   val (fg, sf) = toneOf(st)
   val row = cardView(14)
   val top = hbox()
   top.addView(initials(name, 44), LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(12) })
   val col = vbox()
   col.addView(t(name, 14.5f, ink, true))
   col.addView(t("ID $uid", 12f, muted), lp().apply { topMargin = dp(3) })
   top.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
   top.addView(chip(inviteLabel(st), fg, sf))
   row.addView(top)
   if (st == "pending") row.addView(button("إلغاء الدعوة", 2, Ic.CLOSE) { cancelInvitation(o.optLong("id")) }, lp().apply { topMargin = dp(12) })
   invitesBox.addView(row, lp().apply { bottomMargin = dp(8) })
  }
 }

 private fun renderMembers() {
  if (!::membersBox.isInitialized) return
  membersBox.removeAllViews()
  val q = if (::memberSearch.isInitialized) memberSearch.text.toString().trim().lowercase() else ""
  val list = (0 until members.length()).mapNotNull { members.optJSONObject(it) }.filter { o ->
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   name.lowercase().contains(q) || id.lowercase().contains(q)
  }
  if (list.isEmpty()) {
   membersBox.addView(empty(Ic.USERS, if (q.isNotBlank()) "لا توجد نتائج مطابقة" else "لا يوجد أعضاء منضمّون بعد"))
   return
  }
  for (o in list) {
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   val row = cardView(14).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    background = ripple(card(), 18); isClickable = true; isFocusable = true
   }
   row.addView(initials(name, 46), LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginEnd = dp(12) })
   val col = vbox()
   col.addView(t(name, 14.5f, ink, true))
   col.addView(chip("ID $id", green, greenSoft), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(5) })
   col.addView(t("اضغط لعرض الملف وإدارته", 11f, muted), lp().apply { topMargin = dp(5) })
   row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
   row.addView(ico(Ic.FORWARD, muted, 20), LinearLayout.LayoutParams(dp(20), dp(20)))
   row.setOnClickListener { showMemberDetails(o) }
   membersBox.addView(row, lp().apply { bottomMargin = dp(8) })
  }
 }

 private fun showMemberDetails(member: JSONObject) {
  val name = member.optString("display_name").ifBlank { "—" }
  val id = member.optString("public_id").ifBlank { "—" }
  val joined = member.optString("joined_at").ifBlank { "—" }
  val avatarUrl = member.optString("avatar_url").ifBlank { member.optString("avatar") }
  sheet { box, d ->
   val av = FrameLayout(this)
   av.addView(initials(name, 92), FrameLayout.LayoutParams(dp(92), dp(92)))
   val img = ImageView(this).apply {
    scaleType = ImageView.ScaleType.CENTER_CROP
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
     override fun getOutline(view: View, outline: Outline) { outline.setOval(0, 0, view.width, view.height) }
    }
   }
   av.addView(img, FrameLayout.LayoutParams(dp(92), dp(92)))
   box.addView(av, LinearLayout.LayoutParams(dp(92), dp(92)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12) })
   if (avatarUrl.isNotBlank()) {
    worker.execute {
     try {
      val conn = java.net.URL(avatarUrl).openConnection() as java.net.HttpURLConnection
      conn.connectTimeout = 8000; conn.readTimeout = 8000
      conn.setRequestProperty("User-Agent", "BAK LIVE Android")
      val bitmap = BitmapFactory.decodeStream(conn.inputStream)
      conn.disconnect()
      if (bitmap != null) runOnUiThread { img.setImageBitmap(bitmap) }
     } catch (_: Exception) {}
    }
   }
   box.addView(t(name, 19f, ink, true).apply { gravity = Gravity.CENTER }, lp())
   box.addView(chip("ID $id", brand, brandSoft), LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8); bottomMargin = dp(16) })

   val info = vbox().apply { background = shape(fieldBg, 16); setPadding(dp(12), dp(4), dp(12), dp(4)) }
   info.addView(infoRow(Ic.USER, "معرّف المستخدم (ID)", id))
   info.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))
   info.addView(infoRow(Ic.USERS, "اسم الحساب", name))
   info.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))
   info.addView(infoRow(Ic.CALENDAR, "تاريخ الانضمام", joined))
   box.addView(info, lp())

   val row = hbox()
   row.addView(button("إغلاق", 1) { d.dismiss() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
   row.addView(button("إزالة من الوكالة", 2, Ic.TRASH) { d.dismiss(); confirmRemoveMember(id) }, LinearLayout.LayoutParams(0, -2, 1.4f).apply { marginStart = dp(6) })
   box.addView(row, lp().apply { topMargin = dp(18) })
  }
 }

 private fun confirmRemoveMember(publicId: String) {
  if (publicId.isBlank() || publicId == "—") return
  confirm("إزالة عضو الوكالة", "هل تريد إزالة هذا المذيع من وكالتك؟ لن يتم حذف حسابه.", "إزالة", Ic.TRASH) {
   val tok = token() ?: run { status.text = "يرجى تسجيل الدخول"; return@confirm }
   status.text = "جارٍ إزالة العضو..."
   worker.execute {
    try {
     val r = AuthApi.delete("/api/agency/members/$publicId", tok)
     if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إزالة العضو"))
     runOnUiThread {
      val updated = JSONArray()
      for (i in 0 until members.length()) {
       val item = members.optJSONObject(i)
       if (item != null && item.optString("public_id") != publicId) updated.put(item)
      }
      members = updated
      render()
      status.text = "تمت إزالة العضو من الوكالة بنجاح"
      loadData(afterRemoval = true)
     }
    } catch (e: Exception) {
     runOnUiThread { status.text = "تعذر إزالة العضو. حاول مرة أخرى." }
    }
   }
  }
 }

 private fun cancelInvitation(invitationId: Long) {
  if (invitationId <= 0) return
  confirm("إلغاء الدعوة", "هل تريد إلغاء هذه الدعوة؟", "إلغاء الدعوة", Ic.CLOSE) {
   val tok = token() ?: run { status.text = "يرجى تسجيل الدخول"; return@confirm }
   worker.execute {
    try {
     val r = AuthApi.post("/api/agency/invitations/$invitationId/cancel", JSONObject(), tok)
     if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إلغاء الدعوة"))
     runOnUiThread { status.text = "تم إلغاء الدعوة"; loadData() }
    } catch (e: Exception) {
     runOnUiThread { status.text = e.message ?: "تعذر إلغاء الدعوة" }
    }
   }
  }
 }

 override fun onDestroy() {
  worker.shutdownNow()
  apiExecutor.shutdownNow()
  super.onDestroy()
 }

 private fun render() {
  if (!::invitesBox.isInitialized || !::membersBox.isInitialized) return
  fun count(s: String) = (0 until invites.length()).count { invites.optJSONObject(it)?.optString("status") == s }
  statMembers.text = members.length().toString()
  statPending.text = count("pending").toString()
  anAccepted.text = count("accepted").toString()
  anRejected.text = count("rejected").toString()
  anCancelled.text = count("cancelled").toString()
  anTotal.text = invites.length().toString()
  heroName.text = agencyName
  renderAgencyDetails()
  renderInvites()
  renderMembers()
 }
}

// ───────────── Vector icon set (no emoji, no image resources) ─────────────
private object Ic {
 const val BACK = 0; const val FORWARD = 1; const val REFRESH = 2; const val WALLET = 3; const val USERS = 4
 const val MAIL = 5; const val SEARCH = 6; const val CLOCK = 7; const val SEND = 8; const val CLOSE = 9
 const val CHECK = 10; const val DIAMOND = 11; const val GIFT = 12; const val BUILDING = 13; const val TREND = 14
 const val CALENDAR = 15; const val INFO = 16; const val TRASH = 17; const val USER = 18
}

private class AgencyIcon(private val kind: Int, private val col: Int, private val flip: Boolean) : Drawable() {
 private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
  strokeWidth = 1.9f; color = col
 }
 private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = col }
 private val p = Path()

 private fun poly(c: Canvas, close: Boolean, vararg v: Float) {
  p.reset(); p.moveTo(v[0], v[1])
  var i = 2
  while (i < v.size) { p.lineTo(v[i], v[i + 1]); i += 2 }
  if (close) p.close()
  c.drawPath(p, stroke)
 }

 private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float) =
  c.drawRoundRect(RectF(l, t, r, b), rad, rad, stroke)

 override fun draw(c: Canvas) {
  val b = bounds
  if (b.isEmpty) return
  val s = b.width() / 24f
  c.save()
  c.translate(b.left.toFloat(), b.top.toFloat())
  c.scale(s, s)
  if (flip && (kind == Ic.BACK || kind == Ic.FORWARD || kind == Ic.SEND)) c.scale(-1f, 1f, 12f, 12f)
  when (kind) {
   Ic.BACK -> poly(c, false, 15f, 5f, 8f, 12f, 15f, 19f)
   Ic.FORWARD -> poly(c, false, 9f, 5f, 16f, 12f, 9f, 19f)
   Ic.REFRESH -> {
    c.drawArc(RectF(5f, 5f, 19f, 19f), -40f, 300f, false, stroke)
    poly(c, false, 17.4f, 3.5f, 17.4f, 7.5f, 13.4f, 7.5f)
   }
   Ic.WALLET -> {
    box(c, 3f, 6f, 21f, 20f, 3f)
    poly(c, false, 3f, 10f, 21f, 10f)
    c.drawCircle(16.5f, 15f, 1.3f, fill)
   }
   Ic.USERS -> {
    c.drawCircle(9f, 8f, 3.5f, stroke)
    p.reset(); p.moveTo(2.5f, 20f); p.quadTo(2.5f, 14f, 9f, 14f); p.quadTo(15.5f, 14f, 15.5f, 20f); c.drawPath(p, stroke)
    c.drawCircle(17f, 9f, 2.6f, stroke)
    p.reset(); p.moveTo(17.5f, 14.2f); p.quadTo(21.5f, 14.5f, 21.5f, 19f); c.drawPath(p, stroke)
   }
   Ic.USER -> {
    c.drawCircle(12f, 8f, 4f, stroke)
    p.reset(); p.moveTo(4f, 21f); p.quadTo(4f, 14f, 12f, 14f); p.quadTo(20f, 14f, 20f, 21f); c.drawPath(p, stroke)
   }
   Ic.MAIL -> {
    box(c, 3f, 5f, 21f, 19f, 2.5f)
    poly(c, false, 3.5f, 7.5f, 12f, 13.5f, 20.5f, 7.5f)
   }
   Ic.SEARCH -> {
    c.drawCircle(11f, 11f, 6.5f, stroke)
    poly(c, false, 16f, 16f, 20.5f, 20.5f)
   }
   Ic.CLOCK -> {
    c.drawCircle(12f, 12f, 9f, stroke)
    poly(c, false, 12f, 7f, 12f, 12f, 15.5f, 14f)
   }
   Ic.SEND -> {
    poly(c, true, 21f, 3f, 3f, 10.5f, 10f, 13.5f, 13f, 21f)
    poly(c, false, 10f, 13.5f, 21f, 3f)
   }
   Ic.CLOSE -> {
    poly(c, false, 6f, 6f, 18f, 18f)
    poly(c, false, 18f, 6f, 6f, 18f)
   }
   Ic.CHECK -> poly(c, false, 5f, 12.5f, 10f, 17.5f, 19f, 7.5f)
   Ic.DIAMOND -> {
    poly(c, true, 6.5f, 4f, 17.5f, 4f, 22f, 9.5f, 12f, 21f, 2f, 9.5f)
    poly(c, false, 2f, 9.5f, 22f, 9.5f)
    poly(c, false, 9f, 4f, 7.5f, 9.5f, 12f, 21f)
    poly(c, false, 15f, 4f, 16.5f, 9.5f, 12f, 21f)
   }
   Ic.GIFT -> {
    box(c, 4f, 11f, 20f, 21f, 1.5f)
    box(c, 3f, 7f, 21f, 11f, 1.5f)
    poly(c, false, 12f, 7f, 12f, 21f)
    p.reset(); p.moveTo(12f, 7f); p.quadTo(7f, 7f, 7.5f, 4.5f); p.quadTo(9.5f, 2.5f, 12f, 7f)
    p.moveTo(12f, 7f); p.quadTo(17f, 7f, 16.5f, 4.5f); p.quadTo(14.5f, 2.5f, 12f, 7f)
    c.drawPath(p, stroke)
   }
   Ic.BUILDING -> {
    box(c, 5f, 3f, 19f, 21f, 2f)
    c.drawCircle(9.5f, 8f, 1f, fill); c.drawCircle(14.5f, 8f, 1f, fill)
    c.drawCircle(9.5f, 12f, 1f, fill); c.drawCircle(14.5f, 12f, 1f, fill)
    poly(c, false, 10f, 21f, 10f, 17f, 14f, 17f, 14f, 21f)
   }
   Ic.TREND -> {
    poly(c, false, 3f, 17f, 9f, 11f, 13f, 15f, 21f, 7f)
    poly(c, false, 15f, 7f, 21f, 7f, 21f, 13f)
   }
   Ic.CALENDAR -> {
    box(c, 3f, 5f, 21f, 21f, 3f)
    poly(c, false, 3f, 10f, 21f, 10f)
    poly(c, false, 8f, 3f, 8f, 7f)
    poly(c, false, 16f, 3f, 16f, 7f)
   }
   Ic.INFO -> {
    c.drawCircle(12f, 12f, 9f, stroke)
    poly(c, false, 12f, 11f, 12f, 16.5f)
    c.drawCircle(12f, 7.9f, 1.1f, fill)
   }
   Ic.TRASH -> {
    poly(c, false, 4f, 7f, 20f, 7f)
    poly(c, false, 9f, 7f, 9f, 4f, 15f, 4f, 15f, 7f)
    poly(c, false, 6f, 7f, 7f, 20f, 17f, 20f, 18f, 7f)
    poly(c, false, 10f, 11f, 10f, 16f)
    poly(c, false, 14f, 11f, 14f, 16f)
   }
  }
  c.restore()
 }

 override fun setAlpha(alpha: Int) { stroke.alpha = alpha; fill.alpha = alpha; invalidateSelf() }
 override fun setColorFilter(cf: ColorFilter?) { stroke.colorFilter = cf; fill.colorFilter = cf; invalidateSelf() }
 @Suppress("OVERRIDE_DEPRECATION")
 override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}