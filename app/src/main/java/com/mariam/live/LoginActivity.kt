
package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class LoginActivity : Activity() {

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

        val login = Button(this).apply {
            text = "تسجيل الدخول"
            setBackgroundColor(purple)
            setTextColor(Color.WHITE)

            setOnClickListener {
                Toast.makeText(
                    this@LoginActivity,
                    "سنربط تسجيل الدخول بالسيرفر",
                    Toast.LENGTH_SHORT
                ).show()
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
                    Intent(this@LoginActivity, RegisterActivity::class.java)
                )
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(email)
        layout.addView(password)

        layout.addView(login,
            LinearLayout.LayoutParams(-1, 60).apply {
                topMargin = 25
            }
        )

        layout.addView(register)

        setContentView(layout)
    }
}
