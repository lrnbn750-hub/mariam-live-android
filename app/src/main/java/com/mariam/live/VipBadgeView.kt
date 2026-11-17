package com.mariam.live

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout

/** هل حركات النظام مفعّلة؟ (يعادل prefers-reduced-motion). */
internal fun Context.animationsEnabled(): Boolean =
    try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    } catch (e: Exception) {
        true
    }

/**
 * شارة VIP (viewBox 240x272) — نفس هندسة buildBadgeSVG(n) في النسخة الأصلية:
 * أشعة، أجنحة (1 + (n-1)/3)، إكليل غار (من 8)، تاج (3 أشكال)، درع بثلاث طبقات،
 * جوهرة، شريط "VIP n"، ولمعات متلألئة (3 + n/2) بتأخير 0.45 ثانية بين كل لمعة.
 */
internal class VipBadgeView(context: Context) : View(context) {
    private var level = 1
    private var cache: Bitmap? = null
    private var sparkles: List<Sparkle> = emptyList()
    private var animator: ValueAnimator? = null
    private var startNs = 0L
    private val motion = context.animationsEnabled()
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private class Sparkle(val path: Path, val color: Int, val delaySec: Float)

    fun setLevel(n: Int) {
        val l = n.coerceIn(1, VipConfig.MAX_LEVEL)
        if (l == level && cache != null) return
        level = l
        contentDescription = "شارة VIP $l"
        rebuild()
        invalidate()
    }

    /** يعادل keyframes pop (أول ظهور) وwb.animate (عند تبديل المستوى). */
    fun popIn(initial: Boolean) {
        animate().cancel()
        if (initial) {
            alpha = 0f; translationY = context.dpf(10f); scaleX = 0.88f; scaleY = 0.88f
            animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(550)
                .setInterpolator(PathInterpolator(0.2f, 0.8f, 0.2f, 1f)).start()
        } else {
            alpha = 0.72f; translationY = context.dpf(5f); scaleX = 0.97f; scaleY = 0.97f
            animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(190)
                .setInterpolator(PathInterpolator(0.2f, 0.75f, 0.25f, 1f)).start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (motion) {
            startNs = System.nanoTime()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2600
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { invalidate() }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = cache ?: return
        canvas.drawBitmap(bmp, 0f, 0f, null)
        canvas.save()
        canvas.scale(width / 240f, height / 272f)
        val elapsed = (System.nanoTime() - startNs) / 1_000_000_000f
        for (s in sparkles) {
            var a = 1f
            if (motion) {
                var t = (elapsed - s.delaySec) / 2.6f
                t -= Math.floor(t.toDouble()).toFloat()
                // 0% و100% => .15 ، 50% => 1 (ease-in-out)
                a = 0.15f + 0.85f * (0.5f - 0.5f * Math.cos(2.0 * Math.PI * t).toFloat())
            }
            sparkPaint.color = withAlpha(s.color, a)
            canvas.drawPath(s.path, sparkPaint)
        }
        canvas.restore()
    }

    private fun rebuild() {
        cache?.recycle()
        cache = null
        buildSparkles()
        if (width <= 0 || height <= 0) return
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(width / 240f, height / 272f)
        drawStatic(c, level)
        cache = bmp
    }

    private fun buildSparkles() {
        val n = level
        val t = VIP_TIERS[n - 1]
        val hi = shade(t.metal, .62f)
        val gHi = shade(t.gem, .6f)
        val pos = arrayOf(
            intArrayOf(44, 70), intArrayOf(198, 62), intArrayOf(36, 170), intArrayOf(206, 170),
            intArrayOf(62, 36), intArrayOf(180, 36), intArrayOf(28, 120), intArrayOf(212, 118), intArrayOf(120, 12)
        )
        val count = Math.min(pos.size, 3 + n / 2)
        sparkles = (0 until count).map { i ->
            val x = pos[i][0]; val y = pos[i][1]
            Sparkle(
                SvgPath.parse("M$x ${y - 5}l1.4 3.6 3.6 1.4-3.6 1.4-1.4 3.6-1.4-3.6-3.6-1.4 3.6-1.4z"),
                if (i % 2 == 1) gHi else hi,
                i * 0.45f
            )
        }
    }

    /* ------------------------------------------------------------------ */

    private fun bnd(p: Path): RectF = RectF().also { p.computeBounds(it, true) }

    private fun lin(b: RectF, fx1: Float, fy1: Float, fx2: Float, fy2: Float, pos: FloatArray, cols: IntArray): Shader =
        LinearGradient(
            b.left + fx1 * b.width(), b.top + fy1 * b.height(),
            b.left + fx2 * b.width(), b.top + fy2 * b.height(),
            cols, pos, Shader.TileMode.CLAMP
        )

    private fun drawStatic(c: Canvas, n: Int) {
        val tier = VIP_TIERS[n - 1]
        val metal = tier.metal; val gem = tier.gem; val rib = tier.ribbon
        val hi = shade(metal, .62f); val lo = shade(metal, -.4f); val dk = shade(metal, -.68f)
        val gHi = shade(gem, .6f); val gLo = shade(gem, -.5f)
        val rHi = shade(rib, .35f); val rLo = shade(rib, -.45f)
        val red = n >= 7
        val wings = 1 + (n - 1) / 3
        val f1 = if (red) shade(gem, -.8f) else Color.rgb(0x23, 0x2C, 0x40)
        val f2 = if (red) shade(gem, -.93f) else Color.rgb(0x0E, 0x13, 0x1F)

        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }

        fun mg(b: RectF) = lin(b, 0f, 0f, 0f, 1f, floatArrayOf(0f, .35f, .7f, 1f), intArrayOf(hi, metal, lo, dk))
        fun ms(b: RectF) = lin(b, 0f, 0f, 1f, 0f, floatArrayOf(0f, .25f, .5f, .75f, 1f), intArrayOf(lo, hi, metal, hi, lo))
        fun gg(b: RectF): Shader =
            if (n == 12) lin(
                b, 0f, 0f, 1f, 1f, floatArrayOf(0f, .35f, .7f, 1f),
                intArrayOf(Color.rgb(0xFF, 0x6A, 0xD5), Color.rgb(0x7E, 0xE0, 0xFF), Color.rgb(0x8D, 0xFF, 0x9E), Color.rgb(0xFF, 0xD3, 0x6A))
            ) else lin(b, 0f, 0f, 1f, 1f, floatArrayOf(0f, .5f, 1f), intArrayOf(gHi, gem, gLo))

        fun fillPath(p: Path, sh: Shader?, color: Int = Color.WHITE, alpha: Float = 1f) {
            fp.shader = sh
            fp.color = if (sh == null) withAlpha(color, alpha) else Color.argb(Math.round(alpha * 255f), 255, 255, 255)
            c.drawPath(p, fp)
            fp.shader = null
        }

        fun strokePath(p: Path, color: Int, w: Float, alpha: Float = 1f, sh: Shader? = null) {
            sp.shader = sh
            sp.strokeWidth = w
            sp.color = if (sh == null) withAlpha(color, alpha) else Color.argb(Math.round(alpha * 255f), 255, 255, 255)
            c.drawPath(p, sp)
            sp.shader = null
        }

        // ---- الأشعة (من VIP4) ----
        if (n >= 4) {
            val cnt = 6 + n
            val ray = SvgPath.parse("M120 30L126 138L114 138Z")
            fp.shader = RadialGradient(
                120f, 138f, 108f,
                intArrayOf(withAlpha(hi, .9f), withAlpha(hi, 0f)), null, Shader.TileMode.CLAMP
            )
            fp.color = Color.WHITE
            for (k in 0 until cnt) {
                c.save(); c.rotate(360f / cnt * k, 120f, 138f); c.drawPath(ray, fp); c.restore()
            }
            fp.shader = null
        }

        // ---- الأجنحة (يمين ثم نسخة معكوسة) ----
        fun drawWings() {
            for (i in wings - 1 downTo 0) {
                val tx = 182f + i * 10; val ty = 84f - i * 15; val by = 172f - i * 9
                val w1 = Path().apply {
                    moveTo(128f, by)
                    cubicTo(tx + 18, by - 4, tx + 16, ty + 54, tx, ty)
                    cubicTo(tx - 14, ty + 30, 150f, by - 60, 128f, by - 54)
                    close()
                }
                val op = 1f - i * .06f
                fillPath(w1, mg(bnd(w1)), alpha = op)
                strokePath(w1, dk, 1.3f, alpha = op)
                val w2 = Path().apply {
                    moveTo(132f, by - 8)
                    cubicTo(tx + 4, by - 16, tx, ty + 56, tx - 2, ty + 14)
                }
                strokePath(w2, hi, 1f, alpha = .7f)
            }
        }
        drawWings()
        c.save(); c.translate(240f, 0f); c.scale(-1f, 1f); drawWings(); c.restore()

        // ---- إكليل الغار (من VIP8) ----
        if (n >= 8) {
            val oval = RectF()
            for (side in intArrayOf(1, -1)) {
                for (k in 0 until 7) {
                    val a = Math.toRadians((100 + k * 11).toDouble())
                    val px = (120 + 72 * Math.cos(a)).toFloat()
                    val py = (146 + 72 * Math.sin(a)).toFloat()
                    val ang = (100 + k * 11 + 90).toFloat()
                    val x = if (side == 1) px else 240f - px
                    val r = if (side == 1) ang else -ang
                    oval.set(x - 4.2f, py - 10f, x + 4.2f, py + 10f)
                    c.save(); c.rotate(r, x, py)
                    fp.shader = mg(oval); fp.color = Color.WHITE
                    c.drawOval(oval, fp); fp.shader = null
                    sp.strokeWidth = .8f; sp.color = dk
                    c.drawOval(oval, sp)
                    c.restore()
                }
            }
        }

        // ---- التاج ----
        fun gemDot(x: Float, y: Float, r: Float) {
            val b = RectF(x - r, y - r, x + r, y + r)
            fp.shader = gg(b); fp.color = Color.WHITE
            c.drawCircle(x, y, r, fp); fp.shader = null
            sp.strokeWidth = .8f; sp.color = dk
            c.drawCircle(x, y, r, sp)
        }
        if (n >= 7) {
            val cr = SvgPath.parse("M90 54L88 26L102 41L110 20L120 37L130 20L138 41L152 26L150 54Z")
            fillPath(cr, mg(bnd(cr))); strokePath(cr, dk, 1.3f)
            strokePath(SvgPath.parse("M92 54H148"), hi, 2f)
            gemDot(88f, 26f, 3.4f); gemDot(152f, 26f, 3.4f)
            gemDot(110f, 20f, 3.4f); gemDot(130f, 20f, 3.4f)
            gemDot(120f, 37f, 4f)
        } else if (n >= 4) {
            val cr = SvgPath.parse("M98 54L96 32L108 44L120 26L132 44L144 32L142 54Z")
            fillPath(cr, mg(bnd(cr))); strokePath(cr, dk, 1.3f)
            gemDot(120f, 30f, 3.4f)
        } else {
            val cr = SvgPath.parse("M104 54L120 28L136 54Z")
            fillPath(cr, mg(bnd(cr))); strokePath(cr, dk, 1.3f)
        }

        // ---- الدرع (3 طبقات: 1 / .9 / .8) ----
        val shield = SvgPath.parse("M120 46L178 70L178 144C178 182 150 208 120 234C90 208 62 182 62 144L62 70Z")
        val sb = bnd(shield)
        fillPath(shield, ms(sb)); strokePath(shield, dk, 1.8f)
        c.save(); c.scale(.9f, .9f, 120f, 140f)
        fillPath(shield, mg(sb)); strokePath(shield, dk, 1f)
        c.restore()
        c.save(); c.scale(.8f, .8f, 120f, 140f)
        fillPath(shield, lin(sb, 0f, 0f, 0f, 1f, floatArrayOf(0f, 1f), intArrayOf(f1, f2)))
        strokePath(shield, hi, 1.2f)
        c.restore()

        // انعكاس الضوء
        sp.strokeCap = Paint.Cap.ROUND
        strokePath(SvgPath.parse("M120 70C146 70 160 84 160 106"), Color.WHITE, 3f, alpha = .12f)
        sp.strokeCap = Paint.Cap.BUTT

        // المسامير
        for (p in arrayOf(intArrayOf(72, 84), intArrayOf(168, 84), intArrayOf(120, 60), intArrayOf(66, 118), intArrayOf(174, 118))) {
            fp.color = hi; c.drawCircle(p[0].toFloat(), p[1].toFloat(), 2.4f, fp)
            sp.strokeWidth = .6f; sp.color = dk; c.drawCircle(p[0].toFloat(), p[1].toFloat(), 2.4f, sp)
        }

        // الوهج
        fp.shader = RadialGradient(
            120f, 130f, 48f, intArrayOf(withAlpha(gem, .8f), withAlpha(gem, 0f)), null, Shader.TileMode.CLAMP
        )
        c.drawCircle(120f, 130f, 48f, fp); fp.shader = null

        // الجوهرة
        val gemP = SvgPath.parse("M120 88L152 130L120 172L88 130Z")
        fillPath(gemP, gg(bnd(gemP))); strokePath(gemP, hi, 1.5f)
        fillPath(SvgPath.parse("M120 88L136 130L120 172Z"), null, Color.BLACK, .16f)
        strokePath(SvgPath.parse("M88 130H152M120 88L104 130L120 172L136 130Z"), Color.WHITE, 1f, alpha = .5f)
        fillPath(SvgPath.parse("M104 110L116 98L112 120Z"), null, Color.WHITE, .7f)

        // الجوهرة السفلية
        val gem2 = SvgPath.parse("M120 232L130 243L120 262L110 243Z")
        fillPath(gem2, gg(bnd(gem2))); strokePath(gem2, dk, 1.2f)

        // ذيلا الشريط
        val tailL = SvgPath.parse("M54 190L70 182L70 212L54 212L63 201Z")
        fillPath(tailL, null, rLo); strokePath(tailL, hi, 1f)
        val tailR = SvgPath.parse("M186 190L170 182L170 212L186 212L177 201Z")
        fillPath(tailR, null, rLo); strokePath(tailR, hi, 1f)

        // الشريط
        val band = SvgPath.parse("M66 178L174 178L174 208L66 208Z")
        val bb = bnd(band)
        fillPath(band, lin(bb, 0f, 0f, 0f, 1f, floatArrayOf(0f, .5f, 1f), intArrayOf(rHi, rib, rLo)))
        strokePath(band, Color.WHITE, 2f, sh = ms(bb))
        strokePath(SvgPath.parse("M71 183H169M71 203H169"), hi, .8f, alpha = .55f)

        // نص "VIP n" (الحد أولاً ثم التعبئة = paint-order: stroke)
        val label = "VIP $n"
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 20f
            letterSpacing = 1.5f / 20f
            textAlign = Paint.Align.CENTER
        }
        val tb = Rect()
        tp.getTextBounds(label, 0, label.length, tb)
        tp.style = Paint.Style.STROKE; tp.strokeWidth = .6f; tp.color = rLo
        c.drawText(label, 120f, 199.5f, tp)
        tp.style = Paint.Style.FILL
        tp.shader = LinearGradient(
            0f, 199.5f + tb.top, 0f, 199.5f + tb.bottom,
            Color.rgb(0xFF, 0xFB, 0xE6), shade(metal, .2f), Shader.TileMode.CLAMP
        )
        c.drawText(label, 120f, 199.5f, tp)
    }
}

/**
 * قسم البطل: خلفية متدرجة + شريطان مائلان + توهج بلون المستوى (ينتقل خلال 0.5 ثانية)
 * + الشارة في الأعلى. السحب الأفقي لتبديل المستوى يُربط من الخارج.
 */
internal class VipHeroView(context: Context) : FrameLayout(context) {
    val badge = VipBadgeView(context)
    private var tierColor = VIP_TIERS[0].gem
    private var tierAnim: ValueAnimator? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stripePaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stripePaint2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowMatrix = Matrix()

    init {
        setWillNotDraw(false)
        addView(
            badge,
            FrameLayout.LayoutParams(context.dp(150f), context.dp(170f), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        )
    }

    fun setTier(color: Int, animate: Boolean) {
        tierAnim?.cancel()
        if (!animate || !context.animationsEnabled()) {
            tierColor = color
            rebuildGlow(); invalidate()
            return
        }
        tierAnim = ValueAnimator.ofObject(ArgbEvaluator(), tierColor, color).apply {
            duration = 500
            addUpdateListener { tierColor = it.animatedValue as Int; rebuildGlow(); invalidate() }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        basePaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), VipColors.INK, VipColors.HERO_END, Shader.TileMode.CLAMP)
        stripePaint1.shader = cssLinear(w, h, 118f)
        stripePaint2.shader = cssLinear(w, h, 242f)
        rebuildGlow()
    }

    /** linear-gradient(deg, transparent 0 34%, #ffffff07 34% 52%, transparent 52%) */
    private fun cssLinear(w: Int, h: Int, deg: Float): Shader {
        val a = Math.toRadians(deg.toDouble())
        val dx = Math.sin(a).toFloat(); val dy = (-Math.cos(a)).toFloat()
        val len = Math.abs(w * dx) + Math.abs(h * dy)
        val cx = w / 2f; val cy = h / 2f
        val none = Color.argb(0, 255, 255, 255)
        val band = Color.argb(7, 255, 255, 255)
        return LinearGradient(
            cx - dx * len / 2, cy - dy * len / 2, cx + dx * len / 2, cy + dy * len / 2,
            intArrayOf(none, none, band, band, none, none),
            floatArrayOf(0f, .34f, .34f, .52f, .52f, 1f), Shader.TileMode.CLAMP
        )
    }

    /** radial-gradient(closest-side at 50% 52%, tier 42% , transparent 78%) */
    private fun rebuildGlow() {
        if (width <= 0 || height <= 0) return
        val cx = width / 2f
        val cy = height * 0.52f
        val rx = width / 2f
        val ry = Math.min(height * 0.52f, height * 0.48f)
        val sh = RadialGradient(
            cx, cy, rx,
            intArrayOf(withAlpha(tierColor, .42f), withAlpha(tierColor, 0f)),
            floatArrayOf(0f, .78f), Shader.TileMode.CLAMP
        )
        glowMatrix.setScale(1f, ry / rx, cx, cy)
        sh.setLocalMatrix(glowMatrix)
        glowPaint.shader = sh
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, basePaint)
        canvas.drawRect(0f, 0f, w, h, stripePaint2)
        canvas.drawRect(0f, 0f, w, h, stripePaint1)
        canvas.drawRect(0f, 0f, w, h, glowPaint)
    }
}