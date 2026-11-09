
package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class RegisterActivity : Activity() {

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

        val subtitle = TextView(this).apply {
            text = "إنشاء حساب جديد ❤️"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 25)
        }

        val username = EditText(this).apply {
            hint = "اسم المستخدم"
            textSize = 16f
            setSingleLine()
        }

        val email = EditText(this).apply {
            hint = "البريد الإلكتروني"
            textSize = 16f
            inputType = 33
            setSingleLine()
        }

        val password = EditText(this).apply {
            hint = "كلمة المرور"
            textSize = 16f
            inputType = 129
            setSingleLine()
        }

        val confirmPassword = EditText(this).apply {
            hint = "تأكيد كلمة المرور"
            textSize = 16f
            inputType = 129
            setSingleLine()
        }

        val register = Button(this).apply {
            text = "إنشاء الحساب"
            setBackgroundColor(purple)
            setTextColor(Color.WHITE)

            setOnClickListener {
                val name = username.text.toString().trim()
                val mail = email.text.toString().trim()
                val pass = password.text.toString()
                val confirm = confirmPassword.text.toString()

                when {
                    name.isEmpty() || mail.isEmpty() ||
                    pass.isEmpty() || confirm.isEmpty() -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            "يرجى ملء جميع الحقول",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(mail).matches() -> {
                        email.error = "البريد الإلكتروني غير صحيح"
                    }

                    pass.length < 8 -> {
                        password.error = "كلمة المرور 8 أحرف على الأقل"
                    }

                    pass != confirm -> {
                        confirmPassword.error = "كلمتا المرور غير متطابقتين"
                    }

                    else -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            "البيانات جاهزة، الربط بالسيرفر في الخطوة القادمة",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        val backToLogin = TextView(this).apply {
            text = "لديك حساب؟ تسجيل الدخول"
            textSize = 16f
            setTextColor(purple)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)

            setOnClickListener {
                finish()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(username)
        layout.addView(email)
        layout.addView(password)
        layout.addView(confirmPassword)

        layout.addView(register,
            LinearLayout.LayoutParams(-1, 60).apply {
                topMargin = 25
            }
        )

        layout.addView(backToLogin)

        setContentView(layout)
    }
}
