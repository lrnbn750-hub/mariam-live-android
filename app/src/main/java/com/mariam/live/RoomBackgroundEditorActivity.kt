package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Background editor for BAK LIVE.
 * Only the background image can be moved/scaled. Seat placeholders are a fixed,
 * non-interactive overlay that mirrors the room's 4x2 seat arrangement.
 */
class RoomBackgroundEditorActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var roomId = ""
    private var token = ""
    private var selectedUri: Uri? = null
    private lateinit var preview: FrameLayout
    private lateinit var backgroundImage: ImageView
    private lateinit var zoomLabel: TextView
    private lateinit var status: TextView
    private lateinit var seatsOverlay: GridLayout
    private lateinit var zoomSeek: SeekBar
    private var bitmap: Bitmap? = null
    private val matrix = Matrix()
    private var baseScale = 1f
    private var currentScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var showSeats = true
    private var imageReady = false
    private var pickerOpen = false
    private val minScale = 1f
    private val maxScale = 3.5f

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val bg = Color.rgb(15, 8, 28)
    private val panel = Color.rgb(34, 19, 52)
    private val purple = Color.rgb(164, 72, 235)
    private val gold = Color.rgb(255, 204, 104)

    private fun rounded(color: Int, radius: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create(
                "sans-serif",
                if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = intent.getStringExtra("room_id").orEmpty()
        token = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null).orEmpty()
        window.statusBarColor = bg
        window.navigationBarColor = Color.rgb(10, 5, 19)
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(text("‹", 32f, gold, true).apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
        header.addView(text("تخصيص خلفية الغرفة", 19f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(text("حفظ", 15f, Color.BLACK, true).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(255, 222, 145), Color.rgb(214, 142, 43))).apply {
                cornerRadius = dp(18).toFloat()
            }
            setPadding(dp(18), 0, dp(18), 0)
            setOnClickListener { saveAndApply() }
        }, LinearLayout.LayoutParams(-2, dp(40)))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))

        preview = FrameLayout(this).apply {
            background = rounded(Color.rgb(8, 5, 15), 18, purple)
            clipToOutline = true
        }
        backgroundImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.MATRIX
            setBackgroundColor(Color.rgb(12, 8, 18))
            contentDescription = "معاينة خلفية الغرفة"
        }
        preview.addView(backgroundImage, FrameLayout.LayoutParams(-1, -1))

        // Fixed seat placeholders: intentionally not clickable and never moved.
        seatsOverlay = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = false
            isFocusable = false
        }
        for (i in 1..8) {
            val seat = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = false
                isFocusable = false
            }
            val circle = FrameLayout(this@RoomBackgroundEditorActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x5520182A)
                    setStroke(dp(2), 0xCCFFD28A.toInt())
                }
                addView(text("+", 26f, Color.WHITE, true),
                    FrameLayout.LayoutParams(-1, -1))
            }
            seat.addView(circle, LinearLayout.LayoutParams(dp(48), dp(48)))
            seat.addView(text("♩", 12f, gold, true), LinearLayout.LayoutParams(-1, dp(17)))
            seat.addView(text("$i", 9f, Color.WHITE), LinearLayout.LayoutParams(-1, dp(13)))
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(82)
                columnSpec = GridLayout.spec((i - 1) / 4, 1f)
                rowSpec = GridLayout.spec((i - 1) % 2)
                setMargins(dp(2), dp(4), dp(2), dp(4))
            }
            // GridLayout is row-major; map the intended 4 columns x 2 rows explicitly.
            params.columnSpec = GridLayout.spec((i - 1) % 4, 1f)
            params.rowSpec = GridLayout.spec((i - 1) / 4)
            seatsOverlay.addView(seat, params)
        }
        preview.addView(seatsOverlay, FrameLayout.LayoutParams(-1, -1))
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(8)
            bottomMargin = dp(10)
        })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 18, Color.rgb(89, 47, 126))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val sourceRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        sourceRow.addView(actionButton("اختيار صورة من المعرض") { openPicker() },
            LinearLayout.LayoutParams(0, dp(44), 1f))
        sourceRow.addView(actionButton("إعادة ضبط") { resetImageTransform() },
            LinearLayout.LayoutParams(dp(105), dp(44)).apply { marginStart = dp(8) })
        controls.addView(sourceRow)

        controls.addView(text("تكبير وتصغير الخلفية", 13f, Color.WHITE, true).apply {
            gravity = Gravity.RIGHT
            setPadding(0, dp(9), 0, 0)
        })
        val zoomRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        zoomLabel = text("100%", 12f, gold, true)
        zoomRow.addView(zoomLabel, LinearLayout.LayoutParams(dp(52), dp(40)))
        zoomSeek = SeekBar(this).apply {
            max = 250
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            thumbTintList = android.content.res.ColorStateList.valueOf(gold)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && imageReady) setScaleFromSlider(1f + progress / 100f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        zoomRow.addView(zoomSeek, LinearLayout.LayoutParams(0, dp(40), 1f))
        controls.addView(zoomRow)

        val moveRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        moveRow.addView(actionButton("←") { moveImage(-dp(18).toFloat(), 0f) },
            LinearLayout.LayoutParams(dp(58), dp(42)).apply { marginEnd = dp(6) })
        moveRow.addView(actionButton("↑") { moveImage(0f, -dp(18).toFloat()) },
            LinearLayout.LayoutParams(dp(58), dp(42)).apply { marginEnd = dp(6) })
        moveRow.addView(actionButton("↓") { moveImage(0f, dp(18).toFloat()) },
            LinearLayout.LayoutParams(dp(58), dp(42)).apply { marginEnd = dp(6) })
        moveRow.addView(actionButton("→") { moveImage(dp(18).toFloat(), 0f) },
            LinearLayout.LayoutParams(dp(58), dp(42)))
        controls.addView(moveRow, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(5)
        })

        val toggleRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text("إظهار المقاعد الوهمية في المعاينة", 13f, Color.WHITE),
                LinearLayout.LayoutParams(0, dp(42), 1f))
            val toggle = Switch(this@RoomBackgroundEditorActivity).apply {
                isChecked = true
                buttonTintList = android.content.res.ColorStateList.valueOf(purple)
                setOnCheckedChangeListener { _, checked ->
                    showSeats = checked
                    seatsOverlay.visibility = if (checked) View.VISIBLE else View.GONE
                }
            }
            addView(toggle)
        }
        controls.addView(toggleRow)
        root.addView(controls, LinearLayout.LayoutParams(-1, -2))

        status = text("", 12f, gold).apply { gravity = Gravity.CENTER }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(26)))

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER }
        bottom.addView(actionButton("معاينة الغرفة") {
            Toast.makeText(this@RoomBackgroundEditorActivity,
                "هذه معاينة المقاعد الثابتة فوق الخلفية", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        bottom.addView(actionButton("تطبيق الخلفية") { saveAndApply() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(root)

        val detector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!imageReady) return false
                applyScale(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })
        backgroundImage.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    dragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragging && !detector.isInProgress && imageReady) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        matrix.postTranslate(dx, dy)
                        backgroundImage.imageMatrix = matrix
                        lastX = event.x
                        lastY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
            }
            true
        }

        preview.post {
            if (!imageReady) {
                status.text = "اختر صورة خلفية للبدء"
                openPicker()
            }
        }
    }

    private fun actionButton(title: String, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        background = rounded(Color.rgb(62, 29, 91), 14, Color.rgb(126, 73, 174))
        isClickable = true
        setOnClickListener { action() }
    }

    private fun openPicker() {
        if (pickerOpen) return
        pickerOpen = true
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        try {
            startActivityForResult(picker, 4301)
        } catch (_: Exception) {
            pickerOpen = false
            status.text = "تعذر فتح معرض الصور"
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4301) return
        pickerOpen = false
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        selectedUri = uri
        status.text = "جارٍ تحميل الصورة..."
        scope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: throw Exception("تعذر قراءة الصورة")
                }
                bitmap?.recycle()
                bitmap = decoded
                backgroundImage.setImageBitmap(decoded)
                backgroundImage.post { fitImageToPreview() }
                status.text = "اسحب الصورة لتحريكها، واستخدم إصبعين للتكبير"
            } catch (e: Exception) {
                status.text = e.message ?: "تعذر تحميل الصورة"
            }
        }
    }

    private fun fitImageToPreview() {
        val source = bitmap ?: return
        if (preview.width <= 0 || preview.height <= 0) return
        matrix.reset()
        baseScale = maxOf(
            preview.width.toFloat() / source.width,
            preview.height.toFloat() / source.height
        )
        currentScale = 1f
        val drawnW = source.width * baseScale
        val drawnH = source.height * baseScale
        matrix.postScale(baseScale, baseScale)
        matrix.postTranslate((preview.width - drawnW) / 2f, (preview.height - drawnH) / 2f)
        backgroundImage.imageMatrix = matrix
        imageReady = true
        zoomSeek.progress = 0
        zoomLabel.text = "100%"
    }

    private fun applyScale(factor: Float, focusX: Float, focusY: Float) {
        val next = (currentScale * factor).coerceIn(minScale, maxScale)
        val applied = next / currentScale
        currentScale = next
        matrix.postScale(applied, applied, focusX, focusY)
        backgroundImage.imageMatrix = matrix
        updateZoomUi()
    }

    private fun setScaleFromSlider(value: Float) {
        val target = value.coerceIn(minScale, maxScale)
        val factor = target / currentScale
        currentScale = target
        matrix.postScale(factor, factor, preview.width / 2f, preview.height / 2f)
        backgroundImage.imageMatrix = matrix
        updateZoomUi()
    }

    private fun updateZoomUi() {
        zoomLabel.text = "${(currentScale * 100).toInt()}%"
        val expected = ((currentScale - 1f) * 100).toInt().coerceIn(0, 250)
        if (zoomSeek.progress != expected) zoomSeek.progress = expected
    }

    private fun moveImage(dx: Float, dy: Float) {
        if (!imageReady) return
        matrix.postTranslate(dx, dy)
        backgroundImage.imageMatrix = matrix
    }

    private fun resetImageTransform() {
        if (bitmap == null) {
            openPicker()
            return
        }
        fitImageToPreview()
        status.text = "تمت إعادة ضبط موضع الخلفية"
    }

    private fun saveAndApply() {
        if (!imageReady || bitmap == null) {
            Toast.makeText(this, "اختر صورة أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        if (roomId.isBlank() || token.isBlank()) {
            Toast.makeText(this, "بيانات الغرفة غير متوفرة", Toast.LENGTH_LONG).show()
            return
        }
        status.text = "جارٍ تجهيز وحفظ الخلفية..."
        scope.launch {
            try {
                val payload = withContext(Dispatchers.IO) {
                    val width = preview.width.coerceAtMost(2048)
                    val height = preview.height.coerceAtMost(2048)
                    val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(rendered)
                    val sx = width.toFloat() / preview.width
                    val sy = height.toFloat() / preview.height
                    canvas.scale(sx, sy)
                    backgroundImage.draw(canvas)
                    val output = ByteArrayOutputStream()
                    var quality = 90
                    do {
                        output.reset()
                        rendered.compress(Bitmap.CompressFormat.JPEG, quality, output)
                        quality -= 7
                    } while (output.size() > 3 * 1024 * 1024 && quality >= 55)
                    rendered.recycle()
                    if (output.size() > 4 * 1024 * 1024) throw Exception("الصورة أكبر من الحد المسموح")
                    JSONObject()
                        .put("background_base64", android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP))
                        .put("image_mime", "image/jpeg")
                }
                val response = withContext(Dispatchers.IO) {
                    AuthApi.put("/api/voice-rooms/$roomId/background", payload, token)
                }
                if (response.status in 200..299) {
                    setResult(RESULT_OK)
                    Toast.makeText(this@RoomBackgroundEditorActivity,
                        "تم تطبيق الخلفية على الغرفة", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    status.text = response.body.optString("error", "تعذر حفظ الخلفية")
                }
            } catch (e: Exception) {
                status.text = e.message ?: "تعذر تطبيق الخلفية"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        bitmap?.recycle()
        super.onDestroy()
    }
}
