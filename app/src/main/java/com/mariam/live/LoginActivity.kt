
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

class LoginActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val server = "http://185.174.252.118:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purple = Color.rgb(112, 78, 255)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 40, 32, 40)
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val title = TextView(this).apply {
            text = "Mariam Live"
            textSize = 34f
            setTextColor(purple)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "مرحباً بعودتك ❤️"
            textSize = 19f
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 35)
        }

        val identifier = EditText(this).apply {
            hint = "البريد الإلكتروني أو اسم المستخدم"
            textSize = 16f
            setSingleLine()
        }

        val password = EditText(this).apply {
            hint = "كلمة المرور"
            textSize = 16f
            inputType = 129
            setSingleLine()
        }

        val login = Button(this).apply {
            text = "تسجيل الدخول"
            setBackgroundColor(purple)
            setTextColor(Color.WHITE)
        }

        val message = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 14f
        }

        login.setOnClickListener {

            val user = identifier.text.toString().trim()
            val pass = password.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                message.text = "يرجى إدخال بيانات الدخول"
                return@setOnClickListener
            }

            login.isEnabled = false
            message.text = "جارٍ تسجيل الدخول..."

            executor.execute {
                try {
                    val connection = URL(
                        "$server/api/auth/login"
                    ).openConnection() as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true

                    connection.setRequestProperty(
                        "Content-Type", "application/json"
                    )

                    val data = JSONObject().apply {
                        put("identifier", user)
                        put("password", pass)
                    }

                    OutputStreamWriter(
                        connection.outputStream
                    ).use {
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

                    if (code == 200) {

                        val token = result.optString("token")

                        if (token.isBlank() || token == "null") {
                            throw Exception("لم يصل رمز الدخول من السيرفر")
                        }

                        getSharedPreferences(
                            "MariamLive", MODE_PRIVATE
                        ).edit()
                            .putString("token", token)
                            .apply()

                        runOnUiThread {
                            startActivity(
                                Intent(
                                    this,
                                    HomeActivity::class.java
                                )
                            )
                            finish()
                        }

                    } else {
                        val error = result.optString(
                            "error",
                            "بيانات الدخول غير صحيحة"
                        )

                        runOnUiThread {
                            message.text = error
                            login.isEnabled = true
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        message.text =
                            "فشل الاتصال: ${e.message}"
                        login.isEnabled = true
                    }
                }
            }
        }

        val register = TextView(this).apply {
            text = "ليس لديك حساب؟ إنشاء حساب جديد"
            textSize = 16f
            setTextColor(purple)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)

            setOnClickListener {
                startActivity(
                    Intent(
                        this@LoginActivity,
                        RegisterActivity::class.java
                    )
                )
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(identifier)
        layout.addView(password)

        layout.addView(login,
            LinearLayout.LayoutParams(-1, 60).apply {
                topMargin = 25
            }
        )

        layout.addView(message)
        layout.addView(register)

        setContentView(layout)
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
