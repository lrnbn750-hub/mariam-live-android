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
    private lateinit var avatarRing:View
    private lateinit var status:TextView
    private lateinit var levelNumber:TextView
    private lateinit var levelCard:LinearLayout
    private lateinit var levelRank:TextView
    private lateinit var levelXp:TextView
    private lateinit var levelProgress:ProgressBar
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
        val avatarFrame=FrameLayout(this).apply{
            clipChildren=false;clipToPadding=false
        }
        avatarFrame.addView(avatarPreview,FrameLayout.LayoutParams(dp(106),dp(106),Gravity.CENTER))
        avatarRing=View(this).apply{
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(4),Color.rgb(201,158,75))
            }
            isClickable=false
            isFocusable=false
        }
        avatarFrame.addView(avatarRing,FrameLayout.LayoutParams(dp(120),dp(120),Gravity.CENTER))
        card.addView(avatarFrame,LinearLayout.LayoutParams(dp(120),dp(120)).apply{
            gravity=Gravity.CENTER;topMargin=dp(6);bottomMargin=dp(10)
        })

        levelCard=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(14),dp(16),dp(14))
            background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(34,25,16),Color.rgb(83,54,22),Color.rgb(35,25,17))
            ).apply{cornerRadius=dp(18).toFloat();setStroke(dp(1),Color.rgb(201,158,75))}
        }
        val levelHeader=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
        levelHeader.addView(TextView(this).apply{
            text="✦  مستوى Mariam Live"
            textSize=14f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(255,224,155))
        },LinearLayout.LayoutParams(0,dp(28),1f))
        levelNumber=TextView(this).apply{
            text="LV 1";textSize=22f;typeface=android.graphics.Typeface.create("sans-serif-black",android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(255,215,115));gravity=Gravity.CENTER
        }
        levelHeader.addView(levelNumber)
        levelCard.addView(levelHeader)
        levelRank=TextView(this).apply{
            text="المبتدئ";textSize=13f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE);setPadding(0,dp(5),0,dp(7))
        }
        levelCard.addView(levelRank)
        levelProgress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
            max=1000;progress=0
            progressDrawable=android.graphics.drawable.ClipDrawable(
                android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.rgb(255,226,143),Color.rgb(194,130,36))
                ).apply{cornerRadius=dp(8).toFloat()},
                Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL
            )
            progressBackgroundTintList=android.content.res.ColorStateList.valueOf(Color.rgb(91,69,43))
        }
        levelCard.addView(levelProgress,LinearLayout.LayoutParams(-1,dp(9)))
        levelXp=TextView(this).apply{
            text="0 / 100 XP";textSize=11f;setTextColor(Color.rgb(238,213,167))
            gravity=Gravity.END;setPadding(0,dp(7),0,0)
        }
        levelCard.addView(levelXp)
        card.addView(levelCard,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12);bottomMargin=dp(5)})
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
        loadLevelProgress()
    }

    private fun loadLevelProgress(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{
            try{
                val result=AuthApi.get("/api/levels/me",token)
                if(result.status !in 200..299)throw Exception("تعذر تحميل المستوى")
                val level=result.body.optInt("level",1).coerceIn(1,100)
                val total=result.body.optLong("total_xp",0L).coerceAtLeast(0L)
                val current=result.body.optLong("current_level_xp",0L).coerceAtLeast(0L)
                val next=result.body.optLong("next_level_xp",100L).coerceAtLeast(1L)
                val progress=result.body.optDouble("progress",0.0).coerceIn(0.0,1.0)
                val rank=when(level){
                    in 1..10->"المبتدئ"
                    in 11..20->"الهاوي"
                    in 21..30->"النشيط"
                    in 31..40->"المحترف"
                    in 41..50->"الخبير"
                    in 51..60->"الأسطورة"
                    in 61..70->"الملكي"
                    in 71..80->"الإمبراطوري"
                    in 81..90->"النخبوي"
                    in 91..99->"الأسطوري الذهبي"
                    else->"الإمبراطور الأعظم"
                }
                runOnUiThread{
                    if(isFinishing)return@runOnUiThread
                    applyLevelTheme(level)
                    levelNumber.text="LV $level"
                    levelRank.text=rank
                    levelProgress.progress=(progress*1000).toInt()
                    levelXp.text=if(level>=100)"MAX LEVEL  •  $total XP" else "$current / $next XP  •  المجموع $total"
                }
            }catch(_:Exception){
                runOnUiThread{
                    if(!isFinishing){
                        levelNumber.text="LV 1"
                        levelRank.text="تعذر تحميل المستوى"
                        levelXp.text="تحقق من اتصال الخادم"
                    }
                }
            }
        }
    }

    // Visual tier palette: each 10-level band has its own identity.
    private fun applyLevelTheme(level:Int){
        val colors = when(level){
            in 1..10 -> intArrayOf(Color.rgb(54,37,25),Color.rgb(112,75,43),Color.rgb(43,29,22),Color.rgb(218,157,91),Color.rgb(255,220,174),Color.rgb(226,173,113),Color.rgb(88,61,39))
            in 11..20 -> intArrayOf(Color.rgb(37,43,55),Color.rgb(104,116,133),Color.rgb(34,39,51),Color.rgb(192,205,221),Color.rgb(245,249,255),Color.rgb(218,228,241),Color.rgb(91,103,122))
            in 21..30 -> intArrayOf(Color.rgb(55,39,10),Color.rgb(150,105,20),Color.rgb(64,43,10),Color.rgb(255,207,70),Color.rgb(255,239,164),Color.rgb(255,218,105),Color.rgb(111,76,16))
            in 31..40 -> intArrayOf(Color.rgb(9,55,43),Color.rgb(17,119,83),Color.rgb(8,45,39),Color.rgb(72,222,164),Color.rgb(190,255,225),Color.rgb(102,238,184),Color.rgb(20,105,78))
            in 41..50 -> intArrayOf(Color.rgb(9,35,75),Color.rgb(22,91,160),Color.rgb(8,29,67),Color.rgb(83,174,255),Color.rgb(196,231,255),Color.rgb(112,190,255),Color.rgb(26,87,145))
            in 51..60 -> intArrayOf(Color.rgb(48,18,78),Color.rgb(117,48,160),Color.rgb(39,15,67),Color.rgb(206,130,255),Color.rgb(239,207,255),Color.rgb(211,151,255),Color.rgb(100,45,145))
            in 61..70 -> intArrayOf(Color.rgb(75,12,28),Color.rgb(164,31,59),Color.rgb(58,10,27),Color.rgb(255,105,132),Color.rgb(255,204,213),Color.rgb(255,137,157),Color.rgb(130,27,57))
            in 71..80 -> intArrayOf(Color.rgb(13,48,61),Color.rgb(34,133,151),Color.rgb(9,37,53),Color.rgb(105,231,245),Color.rgb(213,255,255),Color.rgb(135,239,247),Color.rgb(38,119,139))
            in 81..90 -> intArrayOf(Color.rgb(34,22,76),Color.rgb(81,48,151),Color.rgb(23,16,61),Color.rgb(169,145,255),Color.rgb(232,222,255),Color.rgb(183,165,255),Color.rgb(83,57,157))
            in 91..99 -> intArrayOf(Color.rgb(67,13,24),Color.rgb(151,28,44),Color.rgb(42,12,28),Color.rgb(255,180,83),Color.rgb(255,235,187),Color.rgb(255,197,112),Color.rgb(154,48,51))
            else -> intArrayOf(Color.rgb(38,12,54),Color.rgb(126,25,91),Color.rgb(43,12,65),Color.rgb(255,210,92),Color.rgb(255,245,190),Color.rgb(255,221,128),Color.rgb(190,53,112))
        }
        avatarRing.background=android.graphics.drawable.GradientDrawable().apply{
            shape=android.graphics.drawable.GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(dp(if(level>=100)5 else if(level>=91)4 else 3),colors[3])
        }
        levelCard.background=android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(colors[0],colors[1],colors[2])
        ).apply{
            cornerRadius=dp(18).toFloat()
            setStroke(dp(if(level==100)2 else 1),colors[3])
        }
        levelNumber.setTextColor(colors[4])
        levelRank.setTextColor(Color.WHITE)
        levelXp.setTextColor(colors[5])
        levelProgress.progressDrawable=android.graphics.drawable.ClipDrawable(
            android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(colors[4],colors[5],colors[3])
            ).apply{cornerRadius=dp(8).toFloat()},
            Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL
        )
        levelProgress.progressBackgroundTintList=android.content.res.ColorStateList.valueOf(colors[6])
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
                // First get the authenticated account and its public ID.
                val me=AuthApi.get("/api/auth/me",token)
                if(me.status !in 200..299)throw Exception(me.body.optString("error","تعذر تحميل الحساب"))
                val account=me.body.optJSONObject("user")
                    ?:throw Exception("استجابة الحساب لا تحتوي بيانات المستخدم")
                userId=account.optString("id")
                if(userId.isBlank() || userId=="null")throw Exception("لم يصل معرّف المستخدم من الخادم")

                // The backend exposes profile details through /api/users/:id.
                val response=AuthApi.get("/api/users/$userId",token)
                if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحميل الملف الشخصي"))
                val user=response.body.optJSONObject("user")
                    ?:throw Exception("استجابة الملف الشخصي غير مكتملة")
                val url=user.optString("avatar_url").takeUnless{it=="null"}?:""
                val publicId=account.optString("public_id").takeUnless{it=="null"}?:""

                runOnUiThread{
                    idView.text=if(publicId.isNotBlank())publicId else "حسابك"
                    displayName.setText(user.optString("display_name").takeUnless{it=="null"}?:user.optString("username"))
                    bio.setText(user.optString("bio").takeUnless{it=="null"}?:"")
                    country.setText(user.optString("country").takeUnless{it=="null"}?:"")
                    avatarUrl=when{
                        url.isBlank()->""
                        url.startsWith("http://") || url.startsWith("https://")->url
                        else->"https://nokiraq.duckdns.org/"+url.trimStart('/')
                    }
                    status.text=""
                    if(avatarUrl.isNotBlank())loadAvatarPreview(avatarUrl)
                }
            }catch(e:Exception){
                runOnUiThread{status.text=e.message?:"تعذر تحميل البيانات"}
            }
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
