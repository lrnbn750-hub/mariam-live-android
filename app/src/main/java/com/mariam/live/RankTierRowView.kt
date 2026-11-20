package com.mariam.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.ceil

/** Canvas-drawn tier row with current/completed/locked states and RTL-aware layout. */
class RankTierRowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    enum class State { DONE, CURRENT, LOCKED }
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val emblem = LevelEmblemPainter()
    private var tier = 0
    private var state = State.LOCKED
    private var filled = 0
    private var currentLevel = 1

    init {
        namePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        namePaint.textSize = 16f * d
        rangePaint.typeface = Typeface.DEFAULT
        rangePaint.textSize = 12f * d
        chipPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        chipPaint.textSize = 11.5f * d
    }

    fun bind(tierIndex: Int, level: Int) {
        tier = tierIndex.coerceIn(0, 9)
        currentLevel = level.coerceIn(1, 100)
        val first = LevelTiers.firstLevel(tier)
        val last = LevelTiers.lastLevel(tier)
        state = when {
            currentLevel > last -> State.DONE
            currentLevel >= first -> State.CURRENT
            else -> State.LOCKED
        }
        filled = when (state) {
            State.DONE -> 10
            State.CURRENT -> currentLevel - first + 1
            State.LOCKED -> 0
        }
        contentDescription = "${LevelTiers.ALL[tier].displayName(context)} LV $first - LV $last"
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(ceil(300f * d).toInt(), widthMeasureSpec), ceil(92f * d).toInt())
    }

    private val rtl get() = layoutDirection == LAYOUT_DIRECTION_RTL
    private fun lx(off: Float, w: Float): Float = if (rtl) width - off - w else off

    override fun onDraw(canvas: Canvas) {
        val t = LevelTiers.ALL[tier]
        val w = width.toFloat(); val h = height.toFloat(); val cy = h / 2f
        val locked = state == State.LOCKED
        val current = state == State.CURRENT
        val radius = 22f * d
        if (current) {
            for (k in 2 downTo 1) {
                paint.shader = null; paint.style = Paint.Style.STROKE
                paint.strokeWidth = k * 3f * d; paint.color = LevelTiers.alpha(t.glow, 46 / k)
                rect.set(k*1.5f*d, k*1.5f*d, w-k*1.5f*d, h-k*1.5f*d)
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }
        rect.set(1.5f*d, 1.5f*d, w-1.5f*d, h-1.5f*d)
        paint.style = Paint.Style.FILL
        val topC = when (state) {
            State.CURRENT -> LevelTiers.blend(0xFF162135.toInt(), t.innerA, 0.28f)
            State.DONE -> 0xFF162135.toInt()
            State.LOCKED -> 0xFF0F1726.toInt()
        }
        val botC = if (locked) 0xFF0C1422.toInt() else 0xFF101A2C.toInt()
        paint.shader = LinearGradient(0f,0f,0f,h,topC,botC,Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect,radius,radius,paint)
        paint.shader = null; paint.style = Paint.Style.STROKE
        paint.strokeWidth = (if (current) 1.5f else 1f)*d
        paint.color = if (current) LevelTiers.alpha(t.stroke,235) else if (locked) 0xFF1B2740.toInt() else 0xFF243350.toInt()
        canvas.drawRoundRect(rect,radius,radius,paint)

        val er = 24f*d
        val ecx = if (rtl) w-14f*d-er else 14f*d+er
        val shownLevel = if (current) currentLevel else LevelTiers.lastLevel(tier)
        try {
            emblem.draw(canvas,ecx,cy,er*0.92f,shownLevel,locked,false,0f)
        } catch (error: Throwable) {
            Log.e("BAKWealthLevel", "Tier emblem drawing failed for tier=$tier", error)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = t.ringB
            canvas.drawCircle(ecx, cy, er * 0.88f, paint)
        }

        val textOff = 14f*d + er*2f + 14f*d
        val align = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val tx = if (rtl) w-textOff else textOff
        namePaint.textAlign = align; rangePaint.textAlign = align
        namePaint.color = if (locked) 0xFF8391AB.toInt() else 0xFFFFFFFF.toInt()
        rangePaint.color = if (locked) 0xFF5F6C85.toInt() else LevelTiers.alpha(t.ringB,255)
        if (!locked && !current) rangePaint.color = 0xFF8FA0BC.toInt()
        val nameY = 30f*d + (-(namePaint.fontMetrics.ascent+namePaint.fontMetrics.descent)/2f)
        canvas.drawText(t.displayName(context),tx,nameY,namePaint)
        val first = LevelTiers.firstLevel(tier); val last = LevelTiers.lastLevel(tier)
        val rangeY = 50f*d + (-(rangePaint.fontMetrics.ascent+rangePaint.fontMetrics.descent)/2f)
        canvas.drawText("LV $first — LV $last",tx,rangeY,rangePaint)

        val barW=112f*d; val gap=3f*d; val segW=(barW-9f*gap)/10f; val segH=5f*d; val segTop=66f*d
        paint.style=Paint.Style.FILL
        for(i in 0 until 10){
            val left=lx(textOff+i*(segW+gap),segW)
            rect.set(left,segTop,left+segW,segTop+segH)
            paint.color=when{
                i<filled && state==State.DONE -> LevelTiers.alpha(t.ringB,235)
                i<filled -> t.glow
                else -> 0xFF24314A.toInt()
            }
            canvas.drawRoundRect(rect,segH/2f,segH/2f,paint)
        }

        val label=context.getString(when(state){
            State.DONE->R.string.wealth_status_done
            State.CURRENT->R.string.wealth_status_current
            State.LOCKED->R.string.wealth_status_locked
        })
        val iconW=12f*d; val chipTextW=chipPaint.measureText(label)
        val chipW=12f*d+iconW+6f*d+chipTextW+12f*d; val chipH=28f*d
        val chipLeft=if(rtl)14f*d else w-14f*d-chipW
        rect.set(chipLeft,cy-chipH/2f,chipLeft+chipW,cy+chipH/2f)
        val fg:Int
        paint.style=Paint.Style.FILL
        when(state){
            State.DONE->{fg=0xFF6EE7B7.toInt();paint.color=0x2434D399}
            State.CURRENT->{fg=t.glow;paint.color=LevelTiers.alpha(t.glow,44)}
            State.LOCKED->{fg=0xFF7C8AA5.toInt();paint.color=0xFF1A2438.toInt()}
        }
        canvas.drawRoundRect(rect,chipH/2f,chipH/2f,paint)
        if(state!=State.LOCKED){
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1f*d
            paint.color=LevelTiers.alpha(fg,150);canvas.drawRoundRect(rect,chipH/2f,chipH/2f,paint)
        }
        val iconLeft=if(rtl)chipLeft+chipW-12f*d-iconW else chipLeft+12f*d
        val icx=iconLeft+iconW/2f
        paint.style=Paint.Style.STROKE;paint.strokeWidth=1.8f*d
        paint.strokeCap=Paint.Cap.ROUND;paint.strokeJoin=Paint.Join.ROUND;paint.color=fg
        when(state){
            State.DONE->{
                path.reset();path.moveTo(icx-4f*d,cy+0.2f*d);path.lineTo(icx-1.2f*d,cy+3f*d);path.lineTo(icx+4.2f*d,cy-3.2f*d)
                canvas.drawPath(path,paint)
            }
            State.CURRENT->{paint.style=Paint.Style.FILL;canvas.drawCircle(icx,cy,3.2f*d,paint)}
            State.LOCKED->{
                paint.style=Paint.Style.FILL;rect.set(icx-4.5f*d,cy-0.5f*d,icx+4.5f*d,cy+4.8f*d)
                canvas.drawRoundRect(rect,1.4f*d,1.4f*d,paint)
                paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f*d
                rect.set(icx-3f*d,cy-5f*d,icx+3f*d,cy+1.5f*d)
                canvas.drawArc(rect,180f,180f,false,paint)
            }
        }
        paint.strokeCap=Paint.Cap.BUTT
        chipPaint.color=fg;chipPaint.textAlign=if(rtl)Paint.Align.RIGHT else Paint.Align.LEFT
        val chipTx=if(rtl)iconLeft-6f*d else iconLeft+iconW+6f*d
        val chipY=cy+(-(chipPaint.fontMetrics.ascent+chipPaint.fontMetrics.descent)/2f)
        canvas.drawText(label,chipTx,chipY,chipPaint)
    }
}