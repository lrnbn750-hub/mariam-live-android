package com.mariam.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.os.SystemClock
import android.view.View

/**
 * Lightweight animated GIF frame used by room gifts and profile decorations.
 * Kept as a standalone class so both RoomActivity and ProfileActivity can use it.
 */
class RoyalGifFrame(context: Context) : View(context) {
    private var movie: Movie? = null
    private var startedAt = 0L
    private var running = false

    fun setGif(bytes: ByteArray) {
        movie = Movie.decodeByteArray(bytes, 0, bytes.size)
        startedAt = SystemClock.uptimeMillis()
        startAnimation()
    }

    fun startAnimation() {
        if (movie == null) return
        running = true
        startedAt = SystemClock.uptimeMillis()
        visibility = View.VISIBLE
        postInvalidateOnAnimation()
    }

    fun stopAnimation() {
        running = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gif = movie ?: return
        if (!running) return
        val duration = gif.duration().takeIf { it > 0 } ?: 1000
        gif.setTime(((SystemClock.uptimeMillis() - startedAt) % duration).toInt())
        val scale = minOf(
            width.toFloat() / gif.width().coerceAtLeast(1),
            height.toFloat() / gif.height().coerceAtLeast(1)
        )
        val save = canvas.save()
        canvas.translate((width - gif.width() * scale) / 2f, (height - gif.height() * scale) / 2f)
        canvas.scale(scale, scale)
        gif.draw(canvas, 0f, 0f)
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }
}
