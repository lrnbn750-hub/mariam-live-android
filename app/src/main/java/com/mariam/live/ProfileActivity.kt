package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

class ProfileActivity : Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    private val purple=Color.rgb(112,75,246)
    private var userId=""
    private var avatarUrl=""
    private var selectedPhoto:Uri?=null
    private lateinit var displayName:EditText
    private lateinit var bio:EditText
    private lateinit var country:EditText
    private lateinit var avatarPreview:ImageView
    private lateinit var status:TextView
    private val photoPickerCode=7421
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun field(hintText:String)=EditText(this).apply{
        hint=hintText;textSize=15f;setTextColor(Color.rgb(35,34,53))
        setPadding(dp(14),dp(12),dp(14),dp(12))
        background=android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.WHITE);cornerRadius=dp(13).toFloat()
            setStroke(dp(1),Color.rgb(226,222,238))
        }
    }
    private fun caption(s:String)=TextView(this).apply{
        text=s;textSize=13f;setTextColor(Color.rgb(85,80,105));typeface=android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0,dp(12),0,dp(5))
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(250,249,253);window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(18),dp(20),dp(22));setBackgroundColor(Color.rgb(250,249,253))}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val back=TextView(this).apply{text="‹";textSize=30f;setTextColor(purple);gravity=Gravity.CENTER;setOnClickListener{finish()}}
        top.addView(back,LinearLayout.LayoutParams(dp(42),dp(48)))
        top.addView(TextView(this).apply{text="الملف الشخصي";textSize=20f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(35,34,53))},LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(top)
        val card=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(18));background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(20).toFloat()}}
        card.addView(TextView(this).apply{text="معلومات حسابك";textSize=18f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(35,34,53))})
        card.addView(caption("الصورة الشخصية"))
        avatarPreview=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP
            clipToOutline=true
            background=android.graphics.drawable.GradientDrawable().apply{shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(239,233,255));setStroke(dp(2),purple)}
            setImageDrawable(android.graphics.drawable.GradientDrawable().apply{shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(239,233,255))})
            contentDescription="الصورة الشخصية"
        }
        card.addView(avatarPreview,LinearLayout.LayoutParams(dp(112),dp(112)).apply{gravity=Gravity.CENTER;topMargin=dp(6);bottomMargin=dp(10)})
        val choosePhoto=TextView(this).apply{
            text="اختيار صورة من الهاتف";textSize=14f;gravity=Gravity.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(purple)
            background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(246,243,255));cornerRadius=dp(13).toFloat();setStroke(dp(1),Color.rgb(218,207,255))}
            setPadding(dp(14),dp(13),dp(14),dp(13))
            setOnClickListener{pickPhoto()}
        }
        card.addView(choosePhoto,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("رقم الحساب (ID)"))
        val idView=TextView(this).apply{text="جارٍ تحميل المعرّف...";textSize=13f;setTextColor(purple);setPadding(dp(12),dp(13),dp(12),dp(13));background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(246,243,255));cornerRadius=dp(12).toFloat()}}
        card.addView(idView,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("اسم العرض"))
        displayName=field("اكتب الاسم الذي يظهر للناس");card.addView(displayName,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("نبذة عنك"))
        bio=field("اكتب نبذة قصيرة");card.addView(bio,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("الدولة"))
        country=field("الدولة");card.addView(country,LinearLayout.LayoutParams(-1,-2))
        status=TextView(this).apply{textSize=12f;setTextColor(Color.rgb(180,65,75));setPadding(0,dp(10),0,dp(4))}
        card.addView(status)
        val save=TextView(this).apply{text="حفظ التعديلات";textSize=15f;gravity=Gravity.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=android.graphics.drawable.GradientDrawable().apply{setColor(purple);cornerRadius=dp(14).toFloat()};setPadding(0,dp(15),0,dp(15));setOnClickListener{saveProfile()}}
        card.addView(save,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        val logout=TextView(this).apply{
            text="تسجيل الخروج";textSize=15f;gravity=Gravity.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(190,48,65))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.rgb(255,242,244));cornerRadius=dp(14).toFloat()
                setStroke(dp(1),Color.rgb(245,210,216))
            }
            setPadding(0,dp(15),0,dp(15))
            setOnClickListener{
                android.app.AlertDialog.Builder(this@ProfileActivity)
                    .setTitle("تسجيل الخروج")
                    .setMessage("هل تريد تسجيل الخروج من حسابك؟")
                    .setNegativeButton("إلغاء",null)
                    .setPositiveButton("تسجيل الخروج"){_,_->performLogout()}
                    .show()
            }
        }
        card.addView(logout,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
        root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
        setContentView(ScrollView(this).apply{addView(root)})
        loadProfile(idView)
    }
    private fun pickPhoto(){
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="image/*"
        }
        startActivityForResult(intent,photoPickerCode)
    }
    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==photoPickerCode && resultCode==RESULT_OK){
            val uri=data?.data?:return
            selectedPhoto=uri
            try{
                contentResolver.openInputStream(uri)?.use{stream->
                    val bitmap=BitmapFactory.decodeStream(stream)
                    if(bitmap!=null)avatarPreview.setImageBitmap(bitmap)
                }
            }catch(_:Exception){}
            uploadPhoto(uri)
        }
    }
    private fun loadProfile(idView:TextView){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){finish();return}
        status.text="جارٍ تحميل الملف الشخصي..."
        executor.execute{
            try{
                val me=AuthApi.get("/api/profile",token)
                if(me.status !in 200..299)throw Exception(me.body.optString("error","تعذر تحميل الحساب"))
                val user=me.body.optJSONObject("profile")?:me.body
                userId=user.optString("id")
                // Use the authenticated account response directly. The old /api/users/:id
                // endpoint is not provided by this backend and could return an HTML 404 page.
                val url=user.optString("avatar_url").takeUnless{it=="null"}?:""
                runOnUiThread{
                    idView.text=if(user.has("public_id") && !user.isNull("public_id")) user.optString("public_id") else "حسابك"
                    displayName.setText(user.optString("display_name").takeUnless{it=="null"}?:user.optString("username"))
                    bio.setText(user.optString("bio").takeUnless{it=="null"}?:"")
                    country.setText(user.optString("country").takeUnless{it=="null"}?:"")
                    avatarUrl=url
                    status.text=""
                    if(url.isNotBlank())loadAvatarPreview(url)
                }
            }catch(e:Exception){runOnUiThread{status.text=e.message?:"تعذر تحميل البيانات"}}
        }
    }
    private fun loadAvatarPreview(url:String){
        executor.execute{
            try{
                val bitmap=URL(url).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{BitmapFactory.decodeStream(it)}
                if(bitmap!=null)runOnUiThread{if(!isFinishing)avatarPreview.setImageBitmap(bitmap)}
            }catch(_:Exception){}
        }
    }
    private fun uploadPhoto(uri:Uri){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        status.setTextColor(Color.rgb(85,80,105));status.text="جارٍ تجهيز الصورة ورفعها..."
        executor.execute{
            try{
                val source=contentResolver.openInputStream(uri)?.use{it.readBytes()}?:throw Exception("تعذر قراءة الصورة")
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeByteArray(source,0,source.size,bounds)
                var sample=1
                while(bounds.outWidth/sample>1200 || bounds.outHeight/sample>1200)sample*=2
                val bitmap=BitmapFactory.decodeByteArray(source,0,source.size,BitmapFactory.Options().apply{inSampleSize=sample})
                    ?:throw Exception("صيغة الصورة غير مدعومة")
                val scaled=if(bitmap.width>900 || bitmap.height>900){
                    val ratio=minOf(900f/bitmap.width,900f/bitmap.height)
                    Bitmap.createScaledBitmap(bitmap,(bitmap.width*ratio).toInt().coerceAtLeast(1),(bitmap.height*ratio).toInt().coerceAtLeast(1),true)
                }else bitmap
                val output=ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG,82,output)
                val bytes=output.toByteArray()
                if(bytes.size>1536*1024)throw Exception("الصورة كبيرة، اختر صورة أصغر")
                val payload=JSONObject().put("avatar_base64",Base64.encodeToString(bytes,Base64.NO_WRAP))
                val result=AuthApi.post("/api/profile/avatar",payload,token)
                if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر رفع الصورة"))
                avatarUrl=result.body.optString("avatar_url")
                runOnUiThread{
                    status.setTextColor(Color.rgb(35,145,95))
                    status.text="تم رفع الصورة الشخصية بنجاح، وستظهر في مقاعد الغرفة الصوتية"
                }
                if(scaled!==bitmap)scaled.recycle()
                if(bitmap!==scaled)bitmap.recycle()
            }catch(e:Exception){
                runOnUiThread{status.setTextColor(Color.rgb(180,65,75));status.text=e.message?:"تعذر رفع الصورة"}
            }
        }
    }
    private fun saveProfile(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        val name=displayName.text.toString().trim()
        if(name.isBlank()||name.length>60){status.text="اكتب اسماً من 1 إلى 60 حرفاً";return}
        val payload=JSONObject().put("display_name",name)
            .put("bio",bio.text.toString().trim())
            .put("country",country.text.toString().trim())
            .put("avatar_url",avatarUrl)
        status.setTextColor(Color.rgb(85,80,105));status.text="جارٍ حفظ التعديلات..."
        executor.execute{
            try{
                val result=AuthApi.put("/api/profile",payload,token)
                runOnUiThread{
                    status.setTextColor(if(result.status in 200..299)Color.rgb(35,145,95) else Color.rgb(180,65,75))
                    status.text=if(result.status in 200..299)"تم حفظ الملف الشخصي بنجاح" else result.body.optString("error","تعذر حفظ التعديلات")
                }
            }catch(_:Exception){runOnUiThread{status.text="تعذر الاتصال بالخادم"}}
        }
    }
    private fun performLogout(){
        getSharedPreferences("MariamLive",MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this,LoginActivity::class.java).apply{
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
