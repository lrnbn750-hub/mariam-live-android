package com.mariam.live

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Fully native Kotlin profile screen. The previous HTML/WebView and floating-avatar
 * overlay are intentionally removed; all profile layout, scrolling and avatar UI
 * are now part of the Android view hierarchy.
 */
class ProfileActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val photoPickerCode = 7421
    private lateinit var page: LinearLayout
    private lateinit var header: ProfileHeaderView
    private lateinit var avatar: ImageView
    private lateinit var coinValue: TextView
    private lateinit var diamondValue: TextView
    private lateinit var visitorValue: TextView
    private lateinit var friendValue: TextView
    private var profileData = JSONObject()
    private var publicId = ""
    private var userId = ""

    private val ink = Color.rgb(20, 24, 34)
    private val muted = Color.rgb(143, 151, 164)
    private val border = Color.rgb(238, 240, 244)
    private val white = Color.WHITE
    private val pageBg = Color.rgb(255, 255, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        window.statusBarColor = white
        window.navigationBarColor = white
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        buildNativePage()
        loadProfileData()
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()
    private fun rounded(fill: Int, radius: Float, stroke: Int = border): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1f), stroke)
        }

    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = true
        }

    private fun buildNativePage() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(pageBg)
            isFillViewport = true
            clipToPadding = false
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
            setPadding(dp(16f), dp(8f), dp(16f), dp(28f))
        }
        scroll.addView(page, ViewGroup.LayoutParams(-1, -2))
        setContentView(scroll)

        header = ProfileHeaderView(this)
        header.layoutParams = LinearLayout.LayoutParams(-1, -2)
        header.setBackgroundColor(pageBg)
        val slot = header.findViewById<FrameLayout>(R.id.profileHeaderAvatarSlot)
        slot.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(222, 190, 103), Color.rgb(38, 165, 119), Color.rgb(222, 190, 103))
        ).apply { shape = GradientDrawable.OVAL }
        avatar = header.avatarView
        avatar.layoutParams = FrameLayout.LayoutParams(dp(84f), dp(84f), Gravity.CENTER)
        avatar.background = rounded(Color.rgb(235, 239, 240), 50f, Color.TRANSPARENT)
        avatar.clipToOutline = true
        header.editAvatarView.setOnClickListener { pickPhoto() }
        header.copyView.setOnClickListener { copyId() }
        header.settingsView.setOnClickListener { openAction("settings") }
        header.supportView.setOnClickListener { openAction("music") }
        header.fansCell.setOnClickListener { openFollowList("followers") }
        header.followingCell.setOnClickListener { openFollowList("following") }
        page.addView(header)

        val walletRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(14f), 0, dp(14f))
        }
        val coins = walletCard("20", "عملات الشحن", "Ⓑ", Color.rgb(255, 239, 193))
        val diamonds = walletCard("0", "الألماس المكتسب", "◆", Color.rgb(216, 244, 255))
        coinValue = coins.second
        diamondValue = diamonds.second
        walletRow.addView(diamonds.first, LinearLayout.LayoutParams(0, dp(126f), 1f).apply { marginEnd = dp(8f) })
        walletRow.addView(coins.first, LinearLayout.LayoutParams(0, dp(126f), 1f).apply { marginStart = dp(8f) })
        page.addView(walletRow)

        val shortcutCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 28f)
            setPadding(dp(12f), dp(14f), dp(12f), dp(12f))
        }
        val rowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        rowOne.addView(shortcut("العائلة", R.drawable.ic_profile_family_color) { openAction("family") }, weighted())
        rowOne.addView(shortcut("VIP", R.drawable.ic_profile_vip_color) { openAction("vip") }, weighted())
        rowOne.addView(shortcut("المتجر", R.drawable.ic_profile_store_color) { openAction("store") }, weighted())
        rowOne.addView(shortcut("مظهري", R.drawable.ic_profile_appearance_color) { openAction("look") }, weighted())
        val rowTwo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(8f), 0, 0)
        }
        rowTwo.addView(shortcut("مركز الوكالة", R.drawable.ic_profile_agency_color) { openAction("agency") }, weighted())
        rowTwo.addView(shortcut("مركز المضيف", R.drawable.ic_profile_creator_color) { openAction("host") }, weighted())
        rowTwo.addView(View(this), weighted())
        rowTwo.addView(View(this), weighted())
        shortcutCard.addView(rowOne)
        shortcutCard.addView(rowTwo)
        page.addView(shortcutCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f) })

        val settingsTitle = text("إعدادات وخدمات الحساب", 18f, ink, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(0, dp(28f), dp(4f), dp(12f))
        }
        page.addView(settingsTitle)
        val services = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 26f)
            clipToOutline = true
        }
        serviceRow(services, "أنشر", R.drawable.ic_profile_publish) { openAction("post") }
        serviceRow(services, "المساعدة والدعم", R.drawable.ic_profile_help_color) { openAction("help") }
        serviceRow(services, "الأمان والخصوصية", R.drawable.ic_profile_security_color) { openAction("privacy") }
        serviceRow(services, "المستوى", R.drawable.ic_profile_level_color) { openAction("level") }
        page.addView(services, LinearLayout.LayoutParams(-1, -2))
    }

    private fun weighted() = LinearLayout.LayoutParams(0, -2, 1f)

    private fun walletCard(amount: String, label: String, symbol: String, tint: Int): Pair<View, TextView> {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 24f)
            setPadding(dp(12f), dp(12f), dp(12f), dp(10f))
            elevation = dp(2f).toFloat()
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val icon = TextView(this).apply {
            text = symbol
            textSize = 25f
            setTextColor(if (tint == Color.rgb(255, 239, 193)) Color.rgb(178, 128, 24) else Color.rgb(48, 164, 210))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(tint); cornerRadius = dp(18f).toFloat() }
        }
        top.addView(icon, LinearLayout.LayoutParams(dp(64f), dp(64f)))
        val value = text(amount, 25f, ink, true).apply { gravity = Gravity.CENTER }
        top.addView(value, LinearLayout.LayoutParams(0, dp(64f), 1f))
        box.addView(top)
        box.addView(text(label, 14f, muted).apply {
            gravity = Gravity.RIGHT
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(0, dp(8f), dp(2f), 0)
        })
        box.setOnClickListener { openAction(if (label.contains("ألماس")) "diamonds" else "coins") }
        return box to value
    }

    private fun shortcut(label: String, iconRes: Int, action: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3f), dp(3f), dp(3f), dp(4f))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = rounded(
                when (label) {
                    "العائلة" -> Color.rgb(242, 237, 255)
                    "VIP" -> Color.rgb(27, 31, 48)
                    "المتجر" -> Color.rgb(255, 238, 244)
                    "مظهري" -> Color.rgb(232, 244, 255)
                    "مركز الوكالة" -> Color.rgb(228, 248, 243)
                    else -> Color.rgb(246, 235, 255)
                }, 20f, Color.TRANSPARENT
            )
            setPadding(dp(15f), dp(15f), dp(15f), dp(15f))
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(76f), dp(76f)))
        item.addView(text(label, 14f, if (label == "VIP") Color.rgb(151, 116, 37) else ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7f), 0, 0)
        })
        return item
    }

    private fun serviceRow(parent: LinearLayout, label: String, iconRes: Int, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = rounded(Color.rgb(246, 248, 251), 16f, Color.TRANSPARENT)
            setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        row.addView(text(label, 17f, ink, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
        }, LinearLayout.LayoutParams(0, dp(48f), 1f).apply { marginStart = dp(14f) })
        row.addView(text("‹", 28f, Color.rgb(177, 185, 195)).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(26f), dp(48f)))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        val divider = View(this).apply { setBackgroundColor(border) }
        parent.addView(divider, LinearLayout.LayoutParams(-1, dp(1f)))
    }

    private fun loadProfileData() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrBlank()) { finish(); return }
        executor.execute {
            try {
                val me = AuthApi.get("/api/auth/me", token)
                if (me.status !in 200..299) throw Exception(me.body.optString("error", "تعذر تحميل الحساب"))
                val account = me.body.optJSONObject("user") ?: throw Exception("بيانات الحساب غير مكتملة")
                userId = account.optString("id")
                publicId = account.optString("public_id")
                if (userId.isBlank() || userId == "null") throw Exception("معرّف المستخدم غير متوفر")
                val details = AuthApi.get("/api/users/$userId", token)
                if (details.status !in 200..299) throw Exception(details.body.optString("error", "تعذر تحميل الملف الشخصي"))
                val user = details.body.optJSONObject("user") ?: JSONObject()
                val data = JSONObject()
                    .put("name", user.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("username", "مستخدم BAK LIVE"))
                    .put("id", publicId)
                    .put("level", 1)
                    .put("avatarUrl", user.optString("avatar_url"))
                    .put("visitors", JSONObject.NULL)
                    .put("friends", 0)
                    .put("followers", 0)
                    .put("fans", 0)
                    .put("diamonds", 0)
                    .put("coins", 0)
                try {
                    val level = AuthApi.get("/api/levels/me", token)
                    if (level.status in 200..299) data.put("level", level.body.optInt("level", 1).coerceIn(1, 100))
                } catch (_: Exception) {}
                try {
                    val follows = AuthApi.get("/api/follows/me", token)
                    if (follows.status in 200..299) {
                        data.put("followers", follows.body.optInt("following_count", follows.body.optJSONArray("following")?.length() ?: 0))
                        data.put("fans", follows.body.optInt("followers_count", follows.body.optJSONArray("followers")?.length() ?: 0))
                    }
                } catch (_: Exception) {}
                try {
                    val wallet = AuthApi.get("/api/wallet", token)
                    if (wallet.status in 200..299) {
                        val w = wallet.body.optJSONObject("wallet") ?: wallet.body
                        data.put("coins", w.optLong("recharge_coins", 0L))
                        data.put("diamonds", w.optLong("earned_diamonds", 0L))
                    }
                } catch (_: Exception) {}
                profileData = data
                runOnUiThread { bindProfile(data, token) }
            } catch (e: Exception) {
                Log.e("BAKProfileNative", "Profile load failed", e)
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر تحميل بيانات الملف الشخصي", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun bindProfile(data: JSONObject, token: String) {
        header.setUserName(data.optString("name"))
        header.setUserIdText(publicId)
        header.setLevel(data.optInt("level", 1))
        header.setFansCount(data.optInt("fans", 0).toString())
        header.setFollowingCount(data.optInt("followers", 0).toString())
        coinValue.text = data.optLong("coins", 0).toString()
        diamondValue.text = data.optLong("diamonds", 0).toString()
        val avatarUrl = normalizeAvatar(data.optString("avatarUrl"))
        if (avatarUrl.isNotBlank()) loadAvatar(avatarUrl, token)
    }

    private fun normalizeAvatar(value: String): String {
        if (value.isBlank() || value == "null") return ""
        return if (value.startsWith("http://") || value.startsWith("https://")) value
        else "https://nokiraq.duckdns.org/" + value.trimStart('/')
    }

    private fun loadAvatar(url: String, token: String) {
        executor.execute {
            try {
                val connection = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 15000; readTimeout = 20000; instanceFollowRedirects = true
                    setRequestProperty("Accept", "image/*")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("User-Agent", "BAK-LIVE-Android")
                }
                val bitmap = try {
                    val status = connection.responseCode
                    if (status !in 200..299) throw Exception("Avatar HTTP $status")
                    val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty()
                    if (!mime.startsWith("image/")) throw Exception("Avatar response is not image: $mime")
                    val bytes = connection.inputStream.use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw Exception("Unsupported avatar image")
                } finally { connection.disconnect() }
                runOnUiThread { if (!isFinishing) avatar.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                Log.e("BAKProfileNative", "Avatar load failed: $url", e)
            }
        }
    }

    private fun copyId() {
        if (publicId.isBlank()) return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BAK ID", publicId))
        Toast.makeText(this, "تم نسخ المعرّف", Toast.LENGTH_SHORT).show()
    }

    private fun openAction(action: String) {
        when (action) {
            "settings", "privacy" -> startActivity(Intent(this, AccountSecurityActivity::class.java))
            "music" -> Toast.makeText(this, "الموسيقى", Toast.LENGTH_SHORT).show()
            "level" -> startActivity(Intent(this, LevelActivity::class.java))
            "diamonds", "coins" -> startActivity(Intent(this, WalletActivity::class.java))
            "look", "store" -> startActivity(Intent(this, InventoryActivity::class.java))
            "vip" -> startActivity(Intent(this, VipActivity::class.java))
            "host" -> startActivity(Intent(this, CreatorCenterActivity::class.java))
            "agency" -> showInfo("مركز الوكالة")
            "family" -> showInfo("العائلة")
            "help" -> showInfo("المساعدة والدعم")
            "post" -> showInfo("أنشر")
        }
    }

    private fun showInfo(title: String) {
        android.app.AlertDialog.Builder(this).setTitle(title)
            .setMessage("هذه الخدمة ستتوفر ضمن تطبيق BAK LIVE.")
            .setPositiveButton("حسناً", null).show()
    }

    private fun openFollowList(type: String) {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        val title = if (type == "followers") "المعجبون" else "أتابعهم"
        val dialog = android.app.AlertDialog.Builder(this).setTitle(title)
            .setMessage("جارٍ تحميل القائمة...").setNegativeButton("إغلاق", null).create()
        dialog.show()
        executor.execute {
            try {
                val result = AuthApi.get("/api/follows/me", token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر تحميل القائمة"))
                val list = result.body.optJSONArray(type) ?: org.json.JSONArray()
                val names = ArrayList<String>()
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    names.add(item.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: item.optString("username", "مستخدم"))
                }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    dialog.dismiss()
                    android.app.AlertDialog.Builder(this).setTitle("$title (${names.size})")
                        .setMessage(if (names.isEmpty()) "لا توجد حسابات في هذه القائمة بعد" else names.joinToString("\n"))
                        .setPositiveButton("حسناً", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread { dialog.dismiss(); Toast.makeText(this, e.message ?: "تعذر تحميل القائمة", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, photoPickerCode)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == photoPickerCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadPhoto(uri)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        Toast.makeText(this, "جارٍ رفع الصورة...", Toast.LENGTH_SHORT).show()
        executor.execute {
            try {
                val source = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("تعذر قراءة الصورة")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }) ?: throw Exception("صيغة الصورة غير مدعومة")
                val scaled = if (bitmap.width > 900 || bitmap.height > 900) {
                    val ratio = minOf(900f / bitmap.width, 900f / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
                        (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
                } else bitmap
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
                val bytes = output.toByteArray()
                if (bytes.size > 1536 * 1024) throw Exception("الصورة كبيرة، اختر صورة أصغر")
                val result = AuthApi.post("/api/profile/avatar",
                    JSONObject().put("avatar_base64", Base64.encodeToString(bytes, Base64.NO_WRAP)), token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر رفع الصورة"))
                val url = normalizeAvatar(result.body.optString("avatar_url"))
                profileData.put("avatarUrl", url)
                runOnUiThread {
                    loadAvatar(url, token)
                    Toast.makeText(this, "تم تحديث الصورة الشخصية", Toast.LENGTH_SHORT).show()
                }
                if (scaled !== bitmap) scaled.recycle()
                if (bitmap !== scaled) bitmap.recycle()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر رفع الصورة", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
