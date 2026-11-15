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
import android.util.Log
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
    private lateinit var profileHeader:ProfileHeaderView
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

        val ink=Color.rgb(38,43,52)
        val muted=Color.rgb(139,145,157)
        val accent=Color.rgb(40,190,145)
        val page=Color.rgb(246,247,250)
        val white=Color.WHITE
        window.statusBarColor=Color.rgb(226,249,244)
        window.navigationBarColor=white
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        fun shape(color:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=android.graphics.drawable.GradientDrawable().apply{
            setColor(color);cornerRadius=dp(radius).toFloat()
            if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)
        }
        fun gradient(colors:IntArray,radius:Int,stroke:Int=Color.TRANSPARENT)=android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,colors
        ).apply{
            cornerRadius=dp(radius).toFloat()
            if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)
        }

        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(page)}
        val scroll=ScrollView(this).apply{
            isFillViewport=true;clipToPadding=false;overScrollMode=View.OVER_SCROLL_NEVER
        }
        val content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(page)
            clipChildren=false;clipToPadding=false
        }
        scroll.addView(content)

        // Dedicated XML-backed profile header. This replaces the previous hand-built header
        // and stats completely; no legacy white stats strip or decorative avatar frame is attached.
        profileHeader=ProfileHeaderView(this)
        profileTitle=profileHeader.nameView
        publicIdLabel=profileHeader.userIdView
        idViewRef=profileHeader.userIdView
        avatarPreview=profileHeader.avatarView
        followingCountView=profileHeader.followingCountView
        followersCountView=profileHeader.fansCountView
        avatarRing=View(this)
        royalFrame=RoyalGifFrame(this).apply{visibility=View.GONE}

        profileHeader.avatarView.setOnClickListener{showEditProfile()}
        profileHeader.editAvatarView.setOnClickListener{showEditProfile()}
        profileHeader.copyView.setOnClickListener{
            val value=profileHeader.userIdView.text?.toString().orEmpty()
            if(value.isNotBlank() && value!="غير متوفر" && value!="..."){
                val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("BAK ID",value))
                Toast.makeText(this,"تم نسخ ID",Toast.LENGTH_SHORT).show()
            }
        }
        profileHeader.levelView.setOnClickListener{
            try {
                startActivity(Intent(this,LevelActivity::class.java))
            } catch (error: Throwable) {
                Log.e("BAKWealthLevel", "Unable to launch LevelActivity", error)
                Toast.makeText(this, "تعذر فتح صفحة مستوى الثراء", Toast.LENGTH_LONG).show()
            }
        }
        profileHeader.settingsView.setOnClickListener{
            startActivity(Intent(this,AccountSecurityActivity::class.java))
        }
        profileHeader.supportView.setOnClickListener{
            android.app.AlertDialog.Builder(this).setTitle("المساعدة والدعم")
                .setMessage("نحن هنا لمساعدتك في استخدام BAK LIVE.")
                .setPositiveButton("حسناً",null).show()
        }
        profileHeader.fansCell.setOnClickListener{openFollowList("followers")}
        profileHeader.followingCell.setOnClickListener{openFollowList("following")}
        content.addView(profileHeader,LinearLayout.LayoutParams(-1,-2))

        // Wallet balances are read from the existing API, not hard-coded.
        val balances=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14),0,dp(14),dp(10))
        }
        fun balanceCard(title:String,initial:String,emoji:String):Pair<LinearLayout,TextView>{
            val card=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                background=shape(white,17,Color.rgb(239,241,245))
                setPadding(dp(10),dp(8),dp(10),dp(8))
                isClickable=true
                setOnClickListener{startActivity(Intent(this@ProfileActivity,WalletActivity::class.java))}
            }
            val row=LinearLayout(this).apply{gravity=Gravity.CENTER}
            val amount=TextView(this@ProfileActivity).apply{
                text=initial;textSize=20f;setTextColor(ink);typeface=android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(amount)
            row.addView(TextView(this@ProfileActivity).apply{
                text=emoji;textSize=24f;setPadding(dp(6),0,0,0)
            })
            card.addView(row,LinearLayout.LayoutParams(-1,dp(36)))
            card.addView(TextView(this@ProfileActivity).apply{
                text=title;textSize=11f;setTextColor(muted);gravity=Gravity.CENTER
            })
            return Pair(card,amount)
        }
        val (coinsCard,coinsValue)=balanceCard("عملات الشحن","—","🪙")
        val (diamondsCard,diamondsValue)=balanceCard("الألماس المكتسب","—","💎")
        balances.addView(coinsCard,LinearLayout.LayoutParams(0,dp(78),1f).apply{marginEnd=dp(5)})
        balances.addView(diamondsCard,LinearLayout.LayoutParams(0,dp(78),1f).apply{marginStart=dp(5)})
        content.addView(balances,LinearLayout.LayoutParams(-1,-2))

        // Coordinated colorful shortcuts inspired by the reference profile.
        // Events, tasks and cash-earning shortcuts remain intentionally absent.
        val shortcuts=GridLayout(this).apply{
            columnCount=4;useDefaultMargins=false
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            background=shape(white,18);setPadding(dp(5),dp(10),dp(5),dp(10))
        }
        fun shortcut(iconFile:String,title:String,click:()->Unit){
            val cell=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                layoutDirection=View.LAYOUT_DIRECTION_LTR
                isClickable=true;isFocusable=true;setOnClickListener{click()}
                setPadding(dp(2),dp(5),dp(2),dp(5))
            }
            val iconBox=FrameLayout(this@ProfileActivity).apply{
                val tileColor=when(title){
                    "مظهري"->Color.rgb(220,247,250)
                    "المتجر"->Color.rgb(255,230,244)
                    "VIP"->Color.rgb(255,244,216)
                    "العائلة"->Color.rgb(239,231,255)
                    "مركز الوكالة"->Color.rgb(224,248,243)
                    "مركز المضيف"->Color.rgb(232,239,255)
                    else->Color.rgb(232,239,255)
                }
                background=shape(tileColor,15)
                val iconView=ImageView(this@ProfileActivity).apply{
                    scaleType=ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(3),dp(3),dp(3),dp(3))
                    contentDescription=title
                    // Decode the original 2048px PNG off the UI thread at display size.
                    // This keeps the source PNG untouched while avoiding large bitmaps and scroll jank.
                }
                addView(iconView,FrameLayout.LayoutParams(dp(58),dp(58),Gravity.CENTER))
                executor.execute{
                    try{
                        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                        assets.open("profile-icons/$iconFile").use{BitmapFactory.decodeStream(it,null,bounds)}
                        val targetPx=dp(180)
                        var sample=1
                        while(bounds.outWidth/(sample*2)>=targetPx && bounds.outHeight/(sample*2)>=targetPx)sample*=2
                        val options=BitmapFactory.Options().apply{inSampleSize=sample.coerceAtLeast(1);inPreferredConfig=Bitmap.Config.ARGB_8888}
                        val bitmap=assets.open("profile-icons/$iconFile").use{BitmapFactory.decodeStream(it,null,options)}
                        if(bitmap!=null){
                            runOnUiThread{
                                if(!isFinishing && !isDestroyed)iconView.setImageBitmap(bitmap) else bitmap.recycle()
                            }
                        }
                    }catch(e:Exception){
                        android.util.Log.w("ProfileIcons","Could not load $iconFile",e)
                    }
                }
            }
            cell.addView(iconBox,LinearLayout.LayoutParams(dp(58),dp(58)))
            cell.addView(TextView(this@ProfileActivity).apply{
                text=title;textSize=10.5f;setTextColor(ink);gravity=Gravity.CENTER
                typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
                maxLines=2
            },LinearLayout.LayoutParams(-1,dp(36)))
            val params=GridLayout.LayoutParams().apply{
                width=0;height=dp(98)
                columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                setMargins(dp(1),dp(2),dp(1),dp(2))
            }
            shortcuts.addView(cell,params)
        }
        // Six approved profile destinations. Noble and invitation center removed.
        shortcut("1-appearance.png","مظهري"){
            startActivity(Intent(this,InventoryActivity::class.java))
        }
        shortcut("2-store.png","المتجر"){
            startActivity(Intent(this,InventoryActivity::class.java))
        }
        shortcut("3-vip.png","VIP"){
            showInfo("عضوية VIP - سيتم تفعيل الباقات والمزايا بعد ربط نظام الاشتراكات")
        }
        shortcut("4-family.png","العائلة"){
            showInfo("العائلة - قريباً ستتمكن من إنشاء عائلتك وتنظيم أعضائها")
        }
        shortcut("5-host-center.png","مركز المضيف"){
            startActivity(Intent(this,CreatorCenterActivity::class.java))
        }
        shortcut("6-agency-center.png","مركز الوكالة"){
            showInfo("مركز الوكالة - سيتم ربط إدارة الوكالات ودعوات المضيفين عند تجهيز الخدمة")
        }
        content.addView(shortcuts,LinearLayout.LayoutParams(-1,-2).apply{
            setMargins(dp(14),dp(2),dp(14),dp(14))
        })

        // The large Emperor/XP level panel is intentionally removed from the profile.
        // Level remains visible beside the user ID in ProfileHeaderView.

        val menuHeading=TextView(this).apply{
            text="إعدادات وخدمات الحساب";textSize=13f;setTextColor(ink)
            typeface=android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(18),dp(5),dp(18),dp(7))
        }
        content.addView(menuHeading,LinearLayout.LayoutParams(-1,dp(34)))
        val menu=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            background=shape(white,17,Color.rgb(238,240,244))
            setPadding(dp(8),dp(4),dp(8),dp(4))
        }
        fun menuRow(icon:Int,title:String,detail:String,click:()->Unit){
            val row=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                setPadding(dp(10),dp(5),dp(10),dp(5))
                isClickable=true;setOnClickListener{click()}
            }
            val iconBox=FrameLayout(this@ProfileActivity).apply{
                background=shape(Color.rgb(240,247,245),11)
                addView(ImageView(this@ProfileActivity).apply{
                    setImageResource(icon);setColorFilter(Color.rgb(76,155,133),android.graphics.PorterDuff.Mode.SRC_IN)
                    scaleType=ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(8),dp(8),dp(8),dp(8))
                },FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER))
            }
            row.addView(iconBox,LinearLayout.LayoutParams(dp(40),dp(42)))
            row.addView(TextView(this@ProfileActivity).apply{
                text=title;textSize=13f;setTextColor(ink);setPadding(dp(10),0,0,0)
            },LinearLayout.LayoutParams(0,-2,1f))
            row.addView(TextView(this@ProfileActivity).apply{
                text="‹";textSize=22f;setTextColor(Color.rgb(174,181,190));gravity=Gravity.CENTER
            },LinearLayout.LayoutParams(dp(24),dp(42)))
            menu.addView(row,LinearLayout.LayoutParams(-1,dp(52)))
            if(title!="الأمان والخصوصية")menu.addView(View(this@ProfileActivity).apply{
                setBackgroundColor(Color.rgb(242,243,246))
            },LinearLayout.LayoutParams(-1,dp(1)))
        }
        menuRow(R.drawable.ic_profile_publish,"أنشر","شارك لحظاتك مع المجتمع"){showInfo("أنشر")}
        menuRow(R.drawable.ic_profile_help,"المساعدة والدعم","نحن هنا لمساعدتك"){
            android.app.AlertDialog.Builder(this).setTitle("المساعدة والدعم")
                .setMessage("نحن هنا لمساعدتك في استخدام BAK LIVE.")
                .setPositiveButton("حسناً",null).show()
        }
        menuRow(R.drawable.ic_profile_security,"الأمان والخصوصية","إعدادات حماية حسابك"){
            startActivity(Intent(this,AccountSecurityActivity::class.java))
        }
        content.addView(menu,LinearLayout.LayoutParams(-1,-2).apply{
            setMargins(dp(14),0,dp(14),dp(22))
        })

        displayName=EditText(this)
        bio=EditText(this)
        country=EditText(this)
        status=TextView(this).apply{
            text="";textSize=11f;setTextColor(Color.rgb(180,65,75))
            gravity=Gravity.CENTER;setPadding(dp(12),dp(5),dp(12),dp(5))
        }
        frameToggle=TextView(this).apply{text="";isEnabled=false}
        levelShortcutTitle=TextView(this)
        levelShortcutIcon=ImageView(this)
        levelShortcutCard=LinearLayout(this)
        frameToggle.isEnabled=false
        frameToggle.alpha=0.8f
        content.addView(status,LinearLayout.LayoutParams(-1,dp(30)))

        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)

        loadProfile(profileHeader.userIdView)
        loadLevelProgress()
        loadFollowStats()
        executor.execute{
            try{
                val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return@execute
                val response=AuthApi.get("/api/wallet",token)
                if(response.status !in 200..299)throw Exception("تعذر تحميل المحفظة")
                val wallet=response.body.optJSONObject("wallet")?:response.body
                val coins=wallet.optLong("recharge_coins",0L)
                val diamonds=wallet.optLong("earned_diamonds",0L)
                runOnUiThread{
                    if(!isFinishing){
                        coinsValue.text=String.format(java.util.Locale.US,"%,d",coins)
                        diamondsValue.text=String.format(java.util.Locale.US,"%,d",diamonds)
                    }
                }
            }catch(e:Exception){android.util.Log.e("BAKProfile","Could not load wallet preview",e)}
        }
        // Keep profile header lightweight: do not fetch or animate inventory frames here.
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
        // Profile screen does not render equipped decorative frames.
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
            avatarRing.visibility=View.VISIBLE
            royalFrame.visibility=View.GONE
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
                runOnUiThread{
                    if(!isFinishing && !isDestroyed)profileHeader.setLevel(level)
                }
            }catch(_:Exception){
                runOnUiThread{
                }
            }
        }
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
                    profileHeader.setUserIdText(publicId)
                    displayName.setText(user.optString("display_name").takeUnless{it=="null"}?:user.optString("username"))
                    profileHeader.setUserName(displayName.text.toString())
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
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
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