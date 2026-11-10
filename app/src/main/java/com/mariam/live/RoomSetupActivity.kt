package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class RoomSetupActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val navy = Color.rgb(20, 16, 69)
    private val blue = Color.rgb(64, 66, 235)
    private val violet = Color.rgb(83, 48, 222)
    private val white = Color.WHITE
    private lateinit var titleInput: EditText
    private lateinit var preview: ImageView
    private lateinit var startButton: TextView
    private var imageUri: Uri? = null
    private val imagePickerRequest = 7315
    private var selectedCategory = "عام"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(75, 76, 238)
        window.navigationBarColor = Color.rgb(45, 35, 170)
        window.decorView.systemUiVisibility = 0
        imageUri = intent.getStringExtra("image_uri")?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val initialName = intent.getStringExtra("room_name").orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(83, 112, 255), Color.rgb(66, 64, 231), Color.rgb(55, 36, 175)))
        }
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(10), dp(18), dp(18))
        }
        scroll.addView(content)
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val close = text("×", 34f, white, false).apply {
            setOnClickListener { finish() }
        }
        top.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        top.addView(text("إعداد غرفة صوتية", 18f, white, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        top.addView(text("M", 22f, white, true).apply {
            background = rounded(Color.rgb(45, 53, 190), 24, Color.rgb(150, 190, 255))
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        content.addView(top)

        val setupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(42, 44, 190), 18, Color.rgb(105, 135, 255))
        }
        val imageRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(36, 54, 155), 14, Color.WHITE)
            clipToOutline = true
        }
        preview.setOnClickListener { chooseRoomImage() }
        preview.contentDescription = "اختيار صورة الغرفة"
        imageRow.addView(preview, LinearLayout.LayoutParams(dp(66), dp(66)))
        preview.setBackgroundColor(Color.rgb(36, 54, 155))
        preview.setImageDrawable(ChairPlaceholder())
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleColumn.addView(text("عنوان الغرفة", 12f, Color.rgb(213, 222, 255), true).apply {
            gravity = Gravity.START
        })
        titleInput = EditText(this).apply {
            setText(initialName)
            hint = "أضف عنواناً للدردشة"
            textSize = 17f
            setTextColor(white)
            setHintTextColor(Color.rgb(205, 210, 255))
            setSingleLine(true)
            setPadding(0, dp(5), 0, dp(5))
            background = null
        }
        titleColumn.addView(titleInput, LinearLayout.LayoutParams(-1, dp(42)))
        imageRow.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        setupCard.addView(imageRow)
        setupCard.addView(text("＋ اختر صورة الغرفة", 12f, Color.rgb(224, 229, 255), true).apply {
            gravity = Gravity.START
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { chooseRoomImage() }
        })
        setupCard.addView(text("غرفة عامة  •  8 مقاعد صوتية", 12f, Color.rgb(220, 226, 255)).apply {
            gravity = Gravity.START; setPadding(0, dp(9), 0, 0)
        })
        content.addView(setupCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        content.addView(text("اختر تصنيف الغرفة", 16f, white, true).apply {
            gravity = Gravity.START; setPadding(0, dp(20), 0, dp(9))
        })
        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        listOf("مشاعر ♡", "موسيقى ♬", "ألعاب", "عام").forEach { label ->
            val category = text(label, 11f, white, true).apply {
                setPadding(dp(11), dp(9), dp(11), dp(9))
                background = rounded(if (label == "عام") Color.rgb(139, 62, 185) else Color.rgb(72, 66, 199), 20, Color.rgb(140, 157, 255))
                setOnClickListener {
                    selectedCategory = label.substringBefore(" ")
                    for (i in 0 until categories.childCount) {
                        val child = categories.getChildAt(i) as TextView
                        child.background = rounded(if (child === this) Color.rgb(139, 62, 185) else Color.rgb(72, 66, 199), 20, Color.rgb(140, 157, 255))
                    }
                }
            }
            categories.addView(category, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
        content.addView(categories, LinearLayout.LayoutParams(-1, -2))

        content.addView(text("مقاعد المتحدثين", 17f, white, true).apply {
            gravity = Gravity.START; setPadding(0, dp(24), 0, dp(12))
        })
        val seatGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (row in 0..1) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            for (col in 0..3) {
                val number = row * 4 + col + 1
                val seat = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(2), dp(8), dp(2), dp(6))
                }
                val circle = FrameLayout(this).apply {
                    background = rounded(Color.rgb(100, 111, 248), 50, Color.rgb(131, 163, 255))
                    addView(ChairIcon(), FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER))
                }
                seat.addView(circle, LinearLayout.LayoutParams(dp(64), dp(64)))
                seat.addView(text("المقعد $number", 10f, white, true).apply { setPadding(0, dp(5), 0, 0) })
                line.addView(seat, LinearLayout.LayoutParams(0, dp(104), 1f))
            }
            seatGrid.addView(line)
        }
        content.addView(seatGrid, LinearLayout.LayoutParams(-1, -2))

        val note = text("سيتم فتح الغرفة بعد الضغط على بدء البث المباشر", 11f, Color.rgb(225, 228, 255))
        content.addView(note, LinearLayout.LayoutParams(-1, dp(44)))

        startButton = text("ابدأ البث المباشر  ▶", 17f, white, true).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(92, 0, 255), Color.rgb(115, 35, 240))).apply {
                cornerRadius = dp(30).toFloat()
                setStroke(dp(1), Color.rgb(164, 135, 255))
            }
            elevation = dp(5).toFloat()
            setOnClickListener { createAndStart() }
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(startButton, LinearLayout.LayoutParams(-1, dp(58)).apply {
            setMargins(dp(20), dp(7), dp(20), dp(12))
        })
        setContentView(root)
        imageUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    preview.setImageBitmap(BitmapFactory.decodeStream(input))
                }
            } catch (_: Exception) {}
        }
    }

    private fun chooseRoomImage() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(pick, imagePickerRequest)
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickerRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            imageUri = uri
            try {
                val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    preview.setImageBitmap(BitmapFactory.decodeStream(input))
                }
            } catch (_: Exception) {
                Toast.makeText(this, "تعذر فتح الصورة، اختر صورة أخرى", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createAndStart() {
        val name = titleInput.text.toString().trim()
        if (name.length < 2) {
            Toast.makeText(this, "اكتب عنواناً من حرفين على الأقل", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedImage = imageUri
        if (selectedImage == null) {
            Toast.makeText(this, "اختر صورة للغرفة أولاً", Toast.LENGTH_SHORT).show()
            chooseRoomImage()
            return
        }
        startButton.isEnabled = false
        startButton.text = "جارٍ إنشاء الغرفة..."
        val token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "سجّل الدخول أولاً", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        executor.execute {
            try {
                val source = contentResolver.openInputStream(selectedImage)?.use { BitmapFactory.decodeStream(it) }
                    ?: throw Exception("تعذر قراءة صورة الغرفة")
                val maxSide = 1280
                val scale = minOf(1f, maxSide.toFloat() / maxOf(source.width, source.height))
                val resized = if (scale < 1f) Bitmap.createScaledBitmap(source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1), true) else source
                val bytes = ByteArrayOutputStream()
                var quality = 85
                do {
                    bytes.reset()
                    resized.compress(Bitmap.CompressFormat.JPEG, quality, bytes)
                    quality -= 10
                } while (bytes.size() > 3 * 1024 * 1024 && quality >= 45)
                if (bytes.size() > 3 * 1024 * 1024) throw Exception("الصورة كبيرة جداً")
                val payload = JSONObject().put("name", name).put("max_members", 8)
                    .put("image_mime", "image/jpeg")
                    .put("image_base64", android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP))
                if (resized !== source) resized.recycle()
                if (!source.isRecycled) source.recycle()
                val result = AuthApi.post("/api/voice-rooms", payload, token)
                if (result.status !in 200..299) throw Exception(result.body.optString("error", "تعذر إنشاء الغرفة"))
                val room = result.body.optJSONObject("room") ?: throw Exception("استجابة الخادم غير مكتملة")
                val roomId = room.optString("id")
                runOnUiThread {
                    startActivity(Intent(this, RoomActivity::class.java)
                        .putExtra("room_id", roomId)
                        .putExtra("room_name", name))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    startButton.isEnabled = true
                    startButton.text = "ابدأ البث المباشر  ▶"
                    Toast.makeText(this, e.message ?: "تعذر إنشاء الغرفة", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private inner class ChairPlaceholder : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(25).toFloat()
            textAlign = Paint.Align.CENTER
        }
        override fun draw(canvas: Canvas) {
            val b = bounds
            canvas.drawText("＋", b.exactCenterX(), b.exactCenterY() + dp(9), paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    private inner class ChairIcon : View(this@RoomSetupActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            val sx = w / 36f; val sy = h / 36f
            canvas.save(); canvas.scale(sx, sy)
            canvas.drawRoundRect(RectF(10f, 5f, 26f, 19f), 3f, 3f, paint)
            canvas.drawLine(7f, 17f, 7f, 27f, paint)
            canvas.drawLine(29f, 17f, 29f, 27f, paint)
            canvas.drawLine(7f, 24f, 29f, 24f, paint)
            canvas.drawLine(10f, 24f, 10f, 31f, paint)
            canvas.drawLine(26f, 24f, 26f, 31f, paint)
            canvas.restore()
        }
    }
}
