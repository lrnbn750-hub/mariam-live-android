package com.mariam.live

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.DrawableRes

/** Resource lookup helpers for BAK LIVE VIP levels 1..12. */
object VipAssetResources {
    private fun name(level: Int, suffix: String): String {
        require(level in 1..12) { "VIP level must be between 1 and 12" }
        return "vip_%02d_%s".format(level, suffix)
    }
    @DrawableRes fun frame(context: Context, level: Int): Int = drawable(context, level, "frame")
    @DrawableRes fun badge(context: Context, level: Int): Int = drawable(context, level, "badge")
    @DrawableRes fun icon(context: Context, level: Int): Int = drawable(context, level, "icon")
    @DrawableRes fun bubble(context: Context, level: Int): Int = drawable(context, level, "bubble")
    @DrawableRes fun particles(context: Context, level: Int): Int = drawable(context, level, "particles")
    private fun drawable(context: Context, level: Int, suffix: String): Int =
        context.resources.getIdentifier(name(level, suffix), "drawable", context.packageName)
    @RawRes fun entrance(context: Context, level: Int): Int = raw(context, level, "entrance")
    @RawRes fun appear(context: Context, level: Int): Int = raw(context, level, "appear")
    private fun raw(context: Context, level: Int, suffix: String): Int =
        context.resources.getIdentifier(name(level, suffix), "raw", context.packageName)
}
