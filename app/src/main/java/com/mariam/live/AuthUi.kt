package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object AuthUi {
    private const val PURPLE = 0xFF7957F6.toInt()
    private const val PINK = 0xFFFF6B9A.toInt()
    private const val INK = 0xFF17152B.toInt()
    private const val MUTED = 0xFF77748B.toInt()

    fun background(activity: Activity, content: View) {
        activity.window.statusBarColor = 0xFF17152B.toInt()
        activity.window.navigationBarColor = 0xFF17152B.toInt()
        activity.window.decorView.systemUiVisibility = 0
        activity.setContentView(content)
    }

    fun page(activity: Activity): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(activity, 24), dp(activity, 22), dp(activity, 24), dp(activity, 24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF17152B.toInt(), 0xFF252044.toInt(), 0xFF17152B.toInt())
            )
        }
    }

    fun scroll(activity: Activity, page: View): ScrollView =
        ScrollView(activity).apply {
            isFillViewport = true
            clipToPadding = false
            addView(page)
        }

    fun brand(activity: Activity, tagline: String): View {
        val wrap = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 12), 0, dp(activity, 24))
        }
        val mark = TextView(activity).apply {
            text = "M"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(PURPLE, PINK)
            ).apply { shape = GradientDrawable.OVAL }
            layoutParams = LinearLayout.LayoutParams(dp(activity, 76), dp(activity, 76))
        }
        val title = TextView(activity).apply {
            text = "Mariam Live"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 12), 0, dp(activity, 4))
        }
        val sub = TextView(activity).apply {
            text = tagline
            textSize = 14f
            setTextColor(0xFFD0CBE6.toInt())
            gravity = Gravity.CENTER
        }
        wrap.addView(mark); wrap.addView(title); wrap.addView(sub)
        return wrap
    }

    fun card(activity: Activity): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 22), dp(activity, 24), dp(activity, 22), dp(activity, 22))
            background = GradientDrawable().apply {
                setColor(0xFFFCFBFF.toInt())
                cornerRadius = dp(activity, 24).toFloat()
            }
            elevation = dp(activity, 8).toFloat()
        }

    fun heading(activity: Activity, text: String): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(INK)
            gravity = Gravity.RIGHT
        }

    fun hint(activity: Activity, text: String): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = 13f
            setTextColor(MUTED)
            gravity = Gravity.RIGHT
            setPadding(0, dp(activity, 5), 0, dp(activity, 18))
        }

    fun label(activity: Activity, text: String): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF3C3852.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(activity, 10), 0, dp(activity, 7))
        }

    fun field(activity: Activity, placeholder: String, inputType: Int): EditText =
        EditText(activity).apply {
            hint = placeholder
            textSize = 15f
            setTextColor(INK)
            setHintTextColor(0xFF9B97AA.toInt())
            this.inputType = inputType
            textDirection = View.TEXT_DIRECTION_LOCALE
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setSingleLine(true)
            setPadding(dp(activity, 15), 0, dp(activity, 15), 0)
            background = GradientDrawable().apply {
                setColor(0xFFF3F1F8.toInt())
                cornerRadius = dp(activity, 13).toFloat()
                setStroke(dp(activity, 1), 0xFFE8E4F0.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(activity, 54))
        }

    fun primaryButton(activity: Activity, text: String): Button =
        Button(activity).apply {
            this.text = text
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(PURPLE, PINK)
            ).apply { cornerRadius = dp(activity, 14).toFloat() }
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(-1, dp(activity, 54)).apply {
                topMargin = dp(activity, 18)
            }
        }

    fun socialButton(activity: Activity, title: String, symbol: String): TextView =
        TextView(activity).apply {
            text = "$symbol   $title"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(INK)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(activity, 14).toFloat()
                setStroke(dp(activity, 1), 0xFFE6E1EF.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(activity, 50)).apply {
                topMargin = dp(activity, 9)
            }
            setOnClickListener {
                android.widget.Toast.makeText(activity, "قريباً في Mariam Live ✨", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

    fun socialDivider(activity: Activity): TextView =
        TextView(activity).apply {
            text = "أو تابع باستخدام"
            textSize = 12f
            setTextColor(MUTED)
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 15), 0, dp(activity, 5))
        }

    fun message(activity: Activity): TextView =
        TextView(activity).apply {
            textSize = 13f
            setTextColor(0xFFB32645.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 12), 0, 0)
            visibility = View.GONE
        }

    fun link(activity: Activity, text: String): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFD9CFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 22), 0, dp(activity, 12))
        }

    fun showMessage(view: TextView, text: String, success: Boolean = false) {
        view.text = text
        view.setTextColor(if (success) 0xFF21845A.toInt() else 0xFFB32645.toInt())
        view.visibility = View.VISIBLE
    }

    fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
