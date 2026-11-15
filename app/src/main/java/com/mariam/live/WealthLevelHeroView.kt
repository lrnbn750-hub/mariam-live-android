package com.mariam.live

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Hero card showing the actual level, rank, total XP and progress supplied by the API. */
class WealthLevelHeroView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val d = resources.displayMetrics.density
    private val caption: TextView
    private val emblem: LevelEmblemView
    private val levelText: TextView
    private val rankText: TextView
    private val xpText: TextView
    private val percentText: TextView
    private val progress: LevelProgressBarView
    private val footer: TextView

    init {
        orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        setPadding((20 * d).toInt(), (18 * d).toInt(), (20 * d).toInt(), (20 * d).toInt())
        LayoutInflater.from(context).inflate(R.layout.view_wealth_level_hero, this, true)
        caption = findViewById(R.id.wealthCaption)
        emblem = findViewById(R.id.wealthEmblem)
        levelText = findViewById(R.id.wealthLevelText)
        rankText = findViewById(R.id.wealthRankText)
        xpText = findViewById(R.id.wealthXpText)
        percentText = findViewById(R.id.wealthPercentText)
        progress = findViewById(R.id.wealthProgress)
        footer = findViewById(R.id.wealthFooterText)
        if (isInEditMode) bind(100, 990040L, 1f, null)
    }

    fun bind(level: Int, totalXp: Long, progress: Float, remainingXp: Long? = null, animate: Boolean = true) {
        val safeLevel = level.coerceIn(1, 100)
        val tier = LevelTiers.ALL[LevelTiers.indexOf(safeLevel)]
        val royal = LevelTiers.indexOf(safeLevel) == 9
        val base = 0xFF0A1020.toInt()
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(LevelTiers.blend(base, tier.innerA, 0.34f), base)).apply {
            cornerRadius = 28f * d
            setStroke((1f * d).toInt().coerceAtLeast(1), LevelTiers.alpha(tier.stroke, 150))
        }
        caption.setTextColor(LevelTiers.alpha(tier.ringA, 235))
        emblem.level = safeLevel
        levelText.text = "LV $safeLevel"
        levelText.paint.shader = LinearGradient(0f, 0f, 0f, levelText.textSize,
            if (royal) 0xFFFFF3C0.toInt() else 0xFFFFFFFF.toInt(), tier.glow, Shader.TileMode.CLAMP)
        levelText.invalidate()
        rankText.text = context.getString(R.string.wealth_rank_format, tier.displayName(context))
        xpText.text = context.getString(R.string.wealth_xp_format, String.format(Locale.US, "%,d", totalXp))
        val safeProgress = progress.coerceIn(0f, 1f)
        percentText.text = "${(safeProgress * 100f).toInt()}%"
        this.progress.setLevel(safeLevel)
        this.progress.setProgress(safeProgress, animate)
        when {
            safeLevel >= 100 -> {
                footer.text = context.getString(R.string.wealth_max_reached)
                footer.setTextColor(tier.glow); footer.visibility = VISIBLE
            }
            remainingXp != null -> {
                footer.text = context.getString(R.string.wealth_remaining_format,
                    String.format(Locale.US, "%,d", remainingXp), safeLevel + 1)
                footer.setTextColor(tier.glow); footer.visibility = VISIBLE
            }
            else -> footer.visibility = GONE
        }
    }
}