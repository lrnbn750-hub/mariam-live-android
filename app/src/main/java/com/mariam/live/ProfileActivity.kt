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
    private lateinit var royalFrame:RoyalGifFrame
    private lateinit var frameToggle:TextView
    private var royalFrameOwned=false
    private var royalFrameEquipped=false
    private val royalFrameItemId="royal100-frame"
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
        val green=Color.rgb(35,157,91)
        val dark=Color.rgb(34,48,40)
        val muted=Color.rgb(112,128,118)
        val pale=Color.rgb(241,249,244)
        purple=green
        window.statusBarColor=Color.WHITE
        window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        fun panel()=android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.WHITE);cornerRadius=dp(20).toFloat()
            setStroke(dp(1),Color.rgb(225,237,228))
        }
        fun actionButton(textValue:String,fill:Int,textColor:Int)=TextView(this).apply{
            text=textValue;textSize=14f;gravity=Gravity.CENTER
            typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(textColor)
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(fill);cornerRadius=dp(14).toFloat()
            }
            setPadding(dp(14),dp(13),dp(14),dp(13))
        }

        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248,251,249))
        }
        val top=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(16),dp(8),dp(16),dp(8));setBackgroundColor(Color.WHITE)
        }
        val back=TextView(this).apply{
            text="‹";textSize=32f;setTextColor(green);gravity=Gravity.CENTER
            setOnClickListener{finish()}
        }
        top.addView(back,LinearLayout.LayoutParams(dp(42),dp(48)))
        top.addView(TextView(this).apply{
            text="ملفي الشخصي";textSize=20f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(dark)
        },LinearLayout.LayoutParams(0,dp(48),1f))
        top.addView(TextView(this).apply{
            text="MARIAM LIVE";textSize=10f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(green)
        })
        root.addView(top,LinearLayout.LayoutParams(-1,-2))

        val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=false}
        val content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(16),dp(16),dp(28))
        }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

        val hero=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            setPadding(dp(18),dp(22),dp(18),dp(20))
            background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(28,139,78),Color.rgb(61,184,112),Color.rgb(20,119,67))
            ).apply{cornerRadius=dp(24).toFloat()}
        }
        hero.addView(TextView(this).apply{
            text="حسابك • هويتك • حضورك";textSize=13f
            setTextColor(Color.rgb(226,255,237))
        })
        avatarPreview=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP;clipToOutline=true
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(226,244,232));setStroke(dp(3),Color.WHITE)
            }
            setImageDrawable(android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(226,244,232))
            })
            contentDescription="الصورة الشخصية"
        }
        val avatarFrame=FrameLayout(this).apply{clipChildren=false;clipToPadding=false}
        avatarFrame.addView(avatarPreview,FrameLayout.LayoutParams(dp(108),dp(108),Gravity.CENTER))
        avatarRing=View(this).apply{
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.TRANSPARENT);setStroke(dp(4),Color.rgb(255,225,145))
            }
            isClickable=false;isFocusable=false
        }
        avatarFrame.addView(avatarRing,FrameLayout.LayoutParams(dp(120),dp(120),Gravity.CENTER))
        royalFrame=RoyalGifFrame(this).apply{visibility=View.GONE}
        avatarFrame.addView(royalFrame,FrameLayout.LayoutParams(dp(130),dp(126),Gravity.CENTER))
        hero.addView(avatarFrame,LinearLayout.LayoutParams(dp(130),dp(130)).apply{topMargin=dp(10)})
        hero.addView(TextView(this).apply{
            text="صورتك الشخصية";textSize=16f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE);gravity=Gravity.CENTER
        })
        frameToggle=actionButton("جارٍ فحص الإطار...",Color.WHITE,green).apply{
            setOnClickListener{toggleRoyalFrame()}
        }
        hero.addView(frameToggle,LinearLayout.LayoutParams(dp(205),-2).apply{topMargin=dp(10)})
        content.addView(hero,LinearLayout.LayoutParams(-1,-2))

        levelCard=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14))
            background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(25,105,65),Color.rgb(38,143,83),Color.rgb(20,91,56))
            ).apply{cornerRadius=dp(20).toFloat();setStroke(dp(1),Color.rgb(130,207,157))}
        }
        val levelHeader=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
        levelHeader.addView(TextView(this).apply{
            text="✦ مستوى Mariam Live";textSize=14f
            typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)
        },LinearLayout.LayoutParams(0,dp(30),1f))
        levelNumber=TextView(this).apply{
            text="LV 1";textSize=23f
            typeface=android.graphics.Typeface.create("sans-serif-black",android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(255,230,165));gravity=Gravity.CENTER
        }
        levelHeader.addView(levelNumber);levelCard.addView(levelHeader)
        levelRank=TextView(this).apply{
            text="المبتدئ";textSize=13f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE);setPadding(0,dp(4),0,dp(8))
        }
        levelCard.addView(levelRank)
        levelProgress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
            max=1000;progress=0
            progressDrawable=android.graphics.drawable.ClipDrawable(
                android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.rgb(255,237,177),Color.rgb(255,199,89))
                ).apply{cornerRadius=dp(8).toFloat()},
                Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL
            )
            progressBackgroundTintList=android.content.res.ColorStateList.valueOf(Color.rgb(24,92,57))
        }
        levelCard.addView(levelProgress,LinearLayout.LayoutParams(-1,dp(9)))
        levelXp=TextView(this).apply{
            text="0 / 100 XP";textSize=11f;setTextColor(Color.rgb(226,255,237))
            gravity=Gravity.END;setPadding(0,dp(7),0,0)
        }
        levelCard.addView(levelXp)
        content.addView(levelCard,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})

        val toolsCard=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=panel()
        }
        toolsCard.addView(TextView(this).apply{
            text="أدوات الحساب";textSize=17f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(dark)
        })
        val inventoryButton=actionButton("🎒  حقيبتي — الإطارات والخلفيات",pale,green).apply{
            setOnClickListener{startActivity(Intent(this@ProfileActivity,InventoryActivity::class.java))}
        }
        toolsCard.addView(inventoryButton,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        val choosePhoto=actionButton("＋  تغيير الصورة الشخصية",pale,green).apply{setOnClickListener{pickPhoto()}}
        toolsCard.addView(choosePhoto,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
        content.addView(toolsCard,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})

        val details=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(15),dp(16),dp(16));background=panel()
        }
        details.addView(TextView(this).apply{
            text="بيانات الحساب";textSize=17f;typeface=android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(dark)
        })
        details.addView(caption("رقم الحساب (ID)"))
        val idView=TextView(this).apply{
            text="جارٍ تحميل المعرّف...";textSize=14f;setTextColor(green)
            setPadding(dp(13),dp(13),dp(13),dp(13))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(pale);cornerRadius=dp(12).toFloat()
            }
        }
        details.addView(idView,LinearLayout.LayoutParams(-1,-2))
        details.addView(caption("اسم العرض"))
        displayName=field("اكتب الاسم الذي يظهر للناس");details.addView(displayName,LinearLayout.LayoutParams(-1,-2))
        details.addView(caption("نبذة عنك"))
        bio=field("اكتب نبذة قصيرة").apply{minLines=2;gravity=Gravity.TOP or Gravity.START}
        details.addView(bio,LinearLayout.LayoutParams(-1,-2))
        details.addView(caption("الدولة"))
        country=field("الدولة");details.addView(country,LinearLayout.LayoutParams(-1,-2))
        status=TextView(this).apply{
            textSize=12f;setTextColor(Color.rgb(180,65,75));setPadding(0,dp(10),0,dp(4))
        }
        details.addView(status)
        val save=actionButton("حفظ التعديلات",green,Color.WHITE).apply{setOnClickListener{saveProfile()}}
        details.addView(save,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        content.addView(details,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})

        val logout=actionButton("تسجيل الخروج",Color.rgb(255,242,242),Color.rgb(190,48,65)).apply{
            setOnClickListener{
                android.app.AlertDialog.Builder(this@ProfileActivity)
                    .setTitle("تسجيل الخروج")
                    .setMessage("هل تريد تسجيل الخروج من حسابك؟")
                    .setNegativeButton("إلغاء",null)
                    .setPositiveButton("تسجيل الخروج"){_,_->performLogout()}
                    .show()
            }
        }
        content.addView(logout,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
        setContentView(root)
        loadProfile(idView)
        loadLevelProgress()
        loadRoyalFrameState()
    }

    override fun onResume(){
        super.onResume()
        if(::frameToggle.isInitialized) loadRoyalFrameState()
    }

    private fun loadRoyalFrameState(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{
            try{
                val result=AuthApi.get("/api/inventory",token)
                if(result.status !in 200..299)throw Exception("تعذر تحميل الإطار")
                val items=result.body.optJSONArray("items")?:org.json.JSONArray()
                var owned=false
                var equipped=false
                var equippedUrl=""
                var equippedId=""
                for(i in 0 until items.length()){
                    val item=items.optJSONObject(i)?:continue
                    if(item.optString("item_type")=="frame" && item.optBoolean("owned") && item.optBoolean("equipped")){
                        equipped=true
                        equippedId=item.optString("item_id")
                        equippedUrl=item.optString("asset_url")
                        break
                    }
                    if(item.optString("item_id")==royalFrameItemId) owned=item.optBoolean("owned")
                }
                runOnUiThread{
                    if(isFinishing)return@runOnUiThread
                    royalFrameOwned=owned
                    royalFrameEquipped=equipped
                    renderRoyalFrameState()
                    if(equipped && equippedUrl.startsWith("https://")){
                        executor.execute{
                            try{
                                val bytes=URL(equippedUrl).openConnection().apply{connectTimeout=12000;readTimeout=15000}.getInputStream().use{it.readBytes()}
                                runOnUiThread{if(!isFinishing)royalFrame.setGif(bytes)}
                            }catch(e:Exception){
                                android.util.Log.e("ProfileFrame","Could not load equipped frame $equippedId",e)
                            }
                        }
                    }
                }
            }catch(e:Exception){
                runOnUiThread{
                    if(!isFinishing){
                        frameToggle.text="تعذر تحميل حالة الإطار"
                        frameToggle.isEnabled=true
                    }
                }
            }
        }
    }

    private fun renderRoyalFrameState(){
        if(royalFrameEquipped){
            avatarRing.visibility=View.GONE
            royalFrame.visibility=View.VISIBLE
            royalFrame.startAnimation()
            frameToggle.text="إيقاف الإطار  ◉"
            frameToggle.setTextColor(Color.rgb(190,48,65))
        }else{
            royalFrame.stopAnimation()
            royalFrame.visibility=View.GONE
            avatarRing.visibility=View.VISIBLE
            frameToggle.text=if(royalFrameOwned)"تشغيل الإطار الملكي" else "الإطار الملكي غير متوفر"
            frameToggle.setTextColor(if(royalFrameOwned)purple else Color.GRAY)
        }
        frameToggle.isEnabled=royalFrameOwned || royalFrameEquipped
        frameToggle.alpha=if(frameToggle.isEnabled)1f else 0.65f
    }

    private fun toggleRoyalFrame(){
        if(!royalFrameOwned)return
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        frameToggle.isEnabled=false
        frameToggle.text="جارٍ تحديث الإطار..."
        executor.execute{
            try{
                val result=if(royalFrameEquipped){
                    AuthApi.post("/api/inventory/unequip",JSONObject().put("item_type","frame"),token)
                }else{
                    AuthApi.post("/api/inventory/equip",JSONObject().put("item_id",royalFrameItemId),token)
                }
                if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر تحديث الإطار"))
                runOnUiThread{
                    loadRoyalFrameState()
                }
            }catch(e:Exception){
                runOnUiThread{
                    frameToggle.text=e.message?:"تعذر تحديث الإطار"
                    frameToggle.isEnabled=true
                }
            }
        }
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
        // The frame is controlled by the equipped inventory item, not level alone.
        if(!royalFrameEquipped){
            royalFrame.stopAnimation()
            royalFrame.visibility=View.GONE
            avatarRing.visibility=View.VISIBLE
            avatarRing.background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(if(level>=91)4 else 3),colors[3])
            }
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
    override fun onDestroy(){royalFrame.stopAnimation();executor.shutdownNow();super.onDestroy()}
}

private class RoyalGifFrame(context:android.content.Context):View(context){
    private var movie:android.graphics.Movie?=null
    private var startedAt=0L
    private var running=false

    fun setGif(bytes:ByteArray){
        movie=android.graphics.Movie.decodeByteArray(bytes,0,bytes.size)
        startedAt=android.os.SystemClock.uptimeMillis()
        startAnimation()
    }

    fun startAnimation(){
        if(movie==null)return
        running=true
        startedAt=android.os.SystemClock.uptimeMillis()
        visibility=View.VISIBLE
        postInvalidateOnAnimation()
    }

    fun stopAnimation(){
        running=false
        invalidate()
    }

    override fun onDraw(canvas:android.graphics.Canvas){
        super.onDraw(canvas)
        val gif=movie?:return
        if(!running)return
        val duration=gif.duration().takeIf{it>0}?:1000
        gif.setTime(((android.os.SystemClock.uptimeMillis()-startedAt)%duration).toInt())
        val scale=minOf(width.toFloat()/gif.width().coerceAtLeast(1),height.toFloat()/gif.height().coerceAtLeast(1))
        val save=canvas.save()
        canvas.translate((width-gif.width()*scale)/2f,(height-gif.height()*scale)/2f)
        canvas.scale(scale,scale)
        gif.draw(canvas,0f,0f)
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }
}
