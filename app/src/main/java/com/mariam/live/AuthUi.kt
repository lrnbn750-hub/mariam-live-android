package com.mariam.live

import android.app.Activity
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
            setPadding(dp(activity, 22), dp(activity, 30), dp(activity, 22), dp(activity, 30))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF10101F.toInt(), 0xFF242044.toInt(), 0xFF17152B.toInt())
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
            setPadding(0, dp(activity, 18), 0, dp(activity, 24))
        }
        val mascot = PenguinLogoView(activity).apply {
            contentDescription = "شعار البطريق BAK LIVE"
            layoutParams = LinearLayout.LayoutParams(dp(activity, 108), dp(activity, 108))
            elevation = dp(activity, 5).toFloat()
        }
        val title = TextView(activity).apply {
            text = "BAK LIVE"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 10), 0, dp(activity, 4))
        }
        val sub = TextView(activity).apply {
            text = tagline
            textSize = 14f
            setTextColor(0xFFD0CBE6.toInt())
            gravity = Gravity.CENTER
        }
        wrap.addView(mascot)
        wrap.addView(title)
        wrap.addView(sub)
        return wrap
    }

    private class PenguinLogoView(activity: Activity) : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun fill(color: Int) { paint.style = Paint.Style.FILL; paint.color = color; paint.strokeWidth = 3f }
        private fun stroke(color: Int, width: Float = 3f) { paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = width; paint.strokeJoin = Paint.Join.ROUND; paint.strokeCap = Paint.Cap.ROUND }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val save = canvas.save()
            canvas.scale(width / 120f, height / 120f)
            // Small friendly cartoon penguin, drawn directly in code (no bitmap background).
            fill(0xFFFFB52E.toInt())
            canvas.drawOval(20f, 86f, 52f, 108f, paint)
            canvas.drawOval(68f, 86f, 100f, 108f, paint)
            fill(0xFF20202A.toInt())
            canvas.drawOval(17f, 14f, 103f, 105f, paint)
            fill(Color.WHITE)
            canvas.drawOval(31f, 39f, 89f, 99f, paint)
            fill(0xFF20202A.toInt())
            canvas.drawOval(34f, 31f, 53f, 55f, paint)
            canvas.drawOval(67f, 31f, 86f, 55f, paint)
            fill(Color.WHITE)
            canvas.drawCircle(44f, 40f, 5f, paint)
            canvas.drawCircle(77f, 40f, 5f, paint)
            fill(0xFF20202A.toInt())
            canvas.drawCircle(45f, 41f, 2.5f, paint)
            canvas.drawCircle(78f, 41f, 2.5f, paint)
            fill(0xFFFFA928.toInt())
            val beak = Path().apply { moveTo(49f, 53f); quadTo(60f, 47f, 71f, 53f); quadTo(61f, 70f, 49f, 53f); close() }
            canvas.drawPath(beak, paint)
            fill(0xFF292933.toInt())
            canvas.drawOval(4f, 55f, 28f, 83f, paint)
            canvas.drawOval(92f, 55f, 116f, 83f, paint)
            fill(Color.WHITE)
            canvas.drawOval(9f, 57f, 27f, 78f, paint)
            canvas.drawOval(93f, 57f, 111f, 78f, paint)
            stroke(0xFF171720.toInt(), 3f)
            canvas.drawOval(17f, 14f, 103f, 105f, paint)
            canvas.drawOval(31f, 39f, 89f, 99f, paint)
            canvas.restoreToCount(save)
        }
    }

    fun card(activity: Activity): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 22), dp(activity, 26), dp(activity, 22), dp(activity, 26))
            background = GradientDrawable().apply {
                setColor(0xFFFCFBFF.toInt())
                cornerRadius = dp(activity, 28).toFloat()
                setStroke(dp(activity, 1), 0x33FFFFFF)
            }
            elevation = dp(activity, 12).toFloat()
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
            text = if (symbol.isBlank()) title else "$symbol   $title"
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
                android.widget.Toast.makeText(activity, "قريباً في BAK LIVE ✨", android.widget.Toast.LENGTH_SHORT).show()
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
            setTextColor(0xFF7048E8.toInt())
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
