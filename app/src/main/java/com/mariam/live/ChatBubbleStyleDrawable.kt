package com.mariam.live

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock

/** Seven scalable native chat skins; the center remains clear for long messages. */
class ChatBubbleStyleDrawable(private val styleId: String) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private data class Palette(val top:Int,val bottom:Int,val edge:Int,val glow:Int,val accent:Int)

    private fun palette() = when(styleId) {
        "chat-bubble-purple" -> Palette(Color.rgb(91,48,145),Color.rgb(35,18,68),Color.rgb(224,177,255),Color.rgb(174,94,255),Color.rgb(255,220,255))
        "chat-bubble-blue" -> Palette(Color.rgb(28,112,174),Color.rgb(9,35,78),Color.rgb(135,225,255),Color.rgb(35,177,255),Color.rgb(220,250,255))
        "chat-bubble-green" -> Palette(Color.rgb(23,133,100),Color.rgb(7,48,42),Color.rgb(142,242,190),Color.rgb(36,220,154),Color.rgb(226,255,222))
        "chat-bubble-pink" -> Palette(Color.rgb(194,75,132),Color.rgb(77,24,68),Color.rgb(255,190,224),Color.rgb(255,91,174),Color.rgb(255,231,244))
        "chat-bubble-gold" -> Palette(Color.rgb(165,111,27),Color.rgb(57,34,12),Color.rgb(255,220,129),Color.rgb(255,174,46),Color.rgb(255,245,197))
        "chat-bubble-obsidian" -> Palette(Color.rgb(77,83,101),Color.rgb(15,18,30),Color.rgb(193,204,224),Color.rgb(112,145,194),Color.rgb(244,248,255))
        else -> Palette(Color.rgb(145,24,37),Color.rgb(48,5,18),Color.rgb(255,183,93),Color.rgb(255,61,67),Color.rgb(255,232,190))
    }

    override fun draw(canvas: Canvas) {
        val b=bounds
        if(b.width()<=0 || b.height()<=0) return
        val p=palette(); val w=b.width().toFloat(); val h=b.height().toFloat()
        val u=(w/320f).coerceIn(.65f,1.8f); val inset=2.5f*u
        rect.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset)
        path.reset()
        val r=(15f*u).coerceAtMost(h*.22f)
        path.moveTo(rect.left+r,rect.top); path.lineTo(rect.right-r,rect.top)
        path.quadTo(rect.right,rect.top,rect.right,rect.top+r)
        path.lineTo(rect.right,rect.bottom-r*1.35f); path.quadTo(rect.right,rect.bottom,rect.right-r,rect.bottom)
        path.lineTo(rect.left+r*1.55f,rect.bottom); path.lineTo(rect.left+r*.75f,rect.bottom+2f*u)
        path.lineTo(rect.left+r*.9f,rect.bottom-r*.55f); path.quadTo(rect.left,rect.bottom-r*.7f,rect.left,rect.bottom-r*1.6f)
        path.lineTo(rect.left,rect.top+r); path.quadTo(rect.left,rect.top,rect.left+r,rect.top); path.close()

        fill.shader=LinearGradient(rect.left,rect.top,rect.right,rect.bottom,p.top,p.bottom,Shader.TileMode.CLAMP)
        fill.setShadowLayer(5f*u,0f,2f*u,p.glow); canvas.drawPath(path,fill)
        fill.clearShadowLayer(); fill.shader=null
        edge.color=p.edge; edge.strokeWidth=1.45f*u; canvas.drawPath(path,edge)

        shine.style=Paint.Style.STROKE; shine.strokeWidth=1f*u; shine.color=Color.argb(115,255,255,255)
        val inner=RectF(rect.left+4f*u,rect.top+3f*u,rect.right-4f*u,rect.bottom-4f*u)
        canvas.drawRoundRect(inner,r*.72f,r*.72f,shine)

        accent.color=p.accent; accent.style=Paint.Style.STROKE; accent.strokeWidth=1.35f*u; accent.strokeCap=Paint.Cap.ROUND
        val cx=rect.right-16f*u; val cy=rect.top+13f*u
        val pulse=.82f+.18f*kotlin.math.sin(SystemClock.uptimeMillis()/420.0).toFloat()
        accent.alpha=(190*pulse).toInt().coerceIn(0,255)
        canvas.drawLine(cx-3f*u,cy,cx+3f*u,cy,accent); canvas.drawLine(cx,cy-3f*u,cx,cy+3f*u,accent)
        accent.style=Paint.Style.FILL; canvas.drawCircle(rect.left+13f*u,rect.top+13f*u,1.8f*u,accent)
        scheduleSelf({invalidateSelf()},SystemClock.uptimeMillis()+70L)
    }
    override fun setAlpha(alpha:Int){fill.alpha=alpha;edge.alpha=alpha;shine.alpha=alpha;accent.alpha=alpha}
    override fun setColorFilter(colorFilter:android.graphics.ColorFilter?){fill.colorFilter=colorFilter;edge.colorFilter=colorFilter;shine.colorFilter=colorFilter;accent.colorFilter=colorFilter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
