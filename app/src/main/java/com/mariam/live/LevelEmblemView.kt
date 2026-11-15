package com.mariam.live

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Draws the tier emblem using the same artwork and palette as the profile level badge. */
internal class LevelEmblemPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val glyph = Path()
    private val lockedFilter = ColorMatrixColorFilter(ColorMatrix().apply {
        setSaturation(0.15f)
        postConcat(ColorMatrix(floatArrayOf(0.7f,0f,0f,0f,0f,0f,0.7f,0f,0f,0f,0f,0f,0.7f,0f,0f,0f,0f,0f,1f,0f)))
    })

    private fun sparkle(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        LevelTiers.buildGlyph(LevelTiers.G_SPARKLE, x, y, r, glyph)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = color
        c.drawPath(glyph, paint)
    }

    fun draw(c: Canvas, cx: Float, cy: Float, r: Float, level: Int, locked: Boolean, withGlow: Boolean, rayRotation: Float) {
        val ti = LevelTiers.indexOf(level)
        val t = LevelTiers.ALL[ti]
        val royal = ti == 9
        paint.colorFilter = if (locked) lockedFilter else null
        if (withGlow && !locked) {
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(cx, cy, r * 1.75f,
                intArrayOf(LevelTiers.alpha(t.glow, 120), LevelTiers.alpha(t.glow, 0)),
                floatArrayOf(0.35f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r * 1.75f, paint)
        }
        if (royal && !locked) {
            paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = r * 0.05f
            paint.strokeCap = Paint.Cap.ROUND; paint.color = LevelTiers.alpha(t.glow, 200)
            for (i in 0 until 24) {
                val a = Math.toRadians((i * 15 + rayRotation).toDouble())
                val r1 = r * 1.14f
                val r2 = r * (if (i % 2 == 0) 1.34f else 1.24f)
                c.drawLine(cx + (cos(a) * r1).toFloat(), cy + (sin(a) * r1).toFloat(),
                    cx + (cos(a) * r2).toFloat(), cy + (sin(a) * r2).toFloat(), paint)
            }
            paint.strokeCap = Paint.Cap.BUTT
        }
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(cx-r, cy-r, cx+r, cy+r, t.ringA, t.ringB, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = r * 0.045f
        paint.color = 0x28000000; c.drawCircle(cx, cy, r * 0.9f, paint)
        val innerR = r * 0.82f
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, cy-r, 0f, cy+r, t.innerA, t.innerB, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, innerR, paint)
        c.save(); path.reset(); path.addCircle(cx, cy, innerR, Path.Direction.CW); c.clipPath(path)
        paint.shader = LinearGradient(0f, cy-r, 0f, cy, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawRect(cx-r, cy-r, cx+r, cy, paint); c.restore()
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = r * 0.073f
        paint.color = 0x66FFFFFF; c.drawCircle(cx, cy, r * 0.964f, paint)

        val ops: FloatArray
        val gs: Float
        when (t.shape) {
            0 -> { ops = LevelTiers.G_STAR; gs = 0.58f * r }
            1, 5 -> { ops = LevelTiers.G_GEM; gs = 0.50f * r }
            2 -> { ops = LevelTiers.G_SHIELD; gs = 0.50f * r }
            3 -> { ops = LevelTiers.G_BOLT; gs = 0.52f * r }
            else -> { ops = LevelTiers.G_CROWN; gs = 0.52f * r }
        }
        paint.style = Paint.Style.FILL
        LevelTiers.buildGlyph(ops, cx, cy + r * 0.05f, gs, glyph)
        paint.color = 0x5A000000; c.drawPath(glyph, paint)
        LevelTiers.buildGlyph(ops, cx, cy, gs, glyph)
        paint.color = t.glyph; c.drawPath(glyph, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = r * 0.035f
        paint.color = 0x73FFFFFF; c.drawPath(glyph, paint)
        if (t.shape == 1 || t.shape == 5) {
            LevelTiers.buildGlyph(LevelTiers.G_GEM_FACETS, cx, cy, gs, glyph)
            paint.strokeWidth = r * 0.06f; paint.color = LevelTiers.alpha(t.innerA, 170)
            c.drawPath(glyph, paint)
        }
        if (t.shape == 5) sparkle(c, cx+r*0.45f, cy-r*0.45f, r*0.30f, 0xFFFFFFFF.toInt())
        if (royal && !locked) {
            val gold = 0xFFFFF0B0.toInt()
            sparkle(c, cx+r*0.80f, cy-r*0.92f, r*0.20f, gold)
            sparkle(c, cx-r*0.95f, cy+r*0.55f, r*0.14f, gold)
            sparkle(c, cx+r*0.98f, cy+r*0.62f, r*0.11f, gold)
        }
        paint.colorFilter = null
    }
}

/** Large animated emblem used in the wealth-level screen. */
class LevelEmblemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val painter = LevelEmblemPainter()
    private var rotation0 = 0f
    private var animator: ValueAnimator? = null
    var level: Int = 1
        set(value) { field = value.coerceIn(1, 100); updateAnimator(); invalidate() }
    var locked: Boolean = false
        set(value) { field = value; invalidate() }
    var animateRays: Boolean = true
        set(value) { field = value; updateAnimator() }

    private fun updateAnimator() {
        val run = animateRays && level >= 91 && isAttachedToWindow && visibility == VISIBLE
        if (run) {
            if (animator == null) animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 24000L; repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { rotation0 = it.animatedValue as Float; invalidate() }
                start()
            }
        } else { animator?.cancel(); animator = null }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimator() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility); updateAnimator()
    }
    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f * 0.60f
        painter.draw(canvas, width / 2f, height / 2f, r, level, locked, true, rotation0)
    }
}