package com.mariam.live

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/* ═════════════════════════ Shared glyphs ═════════════════════════ */

/** Armchair outline in a 24x24 box (paint must be STROKE). */
private fun drawSeatOutline(c: Canvas, p: Paint) {
    c.drawPath(Path().apply {
        moveTo(5.5f, 11.5f); lineTo(5.5f, 8f); quadTo(5.5f, 4.5f, 9f, 4.5f)
        lineTo(15f, 4.5f); quadTo(18.5f, 4.5f, 18.5f, 8f); lineTo(18.5f, 11.5f)
    }, p)
    c.drawRoundRect(RectF(2.5f, 10.5f, 6.5f, 18.5f), 2f, 2f, p)
    c.drawRoundRect(RectF(17.5f, 10.5f, 21.5f, 18.5f), 2f, 2f, p)
    c.drawRoundRect(RectF(6.5f, 13f, 17.5f, 18.5f), 1.5f, 1.5f, p)
    c.drawLine(4.5f, 18.5f, 4.5f, 20.8f, p)
    c.drawLine(19.5f, 18.5f, 19.5f, 20.8f, p)
}

private fun sparkle(c: Canvas, p: Paint, x: Float, y: Float, r: Float) {
    val path = Path()
    path.moveTo(x, y - r); path.quadTo(x, y, x + r, y)
    path.quadTo(x, y, x, y + r); path.quadTo(x, y, x - r, y)
    path.quadTo(x, y, x, y - r)
    val old = p.style
    p.style = Paint.Style.FILL
    c.drawPath(path, p)
    p.style = old
}

/* ═════════════════════════ Line icons (toolbar / close / announce) ═════════════════════════ */

private class SetupIconView(
    ctx: Context,
    private val kind: String,
    private val tint: Int,
    private val stroke: Float = 1.8f
) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        c.scale(s / 24f, s / 24f)
        p.color = tint
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        when (kind) {
            "close" -> {
                c.drawLine(5.5f, 5.5f, 18.5f, 18.5f, p)
                c.drawLine(18.5f, 5.5f, 5.5f, 18.5f, p)
            }
            "seat" -> drawSeatOutline(c, p)
            "shirt" -> c.drawPath(Path().apply {
                moveTo(8.5f, 3.5f); lineTo(3f, 6.5f); lineTo(4.5f, 10.5f); lineTo(7f, 9.5f)
                lineTo(7f, 20.5f); lineTo(17f, 20.5f); lineTo(17f, 9.5f); lineTo(19.5f, 10.5f)
                lineTo(21f, 6.5f); lineTo(15.5f, 3.5f); quadTo(12f, 7f, 8.5f, 3.5f); close()
            }, p)
            "beauty" -> {
                c.drawPath(Path().apply {
                    moveTo(12f, 2.8f); lineTo(19.8f, 7.3f); lineTo(19.8f, 16.7f)
                    lineTo(12f, 21.2f); lineTo(4.2f, 16.7f); lineTo(4.2f, 7.3f); close()
                }, p)
                c.drawCircle(11.5f, 12.5f, 3f, p)
                sparkle(c, p, 18f, 4.5f, 3.2f)
            }
            "magic" -> {
                p.strokeWidth = stroke + 0.8f
                c.drawLine(3.8f, 20.2f, 14f, 10f, p)
                p.strokeWidth = stroke
                sparkle(c, p, 17.5f, 6.5f, 4.2f)
                sparkle(c, p, 20.5f, 14.5f, 2.1f)
                sparkle(c, p, 8.5f, 5.5f, 2.3f)
            }
            "settings" -> {
                val path = Path()
                val n = 8
                var first = true
                for (i in 0 until n) {
                    val a = i * 2.0 * Math.PI / n
                    val h = Math.PI / n
                    val pts = arrayOf(
                        doubleArrayOf(a - h * 0.70, 7.6), doubleArrayOf(a - h * 0.38, 10.0),
                        doubleArrayOf(a + h * 0.38, 10.0), doubleArrayOf(a + h * 0.70, 7.6)
                    )
                    for (pt in pts) {
                        val x = (12.0 + pt[1] * cos(pt[0])).toFloat()
                        val y = (12.0 + pt[1] * sin(pt[0])).toFloat()
                        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                    }
                }
                path.close()
                c.drawPath(path, p)
                c.drawCircle(12f, 12f, 3.2f, p)
            }
            "mixer" -> {
                c.drawLine(5f, 3f, 5f, 21f, p)
                c.drawLine(12f, 3f, 12f, 21f, p)
                c.drawLine(19f, 3f, 19f, 21f, p)
                p.strokeWidth = stroke + 1.6f
                c.drawLine(2.8f, 14.5f, 7.2f, 14.5f, p)
                c.drawLine(9.8f, 8f, 14.2f, 8f, p)
                c.drawLine(16.8f, 16.5f, 21.2f, 16.5f, p)
            }
            "creator" -> {
                c.drawRoundRect(RectF(3f, 3.5f, 21f, 17.5f), 2.8f, 2.8f, p)
                c.drawPath(Path().apply {
                    moveTo(6.5f, 13.5f); lineTo(10f, 10f); lineTo(13f, 12.5f); lineTo(17.5f, 7.5f)
                }, p)
                c.drawLine(12f, 17.5f, 12f, 20.5f, p)
                c.drawLine(8f, 20.5f, 16f, 20.5f, p)
            }
            "megaphone" -> {
                c.drawLine(2.5f, 8.5f, 5.5f, 8.5f, p)
                c.drawLine(2.5f, 12f, 5.5f, 12f, p)
                c.drawLine(2.5f, 15.5f, 5.5f, 15.5f, p)
                c.drawPath(Path().apply {
                    moveTo(9f, 4.5f); lineTo(21f, 12f); lineTo(9f, 19.5f); close()
                }, p)
            }
        }
        c.restore()
    }
}

/* ═════════════════════════ Category icons (colourful, 48x48 space) ═════════════════════════ */

private class CategoryIconView(ctx: Context, private val kind: String) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun col(s: String) = Color.parseColor(s)

    private fun fillPaint(color: Int) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color
    }

    private fun gradPaint(y0: Float, y1: Float, a: String, b: String) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, y0, 0f, y1, col(a), col(b), Shader.TileMode.CLAMP)
    }

    private fun strokePaint(color: Int, w: Float) {
        p.shader = null; p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        c.scale(s / 48f, s / 48f)
        when (kind) {
            "chat" -> {
                gradPaint(6f, 38f, "#58F2DC", "#12B5AE")
                c.drawRoundRect(RectF(4f, 6f, 44f, 36f), 15f, 15f, p)
                c.drawPath(Path().apply { moveTo(12f, 33f); lineTo(9f, 44f); lineTo(23f, 35f); close() }, p)
                fillPaint(Color.WHITE)
                c.drawCircle(17.5f, 19.5f, 2.6f, p)
                c.drawCircle(30.5f, 19.5f, 2.6f, p)
                strokePaint(Color.WHITE, 3f)
                c.drawArc(RectF(16f, 14f, 32f, 29f), 25f, 130f, false, p)
            }
            "dating" -> {
                gradPaint(16f, 44f, "#AD91FF", "#6A49EE")
                c.drawCircle(19f, 30f, 14f, p)
                fillPaint(col("#2B145E"))
                c.drawCircle(14.5f, 28f, 2.1f, p)
                c.drawCircle(23.5f, 28f, 2.1f, p)
                strokePaint(col("#2B145E"), 2.4f)
                c.drawArc(RectF(13f, 27f, 25f, 37f), 20f, 140f, false, p)
                strokePaint(col("#FFD23F"), 2.2f)
                c.drawLine(26f, 18f, 29f, 13.5f, p)
                fillPaint(col("#FF9F1C"))
                c.drawCircle(30.5f, 11.5f, 2.2f, p)
                c.drawCircle(34f, 14f, 1.4f, p)
                gradPaint(8f, 30f, "#FF93CB", "#F2449E")
                c.drawCircle(34f, 19f, 11f, p)
                fillPaint(col("#5A1442"))
                c.drawCircle(30.8f, 17.5f, 1.9f, p)
                strokePaint(col("#5A1442"), 2.2f)
                c.drawLine(35.5f, 17.5f, 39f, 17.5f, p)
                c.drawArc(RectF(29.5f, 17f, 39.5f, 26f), 25f, 130f, false, p)
            }
            "games" -> {
                gradPaint(12f, 40f, "#AF92FF", "#6246E4")
                c.drawRoundRect(RectF(4f, 13f, 44f, 34f), 10f, 10f, p)
                c.drawCircle(11f, 32f, 7.5f, p)
                c.drawCircle(37f, 32f, 7.5f, p)
                fillPaint(Color.WHITE)
                c.drawRoundRect(RectF(12.6f, 19f, 15.4f, 29f), 1.4f, 1.4f, p)
                c.drawRoundRect(RectF(9f, 22.6f, 19f, 25.4f), 1.4f, 1.4f, p)
                fillPaint(col("#FF5C8A")); c.drawCircle(35f, 20.5f, 2.6f, p)
                fillPaint(col("#FFD23F")); c.drawCircle(40f, 24.5f, 2.6f, p)
                fillPaint(col("#4ADE80")); c.drawCircle(35f, 28.5f, 2.6f, p)
                fillPaint(col("#38BDF8")); c.drawCircle(30f, 24.5f, 2.6f, p)
            }
            "interests" -> {
                fillPaint(col("#5B7CFA"))
                c.drawPath(Path().apply {
                    moveTo(3f, 24f); lineTo(24f, 29f); lineTo(45f, 24f); lineTo(45f, 41f); lineTo(24f, 46f); lineTo(3f, 41f); close()
                }, p)
                fillPaint(col("#F7EDD2"))
                c.drawPath(Path().apply { moveTo(5f, 22f); lineTo(24f, 27f); lineTo(24f, 43.5f); lineTo(5f, 38.5f); close() }, p)
                fillPaint(col("#EBDDB5"))
                c.drawPath(Path().apply { moveTo(43f, 22f); lineTo(24f, 27f); lineTo(24f, 43.5f); lineTo(43f, 38.5f); close() }, p)
                strokePaint(col("#C9B98F"), 1.4f)
                c.drawLine(9f, 28f, 20f, 31f, p); c.drawLine(9f, 32f, 20f, 35f, p)
                c.drawLine(28f, 31f, 39f, 28f, p); c.drawLine(28f, 35f, 39f, 32f, p)
                val star = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) 10f else 4.6f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val x = 24f + r * cos(a).toFloat()
                    val y = 13f + r * sin(a).toFloat()
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                fillPaint(col("#FFC93C"))
                c.drawPath(star, p)
                strokePaint(col("#F59E0B"), 1.2f)
                c.drawPath(star, p)
            }
            "emotional" -> {
                val heart = Path().apply {
                    moveTo(24f, 42f)
                    cubicTo(6f, 30f, 3f, 15f, 13f, 10.5f)
                    cubicTo(18.5f, 8f, 23f, 11f, 24f, 15f)
                    cubicTo(25f, 11f, 29.5f, 8f, 35f, 10.5f)
                    cubicTo(45f, 15f, 42f, 30f, 24f, 42f)
                    close()
                }
                gradPaint(8f, 42f, "#FF84B4", "#FF3A7C")
                c.drawPath(heart, p)
                strokePaint(Color.WHITE, 2.6f)
                c.drawPath(Path().apply {
                    moveTo(9f, 24f); lineTo(18f, 24f); lineTo(21f, 18f); lineTo(25f, 31f); lineTo(28f, 24f); lineTo(39f, 24f)
                }, p)
            }
        }
        c.restore()
    }
}

/* ═════════════════════════ Backdrops ═════════════════════════ */

/** Multi-guest background: violet → indigo gradient. */
private class MultiBackdrop(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        p.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.parseColor("#8A5FD3"), Color.parseColor("#7B66CF"), Color.parseColor("#5B5CB5"),
                Color.parseColor("#3E429A"), Color.parseColor("#29286F")
            ),
            floatArrayOf(0f, 0.30f, 0.62f, 0.82f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(
            0f, h * 0.05f, w * 0.95f,
            intArrayOf(Color.argb(80, 160, 120, 235), Color.argb(0, 160, 120, 235)),
            null, Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }
}

/**
 * Voice-room background (vinyl, mug, photo on a wooden table).
 * Procedural fallback — if the app has a drawable called `bg_voice_room`, that image is used instead.
 */
private class VoiceBackdrop(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun col(s: String) = Color.parseColor(s)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val k = w / 720f
        fun yb(y: Float) = h - (1600f - y) * k

        fun bokeh(cx: Float, cy: Float, r: Float, a: Int) {
            p.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(Color.argb(a, 255, 190, 110), Color.argb(a / 2, 255, 170, 90), Color.argb(0, 255, 170, 90)),
                floatArrayOf(0f, 0.75f, 1f), Shader.TileMode.CLAMP
            )
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, r, p)
            p.shader = null
        }

        // base
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(col("#1D120B"), col("#3B2515"), col("#2A190E")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        bokeh(590f * k, 170f * k, 110f * k, 70)
        bokeh(170f * k, 120f * k, 80f * k, 45)
        bokeh(520f * k, 600f * k, 150f * k, 55)
        bokeh(40f * k, 420f * k, 70f * k, 40)
        bokeh(300f * k, 330f * k, 60f * k, 35)

        // wooden table
        val table = Path().apply {
            moveTo(0f, yb(905f)); lineTo(w, yb(800f)); lineTo(w, h); lineTo(0f, h); close()
        }
        p.shader = LinearGradient(0f, yb(800f), 0f, h, col("#A56F3B"), col("#5E3B1C"), Shader.TileMode.CLAMP)
        c.drawPath(table, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * k
        p.color = Color.argb(45, 0, 0, 0)
        for (i in 1..5) {
            c.drawLine(0f, yb(905f) + i * 150f * k, w, yb(800f) + i * 150f * k, p)
        }
        p.style = Paint.Style.FILL

        // turntable plinth
        p.shader = LinearGradient(0f, yb(1085f), 0f, yb(1445f), col("#DDD3C0"), col("#B3A892"), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(-60f * k, yb(1085f), 520f * k, yb(1445f)), 28f * k, 28f * k, p)
        p.shader = null
        p.color = col("#9C9382")
        c.drawCircle(492f * k, yb(1235f), 22f * k, p)
        p.color = col("#C9C1B2")
        c.drawCircle(492f * k, yb(1235f), 13f * k, p)

        // vinyl record
        val rx = 92f * k
        val ry = yb(1262f)
        p.color = col("#101010")
        c.drawCircle(rx, ry, 292f * k, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * k
        p.color = Color.argb(34, 255, 255, 255)
        var r = 150f
        while (r < 288f) { c.drawCircle(rx, ry, r * k, p); r += 17f }
        p.style = Paint.Style.FILL
        p.shader = SweepGradient(
            rx, ry,
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(46, 255, 255, 255), Color.argb(0, 255, 255, 255),
                Color.argb(46, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null
        )
        c.drawCircle(rx, ry, 290f * k, p)
        p.shader = null
        p.color = col("#E27A31")
        c.drawCircle(rx, ry, 135f * k, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * k
        p.color = col("#B65A1D")
        c.drawCircle(rx, ry, 118f * k, p)
        p.style = Paint.Style.FILL
        p.color = col("#CFC6B8")
        c.drawCircle(rx + 6f * k, ry - 6f * k, 9f * k, p)

        // mug
        val mugL = 6f * k
        val mugR = 216f * k
        p.shader = LinearGradient(
            mugL, 0f, mugR, 0f,
            intArrayOf(col("#A9A298"), col("#E9E3DA"), col("#CFC8BE"), col("#968F85")),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(RectF(mugL, yb(840f), mugR, yb(1090f)), 14f * k, 14f * k, p)
        p.shader = null
        p.color = col("#E7E1D8")
        c.drawOval(RectF(mugL, yb(815f), mugR, yb(885f)), p)
        p.shader = RadialGradient(
            (mugL + mugR) / 2f, yb(850f), 90f * k,
            col("#7B5436"), col("#5A3C26"), Shader.TileMode.CLAMP
        )
        c.drawOval(RectF(mugL + 12f * k, yb(822f), mugR - 12f * k, yb(878f)), p)
        p.shader = null
        p.color = Color.argb(150, 70, 70, 66)
        c.drawCircle(125f * k, yb(978f), 70f * k, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * k
        p.color = Color.argb(90, 255, 255, 255)
        c.drawCircle(125f * k, yb(978f), 58f * k, p)
        p.style = Paint.Style.FILL

        // photo print
        c.save()
        c.translate(585f * k, yb(1395f))
        c.rotate(-9f)
        p.color = col("#EDE6DA")
        c.drawRect(RectF(-190f * k, -170f * k, 190f * k, 170f * k), p)
        p.shader = LinearGradient(
            0f, -156f * k, 0f, 156f * k,
            intArrayOf(col("#F2B268"), col("#D9824A"), col("#6F4338"), col("#3A2A2C")),
            floatArrayOf(0f, 0.4f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(RectF(-176f * k, -156f * k, 176f * k, 156f * k), p)
        p.shader = null
        p.color = Color.argb(70, 255, 210, 150)
        c.drawRect(RectF(-176f * k, 34f * k, 176f * k, 42f * k), p)
        c.restore()

        // vignettes
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.30f, Color.argb(120, 0, 0, 0), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h * 0.30f, p)
        p.shader = LinearGradient(0f, h * 0.8f, 0f, h, Color.argb(0, 0, 0, 0), Color.argb(120, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, h * 0.8f, w, h, p)
        p.shader = null
    }
}

/* ═════════════════════════ Multi-guest grid (1 big + 5 small camera seats) ═════════════════════════ */

private class MultiGridView(ctx: Context) : View(ctx) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(80, 255, 255, 255)
    }
    private val seat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(110, 255, 255, 255)
        strokeWidth = 1.8f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        if (w <= 0f) return
        val k = w / 720f
        val cw = w / 3f
        val sw = max(1f, 1.6f * k)
        line.strokeWidth = sw
        val half = sw / 2f

        // outer border
        c.drawRect(half, half, w - half, w - half, line)
        // inner dividers
        c.drawLine(cw, 0f, cw, w, line)
        c.drawLine(2 * cw, 2 * cw, 2 * cw, w, line)
        c.drawLine(0f, cw, cw, cw, line)
        c.drawLine(0f, 2 * cw, w, 2 * cw, line)

        // seat icons in the 5 small cells
        val size = 43f * k
        val centers = arrayOf(
            floatArrayOf(0.5f, 0.5f), floatArrayOf(0.5f, 1.5f), floatArrayOf(0.5f, 2.5f),
            floatArrayOf(1.5f, 2.5f), floatArrayOf(2.5f, 2.5f)
        )
        for (ct in centers) {
            c.save()
            c.translate(ct[0] * cw - size / 2f, ct[1] * cw - size / 2f)
            c.scale(size / 24f, size / 24f)
            drawSeatOutline(c, seat)
            c.restore()
        }
    }
}

/* ═════════════════════════ Voice seats (host with fire ring + 8 seats) ═════════════════════════ */

private class VoiceSeatsView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val seatPath = Path()
    private val tmp = Path()
    private val flame = Path()
    private val m = Matrix()
    private val xs = floatArrayOf(78f, 219f, 359f, 500f, 641f)
    private var avatar: Bitmap? = null
    private var avatarShader: BitmapShader? = null
    private var ringShader: SweepGradient? = null
    private var ringW = 0
    private var phase = 0f
    private var anim: ValueAnimator? = null

    fun setAvatar(b: Bitmap) {
        avatar = b
        avatarShader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (390f * w / 720f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        if (w <= 0f) return
        val k = w / 720f
        val r1 = 110f * k
        val r2 = 292f * k
        for (i in 0 until 4) seat(c, xs[i] * k, r1, k)
        for (i in 1 until 5) seat(c, xs[i] * k, r2, k)
        host(c, xs[4] * k, r1 + 3f * k, k)
    }

    private fun seat(c: Canvas, cx: Float, cy: Float, k: Float) {
        val s = 94f * k
        seatPath.reset()
        seatPath.addRoundRect(RectF(cx - .34f * s, cy - .50f * s, cx + .34f * s, cy + .10f * s), .22f * s, .22f * s, Path.Direction.CW)
        tmp.reset()
        tmp.addRoundRect(RectF(cx - .50f * s, cy - .12f * s, cx - .18f * s, cy + .46f * s), .15f * s, .15f * s, Path.Direction.CW)
        seatPath.op(tmp, Path.Op.UNION)
        tmp.reset()
        tmp.addRoundRect(RectF(cx + .18f * s, cy - .12f * s, cx + .50f * s, cy + .46f * s), .15f * s, .15f * s, Path.Direction.CW)
        seatPath.op(tmp, Path.Op.UNION)
        tmp.reset()
        tmp.addRoundRect(RectF(cx - .42f * s, cy + .10f * s, cx + .42f * s, cy + .46f * s), .14f * s, .14f * s, Path.Direction.CW)
        seatPath.op(tmp, Path.Op.UNION)
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.argb(92, 255, 255, 255)
        c.drawPath(seatPath, p)
        // plus
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3.6f * k
        p.color = Color.argb(235, 255, 255, 255)
        val py = cy + .05f * s
        val l = 14f * k
        c.drawLine(cx - l, py, cx + l, py, p)
        c.drawLine(cx, py - l, cx, py + l, p)
        p.style = Paint.Style.FILL
    }

    private fun host(c: Canvas, cx: Float, cy: Float, k: Float) {
        fun ex(deg: Float, r: Float) = cx + r * cos(Math.toRadians(deg.toDouble())).toFloat()
        fun ey(deg: Float, r: Float) = cy + r * sin(Math.toRadians(deg.toDouble())).toFloat()

        // glow
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, 80f * k,
            intArrayOf(Color.argb(140, 255, 110, 20), Color.argb(0, 255, 110, 20)),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, 80f * k, p)
        p.shader = null

        // flames
        val n = 20
        val base = 49f * k
        for (i in 0 until n) {
            val a = i * 360f / n
            val flick = sin((phase * 2f + i * 0.31f) * 2.0 * Math.PI).toFloat()
            val sway = 4f * sin((phase + i * 0.17f) * 2.0 * Math.PI).toFloat()
            val len = (11f + 7f * flick) * k
            flame.reset()
            flame.moveTo(ex(a - 8f, base - 3f * k), ey(a - 8f, base - 3f * k))
            flame.quadTo(ex(a - 4f + sway, base + len * 0.55f), ey(a - 4f + sway, base + len * 0.55f),
                ex(a + sway * 0.5f, base + len), ey(a + sway * 0.5f, base + len))
            flame.quadTo(ex(a + 4f + sway, base + len * 0.55f), ey(a + 4f + sway, base + len * 0.55f),
                ex(a + 8f, base - 3f * k), ey(a + 8f, base - 3f * k))
            flame.close()
            p.color = if (i % 2 == 0) Color.argb(235, 255, 154, 31) else Color.argb(235, 255, 74, 26)
            c.drawPath(flame, p)
        }

        // ring
        if (ringShader == null || ringW != width) {
            ringW = width
            ringShader = SweepGradient(
                cx, cy,
                intArrayOf(Color.parseColor("#FFD23F"), Color.parseColor("#FF7A00"), Color.parseColor("#FF3B1F"), Color.parseColor("#FFD23F")),
                null
            )
        }
        p.shader = ringShader
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f * k
        c.save()
        c.rotate(phase * 360f, cx, cy)
        c.drawCircle(cx, cy, 46f * k, p)
        c.restore()
        p.shader = null

        // avatar
        p.style = Paint.Style.FILL
        val rad = 43f * k
        val sh = avatarShader
        val b = avatar
        if (sh != null && b != null) {
            val sc = max(2f * rad / b.width, 2f * rad / b.height)
            m.reset()
            m.setScale(sc, sc)
            m.postTranslate(cx - b.width * sc / 2f, cy - b.height * sc / 2f)
            sh.setLocalMatrix(m)
            p.shader = sh
            c.drawCircle(cx, cy, rad, p)
            p.shader = null
        } else {
            p.color = Color.parseColor("#4B2A1A")
            c.drawCircle(cx, cy, rad, p)
        }
    }
}

/* ═══════════════════════════════════════ Screen ═══════════════════════════════════════ */

@Suppress("DEPRECATION")
class RoomSetupActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var titleInput: EditText
    private lateinit var cardAvatar: ImageView
    private lateinit var cardBg: GradientDrawable
    private lateinit var voiceBackdrop: View
    private lateinit var multiGrid: View
    private lateinit var voiceSeats: VoiceSeatsView
    private lateinit var toolbar: LinearLayout
    private lateinit var tabsRow: LinearLayout
    private lateinit var tabMulti: TextView
    private lateinit var tabVoice: TextView
    private lateinit var startButton: FrameLayout
    private lateinit var startLabel: TextView
    private lateinit var statusCover: View
    private lateinit var layer: FrameLayout

    private val chipBoxes = mutableListOf<FrameLayout>()
    private val chipLabels = mutableListOf<TextView>()
    private var selectedCategory = 4 // visual order left→right, 4 = دردشة

    private var unit = 1f
    private var screenW = 0
    private var profileAvatar: Bitmap? = null
    private var placeholder: Bitmap? = null
    private var announcement = ""
    private var cardAlpha = 76
    private var selectedBroadcastMode = "audio" // "audio" = 8 seats, "camera6" = multi-guest camera
    private var selectedCapacity = 8

    private companion object {
        const val REQ_PICK = 4101
        const val NAV_VOICE = "#2A190E"
        const val NAV_MULTI = "#29286F"
    }

    /* ─────────── helpers ─────────── */

    private fun u(v: Float) = (v * unit).toInt()
    private fun uf(v: Float) = v * unit

    private fun oval(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }

    private fun rrect(fill: Int, r: Float, stroke: Int = Color.TRANSPARENT, sw: Float = 0f) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = uf(r)
            if (sw > 0f) setStroke(max(1, u(sw)), stroke)
        }

    private fun ripple(content: Drawable, r: Float, color: Int) =
        RippleDrawable(ColorStateList.valueOf(color), content, rrect(Color.WHITE, r))

    /** w: 0 = regular, 1 = medium, 2 = bold. Size is in "720-wide design pixels". */
    private fun label(s: CharSequence, sizePx: Float, color: Int, w: Int = 0) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_PX, uf(sizePx))
        setTextColor(color)
        includeFontPadding = false
        typeface = when (w) {
            2 -> Typeface.create("sans-serif", Typeface.BOLD)
            1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun icon(kind: String, tint: Int, stroke: Float = 1.8f) = SetupIconView(this, kind, tint, stroke)

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun View.pressable(scale: Float = 0.96f) {
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(scale).scaleY(scale).setDuration(110)
                        .setInterpolator(DecelerateInterpolator()).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(240)
                        .setInterpolator(OvershootInterpolator(2.4f)).start()
            }
            false
        }
    }

    private fun placeholderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 512f, 512f,
            intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#C026D3")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        paint.shader = null
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 190f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("B", 256f, 325f, paint)
        return bitmap
    }

    private fun setStart(enabled: Boolean, text: String) {
        startButton.isEnabled = enabled
        startButton.alpha = if (enabled) 1f else 0.6f
        startLabel.text = text
    }

    private fun clearTitleFocus() {
        titleInput.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(titleInput.windowToken, 0)
    }

    /* ─────────── lifecycle / UI ─────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.parseColor(NAV_VOICE)
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        screenW = resources.displayMetrics.widthPixels
        unit = screenW / 720f
        placeholder = placeholderBitmap()
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener { clearTitleFocus() }
        }

        // backdrops
        root.addView(MultiBackdrop(this), FrameLayout.LayoutParams(-1, -1))
        val bgRes = resources.getIdentifier("bg_voice_room", "drawable", packageName)
        voiceBackdrop = if (bgRes != 0) {
            ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(bgRes) }
        } else VoiceBackdrop(this)
        root.addView(voiceBackdrop, FrameLayout.LayoutParams(-1, -1))

        layer = FrameLayout(this).apply { clipChildren = false }
        root.addView(layer, FrameLayout.LayoutParams(-1, -1))

        // ── Close (top-left)
        val close = FrameLayout(this).apply {
            isClickable = true
            pressable(0.9f)
            setOnClickListener { finish() }
            addView(icon("close", Color.WHITE, 2.5f), FrameLayout.LayoutParams(u(56f), u(56f), Gravity.CENTER))
        }
        layer.addView(close, FrameLayout.LayoutParams(u(80f), u(80f), Gravity.LEFT or Gravity.TOP).apply { topMargin = u(5f) })

        // ── Top card
        cardBg = GradientDrawable().apply { cornerRadius = uf(36f); setColor(Color.argb(cardAlpha, 0, 0, 0)) }
        val card = FrameLayout(this).apply { background = cardBg; clipChildren = false }
        layer.addView(
            card,
            FrameLayout.LayoutParams(u(668f), u(261f), Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = u(93f) }
        )

        // profile picture + "تبديل صورة..."
        cardAvatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(placeholder)
        }
        val avatarBox = FrameLayout(this).apply {
            isClickable = true
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, uf(12f))
                }
            }
            pressable(0.97f)
            setOnClickListener { pickImage() }
            addView(cardAvatar, FrameLayout.LayoutParams(-1, -1))
            addView(
                label("تبديل صورة...", 19f, Color.WHITE, 0).apply {
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.argb(120, 0, 0, 0))
                    maxLines = 1
                },
                FrameLayout.LayoutParams(-1, u(26f), Gravity.BOTTOM)
            )
        }
        card.addView(
            avatarBox,
            FrameLayout.LayoutParams(u(105f), u(105f), Gravity.RIGHT or Gravity.TOP).apply {
                rightMargin = u(18f); topMargin = u(19f)
            }
        )

        // broadcast title (replaces the word "دايلر")
        titleInput = EditText(this).apply {
            setText(intent.getStringExtra("room_name").orEmpty())
            hint = "اكتب عنوان البث"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, uf(30f))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(140, 255, 255, 255))
            highlightColor = Color.argb(90, 255, 255, 255)
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(40))
            background = null
            setPadding(0, 0, 0, 0)
            includeFontPadding = false
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { clearTitleFocus(); true } else false
            }
            if (Build.VERSION.SDK_INT >= 29) {
                textCursorDrawable = GradientDrawable().apply { setColor(Color.WHITE); setSize(max(2, u(3f)), -1) }
            }
        }
        card.addView(
            titleInput,
            FrameLayout.LayoutParams(u(499f), u(56f), Gravity.RIGHT or Gravity.TOP).apply {
                rightMargin = u(149f); topMargin = u(19f)
            }
        )

        // announcement row
        val announceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            pressable(0.96f)
            setOnClickListener { showAnnouncementDialog() }
            addView(label("إعلان", 25f, Color.WHITE, 0))
            addView(icon("megaphone", Color.WHITE, 1.6f), LinearLayout.LayoutParams(u(27f), u(27f)).apply { leftMargin = u(10f) })
        }
        card.addView(
            announceRow,
            FrameLayout.LayoutParams(-2, u(40f), Gravity.RIGHT or Gravity.TOP).apply {
                rightMargin = u(149f); topMargin = u(87f)
            }
        )

        // category chips
        card.addView(
            buildChips(),
            FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT or Gravity.TOP).apply {
                rightMargin = u(18f); topMargin = u(144f)
            }
        )

        // ── Seat layouts
        multiGrid = MultiGridView(this)
        layer.addView(multiGrid, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = u(392f) })
        voiceSeats = VoiceSeatsView(this)
        voiceSeats.setAvatar(placeholder!!)
        layer.addView(voiceSeats, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = u(354f) })

        // ── Bottom toolbar
        toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        layer.addView(
            toolbar,
            FrameLayout.LayoutParams(u(548f), u(82f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = u(244f) }
        )

        // ── Start button
        startLabel = label("بدء البث المباشر", 32f, Color.WHITE, 2).apply { gravity = Gravity.CENTER }
        startButton = FrameLayout(this).apply {
            val face = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#00E9D6"), Color.parseColor("#55C6FF"))
            ).apply { cornerRadius = uf(42f) }
            background = ripple(face, 42f, Color.argb(70, 255, 255, 255))
            isClickable = true
            pressable(0.97f)
            setOnClickListener { createAndStart(token) }
            addView(startLabel, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        }
        layer.addView(
            startButton,
            FrameLayout.LayoutParams(u(525f), u(84f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = u(116f) }
        )

        // ── Mode tabs (only: صوتي + متعدد الضيوف)
        fun tabLabel(s: String) = label(s, 24f, Color.WHITE, 1).apply {
            setPadding(u(32f), u(8f), u(32f), u(8f))
            isClickable = true
            alpha = 0.55f
        }
        tabMulti = tabLabel("بث متعدد الضيوف").apply { setOnClickListener { if (selectedBroadcastMode != "camera6") applyMode("camera6", true) } }
        tabVoice = tabLabel("بث صوتي").apply { setOnClickListener { if (selectedBroadcastMode != "audio") applyMode("audio", true) } }
        tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            addView(tabMulti, LinearLayout.LayoutParams(-2, -2))
            addView(tabVoice, LinearLayout.LayoutParams(-2, -2))
        }
        val tabsHost = FrameLayout(this).apply {
            clipChildren = false
            addView(tabsRow, FrameLayout.LayoutParams(-2, -2, Gravity.LEFT or Gravity.CENTER_VERTICAL))
        }
        layer.addView(tabsHost, FrameLayout.LayoutParams(-1, u(64f), Gravity.BOTTOM).apply { bottomMargin = u(48f) })

        layer.addView(
            View(this).apply { background = oval(Color.WHITE) },
            FrameLayout.LayoutParams(u(10f), u(10f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = u(32f) }
        )

        // ── Status-bar cover (black, like the reference)
        statusCover = View(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(statusCover, FrameLayout.LayoutParams(-1, 0, Gravity.TOP))

        root.setOnApplyWindowInsetsListener { _, ins ->
            val top = ins.systemWindowInsetTop
            val bottom = ins.systemWindowInsetBottom
            statusCover.layoutParams.height = top
            statusCover.requestLayout()
            // gesture-bar (small) is drawn under like the reference; 3-button nav pushes the UI up
            layer.setPadding(0, top, 0, if (bottom > u(70f)) bottom else 0)
            ins
        }

        setContentView(root)
        root.requestFocus()
        applyMode("audio", false)
        loadProfileAvatar(token)
    }

    /* ─────────── chips ─────────── */

    private fun buildChips(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        val cats = listOf(
            "emotional" to "عاطفية", "interests" to "الاهتمامات", "games" to "الألعاب",
            "dating" to "المواعدة", "chat" to "دردشة"
        )
        cats.forEachIndexed { i, (kind, title) ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                pressable(0.95f)
                setOnClickListener { selectCategory(i) }
            }
            val box = FrameLayout(this)
            box.addView(CategoryIconView(this, kind), FrameLayout.LayoutParams(u(40f), u(40f), Gravity.CENTER))
            val lab = label(title, 21f, Color.WHITE, 0)
            col.addView(box, LinearLayout.LayoutParams(u(105f), u(60f)))
            col.addView(lab, LinearLayout.LayoutParams(-2, -2).apply { topMargin = u(9f) })
            chipBoxes.add(box)
            chipLabels.add(lab)
            row.addView(col, LinearLayout.LayoutParams(u(105f), -2).apply { if (i > 0) leftMargin = u(11f) })
        }
        selectCategory(selectedCategory)
        return row
    }

    private fun selectCategory(i: Int) {
        selectedCategory = i
        chipBoxes.forEachIndexed { idx, box ->
            val sel = idx == i
            box.background =
                if (sel) rrect(Color.argb(46, 255, 255, 255), 18f, Color.argb(235, 255, 255, 255), 2.5f)
                else rrect(Color.argb(36, 255, 255, 255), 18f)
            chipLabels[idx].setTextColor(if (sel) Color.WHITE else Color.argb(200, 255, 255, 255))
            chipLabels[idx].typeface =
                Typeface.create("sans-serif", if (sel) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    /* ─────────── mode switching ─────────── */

    private fun applyMode(mode: String, animate: Boolean) {
        val voice = mode == "audio"
        selectedBroadcastMode = mode
        selectedCapacity = if (voice) 8 else 6
        val dur = if (animate) 320L else 0L

        voiceBackdrop.animate().alpha(if (voice) 1f else 0f).setDuration(dur).start()
        voiceSeats.animate().alpha(if (voice) 1f else 0f).setDuration(dur).start()
        multiGrid.animate().alpha(if (voice) 0f else 1f).setDuration(dur).start()

        val target = if (voice) 76 else 43
        if (animate) {
            ValueAnimator.ofInt(cardAlpha, target).apply {
                duration = dur
                addUpdateListener {
                    cardAlpha = it.animatedValue as Int
                    cardBg.setColor(Color.argb(cardAlpha, 0, 0, 0))
                }
                start()
            }
        } else {
            cardAlpha = target
            cardBg.setColor(Color.argb(cardAlpha, 0, 0, 0))
        }

        window.navigationBarColor = Color.parseColor(if (voice) NAV_VOICE else NAV_MULTI)
        buildToolbar(voice, animate)
        updateTabs(animate)
    }

    private fun updateTabs(animate: Boolean) {
        if (tabsRow.width == 0) {
            tabsRow.post { updateTabs(false) }
            return
        }
        val voice = selectedBroadcastMode == "audio"
        val sel = if (voice) tabVoice else tabMulti
        val other = if (voice) tabMulti else tabVoice
        val dur = if (animate) 300L else 0L
        val shift = screenW / 2f - (sel.left + sel.width / 2f)
        sel.animate().scaleX(1.38f).scaleY(1.38f).alpha(1f).setDuration(dur).start()
        other.animate().scaleX(1f).scaleY(1f).alpha(0.55f).setDuration(dur).start()
        tabsRow.animate().translationX(shift).setDuration(dur).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun buildToolbar(voice: Boolean, animate: Boolean) {
        toolbar.removeAllViews()
        val items = if (voice) listOf(
            Triple("settings", "اعدادات", false),
            Triple("creator", "مركز المبدعين", false),
            Triple("mixer", "دامج الصوت", false),
            Triple("shirt", "مظهر", false),
            Triple("seat", "المقاعد", false)
        ) else listOf(
            Triple("settings", "اعدادات", false),
            Triple("magic", "سحر", true),
            Triple("beauty", "تجميل", false),
            Triple("shirt", "مظهر", false),
            Triple("seat", "المقاعد", false)
        )
        items.forEach { (k, l, d) -> toolbar.addView(toolItem(k, l, d), LinearLayout.LayoutParams(0, -2, 1f)) }
        if (animate) {
            toolbar.alpha = 0f
            toolbar.animate().alpha(1f).setDuration(260).start()
        }
    }

    private fun toolItem(kind: String, title: String, dot: Boolean): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            pressable(0.92f)
            setOnClickListener { toast("قريباً") }
        }
        val wrap = FrameLayout(this).apply { clipChildren = false }
        wrap.addView(icon(kind, Color.WHITE, 1.7f), FrameLayout.LayoutParams(u(44f), u(44f), Gravity.CENTER))
        if (dot) {
            wrap.addView(
                View(this).apply {
                    background = oval(Color.parseColor("#FF2D55"))
                    translationX = -uf(17f)
                    translationY = -uf(17f)
                },
                FrameLayout.LayoutParams(u(15f), u(15f), Gravity.CENTER)
            )
        }
        item.addView(wrap, LinearLayout.LayoutParams(u(54f), u(48f)))
        item.addView(
            label(title, 20f, Color.WHITE, 0).apply { gravity = Gravity.CENTER; maxLines = 1 },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = u(5f) }
        )
        return item
    }

    /* ─────────── announcement + image picking ─────────── */

    private fun showAnnouncementDialog() {
        val input = EditText(this).apply {
            setText(announcement)
            hint = "اكتب إعلان الغرفة"
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(u(40f), u(24f), u(40f), u(24f))
        }
        AlertDialog.Builder(this)
            .setTitle("إعلان")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                announcement = input.text.toString().trim()
                toast("تم حفظ الإعلان")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun pickImage() {
        try {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(i, "اختر صورة"), REQ_PICK)
        } catch (_: Exception) {
            toast("تعذر فتح المعرض")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        executor.execute {
            try {
                val bmp = decodeSampled(uri, 1280) ?: throw Exception("decode")
                profileAvatar = bmp
                runOnUiThread { if (!isFinishing) applyAvatar(bmp) }
            } catch (_: Exception) {
                runOnUiThread { toast("تعذر تحميل الصورة") }
            }
        }
    }

    private fun decodeSampled(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val orientation = contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val deg = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (deg != 0f) {
                    val mt = Matrix().apply { postRotate(deg) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mt, true)
                }
            } catch (_: Exception) {
            }
        }
        return bmp
    }

    private fun applyAvatar(bmp: Bitmap) {
        cardAvatar.setImageBitmap(bmp)
        voiceSeats.setAvatar(bmp)
    }

    /* ─────────── data ─────────── */

    private fun loadProfileAvatar(token: String) {
        if (token.isBlank()) return
        executor.execute {
            try {
                val response = AuthApi.get("/api/profile", token)
                if (response.status !in 200..299) return@execute
                val profile = response.body.optJSONObject("profile") ?: JSONObject()
                val url = profile.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                if (url.isNotBlank()) {
                    val bitmap = URL(url).openConnection()
                        .apply { connectTimeout = 10000; readTimeout = 15000 }
                        .getInputStream().use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null && profileAvatar == null) {
                        profileAvatar = bitmap
                        runOnUiThread { if (!isFinishing) applyAvatar(bitmap) }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun createAndStart(token: String) {
        val name = titleInput.text.toString().trim()
        if (name.length < 2) {
            Toast.makeText(this, "اكتب عنوان البث (حرفان على الأقل)", Toast.LENGTH_SHORT).show(); return
        }
        if (token.isBlank()) {
            Toast.makeText(this, "سجّل الدخول أولاً", Toast.LENGTH_SHORT).show(); finish(); return
        }
        clearTitleFocus()
        setStart(false, "جارٍ تجهيز غرفتك...")
        val mode = selectedBroadcastMode
        val capacity = selectedCapacity
        executor.execute {
            try {
                val source = profileAvatar ?: placeholderBitmap()
                val maxSide = 1280
                val scale = min(1f, maxSide.toFloat() / max(source.width, source.height))
                val resized = if (scale < 1f)
                    Bitmap.createScaledBitmap(
                        source,
                        (source.width * scale).toInt().coerceAtLeast(1),
                        (source.height * scale).toInt().coerceAtLeast(1),
                        true
                    ) else source
                val bytes = ByteArrayOutputStream()
                var quality = 85
                do {
                    bytes.reset(); resized.compress(Bitmap.CompressFormat.JPEG, quality, bytes); quality -= 10
                } while (bytes.size() > 3 * 1024 * 1024 && quality >= 45)
                if (bytes.size() > 3 * 1024 * 1024) throw Exception("تعذر تجهيز صورة الغرفة")
                val payload = JSONObject().put("name", name).put("max_members", capacity)
                    .put("room_mode", if (mode == "audio") "audio" else "camera")
                    .put("camera_seat_count", if (mode == "camera6") 6 else 0)
                    .put("image_mime", "image/jpeg")
                    .put("image_base64", android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP))
                if (resized !== source) resized.recycle()
                if (source !== profileAvatar && !source.isRecycled) source.recycle()
                val result = AuthApi.post("/api/voice-rooms", payload, token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إنشاء الغرفة"))
                val room = result.body.optJSONObject("room") ?: throw Exception("استجابة الخادم غير مكتملة")
                val roomId = room.optString("id")
                runOnUiThread {
                    startActivity(Intent(this, RoomActivity::class.java).putExtra("room_id", roomId).putExtra("room_name", name))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setStart(true, "بدء البث المباشر")
                    Toast.makeText(this, e.message ?: "تعذر إنشاء الغرفة", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}