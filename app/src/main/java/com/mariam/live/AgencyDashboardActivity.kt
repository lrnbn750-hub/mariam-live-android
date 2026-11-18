package com.mariam.live

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class AgencyDashboardActivity : Activity() {

 companion object {
  const val I_BACK = 0; const val I_CHEV = 1; const val I_GEM = 2; const val I_WALLET = 3
  const val I_REFRESH = 4; const val I_USERS = 5; const val I_USERPLUS = 6; const val I_SEND = 7
  const val I_SEARCH = 8; const val I_TREND = 9; const val I_CLOCK = 10; const val I_CHECK = 11
  const val I_X = 12; const val I_MAIL = 13; const val I_HOME = 14; const val I_INFO = 15
  const val I_ALERT = 16; const val I_TRASH = 17; const val I_GIFT = 18
 }

 private val worker = Executors.newSingleThreadExecutor()
 private val imageWorker = Executors.newFixedThreadPool(2)

 // ── Palette ──────────────────────────────────────────────
 private val bg = Color.parseColor("#0A0814")
 private val bg2 = Color.parseColor("#140E2B")
 private val panel = Color.parseColor("#151129")
 private val panelHi = Color.parseColor("#1C1637")
 private val stroke = Color.parseColor("#2B2450")
 private val ink = Color.WHITE
 private val muted = Color.parseColor("#9088AE")
 private val violet = Color.parseColor("#8B5CF6")
 private val violetLight = Color.parseColor("#C4B5FD")
 private val gold = Color.parseColor("#F5C451")
 private val green = Color.parseColor("#34D399")
 private val red = Color.parseColor("#F87171")
 private val amber = Color.parseColor("#FBBF24")
 private val pink = Color.parseColor("#EC4899")

 private val avatarColors = arrayOf(
  intArrayOf(Color.parseColor("#8B5CF6"), Color.parseColor("#6D28D9")),
  intArrayOf(Color.parseColor("#EC4899"), Color.parseColor("#BE185D")),
  intArrayOf(Color.parseColor("#06B6D4"), Color.parseColor("#0E7490")),
  intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#B45309")),
  intArrayOf(Color.parseColor("#10B981"), Color.parseColor("#047857")),
  intArrayOf(Color.parseColor("#6366F1"), Color.parseColor("#4338CA"))
 )

 // ── Views ────────────────────────────────────────────────
 private lateinit var scroll: ScrollView
 private lateinit var body: LinearLayout
 private lateinit var earningsBox: LinearLayout
 private lateinit var earningsStatus: TextView
 private lateinit var membersEarningsBox: LinearLayout
 private lateinit var historyBox: LinearLayout
 private lateinit var agencyDetailsBox: LinearLayout
 private lateinit var input: EditText
 private lateinit var send: View
 private lateinit var status: TextView
 private lateinit var invitesBox: LinearLayout
 private lateinit var membersBox: LinearLayout
 private lateinit var membersCountText: TextView
 private lateinit var pendingCountText: TextView
 private lateinit var anAccepted: TextView
 private lateinit var anRejected: TextView
 private lateinit var anCancelled: TextView
 private lateinit var anTotal: TextView
 private lateinit var memberSearch: EditText
 private lateinit var inviteSearch: EditText
 private lateinit var heroName: TextView
 private lateinit var heroTotal: TextView
 private lateinit var headerSub: TextView
 private lateinit var earningsAnchor: View
 private lateinit var membersAnchor: View
 private lateinit var inviteAnchor: View

 // ── Data ─────────────────────────────────────────────────
 private var invites = JSONArray()
 private var members = JSONArray()
 private var agencyInfo = JSONObject()
 private var agencyName = "وكالتك"

 private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

 // ══════════════════════════════════════════════════════════
 //  Design helpers
 // ══════════════════════════════════════════════════════════
 private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
 private fun sz(v: Int) = if (v > 0) dp(v) else v
 private fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

 private fun lp(w: Int, h: Int, top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0, weight: Float = 0f) =
  LinearLayout.LayoutParams(sz(w), sz(h), weight).apply {
   topMargin = dp(top); bottomMargin = dp(bottom); marginStart = dp(start); marginEnd = dp(end)
  }

 private fun ll(o: Int = 1) = LinearLayout(this).apply { orientation = o }
 private fun solid(c: Int) = View(this).apply { setBackgroundColor(c) }
 private fun spacer() = View(this)

 private fun rnd(color: Int, r: Int, sw: Int = 0, sc: Int = 0) = GradientDrawable().apply {
  cornerRadius = dp(r).toFloat(); setColor(color); if (sw > 0) setStroke(dp(sw), sc)
 }

 private fun grad(c1: Int, c2: Int, r: Int, o: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT) =
  GradientDrawable(o, intArrayOf(c1, c2)).apply { cornerRadius = dp(r).toFloat() }

 private fun card() = rnd(panel, 18, 1, stroke)

 private fun ripple(content: Drawable, r: Int): Drawable =
  RippleDrawable(
   ColorStateList.valueOf(0x40FFFFFF), content,
   GradientDrawable().apply { cornerRadius = dp(r).toFloat(); setColor(Color.WHITE) }
  )

 private fun View.press() {
  setOnTouchListener { v, e ->
   when (e.action) {
    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
   }
   false
  }
 }

 private fun t(s: String, z: Float = 14f, c: Int = ink, b: Boolean = false) = TextView(this).apply {
  text = s; textSize = z; setTextColor(c); setLineSpacing(0f, 1.12f)
  if (b) typeface = Typeface.DEFAULT_BOLD
 }

 private val ovalOutline = object : ViewOutlineProvider() {
  override fun getOutline(view: View, outline: Outline) {
   outline.setOval(0, 0, view.width, view.height)
  }
 }

 // ── Vector-style icons drawn in code (no resources needed) ──
 private fun poly(closed: Boolean, vararg v: Float): Path {
  val p = Path()
  var i = 0
  while (i + 1 < v.size) {
   if (i == 0) p.moveTo(v[0], v[1]) else p.lineTo(v[i], v[i + 1])
   i += 2
  }
  if (closed) p.close()
  return p
 }

 private inner class IconDrawable(val kind: Int, val col: Int) : Drawable() {
  private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
   style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; strokeWidth = 2f; color = col
  }
  private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = col }
  override fun getIntrinsicWidth() = 96
  override fun getIntrinsicHeight() = 96
  override fun setAlpha(alpha: Int) { p.alpha = alpha; f.alpha = alpha }
  override fun setColorFilter(cf: ColorFilter?) { p.colorFilter = cf; f.colorFilter = cf }
  @Suppress("DEPRECATION")
  override fun getOpacity() = PixelFormat.TRANSLUCENT
  override fun draw(c: Canvas) {
   val b = bounds
   if (b.width() <= 0 || b.height() <= 0) return
   val s = minOf(b.width(), b.height()) / 24f
   c.save()
   c.translate(b.left + (b.width() - 24 * s) / 2f, b.top + (b.height() - 24 * s) / 2f)
   c.scale(s, s)
   // the screen is RTL: flip directional arrows
   if (kind == I_BACK || kind == I_CHEV) { c.scale(-1f, 1f); c.translate(-24f, 0f) }
   when (kind) {
    I_BACK -> c.drawPath(poly(false, 15f, 5f, 8f, 12f, 15f, 19f), p)
    I_CHEV -> c.drawPath(poly(false, 9f, 5f, 16f, 12f, 9f, 19f), p)
    I_GEM -> {
     c.drawPath(poly(true, 6f, 3f, 18f, 3f, 22f, 9f, 12f, 21f, 2f, 9f), p)
     c.drawLine(2f, 9f, 22f, 9f, p)
     c.drawLine(9f, 3f, 7f, 9f, p); c.drawLine(15f, 3f, 17f, 9f, p)
     c.drawLine(7f, 9f, 12f, 21f, p); c.drawLine(17f, 9f, 12f, 21f, p)
    }
    I_WALLET -> {
     c.drawRoundRect(RectF(3f, 6f, 21f, 20f), 3f, 3f, p)
     c.drawPath(poly(false, 3f, 10f, 21f, 10f), p)
     c.drawPath(poly(false, 7f, 6f, 16f, 3.5f, 17f, 6f), p)
     c.drawCircle(16.5f, 15f, 1.3f, f)
    }
    I_REFRESH -> {
     c.drawArc(RectF(3f, 3f, 21f, 21f), 19.5f, 295.5f, false, p)
     c.drawLine(18.37f, 5.64f, 23f, 10f, p)
     c.drawPath(poly(false, 23f, 4f, 23f, 10f, 17f, 10f), p)
    }
    I_USERS -> {
     c.drawCircle(9f, 7f, 4f, p)
     val body = Path().apply { moveTo(1f, 21f); lineTo(1f, 19f); quadTo(1f, 15f, 5f, 15f); lineTo(13f, 15f); quadTo(17f, 15f, 17f, 19f); lineTo(17f, 21f) }
     c.drawPath(body, p)
     c.drawArc(RectF(12f, 3f, 20f, 11f), -75f, 150f, false, p)
     val body2 = Path().apply { moveTo(23f, 21f); lineTo(23f, 19f); quadTo(23f, 16.2f, 20f, 15.13f) }
     c.drawPath(body2, p)
    }
    I_USERPLUS -> {
     c.drawCircle(9f, 7f, 4f, p)
     val body = Path().apply { moveTo(1f, 21f); lineTo(1f, 19f); quadTo(1f, 15f, 5f, 15f); lineTo(13f, 15f); quadTo(17f, 15f, 17f, 19f); lineTo(17f, 21f) }
     c.drawPath(body, p)
     c.drawLine(20f, 8f, 20f, 14f, p); c.drawLine(17f, 11f, 23f, 11f, p)
    }
    I_SEND -> {
     c.drawLine(22f, 2f, 11f, 13f, p)
     c.drawPath(poly(true, 22f, 2f, 15f, 22f, 11f, 13f, 2f, 9f), p)
    }
    I_SEARCH -> {
     c.drawCircle(11f, 11f, 8f, p)
     c.drawLine(21f, 21f, 16.65f, 16.65f, p)
    }
    I_TREND -> {
     c.drawPath(poly(false, 23f, 6f, 13.5f, 15.5f, 8.5f, 10.5f, 1f, 18f), p)
     c.drawPath(poly(false, 17f, 6f, 23f, 6f, 23f, 12f), p)
    }
    I_CLOCK -> {
     c.drawCircle(12f, 12f, 10f, p)
     c.drawPath(poly(false, 12f, 6f, 12f, 12f, 16f, 14f), p)
    }
    I_CHECK -> c.drawPath(poly(false, 20f, 6f, 9f, 17f, 4f, 12f), p)
    I_X -> { c.drawLine(18f, 6f, 6f, 18f, p); c.drawLine(6f, 6f, 18f, 18f, p) }
    I_MAIL -> {
     c.drawRoundRect(RectF(2f, 4f, 22f, 20f), 2.5f, 2.5f, p)
     c.drawPath(poly(false, 22f, 6f, 12f, 13f, 2f, 6f), p)
    }
    I_HOME -> {
     c.drawPath(poly(true, 3f, 9f, 12f, 2f, 21f, 9f, 21f, 20f, 3f, 20f), p)
     c.drawPath(poly(false, 9f, 20f, 9f, 13f, 15f, 13f, 15f, 20f), p)
    }
    I_INFO -> {
     c.drawCircle(12f, 12f, 10f, p)
     c.drawLine(12f, 16f, 12f, 12f, p)
     c.drawCircle(12f, 8f, 1.2f, f)
    }
    I_ALERT -> {
     c.drawPath(poly(true, 12f, 3f, 22f, 20f, 2f, 20f), p)
     c.drawLine(12f, 9f, 12f, 14f, p)
     c.drawCircle(12f, 17f, 1.2f, f)
    }
    I_TRASH -> {
     c.drawLine(3f, 6f, 21f, 6f, p)
     c.drawPath(poly(false, 5f, 6f, 6.2f, 20f, 8f, 22f, 16f, 22f, 17.8f, 20f, 19f, 6f), p)
     c.drawLine(10f, 11f, 10f, 17f, p); c.drawLine(14f, 11f, 14f, 17f, p)
     c.drawPath(poly(false, 9f, 6f, 9f, 3f, 15f, 3f, 15f, 6f), p)
    }
    I_GIFT -> {
     c.drawRoundRect(RectF(3f, 8f, 21f, 12f), 1f, 1f, p)
     c.drawPath(poly(false, 4f, 12f, 4f, 21f, 20f, 21f, 20f, 12f), p)
     c.drawLine(12f, 8f, 12f, 21f, p)
     c.drawPath(poly(false, 12f, 8f, 9f, 8f, 7.5f, 6.5f, 9f, 5f, 10.5f, 5f, 12f, 8f, 13.5f, 5f, 15f, 5f, 16.5f, 6.5f, 15f, 8f, 12f, 8f), p)
    }
   }
   c.restore()
  }
 }

 private fun iv(kind: Int, tint: Int) = ImageView(this).apply {
  setImageDrawable(IconDrawable(kind, tint)); scaleType = ImageView.ScaleType.FIT_CENTER
 }

 private fun badge(ic: Int, tint: Int, isz: Int = 20, r: Int = 12) = FrameLayout(this).apply {
  background = rnd(withAlpha(tint, 38), r)
  addView(iv(ic, tint), FrameLayout.LayoutParams(dp(isz), dp(isz), Gravity.CENTER))
 }

 private fun pill(s: String, c: Int) = t(s, 11f, c, true).apply {
  gravity = Gravity.CENTER; setPadding(dp(10), dp(4), dp(10), dp(4)); background = rnd(withAlpha(c, 36), 20)
 }

 private fun avatar(name: String, size: Int): TextView {
  val ch = name.trim().take(1).uppercase().ifBlank { "?" }
  val pal = avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size]
  return t(ch, size * 0.38f, ink, true).apply {
   gravity = Gravity.CENTER
   background = GradientDrawable(GradientDrawable.Orientation.TL_BR, pal).apply { shape = GradientDrawable.OVAL }
  }
 }

 private fun gemValue(v: String, size: Float, color: Int = gold, iconColor: Int = gold): LinearLayout {
  val row = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  val isz = (size * 1.05f).toInt()
  row.addView(iv(I_GEM, iconColor), lp(isz, isz, end = 6))
  row.addView(t(v, size, color, true))
  return row
 }

 private fun button(s: String, ic: Int = -1, style: Int = 0, action: () -> Unit): LinearLayout {
  val fg = when (style) { 1 -> violetLight; 2 -> red; else -> ink }
  val base: Drawable = when (style) {
   1 -> rnd(withAlpha(violet, 34), 14, 1, withAlpha(violet, 90))
   2 -> rnd(withAlpha(red, 28), 14, 1, withAlpha(red, 90))
   3 -> grad(Color.parseColor("#F87171"), Color.parseColor("#DC2626"), 14)
   else -> grad(Color.parseColor("#9D6BFF"), Color.parseColor("#6D28D9"), 14)
  }
  return ll(0).apply {
   gravity = Gravity.CENTER
   setPadding(dp(14), dp(12), dp(14), dp(12))
   background = ripple(base, 14)
   if (ic >= 0) addView(iv(ic, fg), lp(18, 18, end = 8))
   addView(t(s, 14f, fg, true))
   isClickable = true; isFocusable = true
   setOnClickListener { action() }
   press()
  }
 }

 private fun rowCard(click: (() -> Unit)? = null): LinearLayout = ll().apply {
  setPadding(dp(14), dp(14), dp(14), dp(14))
  background = ripple(card(), 18)
  if (click != null) { isClickable = true; isFocusable = true; setOnClickListener { click() }; press() }
 }

 private fun empty(s: String, ic: Int): LinearLayout = ll().apply {
  gravity = Gravity.CENTER; setPadding(dp(16), dp(26), dp(16), dp(26)); background = card()
  addView(iv(ic, muted), lp(30, 30, bottom = 8))
  addView(t(s, 12f, muted).apply { gravity = Gravity.CENTER })
 }

 private fun sectionView(s: String, sub: String, ic: Int): LinearLayout {
  val row = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  if (ic >= 0) row.addView(badge(ic, violetLight, 19, 12), lp(38, 38, end = 12))
  val col = ll()
  col.addView(t(s, 17f, ink, true))
  if (sub.isNotBlank()) col.addView(t(sub, 12f, muted), lp(-1, -2, top = 2))
  row.addView(col, lp(0, -2, weight = 1f))
  return row
 }

 private fun section(s: String, sub: String = "", ic: Int = -1): View {
  val row = sectionView(s, sub, ic)
  body.addView(row, lp(-1, -2, top = 28, bottom = 12))
  return row
 }

 private fun field(hint: String, ic: Int, tsz: Float = 14f): Pair<LinearLayout, EditText> {
  val et = EditText(this).apply {
   this.hint = hint; setSingleLine(); this.textSize = tsz
   setTextColor(ink); setHintTextColor(muted); background = null; setPadding(0, dp(10), 0, dp(10))
  }
  val box = ll(0).apply {
   gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(52)
   setPadding(dp(14), 0, dp(14), 0); background = rnd(panel, 16, 1, stroke)
  }
  box.addView(iv(ic, muted), lp(18, 18, end = 10))
  box.addView(et, lp(0, -2, weight = 1f))
  et.setOnFocusChangeListener { _, focused ->
   box.background = rnd(panel, 16, 1, if (focused) violet else stroke)
  }
  return Pair(box, et)
 }

 private fun watch(et: EditText, f: () -> Unit) {
  et.addTextChangedListener(object : TextWatcher {
   override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
   override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { f() }
   override fun afterTextChanged(s: Editable?) {}
  })
 }

 private inner class HeroBg : Drawable() {
  private val p = Paint(Paint.ANTI_ALIAS_FLAG)
  override fun draw(c: Canvas) {
   val b = bounds
   val rect = RectF(b)
   val r = dp(26).toFloat()
   p.style = Paint.Style.FILL
   p.shader = LinearGradient(
    b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(),
    intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#4C1D95"), Color.parseColor("#1E1038")),
    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
   )
   c.drawRoundRect(rect, r, r, p)
   p.shader = null
   c.save()
   c.clipPath(Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) })
   p.color = 0x1AFFFFFF
   c.drawCircle(b.left + dp(40).toFloat(), b.top - dp(10).toFloat(), dp(120).toFloat(), p)
   p.color = withAlpha(gold, 26)
   c.drawCircle(b.right - dp(10).toFloat(), b.bottom + dp(10).toFloat(), dp(100).toFloat(), p)
   p.color = 0x12FFFFFF
   c.drawCircle(b.right - dp(60).toFloat(), b.top + dp(30).toFloat(), dp(46).toFloat(), p)
   c.restore()
   p.style = Paint.Style.STROKE
   p.strokeWidth = dp(1).toFloat()
   p.color = 0x33FFFFFF
   c.drawRoundRect(rect, r, r, p)
  }
  override fun setAlpha(alpha: Int) {}
  override fun setColorFilter(cf: ColorFilter?) {}
  @Suppress("DEPRECATION")
  override fun getOpacity() = PixelFormat.TRANSLUCENT
 }

 private fun heroAction(label: String, ic: Int, click: () -> Unit): LinearLayout {
  val c = ll().apply {
   gravity = Gravity.CENTER; setPadding(dp(6), dp(12), dp(6), dp(10))
   background = ripple(rnd(0x1FFFFFFF, 16, 1, 0x2EFFFFFF), 16)
   isClickable = true; isFocusable = true
   setOnClickListener { click() }; press()
  }
  c.addView(badge(ic, Color.WHITE, 18, 14), lp(36, 36, bottom = 7))
  c.addView(t(label, 11.5f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
  return c
 }

 // ══════════════════════════════════════════════════════════
 //  onCreate
 // ══════════════════════════════════════════════════════════
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = bg; window.navigationBarColor = bg
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

  val root = ll().apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bg2, bg)) }
  root.layoutDirection = View.LAYOUT_DIRECTION_RTL

  // ── Top bar ──
  val head = ll(0).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(10)) }
  val back = FrameLayout(this).apply {
   background = ripple(rnd(withAlpha(Color.WHITE, 20), 14, 1, withAlpha(Color.WHITE, 26)), 14)
   isClickable = true; isFocusable = true
   setOnClickListener { finish() }; press()
  }
  back.addView(iv(I_BACK, ink), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
  head.addView(back, lp(42, 42))
  val titleCol = ll().apply { gravity = Gravity.CENTER }
  titleCol.addView(t("لوحة الوكالة", 18f, ink, true))
  headerSub = t(agencyName, 11f, muted)
  titleCol.addView(headerSub, lp(-2, -2, top = 1))
  head.addView(titleCol, lp(0, -2, weight = 1f))
  head.addView(
   t("BAK", 12f, Color.WHITE, true).apply {
    gravity = Gravity.CENTER; letterSpacing = 0.1f
    setPadding(dp(12), dp(7), dp(12), dp(7))
    background = grad(Color.parseColor("#A97BFF"), Color.parseColor("#6D28D9"), 20)
   }
  )
  root.addView(head)

  scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
  body = ll().apply { setPadding(dp(16), dp(6), dp(16), dp(30)) }
  scroll.addView(body)
  root.addView(scroll, lp(-1, 0, weight = 1f))
  setContentView(root)

  // ── Hero ──
  val hero = ll().apply { setPadding(dp(20), dp(20), dp(20), dp(18)); background = HeroBg() }
  val heroTop = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  heroTop.addView(
   t("AGENCY • PARTNER DASHBOARD", 10f, Color.parseColor("#E4CCF8"), true).apply {
    letterSpacing = 0.08f; setPadding(dp(10), dp(5), dp(10), dp(5)); background = rnd(0x26FFFFFF, 20)
   }, lp(-2, -2)
  )
  heroTop.addView(spacer(), lp(0, 1, weight = 1f))
  heroTop.addView(badge(I_HOME, Color.WHITE, 18, 13), lp(36, 36))
  hero.addView(heroTop)
  heroName = t(agencyName, 22f, ink, true)
  hero.addView(heroName, lp(-1, -2, top = 14))
  hero.addView(t("إدارة وكالتك وصُنّاع المحتوى", 13f, Color.parseColor("#D9CCF2")), lp(-1, -2, top = 3))
  hero.addView(solid(0x24FFFFFF), lp(-1, 1, top = 16, bottom = 14))
  hero.addView(t("إجمالي أرباح الوكالة", 12f, Color.parseColor("#CDBDEB"), true))
  heroTotal = t("—", 30f, gold, true)
  val heroTotalRow = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  heroTotalRow.addView(iv(I_GEM, gold), lp(26, 26, end = 8))
  heroTotalRow.addView(heroTotal)
  hero.addView(heroTotalRow, lp(-1, -2, top = 6))
  val actions = ll(0)
  actions.addView(heroAction("أرباح الوكيل", I_WALLET) { scroll.smoothScrollTo(0, earningsAnchor.top - dp(8)) }, lp(0, -2, weight = 1f, end = 4))
  actions.addView(heroAction("الأعضاء", I_USERS) { scroll.smoothScrollTo(0, membersAnchor.top - dp(8)) }, lp(0, -2, weight = 1f, start = 4, end = 4))
  actions.addView(heroAction("دعوة جديدة", I_USERPLUS) { scroll.smoothScrollTo(0, inviteAnchor.top - dp(8)) }, lp(0, -2, weight = 1f, start = 4))
  hero.addView(actions, lp(-1, -2, top = 18))
  body.addView(hero)

  // ── Earnings ──
  earningsAnchor = section("أرباح الوكالة", "12% من حصة المنصة • مستقلة عن أرباح المذيع", I_WALLET)
  earningsBox = ll()
  body.addView(earningsBox, lp(-1, -2))
  val earningsRow = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  earningsStatus = t("جارٍ تحميل الأرباح...", 12f, muted)
  earningsRow.addView(earningsStatus, lp(0, -2, weight = 1f))
  earningsRow.addView(
   button("تحديث الأرباح", I_REFRESH, 1) { loadEarnings() }.apply { setPadding(dp(14), dp(8), dp(14), dp(8)) },
   lp(-2, 40)
  )
  body.addView(earningsRow, lp(-1, -2, top = 12))

  section("عمولة الوكالة حسب العضو", "عمولة الوكالة الناتجة عن هدايا كل عضو • ليست أرباح المذيع", I_USERS)
  membersEarningsBox = ll()
  body.addView(membersEarningsBox, lp(-1, -2))

  section("سجل الأرباح", "آخر عمليات العمولة المسجلة", I_CLOCK)
  historyBox = ll()
  body.addView(historyBox, lp(-1, -2))

  agencyDetailsBox = ll()
  body.addView(agencyDetailsBox, lp(-1, -2))
  renderAgencyDetails()

  // ── Summary ──
  section("ملخص الوكالة", "بيانات حقيقية من السيرفر", I_TREND)
  val statsRow = ll(0)
  val (membersCard, membersTv) = statCard("الأعضاء", I_USERS, green)
  val (pendingCard, pendingTv) = statCard("دعوات معلّقة", I_MAIL, amber)
  membersCountText = membersTv; pendingCountText = pendingTv
  statsRow.addView(membersCard, lp(0, -2, weight = 1f, end = 5))
  statsRow.addView(pendingCard, lp(0, -2, weight = 1f, start = 5))
  body.addView(statsRow)

  val analytics = ll(0).apply { setPadding(dp(6), dp(14), dp(6), dp(14)); background = card() }
  val (a1, v1) = miniStat("مقبولة", green); anAccepted = v1
  val (a2, v2) = miniStat("مرفوضة", red); anRejected = v2
  val (a3, v3) = miniStat("ملغاة", muted); anCancelled = v3
  val (a4, v4) = miniStat("الإجمالي", violetLight); anTotal = v4
  analytics.addView(a1, lp(0, -2, weight = 1f)); analytics.addView(a2, lp(0, -2, weight = 1f))
  analytics.addView(a3, lp(0, -2, weight = 1f)); analytics.addView(a4, lp(0, -2, weight = 1f))
  body.addView(analytics, lp(-1, -2, top = 10))

  val statusRow = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  status = t("جارٍ تحميل البيانات...", 12f, muted)
  statusRow.addView(status, lp(0, -2, weight = 1f))
  statusRow.addView(
   button("تحديث البيانات", I_REFRESH, 1) { loadData() }.apply { setPadding(dp(14), dp(8), dp(14), dp(8)) },
   lp(-2, 40)
  )
  body.addView(statusRow, lp(-1, -2, top = 12))

  // ── Invite ──
  inviteAnchor = section("دعوة صانع محتوى", "أدخل ID المستخدم لإرسال دعوة حقيقية", I_USERPLUS)
  val (inBox, inEt) = field("معرّف المستخدم (ID)", I_USERPLUS, 15f)
  input = inEt
  body.addView(inBox, lp(-1, -2))
  send = button("إرسال دعوة الوكالة", I_SEND) { sendInvitation() }
  body.addView(send, lp(-1, 50, top = 10))

  section("دعوات الوكالة", "حالات الدعوات المحفوظة", I_MAIL)
  val (invSearchBox, invSearchEt) = field("بحث بالدعوة: الاسم أو ID أو الحالة", I_SEARCH)
  inviteSearch = invSearchEt
  body.addView(invSearchBox, lp(-1, -2, bottom = 10))
  invitesBox = ll()
  body.addView(invitesBox, lp(-1, -2))

  // ── Members ──
  membersAnchor = section("أعضاء الوكالة", "المستخدمون الذين قبلوا الدعوة", I_USERS)
  val (memSearchBox, memSearchEt) = field("بحث بالاسم أو ID", I_SEARCH)
  memberSearch = memSearchEt
  body.addView(memSearchBox, lp(-1, -2, bottom = 10))
  membersBox = ll()
  body.addView(membersBox, lp(-1, -2))

  watch(inviteSearch) { renderInvites() }
  watch(memberSearch) { renderMembers() }

  // ── Footer ──
  val footer = ll(0).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = card() }
  footer.addView(badge(I_INFO, muted, 16, 10), lp(30, 30, end = 10))
  footer.addView(
    t("بيانات الدعوات والأعضاء من قاعدة البيانات. تقارير الألماس والأرباح ستُربط عند توفر مصدرها الحقيقي.", 11f, muted),
    lp(0, -2, weight = 1f)
  )
  body.addView(footer, lp(-1, -2, top = 22))

  // entrance animation
  for (i in 0 until minOf(body.childCount, 7)) {
   val v = body.getChildAt(i)
   v.alpha = 0f; v.translationY = dp(18).toFloat()
   v.animate().alpha(1f).translationY(0f).setStartDelay(i * 60L).setDuration(380).start()
  }

  loadData()
  loadEarnings()
 }

 override fun onDestroy() {
  super.onDestroy()
  worker.shutdownNow(); imageWorker.shutdownNow()
 }

 // ══════════════════════════════════════════════════════════
 //  Cards
 // ══════════════════════════════════════════════════════════
 private fun statCard(title: String, ic: Int, color: Int): Pair<LinearLayout, TextView> {
  val v = t("—", 26f, ink, true)
  val c = ll().apply { setPadding(dp(14), dp(14), dp(14), dp(14)); background = card() }
  val h = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  h.addView(badge(ic, color, 17, 11), lp(34, 34, end = 10))
  h.addView(t(title, 12f, muted, true), lp(0, -2, weight = 1f))
  c.addView(h)
  c.addView(v, lp(-1, -2, top = 12))
  return Pair(c, v)
 }

 private fun miniStat(label: String, color: Int): Pair<LinearLayout, TextView> {
  val v = t("—", 18f, color, true).apply { gravity = Gravity.CENTER }
  val c = ll().apply { gravity = Gravity.CENTER }
  c.addView(v)
  c.addView(t(label, 11f, muted).apply { gravity = Gravity.CENTER }, lp(-2, -2, top = 3))
  return Pair(c, v)
 }

 private fun totalCard(value: String): LinearLayout {
  val c = ll().apply {
   setPadding(dp(18), dp(18), dp(18), dp(18))
   background = grad(Color.parseColor("#2B2150"), Color.parseColor("#17122D"), 20, GradientDrawable.Orientation.TL_BR)
    .apply { setStroke(dp(1), withAlpha(gold, 80)) }
  }
  val h = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  h.addView(badge(I_GEM, gold, 19, 12), lp(38, 38, end = 10))
  h.addView(t("إجمالي أرباح الوكالة", 13f, Color.parseColor("#E6DEF7"), true), lp(0, -2, weight = 1f))
  h.addView(pill("12%", gold))
  c.addView(h)
  c.addView(gemValue(value, 32f, ink, gold), lp(-1, -2, top = 14))
  return c
 }

 private fun earnCard(label: String, value: String, ic: Int, color: Int): LinearLayout {
  val c = ll().apply { setPadding(dp(14), dp(14), dp(14), dp(14)); background = card() }
  val h = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
  h.addView(badge(ic, color, 16, 10), lp(30, 30, end = 8))
  h.addView(t(label, 12f, muted, true), lp(0, -2, weight = 1f))
  c.addView(h)
  c.addView(gemValue(value, 19f, ink, gold), lp(-1, -2, top = 12))
  return c
 }

 private fun miniCard(label: String, value: String, ic: Int, color: Int): LinearLayout {
  val c = ll().apply { setPadding(dp(12), dp(12), dp(12), dp(12)); background = card() }
  c.addView(iv(ic, color), lp(18, 18, bottom = 8))
  c.addView(t(label, 11f, muted, true))
  c.addView(gemValue(value, 14f, ink, gold), lp(-1, -2, top = 5))
  return c
 }

 private fun coins(value: Long): String = String.format(java.util.Locale.US, "%,d", value)

 // ══════════════════════════════════════════════════════════
 //  Earnings (API paths unchanged)
 // ══════════════════════════════════════════════════════════
 private fun loadEarnings() {
  val tok = token() ?: run { if (::earningsStatus.isInitialized) earningsStatus.text = "يرجى تسجيل الدخول"; return }
  if (::earningsStatus.isInitialized) earningsStatus.text = "جارٍ تحميل تقرير الأرباح..."
  worker.execute {
   try {
    val overview = AuthApi.get("/api/agency/earnings", tok)
    val perMember = AuthApi.get("/api/agency/earnings/members", tok)
    val history = AuthApi.get("/api/agency/earnings/history?limit=30", tok)
    if (overview.status !in 200..299) throw Exception(overview.body.optString("error", "فشل تحميل الأرباح"))
    if (perMember.status !in 200..299) throw Exception(perMember.body.optString("error", "فشل تحميل أرباح الأعضاء"))
    if (history.status !in 200..299) throw Exception(history.body.optString("error", "فشل تحميل سجل الأرباح"))
    val o = overview.body.optJSONObject("coin_earnings") ?: JSONObject()
    val m = perMember.body.optJSONArray("members") ?: JSONArray()
    val h = history.body.optJSONArray("coin_history") ?: JSONArray()
    runOnUiThread { renderEarnings(o, m, h); earningsStatus.text = "تم تحديث الأرباح من الخادم" }
   } catch (e: Exception) {
    runOnUiThread { earningsStatus.text = e.message ?: "تعذر تحميل الأرباح. حاول تحديث التقرير." }
   }
  }
 }

 private fun renderEarnings(o: JSONObject, m: JSONArray, h: JSONArray) {
  if (!::earningsBox.isInitialized) return
  earningsBox.removeAllViews()
  val total = o.optLong("total_coins", 0); val available = o.optLong("available_coins", 0); val paid = o.optLong("paid_coins", 0)
  val today = o.optLong("today_coins", 0); val week = o.optLong("last_7_days_coins", 0); val month = o.optLong("month_coins", 0)
  heroTotal.text = coins(total)

  earningsBox.addView(totalCard(coins(total)))
  val row2 = ll(0)
  row2.addView(earnCard("العملات المتاحة", coins(available), I_CHECK, green), lp(0, -2, weight = 1f, end = 5))
  row2.addView(earnCard("عملات تمت تسويتها", coins(paid), I_WALLET, violetLight), lp(0, -2, weight = 1f, start = 5))
  earningsBox.addView(row2, lp(-1, -2, top = 10))
  val row3 = ll(0)
  row3.addView(miniCard("أرباح اليوم", coins(today), I_TREND, green), lp(0, -2, weight = 1f, end = 4))
  row3.addView(miniCard("آخر 7 أيام", coins(week), I_CLOCK, amber), lp(0, -2, weight = 1f, start = 4, end = 4))
  row3.addView(miniCard("هذا الشهر", coins(month), I_GIFT, pink), lp(0, -2, weight = 1f, start = 4))
  earningsBox.addView(row3, lp(-1, -2, top = 10))

  membersEarningsBox.removeAllViews()
  if (m.length() == 0) membersEarningsBox.addView(empty("لا توجد بيانات أرباح أعضاء بعد", I_USERS))
  for (i in 0 until m.length()) {
   val item = m.optJSONObject(i) ?: continue
   val id = item.optString("public_id", "—")
   val name = item.optString("display_name").ifBlank { item.optString("username").ifBlank { "ID $id" } }
   val totalMember = item.optLong("total_coins", 0); val availMember = item.optLong("available_coins", 0)
   val gifts = item.optLong("coin_gifts_count", 0)
   val c = rowCard()
   val r = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   r.addView(avatar(name, 46), lp(46, 46, end = 12))
   val mid = ll()
   mid.addView(t(name, 14f, ink, true))
   val sub = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   sub.addView(iv(I_GIFT, muted), lp(13, 13, end = 4))
   sub.addView(t("ID $id • هدايا: $gifts", 11f, muted))
   mid.addView(sub, lp(-1, -2, top = 4))
   r.addView(mid, lp(0, -2, weight = 1f))
   val endCol = ll().apply { gravity = Gravity.END }
   endCol.addView(t("عمولة الوكالة", 10f, muted))
   endCol.addView(gemValue(coins(totalMember), 15f, ink, gold), lp(-2, -2, top = 2))
   endCol.addView(t("متاح: " + coins(availMember), 11f, green, true), lp(-2, -2, top = 3))
   r.addView(endCol)
   c.addView(r)
   membersEarningsBox.addView(c, lp(-1, -2, bottom = 8))
  }

  historyBox.removeAllViews()
  if (h.length() == 0) historyBox.addView(empty("لا توجد عمليات مسجلة حتى الآن", I_CLOCK))
  for (i in 0 until h.length()) {
   val item = h.optJSONObject(i) ?: continue
   val name = item.optString("display_name").ifBlank { item.optString("public_id", "عضو") }
   val amount = item.optLong("amount_coins", 0)
   val st = item.optString("status")
   val statusLabel = when (st) { "available" -> "متاح"; "pending" -> "قيد الانتظار"; "paid" -> "مدفوع"; "reversed" -> "معكوس"; else -> item.optString("status", "—") }
   val stColor = when (st) { "available" -> green; "pending" -> amber; "paid" -> violetLight; "reversed" -> red; else -> muted }
   val date = item.optString("created_at", "").replace("T", " ").take(16)
   val c = rowCard()
   val r = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   r.addView(badge(if (st == "reversed") I_X else I_TREND, stColor, 18, 20), lp(40, 40, end = 12))
   val mid = ll()
   mid.addView(t(name, 14f, ink, true))
   mid.addView(t(date, 11f, muted), lp(-1, -2, top = 3))
   r.addView(mid, lp(0, -2, weight = 1f))
   val endCol = ll().apply { gravity = Gravity.END }
   endCol.addView(gemValue("+" + coins(amount), 14f, ink, gold))
   endCol.addView(pill(statusLabel, stColor), lp(-2, -2, top = 6))
   r.addView(endCol)
   c.addView(r)
   historyBox.addView(c, lp(-1, -2, bottom = 8))
  }
 }

 // ══════════════════════════════════════════════════════════
 //  Agency details
 // ══════════════════════════════════════════════════════════
 private fun labelFor(key: String) = when (key) {
  "agency_name" -> "اسم الوكالة"
  "agency_code" -> "كود الوكالة"
  "status" -> "الحالة"
  "created_at" -> "تاريخ الإنشاء"
  else -> key.replace("_", " ")
 }

 private fun renderAgencyDetails() {
  if (!::agencyDetailsBox.isInitialized) return
  agencyDetailsBox.removeAllViews()
  val rows = ArrayList<Pair<String, String>>()
  for (key in agencyInfo.keys().asSequence().toList()) {
   val raw = agencyInfo.opt(key)
   if (raw == null || raw == JSONObject.NULL || raw is JSONObject || raw is JSONArray) continue
   if (key == "id" || key == "user_id" || key.endsWith("_user_id")) continue
   var value = raw.toString()
   if (value.isBlank() || value == "null") continue
   if (key.endsWith("_at")) value = value.replace("T", " ").take(16)
   rows.add(Pair(labelFor(key), value))
  }
  if (rows.isEmpty()) return
  agencyDetailsBox.addView(sectionView("بيانات الوكالة", "معلومات الوكالة من استجابة السيرفر", I_HOME), lp(-1, -2, top = 28, bottom = 12))
  val panelBox = ll().apply { setPadding(dp(16), dp(8), dp(16), dp(8)); background = card() }
  for ((i, pair) in rows.withIndex()) {
   val row = ll(0).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)) }
   row.addView(t(pair.first, 12f, muted), lp(0, -2, weight = 1f))
   row.addView(t(pair.second, 13f, ink, true).apply { gravity = Gravity.END }, lp(0, -2, weight = 1.3f))
   panelBox.addView(row)
   if (i < rows.size - 1) panelBox.addView(solid(withAlpha(Color.WHITE, 16)), lp(-1, 1))
  }
  agencyDetailsBox.addView(panelBox, lp(-1, -2))
 }

 // ══════════════════════════════════════════════════════════
 //  Members / invitations data (API paths unchanged)
 // ══════════════════════════════════════════════════════════
 private fun loadData(afterRemoval: Boolean = false) {
  val tok = token() ?: run { status.text = "يرجى تسجيل الدخول مجدداً"; return }
  if (!afterRemoval) status.text = "جارٍ تحديث البيانات..."
  worker.execute {
   try {
    val m = AuthApi.get("/api/agency/members", tok); val i = AuthApi.get("/api/agency/invitations", tok)
    if (m.status !in 200..299) throw Exception(m.body.optString("error", "فشل تحميل الأعضاء"))
    if (i.status !in 200..299) throw Exception(i.body.optString("error", "فشل تحميل الدعوات"))
    val mm = m.body.optJSONArray("members") ?: JSONArray(); val ii = i.body.optJSONArray("invitations") ?: JSONArray(); val ag = m.body.optJSONObject("agency")
    runOnUiThread {
     members = mm; invites = ii; agencyInfo = ag ?: JSONObject()
     agencyName = ag?.optString("agency_name")?.takeIf { it.isNotBlank() } ?: agencyName
     heroName.text = agencyName; headerSub.text = agencyName
     status.text = "تم تحديث $agencyName"
     render(); renderAgencyDetails()
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
  send.isEnabled = false; send.alpha = 0.5f; status.text = "جارٍ إرسال الدعوة..."
  worker.execute {
   try {
    val r = AuthApi.post("/api/agency/invitations", JSONObject().put("public_id", id), tok)
    if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إرسال الدعوة"))
    runOnUiThread { input.setText(""); status.text = "تم إرسال الدعوة إلى ID $id"; send.isEnabled = true; send.alpha = 1f }
    loadData()
   } catch (e: Exception) {
    runOnUiThread { status.text = e.message ?: "تعذر إرسال الدعوة"; send.isEnabled = true; send.alpha = 1f }
   }
  }
 }

 private fun inviteStatusLabel(s: String) = when (s) {
  "pending" -> "بانتظار الرد"; "accepted" -> "مقبولة"; "rejected" -> "مرفوضة"; "cancelled" -> "ملغاة"; else -> s
 }

 private fun renderInvites() {
  if (!::invitesBox.isInitialized) return
  invitesBox.removeAllViews()
  val q = if (::inviteSearch.isInitialized) inviteSearch.text.toString().trim().lowercase() else ""
  val filtered = (0 until invites.length()).mapNotNull { invites.optJSONObject(it) }.filter { o ->
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   val st = o.optString("status")
   val ar = inviteStatusLabel(st)
   name.lowercase().contains(q) || id.lowercase().contains(q) || st.lowercase().contains(q) || ar.lowercase().contains(q)
  }
  if (filtered.isEmpty()) {
   invitesBox.addView(empty(if (q.isNotBlank()) "لا توجد دعوات مطابقة" else "لا توجد دعوات", I_MAIL))
   return
  }
  for (o in filtered) {
   val uid = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $uid" }
   val stRaw = o.optString("status")
   val stColor = when (stRaw) { "pending" -> amber; "accepted" -> green; "rejected" -> red; else -> muted }
   val c = rowCard()
   val r = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   r.addView(avatar(name, 44), lp(44, 44, end = 12))
   val mid = ll()
   mid.addView(t(name, 14f, ink, true))
   mid.addView(t("ID $uid", 12f, muted), lp(-1, -2, top = 3))
   r.addView(mid, lp(0, -2, weight = 1f))
   r.addView(pill(inviteStatusLabel(stRaw), stColor))
   c.addView(r)
   if (stRaw == "pending") {
    c.addView(button("إلغاء الدعوة", I_X, 2) { cancelInvitation(o.optLong("id")) }.apply { setPadding(dp(12), dp(9), dp(12), dp(9)) }, lp(-1, 42, top = 12))
   }
   invitesBox.addView(c, lp(-1, -2, bottom = 8))
  }
 }

 private fun renderMembers() {
  if (!::membersBox.isInitialized) return
  membersBox.removeAllViews()
  val query = if (::memberSearch.isInitialized) memberSearch.text.toString().trim().lowercase() else ""
  val filtered = (0 until members.length()).mapNotNull { members.optJSONObject(it) }.filter { o ->
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   name.lowercase().contains(query) || id.lowercase().contains(query)
  }
  if (filtered.isEmpty()) {
   membersBox.addView(empty(if (query.isNotBlank()) "لا توجد نتائج مطابقة" else "لا يوجد أعضاء منضمّون بعد", I_USERS))
   return
  }
  for (o in filtered) {
   val id = o.optString("public_id", "—")
   val name = o.optString("display_name").ifBlank { "ID $id" }
   val c = rowCard { showMemberDetails(o) }
   val r = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   r.addView(avatar(name, 46), lp(46, 46, end = 12))
   val mid = ll()
   mid.addView(t(name, 14f, ink, true))
   val sub = ll(0).apply { gravity = Gravity.CENTER_VERTICAL }
   sub.addView(pill("ID $id", green))
   mid.addView(sub, lp(-1, -2, top = 6))
   mid.addView(t("عضو وكالة • اضغط لعرض الملف وإدارته", 11f, muted), lp(-1, -2, top = 6))
   r.addView(mid, lp(0, -2, weight = 1f))
   r.addView(iv(I_CHEV, muted), lp(18, 18))
   c.addView(r)
   membersBox.addView(c, lp(-1, -2, bottom = 8))
  }
 }

 private fun render() {
  if (!::invitesBox.isInitialized || !::membersBox.isInitialized) return
  invitesBox.removeAllViews(); membersBox.removeAllViews()
  val pending = (0 until invites.length()).count { invites.optJSONObject(it)?.optString("status") == "pending" }
  val accepted = (0 until invites.length()).count { invites.optJSONObject(it)?.optString("status") == "accepted" }
  val rejected = (0 until invites.length()).count { invites.optJSONObject(it)?.optString("status") == "rejected" }
  val cancelled = (0 until invites.length()).count { invites.optJSONObject(it)?.optString("status") == "cancelled" }
  anAccepted.text = accepted.toString(); anRejected.text = rejected.toString()
  anCancelled.text = cancelled.toString(); anTotal.text = invites.length().toString()
  membersCountText.text = members.length().toString()
  pendingCountText.text = pending.toString()
  renderInvites()
  renderMembers()
 }

 // ══════════════════════════════════════════════════════════
 //  Dialogs
 // ══════════════════════════════════════════════════════════
 private fun sheet(content: View): Dialog {
  val d = Dialog(this)
  d.requestWindowFeature(Window.FEATURE_NO_TITLE)
  content.layoutDirection = View.LAYOUT_DIRECTION_RTL
  d.setContentView(content)
  d.window?.let { w ->
   w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
   w.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
   w.setDimAmount(0.7f)
  }
  return d
 }

 private fun confirmDialog(title: String, msg: String, yes: String, no: String, danger: Boolean, onYes: () -> Unit) {
  val c = ll().apply {
   gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(22), dp(24), dp(22), dp(20)); background = rnd(panelHi, 26, 1, stroke)
  }
  c.addView(badge(if (danger) I_ALERT else I_INFO, if (danger) red else violetLight, 26, 28), lp(56, 56, bottom = 14))
  c.addView(t(title, 18f, ink, true).apply { gravity = Gravity.CENTER })
  c.addView(t(msg, 13f, muted).apply { gravity = Gravity.CENTER }, lp(-1, -2, top = 8, bottom = 20))
  val d = sheet(c)
  val row = ll(0)
  row.addView(button(no, -1, 1) { d.dismiss() }, lp(0, 48, weight = 1f, end = 5))
  row.addView(button(yes, -1, if (danger) 3 else 0) { d.dismiss(); onYes() }, lp(0, 48, weight = 1f, start = 5))
  c.addView(row, lp(-1, -2))
  d.show()
 }

 private fun infoRow(label: String, value: String, ic: Int): LinearLayout {
  val c = ll(0).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = card() }
  c.addView(badge(ic, violetLight, 16, 10), lp(32, 32, end = 10))
  val col = ll()
  col.addView(t(label, 11f, muted, true))
  col.addView(t(value, 14f, ink, true), lp(-1, -2, top = 3))
  c.addView(col, lp(0, -2, weight = 1f))
  return c
 }

 private fun showMemberDetails(member: JSONObject) {
  val name = member.optString("display_name").ifBlank { "—" }
  val id = member.optString("public_id").ifBlank { "—" }
  val joinedRaw = member.optString("joined_at").ifBlank { "—" }
  val joined = if (joinedRaw.length > 16) joinedRaw.replace("T", " ").take(16) else joinedRaw
  val avatarUrl = member.optString("avatar_url").ifBlank { member.optString("avatar") }

  val content = ll().apply {
   gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(20), dp(24), dp(20), dp(18)); background = rnd(panelHi, 26, 1, stroke)
  }
  val sv = ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(content) }
  val d = sheet(sv)

  val ring = FrameLayout(this).apply {
   background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(violet, pink)).apply { shape = GradientDrawable.OVAL }
   setPadding(dp(3), dp(3), dp(3), dp(3))
  }
  val inner = FrameLayout(this).apply { clipToOutline = true; outlineProvider = ovalOutline }
  inner.addView(avatar(name, 88), FrameLayout.LayoutParams(-1, -1))
  val photo = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
  inner.addView(photo, FrameLayout.LayoutParams(-1, -1))
  ring.addView(inner, FrameLayout.LayoutParams(-1, -1))
  content.addView(ring, lp(94, 94, bottom = 14))

  if (avatarUrl.isNotBlank()) {
   imageWorker.execute {
    try {
     val url = java.net.URL(avatarUrl)
     val conn = url.openConnection() as java.net.HttpURLConnection
     conn.connectTimeout = 8000; conn.readTimeout = 8000
     conn.setRequestProperty("User-Agent", "BAK LIVE Android")
     val bitmap = BitmapFactory.decodeStream(conn.inputStream)
     conn.disconnect()
     if (bitmap != null) runOnUiThread { if (!isFinishing) photo.setImageBitmap(bitmap) }
    } catch (_: Exception) {}
   }
  }

  content.addView(t(name, 19f, ink, true).apply { gravity = Gravity.CENTER })
  content.addView(pill("عضو وكالة", green), lp(-2, -2, top = 8, bottom = 16))
  content.addView(infoRow("معرّف المستخدم (ID)", id, I_USERS), lp(-1, -2, bottom = 8))
  content.addView(infoRow("اسم الحساب", name, I_HOME), lp(-1, -2, bottom = 8))
  content.addView(infoRow("تاريخ الانضمام", joined, I_CLOCK), lp(-1, -2, bottom = 18))

  val row = ll(0)
  row.addView(button("إغلاق", -1, 1) { d.dismiss() }, lp(0, 48, weight = 1f, end = 5))
  row.addView(button("إزالة من الوكالة", I_TRASH, 2) { d.dismiss(); confirmRemoveMember(id) }, lp(0, 48, weight = 1.4f, start = 5))
  content.addView(row, lp(-1, -2))
  d.show()
 }

 private fun confirmRemoveMember(publicId: String) {
  if (publicId.isBlank() || publicId == "—") return
  confirmDialog("إزالة عضو الوكالة", "هل تريد إزالة هذا المذيع من وكالتك؟ لن يتم حذف حسابه.", "إزالة", "إلغاء", true) {
   val tok = token() ?: run { status.text = "يرجى تسجيل الدخول"; return@confirmDialog }
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
  confirmDialog("إلغاء الدعوة", "هل تريد إلغاء هذه الدعوة؟", "إلغاء الدعوة", "رجوع", true) {
   val tok = token() ?: run { status.text = "يرجى تسجيل الدخول"; return@confirmDialog }
   worker.execute {
    try {
     val r = AuthApi.post("/api/agency/invitations/$invitationId/cancel", JSONObject(), tok)
     if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر إلغاء الدعوة"))
     runOnUiThread {
      status.text = "تم إلغاء الدعوة"
      loadData()
     }
    } catch (e: Exception) {
     runOnUiThread { status.text = e.message ?: "تعذر إلغاء الدعوة" }
    }
   }
  }
 }
}
