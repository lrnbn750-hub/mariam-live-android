package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class LoginActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var loginPlayer: MediaPlayer? = null
    private var loginSurface: Surface? = null

    private fun actionButton(text: String, colors: IntArray): TextView = TextView(this).apply {
        this.text = text
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = AuthUi.dp(this@LoginActivity, 28).toFloat()
        }
        elevation = AuthUi.dp(this@LoginActivity, 3).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, AuthUi.dp(this@LoginActivity, 62)).apply {
            topMargin = AuthUi.dp(this@LoginActivity, 12)
        }
    }

    private fun saveSession(result: JSONObject) {
        val token = result.optString("token")
        if (token.isBlank() || token == "null") throw Exception("لم يصل رمز الدخول من الخادم")
        val user = result.optJSONObject("user") ?: JSONObject()
        getSharedPreferences("MariamLive", MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("user_id", user.optString("id"))
            .putString("username", user.optString("username"))
            .putString("phone", user.optString("phone"))
            .putString("email", user.optString("email")).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(AuthUi.dp(this@LoginActivity, 30), AuthUi.dp(this@LoginActivity, 28),
                AuthUi.dp(this@LoginActivity, 30), AuthUi.dp(this@LoginActivity, 24))
            background = ColorDrawable(Color.TRANSPARENT)
        }

        // Brand area, centered like the reference screen.
        val brand = AuthUi.brand(this, "صوتك وأصدقاؤك وعالمك يبدأ من هنا")
        page.addView(brand, LinearLayout.LayoutParams(-1, AuthUi.dp(this, 190)).apply {
            bottomMargin = AuthUi.dp(this@LoginActivity, 34)
        })

        val intro = TextView(this).apply {
            text = "قم بتسجيل الدخول لإكمال ملفك"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(5f, 0f, 2f, 0x99000000.toInt())
            setPadding(0, 0, 0, AuthUi.dp(this@LoginActivity, 22))
        }
        page.addView(intro)

        val emailChoice = actionButton("البريد الإلكتروني", intArrayOf(0xFF7650F5.toInt(), 0xFFB24CEB.toInt()))
        val phoneChoice = actionButton("رقم الهاتف", intArrayOf(0xFF1686F8.toInt(), 0xFF3766E8.toInt()))
        page.addView(emailChoice)
        page.addView(phoneChoice)

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(AuthUi.dp(this@LoginActivity, 18), AuthUi.dp(this@LoginActivity, 18),
                AuthUi.dp(this@LoginActivity, 18), AuthUi.dp(this@LoginActivity, 18))
            background = GradientDrawable().apply {
                setColor(0xCC17152B.toInt())
                cornerRadius = AuthUi.dp(this@LoginActivity, 22).toFloat()
                setStroke(AuthUi.dp(this@LoginActivity, 1), 0x44FFFFFF)
            }
        }
        val formTitle = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.RIGHT
        }
        form.addView(formTitle)

        val identifierLabel = AuthUi.label(this, "البريد الإلكتروني").apply { setTextColor(Color.WHITE) }
        val identifier = AuthUi.field(this, "أدخل البريد الإلكتروني", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val phoneLabel = AuthUi.label(this, "رقم الهاتف").apply { setTextColor(Color.WHITE) }
        val phone = AuthUi.field(this, "+964 7XX XXX XXXX", InputType.TYPE_CLASS_PHONE).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        val passwordLabel = AuthUi.label(this, "كلمة المرور").apply { setTextColor(Color.WHITE) }
        val password = AuthUi.field(this, "أدخل كلمة المرور", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        password.textDirection = View.TEXT_DIRECTION_LTR

        form.addView(identifierLabel); form.addView(identifier)
        form.addView(phoneLabel); form.addView(phone)
        form.addView(passwordLabel); form.addView(password)

        val showPassword = CheckBox(this).apply {
            text = "إظهار كلمة المرور"
            textSize = 12f
            setTextColor(Color.WHITE)
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFFB58BFF.toInt())
            setOnCheckedChangeListener { _, checked ->
                val pos = password.selectionStart
                password.inputType = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                password.setSelection(pos.coerceAtLeast(0))
            }
        }
        form.addView(showPassword)
        val message = AuthUi.message(this)
        form.addView(message)
        val submit = AuthUi.primaryButton(this, "تسجيل الدخول")
        form.addView(submit)

        var usePhone = false
        fun choose(phoneMode: Boolean) {
            usePhone = phoneMode
            formTitle.text = if (phoneMode) "تسجيل الدخول برقم الهاتف" else "تسجيل الدخول بالبريد الإلكتروني"
            identifierLabel.visibility = if (phoneMode) View.GONE else View.VISIBLE
            identifier.visibility = if (phoneMode) View.GONE else View.VISIBLE
            phoneLabel.visibility = if (phoneMode) View.VISIBLE else View.GONE
            phone.visibility = if (phoneMode) View.VISIBLE else View.GONE
            message.visibility = View.GONE
            form.visibility = View.VISIBLE
            emailChoice.visibility = View.GONE
            phoneChoice.visibility = View.GONE
        }
        emailChoice.setOnClickListener { choose(false) }
        phoneChoice.setOnClickListener { choose(true) }

        submit.setOnClickListener {
            val id = if (usePhone) phone.text.toString().trim().replace(" ", "").replace("-", "")
                else identifier.text.toString().trim()
            val pass = password.text.toString()
            if (id.isBlank() || pass.isBlank()) {
                AuthUi.showMessage(message, "أدخل بيانات الدخول وكلمة المرور")
                return@setOnClickListener
            }
            if (usePhone && !Regex("^\\+?[1-9][0-9]{7,14}$").matches(id)) {
                AuthUi.showMessage(message, "أدخل رقم هاتف دولي صحيحاً، مثال +9647XXXXXXXXX")
                return@setOnClickListener
            }
            submit.isEnabled = false
            submit.text = "جارٍ تسجيل الدخول..."
            message.visibility = View.GONE
            executor.execute {
                try {
                    val endpoint = if (usePhone) "/api/auth/phone/login" else "/api/auth/login"
                    val payload = JSONObject().apply {
                        if (usePhone) put("phone", id) else put("identifier", id)
                        put("password", pass)
                    }
                    val result = AuthApi.post(endpoint, payload)
                    if (result.status in 200..299) {
                        saveSession(result.body)
                        runOnUiThread {
                            startActivity(Intent(this, MalakWelcomeActivity::class.java))
                            finish()
                        }
                    } else {
                        val error = when (result.status) {
                            401 -> "بيانات الدخول أو كلمة المرور غير صحيحة"
                            403 -> "هذا الحساب غير نشط"
                            404 -> "الحساب غير موجود"
                            else -> result.body.optString("error", "تعذر تسجيل الدخول")
                        }
                        runOnUiThread { AuthUi.showMessage(message, error); submit.isEnabled = true; submit.text = "تسجيل الدخول" }
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        AuthUi.showMessage(message, "تعذر الاتصال بالخادم. تأكد من اتصال الإنترنت.")
                        submit.isEnabled = true
                        submit.text = "تسجيل الدخول"
                    }
                }
            }
        }

        page.addView(form)
        val create = AuthUi.link(this, "ليس لديك حساب؟ أنشئ حساباً جديداً").apply {
            setTextColor(Color.WHITE)
            setOnClickListener { startActivity(Intent(this@LoginActivity, RegisterActivity::class.java)) }
        }
        val forgot = AuthUi.link(this, "نسيت كلمة المرور؟").apply {
            setTextColor(0xFFE0D9F5.toInt())
            setOnClickListener { Toast.makeText(this@LoginActivity, "استعادة كلمة المرور ستُضاف بعد ربط التحقق", Toast.LENGTH_LONG).show() }
        }
        page.addView(create)
        page.addView(forgot)

        val root = FrameLayout(this).apply { setBackgroundColor(0xFF17152B.toInt()) }
        val videoId = resources.getIdentifier("login_background", "raw", packageName)
        if (videoId != 0) {
            val video = TextureView(this)
            root.addView(video, FrameLayout.LayoutParams(-1, -1))
            video.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    try {
                        val surface = Surface(texture)
                        loginSurface = surface
                        val player = MediaPlayer()
                        loginPlayer = player
                        player.setSurface(surface)
                        player.setDataSource(this@LoginActivity, Uri.parse("android.resource://$packageName/$videoId"))
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        player.setOnPreparedListener {
                            try {
                                it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                it.start()
                            } catch (_: Exception) { }
                        }
                        player.setOnErrorListener { _, _, _ -> true }
                        player.prepareAsync()
                    } catch (_: Exception) {
                        root.setBackgroundColor(0xFF17152B.toInt())
                    }
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    loginPlayer?.release(); loginPlayer = null
                    loginSurface?.release(); loginSurface = null
                    return true
                }
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
            }
        }
        root.addView(View(this).apply { setBackgroundColor(0x660D0B1B) },
            FrameLayout.LayoutParams(-1, -1))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(page)
            background = ColorDrawable(Color.TRANSPARENT)
        }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = 0xFF17152B.toInt()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setContentView(root)
    }

    override fun onDestroy() {
        loginPlayer?.release(); loginPlayer = null
        loginSurface?.release(); loginSurface = null
        executor.shutdown()
        super.onDestroy()
    }
}
