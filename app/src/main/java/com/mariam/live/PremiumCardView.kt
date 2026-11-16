package com.mariam.live

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

enum class PremiumKind { VIP, SVIP }

/**
 * VIP / SVIP membership card with a slow diagonal shimmer. Display only: the click
 * (opening VipActivity) is attached by ProfileActivity.
 */
class PremiumCardView(
    context: Context,
    private val kind: PremiumKind,
    titleText: String,
    subtitleText: String
) : FrameLayout(context) {

    private val d = resources.displayMetrics.density
    private val radius = 18f * d
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private var body: LinearGradient? = null
    private var animator: ValueAnimator? = null
    private var phase = 1f
    private val beamW = 40f * d
    private val skew = 0.35f

    private val isVip = kind == PremiumKind.VIP
    private val colors: IntArray = if (isVip)
        intArrayOf(0xFFFFE9A3.toInt(), 0xFFF7C546.toInt(), 0xFFD9962A.toInt())
    else
        intArrayOf(0xFF2E1A66.toInt(), 0xFF4B2A95.toInt(), 0xFF1A0F3A.toInt())
    private val rimColor = if (isVip) 0x88FFFFFF.toInt() else 0x80FFD76A.toInt()
    private val orbColor = if (isVip) 0x38FFFFFF else 0x22FFD76A
    private val beam: LinearGradient = LinearGradient(
        0f, 0f, beamW, 0f,
        intArrayOf(0x00FFFFFF, if (isVip) 0xA0FFFFFF.toInt() else 0x66FFE9A8, 0x00FFFFFF),
        floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
    )

    init {
        setWillNotDraw(false)
        minimumHeight = (62f * d).toInt()
        isClickable = true
        isFocusable = true
        elevation = 3f * d
        contentDescription = titleText
        setPadding((14f * d).toInt(), (8f * d).toInt(), (14f * d).toInt(), (8f * d).toInt())
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
                outline.alpha = 1f
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply {
            setImageResource(if (isVip) R.drawable.ic_profile_vip_color else R.drawable.ic_profile_svip_color)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val textColor = if (isVip) 0xFF3A2400.toInt() else 0xFFFFE9A8.toInt()
        val title = TextView(context).apply {
            text = titleText
            setTextColor(textColor)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD_ITALIC)
            letterSpacing = 0.04f
            maxLines = 1
            gravity = Gravity.RIGHT
        }
        val sub = TextView(context).apply {
            text = subtitleText
            setTextColor(if (isVip) 0xCC3A2400.toInt() else 0xCCFFFFFF.toInt())
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.RIGHT
        }
        texts.addView(title)
        texts.addView(sub)
        // visual order: texts (right aligned) | crown icon on the right edge
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = (10f * d).toInt() })
        row.addView(icon, LinearLayout.LayoutParams((34f * d).toInt(), (34f * d).toInt()))
        addView(row, LayoutParams(-1, -2, Gravity.CENTER_VERTICAL))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        body = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
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
        paint.color = orbColor
        canvas.drawCircle(w - 26f * d, h / 2f, 44f * d, paint)
        canvas.drawCircle(18f * d, h + 10f * d, 40f * d, paint)

        if (animator != null) {
            val x = -beamW + (w + skew * h + beamW) * min(phase, 1f)
            canvas.save()
            canvas.translate(x, 0f)
            canvas.skew(-skew, 0f)
            paint.shader = beam
            canvas.drawRect(0f, 0f, beamW, h, paint)
            paint.shader = null
            canvas.restore()
        }
        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = rimColor
        rect.set(0.5f * d, 0.5f * d, w - 0.5f * d, h - 0.5f * d)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    // ------------------------------------------------------------------ shimmer lifecycle
    private fun shouldAnimateShimmer(): Boolean {
        if (!isAttachedToWindow || visibility != VISIBLE || windowVisibility != VISIBLE) return false
        return if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled() else true
    }

    private fun updateAnimator() {
        if (shouldAnimateShimmer()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1.9f).apply {
                    duration = 3600L
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        phase = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        } else {
            animator?.cancel()
            animator = null
            invalidate()
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimator() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateAnimator() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility); updateAnimator()
    }
}
