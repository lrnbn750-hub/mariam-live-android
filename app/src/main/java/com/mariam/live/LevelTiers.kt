package com.mariam.live

import android.content.Context
import android.graphics.Path
import java.util.Locale

/** Single source of truth for the 10 wealth tiers, aligned with the existing LevelBadgeView palette. */
internal object LevelTiers {
    class Tier(
        val nameAr: String, val nameEn: String,
        val pillA: Int, val pillB: Int, val stroke: Int,
        val ringA: Int, val ringB: Int, val innerA: Int, val innerB: Int,
        val glyph: Int, val text: Int, val glow: Int, val shape: Int
    ) {
        fun displayName(context: Context): String =
            if (Locale.getDefault().language == "ar") nameAr else nameEn
    }

    val ALL = arrayOf(
        Tier("الفولاذي", "Steel", 0xFF7C8BA1.toInt(), 0xFF46546B.toInt(), 0xFFB9C6D8.toInt(), 0xFFF1F5FA.toInt(), 0xFF8895AA.toInt(), 0xFF5B6980.toInt(), 0xFF39445A.toInt(), 0xFFF4F8FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF9FB2CC.toInt(), 0),
        Tier("الزمردي", "Emerald", 0xFF35D58A.toInt(), 0xFF0F7A55.toInt(), 0xFF8FF7C2.toInt(), 0xFFE9FFF3.toInt(), 0xFF2BAA78.toInt(), 0xFF14946A.toInt(), 0xFF0A5E44.toInt(), 0xFFEFFFF7.toInt(), 0xFFFFFFFF.toInt(), 0xFF3EE6A0.toInt(), 0),
        Tier("الفيروزي", "Aqua", 0xFF26E0D0.toInt(), 0xFF0A7F95.toInt(), 0xFF9BFBF2.toInt(), 0xFFEAFFFD.toInt(), 0xFF1BB3C1.toInt(), 0xFF0E9FB0.toInt(), 0xFF065F73.toInt(), 0xFFF0FFFE.toInt(), 0xFFFFFFFF.toInt(), 0xFF38F0E0.toInt(), 1),
        Tier("الياقوتي", "Sapphire", 0xFF4C9AFF.toInt(), 0xFF1B3CC0.toInt(), 0xFFA9CCFF.toInt(), 0xFFEEF5FF.toInt(), 0xFF3E78E0.toInt(), 0xFF2D63D8.toInt(), 0xFF14288F.toInt(), 0xFFF2F7FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF5FA2FF.toInt(), 1),
        Tier("الجمشتي", "Amethyst", 0xFFA66BFF.toInt(), 0xFF5A1FB8.toInt(), 0xFFD6BAFF.toInt(), 0xFFF6EEFF.toInt(), 0xFF8B52E6.toInt(), 0xFF7A3FD6.toInt(), 0xFF431590.toInt(), 0xFFFAF4FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFB07BFF.toInt(), 2),
        Tier("الوردي", "Rose", 0xFFFF5CAD.toInt(), 0xFFB01270.toInt(), 0xFFFFBBDD.toInt(), 0xFFFFF0F8.toInt(), 0xFFE6469A.toInt(), 0xFFD12D88.toInt(), 0xFF85104F.toInt(), 0xFFFFF4FA.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF7BBF.toInt(), 2),
        Tier("الناري", "Flame", 0xFFFF9A45.toInt(), 0xFFD4202B.toInt(), 0xFFFFCFA3.toInt(), 0xFFFFF3E8.toInt(), 0xFFF2722F.toInt(), 0xFFE8501F.toInt(), 0xFFA3141C.toInt(), 0xFFFFF6EC.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF8A4A.toInt(), 3),
        Tier("الذهبي", "Gold", 0xFFFFE08A.toInt(), 0xFFC8870A.toInt(), 0xFFFFF3C2.toInt(), 0xFFFFFBE8.toInt(), 0xFFE9A91C.toInt(), 0xFFE3A317.toInt(), 0xFF9A6205.toInt(), 0xFFFFF9E0.toInt(), 0xFF4B2E00.toInt(), 0xFFFFD25C.toInt(), 4),
        Tier("البلاتيني", "Platinum", 0xFFF7FCFF.toInt(), 0xFF9CC2E8.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF8DB6E0.toInt(), 0xFF7FA9D8.toInt(), 0xFF4F78AE.toInt(), 0xFFFFFFFF.toInt(), 0xFF12304D.toInt(), 0xFFBFE3FF.toInt(), 5),
        Tier("الملكي", "Royal", 0xFF34175C.toInt(), 0xFF0C0615.toInt(), 0xFFFFD76A.toInt(), 0xFFFFF2B8.toInt(), 0xFFD9A62B.toInt(), 0xFF4A1E80.toInt(), 0xFF12071F.toInt(), 0xFFFFE17A.toInt(), 0xFFFFE28A.toInt(), 0xFFFFC94D.toInt(), 6)
    )

    fun indexOf(level: Int): Int = (level.coerceIn(1, 100) - 1) / 10
    fun firstLevel(tier: Int): Int = tier * 10 + 1
    fun lastLevel(tier: Int): Int = tier * 10 + 10
    fun alpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    fun blend(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(shift: Int): Int {
            val x = (a shr shift) and 0xFF
            val y = (b shr shift) and 0xFF
            return (x + (y - x) * k).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private val G_STAR = floatArrayOf(0f,0f,-1f,1f,0.247f,-0.34f,1f,0.951f,-0.309f,1f,0.399f,0.13f,1f,0.588f,0.809f,1f,0f,0.42f,1f,-0.588f,0.809f,1f,-0.399f,0.13f,1f,-0.951f,-0.309f,1f,-0.247f,-0.34f,3f)
    private val G_GEM = floatArrayOf(0f,-0.95f,-0.35f,1f,-0.55f,-0.9f,1f,0.55f,-0.9f,1f,0.95f,-0.35f,1f,0f,0.95f,3f)
    private val G_GEM_FACETS = floatArrayOf(0f,-0.95f,-0.35f,1f,0.95f,-0.35f,0f,-0.4f,-0.9f,1f,-0.2f,-0.35f,1f,0f,0.95f,0f,0.4f,-0.9f,1f,0.2f,-0.35f,1f,0f,0.95f)
    private val G_SHIELD = floatArrayOf(0f,0f,-1f,1f,0.85f,-0.65f,1f,0.85f,0.1f,2f,0.85f,0.65f,0f,1f,2f,-0.85f,0.65f,-0.85f,0.1f,1f,-0.85f,-0.65f,3f)
    private val G_BOLT = floatArrayOf(0f,0.25f,-1f,1f,-0.6f,0.15f,1f,-0.05f,0.15f,1f,-0.3f,1f,1f,0.65f,-0.2f,1f,0.05f,-0.2f,3f)
    private val G_CROWN = floatArrayOf(0f,-0.95f,0.75f,1f,-0.95f,-0.55f,1f,-0.45f,0f,1f,0f,-0.85f,1f,0.45f,0f,1f,0.95f,-0.55f,1f,0.95f,0.75f,3f)
    private val G_SPARKLE = floatArrayOf(0f,0f,-1f,2f,0.12f,-0.12f,1f,0f,2f,0.12f,0.12f,0f,1f,2f,-0.12f,0.12f,-1f,0f,2f,-0.12f,-0.12f,0f,-1f,3f)

    fun buildGlyph(ops: FloatArray, cx: Float, cy: Float, s: Float, out: Path) {
        out.reset()
        var i = 0
        while (i < ops.size) {
            when (ops[i].toInt()) {
                0 -> { out.moveTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s); i += 3 }
                1 -> { out.lineTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s); i += 3 }
                2 -> { out.quadTo(cx + ops[i + 1] * s, cy + ops[i + 2] * s, cx + ops[i + 3] * s, cy + ops[i + 4] * s); i += 5 }
                else -> { out.close(); i += 1 }
            }
        }
    }
}