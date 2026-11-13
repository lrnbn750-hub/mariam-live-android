package com.mariam.live

import android.app.Activity
import androidx.core.view.WindowCompat
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
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
    private val purple=Color.rgb(35,157,91)
    private var userId=""
    private var avatarUrl=""
    private var selectedPhoto:Uri?=null
    private lateinit var profileTitle:TextView
    private lateinit var publicIdLabel:TextView
    private var idViewRef:TextView?=null
    private lateinit var displayName:EditText
    private lateinit var bio:EditText
    private lateinit var country:EditText
    private lateinit var avatarPreview:ImageView
    private var editPhotoPreview:ImageView?=null
    private lateinit var avatarRing:View
    private lateinit var royalFrame:RoyalGifFrame
    private lateinit var frameToggle:TextView
    private var royalFrameOwned=false
    private var royalFrameEquipped=false
    private val royalFrameItemId="royal100-frame"
    private lateinit var status:TextView
    private lateinit var levelShortcutTitle:TextView
    private lateinit var levelShortcutIcon:ImageView
    private lateinit var levelShortcutCard:LinearLayout
    private lateinit var levelNumber:TextView
    private lateinit var levelCard:LinearLayout
    private lateinit var levelRank:TextView
    private lateinit var levelXp:TextView
    private lateinit var levelProgress:ProgressBar
    private val photoPickerCode=7421
    private lateinit var followingCountView:TextView
    private lateinit var followersCountView:TextView
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
        WindowCompat.setDecorFitsSystemWindows(window,true)
        if(android.os.Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=false
        val gold=Color.rgb(218,174,91)
        val ink=Color.rgb(38,29,58)
        val muted=Color.rgb(139,132,153)
        val cream=Color.rgb(255,248,231)
        window.statusBarColor=Color.rgb(35,24,57)
        window.navigationBarColor=Color.rgb(248,246,251)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        fun rounded(color:Int,r:Int,stroke:Int=Color.TRANSPARENT)=android.graphics.drawable.GradientDrawable().apply{
            setColor(color);cornerRadius=dp(r).toFloat()
            if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)
        }
        fun txt(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
            text=value;textSize=size;setTextColor(color);gravity=Gravity.CENTER
            typeface=android.graphics.Typeface.create("sans-serif",if(bold)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(248,246,251))}
        val scroll=ScrollView(this).apply{isFillViewport=true;overScrollMode=View.OVER_SCROLL_NEVER}
        val content=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(248,246,251))}
        scroll.addView(content)

        val hero=LinearLayout(this).apply{
            orientation=1;gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(18),dp(10),dp(18),dp(24))
            background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(31,20,54),Color.rgb(58,35,91),Color.rgb(39,27,66))
            )
        }
        val tools=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        tools.addView(ImageView(this).apply{
            setImageResource(R.drawable.ic_profile_close)
            setColorFilter(Color.rgb(235,218,177),android.graphics.PorterDuff.Mode.SRC_IN)
            scaleType=ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(10),dp(10),dp(10),dp(10))
            contentDescription="إغلاق الملف الشخصي"
            setOnClickListener{finish()}
        },LinearLayout.LayoutParams(dp(42),dp(42)))
        tools.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        hero.addView(tools,LinearLayout.LayoutParams(-1,dp(46)))

        avatarPreview=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP;clipToOutline=true
            background=rounded(Color.rgb(230,230,230),100,gold)
            setImageDrawable(android.graphics.drawable.GradientDrawable().apply{shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(230,230,230))})
            contentDescription="الصورة الشخصية"
        }
        val avatarFrame=FrameLayout(this).apply{clipChildren=false;clipToPadding=false}
        avatarFrame.addView(avatarPreview,FrameLayout.LayoutParams(dp(112),dp(112),Gravity.CENTER))
        avatarPreview.isClickable=false
        avatarPreview.setOnClickListener(null)
        avatarRing=View(this).apply{
            background=android.graphics.drawable.GradientDrawable().apply{shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.TRANSPARENT);setStroke(dp(4),gold)}
        }
        avatarFrame.addView(avatarRing,FrameLayout.LayoutParams(dp(122),dp(122),Gravity.CENTER))
        royalFrame=RoyalGifFrame(this).apply{visibility=View.GONE}
        avatarFrame.addView(royalFrame,FrameLayout.LayoutParams(dp(136),dp(132),Gravity.CENTER))
        val avatarEdit=ImageView(this).apply{
            setImageResource(R.drawable.ic_profile_edit)
            setColorFilter(Color.WHITE,android.graphics.PorterDuff.Mode.SRC_IN)
            scaleType=ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(8),dp(8),dp(8),dp(8))
            contentDescription="تعديل الصورة الشخصية"
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(126,83,181))
                setStroke(dp(2),Color.WHITE)
            }
            setOnClickListener{showEditProfile()}
        }
        avatarFrame.addView(avatarEdit,FrameLayout.LayoutParams(dp(34),dp(34),Gravity.TOP or Gravity.START).apply{
            topMargin=dp(8);marginStart=dp(3)
        })
        hero.addView(avatarFrame,LinearLayout.LayoutParams(dp(140),dp(140)).apply{topMargin=dp(22)})
        profileTitle=txt("جارٍ تحميل الاسم...",20f,Color.WHITE,true)
        hero.addView(profileTitle,LinearLayout.LayoutParams(-1,dp(38)))
        val idRow=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
        publicIdLabel=TextView(this).apply{text="ID: جارٍ التحميل";textSize=13f;setTextColor(Color.rgb(204,192,220))}
        idRow.addView(publicIdLabel,LinearLayout.LayoutParams(-2,dp(30)))
        idRow.addView(TextView(this).apply{text="نسخ";textSize=12f;setTextColor(Color.rgb(232,194,117));
            setPadding(dp(10),0,dp(10),0)
            setOnClickListener{
                val value=idViewRef?.text?.toString().orEmpty()
                if(value.isNotBlank() && value!="حسابك"){
                    val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("BAK ID",value))
                    Toast.makeText(this@ProfileActivity,"تم نسخ ID",Toast.LENGTH_SHORT).show()
                }
            }
        },LinearLayout.LayoutParams(-2,dp(30)))
        hero.addView(idRow,LinearLayout.LayoutParams(-1,dp(30)))
        content.addView(hero,LinearLayout.LayoutParams(-1,-2))

        val stats=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;setPadding(dp(8),dp(14),dp(8),dp(14));background=rounded(Color.WHITE,18)}
        fun stat(value:String,title:String,click:()->Unit={})=LinearLayout(this).apply{
            orientation=1;gravity=Gravity.CENTER
            val number=txt(value,23f,ink,true)
            if(title=="المتابعون") followingCountView=number
            if(title=="المُعجبين") followersCountView=number
            addView(number,LinearLayout.LayoutParams(-1,dp(34)))
            addView(txt(title,13f,muted),LinearLayout.LayoutParams(-1,dp(28)))
            isClickable=title!="الأصدقاء"
            if(isClickable)setOnClickListener{click()}
        }
        stats.addView(stat("0","الأصدقاء"),LinearLayout.LayoutParams(0,dp(70),1f))
        stats.addView(stat("0","المتابعون"){openFollowList("following")},LinearLayout.LayoutParams(0,dp(70),1f))
        stats.addView(stat("0","المُعجبين"){openFollowList("followers")},LinearLayout.LayoutParams(0,dp(70),1f))
        content.addView(stats)

        val shortcuts=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;setPadding(dp(12),dp(10),dp(12),dp(18))}
        fun shortcut(icon:Int,title:String,bg:Int,fg:Int,click:()->Unit){
            val card=LinearLayout(this).apply{
                orientation=1;gravity=Gravity.CENTER
                background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(54,37,82),Color.rgb(35,25,57))).apply{cornerRadius=dp(17).toFloat();setStroke(dp(1),Color.rgb(94,72,125))}
                setOnClickListener{click()}
                val iconView=ImageView(this@ProfileActivity).apply{
                    setImageResource(icon)
                    setColorFilter(fg,android.graphics.PorterDuff.Mode.SRC_IN)
                    scaleType=ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(16),dp(10),dp(16),dp(6))
                    contentDescription=title
                }
                addView(iconView,LinearLayout.LayoutParams(-1,dp(58)))
                val titleView=txt(title,12f,Color.rgb(235,218,177),true)
                if(title=="LV 1"){
                    levelShortcutTitle=titleView
                    levelShortcutIcon=iconView
                    levelShortcutCard=this
                }
                addView(titleView,LinearLayout.LayoutParams(-1,dp(25)))
            }
            shortcuts.addView(card,LinearLayout.LayoutParams(0,dp(106),1f).apply{marginStart=dp(5);marginEnd=dp(5)})
        }
        shortcut(R.drawable.ic_profile_wallet,"ربح النقود",Color.rgb(218,246,255),Color.rgb(232,194,117)){showInfo("ربح النقود")}
        shortcut(R.drawable.ic_profile_crown,"العائلة",Color.rgb(255,239,218),Color.rgb(232,194,117)){showInfo("العائلة - قريباً ستتمكن من إنشاء عائلتك وتنظيم أعضائها وإظهار اسم العائلة في ملفك")}
        shortcut(R.drawable.ic_profile_vip,"شراء VIP",Color.rgb(255,242,211),Color.rgb(232,194,117)){showInfo("شراء VIP")}
        shortcut(R.drawable.ic_profile_level,"LV 1",Color.rgb(213,250,249),Color.rgb(232,194,117)){startActivity(Intent(this,LevelActivity::class.java))}
        content.addView(shortcuts)

        val separator=View(this).apply{setBackgroundColor(Color.rgb(238,233,244))}
        fun menuRow(icon:Int,title:String,detail:String="",click:()->Unit){
            val row=LinearLayout(this).apply{
                orientation=0;gravity=Gravity.CENTER_VERTICAL
                setPadding(dp(18),dp(9),dp(18),dp(9));setBackgroundColor(Color.WHITE)
                setOnClickListener{click()}
            }
            row.addView(ImageView(this).apply{
                setImageResource(R.drawable.ic_profile_chevron)
                setColorFilter(Color.rgb(195,198,203),android.graphics.PorterDuff.Mode.SRC_IN)
                scaleType=ImageView.ScaleType.CENTER_INSIDE
            },LinearLayout.LayoutParams(dp(28),dp(52)))
            row.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
            if(detail.isNotBlank())row.addView(txt(detail,13f,muted),LinearLayout.LayoutParams(-2,dp(48)).apply{marginEnd=dp(10)})
            row.addView(txt(title,17f,ink),LinearLayout.LayoutParams(-2,dp(52)))
            val badge=FrameLayout(this).apply{
                val iconBackground=Color.rgb(244,239,249)
                val iconForeground=Color.rgb(111,75,151)
                background=rounded(iconBackground,100)
                addView(ImageView(this@ProfileActivity).apply{
                    setImageResource(icon)
                    setColorFilter(iconForeground,android.graphics.PorterDuff.Mode.SRC_IN)
                    scaleType=ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(8),dp(8),dp(8),dp(8))
                },FrameLayout.LayoutParams(dp(38),dp(38),Gravity.CENTER))
            }
            row.addView(badge,LinearLayout.LayoutParams(dp(42),dp(48)).apply{marginStart=dp(12)})
            content.addView(row,LinearLayout.LayoutParams(-1,dp(66)))
        }
        fun sectionGap(){content.addView(View(this).apply{setBackgroundColor(Color.rgb(238,233,244))},LinearLayout.LayoutParams(-1,dp(9)))}
        content.addView(separator,LinearLayout.LayoutParams(-1,dp(8)))
        menuRow(R.drawable.ic_profile_creator,"مركز صناع المحتوى"){showInfo("مركز صناع المحتوى")}
        menuRow(R.drawable.ic_profile_events,"مركز الفعاليات"){showInfo("مركز الفعاليات")}
        sectionGap()
        menuRow(R.drawable.ic_profile_wallet,"المحفظة"){showInfo("المحفظة")}
        menuRow(R.drawable.ic_profile_bag,"الحقيبة","13"){startActivity(Intent(this,InventoryActivity::class.java))}
        menuRow(R.drawable.ic_profile_publish,"أنشر"){showInfo("أنشر")}
        sectionGap()
        menuRow(R.drawable.ic_profile_svip,"SVIP"){showInfo("SVIP")}
        menuRow(R.drawable.ic_profile_tasks,"مركز المهام"){showInfo("مركز المهام")}
        sectionGap()

        // Keep the existing profile fields available to the profile loader and editor.
        displayName=EditText(this)
        bio=EditText(this)
        country=EditText(this)
        status=txt("",12f,Color.rgb(180,65,75))
        levelCard=LinearLayout(this).apply{orientation=1}
        levelNumber=txt("LV 1",14f,ink,true)
        levelRank=txt("المبتدئ",12f,muted)
        levelXp=txt("0 XP",10f,muted)
        levelProgress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=1000}
        frameToggle=txt("",12f,ink)
        content.addView(levelCard,LinearLayout.LayoutParams(-1,dp(1)))
        content.addView(status,LinearLayout.LayoutParams(-1,dp(32)))

        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)

        loadProfile(TextView(this).also{it.tag="profile_id"; idViewRef=it})
        loadLevelProgress()
        loadFollowStats()
        loadRoyalFrameState()
    }


    private fun loadFollowStats(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{
            try{
                val result=AuthApi.get("/api/follows/me",token)
                if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر تحميل المتابعة"))
                val following=result.body.optInt("following_count",result.body.optJSONArray("following")?.length()?:0)
                val followers=result.body.optInt("followers_count",result.body.optJSONArray("followers")?.length()?:0)
                runOnUiThread{
                    if(!isFinishing && ::followingCountView.isInitialized){
                        followingCountView.text=following.toString()
                        followersCountView.text=followers.toString()
                    }
                }
            }catch(e:Exception){android.util.Log.e("BAKFollow","Load follow counts failed",e)}
        }
    }

    private fun openFollowList(type:String){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        val title=if(type=="followers")"المعجبون" else "أتابعهم"
        val loading=android.app.AlertDialog.Builder(this).setTitle(title).setMessage("جارٍ تحميل القائمة...").setNegativeButton("إغلاق",null).create()
        loading.show()
        executor.execute{
            try{
                val result=AuthApi.get("/api/follows/me",token)
                if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر تحميل القائمة"))
                val list=result.body.optJSONArray(type)?:org.json.JSONArray()
                val names=ArrayList<String>()
                for(i in 0 until list.length()){
                    val item=list.optJSONObject(i)?:continue
                    val name=item.optString("display_name").takeIf{it.isNotBlank() && it!="null"}
                        ?:item.optString("username","مستخدم")
                    names.add(name)
                }
                runOnUiThread{
                    if(isFinishing){loading.dismiss();return@runOnUiThread}
                    loading.dismiss()
                    val message=if(names.isEmpty())"لا توجد حسابات في هذه القائمة بعد" else names.joinToString("\\n")
                    android.app.AlertDialog.Builder(this).setTitle("$title (${names.size})").setMessage(message).setPositiveButton("حسناً",null).show()
                }
            }catch(e:Exception){
                runOnUiThread{loading.dismiss();Toast.makeText(this,e.message?:"تعذر تحميل القائمة",Toast.LENGTH_SHORT).show()}
            }
        }
    }

    private fun showInfo(title:String){
        android.app.AlertDialog.Builder(this).setTitle(title).setMessage("هذه الخدمة ستتوفر ضمن تطبيق BAK LIVE.").setPositiveButton("حسناً",null).show()
    }

    private fun showEditProfile(){
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(18),dp(18),dp(12))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.rgb(248,250,249));cornerRadius=dp(22).toFloat()
                setStroke(dp(1),Color.rgb(226,235,230))
            }
        }
        val photoRow=LinearLayout(this).apply{
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(12),dp(12),dp(12),dp(12))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.WHITE);cornerRadius=dp(18).toFloat()
                setStroke(dp(1),Color.rgb(230,235,232))
            }
        }
        val photo=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP
            setImageDrawable(avatarPreview.drawable)
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(230,235,232));setStroke(dp(2),Color.rgb(45,160,119))
            }
            clipToOutline=true
        }
        editPhotoPreview=photo
        photoRow.addView(photo,LinearLayout.LayoutParams(dp(76),dp(76)))
        val photoInfo=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)}
        photoInfo.addView(TextView(this).apply{text="الصورة الشخصية";textSize=15f;setTextColor(Color.rgb(45,55,51));setTypeface(null,android.graphics.Typeface.BOLD)})
        photoInfo.addView(TextView(this).apply{text="اختر صورة واضحة لحسابك";textSize=12f;setTextColor(Color.rgb(135,145,140))})
        photoRow.addView(photoInfo,LinearLayout.LayoutParams(0,-2,1f))
        val changePhoto=TextView(this).apply{
            text="تغيير";textSize=13f;gravity=Gravity.CENTER
            setTextColor(Color.WHITE);setPadding(dp(13),dp(9),dp(13),dp(9))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.rgb(38,155,112));cornerRadius=dp(14).toFloat()
            }
            setOnClickListener{pickPhoto()}
        }
        photoRow.addView(changePhoto)
        card.addView(photoRow,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
        fun input(hint:String,value:String)=field(hint).apply{
            setText(value);textSize=14f
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.WHITE);cornerRadius=dp(14).toFloat()
                setStroke(dp(1),Color.rgb(225,231,228))
            }
        }
        card.addView(caption("اسم العرض"))
        val name=input("اكتب اسمك",displayName.text.toString())
        card.addView(name,LinearLayout.LayoutParams(-1,dp(50)))
        card.addView(caption("نبذة عنك"))
        val about=input("اكتب نبذة قصيرة",bio.text.toString())
        card.addView(about,LinearLayout.LayoutParams(-1,dp(50)))
        card.addView(caption("الدولة"))
        val nation=input("الدولة",country.text.toString())
        card.addView(nation,LinearLayout.LayoutParams(-1,dp(50)))
        val scroll=ScrollView(this).apply{isFillViewport=true;setPadding(dp(4),dp(4),dp(4),dp(4));addView(card)}
        val dialog=android.app.AlertDialog.Builder(this)
            .setTitle("تعديل الملف الشخصي")
            .setView(scroll)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ",null)
            .create()
        dialog.setOnShowListener{
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(38,155,112))
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(130,140,135))
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                displayName.setText(name.text.toString())
                bio.setText(about.text.toString())
                country.setText(nation.text.toString())
                saveProfile()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onResume(){
        super.onResume()
        if(::frameToggle.isInitialized) loadRoyalFrameState()
        if(::followingCountView.isInitialized) loadFollowStats()
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
                    if(::levelShortcutTitle.isInitialized)levelShortcutTitle.text="LV $level"
                    levelRank.text=rank
                    levelProgress.progress=(progress*1000).toInt()
                    levelXp.text=if(level>=100)"MAX LEVEL  •  $total XP" else "$current / $next XP  •  المجموع $total"
                }
            }catch(_:Exception){
                runOnUiThread{
                    if(!isFinishing){
                        levelNumber.text="LV 1"
                        if(::levelShortcutTitle.isInitialized)levelShortcutTitle.text="LV 1"
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
        if(::levelShortcutCard.isInitialized){
            levelShortcutCard.background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(colors[0],colors[1],colors[2])
            ).apply{
                cornerRadius=dp(15).toFloat()
                setStroke(dp(2),colors[3])
            }
            levelShortcutIcon.setColorFilter(colors[4],android.graphics.PorterDuff.Mode.SRC_IN)
            levelShortcutTitle.setTextColor(colors[5])
        }
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
                    if(bitmap!=null){avatarPreview.setImageBitmap(bitmap);editPhotoPreview?.setImageBitmap(bitmap)}
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
                    publicIdLabel.text=if(publicId.isNotBlank())"ID: $publicId" else "ID: غير متوفر"
                    displayName.setText(user.optString("display_name").takeUnless{it=="null"}?:user.optString("username"))
                    profileTitle.text=displayName.text.toString().ifBlank{"مستخدم BAK LIVE"}
                    bio.setText(user.optString("bio").takeUnless{it=="null"}?:"")
                    country.setText(user.optString("country").takeUnless{it=="null"}?:"")
                    avatarUrl=when{
                        url.isBlank()->""
                        url.startsWith("http://") || url.startsWith("https://")->url
                        else->"https://nokiraq.duckdns.org/"+url.trimStart('/')
                    }
                    status.text=""
                    if(avatarUrl.isNotBlank())loadAvatarPreview(avatarUrl)
                    if(intent.getBooleanExtra("open_edit_profile",false))showEditProfile()
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

class RoyalGifFrame(context:android.content.Context):View(context){
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