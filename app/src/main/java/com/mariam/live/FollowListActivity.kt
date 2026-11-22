package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class FollowPerson(val id: String, val name: String, val avatarUrl: String, val level: Int)

/** Loads followers / following lists. Shared by FollowListActivity and ProfileActivity. */
object FollowApi {
    // Endpoints tried (in order) for ANOTHER user's lists. {id} = user id.
    // Each must return JSON with "followers" and "following" arrays, like /api/follows/me.
    // >>> If your backend uses different paths, edit this list. <<<
    private val OTHER_USER_ENDPOINTS = listOf("/api/follows/user/{id}", "/api/users/{id}/follows")

    class Lists(val followers: List<FollowPerson>, val following: List<FollowPerson>) {
        val friends: List<FollowPerson>
            get() {
                val ids = followers.map { it.id }.toSet()
                return following.filter { it.id in ids }
            }
    }

    fun normalizeAvatar(value: String): String {
        if (value.isBlank() || value == "null") return ""
        return if (value.startsWith("http://") || value.startsWith("https://")) value
        else "https://nokiraq.duckdns.org/" + value.trimStart('/')
    }

    private fun parse(arr: JSONArray?): List<FollowPerson> {
        if (arr == null) return emptyList()
        val out = ArrayList<FollowPerson>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val u = o.optJSONObject("user") ?: o
            val id = listOf("id", "user_id", "following_id", "follower_id").firstNotNullOfOrNull { k ->
                u.optString(k).takeIf { it.isNotBlank() && it != "null" }
            } ?: continue
            val name = listOf("display_name", "username", "name").firstNotNullOfOrNull { k ->
                u.optString(k).takeIf { it.isNotBlank() && it != "null" }
            } ?: "مستخدم BAK LIVE"
            val avatar = normalizeAvatar(u.optString("avatar_url").ifBlank { u.optString("avatarUrl") })
            out.add(FollowPerson(id, name, avatar, u.optInt("level", 0)))
        }
        return out
    }

    /** [targetUserId] blank = my own lists. Returns null when no endpoint worked. Call off the main thread. */
    fun load(targetUserId: String, token: String): Lists? {
        val paths = if (targetUserId.isBlank()) listOf("/api/follows/me")
        else OTHER_USER_ENDPOINTS.map { it.replace("{id}", targetUserId) }
        for (p in paths) {
            val r = runCatching { AuthApi.get(p, token) }.getOrNull() ?: continue
            if (r.status in 200..299) {
                return Lists(parse(r.body.optJSONArray("followers")), parse(r.body.optJSONArray("following")))
            }
        }
        return null
    }
}

class FollowListActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(4)
    private val cache = ConcurrentHashMap<String, Bitmap>()
    private lateinit var listBox: LinearLayout
    private lateinit var searchInput: EditText
    private var people: List<FollowPerson> = emptyList()
    private var myId = ""
    private var myFollowing: Set<String> = emptySet()
    private var myFollowers: Set<String> = emptySet()
    private var listType = "following"
    private var targetUserId = ""
    private var token = ""

    private val ink = Color.rgb(20, 24, 34)
    private val muted = Color.rgb(143, 151, 164)
    private val fieldBg = Color.rgb(244, 245, 250)

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun bg(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        listType = intent.getStringExtra("list_type") ?: "following"
        targetUserId = intent.getStringExtra("target_user_id").orEmpty()
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()
        if (token.isBlank()) { finish(); return }
        buildUi()
        loadData()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(Color.WHITE)
        }
        // Title bar: centered title, back chevron on the right
        val title = when (listType) {
            "followers" -> "المعجبون"
            "friends" -> "الأصدقاء"
            else -> "المُتابَعون"
        }
        val bar = FrameLayout(this)
        bar.addView(label(title, 18f, ink, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1))
        bar.addView(label("›", 38f, ink).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, FrameLayout.LayoutParams(dp(56f), -1, Gravity.RIGHT))
        root.addView(bar, LinearLayout.LayoutParams(-1, dp(56f)))

        // Tabs: "All" (selected, right) | private follow (left)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(label("متابعة خاصة(٠)", 17f, muted).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, dp(44f), 1f))
        val allCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        allCol.addView(label("All", 20f, ink, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-2, dp(40f)))
        allCol.addView(View(this).apply { setBackgroundColor(ink) }, LinearLayout.LayoutParams(dp(14f), dp(3f)))
        tabs.addView(allCol, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(tabs, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6f) })

        // Search field
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bg(fieldBg, 24f)
            setPadding(dp(16f), 0, dp(16f), 0)
        }
        searchInput = EditText(this).apply {
            hint = "البحث"
            textSize = 16f
            setHintTextColor(muted)
            setTextColor(ink)
            background = null
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { render() }
            })
        }
        searchBox.addView(searchInput, LinearLayout.LayoutParams(0, -1, 1f))
        searchBox.addView(label("⌕", 26f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(30f), -1))
        root.addView(searchBox, LinearLayout.LayoutParams(-1, dp(52f)).apply {
            setMargins(dp(16f), dp(10f), dp(16f), dp(8f))
        })

        // List
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun loadData() {
        executor.execute {
            val mine = FollowApi.load("", token)
            myFollowing = mine?.following?.map { it.id }?.toSet() ?: emptySet()
            myFollowers = mine?.followers?.map { it.id }?.toSet() ?: emptySet()
            myId = runCatching { AuthApi.get("/api/auth/me", token) }.getOrNull()
                ?.body?.optJSONObject("user")?.optString("id").orEmpty()
            val lists = if (targetUserId.isBlank() || targetUserId == myId) mine else FollowApi.load(targetUserId, token)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (lists == null) {
                    Toast.makeText(this, "تعذر تحميل القائمة", Toast.LENGTH_SHORT).show()
                } else {
                    people = when (listType) {
                        "followers" -> lists.followers
                        "friends" -> lists.friends
                        else -> lists.following
                    }
                }
                render()
            }
        }
    }

    private fun render() {
        if (!::listBox.isInitialized) return
        val q = searchInput.text.toString().trim()
        val shown = if (q.isEmpty()) people else people.filter { it.name.contains(q, ignoreCase = true) }
        listBox.removeAllViews()
        if (shown.isEmpty()) {
            listBox.addView(label("لا يوجد أحد هنا", 15f, muted).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(-1, dp(160f)))
            return
        }
        shown.forEach { listBox.addView(row(it), LinearLayout.LayoutParams(-1, -2)) }
    }

    private fun relationText(id: String) = when {
        id in myFollowing && id in myFollowers -> "الأصدقاء"
        id in myFollowing -> "متابَع"
        else -> "متابعة +"
    }

    private fun row(p: FollowPerson): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            isClickable = true
            if (p.id != myId) setOnClickListener {
                startActivity(Intent(this@FollowListActivity, ProfileActivity::class.java)
                    .putExtra("profile_user_id", p.id))
            }
        }
        // left: relation button
        if (p.id != myId) {
            val btn = label(relationText(p.id), 14f, muted).apply {
                gravity = Gravity.CENTER
                background = bg(fieldBg, 22f)
                setOnClickListener {
                    if (p.id in myFollowing) {
                        showUnfollowDialog(this@FollowListActivity, p.name, cache[p.avatarUrl]) { toggleFollow(p.id, this) }
                    } else toggleFollow(p.id, this)
                }
            }
            row.addView(btn, LinearLayout.LayoutParams(dp(92f), dp(42f)))
        } else row.addView(View(this), LinearLayout.LayoutParams(dp(92f), 1))
        // center: name (right aligned)
        row.addView(label(p.name, 16f, ink).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(10f), 0, dp(12f), 0) })
        // right: avatar + level pill
        val frame = FrameLayout(this)
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(226, 229, 238)) }
            clipToOutline = true
        }
        frame.addView(img, FrameLayout.LayoutParams(dp(66f), dp(66f), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        if (p.level > 0) {
            frame.addView(label("◆ ${p.level}", 10f, Color.rgb(80, 105, 140), true).apply {
                gravity = Gravity.CENTER
                background = bg(Color.rgb(222, 234, 246), 9f)
                setPadding(dp(8f), 0, dp(8f), 0)
            }, FrameLayout.LayoutParams(-2, dp(18f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        }
        row.addView(frame, LinearLayout.LayoutParams(dp(70f), dp(74f)))
        loadImage(p.avatarUrl, img)
        return row
    }

    private fun toggleFollow(id: String, btn: TextView) {
        btn.isEnabled = false
        val wasFollowing = id in myFollowing
        executor.execute {
            try {
                val endpoint = "/api/follows/$id"
                val r = if (wasFollowing) AuthApi.delete(endpoint, token) else AuthApi.post(endpoint, JSONObject(), token)
                if (r.status !in 200..299) throw Exception(r.body.optString("error", "تعذر تحديث المتابعة"))
                myFollowing = if (wasFollowing) myFollowing - id else myFollowing + id
                runOnUiThread { btn.text = relationText(id); btn.isEnabled = true }
            } catch (e: Exception) {
                runOnUiThread {
                    btn.isEnabled = true
                    Toast.makeText(this, e.message ?: "تعذر تحديث المتابعة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadImage(url: String, view: ImageView) {
        if (url.isBlank()) return
        view.tag = url
        cache[url]?.let { view.setImageBitmap(it); return }
        executor.execute {
            try {
                val bytes = URL(url).openStream().use { it.readBytes() }
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                var sample = 1
                while (opts.outWidth / sample > 300) sample *= 2
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@execute
                cache[url] = bmp
                runOnUiThread { if (!isFinishing && view.tag == url) view.setImageBitmap(bmp) }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}


/** "Are you sure you want to unfollow X?" confirmation card (bottom sheet style). */
fun showUnfollowDialog(activity: Activity, name: String, avatar: Bitmap?, onConfirm: () -> Unit) {
    val density = activity.resources.displayMetrics.density
    fun dp(v: Float) = (v * density).toInt()
    val pink = Color.rgb(255, 61, 110)
    val soft = Color.rgb(250, 128, 150)
    val dialog = android.app.Dialog(activity)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    val card = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(24f).toFloat() }
        setPadding(dp(22f), dp(24f), dp(22f), dp(20f))
    }
    val frame = FrameLayout(activity)
    val img = ImageView(activity).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(226, 229, 238)) }
        clipToOutline = true
        if (avatar != null) setImageBitmap(avatar)
    }
    frame.addView(img, FrameLayout.LayoutParams(dp(74f), dp(74f)))
    frame.addView(TextView(activity).apply { text = "💔"; textSize = 16f },
        FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.LEFT))
    card.addView(frame, LinearLayout.LayoutParams(dp(74f), dp(74f)))
    fun line(value: String, color: Int) = TextView(activity).apply {
        text = value; textSize = 14f; setTextColor(color); gravity = Gravity.CENTER
        textDirection = View.TEXT_DIRECTION_RTL
    }
    card.addView(line("هل أنت متأكد من", Color.rgb(20, 24, 34)), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18f) })
    card.addView(line("إلغاء متابعه $name؟", soft), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f) })
    fun button(label: String, color: Int, bold: Boolean, onClick: () -> Unit) = TextView(activity).apply {
        text = label; textSize = 15f; setTextColor(color); gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(24f).toFloat()
            setStroke(dp(1f), if (bold) color else Color.rgb(205, 208, 215))
        }
        setOnClickListener { dialog.dismiss(); onClick() }
    }
    card.addView(button("إلغاء المتابعة", pink, true) { onConfirm() },
        LinearLayout.LayoutParams(-1, dp(46f)).apply { topMargin = dp(22f) })
    card.addView(button("إلغاء", Color.rgb(20, 24, 34), false) {},
        LinearLayout.LayoutParams(-1, dp(46f)).apply { topMargin = dp(10f) })
    val holder = FrameLayout(activity).apply { setPadding(dp(16f), 0, dp(16f), dp(16f)) }
    holder.addView(card, FrameLayout.LayoutParams(-1, -2))
    dialog.setContentView(holder)
    dialog.window?.apply {
        setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        setGravity(Gravity.BOTTOM)
        setLayout(-1, -2)
        setDimAmount(0.5f)
    }
    dialog.show()
}
