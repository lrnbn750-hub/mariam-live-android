package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class HomeActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val purple = Color.rgb(111, 76, 255)
    private val ink = Color.rgb(35, 32, 58)
    private val muted = Color.rgb(137, 135, 157)
    private val page = Color.rgb(247, 246, 252)
    private lateinit var roomsContainer: LinearLayout
    private lateinit var roomStatus: TextView
    private lateinit var greeting: TextView
    private lateinit var userLine: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun shape(color: Int, radius: Int = 18, stroke: Int = Color.TRANSPARENT) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun params(width: Int = -1, height: Int = -2, top: Int = 0) =
        LinearLayout.LayoutParams(width, height).apply { topMargin = dp(top) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }

        // Top brand bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(10))
        }
        val brandStack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandStack.addView(text("MARIAM LIVE", 20f, ink, true))
        brandStack.addView(text("تواصل • شارك • استمتع", 11f, muted))
        header.addView(brandStack, LinearLayout.LayoutParams(0, -2, 1f))
        val notification = TextView(this).apply {
            text = "♧"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(ink)
            background = shape(Color.WHITE, 15)
            elevation = dp(2).toFloat()
        }
        header.addView(notification, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)

        // Welcome block
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(8))
        }
        welcome.addView(text("أهلاً بعودتك 👋", 25f, ink, true))
        greeting = text("خلّينا نكتشف الأجواء اليوم", 14f, muted)
        welcome.addView(greeting, params(top = 5))
        userLine = text("", 12f, purple, true)
        welcome.addView(userLine, params(top = 7))
        root.addView(welcome)

        // Main feature card
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(119, 83, 255), Color.rgb(77, 56, 190))
            ).apply { cornerRadius = dp(24).toFloat() }
            elevation = dp(4).toFloat()
        }
        hero.addView(text("مساحتك… صوتك… أصدقاؤك", 20f, Color.WHITE, true))
        hero.addView(text("انضم إلى غرفة وتعرّف على أشخاص جدد", 13f, Color.rgb(235, 230, 255)).apply {
            setPadding(0, dp(7), 0, dp(14))
        })
        val explore = TextView(this).apply {
            text = "استكشف الغرف  →"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(purple)
            gravity = Gravity.CENTER
            background = shape(Color.WHITE, 13)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            setOnClickListener { loadRooms() }
        }
        hero.addView(explore, LinearLayout.LayoutParams(-2, -2))
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(174)).apply {
            setMargins(dp(20), dp(14), dp(20), dp(18))
        })

        // Quick categories
        val categoryTitle = text("اكتشف مجتمعك", 18f, ink, true)
        root.addView(categoryTitle, LinearLayout.LayoutParams(-1, dp(32)).apply {
            setMargins(dp(20), 0, dp(20), dp(8))
        })
        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), 0, dp(20), dp(4))
        }
        listOf("الكل", "🎙 صوتية", "🎵 موسيقى", "💬 دردشة").forEachIndexed { index, name ->
            val chip = TextView(this).apply {
                text = name
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(15), dp(10), dp(15), dp(10))
                setTextColor(if (index == 0) Color.WHITE else muted)
                background = shape(if (index == 0) purple else Color.WHITE, 20)
            }
            categories.addView(chip, LinearLayout.LayoutParams(-2, dp(40)).apply {
                marginEnd = dp(8)
            })
        }
        root.addView(categories)

        // Rooms section
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(8))
        }
        section.addView(text("الغرف النشطة", 19f, ink, true), LinearLayout.LayoutParams(0, dp(34), 1f))
        section.addView(text("مباشر الآن  ●", 11f, Color.rgb(28, 164, 112), true))
        root.addView(section)

        val roomsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.WHITE, 22)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            elevation = dp(2).toFloat()
        }
        roomStatus = text("جارٍ تحميل الغرف...", 13f, muted)
        roomsCard.addView(roomStatus, params(top = 2))
        roomsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        roomsCard.addView(roomsContainer, params(top = 8))
        root.addView(roomsCard, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(20), 0, dp(20), dp(14))
        })

        val create = TextView(this).apply {
            text = "＋   أنشئ غرفتك الصوتية"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = shape(purple, 16)
            elevation = dp(3).toFloat()
            setOnClickListener { showCreateRoomDialog() }
        }
        root.addView(create, LinearLayout.LayoutParams(-1, dp(54)).apply {
            setMargins(dp(20), dp(4), dp(20), dp(24))
        })

        // Bottom navigation presentation
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            elevation = dp(8).toFloat()
        }
        listOf("⌂\nالرئيسية", "◉\nالغرف", "♡\nالأصدقاء", "●\nحسابي").forEachIndexed { index, item ->
            val tab = TextView(this).apply {
                text = item
                textSize = 11f
                gravity = Gravity.CENTER
                setLineSpacing(dp(3).toFloat(), 1f)
                setTextColor(if (index == 0) purple else muted)
                typeface = if (index == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(nav)
        setContentView(scroll)

        verifyAccount()
        loadRooms()
    }

    private fun verifyAccount() {
        val prefs = getSharedPreferences("MariamLive", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token.isNullOrBlank()) {
            openLogin()
            return
        }
        executor.execute {
            try {
                val result = AuthApi.get("/api/auth/me", token)
                if (result.status in 200..299) {
                    val user = result.body.optJSONObject("user") ?: result.body
                    val name = user.optString("display_name")
                        .takeIf { it.isNotBlank() } ?: user.optString("username", "صديقنا")
                    val username = user.optString("username")
                    runOnUiThread {
                        greeting.text = "سعيدون بوجودك، $name"
                        userLine.text = if (username.isNotBlank()) "@$username" else "عضو في مجتمع Mariam Live"
                    }
                } else if (result.status == 401 || result.status == 403) {
                    prefs.edit().remove("token").apply()
                    runOnUiThread { openLogin() }
                } else {
                    runOnUiThread { userLine.text = "تعذر تحديث بيانات الحساب" }
                }
            } catch (_: Exception) {
                runOnUiThread { userLine.text = "تحقق من اتصال الإنترنت" }
            }
        }
    }

    private fun loadRooms() {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        roomStatus.text = "جارٍ تحديث قائمة الغرف..."
        executor.execute {
            try {
                val result = AuthApi.get("/api/voice-rooms", token)
                val rooms = result.body.optJSONArray("rooms")
                runOnUiThread {
                    roomsContainer.removeAllViews()
                    if (result.status !in 200..299 || rooms == null) {
                        roomStatus.text = "تعذر تحميل الغرف حالياً."
                        return@runOnUiThread
                    }
                    roomStatus.text = if (rooms.length() == 0) "لا توجد غرف نشطة الآن. كن أول من ينشئ غرفة!" else "اختر غرفة وانضم إلى الأصدقاء"
                    for (i in 0 until rooms.length()) {
                        val room = rooms.optJSONObject(i) ?: continue
                        val id = room.optString("id")
                        val name = room.optString("name", "غرفة صوتية")
                        val count = room.optInt("member_count", 0)
                        val max = room.optInt("max_members", 8)
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(12), dp(12), dp(12), dp(12))
                            background = shape(Color.rgb(248, 247, 253), 16)
                        }
                        val icon = TextView(this).apply {
                            text = "🎙"
                            textSize = 23f
                            gravity = Gravity.CENTER
                            background = shape(Color.rgb(235, 230, 255), 14)
                        }
                        row.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                        val info = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(11), 0, dp(6), 0)
                        }
                        info.addView(text(name, 14f, ink, true))
                        info.addView(text("👥  $count / $max مشارك", 11f, muted).apply {
                            setPadding(0, dp(5), 0, 0)
                        })
                        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                        val join = TextView(this).apply {
                            text = "دخول"
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(purple)
                            gravity = Gravity.CENTER
                            background = shape(Color.WHITE, 12, Color.rgb(226, 220, 255))
                            setPadding(dp(13), dp(9), dp(13), dp(9))
                            setOnClickListener { joinRoom(id, name) }
                        }
                        row.addView(join)
                        roomsContainer.addView(row, params(top = if (i == 0) 0 else 9))
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { roomStatus.text = "تعذر الاتصال بالخادم لتحميل الغرف." }
            }
        }
    }

    private fun showCreateRoomDialog() {
        val input = EditText(this).apply { hint = "اسم الغرفة"; setSingleLine(true) }
        android.app.AlertDialog.Builder(this)
            .setTitle("إنشاء غرفة صوتية")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إنشاء") { _, _ ->
                val name = input.text.toString().trim()
                if (name.length < 2) Toast.makeText(this, "اكتب اسماً من حرفين على الأقل", Toast.LENGTH_SHORT).show()
                else createRoom(name)
            }.show()
    }

    private fun createRoom(name: String) {
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        roomStatus.text = "جارٍ إنشاء الغرفة..."
        executor.execute {
            try {
                val result = AuthApi.post("/api/voice-rooms", JSONObject().put("name", name).put("max_members", 8), token)
                runOnUiThread {
                    if (result.status in 200..299) {
                        Toast.makeText(this, "تم إنشاء الغرفة", Toast.LENGTH_SHORT).show()
                        loadRooms()
                    } else {
                        val message = result.body.optString("error", "تعذر إنشاء الغرفة")
                        roomStatus.text = message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { roomStatus.text = "تعذر الاتصال بالخادم." }
            }
        }
    }

    private fun joinRoom(id: String, name: String) {
        if (id.isBlank()) return
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null) ?: return
        roomStatus.text = "جارٍ الانضمام إلى الغرفة..."
        executor.execute {
            try {
                val result = AuthApi.post("/api/voice-rooms/$id/join", JSONObject(), token)
                runOnUiThread {
                    if (result.status in 200..299) {
                        startActivity(Intent(this, RoomActivity::class.java).apply {
                            putExtra("room_id", id)
                            putExtra("room_name", name)
                        })
                    } else {
                        val message = result.body.optString("error", "تعذر دخول الغرفة")
                        roomStatus.text = message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        loadRooms()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { roomStatus.text = "تعذر الاتصال بالخادم لدخول الغرفة." }
            }
        }
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
