package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class LegalActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra("legal_page") ?: "privacy"
        val file = when (page) {
            "terms" -> "terms.html"
            "delete" -> "delete-account.html"
            else -> "privacy.html"
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 245, 242))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = TextView(this).apply {
            text = when (page) {
                "terms" -> "شروط الاستخدام"
                "delete" -> "حذف الحساب"
                else -> "سياسة الخصوصية"
            }
            textSize = 18f
            setTextColor(Color.rgb(32, 33, 42))
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.WHITE)
            setOnClickListener { finish() }
        }
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        web = WebView(this).apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "mailto") {
                        try { startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                        catch (_: Exception) { Toast.makeText(this@LegalActivity, "لا يوجد تطبيق بريد مثبت", Toast.LENGTH_SHORT).show() }
                        return true
                    }
                    return uri.scheme != "file"
                }
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val uri = Uri.parse(url)
                    if (uri.scheme == "mailto") {
                        try { startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                        catch (_: Exception) { Toast.makeText(this@LegalActivity, "لا يوجد تطبيق بريد مثبت", Toast.LENGTH_SHORT).show() }
                        return true
                    }
                    return uri.scheme != "file"
                }
            }
            loadUrl("file:///android_asset/legal/$file")
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }
}
