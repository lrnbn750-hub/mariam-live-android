package com.mariam.live

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * BAK LIVE level badge, levels 1..100. Pure Canvas drawing: no images, no libraries, crisp at any density.
 *
 * 10 tiers x 10 levels. Each tier has its own palette + emblem
 * (Steel, Emerald, Aqua, Sapphire, Amethyst, Rose, Flame, Gold, Platinum, Royal).
 * Inside a tier the badge "levels up" visually: sub-level 4+ gets a thicker rim, 6+ a soft glow,
 * 9 a glitter spark; the last tier (91-100) also gets a sun-burst emblem, sparkles and a shimmer sweep.
 *
 * UI only: set [level] from the value you already load from your API.
 *
 *   levelBadge.level = user.level          // 1..100, clamped
 */
class LevelBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Tier(
        val pillA: Int, val pillB: Int, val stroke: Int,
        val ringA: Int, val ringB: Int, val innerA: Int, val innerB: Int,
        val glyph: Int, val text: Int, val glow: Int, val shape: Int, val textShadow: Int
    )

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val glyphPath = Path()
    private val rect = RectF()

    private var levelValue = 1
    private var tierIdx = 0
    private var sub = 0
    private var labelW = 0f
    private var numW = 0f
    private var shimmerPhase = 0.55f
    private var animator: ValueAnimator? = null

    // geometry (dp)
    private val emblemR = 11f * d
    private val pillH = 16f * d
    private val extraX = 4f * d
    private val extraY = 3f * d
    private val textGap = 3f * d
    private val padEnd = 8f * d
    private val labelToNum = 2f * d

    /** Sweeping shine for levels 81..100. Set false to keep the badge fully static. */
    var shimmerEnabled: Boolean = true
        set(value) { field = value; updateAnimator(); invalidate() }

    /** 1..100 (values outside are clamped). */
    var level: Int
        get() = levelValue
        set(value) {
            levelValue = value.coerceIn(1, 100)
            tierIdx = (levelValue - 1) / 10
            sub = (levelValue - 1) % 10
            contentDescription = "Level $levelValue"
            measureTexts()
            requestLayout()
            updateAnimator()
            invalidate()
        }

    init {
        labelPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        labelPaint.textSize = 8f * d
        numPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        numPaint.textSize = 12f * d
        level = 1
    }

    private fun measureTexts() {
        labelW = labelPaint.measureText("LV")
        numW = numPaint.measureText(levelValue.toString())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = extraX + emblemR * 2f + textGap + labelW + labelToNum + numW + padEnd + extraX
        val h = emblemR * 2f + extraY * 2f
        setMeasuredDimension(
            resolveSize(ceil(w).toInt(), widthMeasureSpec),
            resolveSize(ceil(h).toInt(), heightMeasureSpec)
        )
    }

    // ------------------------------------------------------------------ animation
    private fun updateAnimator() {
        val shouldRun = shimmerEnabled && levelValue >= 81 && isAttachedToWindow && visibility == VISIBLE
        if (shouldRun) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1.7f).apply {
                    duration = 2800L
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        shimmerPhase = (it.animatedValue as Float).coerceAtMost(1f)
                        invalidate()
                    }
                    start()
                }
            }
        } else {
            animator?.cancel()
            animator = null
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimator() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility); updateAnimator()
    }

    // ------------------------------------------------------------------ drawing
    private fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    private fun buildGlyph(ops: FloatArray, cx: Float, cy: Float, s: Float, out: Path) {
        out.reset()
        var i = 0
        while (i < ops.size) {
            when (ops[i].toInt()) {
                0 -> { out.moveTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s); i += 3 }
                1 -> { out.lineTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s); i += 3 }
                2 -> {
                    out.quadTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s, cx + ops[i + 3] * s, cy + ops[i + 4] * s)
                    i += 5
                }
                else -> { out.close(); i += 1 }
            }
        }
    }

    private fun drawSparkle(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        buildGlyph(G_SPARKLE, x, y, r, glyphPath)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        c.drawPath(glyphPath, paint)
    }

    override fun onDraw(canvas: Canvas) {
        val t = TIERS[tierIdx]
        val cx = extraX + emblemR
        val cy = height / 2f
        val pillL = cx
        val pillR = width - extraX
        val pillT = cy - pillH / 2f
        val pillB = cy + pillH / 2f
        val rad = pillH / 2f
        val isRoyal = tierIdx == 9

        // 1) soft glow (no blur filters -> works with hardware acceleration on every API)
        val glowLayers = when {
            sub >= 8 -> 3
            sub >= 5 -> 2
            isRoyal -> 2
            else -> 0
        }
        paint.shader = null
        paint.style = Paint.Style.STROKE
        for (k in glowLayers downTo 1) {
            paint.strokeWidth = k * 1.7f * d
            paint.color = withAlpha(t.glow, 95 / k)
            val e = k * 0.85f * d
            rect.set(pillL - e, pillT - e, pillR + e, pillB + e)
            canvas.drawRoundRect(rect, rad + e, rad + e, paint)
        }

        // 2) sun-burst rays behind the emblem (tier 10 only)
        if (t.shape == 6) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f * d
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(t.glow, 210)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                val r1 = emblemR + 0.6f * d
                val r2 = emblemR + (if (i % 2 == 0) 3.0f else 2.0f) * d
                canvas.drawLine(
                    cx + (cos(a) * r1).toFloat(), cy + (sin(a) * r1).toFloat(),
                    cx + (cos(a) * r2).toFloat(), cy + (sin(a) * r2).toFloat(), paint
                )
            }
            paint.strokeCap = Paint.Cap.BUTT
        }

        // 3) pill body
        rect.set(pillL, pillT, pillR, pillB)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(pillL, 0f, pillR, 0f, t.pillA, t.pillB, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, rad, rad, paint)
        // depth: darker bottom edge
        paint.shader = LinearGradient(0f, pillT, 0f, pillB, 0x00000000, 0x30000000, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, rad, rad, paint)

        // gloss + shimmer, clipped to the pill
        canvas.save()
        path.reset()
        path.addRoundRect(rect, rad, rad, Path.Direction.CW)
        canvas.clipPath(path)
        paint.shader = LinearGradient(0f, pillT, 0f, cy, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(pillL, pillT, pillR, cy, paint)
        if (shimmerEnabled && levelValue >= 81) {
            val bw = 14f * d
            val sx = pillL - bw + (pillR - pillL + 2f * bw) * shimmerPhase
            paint.shader = LinearGradient(
                sx - bw, pillT, sx + bw, pillB,
                intArrayOf(0x00FFFFFF, 0x77FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawRect(pillL, pillT, pillR, pillB, paint)
        }
        canvas.restore()

        // rim
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (if (sub >= 3) 1.5f else 1.0f) * d
        paint.color = t.stroke
        rect.set(pillL + paint.strokeWidth / 2f, pillT + paint.strokeWidth / 2f, pillR - paint.strokeWidth / 2f, pillB - paint.strokeWidth / 2f)
        canvas.drawRoundRect(rect, rad, rad, paint)

        // 4) text "LV" + number
        val baseY = cy - (numPaint.fontMetrics.ascent + numPaint.fontMetrics.descent) / 2f
        val tx = cx + emblemR + textGap
        val nx = tx + labelW + labelToNum
        val sdy = 0.6f * d
        labelPaint.color = t.textShadow; canvas.drawText("LV", tx, baseY + sdy, labelPaint)
        numPaint.shader = null
        numPaint.color = t.textShadow; canvas.drawText(levelValue.toString(), nx, baseY + sdy, numPaint)
        labelPaint.color = withAlpha(t.text, 215); canvas.drawText("LV", tx, baseY, labelPaint)
        if (isRoyal) {
            numPaint.shader = LinearGradient(0f, cy - 6f * d, 0f, cy + 6f * d, 0xFFFFF3C0.toInt(), 0xFFFFC94D.toInt(), Shader.TileMode.CLAMP)
        } else {
            numPaint.color = t.text
        }
        canvas.drawText(levelValue.toString(), nx, baseY, numPaint)
        numPaint.shader = null

        // 5) emblem: metallic ring + inner disc + glyph
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(cx - emblemR, cy - emblemR, cx + emblemR, cy + emblemR, t.ringA, t.ringB, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, emblemR, paint)
        val innerR = emblemR - 2f * d
        paint.shader = LinearGradient(0f, cy - emblemR, 0f, cy + emblemR, t.innerA, t.innerB, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, innerR, paint)
        canvas.save()
        path.reset()
        path.addCircle(cx, cy, innerR, Path.Direction.CW)
        canvas.clipPath(path)
        paint.shader = LinearGradient(0f, cy - emblemR, 0f, cy, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(cx - emblemR, cy - emblemR, cx + emblemR, cy, paint)
        canvas.restore()
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f * d
        paint.color = 0x66FFFFFF
        canvas.drawCircle(cx, cy, emblemR - 0.4f * d, paint)

        val ops: FloatArray
        val gs: Float
        when (t.shape) {
            0 -> { ops = G_STAR; gs = 0.58f * emblemR }
            1, 5 -> { ops = G_GEM; gs = 0.50f * emblemR }
            2 -> { ops = G_SHIELD; gs = 0.50f * emblemR }
            3 -> { ops = G_BOLT; gs = 0.52f * emblemR }
            else -> { ops = G_CROWN; gs = 0.52f * emblemR }
        }
        buildGlyph(ops, cx, cy, gs, glyphPath)
        paint.style = Paint.Style.FILL
        paint.color = t.glyph
        canvas.drawPath(glyphPath, paint)
        if (t.shape == 1 || t.shape == 5) {
            buildGlyph(G_GEM_FACETS, cx, cy, gs, glyphPath)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.7f * d
            paint.color = withAlpha(t.innerA, 170)
            canvas.drawPath(glyphPath, paint)
        }

        // 6) sparkles
        if (t.shape == 5) drawSparkle(canvas, cx + emblemR * 0.45f, cy - emblemR * 0.45f, emblemR * 0.30f, 0xFFFFFFFF.toInt())
        if (sub == 9 || isRoyal) {
            drawSparkle(canvas, pillR - 7f * d, pillT + 1.5f * d, 3f * d, if (isRoyal) 0xFFFFF0B0.toInt() else 0xEBFFFFFF.toInt())
        }
        if (isRoyal) {
            drawSparkle(canvas, cx + emblemR * 0.75f, cy - emblemR * 0.85f, 2.4f * d, 0xFFFFF0B0.toInt())
            if (levelValue == 100) drawSparkle(canvas, cx - emblemR * 0.7f, cy + emblemR * 0.85f, 2f * d, 0xFFFFF0B0.toInt())
        }
    }

    companion object {
        private val TIERS = arrayOf(
        Tier(0xFF7C8BA1.toInt(), 0xFF46546B.toInt(), 0xFFB9C6D8.toInt(), 0xFFF1F5FA.toInt(), 0xFF8895AA.toInt(), 0xFF5B6980.toInt(), 0xFF39445A.toInt(), 0xFFF4F8FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF9FB2CC.toInt(), 0, 0x5A000000.toInt()), // Steel
        Tier(0xFF35D58A.toInt(), 0xFF0F7A55.toInt(), 0xFF8FF7C2.toInt(), 0xFFE9FFF3.toInt(), 0xFF2BAA78.toInt(), 0xFF14946A.toInt(), 0xFF0A5E44.toInt(), 0xFFEFFFF7.toInt(), 0xFFFFFFFF.toInt(), 0xFF3EE6A0.toInt(), 0, 0x5A000000.toInt()), // Emerald
        Tier(0xFF26E0D0.toInt(), 0xFF0A7F95.toInt(), 0xFF9BFBF2.toInt(), 0xFFEAFFFD.toInt(), 0xFF1BB3C1.toInt(), 0xFF0E9FB0.toInt(), 0xFF065F73.toInt(), 0xFFF0FFFE.toInt(), 0xFFFFFFFF.toInt(), 0xFF38F0E0.toInt(), 1, 0x5A000000.toInt()), // Aqua
        Tier(0xFF4C9AFF.toInt(), 0xFF1B3CC0.toInt(), 0xFFA9CCFF.toInt(), 0xFFEEF5FF.toInt(), 0xFF3E78E0.toInt(), 0xFF2D63D8.toInt(), 0xFF14288F.toInt(), 0xFFF2F7FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF5FA2FF.toInt(), 1, 0x5A000000.toInt()), // Sapphire
        Tier(0xFFA66BFF.toInt(), 0xFF5A1FB8.toInt(), 0xFFD6BAFF.toInt(), 0xFFF6EEFF.toInt(), 0xFF8B52E6.toInt(), 0xFF7A3FD6.toInt(), 0xFF431590.toInt(), 0xFFFAF4FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFB07BFF.toInt(), 2, 0x5A000000.toInt()), // Amethyst
        Tier(0xFFFF5CAD.toInt(), 0xFFB01270.toInt(), 0xFFFFBBDD.toInt(), 0xFFFFF0F8.toInt(), 0xFFE6469A.toInt(), 0xFFD12D88.toInt(), 0xFF85104F.toInt(), 0xFFFFF4FA.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF7BBF.toInt(), 2, 0x5A000000.toInt()), // Rose
        Tier(0xFFFF9A45.toInt(), 0xFFD4202B.toInt(), 0xFFFFCFA3.toInt(), 0xFFFFF3E8.toInt(), 0xFFF2722F.toInt(), 0xFFE8501F.toInt(), 0xFFA3141C.toInt(), 0xFFFFF6EC.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF8A4A.toInt(), 3, 0x5A000000.toInt()), // Flame
        Tier(0xFFFFE08A.toInt(), 0xFFC8870A.toInt(), 0xFFFFF3C2.toInt(), 0xFFFFFBE8.toInt(), 0xFFE9A91C.toInt(), 0xFFE3A317.toInt(), 0xFF9A6205.toInt(), 0xFFFFF9E0.toInt(), 0xFF4B2E00.toInt(), 0xFFFFD25C.toInt(), 4, 0x80FFFFFF.toInt()), // Gold
        Tier(0xFFF7FCFF.toInt(), 0xFF9CC2E8.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF8DB6E0.toInt(), 0xFF7FA9D8.toInt(), 0xFF4F78AE.toInt(), 0xFFFFFFFF.toInt(), 0xFF12304D.toInt(), 0xFFBFE3FF.toInt(), 5, 0x99FFFFFF.toInt()), // Platinum
        Tier(0xFF34175C.toInt(), 0xFF0C0615.toInt(), 0xFFFFD76A.toInt(), 0xFFFFF2B8.toInt(), 0xFFD9A62B.toInt(), 0xFF4A1E80.toInt(), 0xFF12071F.toInt(), 0xFFFFE17A.toInt(), 0xFFFFE28A.toInt(), 0xFFFFC94D.toInt(), 6, 0x99000000.toInt()), // Royal
        )
        private val G_STAR = floatArrayOf(0f, 0f, -1f, 1f, 0.247f, -0.34f, 1f, 0.951f, -0.309f, 1f, 0.399f, 0.13f, 1f, 0.588f, 0.809f, 1f, 0f, 0.42f, 1f, -0.588f, 0.809f, 1f, -0.399f, 0.13f, 1f, -0.951f, -0.309f, 1f, -0.247f, -0.34f, 3f)
        private val G_GEM = floatArrayOf(0f, -0.95f, -0.35f, 1f, -0.55f, -0.9f, 1f, 0.55f, -0.9f, 1f, 0.95f, -0.35f, 1f, 0f, 0.95f, 3f)
        private val G_GEM_FACETS = floatArrayOf(0f, -0.95f, -0.35f, 1f, 0.95f, -0.35f, 0f, -0.4f, -0.9f, 1f, -0.2f, -0.35f, 1f, 0f, 0.95f, 0f, 0.4f, -0.9f, 1f, 0.2f, -0.35f, 1f, 0f, 0.95f)
        private val G_SHIELD = floatArrayOf(0f, 0f, -1f, 1f, 0.85f, -0.65f, 1f, 0.85f, 0.1f, 2f, 0.85f, 0.65f, 0f, 1f, 2f, -0.85f, 0.65f, -0.85f, 0.1f, 1f, -0.85f, -0.65f, 3f)
        private val G_BOLT = floatArrayOf(0f, 0.25f, -1f, 1f, -0.6f, 0.15f, 1f, -0.05f, 0.15f, 1f, -0.3f, 1f, 1f, 0.65f, -0.2f, 1f, 0.05f, -0.2f, 3f)
        private val G_CROWN = floatArrayOf(0f, -0.95f, 0.75f, 1f, -0.95f, -0.55f, 1f, -0.45f, 0f, 1f, 0f, -0.85f, 1f, 0.45f, 0f, 1f, 0.95f, -0.55f, 1f, 0.95f, 0.75f, 3f)
        private val G_SPARKLE = floatArrayOf(0f, 0f, -1f, 2f, 0.12f, -0.12f, 1f, 0f, 2f, 0.12f, 0.12f, 0f, 1f, 2f, -0.12f, 0.12f, -1f, 0f, 2f, -0.12f, -0.12f, 0f, -1f, 3f)
    }
}
