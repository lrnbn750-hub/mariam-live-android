package com.mariam.live

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class SvipActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var webView: WebView
    private var token = ""
    @Volatile private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        window.statusBarColor = Color.rgb(22, 27, 39)
        window.navigationBarColor = Color.rgb(15, 19, 28)
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(22, 27, 39))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    return !url.startsWith("file:///android_asset/vip/") && url != "about:blank"
                }
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    val value = url.orEmpty()
                    return !value.startsWith("file:///android_asset/vip/") && value != "about:blank"
                }
            }
            addJavascriptInterface(SvipBridge(), "AndroidVip")
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/vip/index.html")
    }

    private inner class SvipBridge {
        @JavascriptInterface
        fun onPageReady() {
            if (loading) return
            loading = true
            // Paint the last successful SVIP response immediately while the
            // network refresh runs in the background (stale-while-revalidate).
            applyCachedData()
            loadData()
        }

        @JavascriptInterface
        fun close() {
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun openMemberCenter() {
            runOnUiThread {
                startActivity(Intent(this@SvipActivity, SvipMemberActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openWhatsApp(message: String) {
            runOnUiThread {
                try {
                    val url = "https://wa.me/9647715588869?text=" +
                        java.net.URLEncoder.encode(message, "UTF-8")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                    Toast.makeText(this@SvipActivity, "تعذر فتح واتساب. تأكد من تثبيته أو جرّب واتساب ويب.", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun purchase(level: Int) {
            if (level !in 1..7) {
                notifyPurchase(false, "مستوى SVIP غير صالح")
                return
            }
            io.execute {
                try {
                    val result = AuthApi.post("/api/svip/purchase", JSONObject().put("level", level), token)
                    if (result.status !in 200..299 || !result.body.optBoolean("success", false)) {
                        val message = result.body.optString("error",
                            result.body.optString("message", "تعذر إتمام شراء SVIP"))
                        notifyPurchase(false, message)
                        return@execute
                    }
                    val message = result.body.optString("message", "تم تفعيل عضوية SVIP " + level + " بنجاح")
                    loadData(message)
                } catch (error: Exception) {
                    notifyPurchase(false, error.message ?: "تعذر الاتصال بالخادم")
                }
            }
        }
    }

    private fun applyCachedData() {
        try {
            val cached = getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE)
                .getString("page_data", null) ?: return
            val data = JSONObject(cached)
            if (data.optJSONArray("packages") == null) return
            if (!::webView.isInitialized || isFinishing || isDestroyed) return
            webView.evaluateJavascript(
                "window.applyNativeData(" + JSONObject.quote(data.toString()) + ");",
                null
            )
        } catch (_: Exception) {
            // A bad or outdated cache must never prevent the live refresh.
        }
    }

    private fun loadData(successMessage: String? = null) {
        io.execute {
            try {
                // Fetch independent account data concurrently so the SVIP page
                // does not wait for three network round-trips one after another.
                val levelsFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                    AuthApi.get("/api/svip/levels", token)
                }
                val meFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                    AuthApi.get("/api/svip/me", token)
                }
                val walletFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                    AuthApi.get("/api/wallet", token)
                }
                val levelsResult = levelsFuture.get()
                val meResult = meFuture.get()
                val walletResult = walletFuture.get()

                if (levelsResult.status !in 200..299 || !levelsResult.body.optBoolean("success", false)) {
                    throw IllegalStateException(levelsResult.body.optString("error", "تعذر تحميل مستويات SVIP"))
                }
                if (meResult.status !in 200..299 || !meResult.body.optBoolean("success", false)) {
                    throw IllegalStateException(meResult.body.optString("error", "تعذر تحميل عضويتك الحالية"))
                }
                if (walletResult.status !in 200..299) {
                    throw IllegalStateException(walletResult.body.optString("error", "تعذر تحميل رصيد المحفظة"))
                }

                // Accept both current and legacy SVIP API field names. Some deployed
                // backend revisions used vip_level/price_diamonds or returned packages.
                val rawLevels = levelsResult.body.optJSONArray("levels")
                    ?: levelsResult.body.optJSONArray("packages")
                    ?: JSONArray()
                val packages = JSONArray()
                for (i in 0 until rawLevels.length()) {
                    val level = rawLevels.optJSONObject(i) ?: continue
                    val number = level.optInt("level", level.optInt("vip_level", 0))
                    val price = level.optLong(
                        "price_coins",
                        level.optLong("price_diamonds", level.optLong("price", 0))
                    )
                    val active = level.optBoolean("active", true) &&
                        level.optBoolean("is_active", true) && price > 0
                    if (number in 1..7) {
                        packages.put(JSONObject()
                            .put("level", number)
                            .put("name", level.optString("name", "SVIP " + number))
                            .put("price", price)
                            .put("active", active)
                            .put("badge_asset_url", level.optString("badge_asset_url", ""))
                            .put("frame_asset_url", level.optString("frame_asset_url", ""))
                            .put("chat_bubble_asset_url", level.optString("chat_bubble_asset_url", ""))
                            .put("entry_effect_asset_url", level.optString("entry_effect_asset_url", "")))
                    }
                }
                val rawSubscription = meResult.body.optJSONObject("subscription") ?: JSONObject()
                val membership = JSONObject()
                    .put("vip_level", rawSubscription.optInt("level", 0))
                    .put("starts_at", rawSubscription.optString("starts_at", ""))
                    .put("expires_at", rawSubscription.optString("expires_at", ""))
                val wallet = walletResult.body.optJSONObject("wallet") ?: walletResult.body
                val balance = wallet.optLong("recharge_coins", 0)
                val data = JSONObject()
                    .put("packages", packages)
                    .put("membership", membership)
                    .put("balance", balance)

                // Keep a compact local snapshot so the next visit opens with
                // real account data before the API requests finish.
                getSharedPreferences("SVIP_FAST_CACHE", MODE_PRIVATE)
                    .edit()
                    .putString("page_data", data.toString())
                    .putLong("saved_at", System.currentTimeMillis())
                    .apply()

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    loading = false
                    webView.evaluateJavascript("window.applyNativeData(" + JSONObject.quote(data.toString()) + ");", null)
                    if (!successMessage.isNullOrBlank()) {
                        webView.evaluateJavascript(
                            "window.onNativePurchaseResult(true, " + JSONObject.quote(successMessage) + ");",
                            null
                        )
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    loading = false
                    if (!isFinishing && !isDestroyed) {
                        webView.evaluateJavascript(
                            "window.onNativeLoadError(" + JSONObject.quote(error.message ?: "تعذر تحميل بيانات SVIP") + ");",
                            null
                        )
                    }
                }
            }
        }
    }

    private fun notifyPurchase(success: Boolean, message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed && ::webView.isInitialized) {
                webView.evaluateJavascript(
                    "window.onNativePurchaseResult(" + success + ", " + JSONObject.quote(message) + ");",
                    null
                )
            }
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AndroidVip")
            webView.stopLoading()
            webView.destroy()
        }
        io.shutdownNow()
        super.onDestroy()
    }
}
