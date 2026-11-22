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
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class LegalActivity : Activity() {
    private lateinit var web: WebView
    private val purple = Color.rgb(35, 17, 66)
    private val gold = Color.rgb(235, 179, 72)

    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra("legal_page") ?: "privacy"
        val file = when (page) {
            "terms" -> "terms.html"
            "delete" -> "delete-account.html"
            else -> "privacy.html"
        }
        val title = when (page) {
            "terms" -> "شروط الاستخدام"
            "delete" -> "حذف الحساب"
            else -> "سياسة الخصوصية"
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 245, 251))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(12f), dp(18f), dp(12f))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(purple, Color.rgb(69, 37, 105)))
            elevation = dp(4f).toFloat()
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            isClickable = true
            setPadding(dp(8f), 0, dp(8f), dp(4f))
            setOnClickListener { finish() }
            contentDescription = "رجوع"
        }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(42f), dp(48f)))
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(this).apply {
            text = "BAK LIVE"
            textSize = 12f
            setTextColor(gold)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        heading.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        toolbar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(TextView(this).apply {
            text = "◆"
            textSize = 23f
            setTextColor(gold)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(38f), dp(44f)))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(247, 245, 251))
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
