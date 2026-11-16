package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class VipActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var webView: WebView
    private var token = ""
    @Volatile private var pageReady = false
    @Volatile private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", "").orEmpty()
        window.statusBarColor = Color.rgb(22, 27, 39)
        window.navigationBarColor = Color.rgb(15, 19, 28)
        window.decorView.systemUiVisibility = 0

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(15, 19, 28))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    pageReady = true
                    loadVipData()
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(VipBridge(), "AndroidVip")
            loadUrl("file:///android_asset/vip/index.html")
        }
        setContentView(FrameLayout(this).apply {
            addView(webView, FrameLayout.LayoutParams(-1, -1))
        })
    }

    private fun loadVipData() {
        if (loading) return
        if (token.isBlank()) {
            showResult(false, "انتهت الجلسة، سجّل الدخول من جديد")
            return
        }
        loading = true
        worker.execute {
            try {
                val packagesResponse = AuthApi.get("/api/vip/packages", token)
                if (packagesResponse.status !in 200..299) {
                    throw Exception(packagesResponse.body.optString("error", "تعذر تحميل باقات VIP"))
                }
                val walletResponse = AuthApi.get("/api/wallet", token)
                if (walletResponse.status !in 200..299) {
                    throw Exception(walletResponse.body.optString("error", "تعذر تحميل المحفظة"))
                }
                val membershipResponse = AuthApi.get("/api/vip/me", token)
                val wallet = walletResponse.body.optJSONObject("wallet") ?: walletResponse.body
                val membership = membershipResponse.body.optJSONObject("membership")
                    ?: membershipResponse.body.optJSONObject("vip") ?: JSONObject()
                val data = JSONObject()
                    .put("packages", packagesResponse.body.optJSONArray("packages") ?: JSONArray())
                    .put("balance", wallet.optLong("recharge_coins", 0L))
                    .put("membership", membership)
                runOnUiThread {
                    loading = false
                    if (pageReady && !isFinishing) {
                        webView.evaluateJavascript(
                            "window.applyNativeData(${JSONObject.quote(data.toString())})",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false
                    showResult(false, e.message ?: "تعذر تحميل صفحة VIP")
                }
            }
        }
    }

    private fun showResult(ok: Boolean, message: String) {
        if (!::webView.isInitialized || isFinishing) return
        webView.evaluateJavascript(
            "window.onNativePurchaseResult($ok,${JSONObject.quote(message)})",
            null
        )
    }

    private fun purchase(level: Int) {
        if (token.isBlank()) {
            showResult(false, "سجّل الدخول أولاً")
            return
        }
        worker.execute {
            try {
                val response = AuthApi.post(
                    "/api/vip/purchase",
                    JSONObject().put("vip_level", level),
                    token
                )
                val success = response.status in 200..299 &&
                    response.body.optBoolean("success", true)
                val message = if (success) {
                    "تم تفعيل VIP $level لمدة 30 يوماً"
                } else {
                    response.body.optString("error", "فشل شراء العضوية")
                }
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        showResult(true, message)
                        loadVipData()
                    } else {
                        showResult(false, message)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showResult(false, e.message ?: "تعذر الاتصال بالخادم") }
            }
        }
    }

    inner class VipBridge {
        @JavascriptInterface
        fun onPageReady() {
            runOnUiThread { loadVipData() }
        }

        @JavascriptInterface
        fun close() {
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun refresh() {
            runOnUiThread { loadVipData() }
        }

        @JavascriptInterface
        fun purchase(level: Int) {
            if (level !in 1..12) {
                runOnUiThread { showResult(false, "مستوى VIP غير صالح") }
                return
            }
            runOnUiThread { purchase(level) }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else finish()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AndroidVip")
            webView.destroy()
        }
        worker.shutdownNow()
        super.onDestroy()
    }
}
