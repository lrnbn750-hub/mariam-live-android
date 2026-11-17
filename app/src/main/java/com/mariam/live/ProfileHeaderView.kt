package com.mariam.live

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.airbnb.lottie.LottieAnimationView
import android.widget.TextView

/**
 * Dedicated profile header. Layout direction is fixed LTR for stable avatar placement;
 * Arabic text views keep their own RTL content direction.
 * Public API is unchanged (same properties and setters); [settingsView] and [supportView]
 * are now icon buttons, so they are typed as View (setOnClickListener works exactly the same).
 */
class ProfileHeaderView(context: Context) : LinearLayout(context) {
    val avatarView: ImageView
    val editAvatarView: ImageView
    val nameView: TextView
    val userIdView: TextView
    val levelBadgeView: LevelBadgeView
    val levelView: LevelBadgeView
    val copyView: ImageView
    val fansCountView: TextView
    val followingCountView: TextView
    val fansCell: View
    val followingCell: View
    val settingsView: View
    val supportView: View

    /** Animated avatar frame. `avatarFrame.frameEnabled = false` hides it. */
    val avatarFrame: AvatarFrameView
    private lateinit var vipBadgeView: ImageView
    private lateinit var vipFrameOverlay: ImageView
    private lateinit var vipAppearOverlay: LottieAnimationView
    private var renderedVipLevel = -1

    init {
        orientation = VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        clipChildren = false
        clipToPadding = false
        background = HeroBackdropDrawable(context)
        LayoutInflater.from(context).inflate(R.layout.view_profile_header, this, true)
        avatarView = findViewById(R.id.profileHeaderAvatar)
        editAvatarView = findViewById(R.id.profileHeaderEditAvatar)
        nameView = findViewById(R.id.profileHeaderName)
        vipBadgeView = findViewById(R.id.profileHeaderVipBadge)
        userIdView = findViewById(R.id.profileHeaderUserId)
        levelBadgeView = findViewById(R.id.profileLevelBadge)
        levelView = levelBadgeView
        copyView = findViewById(R.id.profileHeaderCopy)
        fansCountView = findViewById(R.id.profileHeaderFansCount)
        followingCountView = findViewById(R.id.profileHeaderFollowingCount)
        fansCell = findViewById(R.id.profileHeaderFansCell)
        followingCell = findViewById(R.id.profileHeaderFollowingCell)
        settingsView = findViewById(R.id.profileHeaderSettings)
        supportView = findViewById(R.id.profileHeaderSupport)
        avatarFrame = findViewById(R.id.profileHeaderAvatarSlot)
        vipFrameOverlay = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = "إطار VIP"
            visibility = View.GONE
        }
        // Keep the avatar below the VIP artwork and the edit control above it.
        avatarFrame.addView(vipFrameOverlay, 1, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        vipAppearOverlay = LottieAnimationView(context).apply {
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            repeatCount = 0
        }
        avatarFrame.addView(vipAppearOverlay, 2, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        editAvatarView.bringToFront()

        // Tapping the whole ID chip copies the ID (uses the listener set on copyView).
        findViewById<View>(R.id.profileHeaderIdChip).setOnClickListener { copyView.performClick() }
    }

    /** Accepts the raw public ID only. The ID: prefix is provided by the XML label. */
    fun setUserIdText(rawUserId: String) {
        userIdView.text = rawUserId.replace(Regex("^ID\\s*:?\\s*", RegexOption.IGNORE_CASE), "").trim().ifBlank { "غير متوفر" }
    }

    fun setUserName(name: String) {
        nameView.text = name.ifBlank { "مستخدم BAK LIVE" }
    }

    fun setLevel(level: Int) {
        levelBadgeView.level = level
    }

    fun setLevelText(level: Int) {
        setLevel(level)
    }

    /** Applies the matching VIP 1..12 frame without replacing the user's avatar. */
    fun setVipLevel(level: Int) {
        val safeLevel = level.coerceIn(0, 12)
        val badgeResource = if (safeLevel == 0) 0 else VipAssetResources.badge(context, safeLevel)
        if (badgeResource != 0) {
            vipBadgeView.setImageResource(badgeResource)
            vipBadgeView.visibility = View.VISIBLE
        } else {
            vipBadgeView.setImageDrawable(null)
            vipBadgeView.visibility = View.GONE
        }
        val resource = if (safeLevel == 0) 0 else VipAssetResources.frame(context, safeLevel)
        if (resource == 0) {
            vipFrameOverlay.setImageDrawable(null)
            vipFrameOverlay.visibility = View.GONE
            vipAppearOverlay.cancelAnimation()
            vipAppearOverlay.visibility = View.GONE
            avatarFrame.frameEnabled = true
        } else {
            vipFrameOverlay.setImageResource(resource)
            vipFrameOverlay.visibility = View.VISIBLE
            avatarFrame.frameEnabled = false
            if (renderedVipLevel != safeLevel) {
                val appearResource = VipAssetResources.appear(context, safeLevel)
                if (appearResource != 0) {
                    vipAppearOverlay.setAnimation(appearResource)
                    vipAppearOverlay.visibility = View.VISIBLE
                    vipAppearOverlay.progress = 0f
                    vipAppearOverlay.playAnimation()
                }
                renderedVipLevel = safeLevel
            }
        }
    }

    fun setFansCount(value: String) { fansCountView.text = value }
    fun setFollowingCount(value: String) { followingCountView.text = value }
}
