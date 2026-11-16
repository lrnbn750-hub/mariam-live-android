package com.mariam.live

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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

    fun setFansCount(value: String) { fansCountView.text = value }
    fun setFollowingCount(value: String) { followingCountView.text = value }
}
