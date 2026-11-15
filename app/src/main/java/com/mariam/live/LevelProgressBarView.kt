package com.mariam.live

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Rounded progress bar tinted with the current tier colors (glossy fill + glowing tip). */
class LevelProgressBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private var shown = 0f
    private var tierIdx = 0
    private var animator: ValueAnimator? = null

    fun setLevel(level: Int) { tierIdx = LevelTiers.indexOf(level); invalidate() }

    fun setProgress(value: Float, animate: Boolean = true) {
        val target = value.coerceIn(0f, 1f)
        animator?.cancel()
        if (!animate) { shown = target; invalidate(); return }
        animator = ValueAnimator.ofFloat(shown, target).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        val t = LevelTiers.ALL[tierIdx]
        val w = width.toFloat(); val h = height.toFloat(); val rad = h / 2f
        rect.set(0f, 0f, w, h)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = 0xFF1A2540.toInt()
        canvas.drawRoundRect(rect, rad, rad, paint)
        if (shown <= 0f) return
        val fillW = maxOf(h, w * shown)
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val left = if (rtl) w - fillW else 0f
        val right = if (rtl) w else fillW
        rect.set(left, 0f, right, h)
        paint.shader = LinearGradient(left, 0f, right, 0f, t.ringB, t.glow, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, rad, rad, paint)
        canvas.save()
        clip.reset(); clip.addRoundRect(rect, rad, rad, Path.Direction.CW); canvas.clipPath(clip)
        paint.shader = LinearGradient(0f, 0f, 0f, h / 2f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(left, 0f, right, h / 2f, paint)
        canvas.restore()
        if (shown < 1f) {
            val tipX = if (rtl) left else right
            paint.shader = RadialGradient(tipX, h / 2f, h * 1.4f,
                intArrayOf(LevelTiers.alpha(t.glow, 150), LevelTiers.alpha(t.glow, 0)), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(tipX, h / 2f, h * 1.4f, paint)
        }
        paint.shader = null
    }
}