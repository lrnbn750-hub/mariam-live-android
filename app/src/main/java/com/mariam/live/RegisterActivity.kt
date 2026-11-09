package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class RegisterActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val server = "http://185.174.252.118:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purple = Color.rgb(112, 78, 255)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 30, 32, 30)
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val title = TextView(this).apply {
            text = "Mariam Live"
            textSize = 32f
            setTextColor(purple)
            gravity = Gravity.CENTER
        }

        val username = EditText(this).apply {
            hint = "اسم المستخدم"
            setSingleLine()
        }

        val displayName = EditText(this).apply {
            hint = "الاسم الظاهر"
            setSingleLine()
        }

        val email = EditText(this).apply {
            hint = "البريد الإلكتروني"
            inputType = 33
            setSingleLine()
        }

        val password = EditText(this).apply {
            hint = "كلمة المرور"
            inputType = 129
            setSingleLine()
        }

        val register = Button(this).apply {
            text = "إنشاء الحساب"
            setBackgroundColor(purple)
            setTextColor(Color.WHITE)
        }

        val message = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 14f
        }

        register.setOnClickListener {
            val name = username.text.toString().trim()
            val display = displayName.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString()

            if (name.isEmpty() || display.isEmpty() ||
                mail.isEmpty() || pass.isEmpty()) {
                message.text = "يرجى ملء جميع الحقول"
                return@setOnClickListener
            }

            if (pass.length < 8) {
                message.text = "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
                return@setOnClickListener
            }

            register.isEnabled = false
            message.text = "جارٍ إنشاء الحساب..."

            executor.execute {
                try {
                    val connection = URL(
                        "$server/api/auth/register"
                    ).openConnection() as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type", "application/json"
                    )

                    val data = JSONObject().apply {
                        put("username", name)
                        put("email", mail)
                        put("password", pass)
                        put("displayName", display)
                    }

                    OutputStreamWriter(connection.outputStream).use {
                        it.write(data.toString())
                    }

                    val code = connection.responseCode
                    val stream = if (code in 200..299)
                        connection.inputStream
                    else
                        connection.errorStream

                    val response = stream.bufferedReader().use {
                        it.readText()
                    }

                    connection.disconnect()

                    val result = JSONObject(response)

                    if (code == 200 || code == 201) {
                        val token = result.optString("token", "")

                        if (token.isBlank() || token == "null") {
                            runOnUiThread {
                                message.text =
                                    "تم إنشاء الحساب، سجّل الدخول للمتابعة"
                                register.isEnabled = true
                            }
                            return@execute
                        }

                        getSharedPreferences(
                            "MariamLive", MODE_PRIVATE
                        ).edit()
                            .putString("token", token)
                            .apply()

                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "تم إنشاء الحساب بنجاح",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                this,
                                HomeActivity::class.java
                            )

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                            startActivity(intent)
                            finish()
                        }

                    } else {
                        val error = result.optString(
                            "error",
                            result.optString(
                                "message",
                                "تعذر إنشاء الحساب"
                            )
                        )

                        runOnUiThread {
                            message.text = error
                            register.isEnabled = true
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        message.text =
                            "تعذر الاتصال بالسيرفر: ${e.message}"
                        register.isEnabled = true
                    }
                }
            }
        }

        val back = TextView(this).apply {
            text = "لديك حساب؟ تسجيل الدخول"
            gravity = Gravity.CENTER
            setTextColor(purple)
            setPadding(0, 20, 0, 10)
            setOnClickListener { finish() }
        }

        layout.addView(title)
        layout.addView(username)
        layout.addView(displayName)
        layout.addView(email)
        layout.addView(password)
        layout.addView(register)
        layout.addView(message)
        layout.addView(back)

        setContentView(layout)
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
