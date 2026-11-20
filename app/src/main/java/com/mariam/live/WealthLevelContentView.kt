package com.mariam.live

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Complete wealth screen body. Receives values from the existing authenticated level API. */
class WealthLevelContentView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val d = resources.displayMetrics.density
    private val hero: WealthLevelHeroView
    private val list: LinearLayout
    private val count: TextView
    private val rows = ArrayList<RankTierRowView>()
    var onTierClick: ((Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding((16 * d).toInt(), (8 * d).toInt(), (16 * d).toInt(), (28 * d).toInt())
        clipToPadding = false
        LayoutInflater.from(context).inflate(R.layout.view_wealth_level_content, this, true)
        hero = findViewById(R.id.wealthHero)
        list = findViewById(R.id.wealthTierList)
        count = findViewById(R.id.wealthPathCount)
        for (i in 0 until 10) {
            val row = RankTierRowView(context)
            row.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                if (i > 0) topMargin = (10 * d).toInt()
            }
            row.setOnClickListener { onTierClick?.invoke(i) }
            list.addView(row); rows.add(row)
        }
        count.text = context.getString(R.string.wealth_path_count, 10)
        if (isInEditMode) bind(100, 990040L, 1f, null)
    }

    fun bind(level: Int, totalXp: Long, progress: Float, remainingXp: Long? = null, animate: Boolean = true) {
        val safeLevel = level.coerceIn(1, 100)
        hero.bind(safeLevel, totalXp, progress, remainingXp, animate)
        rows.forEachIndexed { index, row -> row.bind(index, safeLevel) }
    }
}