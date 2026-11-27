package com.mariam.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat

enum class WalletKind { COINS, DIAMONDS }

/**
 * Wallet card (charge coins / earned diamonds). Display only: the amount is written by
 * ProfileActivity into [amountView] exactly like before; click handling stays in the activity.
 */
class WalletCardView(
    context: Context,
    private val kind: WalletKind,
    labelText: String
) : FrameLayout(context) {

    val amountView: TextView
    val labelView: TextView

    private val d = resources.displayMetrics.density
    private val radius = 15f * d
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private var body: LinearGradient? = null
    private var gloss: LinearGradient? = null

    private val colors: IntArray = if (kind == WalletKind.COINS)
        intArrayOf(Color.WHITE, Color.WHITE, Color.WHITE)
    else
        intArrayOf(Color.WHITE, Color.WHITE, Color.WHITE)

    private val watermark: Drawable? = ContextCompat.getDrawable(
        context, if (kind == WalletKind.COINS) R.drawable.ic_wallet_coin else R.drawable.ic_wallet_diamond
    )?.mutate()

    init {
        setWillNotDraw(false)
        minimumHeight = (52f * d).toInt()
        isClickable = true
        isFocusable = true
        clipChildren = false
        elevation = 1.5f * d
        setPadding((8f * d).toInt(), (6f * d).toInt(), (8f * d).toInt(), (6f * d).toInt())
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
                outline.alpha = 1f
            }
        }

        // compact row (visual order): arrow | amount + label (right aligned) | icon chip
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val chip = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke((1f * d).toInt(), 0xFFE7EBEA.toInt())
            }
        }
        val icon = ImageView(context).apply {
            setImageResource(if (kind == WalletKind.COINS) R.drawable.ic_wallet_coin else R.drawable.ic_wallet_diamond)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        chip.addView(icon, LayoutParams((18f * d).toInt(), (18f * d).toInt(), Gravity.CENTER))
        val arrow = ImageView(context).apply {
            setImageResource(R.drawable.ic_profile_chevron)
            setColorFilter(0xFF9AA5A3.toInt())
            setPadding((2f * d).toInt(), (2f * d).toInt(), (2f * d).toInt(), (2f * d).toInt())
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFFF5F7F6.toInt()) }
        }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        amountView = TextView(context).apply {
            text = "0"
            setTextColor(0xFF202B2A.toInt())
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(amountView, 11, 17, 1, TypedValue.COMPLEX_UNIT_SP)
        texts.addView(amountView, LinearLayout.LayoutParams(-1, (21f * d).toInt()))
        labelView = TextView(context).apply {
            text = labelText
            setTextColor(0xFF788582.toInt())
            textSize = 10f
            maxLines = 1
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        texts.addView(labelView, LinearLayout.LayoutParams(-1, -2))
        content.addView(arrow, LinearLayout.LayoutParams((17f * d).toInt(), (17f * d).toInt()))
        content.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = (5f * d).toInt(); marginEnd = (6f * d).toInt()
        })
        content.addView(chip, LinearLayout.LayoutParams((28f * d).toInt(), (28f * d).toInt()))
        addView(content, LayoutParams(-1, -2))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        body = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        gloss = LinearGradient(0f, 0f, 0f, h * 0.55f, 0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        paint.style = Paint.Style.FILL
        paint.shader = body
        canvas.drawRoundRect(rect, radius, radius, paint)

        canvas.save()
        canvas.clipPath(clip)
        paint.shader = null
        paint.color = 0x08FFFFFF
        canvas.drawCircle(14f * d, 6f * d, 44f * d, paint)
        paint.color = 0x06FFFFFF
        canvas.drawCircle(w * 0.78f, h + 4f * d, 40f * d, paint)

        watermark?.let {
            val size = (44f * d).toInt()
            val left = (-8f * d).toInt()
            val topY = (h - size * 0.7f).toInt()
            it.setBounds(left, topY, left + size, topY + size)
            it.alpha = 16
            canvas.save()
            canvas.rotate(-14f, left + size / 2f, topY + size / 2f)
            it.draw(canvas)
            canvas.restore()
        }

        paint.shader = gloss
        canvas.drawRect(0f, 0f, w, h * 0.55f, paint)
        canvas.restore()

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = 0xFFE6ECEA.toInt()
        rect.set(0.5f * d, 0.5f * d, w - 0.5f * d, h - 0.5f * d)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
}
