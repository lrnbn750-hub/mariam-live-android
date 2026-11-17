package com.mariam.live

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.util.Locale

/* ===================== Config (do not change rates) ===================== */
internal object VipConfig {
    const val FEE_RATE = 0.35          // حصة BAK LIVE
    const val BACK_RATE = 0.65         // تعود إلى محفظة المستخدم
    const val DURATION_DAYS = 30
    const val ELITE_MIN = 4
    const val MAX_LEVEL = 12
    const val USD_PER_1000 = 5.0       // usd = price / 1000 * 5

    /** الإهداء مخفي في النسخة الأصلية حتى يتوفر endpoint حقيقي في الخادم. */
    const val GIFT_ENABLED = false
}

internal object VipColors {
    val PAGE = Color.rgb(0x0F, 0x13, 0x1C)
    val INK = Color.rgb(0x16, 0x1B, 0x27)
    val DEEP = Color.rgb(0x1B, 0x21, 0x30)
    val PANEL = Color.rgb(0x23, 0x29, 0x38)
    val LINE = Color.rgb(0x3A, 0x41, 0x54)
    val TEXT = Color.rgb(0xE9, 0xEC, 0xF3)
    val MUTED = Color.rgb(0x7F, 0x88, 0x9B)
    val CREAM = Color.rgb(0xEF, 0xDF, 0xC0)
    val CREAM_D = Color.rgb(0xD6, 0xBD, 0x8C)
    val OK = Color.rgb(0x5F, 0xD6, 0xA4)
    val BAD = Color.rgb(0xFF, 0x7A, 0x86)
    val DIAMOND = Color.rgb(0x7F, 0xD8, 0xFF)
    val CIRC_BG = Color.rgb(0x26, 0x2C, 0x3B)
    val CIRC_BORDER = Color.rgb(0x56, 0x5E, 0x72)
    val ELITE_BG = Color.rgb(0x21, 0x27, 0x36)
    val SPLIT_BG = Color.rgb(0x20, 0x26, 0x35)
    val ROW_LINE = Color.rgb(0x2B, 0x32, 0x44)
    val BUY_BORDER = Color.rgb(0x2C, 0x33, 0x45)
    val HERO_END = Color.rgb(0x18, 0x1E, 0x2B)
    val CTA_TEXT = Color.rgb(0x2D, 0x24, 0x11)
    val VEIL = Color.argb(0xCC, 0x05, 0x08, 0x0D)
}

/* ===================== Data models ===================== */
internal data class VipPackage(val level: Int, val price: Long, val active: Boolean)

internal class VipSplit(val fee: Long, val back: Long)

internal fun vipSplit(price: Long): VipSplit {
    val fee = Math.round(price * VipConfig.FEE_RATE)
    return VipSplit(fee, price - fee)
}

internal fun vipUsd(price: Long): Double = price / 1000.0 * VipConfig.USD_PER_1000

/* ===================== Formatting (en-US like toLocaleString) ===================== */
internal fun fmtInt(n: Long): String = String.format(Locale.US, "%,d", n)
internal fun fmtUsd(v: Double): String = String.format(Locale.US, "%,.2f", v)

/* ===================== Tiers: [metal, gem, ribbon] ===================== */
internal class VipTier(metal: String, gem: String, ribbon: String) {
    val metal: Int = Color.parseColor(metal)
    val gem: Int = Color.parseColor(gem)
    val ribbon: Int = Color.parseColor(ribbon)
}

internal val VIP_TIERS = listOf(
    VipTier("#c98a54", "#ff9a4a", "#7a3b1a"),
    VipTier("#c3ccd8", "#5ec8ff", "#2c4a6b"),
    VipTier("#e9b94a", "#3ddc97", "#1f6b4a"),
    VipTier("#efc15a", "#4aa3ff", "#24407a"),
    VipTier("#f2c660", "#b36bff", "#5a2a8a"),
    VipTier("#f4cb68", "#ff5fa8", "#8a1f55"),
    VipTier("#f6d070", "#ff3b4a", "#9c1424"),
    VipTier("#f4d27a", "#a855ff", "#4b1a8a"),
    VipTier("#f8d67c", "#19d3da", "#0b5f6b"),
    VipTier("#fadb84", "#2ee59d", "#0f6b4a"),
    VipTier("#fcdf8c", "#ff8a1f", "#a33c06"),
    VipTier("#ffe9ad", "#7ee0ff", "#6a0f2a")
)

/** t > 0 يفتّح نحو الأبيض، t < 0 يغمّق نحو الأسود (نفس shade() في JS). */
internal fun shade(color: Int, t: Float): Int {
    val to = if (t > 0f) 255f else 0f
    val k = Math.abs(t)
    fun ch(v: Int): Int = Math.max(0, Math.min(255, Math.round(v + (to - v) * k)))
    return Color.rgb(ch(Color.red(color)), ch(Color.green(color)), ch(Color.blue(color)))
}

internal fun withAlpha(color: Int, a: Float): Int =
    Color.argb(Math.round(a * 255f).coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

/* ===================== Privileges ===================== */
internal enum class VipIcon {
    ENTRY, FRAME, VIP, FOLLOW, FRIENDS, CAR, SHIELD, BUBBLE, CARD, STAR, SEAT, SUPPORT, MASK, MUTE,
    GIFT, PALETTE, HIGHLIGHT, DOOR, EYEOFF, BOOST, SPARK, LOCK, DIAMOND, BACK
}

internal class VipPrivilege(val icon: VipIcon, val min: Int, val label: (Int) -> String)

internal val VIP_PRIVILEGES = listOf(
    VipPrivilege(VipIcon.ENTRY, 1) { "مؤثرات الدخول" },
    VipPrivilege(VipIcon.FRAME, 2) { "براويز مميزة" },
    VipPrivilege(VipIcon.VIP, 1) { n -> "أيقونة VIP$n" },
    VipPrivilege(VipIcon.FOLLOW, 1) { n -> "المتابعة *${n * 100}%" },
    VipPrivilege(VipIcon.FRIENDS, 2) { "مزيد من الاصدقاء" },
    VipPrivilege(VipIcon.CAR, 3) { "احصل/ي على هذه السيارة" },
    VipPrivilege(VipIcon.SUPPORT, 2) { "دعم متخصص" },
    VipPrivilege(VipIcon.BUBBLE, 3) { "فقاعات دردشة" },
    VipPrivilege(VipIcon.CARD, 3) { "بطاقة تعريف مميزة" },
    VipPrivilege(VipIcon.PALETTE, 2) { "ألوان دردشة مميزة" },
    VipPrivilege(VipIcon.HIGHLIGHT, 4) { "رسائل بارزة في الدردشة" },
    VipPrivilege(VipIcon.SHIELD, 3) { "منع الطرد" },
    VipPrivilege(VipIcon.MUTE, 3) { "منع الكتم من التعليقات" },
    VipPrivilege(VipIcon.STAR, 6) { "أولوية الظهور" },
    VipPrivilege(VipIcon.DOOR, 6) { "غرف VIP حصرية" },
    VipPrivilege(VipIcon.SEAT, 8) { "مقعد VIP في الغرفة" },
    VipPrivilege(VipIcon.EYEOFF, 8) { "زيارة خفية" },
    VipPrivilege(VipIcon.GIFT, 10) { "هدية حصرية" },
    VipPrivilege(VipIcon.BOOST, 10) { "تسريع رفع المستوى" },
    VipPrivilege(VipIcon.SPARK, 11) { "وصول مبكر للمزايا الجديدة" },
    VipPrivilege(VipIcon.FRAME, 12) { "إطار ملكي متحرك" }
)

/* ===================== UI helpers ===================== */
internal fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
internal fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density
internal fun Context.dp(v: Int): Int = dp(v.toFloat())

internal fun roundedBg(
    context: Context,
    fill: Int,
    radiusDp: Float,
    strokeColor: Int = 0,
    strokeDp: Float = 0f
): GradientDrawable = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = context.dpf(radiusDp)
    if (strokeDp > 0f) setStroke(Math.max(1, context.dpf(strokeDp).toInt()), strokeColor)
}

internal fun ovalBg(context: Context, fill: Int, strokeColor: Int, strokeDp: Float): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke(Math.max(1, context.dpf(strokeDp).toInt()), strokeColor)
    }