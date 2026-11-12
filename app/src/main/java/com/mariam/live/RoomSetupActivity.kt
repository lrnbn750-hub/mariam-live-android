package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

class RoomSetupActivity : Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    private val green=Color.rgb(72,125,86)
    private val dark=Color.rgb(48,62,51)
    private val muted=Color.rgb(112,122,111)
    private val pale=Color.rgb(241,244,237)
    private lateinit var titleInput:EditText
    private lateinit var avatarPreview:ImageView
    private lateinit var startButton:TextView
    private var profileAvatarUrl=""
    private var profileAvatar:Bitmap?=null
    private var selectedCapacity=8
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,radius:Int,stroke:Int=0)=GradientDrawable().apply{
        setColor(color);cornerRadius=dp(radius).toFloat()
        if(stroke!=0)setStroke(dp(1),stroke)
    }
    private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
        text=value;textSize=size;setTextColor(color);gravity=Gravity.CENTER
        typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
    }
    private fun caption(value:String)=text(value,13f,dark,true).apply{
        gravity=Gravity.START
        setPadding(0,dp(15),0,dp(7))
    }
    private fun placeholderBitmap():Bitmap{
        val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader=android.graphics.LinearGradient(0f,0f,512f,512f,
            intArrayOf(Color.rgb(224,234,220),Color.rgb(192,211,190)),null,android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,512f,512f,paint)
        paint.shader=null;paint.color=green;paint.textAlign=Paint.Align.CENTER;paint.textSize=190f;paint.typeface=Typeface.DEFAULT_BOLD
        canvas.drawText("B",256f,325f,paint)
        return bitmap
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(248,249,245)
        window.navigationBarColor=Color.rgb(248,249,245)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null).orEmpty()
        val root=LinearLayout(this).apply{
            orientation=1
            setBackgroundColor(Color.rgb(248,249,245))
            layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        val top=LinearLayout(this).apply{
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(16),dp(7),dp(16),dp(7))
            setBackgroundColor(Color.WHITE)
        }
        top.addView(text("×",30f,green,true).apply{setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(46)))
        top.addView(text("إعداد البث المباشر",19f,dark,true),LinearLayout.LayoutParams(0,dp(46),1f))
        top.addView(text("BAK LIVE",11f,green,true),LinearLayout.LayoutParams(-2,dp(46)))
        root.addView(top,LinearLayout.LayoutParams(-1,-2))
        val scroll=ScrollView(this).apply{clipToPadding=false}
        val content=LinearLayout(this).apply{
            orientation=1
            setPadding(dp(18),dp(18),dp(18),dp(22))
        }
        scroll.addView(content)
        content.addView(text("جهّز غرفتك وابدأ لقاءك",23f,dark,true).apply{gravity=Gravity.START})
        content.addView(text("إعداد بسيط قبل دخولك إلى البث الصوتي",13f,muted).apply{
            gravity=Gravity.START;setPadding(0,dp(5),0,dp(16))
        })
        val profileCard=LinearLayout(this).apply{
            orientation=0;gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(14),dp(14),dp(14),dp(14))
            background=rounded(Color.WHITE,20,Color.rgb(222,229,218))
        }
        avatarPreview=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP
            background=rounded(pale,40,Color.rgb(190,207,187))
            clipToOutline=true
            setImageBitmap(placeholderBitmap())
        }
        profileCard.addView(avatarPreview,LinearLayout.LayoutParams(dp(68),dp(68)))
        val profileInfo=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)}
        profileInfo.addView(text("صورة ملفك الشخصي",15f,dark,true).apply{gravity=Gravity.START})
        profileInfo.addView(text("ستُستخدم تلقائياً كصورة الغرفة",12f,muted).apply{
            gravity=Gravity.START;setPadding(0,dp(5),0,0)
        })
        profileCard.addView(profileInfo,LinearLayout.LayoutParams(0,-2,1f))
        profileCard.addView(text("تلقائي",11f,green,true).apply{
            setPadding(dp(10),dp(7),dp(10),dp(7));background=rounded(pale,15)
        })
        content.addView(profileCard,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(18)})
        content.addView(caption("اسم الغرفة"))
        titleInput=EditText(this).apply{
            setText(intent.getStringExtra("room_name").orEmpty())
            hint="مثلاً: سهرة الأصدقاء"
            textSize=15f;setTextColor(dark);setHintTextColor(Color.rgb(151,158,148))
            setSingleLine(true);setPadding(dp(14),dp(12),dp(14),dp(12))
            background=rounded(Color.WHITE,14,Color.rgb(218,226,215))
        }
        content.addView(titleInput,LinearLayout.LayoutParams(-1,dp(52)))
        content.addView(caption("عدد مقاعد المتحدثين"))
        val capacities=LinearLayout(this).apply{gravity=Gravity.CENTER;orientation=0}
        listOf(4,6,8).forEach{count->
            val chip=text("$count مقاعد",13f,if(count==8)Color.WHITE else dark,true).apply{
                setPadding(dp(12),dp(11),dp(12),dp(11))
                background=rounded(if(count==8)green else Color.WHITE,14,if(count==8)green else Color.rgb(218,226,215))
                setOnClickListener{
                    selectedCapacity=count
                    for(i in 0 until capacities.childCount){
                        val child=capacities.getChildAt(i) as TextView
                        val active=child.tag==count
                        child.setTextColor(if(active)Color.WHITE else dark)
                        child.background=rounded(if(active)green else Color.WHITE,14,if(active)green else Color.rgb(218,226,215))
                    }
                }
                tag=count
            }
            capacities.addView(chip,LinearLayout.LayoutParams(0,dp(44),1f).apply{setMargins(dp(4),0,dp(4),0)})
        }
        content.addView(capacities,LinearLayout.LayoutParams(-1,-2))
        val info=LinearLayout(this).apply{
            orientation=1;setPadding(dp(14),dp(12),dp(14),dp(12))
            background=rounded(Color.rgb(237,243,234),16)
        }
        info.addView(text("ماذا سيظهر للمستخدمين؟",13f,dark,true).apply{gravity=Gravity.START})
        info.addView(text("صورتك الشخصية واسم الغرفة في بطاقة البث بالصفحة الرئيسية. لا تحتاج إلى اختيار خلفية أو رفع صورة إضافية.",12f,muted).apply{
            gravity=Gravity.START;setPadding(0,dp(6),0,0)
        })
        content.addView(info,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)})
        content.addView(text("الخلفية اختيارية بالكامل، وسيتم استخدام تصميم الغرفة الافتراضي.",11f,muted).apply{
            gravity=Gravity.CENTER;setPadding(0,dp(14),0,dp(8))
        })
        startButton=text("ابدأ البث المباشر  ▶",16f,Color.WHITE,true).apply{
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(104,145,104),green)).apply{cornerRadius=dp(18).toFloat()}
            elevation=dp(2).toFloat()
            setOnClickListener{createAndStart(token)}
        }
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(startButton,LinearLayout.LayoutParams(-1,dp(56)).apply{setMargins(dp(18),dp(8),dp(18),dp(12))})
        ViewCompat.setOnApplyWindowInsetsListener(startButton){ view,insets->
            val bottomInset=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp=view.layoutParams
            val targetHeight=dp(56)+bottomInset
            if(lp.height!=targetHeight){lp.height=targetHeight;view.layoutParams=lp}
            view.setPadding(0,0,0,bottomInset)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(startButton)
        loadProfileAvatar(token)
    }
    private fun loadProfileAvatar(token:String){
        if(token.isBlank())return
        executor.execute{
            try{
                val response=AuthApi.get("/api/profile",token)
                if(response.status !in 200..299)return@execute
                val profile=response.body.optJSONObject("profile")?:JSONObject()
                profileAvatarUrl=profile.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
                if(profileAvatarUrl.isNotBlank()){
                    val bitmap=URL(profileAvatarUrl).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{BitmapFactory.decodeStream(it)}
                    if(bitmap!=null){
                        profileAvatar=bitmap
                        runOnUiThread{if(!isFinishing)avatarPreview.setImageBitmap(bitmap)}
                    }
                }
            }catch(_:Exception){}
        }
    }
    private fun createAndStart(token:String){
        val name=titleInput.text.toString().trim()
        if(name.length<2){Toast.makeText(this,"اكتب اسم الغرفة (حرفان على الأقل)",Toast.LENGTH_SHORT).show();return}
        if(token.isBlank()){Toast.makeText(this,"سجّل الدخول أولاً",Toast.LENGTH_SHORT).show();finish();return}
        startButton.isEnabled=false
        startButton.text="جارٍ تجهيز غرفتك..."
        executor.execute{
            try{
                val source=profileAvatar?:placeholderBitmap()
                val maxSide=1280
                val scale=minOf(1f,maxSide.toFloat()/maxOf(source.width,source.height))
                val resized=if(scale<1f)Bitmap.createScaledBitmap(source,(source.width*scale).toInt().coerceAtLeast(1),(source.height*scale).toInt().coerceAtLeast(1),true)else source
                val bytes=ByteArrayOutputStream()
                var quality=85
                do{bytes.reset();resized.compress(Bitmap.CompressFormat.JPEG,quality,bytes);quality-=10}while(bytes.size()>3*1024*1024&&quality>=45)
                if(bytes.size()>3*1024*1024)throw Exception("تعذر تجهيز صورة الغرفة")
                val payload=JSONObject().put("name",name).put("max_members",selectedCapacity)
                    .put("image_mime","image/jpeg")
                    .put("image_base64",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP))
                if(resized!==source)resized.recycle()
                if(source!==profileAvatar&&!source.isRecycled)source.recycle()
                val result=AuthApi.post("/api/voice-rooms",payload,token)
                if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر إنشاء الغرفة"))
                val room=result.body.optJSONObject("room")?:throw Exception("استجابة الخادم غير مكتملة")
                val roomId=room.optString("id")
                runOnUiThread{
                    startActivity(Intent(this,RoomActivity::class.java).putExtra("room_id",roomId).putExtra("room_name",name))
                    finish()
                }
            }catch(e:Exception){
                runOnUiThread{
                    startButton.isEnabled=true;startButton.text="ابدأ البث المباشر  ▶"
                    Toast.makeText(this,e.message?:"تعذر إنشاء الغرفة",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
