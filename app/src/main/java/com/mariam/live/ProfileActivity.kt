package com.mariam.live

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

class ProfileActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val photoPickerCode = 7421
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var nativeAvatar: ImageView
    private var selectedPhoto: Uri? = null
    private var profileData = JSONObject()
    private var publicId = ""
    private var firstLoadStarted = false
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        web = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(ProfileBridge(), "BAK")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    positionNativeAvatar()
                    loadNativeAvatarFromProfile()
                }
            }
        }
        root = FrameLayout(this)
        nativeAvatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.INVISIBLE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.TRANSPARENT)
            }
            clipToOutline = true
            contentDescription = "الصورة الشخصية"
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        root.addView(nativeAvatar, FrameLayout.LayoutParams(1, 1))
        // Keep the native avatar aligned with its HTML frame whenever the WebView scrolls.
        web.setOnScrollChangeListener { _, _, _, _, _ -> positionNativeAvatar() }
        web.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionNativeAvatar() }
        setContentView(root)
        web.loadUrl("file:///android_asset/profile/BAK_LIVE_Profile.html")
        loadProfileData()
    }

    inner class ProfileBridge {
        @JavascriptInterface fun onAction(action: String) {
            runOnUiThread {
                when (action) {
                    "settings", "privacy" -> startActivity(Intent(this@ProfileActivity, AccountSecurityActivity::class.java))
                    "music" -> Toast.makeText(this@ProfileActivity, "الموسيقى", Toast.LENGTH_SHORT).show()
                    "level" -> startActivity(Intent(this@ProfileActivity, LevelActivity::class.java))
                    "diamonds", "coins" -> startActivity(Intent(this@ProfileActivity, WalletActivity::class.java))
                    "look", "store" -> startActivity(Intent(this@ProfileActivity, InventoryActivity::class.java))
                    "vip" -> startActivity(Intent(this@ProfileActivity, VipActivity::class.java))
                    "host" -> startActivity(Intent(this@ProfileActivity, CreatorCenterActivity::class.java))
                    "agency" -> showInfo("مركز الوكالة")
                    "family" -> showInfo("العائلة")
                    "help" -> showInfo("المساعدة والدعم")
                    "post" -> showInfo("أنشر")
                    "visitors", "friends" -> showInfo(if (action == "visitors") "الزوار" else "الأصدقاء")
                    "followers" -> openFollowList("followers")
                    "fans" -> openFollowList("followers")
                    "following" -> openFollowList("following")
                    "editAvatar" -> pickPhoto()
                }
            }
        }

        @JavascriptInterface fun copyId() {
            runOnUiThread {
                if (publicId.isNotBlank()) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("BAK ID", publicId))
                    Toast.makeText(this@ProfileActivity, "تم نسخ المعرّف", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendProfileToPage() {
        if (!::web.isInitialized || isFinishing) return
        val json = JSONObject.quote(profileData.toString())
        web.post {
            web.evaluateJavascript("window.setProfileData && window.setProfileData(JSON.parse($json));", null)
            positionNativeAvatar()
            loadNativeAvatarFromProfile()
        }
    }

    private fun loadProfileData() {
        firstLoadStarted = true
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

                val detailsResponse = AuthApi.get("/api/users/$userId", token)
                if (detailsResponse.status !in 200..299) throw Exception(detailsResponse.body.optString("error", "تعذر تحميل الملف الشخصي"))
                val user = detailsResponse.body.optJSONObject("user") ?: JSONObject()

                val data = JSONObject()
                    .put("name", user.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("username", "مستخدم BAK LIVE"))
                    .put("id", publicId)
                    .put("level", 1)
                    .put("avatarUrl", "")
                    .put("visitors", JSONObject.NULL)
                    .put("friends", 0)
                    .put("followers", 0)
                    .put("fans", 0)
                    .put("diamonds", 0)
                    .put("coins", 0)

                // Avatar may be stored on the user row or in a nested profile object.
                val userProfile = user.optJSONObject("profile") ?: user.optJSONObject("user_profile")
                val accountProfile = account.optJSONObject("profile") ?: account.optJSONObject("user_profile")
                val avatarPath = sequenceOf(
                    user.optString("avatar_url"),
                    userProfile?.optString("avatar_url").orEmpty(),
                    account.optString("avatar_url"),
                    accountProfile?.optString("avatar_url").orEmpty()
                ).firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
                if (avatarPath.isNotBlank()) {
                    try {
                        data.put("avatarUrl", normalizeAvatar(avatarPath))
                    } catch (avatarError: Exception) {
                        Log.e("BAKProfileWeb", "Avatar download failed: ${normalizeAvatar(avatarPath)}", avatarError)
                        data.put("avatarUrl", normalizeAvatar(avatarPath))
                    }
                } else {
                    Log.w("BAKProfileWeb", "No avatar_url found in /api/auth/me or /api/users response")
                }

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
                    val walletResponse = AuthApi.get("/api/wallet", token)
                    if (walletResponse.status in 200..299) {
                        val wallet = walletResponse.body.optJSONObject("wallet") ?: walletResponse.body
                        data.put("coins", wallet.optLong("recharge_coins", 0L))
                        data.put("diamonds", wallet.optLong("earned_diamonds", 0L))
                    }
                } catch (_: Exception) {}

                profileData = data
                runOnUiThread { sendProfileToPage() }
            } catch (e: Exception) {
                Log.e("BAKProfileWeb", "Profile load failed", e)
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر تحميل بيانات الملف الشخصي", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun normalizeAvatar(value: String): String {
        if (value.isBlank() || value == "null") return ""
        return if (value.startsWith("http://") || value.startsWith("https://")) value
        else "https://nokiraq.duckdns.org/" + value.trimStart('/')
    }

    private fun positionNativeAvatar() {
        if (!::web.isInitialized || !::nativeAvatar.isInitialized || web.width == 0) return
        web.evaluateJavascript(
            "(function(){var e=document.querySelector('.av .ring i');if(!e)return '';var r=e.getBoundingClientRect();return JSON.stringify({x:r.left,y:r.top,w:r.width,h:r.height});})()"
        ) { value ->
            try {
                val raw = org.json.JSONTokener(value).nextValue() as? String ?: return@evaluateJavascript
                val rect = JSONObject(raw)
                val density = resources.displayMetrics.density
                val x = (rect.optDouble("x") * density).toInt()
                val y = (rect.optDouble("y") * density).toInt()
                val w = (rect.optDouble("w") * density).toInt().coerceAtLeast(1)
                val h = (rect.optDouble("h") * density).toInt().coerceAtLeast(1)
                val params = nativeAvatar.layoutParams as FrameLayout.LayoutParams
                params.width = w
                params.height = h
                params.leftMargin = x
                params.topMargin = y
                nativeAvatar.layoutParams = params
                nativeAvatar.bringToFront()
            } catch (e: Exception) {
                Log.e("BAKProfileNativeAvatar", "Could not position native avatar", e)
            }
        }
    }

    private fun loadNativeAvatarFromProfile() {
        val avatarUrl = profileData.optString("avatarUrl")
        if (avatarUrl.isBlank() || avatarUrl == "null") {
            nativeAvatar.setImageDrawable(null)
            nativeAvatar.visibility = View.INVISIBLE
            return
        }
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()
        executor.execute {
            try {
                val connection = (URL(avatarUrl).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "image/*")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("User-Agent", "BAK-LIVE-Android")
                }
                val bitmap = try {
                    val status = connection.responseCode
                    if (status !in 200..299) throw Exception("Avatar HTTP $status")
                    val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty()
                    if (!mime.startsWith("image/")) throw Exception("Avatar response is not image: $mime")
                    val bytes = connection.inputStream.use { input ->
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > 5 * 1024 * 1024) throw Exception("Avatar exceeds 5 MB")
                            out.write(buffer, 0, n)
                        }
                        out.toByteArray()
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw Exception("Unsupported avatar image")
                } finally {
                    connection.disconnect()
                }
                runOnUiThread {
                    if (!isFinishing && ::nativeAvatar.isInitialized) {
                        nativeAvatar.setImageBitmap(bitmap)
                        nativeAvatar.visibility = View.VISIBLE
                        positionNativeAvatar()
                    }
                }
            } catch (e: Exception) {
                Log.e("BAKProfileNativeAvatar", "Native avatar load failed: $avatarUrl", e)
                runOnUiThread { nativeAvatar.visibility = View.INVISIBLE }
            }
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
            selectedPhoto = uri
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
                val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: throw Exception("صيغة الصورة غير مدعومة")
                val scaled = if (bitmap.width > 900 || bitmap.height > 900) {
                    val ratio = minOf(900f / bitmap.width, 900f / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
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
                    sendProfileToPage()
                    Toast.makeText(this, "تم تحديث الصورة الشخصية", Toast.LENGTH_SHORT).show()
                }
                if (scaled !== bitmap) scaled.recycle()
                if (bitmap !== scaled) bitmap.recycle()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر رفع الصورة", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized && firstLoadStarted) loadProfileData()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.removeJavascriptInterface("BAK")
            web.destroy()
        }
        executor.shutdownNow()
        super.onDestroy()
    }
}
