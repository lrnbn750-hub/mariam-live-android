package com.mariam.live

import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity

/** Plays a licensed SVGA gift stored in app/src/main/res/raw. */
@Composable
fun SvgaGiftPlayer(
    @RawRes resourceId: Int,
    modifier: Modifier = Modifier,
    loops: Int = 1,
    onFinished: (() -> Unit)? = null,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val player = remember(context) { SVGAImageView(context) }
    val currentOnFinished = rememberUpdatedState(onFinished)
    val currentOnError = rememberUpdatedState(onError)

    DisposableEffect(player, resourceId, loops) {
        player.loops = loops.coerceAtLeast(1)
        player.clearsAfterDetached = true
        player.callback = object : SVGACallback {
            override fun onFinished() { currentOnFinished.value?.invoke() }
            override fun onPause() = Unit
            override fun onRepeat() = Unit
            override fun onStep(frame: Int, percentage: Double) = Unit
        }

        val parser = SVGAParser(context)
        val stream = try {
            context.resources.openRawResource(resourceId)
        } catch (_: Exception) {
            currentOnError.value?.invoke()
            null
        }

        if (stream != null) {
            parser.decodeFromInputStream(
                stream,
                "android.resource://${context.packageName}/$resourceId",
                object : SVGAParser.ParseCompletion {
                    override fun onComplete(videoItem: SVGAVideoEntity) {
                        player.post {
                            player.setVideoItem(videoItem)
                            player.startAnimation()
                        }
                    }
                    override fun onError() { currentOnError.value?.invoke() }
                },
                true
            )
        }

        onDispose {
            player.stopAnimation(true)
            player.callback = null
            player.clear()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            player.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = {}
    )
}
