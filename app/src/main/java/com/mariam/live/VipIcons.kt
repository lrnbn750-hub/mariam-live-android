package com.mariam.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * محلل بسيط لبيانات مسارات SVG (M L H V C S A Z بالحالتين الكبيرة والصغيرة).
 * يدعم الأرقام المتلاصقة مثل "3.3.5" وأعلام القوس المتلاصقة مثل "a2 2 0 014 0".
 * الأقواس المدعومة بدون دوران (rot = 0) وهذا كل ما تستخدمه الأيقونات.
 */
internal object SvgPath {
    private class Reader(val s: String) {
        var i = 0
        fun eof() = i >= s.length
        fun skipSep() { while (i < s.length && (s[i] == ' ' || s[i] == ',' || s[i] == '\n' || s[i] == '\t' || s[i] == '\r')) i++ }
        fun peek(): Char = s[i]
        fun number(): Float {
            skipSep()
            val st = i
            if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
            while (i < s.length && s[i].isDigit()) i++
            if (i < s.length && s[i] == '.') {
                i++
                while (i < s.length && s[i].isDigit()) i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
                while (i < s.length && s[i].isDigit()) i++
            }
            return s.substring(st, i).toFloat()
        }
        fun flag(): Boolean {
            skipSep()
            val c = s[i]
            i++
            return c == '1'
        }
    }

    fun parse(d: String): Path {
        val p = Path()
        val r = Reader(d)
        var cmd = ' '
        var cx = 0f; var cy = 0f      // النقطة الحالية
        var sx = 0f; var sy = 0f      // بداية المسار الفرعي
        var lcx = 0f; var lcy = 0f    // آخر نقطة تحكم (للأمر S)
        var prevCubic = false

        while (true) {
            r.skipSep()
            if (r.eof()) break
            val ch = r.peek()
            if (ch.isLetter()) {
                cmd = ch
                r.i++
                if (cmd == 'Z' || cmd == 'z') {
                    p.close(); cx = sx; cy = sy; prevCubic = false
                    continue
                }
            } else if (cmd == ' ') {
                break
            }
            when (cmd) {
                'M', 'm' -> {
                    val x = r.number(); val y = r.number()
                    if (cmd == 'm') { cx += x; cy += y } else { cx = x; cy = y }
                    p.moveTo(cx, cy); sx = cx; sy = cy
                    cmd = if (cmd == 'M') 'L' else 'l'
                    prevCubic = false
                }
                'L', 'l' -> {
                    val x = r.number(); val y = r.number()
                    if (cmd == 'l') { cx += x; cy += y } else { cx = x; cy = y }
                    p.lineTo(cx, cy); prevCubic = false
                }
                'H', 'h' -> {
                    val x = r.number()
                    cx = if (cmd == 'h') cx + x else x
                    p.lineTo(cx, cy); prevCubic = false
                }
                'V', 'v' -> {
                    val y = r.number()
                    cy = if (cmd == 'v') cy + y else y
                    p.lineTo(cx, cy); prevCubic = false
                }
                'C', 'c' -> {
                    var x1 = r.number(); var y1 = r.number()
                    var x2 = r.number(); var y2 = r.number()
                    var x = r.number(); var y = r.number()
                    if (cmd == 'c') { x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy }
                    p.cubicTo(x1, y1, x2, y2, x, y)
                    lcx = x2; lcy = y2; cx = x; cy = y; prevCubic = true
                }
                'S', 's' -> {
                    var x2 = r.number(); var y2 = r.number()
                    var x = r.number(); var y = r.number()
                    if (cmd == 's') { x2 += cx; y2 += cy; x += cx; y += cy }
                    val x1 = if (prevCubic) 2 * cx - lcx else cx
                    val y1 = if (prevCubic) 2 * cy - lcy else cy
                    p.cubicTo(x1, y1, x2, y2, x, y)
                    lcx = x2; lcy = y2; cx = x; cy = y; prevCubic = true
                }
                'A', 'a' -> {
                    val rx = r.number(); val ry = r.number(); r.number() // rot (غير مستخدم)
                    val large = r.flag(); val sweep = r.flag()
                    var x = r.number(); var y = r.number()
                    if (cmd == 'a') { x += cx; y += cy }
                    arc(p, cx, cy, rx, ry, large, sweep, x, y)
                    cx = x; cy = y; prevCubic = false
                }
                else -> break
            }
        }
        return p
    }

    /** تحويل قوس SVG (endpoint) إلى arcTo في أندرويد (ملحق F.6.5 من مواصفة SVG، rot = 0). */
    private fun arc(p: Path, x1: Float, y1: Float, rxIn: Float, ryIn: Float, large: Boolean, sweep: Boolean, x2: Float, y2: Float) {
        if (x1 == x2 && y1 == y2) return
        var rx = Math.abs(rxIn).toDouble()
        var ry = Math.abs(ryIn).toDouble()
        if (rx == 0.0 || ry == 0.0) { p.lineTo(x2, y2); return }
        val x1p = (x1 - x2) / 2.0
        val y1p = (y1 - y2) / 2.0
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if (lambda > 1.0) { val s = Math.sqrt(lambda); rx *= s; ry *= s }
        val num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
        val den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        var coef = Math.sqrt(Math.max(0.0, num / den))
        if (large == sweep) coef = -coef
        val cxp = coef * rx * y1p / ry
        val cyp = -coef * ry * x1p / rx
        val cx = cxp + (x1 + x2) / 2.0
        val cy = cyp + (y1 + y2) / 2.0
        val ux = (x1p - cxp) / rx
        val uy = (y1p - cyp) / ry
        val vx = (-x1p - cxp) / rx
        val vy = (-y1p - cyp) / ry
        val theta = Math.toDegrees(Math.atan2(uy, ux))
        var delta = Math.toDegrees(Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy))
        if (!sweep && delta > 0) delta -= 360.0 else if (sweep && delta < 0) delta += 360.0
        p.arcTo(
            RectF((cx - rx).toFloat(), (cy - ry).toFloat(), (cx + rx).toFloat(), (cy + ry).toFloat()),
            theta.toFloat(), delta.toFloat()
        )
    }
}

/* ===================== Icon shapes (viewBox 24x24) ===================== */
internal sealed class IconShape {
    class Pth(private val d: String) : IconShape() { val path: Path by lazy { SvgPath.parse(d) } }
    class Circ(val cx: Float, val cy: Float, val r: Float, val fill: Int? = null) : IconShape()
    class Rct(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float) : IconShape()
    object VipText : IconShape()
}

internal object VipIcons {
    private val WHEEL = Color.rgb(0x26, 0x2C, 0x3B)

    private val SHAPES: Map<VipIcon, List<IconShape>> = mapOf(
        VipIcon.ENTRY to listOf(IconShape.Pth("M13 3L5 13h6l-1 8 8-11h-6z")),
        VipIcon.FRAME to listOf(
            IconShape.Circ(12f, 13.5f, 6.5f), IconShape.Circ(12f, 13.5f, 2.8f), IconShape.Pth("M8 4.5l2 2h4l2-2")
        ),
        VipIcon.VIP to listOf(IconShape.Rct(2.5f, 6f, 19f, 12f, 6f), IconShape.VipText),
        VipIcon.FOLLOW to listOf(
            IconShape.Circ(10f, 8f, 3.6f),
            IconShape.Pth("M3.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6"),
            IconShape.Pth("M16 11.5l2 2 3.5-4")
        ),
        VipIcon.FRIENDS to listOf(
            IconShape.Circ(10f, 8f, 3.6f),
            IconShape.Pth("M3.5 20c0-3.6 2.9-6 6.5-6 1.4 0 2.6.3 3.6.8"),
            IconShape.Pth("M18 20l-3-2.8c-1.3-1.5.6-3.4 2-2 .5.4.8.7 1 1 .2-.3.5-.6 1-1 1.4-1.4 3.3.5 2 2z")
        ),
        VipIcon.CAR to listOf(
            IconShape.Pth("M3.5 16.5v-4L6 8h12l2.5 4.5v4z"),
            IconShape.Circ(7.5f, 16.8f, 1.9f, WHEEL),
            IconShape.Circ(16.5f, 16.8f, 1.9f, WHEEL)
        ),
        VipIcon.SHIELD to listOf(
            IconShape.Pth("M12 3l7 3v5.5c0 4.4-3 7.8-7 9.5-4-1.7-7-5.1-7-9.5V6z"),
            IconShape.Rct(9f, 11f, 6f, 5f, 1f),
            IconShape.Pth("M10 11V9.6a2 2 0 014 0V11")
        ),
        VipIcon.BUBBLE to listOf(
            IconShape.Rct(2.5f, 6f, 19f, 12f, 6f),
            IconShape.Pth("M10.2 10.2h3.6M12 10.2V14"),
            IconShape.Pth("M17 10h1.5M17 13.5h1.5")
        ),
        VipIcon.CARD to listOf(
            IconShape.Rct(3f, 7.5f, 18f, 12.5f, 3f),
            IconShape.Pth("M7 12.5h6M7 16.2h4"),
            IconShape.Pth("M15 6l1.2-2.2L17.5 6l1.3-2.2L19.5 7")
        ),
        VipIcon.STAR to listOf(
            IconShape.Pth("M12 3l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9L6.7 19.5l1.1-6L3.4 9.3l6-.8z")
        ),
        VipIcon.SEAT to listOf(
            IconShape.Pth("M5.5 11V8a3 3 0 013-3h7a3 3 0 013 3v3"),
            IconShape.Rct(3f, 11f, 18f, 6f, 2.5f),
            IconShape.Pth("M6.5 17v3M17.5 17v3")
        ),
        VipIcon.SUPPORT to listOf(
            IconShape.Pth("M4 14v-2a8 8 0 0116 0v2"),
            IconShape.Rct(3f, 13f, 4f, 6f, 1.5f),
            IconShape.Rct(17f, 13f, 4f, 6f, 1.5f),
            IconShape.Pth("M19 19c0 1.5-2 2-5 2")
        ),
        VipIcon.MASK to listOf(
            IconShape.Pth("M3 8.5c3-2 6-2 9 0 3-2 6-2 9 0 0 5.5-2.5 8.5-5.5 8.5-1.7 0-2.5-1.2-3.5-1.2S10.2 17 8.5 17C5.5 17 3 14 3 8.5z"),
            IconShape.Pth("M6.5 11.5c1-.8 2.3-.8 3.2 0M14.3 11.5c.9-.8 2.2-.8 3.2 0")
        ),
        VipIcon.MUTE to listOf(
            IconShape.Rct(3f, 5.5f, 18f, 12f, 5f),
            IconShape.Pth("M8.5 17.5L7 21l4.5-3.5"),
            IconShape.Pth("M4 3.5l16 17")
        ),
        VipIcon.GIFT to listOf(
            IconShape.Rct(3.5f, 9f, 17f, 4f, 1f),
            IconShape.Pth("M5 13v7h14v-7M12 9v11"),
            IconShape.Pth("M12 9C9 9 7.5 7.8 7.5 6.5S9 4.5 10 5c1 .5 2 2 2 4zM12 9c3 0 4.5-1.2 4.5-2.5S15 4.5 14 5c-1 .5-2 2-2 4z")
        ),
        VipIcon.PALETTE to listOf(
            IconShape.Pth("M12 3a9 9 0 100 18c1.4 0 2-1 1.5-2.2-.5-1.2.3-2.3 1.6-2.3H17a4 4 0 004-4c0-5-4-9.5-9-9.5z"),
            IconShape.Circ(7.5f, 11f, 1f), IconShape.Circ(10f, 7.5f, 1f), IconShape.Circ(14.5f, 7.5f, 1f)
        ),
        VipIcon.HIGHLIGHT to listOf(
            IconShape.Rct(3f, 5.5f, 18f, 11.5f, 4f),
            IconShape.Pth("M8 17l-1 3.5 4.5-3.5"),
            IconShape.Pth("M12 8.3l1 2 2.2.3-1.6 1.5.4 2.2-2-1.1-2 1.1.4-2.2-1.6-1.5 2.2-.3z")
        ),
        VipIcon.DOOR to listOf(
            IconShape.Rct(5.5f, 3f, 11f, 18f, 1.5f),
            IconShape.Circ(13f, 12f, 0.9f),
            IconShape.Pth("M16.5 8H20M16.5 16H20M3 21h18")
        ),
        VipIcon.EYEOFF to listOf(
            IconShape.Pth("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"),
            IconShape.Circ(12f, 12f, 3f),
            IconShape.Pth("M4 4l16 16")
        ),
        VipIcon.BOOST to listOf(
            IconShape.Pth("M12 19V6M6.5 11.5L12 6l5.5 5.5"),
            IconShape.Pth("M6.5 20.5h11")
        ),
        VipIcon.SPARK to listOf(
            IconShape.Pth("M10 4l1.8 4.7 4.7 1.8-4.7 1.8L10 17l-1.8-4.7L3.5 10.5l4.7-1.8z"),
            IconShape.Pth("M18 3v4M16 5h4M18 15v4M16 17h4")
        ),
        VipIcon.LOCK to listOf(
            IconShape.Rct(5f, 11f, 14f, 9f, 2f),
            IconShape.Pth("M8 11V8a4 4 0 018 0v3")
        ),
        VipIcon.DIAMOND to listOf(
            IconShape.Pth("M6 4h12l4 6-10 11L2 10z"),
            IconShape.Pth("M2 10h20M9 4l-2 6 5 11 5-11-2-6")
        ),
        VipIcon.BACK to listOf(IconShape.Pth("M9 5l7 7-7 7"))
    )

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD_ITALIC)
        textSize = 6.6f
        color = Color.rgb(0xF1, 0xEC, 0xDD)
    }
    private val rect = RectF()

    /** يرسم الأيقونة داخل مربع [size] بدءاً من ([left],[top]). سمك الخط بوحدات الـ viewBox (24). */
    fun draw(c: Canvas, icon: VipIcon, left: Float, top: Float, size: Float, strokeColor: Int, strokeUnits: Float = 1.5f) {
        val shapes = SHAPES[icon] ?: return
        c.save()
        c.translate(left, top)
        val s = size / 24f
        c.scale(s, s)
        stroke.color = strokeColor
        stroke.strokeWidth = strokeUnits
        for (sh in shapes) {
            when (sh) {
                is IconShape.Pth -> c.drawPath(sh.path, stroke)
                is IconShape.Circ -> {
                    if (sh.fill != null) { fill.color = sh.fill; c.drawCircle(sh.cx, sh.cy, sh.r, fill) }
                    c.drawCircle(sh.cx, sh.cy, sh.r, stroke)
                }
                is IconShape.Rct -> {
                    rect.set(sh.x, sh.y, sh.x + sh.w, sh.y + sh.h)
                    c.drawRoundRect(rect, sh.rx, sh.rx, stroke)
                }
                IconShape.VipText -> c.drawText("VIP", 12f, 14.6f, text)
            }
        }
        c.restore()
    }
}

/** View يرسم أيقونة واحدة بحجم الـ View. */
internal class VipIconView(context: Context) : View(context) {
    var icon: VipIcon = VipIcon.STAR
        set(v) { field = v; invalidate() }
    var strokeColor: Int = Color.WHITE
        set(v) { field = v; invalidate() }
    var strokeUnits: Float = 1.5f
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val size = Math.min(width, height).toFloat()
        VipIcons.draw(canvas, icon, (width - size) / 2f, (height - size) / 2f, size, strokeColor, strokeUnits)
    }
}