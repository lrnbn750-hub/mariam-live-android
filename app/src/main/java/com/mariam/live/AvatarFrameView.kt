package com.mariam.live

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SweepGradient
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated avatar frame (pure Canvas, no images). Put the avatar ImageView inside it, centred.
 * Draws: pulsing soft glow, a slowly rotating metallic sweep ring (gold / emerald),
 * a fixed jewel at the top and three twinkling sparkles travelling with the ring.
 *
 * Set [frameEnabled] = false to hide the frame completely (no animation, no drawing).
 * UI only, no data/API logic.
 */
class AvatarFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val d = resources.displayMetrics.density
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    private var sweep: SweepGradient? = null
    private var t = 0f
    private var animator: ValueAnimator? = null

    /** Colors of the rotating ring. First and last color should match for a seamless loop. */
    var frameColors: IntArray = DEFAULT_COLORS
        set(value) {
            field = value
            rebuildShader()
            invalidate()
        }

    var frameEnabled: Boolean = true
        set(value) {
            field = value
            updateAnimator()
            invalidate()
        }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShader()
    }

    private fun rebuildShader() {
        if (width == 0 || height == 0) return
        sweep = SweepGradient(width / 2f, height / 2f, frameColors, null)
    }

    private fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!frameEnabled) return
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 7f * d
        if (r <= 0f) return
        val twoPi = (2.0 * PI).toFloat()
        val pulse = 0.5f + 0.5f * sin(t * twoPi * 2f)

        // 1) soft pulsing glow (stacked strokes, no blur filters -> safe on every API)
        for (k in 3 downTo 1) {
            glowPaint.strokeWidth = (3.4f + k * 2.6f) * d
            glowPaint.color = withAlpha(GLOW, ((14f + 22f * pulse) / k).toInt())
            canvas.drawCircle(cx, cy, r, glowPaint)
        }

        // 2) rotating metallic sweep ring
        ringPaint.strokeWidth = 3.4f * d
        ringPaint.shader = sweep
        canvas.save()
        canvas.rotate(t * 360f, cx, cy)
        canvas.drawCircle(cx, cy, r, ringPaint)
        canvas.restore()
        ringPaint.shader = null

        // 3) fine highlight hairlines for depth
        hairPaint.strokeWidth = 0.8f * d
        hairPaint.color = withAlpha(Color.WHITE, 90)
        canvas.drawCircle(cx, cy, r + 2.6f * d, hairPaint)
        hairPaint.color = withAlpha(Color.BLACK, 40)
        canvas.drawCircle(cx, cy, r - 2.2f * d, hairPaint)

        // 4) twinkling sparkles that travel with the ring
        for (i in 0 until 3) {
            val ang = Math.toRadians((t * 360f + i * 120f + 20f).toDouble())
            val sx = cx + (cos(ang) * r).toFloat()
            val sy = cy + (sin(ang) * r).toFloat()
            val tw = 0.5f + 0.5f * sin(twoPi * (t * 2f + i / 3f))
            drawSparkle(canvas, sx, sy, (1.8f + 2.2f * tw) * d, (110 + 145 * tw).toInt())
        }

        // 5) fixed jewel on top of the ring
        val jx = cx
        val jy = cy - r
        val jr = 5.2f * d
        path.reset()
        path.moveTo(jx, jy - jr)
        path.lineTo(jx + jr * 0.8f, jy)
        path.lineTo(jx, jy + jr)
        path.lineTo(jx - jr * 0.8f, jy)
        path.close()
        fillPaint.color = 0xFFFFD76A.toInt()
        canvas.drawPath(path, fillPaint)
        hairPaint.strokeWidth = 1f * d
        hairPaint.color = 0xFFFFFFFF.toInt()
        canvas.drawPath(path, hairPaint)
        fillPaint.color = withAlpha(Color.WHITE, 170)
        canvas.drawCircle(jx - jr * 0.15f, jy - jr * 0.25f, jr * 0.22f, fillPaint)
    }

    private fun drawSparkle(c: Canvas, x: Float, y: Float, s: Float, alpha: Int) {
        path.reset()
        path.moveTo(x, y - s)
        path.quadTo(x + s * 0.14f, y - s * 0.14f, x + s, y)
        path.quadTo(x + s * 0.14f, y + s * 0.14f, x, y + s)
        path.quadTo(x - s * 0.14f, y + s * 0.14f, x - s, y)
        path.quadTo(x - s * 0.14f, y - s * 0.14f, x, y - s)
        path.close()
        fillPaint.color = withAlpha(Color.WHITE, alpha)
        c.drawPath(path, fillPaint)
    }

    // ------------------------------------------------------------------ animation lifecycle
    private fun shouldAnimateFrame(): Boolean {
        if (!frameEnabled || !isAttachedToWindow || visibility != VISIBLE || windowVisibility != VISIBLE) return false
        return if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled() else true
    }

    private fun updateAnimator() {
        if (shouldAnimateFrame()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 7000L
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        t = it.animatedFraction
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
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateAnimator() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility); updateAnimator()
    }

    companion object {
        private val GLOW = 0xFFFFC94D.toInt()
        private val DEFAULT_COLORS = intArrayOf(
            0xFFFFD76A.toInt(), 0xFFFFF4C9.toInt(), 0xFFFFC94D.toInt(),
            0xFF28BE91.toInt(), 0xFF35E0B4.toInt(), 0xFFFFD76A.toInt()
        )
    }
}
