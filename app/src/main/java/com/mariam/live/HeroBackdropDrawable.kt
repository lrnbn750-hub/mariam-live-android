package com.mariam.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import java.util.Random

/**
 * Soft mint-to-cyan profile header with subtle light gradients and rounded bottom corners.
 * Pure drawing, resolution independent.
 */
class HeroBackdropDrawable(context: Context) : Drawable() {
    private val d = context.resources.displayMetrics.density
    private val corner = 32f * d
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowGold = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowGreen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowViolet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private val box = RectF()
    private var stars = FloatArray(0) // x, y, radius, alpha (normalised x,y)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        box.set(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
        clip.reset()
        clip.addRoundRect(box, floatArrayOf(0f, 0f, 0f, 0f, corner, corner, corner, corner), Path.Direction.CW)

        fill.shader = LinearGradient(
            0f, box.top, 0f, box.bottom,
            intArrayOf(0xFFF5FFE9.toInt(), 0xFFDDFBF1.toInt(), 0xFFB8F3EA.toInt()),
            floatArrayOf(0f, 0.52f, 1f), Shader.TileMode.CLAMP
        )
        glowGold.shader = RadialGradient(
            box.left + w * 0.88f, box.top + h * 0.22f, w * 0.60f,
            0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        glowGreen.shader = RadialGradient(
            box.left + w * 0.06f, box.top + h * 0.92f, w * 0.55f,
            0x3328BE91, 0x0028BE91, Shader.TileMode.CLAMP
        )
        glowViolet.shader = RadialGradient(
            box.left + w * 0.30f, box.top + h * 0.10f, w * 0.50f,
            0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        line.shader = LinearGradient(
            box.left, 0f, box.right, 0f,
            intArrayOf(0x00FFFFFF, 0x66FFFFFF, 0x0028BE91),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        line.strokeWidth = 1f * d

        val rnd = Random(11L)
        stars = FloatArray(30 * 4)
        for (i in 0 until 30) {
            stars[i * 4] = rnd.nextFloat()
            stars[i * 4 + 1] = rnd.nextFloat() * 0.85f
            stars[i * 4 + 2] = (0.5f + rnd.nextFloat() * 1.1f) * d
            stars[i * 4 + 3] = 40f + rnd.nextFloat() * 110f
        }
    }

    override fun draw(canvas: Canvas) {
        if (box.isEmpty) return
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(box, fill)
        canvas.drawRect(box, glowViolet)
        canvas.drawRect(box, glowGold)
        canvas.drawRect(box, glowGreen)
        var i = 0
        while (i < stars.size) {
            starPaint.color = ((stars[i + 3].toInt() and 0xFF) shl 24) or 0x00FFFFFF
            canvas.drawCircle(box.left + stars[i] * box.width(), box.top + stars[i + 1] * box.height(), stars[i + 2], starPaint)
            i += 4
        }
        canvas.drawLine(box.left, box.bottom - 1f * d, box.right, box.bottom - 1f * d, line)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
