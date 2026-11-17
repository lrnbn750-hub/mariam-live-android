package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import android.util.Base64

class AgencyActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val pickFront = 8101
    private val pickBack = 8102
    private val takeSelfie = 8103
    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var selfieUri: Uri? = null
    private var cameraFile: File? = null
    private lateinit var agencyName: EditText
    private lateinit var legalName: EditText
    private lateinit var country: Spinner
    private lateinit var frontState: TextView
    private lateinit var backState: TextView
    private lateinit var selfieState: TextView
    private lateinit var consent: CheckBox
    private lateinit var submit: Button
    private lateinit var status: TextView

    private val ink = Color.rgb(27, 35, 48)
    private val teal = Color.rgb(15, 157, 133)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun token() = getSharedPreferences("MariamLive", MODE_PRIVATE).getString("token", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 249, 251)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply { orientation = 1; setBackgroundColor(Color.rgb(247,249,251)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18),dp(8),dp(18),dp(8)) }
        header.addView(TextView(this).apply {
            text = "‹"; textSize = 34f; setTextColor(ink); gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(44),dp(52)))
        header.addView(TextView(this).apply {
            text = "مركز الوكالة"; textSize = 20f; setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0,dp(52),1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(44),dp(52)))
        root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = 1
            setPadding(dp(18),dp(10),dp(18),dp(30))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        val intro = TextView(this).apply {
            text = "قدّم طلب إنشاء وكالتك في BAK LIVE\nنقبل بطاقة الهوية الوطنية من جميع الدول. لا نقبل جواز السفر."
            textSize = 15f; setTextColor(ink); gravity = Gravity.RIGHT
            setPadding(dp(18),dp(18),dp(18),dp(18))
            background = panel()
        }
        body.addView(intro, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(16) })

        agencyName = edit("اسم الوكالة")
        legalName = edit("الاسم الكامل كما يظهر في الهوية")
        body.addView(fieldLabel("اسم الوكالة")); body.addView(agencyName)
        body.addView(fieldLabel("الاسم القانوني")); body.addView(legalName)
        body.addView(fieldLabel("بلد إصدار الهوية"))
        val countries = Locale.getISOCountries().map { code ->
            code to (Locale("", code).getDisplayCountry(Locale("ar")).ifBlank { Locale("", code).displayCountry })
        }.sortedBy { it.second }
        country = Spinner(this).apply {
            adapter = ArrayAdapter(this@AgencyActivity, android.R.layout.simple_spinner_dropdown_item,
                countries.map { "${it.second} (+${it.first})" })
        }
        body.addView(country, LinearLayout.LayoutParams(-1,dp(52)).apply { bottomMargin=dp(12) })

        body.addView(fieldLabel("توثيق الهوية الوطنية"))
        body.addView(uploadRow("صورة الوجه الأمامي للهوية", "اختر صورة واضحة", frontState = true) { choose(pickFront) })
        body.addView(uploadRow("صورة الوجه الخلفي للهوية", "اختر صورة واضحة", frontState = false) { choose(pickBack) })
        body.addView(fieldLabel("صورة شخصية مباشرة"))
        body.addView(uploadRow("التقط صورة سيلفي بالكاميرا", "يجب أن يظهر وجه صاحب الطلب بوضوح", selfie = true) { captureSelfie() })

        consent = CheckBox(this).apply {
            text = "أوافق على معالجة صور الهوية والصورة الشخصية لغرض التحقق من طلب الوكالة، وأفهم أنها لن تكون ظاهرة للمستخدمين."
            textSize = 13f; setTextColor(ink); gravity = Gravity.RIGHT
        }
        body.addView(consent, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(14); bottomMargin=dp(10) })
        status = TextView(this).apply { textSize=13f; setTextColor(teal); gravity=Gravity.RIGHT }
        body.addView(status)
        submit = Button(this).apply {
            text = "إرسال طلب الوكالة"; isAllCaps = false
            setTextColor(Color.WHITE); background = GradientDrawable().apply {
                cornerRadius=dp(15).toFloat(); setColor(teal)
            }
            setOnClickListener { sendApplication() }
        }
        body.addView(submit, LinearLayout.LayoutParams(-1,dp(54)).apply { topMargin=dp(10) })
        setContentView(root)
        loadStatus()
    }

    private fun panel() = GradientDrawable().apply {
        cornerRadius=dp(16).toFloat(); setColor(Color.WHITE)
        setStroke(dp(1),Color.rgb(231,235,240))
    }
    private fun fieldLabel(s:String)=TextView(this).apply {
        text=s; textSize=14f; setTextColor(ink); typeface=Typeface.DEFAULT_BOLD
        gravity=Gravity.RIGHT; setPadding(0,dp(8),0,dp(7))
    }
    private fun edit(hintText:String)=EditText(this).apply {
        hint=hintText; textSize=15f; setTextColor(ink); textDirection=View.TEXT_DIRECTION_RTL
        setSingleLine(true); setPadding(dp(14),0,dp(14),0); background=panel()
        layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply { bottomMargin=dp(10) }
    }
    private fun uploadRow(title:String, subtitle:String, frontState:Boolean=false, selfie:Boolean=false, click:()->Unit):View {
        val row=LinearLayout(this).apply {
            orientation=1; setPadding(dp(15),dp(12),dp(15),dp(12)); background=panel()
            isClickable=true; isFocusable=true; setOnClickListener { click() }
        }
        row.addView(TextView(this).apply { text=title; textSize=14f; setTextColor(ink); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.RIGHT })
        row.addView(TextView(this).apply {
            text=subtitle; textSize=12f; setTextColor(Color.GRAY); gravity=Gravity.RIGHT
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) })
        val state=TextView(this).apply { text="لم يتم الاختيار"; textSize=12f; setTextColor(teal); gravity=Gravity.RIGHT }
        if(frontState) this.frontState=state else if(title.contains("الخلفي")) this.backState=state else if(selfie) this.selfieState=state
        row.addView(state,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(6) })
        row.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(9) }
        return row
    }

    private fun choose(code:Int) {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type="image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent,code)
    }

    private fun captureSelfie() {
        try {
            val dir=File(cacheDir,"agency-capture").apply { mkdirs() }
            cameraFile=File(dir,"selfie-${System.currentTimeMillis()}.jpg")
            val uri=FileProvider.getUriForFile(this,"${applicationId}.fileprovider",cameraFile!!)
            val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT,uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent,takeSelfie)
        } catch (_:Exception) {
            Toast.makeText(this,"تعذر فتح الكاميرا",Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK) return
        when(requestCode) {
            pickFront -> { frontUri=data?.data; frontState.text= "تم اختيار صورة الهوية الأمامية" }
            pickBack -> { backUri=data?.data; backState.text= "تم اختيار صورة الهوية الخلفية" }
            takeSelfie -> {
                val f=cameraFile
                if(f!=null && f.exists() && f.length()>0) {
                    selfieUri=Uri.fromFile(f); selfieState.text="تم التقاط صورة الوجه"
                } else Toast.makeText(this,"لم يتم حفظ صورة السيلفي",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun imageBase64(uri:Uri):String {
        val bytes=contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("تعذر قراءة إحدى الصور")
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth<=0 || bounds.outHeight<=0) throw Exception("ملف صورة غير صالح")
        var sample=1
        while(bounds.outWidth/sample>1600 || bounds.outHeight/sample>1600) sample*=2
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inSampleSize=sample })
            ?: throw Exception("صيغة الصورة غير مدعومة")
        val out=ByteArrayOutputStream()
        var quality=86
        do {
            out.reset(); bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out); quality-=7
        } while(out.size()>1536*1024 && quality>=58)
        bitmap.recycle()
        if(out.size()>1536*1024) throw Exception("حجم الصورة أكبر من المسموح")
        return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
    }

    private fun sendApplication() {
        val name=agencyName.text.toString().trim()
        val legal=legalName.text.toString().trim()
        val front=frontUri; val back=backUri; val selfie=selfieUri
        if(name.length<2 || legal.length<3 || front==null || back==null || selfie==null || !consent.isChecked) {
            Toast.makeText(this,"أكمل البيانات والصور الثلاثة والموافقة أولاً",Toast.LENGTH_LONG).show(); return
        }
        val countryCode=Locale.getISOCountries().getOrNull(country.selectedItemPosition)
            ?: run { Toast.makeText(this,"اختر الدولة",Toast.LENGTH_SHORT).show(); return }
        val auth=token() ?: run { finish(); return }
        submit.isEnabled=false; status.text="جارٍ تجهيز الصور وإرسال الطلب..."
        worker.execute {
            try {
                val payload=JSONObject()
                    .put("agency_name",name)
                    .put("legal_name",legal)
                    .put("country_code",countryCode)
                    .put("id_front_base64",imageBase64(front))
                    .put("id_back_base64",imageBase64(back))
                    .put("selfie_base64",imageBase64(selfie))
                    .put("consent",true)
                val result=AuthApi.post("/api/agency/application",payload,auth)
                if(result.status !in 200..299) throw Exception(result.body.optString("error","تعذر إرسال الطلب"))
                runOnUiThread {
                    status.text="تم استلام طلبك وهو قيد مراجعة الإدارة"
                    submit.isEnabled=false
                    Toast.makeText(this,"تم إرسال طلب الوكالة بنجاح",Toast.LENGTH_LONG).show()
                }
                cameraFile?.delete()
            } catch(e:Exception) {
                runOnUiThread {
                    status.text=e.message ?: "تعذر إرسال الطلب"
                    submit.isEnabled=true
                }
            }
        }
    }

    private fun loadStatus() {
        val auth=token() ?: return
        worker.execute {
            try {
                val result=AuthApi.get("/api/agency/application",auth)
                if(result.status in 200..299) {
                    val app=result.body.optJSONObject("application") ?: return@execute
                    runOnUiThread {
                        status.text=when(app.optString("status")) {
                            "pending" -> "طلبك قيد مراجعة الإدارة"
                            "approved" -> "تمت الموافقة على وكالتك"
                            "rejected" -> "تم رفض الطلب: "+app.optString("review_note")
                            else -> ""
                        }
                        if(app.optString("status")=="pending" || app.optString("status")=="approved") submit.isEnabled=false
                    }
                }
            } catch (_:Exception) {}
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
