package com.mariam.live

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Fully native Kotlin profile screen (no WebView). UI redesign only:
 * all API calls, data parsing, wallet / level values, navigation targets and the avatar
 * upload flow are identical to the previous version.
 */
class ProfileActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(5)
    private val photoPickerCode = 7421
    private lateinit var page: LinearLayout
    private lateinit var header: ProfileHeaderView
    private lateinit var avatar: ImageView
    private lateinit var coinValue: TextView
    private lateinit var diamondValue: TextView
    private lateinit var visitorValue: TextView
    private lateinit var friendValue: TextView
    private var profileData = JSONObject()
    private var publicId = ""
    private var userId = ""
    private var requestedProfileUserId = ""
    private var isOwnProfile = true
    private lateinit var adminEntry: LinearLayout
    private var publicAvatar: ImageView? = null
    private var publicName: TextView? = null
    private var publicIdView: TextView? = null
    private var publicLevel: TextView? = null
    private var publicFans: TextView? = null
    private var publicFollowing: TextView? = null
    private var publicSent: TextView? = null
    private var publicReceived: TextView? = null
    private var publicFollowButton: TextView? = null
    private var publicGender: TextView? = null
    private var publicLocation: TextView? = null
    private var publicFriendsButton: LinearLayout? = null
    private var publicFriendsLabel: TextView? = null
    private var publicInfoBox: LinearLayout? = null
    private var publicFriendsCount = -1
    private var isFollowingPublic = false

    private val ink = Color.rgb(20, 24, 34)
    private val muted = Color.rgb(143, 151, 164)
    private val border = Color.rgb(238, 240, 244)
    private val white = Color.WHITE
    private val pageBg = Color.rgb(243, 244, 250)
    private val heroTop = Color.rgb(20, 10, 48)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        window.statusBarColor = heroTop              // same color as the top of the hero gradient
        window.navigationBarColor = pageBg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR // light status icons on the dark hero
        requestedProfileUserId = intent.getStringExtra("profile_user_id").orEmpty()
        isOwnProfile = requestedProfileUserId.isBlank()
        buildNativePage()
        if (!isOwnProfile && ::header.isInitialized) {
            header.editAvatarView.visibility = View.GONE
            header.settingsView.visibility = View.GONE
            header.supportView.visibility = View.GONE
        }
        loadProfileData()
    }

    // ------------------------------------------------------------------ small UI helpers
    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, radius: Float, stroke: Int = border): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1f), stroke)
        }

    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = true
        }

    /** Touch ripple. [radius] = 0 gives a rectangular ripple (used for list rows). */
    private fun ripple(content: Drawable?, radius: Float): Drawable {
        val mask: Drawable = if (radius > 0f)
            GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(radius).toFloat() }
        else ColorDrawable(Color.WHITE)
        return RippleDrawable(ColorStateList.valueOf(Color.argb(34, 90, 60, 200)), content, mask)
    }

    /** Subtle press-scale feedback; does not consume the touch so click listeners keep working. */
    @Suppress("ClickableViewAccessibility")
    private fun View.pressable() {
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
    }

    private fun cardBackground(radius: Float): GradientDrawable = rounded(white, radius, Color.rgb(236, 238, 246))

    private fun formatNumber(v: Long): String = NumberFormat.getInstance(Locale.US).format(v)

    // ------------------------------------------------------------------ page
    private fun buildNativePage() {
        if (!isOwnProfile) {
            buildPublicProfilePage()
            return
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(pageBg)
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
            clipChildren = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(28f))
        }
        scroll.addView(page, ViewGroup.LayoutParams(-1, -2))

        // Keep the same bottom navigation visible on both Home and Profile.
        val root = FrameLayout(this)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        val navWrap = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            clipChildren = false
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(255, 247, 224), Color.rgb(247, 210, 125))).apply {
                cornerRadii = floatArrayOf(dp(24f).toFloat(), dp(24f).toFloat(), dp(24f).toFloat(), dp(24f).toFloat(), 0f, 0f, 0f, 0f)
                setStroke(dp(1f), 0x88E6B450.toInt())
            }
            setPadding(dp(6f), dp(4f), dp(6f), 0)
            elevation = dp(8f).toFloat()
        }
        fun navItem(kind: String, title: String, selected: Boolean, action: () -> Unit) =
            FrameLayout(this).apply {
                isClickable = true
                isFocusable = true
                contentDescription = title
                val column = LinearLayout(this@ProfileActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(HomeActivity.NavIcon(this@ProfileActivity, kind, selected),
                        LinearLayout.LayoutParams(dp(30f), dp(30f)))
                    addView(TextView(this@ProfileActivity).apply {
                        text = title
                        textSize = 10.5f
                        gravity = Gravity.CENTER
                        setSingleLine(true)
                        setTextColor(if (selected) Color.rgb(150, 88, 10) else Color.rgb(160, 124, 66))
                        typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    }, LinearLayout.LayoutParams(-1, dp(16f)))
                }
                addView(column, FrameLayout.LayoutParams(-1, -1))
                setOnClickListener { action() }
            }
        bar.addView(navItem("home", "الرئيسية", false) {
            startActivity(Intent(this@ProfileActivity, HomeActivity::class.java))
            finish()
        }, LinearLayout.LayoutParams(0, -1, 1f))
        bar.addView(navItem("discover", "اكتشف", false) {
            startActivity(Intent(this@ProfileActivity, HomeActivity::class.java).putExtra("open_discover", true))
            finish()
        }, LinearLayout.LayoutParams(0, -1, 1f))
        val createSlot = FrameLayout(this)
        val halo = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x66FFF1C4)
            }
        }
        createSlot.addView(halo, FrameLayout.LayoutParams(dp(76f), dp(76f), Gravity.CENTER))
        val createButton = HomeActivity.CreateRoomButton(this).apply {
            contentDescription = "إنشاء غرفة"
            isClickable = true
            elevation = dp(8f).toFloat()
            setOnClickListener { startActivity(Intent(this@ProfileActivity, RoomSetupActivity::class.java)) }
        }
        createSlot.addView(createButton, FrameLayout.LayoutParams(dp(62f), dp(62f), Gravity.CENTER))
        android.animation.ObjectAnimator.ofFloat(createButton, View.SCALE_X, 1f, 1.055f).apply {
            duration = 1100; repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE; start()
        }
        android.animation.ObjectAnimator.ofFloat(createButton, View.SCALE_Y, 1f, 1.055f).apply {
            duration = 1100; repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE; start()
        }
        android.animation.ObjectAnimator.ofFloat(halo, View.ALPHA, 0.5f, 1f).apply {
            duration = 1100; repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE; start()
        }
        bar.addView(createSlot, LinearLayout.LayoutParams(0, -1, 1.25f))
        bar.addView(navItem("messages", "رسائلي", false) {
            startActivity(Intent(this@ProfileActivity, MessagesActivity::class.java))
        }, LinearLayout.LayoutParams(0, -1, 1f))
        bar.addView(navItem("profile", "حسابي", true) {}, LinearLayout.LayoutParams(0, -1, 1f))
        navWrap.addView(bar, FrameLayout.LayoutParams(-1, dp(62f), Gravity.BOTTOM))
        root.addView(navWrap, FrameLayout.LayoutParams(-1, dp(86f), Gravity.BOTTOM))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(navWrap) { view, insets ->
            val bottomInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            view.layoutParams = view.layoutParams.apply { height = dp(86f) + bottomInset }
            bar.layoutParams = bar.layoutParams.apply { height = dp(62f) + bottomInset }
            bar.setPadding(dp(6f), dp(4f), dp(6f), bottomInset)
            page.setPadding(0, 0, 0, dp(110f) + bottomInset)
            insets
        }
        setContentView(root)
        androidx.core.view.ViewCompat.requestApplyInsets(navWrap)

        // ---- Hero header (avatar, name, ID, level, stats) -------------------------------
        header = ProfileHeaderView(this)
        header.layoutParams = LinearLayout.LayoutParams(-1, -2)
        avatar = header.avatarView
        avatar.clipToOutline = true
        header.editAvatarView.setOnClickListener { pickPhoto() }
        header.copyView.setOnClickListener { copyId() }
        header.settingsView.setOnClickListener { openAction("settings") }
        header.supportView.setOnClickListener { openAction("music") }
        header.levelBadgeView.setOnClickListener { openAction("level") }
        header.fansCell.setOnClickListener { openFollowList("followers") }
        header.followingCell.setOnClickListener { openFollowList("following") }
        header.friendsCell.setOnClickListener { openFollowList("friends") }
        header.settingsView.pressable()
        header.supportView.pressable()
        page.addView(header)
        // Edge-to-edge (Android 15+ ignores setDecorFitsSystemWindows): pad for status / navigation bars.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            header.setPadding(0, bars.top, 0, 0)
            page.setPadding(0, 0, 0, dp(28f) + bars.bottom)
            insets
        }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipChildren = false
            clipToPadding = false
            setPadding(dp(20f), dp(16f), dp(20f), 0)
        }
        page.addView(body, LinearLayout.LayoutParams(-1, -2))

        // ---- Wallet: earned diamonds (left) + charge coins (right), same positions as before ----
        val walletRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipChildren = false
        }
        val coins = WalletCardView(this, WalletKind.COINS, "عملات الشحن")
        val diamonds = WalletCardView(this, WalletKind.DIAMONDS, "الألماس المكتسب")
        coinValue = coins.amountView
        diamondValue = diamonds.amountView
        coins.setOnClickListener { openAction("coins") }
        diamonds.setOnClickListener { openAction("diamonds") }
        coins.pressable()
        diamonds.pressable()
        walletRow.addView(diamonds, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5f) })
        walletRow.addView(coins, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(5f) })
        body.addView(walletRow, LinearLayout.LayoutParams(-1, -2))

        // Premium membership system removed. Wallet and general store remain available.

        // ---- Shortcuts: family, store, my look, agency, host -------------------------------
        val shortcutCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = cardBackground(24f)
            elevation = dp(2f).toFloat()
            setPadding(dp(4f), dp(12f), dp(4f), dp(10f))
        }
        shortcutCard.addView(shortcut("مركز المضيف", R.drawable.ic_shortcut_host, Color.rgb(255, 154, 92), Color.rgb(240, 68, 122)) { openAction("host") }, weighted())
        shortcutCard.addView(shortcut("مركز الوكالة", R.drawable.ic_shortcut_agency, Color.rgb(62, 224, 176), Color.rgb(20, 163, 127)) { openAction("agency") }, weighted())
        shortcutCard.addView(shortcut("مظهري", R.drawable.ic_shortcut_look, Color.rgb(79, 211, 240), Color.rgb(31, 143, 214)) { openAction("look") }, weighted())
        shortcutCard.addView(shortcut("المتجر", R.drawable.ic_shortcut_store, Color.rgb(255, 123, 181), Color.rgb(224, 51, 138)) { openAction("store") }, weighted())
        shortcutCard.addView(shortcut("العائلة", R.drawable.ic_shortcut_family, Color.rgb(166, 124, 255), Color.rgb(106, 63, 224)) { openAction("family") }, weighted())
        body.addView(shortcutCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12f) })

        // ---- Account settings & services ---------------------------------------------------
        val settingsTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(4f), dp(26f), dp(4f), dp(12f))
        }
        val accent = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(255, 201, 77), Color.rgb(40, 190, 145))
            ).apply { cornerRadius = dp(2f).toFloat() }
        }
        settingsTitle.addView(text("إعدادات وخدمات الحساب", 17f, ink, true).apply {
            textDirection = View.TEXT_DIRECTION_RTL
        })
        settingsTitle.addView(accent, LinearLayout.LayoutParams(dp(4f), dp(18f)).apply { marginStart = dp(8f) })
        body.addView(settingsTitle)

        val services = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = cardBackground(26f)
            elevation = dp(2f).toFloat()
            clipToOutline = true
        }
        serviceRow(services, "المساعدة والدعم", R.drawable.ic_profile_help_color, rounded(Color.rgb(226, 247, 243), 14f, Color.TRANSPARENT), dp(8f)) { openAction("help") }
        serviceRow(services, "الأمان والخصوصية", R.drawable.ic_profile_security_color, rounded(Color.rgb(230, 242, 255), 14f, Color.TRANSPARENT), dp(8f), last = true) { openAction("privacy") }
        body.addView(services, LinearLayout.LayoutParams(-1, -2))

        // لا نعرض قسم الإدارة إلا بعد تأكيد الصلاحية من الخادم.
        val adminCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = cardBackground(26f)
            elevation = dp(2f).toFloat()
            clipToOutline = true
            visibility = View.GONE
        }
        serviceRow(
            adminCard,
            "لوحة الإدارة والتحكم",
            R.drawable.ic_profile_security_color,
            rounded(Color.rgb(245, 238, 255), 14f, Color.TRANSPARENT),
            dp(8f),
            last = true
        ) { startActivity(Intent(this, AdminActivity::class.java)) }
        body.addView(adminCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14f) })
        adminEntry = adminCard

        animateIn(listOf(walletRow, shortcutCard, settingsTitle, services))
    }

    private fun animateIn(views: List<View>) {
        views.forEachIndexed { i, v ->
            v.translationY = dp(16f).toFloat()
            v.animate().translationY(0f)
                .setStartDelay(70L * i).setDuration(380L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun weighted() = LinearLayout.LayoutParams(0, -2, 1f)

    private fun shortcut(label: String, iconRes: Int, colorA: Int, colorB: Int, action: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2f), dp(6f), dp(2f), dp(6f))
            isClickable = true
            isFocusable = true
            contentDescription = label
            background = ripple(null, 16f)
            setOnClickListener { action() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(colorA, colorB))
                .apply { cornerRadius = dp(15f).toFloat() }
            setPadding(dp(11f), dp(11f), dp(11f), dp(11f))
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(46f), dp(46f)))
        item.addView(text(label, 12f, ink, true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(0, dp(7f), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        item.pressable()
        return item
    }

    /** Adds one service row (RTL: icon on the right, label, chevron on the left) and returns it. */
    private fun serviceRow(
        parent: LinearLayout, label: String, iconRes: Int, tile: Drawable, iconPadding: Int,
        last: Boolean = false, action: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            isClickable = true
            isFocusable = true
            background = ripple(null, 0f)
            setOnClickListener { action() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = tile
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        }
        // visual order (LTR container, Arabic look): chevron | label | icon  -> icon on the right
        row.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_profile_chevron)
            setColorFilter(Color.rgb(177, 185, 195))
        }, LinearLayout.LayoutParams(dp(22f), dp(22f)))
        row.addView(text(label, 15f, ink, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
        }, LinearLayout.LayoutParams(0, dp(40f), 1f).apply { marginEnd = dp(12f) })
        row.addView(icon, LinearLayout.LayoutParams(dp(40f), dp(40f)))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        if (!last) {
            val divider = View(this).apply { setBackgroundColor(border) }
            parent.addView(divider, LinearLayout.LayoutParams(-1, dp(1f)).apply {
                marginEnd = dp(66f)
                marginStart = dp(14f)
            })
        }
        return row
    }


    private fun buildPublicProfilePage() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(Color.WHITE)
        }
        // Keep the photo below the status bar and the bottom bar above the navigation bar.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val sub = Color.rgb(120, 128, 142)

        // ---- Hero photo ----
        val hero = FrameLayout(this)
        publicAvatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(226, 229, 238))
        }
        hero.addView(publicAvatar, FrameLayout.LayoutParams(-1, -1))
        val back = text("›", 30f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setShadowLayer(6f, 0f, 2f, 0xAA000000.toInt())
            setOnClickListener { finish() }
        }
        hero.addView(back, FrameLayout.LayoutParams(dp(44f), dp(44f), Gravity.TOP or Gravity.RIGHT).apply {
            topMargin = dp(8f); rightMargin = dp(6f)
        })
        val menu = text("•••", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setShadowLayer(6f, 0f, 2f, 0xAA000000.toInt())
            setOnClickListener { showPublicActions() }
        }
        hero.addView(menu, FrameLayout.LayoutParams(dp(44f), dp(44f), Gravity.TOP or Gravity.LEFT).apply {
            topMargin = dp(8f); leftMargin = dp(8f)
        })
        content.addView(hero, LinearLayout.LayoutParams(-1, resources.displayMetrics.widthPixels * 9 / 10))

        // ---- Name + gender ----
        val nameRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), 0)
        }
        publicGender = text("♂", 11f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(84, 170, 235), 10f, Color.TRANSPARENT)
        }
        nameRow.addView(publicGender, LinearLayout.LayoutParams(dp(30f), dp(20f)).apply { marginEnd = dp(8f) })
        publicName = text("مستخدم BAK LIVE", 22f, ink, true).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            setSingleLine(true)
        }
        nameRow.addView(publicName, LinearLayout.LayoutParams(-2, -2))
        content.addView(nameRow)

        // ---- Location | ID ----
        val idRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(6f), dp(16f), 0)
        }
        publicLocation = text("", 13f, sub).apply { visibility = View.GONE; textDirection = View.TEXT_DIRECTION_RTL }
        idRow.addView(publicLocation)
        idRow.addView(text("⧉", 13f, sub).apply { setOnClickListener { copyId() }; setPadding(0, 0, dp(5f), 0) })
        publicIdView = text("ID: —", 14f, sub).apply { setOnClickListener { copyId() } }
        idRow.addView(publicIdView)
        content.addView(idRow)

        // ---- Stats ----
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(15f), dp(12f), dp(12f))
            setBackgroundColor(Color.WHITE)
        }
        fun statPair(caption: String, setter: (TextView) -> Unit, onClick: (() -> Unit)?): View {
            val pair = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.CENTER
                minimumWidth = dp(1f)
                if (onClick != null) {
                    isClickable = true
                    isFocusable = true
                    background = ripple(null, 8f)
                    setOnClickListener { onClick() }
                }
            }
            val value = text("—", 20f, ink, true).apply {
                setSingleLine(true)
                includeFontPadding = false
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            }
            setter(value)
            pair.addView(text(caption, 13f, sub, false).apply {
                textDirection = View.TEXT_DIRECTION_RTL
                setSingleLine(true)
                includeFontPadding = false
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }, LinearLayout.LayoutParams(-2, -2))
            pair.addView(value, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(5f) })
            return pair
        }
        // RTL order: fans are the first item and therefore sit at the far right, like the reference.
        stats.addView(statPair("معجبين", { publicFans = it }) { openFollowList("followers", userId) },
            LinearLayout.LayoutParams(0, dp(44f), 1.05f))
        stats.addView(statPair("متابعون", { publicFollowing = it }) { openFollowList("following", userId) },
            LinearLayout.LayoutParams(0, dp(44f), 1f))
        stats.addView(statPair("Beans", { publicReceived = it }, null), LinearLayout.LayoutParams(0, dp(44f), 1.1f))
        stats.addView(statPair("الماس", { publicSent = it }, null), LinearLayout.LayoutParams(0, dp(44f), 0.9f))
        content.addView(stats)

        // ---- Level badge ----
        val badges = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(6f), dp(16f), dp(14f))
        }
        publicLevel = text("◆  1", 12f, Color.rgb(70, 100, 140), true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(222, 236, 250), 12f, Color.TRANSPARENT)
            setPadding(dp(11f), dp(3f), dp(11f), dp(3f))
        }
        badges.addView(publicLevel, LinearLayout.LayoutParams(-2, -2))
        content.addView(badges)
        content.addView(View(this).apply { setBackgroundColor(Color.rgb(246, 247, 250)) }, LinearLayout.LayoutParams(-1, dp(8f)))

        // ---- Tab ----
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.RIGHT
            setPadding(dp(16f), dp(14f), dp(16f), 0)
        }
        val tabCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        tabCol.addView(text("الملف الشخصي", 18f, ink, true).apply { textDirection = View.TEXT_DIRECTION_RTL },
            LinearLayout.LayoutParams(-2, -2))
        tabCol.addView(View(this).apply { setBackgroundColor(ink) },
            LinearLayout.LayoutParams(dp(16f), dp(2f)).apply { topMargin = dp(5f) })
        tabRow.addView(tabCol, LinearLayout.LayoutParams(-2, -2))
        content.addView(tabRow)

        // ---- Basic info ----
        content.addView(text("معلومات اساسية", 18f, ink, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(16f), dp(18f), dp(16f), dp(10f))
        }, LinearLayout.LayoutParams(-1, -2))
        publicInfoBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), 0, dp(16f), dp(20f))
        }
        content.addView(publicInfoBox, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---- Bottom bar: chat | follow (star) | friends ----
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            elevation = dp(6f).toFloat()
        }
        val chat = text("💬  دردشة", 14f, ink, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 23f, Color.rgb(200, 204, 212))
            setOnClickListener { Toast.makeText(this@ProfileActivity, "المراسلة غير مربوطة بعد", Toast.LENGTH_SHORT).show() }
            pressable()
        }
        val gold = Color.rgb(247, 181, 0)
        val star = text("☆", 22f, gold, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 23f, gold)
            setOnClickListener { onPublicStarClick() }
            pressable()
        }
        publicFollowButton = star
        val friendsBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(240, 241, 246), 23f, Color.TRANSPARENT)
            isClickable = true
            setOnClickListener { openFollowList("friends", userId) }
            pressable()
        }
        publicFriendsLabel = text("⇄  الأصدقاء", 14f, Color.rgb(110, 116, 130), true).apply { gravity = Gravity.CENTER }
        friendsBtn.addView(publicFriendsLabel, LinearLayout.LayoutParams(-2, -2))
        publicFriendsButton = friendsBtn
        bar.addView(chat, LinearLayout.LayoutParams(0, dp(46f), 1f))
        bar.addView(star, LinearLayout.LayoutParams(dp(46f), dp(46f)).apply { setMargins(dp(10f), 0, dp(10f), 0) })
        bar.addView(friendsBtn, LinearLayout.LayoutParams(0, dp(46f), 1f))
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        androidx.core.view.ViewCompat.requestApplyInsets(root)
    }

    private fun onPublicStarClick() {
        if (isFollowingPublic) {
            val bmp = (publicAvatar?.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            showUnfollowDialog(this, publicName?.text?.toString().orEmpty(), bmp) { togglePublicFollow() }
        } else togglePublicFollow()
    }

    private fun updatePublicFollowUi() {
        publicFollowButton?.text = if (isFollowingPublic) "★" else "☆"
        publicFriendsLabel?.text = "⇄  الأصدقاء" + if (publicFriendsCount >= 0) "  $publicFriendsCount" else ""
    }

    private fun addInfoChip(label: String) {
        val chip = text(label, 13f, ink).apply {
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            background = rounded(Color.WHITE, 22f, Color.rgb(214, 217, 224))
            setPadding(dp(16f), dp(9f), dp(16f), dp(9f))
        }
        publicInfoBox?.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.RIGHT; bottomMargin = dp(8f) })
    }

    private fun compactPublicCount(value: Long): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0).replace(".0M", "M")
        value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0).replace(".0K", "K")
        else -> value.toString()
    }

    private fun showPublicActions() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val r = dp(24f).toFloat()
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(dp(28f), dp(8f), dp(28f), dp(12f))
        }
        fun item(label: String, color: Int, onClick: () -> Unit) {
            sheet.addView(text(label, 17f, color).apply {
                gravity = Gravity.CENTER
                setOnClickListener { dialog.dismiss(); onClick() }
            }, LinearLayout.LayoutParams(-1, dp(62f)))
        }
        item("الإشعارات مغلقة", ink) {
            Toast.makeText(this, "الإشعارات غير مربوطة بعد", Toast.LENGTH_SHORT).show()
        }
        item("إبلاغ", ink) { Toast.makeText(this, "الإبلاغ سيُربط بنظام البلاغات", Toast.LENGTH_SHORT).show() }
        item("حظر", Color.RED) {
            android.app.AlertDialog.Builder(this).setMessage("هل تريد حظر هذا المستخدم؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حظر") { _, _ ->
                    Toast.makeText(this, "الحظر سيُربط بخدمة الحظر", Toast.LENGTH_SHORT).show()
                }.show()
        }
        sheet.addView(View(this).apply { setBackgroundColor(Color.rgb(222, 224, 230)) }, LinearLayout.LayoutParams(-1, dp(1f)))
        item("Cancel", muted) {}
        dialog.setContentView(sheet)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.45f)
        }
        dialog.show()
    }

    // ------------------------------------------------------------------ data (unchanged logic)
    private fun loadProfileData() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrBlank()) { finish(); return }
        executor.execute {
            try {
                val me = AuthApi.get("/api/auth/me", token)
                if (me.status !in 200..299) throw Exception(me.body.optString("error", "تعذر تحميل الحساب"))
                val account = me.body.optJSONObject("user") ?: throw Exception("بيانات الحساب غير مكتملة")
                val ownUserId = account.optString("id")
                isOwnProfile = requestedProfileUserId.isBlank() || requestedProfileUserId == ownUserId
                userId = if (isOwnProfile) ownUserId else requestedProfileUserId
                publicId = if (isOwnProfile) account.optString("public_id") else ""
                if (userId.isBlank() || userId == "null") throw Exception("معرّف المستخدم غير متوفر")
                val details = AuthApi.get("/api/users/$userId", token)
                if (details.status !in 200..299) throw Exception(details.body.optString("error", "تعذر تحميل الملف الشخصي"))
                val user = details.body.optJSONObject("user") ?: JSONObject()
                if (!isOwnProfile) publicId = user.optString("public_id")
                val data = JSONObject()
                    .put("name", user.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("username", "مستخدم BAK LIVE"))
                    .put("id", publicId)
                    .put("level", 1)
                    .put("avatarUrl", user.optString("avatar_url"))
                    .put("visitors", JSONObject.NULL)
                    .put("publicFans", user.optLong("followers_count", 0L))
                    .put("publicFollowing", user.optLong("following_count", 0L))
                    .put("publicSent", user.optLong("diamonds_sent", 0L))
                    .put("publicReceived", user.optLong("diamonds_received", 0L))
                    .put("gender", user.optString("gender"))
                    .put("city", user.optString("city"))
                    .put("country", user.optString("country"))
                    .put("bio", user.optString("bio"))
                    .put("friends", 0)
                    .put("followers", 0)
                    .put("fans", 0)
                    .put("diamonds", 0)
                    .put("coins", 0)
                if (!isOwnProfile) data.put("level", user.optInt("level", 1))
                // These four requests are independent. Run them concurrently so the profile
                // does not wait for each network round-trip one after another.
                val levelFuture = if (isOwnProfile) executor.submit<AuthApi.Result?> { runCatching { AuthApi.get("/api/levels/me", token) }.getOrNull() } else null
                val followsFuture = if (isOwnProfile) executor.submit<AuthApi.Result?> { runCatching { AuthApi.get("/api/follows/me", token) }.getOrNull() } else null
                val walletFuture = if (isOwnProfile) executor.submit<AuthApi.Result?> { runCatching { AuthApi.get("/api/wallet", token) }.getOrNull() } else null
                val adminFuture = if (isOwnProfile) executor.submit<AuthApi.Result?> { runCatching { AuthApi.get("/api/admin/v2/me", token) }.getOrNull() } else null

                val level = levelFuture?.get()
                if (level != null && level.status in 200..299) {
                    data.put("level", level.body.optInt("level", 1).coerceIn(1, 100))
                }
                val follows = followsFuture?.get()
                if (follows != null && follows.status in 200..299) {
                    data.put("followers", follows.body.optInt("following_count", follows.body.optJSONArray("following")?.length() ?: 0))
                    data.put("fans", follows.body.optInt("followers_count", follows.body.optJSONArray("followers")?.length() ?: 0))
                    val followersArray = follows.body.optJSONArray("followers") ?: org.json.JSONArray()
                    val followingArray = follows.body.optJSONArray("following") ?: org.json.JSONArray()
                    val followingIds = (0 until followingArray.length()).mapNotNull { i ->
                        val o = followingArray.optJSONObject(i) ?: return@mapNotNull null
                        listOf("id", "user_id", "following_id", "follower_id").firstNotNullOfOrNull { key ->
                            o.optString(key).takeIf { it.isNotBlank() && it != "null" }
                        }
                    }.toSet()
                    val followerIds = (0 until followersArray.length()).mapNotNull { i ->
                        val o = followersArray.optJSONObject(i) ?: return@mapNotNull null
                        listOf("id", "user_id", "following_id", "follower_id").firstNotNullOfOrNull { key ->
                            o.optString(key).takeIf { it.isNotBlank() && it != "null" }
                        }
                    }.toSet()
                    data.put("friends", followingIds.intersect(followerIds).size)
                }
                val wallet = walletFuture?.get()
                if (wallet != null && wallet.status in 200..299) {
                    val w = wallet.body.optJSONObject("wallet") ?: wallet.body
                    data.put("coins", w.optLong("recharge_coins", 0L))
                    data.put("diamonds", w.optLong("earned_diamonds", 0L))
                }
                val adminCheck = adminFuture?.get()
                val isAdmin = adminCheck != null && adminCheck.status in 200..299 &&
                    adminCheck.body.optBoolean("success", false) &&
                    adminCheck.body.optJSONObject("admin") != null
                profileData = data
                runOnUiThread {
                    bindProfile(data, token)
                    if (::adminEntry.isInitialized) adminEntry.visibility = if (isOwnProfile && isAdmin) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("BAKProfileNative", "Profile load failed", e)
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر تحميل بيانات الملف الشخصي", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun bindProfile(data: JSONObject, token: String) {
        if (!isOwnProfile) {
            publicName?.text = data.optString("name")
            publicIdView?.text = "ID: " + publicId
            publicLevel?.text = "◆  " + data.optInt("level", 1)
            publicFans?.text = compactPublicCount(data.optLong("publicFans", 0L))
            publicFollowing?.text = compactPublicCount(data.optLong("publicFollowing", 0L))
            publicSent?.text = compactPublicCount(data.optLong("publicSent", 0L))
            publicReceived?.text = compactPublicCount(data.optLong("publicReceived", 0L))
            fun clean(k: String) = data.optString(k).takeIf { it.isNotBlank() && it != "null" } ?: ""
            val female = clean("gender").lowercase() in setOf("female", "f", "أنثى", "انثى", "2")
            publicGender?.text = if (female) "♀" else "♂"
            publicGender?.background = rounded(if (female) Color.rgb(240, 110, 170) else Color.rgb(84, 170, 235), 12f, Color.TRANSPARENT)
            val loc = listOf(clean("city"), clean("country")).filter { it.isNotEmpty() }.joinToString("، ")
            if (loc.isNotEmpty()) {
                publicLocation?.text = loc + "   |   "
                publicLocation?.visibility = View.VISIBLE
            }
            publicInfoBox?.removeAllViews()
            if (loc.isNotEmpty()) addInfoChip("📍  $loc")
            if (clean("bio").isNotEmpty()) addInfoChip(clean("bio"))
            executor.execute {
                try {
                    val status = AuthApi.get("/api/follows/status/$userId", token)
                    if (status.status in 200..299) {
                        isFollowingPublic = status.body.optBoolean("following", false)
                        runOnUiThread { updatePublicFollowUi() }
                    }
                } catch (_: Exception) {}
            }
            executor.execute {
                val lists = FollowApi.load(userId, token)
                if (lists != null) runOnUiThread {
                    publicFriendsCount = lists.friends.size
                    updatePublicFollowUi()
                }
            }
            val url = normalizeAvatar(data.optString("avatarUrl"))
            if (url.isNotBlank()) executor.execute {
                try {
                    val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null) runOnUiThread { if (!isFinishing) publicAvatar?.setImageBitmap(bitmap) }
                } catch (e: Exception) { Log.w("BAKPublicProfile", "Avatar load failed", e) }
            }
            return
        }
        header.setUserName(data.optString("name"))
        header.setUserIdText(publicId)
        header.setLevel(data.optInt("level", 1))
        header.setFansCount(data.optInt("fans", 0).toString())
        header.setFollowingCount(data.optInt("followers", 0).toString())
        header.findViewById<android.widget.TextView>(R.id.profileHeaderFriendsCount).text = data.optInt("friends", 0).toString()
        coinValue.text = formatNumber(data.optLong("coins", 0))
        diamondValue.text = formatNumber(data.optLong("diamonds", 0))
        val avatarUrl = normalizeAvatar(data.optString("avatarUrl"))
        if (avatarUrl.isNotBlank()) loadAvatar(avatarUrl, token)
    }

    private fun normalizeAvatar(value: String): String {
        if (value.isBlank() || value == "null") return ""
        return if (value.startsWith("http://") || value.startsWith("https://")) value
        else "https://nokiraq.duckdns.org/" + value.trimStart('/')
    }

    private fun loadAvatar(url: String, token: String) {
        executor.execute {
            try {
                val connection = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 15000; readTimeout = 20000; instanceFollowRedirects = true
                    setRequestProperty("Accept", "image/*")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("User-Agent", "BAK-LIVE-Android")
                }
                val bitmap = try {
                    val status = connection.responseCode
                    if (status !in 200..299) throw Exception("Avatar HTTP $status")
                    val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty()
                    if (!mime.startsWith("image/")) throw Exception("Avatar response is not image: $mime")
                    val bytes = connection.inputStream.use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw Exception("Unsupported avatar image")
                } finally { connection.disconnect() }
                runOnUiThread { if (!isFinishing) avatar.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                Log.e("BAKProfileNative", "Avatar load failed: $url", e)
            }
        }
    }

    private fun copyId() {
        if (publicId.isBlank()) return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BAK ID", publicId))
        Toast.makeText(this, "تم نسخ المعرّف", Toast.LENGTH_SHORT).show()
    }

    private fun openAction(action: String) {
        when (action) {
            "settings", "privacy" -> startActivity(Intent(this, AccountSecurityActivity::class.java))
            "music" -> Toast.makeText(this, "الموسيقى", Toast.LENGTH_SHORT).show()
            "level" -> startActivity(Intent(this, LevelActivity::class.java))
            "diamonds", "coins" -> startActivity(Intent(this, WalletActivity::class.java))
            "look", "store" -> startActivity(Intent(this, InventoryActivity::class.java))
            "host" -> startActivity(Intent(this, CreatorCenterActivity::class.java))
            "agency" -> startActivity(Intent(this, AgencyActivity::class.java))
            "family" -> showInfo("العائلة")
            "help" -> showInfo("المساعدة والدعم")
            "post" -> showInfo("أنشر")
        }
    }

    private fun showInfo(title: String) {
        android.app.AlertDialog.Builder(this).setTitle(title)
            .setMessage("هذه الخدمة ستتوفر ضمن تطبيق BAK LIVE.")
            .setPositiveButton("حسناً", null).show()
    }

    private fun openFollowList(type: String, targetUserId: String = "") {
        startActivity(Intent(this, FollowListActivity::class.java)
            .putExtra("list_type", type)
            .putExtra("target_user_id", targetUserId))
    }

    private fun togglePublicFollow() {
        val target = userId
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (target.isBlank() || token.isNullOrBlank()) return
        publicFollowButton?.isEnabled = false
        executor.execute {
            try {
                val endpoint = "/api/follows/$target"
                val result = if (isFollowingPublic) AuthApi.delete(endpoint, token) else AuthApi.post(endpoint, JSONObject(), token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر تحديث المتابعة"))
                isFollowingPublic = !isFollowingPublic
                runOnUiThread {
                    updatePublicFollowUi()
                    publicFollowButton?.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    publicFollowButton?.isEnabled = true
                    Toast.makeText(this, e.message ?: "تعذر تحديث المتابعة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, photoPickerCode)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == photoPickerCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadPhoto(uri)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        Toast.makeText(this, "جارٍ رفع الصورة...", Toast.LENGTH_SHORT).show()
        executor.execute {
            try {
                val source = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("تعذر قراءة الصورة")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }) ?: throw Exception("صيغة الصورة غير مدعومة")
                val scaled = if (bitmap.width > 900 || bitmap.height > 900) {
                    val ratio = minOf(900f / bitmap.width, 900f / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
                        (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
                } else bitmap
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
                val bytes = output.toByteArray()
                if (bytes.size > 1536 * 1024) throw Exception("الصورة كبيرة، اختر صورة أصغر")
                val result = AuthApi.post("/api/profile/avatar",
                    JSONObject().put("avatar_base64", Base64.encodeToString(bytes, Base64.NO_WRAP)), token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر رفع الصورة"))
                val url = normalizeAvatar(result.body.optString("avatar_url"))
                profileData.put("avatarUrl", url)
                runOnUiThread {
                    loadAvatar(url, token)
                    Toast.makeText(this, "تم تحديث الصورة الشخصية", Toast.LENGTH_SHORT).show()
                }
                if (scaled !== bitmap) scaled.recycle()
                if (bitmap !== scaled) bitmap.recycle()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "تعذر رفع الصورة", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
