package com.mariam.live

import android.view.ViewGroup
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

/**
 * Plays an SVGA gift animation bundled in app/src/main/assets.
 *
 * Example: app/src/main/assets/gifts/sample_gift.svga
 * Only bundle animation files that BAK LIVE is licensed to distribute.
 */
@Composable
fun SvgaGiftPlayer(
    assetPath: String,
    modifier: Modifier = Modifier,
    loops: Int = 1,
    onFinished: (() -> Unit)? = null,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val player = remember(context) { SVGAImageView(context) }
    val currentOnFinished = rememberUpdatedState(onFinished)
    val currentOnError = rememberUpdatedState(onError)

    DisposableEffect(player, assetPath, loops) {
        player.loops = loops.coerceAtLeast(1)
        player.clearsAfterDetached = true

        val parser = SVGAParser(context)
        parser.decodeFromAssets(assetPath, object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
                player.post {
                    player.setVideoItem(videoItem)
                    player.startAnimation()
                }
            }

            override fun onError() {
                currentOnError.value?.invoke()
            }
        })

        player.callback = object : SVGACallback {
            override fun onFinished() {
                currentOnFinished.value?.invoke()
            }

            override fun onPause() = Unit
            override fun onRepeat() = Unit
            override fun onStep(frame: Int, percentage: Double) = Unit
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
        update = { }
    )
}
