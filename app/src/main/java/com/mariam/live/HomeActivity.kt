package com.mariam.live
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Shader
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.Executors

class HomeActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(4)
    private val ink = Color.rgb(35,45,39)
    private val muted = Color.rgb(150,128,90)
    private val gold = Color.rgb(224,152,24)
    private val goldDark = Color.rgb(150,88,10)
    private lateinit var grid: GridLayout
    private lateinit var roomStatus: TextView
    private lateinit var tabsRow: LinearLayout
    private var messagesBadge: TextView? = null
    private var tabMode = 0    // 0 شائع | 1 استكشف | 2 المميزات
    private var chipMode = 0   // 0 الكل | 1 الأكثر مشاهدة | 2 الأكثر تفاعلاً
    private var searchQuery = ""
    private var pendingRoomName = ""
    private val roomImagePicker = 7314
    private val tabsData = listOf(0 to "شائع", 2 to "المميزات", 1 to "استكشف")
    private data class RoomItem(val id:String,val name:String,val count:Int,val max:Int,val imageUrl:String?,val roomMode:String="audio")
    private val liveRooms= mutableListOf<RoomItem>()
    private var roomsLoadInFlight = false
    private var roomsReloadPending = false
    private var lastCardsSignature = ""
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{
        text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER_VERTICAL
        if(b)typeface=Typeface.DEFAULT_BOLD
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        // طلب إذن إشعارات Android 13+ بعد دخول المستخدم إلى الصفحة الرئيسية.
        PushTokenManager.requestNotificationPermission(this)
        val pageBg=Color.rgb(255,253,247)
        window.statusBarColor=Color.rgb(255,240,200)
        window.navigationBarColor=Color.rgb(247,210,125)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root=FrameLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(pageBg)}
        val column=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        root.addView(column,FrameLayout.LayoutParams(-1,-1))

        // ───────────── الهيدر: العنوان يمين + بحث/غرفتي يسار ─────────────
        val header=FrameLayout(this).apply{
            layoutDirection=View.LAYOUT_DIRECTION_LTR
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(255,240,200),Color.rgb(255,250,235)))
        }
        val appTitle=TextView(this).apply{
            text="BAK LIVE";textSize=22f;gravity=Gravity.CENTER
            typeface=Typeface.create("sans-serif",Typeface.BOLD_ITALIC)
            setTextColor(Color.rgb(196,128,16))
            setShadowLayer(dp(2).toFloat(),0f,dp(1).toFloat(),0x88FFFFFF.toInt())
        }
        header.addView(appTitle,FrameLayout.LayoutParams(-2,-1,Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply{rightMargin=dp(16)})
        appTitle.translationY=dp(18).toFloat()
        val goldLine=View(this).apply{
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x00E0A030,0xFFE0A030.toInt(),0x00E0A030))
        }
        header.addView(goldLine,FrameLayout.LayoutParams(-1,dp(2),Gravity.BOTTOM))
        fun headerIcon(kind:String,description:String)=LineIcon(this,kind,gold).apply{
            setPadding(dp(11),dp(11),dp(11),dp(11))
            background=shape(0x66FFFFFF,23)
            contentDescription=description
            isClickable=true
        }
        val searchButton=headerIcon("search","بحث عن غرفة").apply{setOnClickListener{showSearchDialog()}}
        header.addView(searchButton,FrameLayout.LayoutParams(dp(46),dp(46),Gravity.LEFT or Gravity.CENTER_VERTICAL).apply{leftMargin=dp(12)})
        searchButton.translationY=dp(18).toFloat()
        val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
        val returnRoomButton=headerIcon("mic","العودة إلى غرفتك").apply{
            tag="return_room_button"
            visibility=if(prefs.getString("active_room_id",null).isNullOrBlank())View.GONE else View.VISIBLE
            setOnClickListener{
                val id=prefs.getString("active_room_id",null).orEmpty()
                if(id.isNotBlank())startActivity(Intent(this@HomeActivity,RoomActivity::class.java).putExtra("room_id",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                else visibility=View.GONE
            }
        }
        header.addView(returnRoomButton,FrameLayout.LayoutParams(dp(46),dp(46),Gravity.LEFT or Gravity.CENTER_VERTICAL).apply{leftMargin=dp(64)})
        returnRoomButton.translationY=dp(18).toFloat()
        column.addView(header,LinearLayout.LayoutParams(-1,dp(104)))

        // ───────────── التبويبات ─────────────
        tabsRow=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(pageBg)
        }
        column.addView(tabsRow,LinearLayout.LayoutParams(-1,dp(46)))
        buildTabs()

        // ───────────── المحتوى القابل للتمرير ─────────────
        val content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12),dp(6),dp(12),dp(110));setBackgroundColor(pageBg)
        }
        val scroll=ScrollView(this).apply{
            isVerticalScrollBarEnabled=false;isFillViewport=false;clipToPadding=false;addView(content)
        }
        column.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))


        grid=GridLayout(this).apply{columnCount=2;useDefaultMargins=false;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        content.addView(grid,LinearLayout.LayoutParams(-1,-2))
        roomStatus=TextView(this).apply{text="جارٍ تحميل الغرف...";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER}
        content.addView(roomStatus,LinearLayout.LayoutParams(-1,dp(42)))

        // ───────────── القائمة السفلية ─────────────
        val navWrap=FrameLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_RTL;clipChildren=false}
        val bar=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(255,247,224),Color.rgb(247,210,125))).apply{                cornerRadii=floatArrayOf(dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),0f,0f,0f,0f)
                setStroke(dp(1),0x88E6B450.toInt())
            }
            setPadding(dp(6),dp(4),dp(6),0)
            elevation=dp(8).toFloat()
        }
        fun navItem(kind:String,title:String,selected:Boolean,onTap:()->Unit)=FrameLayout(this).apply{
            isClickable=true;isFocusable=true;contentDescription=title
            val col=LinearLayout(this@HomeActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
            col.addView(NavIcon(this@HomeActivity,kind,selected),LinearLayout.LayoutParams(dp(30),dp(30)))
            col.addView(TextView(this@HomeActivity).apply{
                text=title;textSize=10.5f;gravity=Gravity.CENTER;setSingleLine(true)
                setTextColor(if(selected)goldDark else Color.rgb(160,124,66))
                typeface=if(selected)Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium",Typeface.NORMAL)
            },LinearLayout.LayoutParams(-1,dp(16)))
            addView(col,FrameLayout.LayoutParams(-1,-1))
            setOnClickListener{onTap()}
        }
        // الترتيب (من اليمين): الرئيسية - اكتشف - [زر الإنشاء] - رسائلي - حسابي
        bar.addView(navItem("home","الرئيسية",true){
        },LinearLayout.LayoutParams(0,-1,1f))
        bar.addView(navItem("discover","اكتشف",false){
            searchQuery="";tabMode=1;buildTabs();loadRooms()
            Toast.makeText(this@HomeActivity,"تحديث الغرف المباشرة",Toast.LENGTH_SHORT).show()
        },LinearLayout.LayoutParams(0,-1,1f))
        bar.addView(View(this),LinearLayout.LayoutParams(0,-1,1.25f)) // مساحة الزر المركزي
        val messagesItem=navItem("messages","رسائلي",false){
            startActivity(Intent(this@HomeActivity,MessagesActivity::class.java))
        }
        val badge=TextView(this).apply{
            textSize=9f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD
            background=shape(Color.rgb(255,64,96),9)
            setPadding(dp(5),0,dp(5),0);minWidth=dp(18);visibility=View.GONE
        }
        messagesBadge=badge
        messagesItem.addView(badge,FrameLayout.LayoutParams(-2,dp(18),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{leftMargin=dp(26);topMargin=dp(2)})
        bar.addView(messagesItem,LinearLayout.LayoutParams(0,-1,1f))
        bar.addView(navItem("profile","حسابي",false){
            startActivity(Intent(this@HomeActivity,ProfileActivity::class.java))
        },LinearLayout.LayoutParams(0,-1,1f))
        navWrap.addView(bar,FrameLayout.LayoutParams(-1,dp(62),Gravity.BOTTOM))

        // الزر المركزي البارز (إنشاء غرفة)
        val createSlot=FrameLayout(this).apply{elevation=dp(12).toFloat()}
        val halo=View(this).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(0x66FFF1C4)}}
        createSlot.addView(halo,FrameLayout.LayoutParams(dp(76),dp(76),Gravity.CENTER))
        val create=CreateRoomButton(this).apply{
            contentDescription="إنشاء غرفة"
            isClickable=true
            elevation=dp(8).toFloat()
            outlineProvider=object:android.view.ViewOutlineProvider(){
                override fun getOutline(v:View,outline:android.graphics.Outline){outline.setOval(0,0,v.width,v.height)}
            }
            setOnClickListener{showCreateRoomDialog()}
        }
        createSlot.addView(create,FrameLayout.LayoutParams(dp(62),dp(62),Gravity.CENTER))
        ObjectAnimator.ofFloat(create,View.SCALE_X,1f,1.055f).apply{duration=1100;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE;start()}
        ObjectAnimator.ofFloat(create,View.SCALE_Y,1f,1.055f).apply{duration=1100;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE;start()}
        ObjectAnimator.ofFloat(halo,View.ALPHA,0.5f,1f).apply{duration=1100;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE;start()}
        navWrap.addView(createSlot,FrameLayout.LayoutParams(dp(76),dp(76),Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        root.addView(navWrap,FrameLayout.LayoutParams(-1,dp(86),Gravity.BOTTOM))
        ViewCompat.setOnApplyWindowInsetsListener(navWrap){ view,insets->
            val b=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val wrapLp=view.layoutParams
            if(wrapLp.height!=dp(86)+b){wrapLp.height=dp(86)+b;view.layoutParams=wrapLp}
            val barLp=bar.layoutParams
            if(barLp.height!=dp(62)+b){barLp.height=dp(62)+b;bar.layoutParams=barLp}
            bar.setPadding(dp(6),dp(4),dp(6),b)
            content.setPadding(dp(12),dp(6),dp(12),dp(110)+b)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(navWrap)
        handlePushIntent(intent)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        handlePushIntent(newIntent)
    }

    private fun handlePushIntent(pushIntent: Intent?) {
        if (pushIntent?.getBooleanExtra("from_push", false) != true) return
        val screen = pushIntent.getStringExtra("screen").orEmpty().lowercase()
        val type = pushIntent.getStringExtra("type").orEmpty().lowercase()
        val conversationId = (pushIntent.getStringExtra("conversation_id")
            ?: pushIntent.getStringExtra("conversationId")
            ?: pushIntent.getStringExtra("chat_id")).orEmpty()
        val roomId = (pushIntent.getStringExtra("room_id")
            ?: pushIntent.getStringExtra("live_room_id")
            ?: pushIntent.getStringExtra("roomId")
            ?: pushIntent.getStringExtra("broadcast_id")).orEmpty()
        val userId = (pushIntent.getStringExtra("user_id")
            ?: pushIntent.getStringExtra("sender_id")
            ?: pushIntent.getStringExtra("from_user_id")).orEmpty()
        val isMessage = screen == "chat" || screen == "message" ||
            type.contains("message") || type.contains("chat")
        val isLive = screen.contains("live") || type.contains("live") ||
            type.contains("broadcast") || type.contains("stream")
        val destination = when {
            isMessage ->
                Intent(this, DirectChatActivity::class.java)
                    .putExtra("conversation_id", conversationId)
                    .putExtra("other_user_id", userId)
                    .putExtra("other_name", pushIntent.getStringExtra("sender_name").orEmpty())
            (isLive || screen.contains("room") || screen.contains("invite") ||
                type == "gift" || type.contains("room") || type.contains("invite")) && roomId.isNotBlank() ->
                Intent(this, RoomActivity::class.java).putExtra("room_id", roomId)
            (screen == "profile" || type == "follow") && userId.isNotBlank() ->
                Intent(this, ProfileActivity::class.java).putExtra("profile_user_id", userId)
            screen == "agency" || type.contains("agency") ->
                Intent(this, AgencyActivity::class.java)
            else -> null
        }
        // Clear the push marker before navigating so returning to Home cannot reopen it.
        pushIntent.removeExtra("from_push")
        if (destination != null) {
            destination.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(destination)
        }
    }

    override fun onResume(){
        super.onResume()
        // Retry FCM registration when returning to the app after sign-in/network recovery.
        PushTokenManager.sync(this)
        verifyAccount()
        val activeId=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("active_room_id",null)
        window.decorView.findViewWithTag<View>("return_room_button")?.visibility=if(activeId.isNullOrBlank())View.GONE else View.VISIBLE
        if(::grid.isInitialized && ::roomStatus.isInitialized)loadRooms()
    }

    /** اربطها بعدد الرسائل غير المقروءة (0 = إخفاء الشارة). */
    fun setMessagesBadge(count:Int){
        messagesBadge?.apply{
            if(count<=0){visibility=View.GONE}
            else{text=if(count>99)"99+" else count.toString();visibility=View.VISIBLE}
        }
    }

    private fun buildTabs(){
        tabsRow.removeAllViews()
        tabsData.forEach{(mode,title)->
            val selected=mode==tabMode
            val col=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                isClickable=true;isFocusable=true
                setOnClickListener{
                    tabMode=mode;buildTabs();buildCards()
                }
            }
            col.addView(TextView(this).apply{
                text=title;textSize=if(selected)17f else 15f;gravity=Gravity.CENTER
                setTextColor(if(selected)gold else muted)
                typeface=if(selected)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            },LinearLayout.LayoutParams(-2,-2))
            col.addView(View(this).apply{
                background=shape(gold,2)
                visibility=if(selected)View.VISIBLE else View.INVISIBLE
            },LinearLayout.LayoutParams(dp(22),dp(3)).apply{topMargin=dp(3)})
            tabsRow.addView(col,LinearLayout.LayoutParams(0,-1,1f))
        }
    }

    private fun buildCards(){
        val avail=resources.displayMetrics.widthPixels-dp(24)
        val cardW=avail/2-dp(8)
        val cardH=(cardW*1.12f).toInt()
        var list:List<RoomItem> = liveRooms.filter{it.name.contains(searchQuery,true)}
        list=when(tabMode){
            0->list.sortedByDescending{it.count}
            2->list.filter{!it.imageUrl.isNullOrBlank()}.sortedByDescending{it.count}
            else->list
        }
        list=when(chipMode){
            1->list.sortedByDescending{it.count}
            2->list.sortedByDescending{if(it.max>0)it.count.toFloat()/it.max else 0f}
            else->list
        }
        val cardsSignature = "$tabMode|$chipMode|$searchQuery|" + list.joinToString(";") { "${it.id}:${it.name}:${it.count}:${it.max}:${it.imageUrl.orEmpty()}" }
        if (cardsSignature == lastCardsSignature) return
        lastCardsSignature = cardsSignature
        grid.removeAllViews()
        if(list.isEmpty()){
            roomStatus.visibility=View.VISIBLE
            roomStatus.text=if(searchQuery.isNotBlank())"لا توجد غرف تطابق البحث" else "لا توجد غرف نشطة حالياً. أنشئ أول غرفة!"
            return
        }
        roomStatus.visibility=View.GONE
        list.forEach{room->
            val frame=FrameLayout(this).apply{
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.rgb(255,243,208),Color.rgb(246,216,140))).apply{cornerRadius=dp(14).toFloat()}
                clipToOutline=true
                isClickable=true
                setOnClickListener{joinRoom(room.id,room.name)}
            }
            // أيقونة بديلة إذا لا توجد صورة
            frame.addView(LineIcon(this,"mic",Color.argb(140,210,150,40)),FrameLayout.LayoutParams(dp(52),dp(52),Gravity.CENTER))
            val cover=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
            frame.addView(cover,FrameLayout.LayoutParams(-1,-1))
            if(!room.imageUrl.isNullOrBlank()){cover.tag=room.imageUrl;loadRoomCover(cover,room.imageUrl)}
            // تدرجات الظل أعلى وأسفل البطاقة
            val topShade=View(this).apply{
                background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x66000000,0x00000000))
            }
            frame.addView(topShade,FrameLayout.LayoutParams(-1,dp(46),Gravity.TOP))
            val bottomShade=View(this).apply{
                background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x00000000,0xB3000000.toInt()))
            }
            frame.addView(bottomShade,FrameLayout.LayoutParams(-1,dp(74),Gravity.BOTTOM))
            // عدد المتواجدين (يسار) + إشارة البث (يمين)
            frame.addView(RoomCountView(this,room.count),
                FrameLayout.LayoutParams(dp(84),dp(24),Gravity.TOP or Gravity.LEFT).apply{setMargins(dp(10),dp(8),0,0)})
            // نوع البث مع مؤشر صوتي أزرق بجانبه داخل الشارة نفسها.
            val modeLabel=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL
                gravity=Gravity.CENTER
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                setPadding(dp(7),dp(3),dp(7),dp(3))
                background=GradientDrawable().apply{
                    setColor(Color.argb(175,18,16,24))
                    cornerRadius=dp(10).toFloat()
                    setStroke(dp(1),Color.argb(65,255,255,255))
                }
                elevation=dp(2).toFloat()
                addView(label(
                    if(room.roomMode=="camera" || room.roomMode=="camera6" || room.roomMode=="video") "غرفة الفيديو" else "غرفة صوتية",
                    10.5f,Color.WHITE,true
                ).apply{gravity=Gravity.CENTER;includeFontPadding=false},
                    LinearLayout.LayoutParams(-2,dp(17)))
                addView(SignalBarsView(this@HomeActivity),
                    LinearLayout.LayoutParams(dp(18),dp(15)).apply{marginStart=dp(5)})
            }
            frame.addView(modeLabel,FrameLayout.LayoutParams(-2,dp(25),Gravity.TOP or Gravity.RIGHT).apply{
                setMargins(0,dp(8),dp(9),0)
            })
            // اسم الغرفة يبقى ظاهراً أسفل الصورة.
            frame.addView(label(room.name,14f,Color.WHITE,true).apply{
                maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
                gravity=Gravity.CENTER_VERTICAL or Gravity.START
                setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),0xCC000000.toInt())
                setPadding(dp(10),0,dp(10),dp(10))
            },FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))
            grid.addView(frame,GridLayout.LayoutParams().apply{
                width=cardW;height=cardH;setMargins(dp(4),dp(4),dp(4),dp(4))
            })
        }
    }

    private fun verifyAccount(){
        val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
        val token=prefs.getString("token",null)?:return
        executor.execute{
            try{
                val r=AuthApi.get("/api/auth/me",token)
                if(r.status==401 || r.status==403){
                    val banned=r.body.optBoolean("banned") ||
                        r.body.optString("code")=="ACCOUNT_BANNED"
                    if(banned){
                        runOnUiThread{
                            if(isFinishing || isDestroyed)return@runOnUiThread
                            startActivity(Intent(this,AccountBannedActivity::class.java).apply{
                                putExtra("reason",r.body.optString("reason"))
                                putExtra("ban_type",r.body.optString("ban_type"))
                                putExtra("started_at",r.body.optString("created_at"))
                                putExtra("expires_at",r.body.optString("expires_at"))
                                flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finishAffinity()
                        }
                    }else{
                        prefs.edit().clear().apply()
                        runOnUiThread{if(!isFinishing && !isDestroyed)openLogin()}
                    }
                }
            }catch(_:Exception){}
        }
    }
    private fun loadRooms(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){roomStatus.text="سجّل الدخول لعرض الغرف";return}
        if (roomsLoadInFlight) { roomsReloadPending = true; return }
        roomsLoadInFlight = true
        roomStatus.text="جارٍ تحميل الغرف الحقيقية..."
        executor.execute{
            var parsed = mutableListOf<RoomItem>()
            var errorMessage: String? = null
            try{
                val result=AuthApi.get("/api/voice-rooms",token)
                val array=result.body.optJSONArray("rooms")
                if(result.status in 200..299 && array!=null){
                    for(i in 0 until array.length()){
                        val item=array.optJSONObject(i)?:continue
                        parsed.add(RoomItem(
                            item.optString("id"),
                            item.optString("name","غرفة صوتية"),
                            item.optInt("member_count",0),
                            item.optInt("max_members",8),
                            roomImageUrl(item),
                            item.optString("room_mode","audio").lowercase()
                        ))
                    }
                } else errorMessage=result.body.optString("error","تعذر تحميل الغرف")
            }catch(_:Exception){ errorMessage="تعذر الاتصال بالخادم" }
            runOnUiThread{
                if (!isFinishing && !isDestroyed) {
                    if (errorMessage == null) {
                        liveRooms.clear();liveRooms.addAll(parsed);buildCards()
                    } else if (liveRooms.isEmpty()) {
                        roomStatus.text=errorMessage
                    }
                }
                roomsLoadInFlight = false
                if (roomsReloadPending && !isFinishing && !isDestroyed) {
                    roomsReloadPending = false
                    loadRooms()
                }
            }        }
    }
    private fun roomImageUrl(item:JSONObject):String?{
        val image=item.optJSONObject("image")
        val raw=sequenceOf(            item.optString("image_url"),
            item.optString("imageUrl"),
            item.optString("cover_url"),
            item.optString("coverUrl"),
            image?.optString("url").orEmpty()
        ).firstOrNull{it.isNotBlank()&&it!="null"} ?: return null
        return when{
            raw.startsWith("https://",true)->raw
            raw.startsWith("http://",true)->raw
            raw.startsWith("/")->"https://nokiraq.duckdns.org$raw"
            else->"https://nokiraq.duckdns.org/uploads/rooms/$raw"
        }
    }
    private fun loadRoomCover(view:ImageView,imageUrl:String){
        executor.execute{
            var connection:HttpURLConnection?=null
            try{
                connection=(URL(imageUrl).openConnection() as HttpURLConnection).apply{
                    connectTimeout=10000
                    readTimeout=12000
                    instanceFollowRedirects=true
                    setRequestProperty("Accept","image/avif,image/webp,image/png,image/jpeg,*/*")
                    setRequestProperty("User-Agent","MariamLive-Android")
                    connect()
                }
                if(connection.responseCode !in 200..299)throw Exception("HTTP ${connection.responseCode}")
                val bytes=connection.inputStream.use{it.readBytes()}
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                val maxDimension=720
                var sample=1
                while(bounds.outWidth/sample>maxDimension || bounds.outHeight/sample>maxDimension) sample*=2
                val options=BitmapFactory.Options().apply{
                    inSampleSize=sample
                    inPreferredConfig=Bitmap.Config.RGB_565
                }
                val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
                    ?: throw Exception("Invalid room image")
                runOnUiThread{
                    if(!isFinishing && !isDestroyed && view.tag==imageUrl) view.setImageBitmap(bitmap)
                    else bitmap.recycle()
                }
            }catch(e:Exception){
                android.util.Log.w("MariamLive","Room cover load failed: $imageUrl",e)
            }finally{connection?.disconnect()}
        }
    }
    private fun joinRoom(id:String,name:String){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{
            try{
                val result=AuthApi.post("/api/voice-rooms/$id/join",JSONObject(),token)
                runOnUiThread{
                    if(result.status in 200..299){
                        startActivity(Intent(this,RoomActivity::class.java).putExtra("room_id",id).putExtra("room_name",name))
                    }else Toast.makeText(this,result.body.optString("error","تعذر دخول الغرفة"),Toast.LENGTH_LONG).show()
                }
            }catch(_:Exception){runOnUiThread{Toast.makeText(this,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}}
        }
    }
    private fun showSearchDialog(){
        val input=EditText(this).apply{hint="ابحث عن اسم غرفة";setSingleLine(true);setText(searchQuery)}
        android.app.AlertDialog.Builder(this).setTitle("بحث في الغرف الحقيقية").setView(input)
            .setNegativeButton("مسح"){_,_->searchQuery="";buildCards()}
            .setPositiveButton("بحث"){_,_->searchQuery=input.text.toString().trim();buildCards()}.show()
    }
    private fun showCreateRoomDialog(){
        startActivity(Intent(this,RoomSetupActivity::class.java))
    }
    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==roomImagePicker && resultCode==RESULT_OK){
            val uri=data?.data ?: return
            try {
                val flags=data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch(_:Exception) {}
            startActivity(Intent(this,RoomSetupActivity::class.java)
                .putExtra("room_name",pendingRoomName)
                .putExtra("image_uri",uri.toString()))
        }
    }
    private fun createRoomWithImage(name:String,uri:Uri){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        Toast.makeText(this,"جارٍ تجهيز الصورة وإنشاء الغرفة...",Toast.LENGTH_SHORT).show()
        executor.execute{
            try{
                val source=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)} ?: throw Exception("تعذر قراءة الصورة")
                val maxSide=1280
                val scale=minOf(1f,maxSide.toFloat()/maxOf(source.width,source.height))
                val resized=if(scale<1f)Bitmap.createScaledBitmap(source,(source.width*scale).toInt().coerceAtLeast(1),(source.height*scale).toInt().coerceAtLeast(1),true)else source
                val bytes=ByteArrayOutputStream()
                var quality=85
                do{bytes.reset();resized.compress(Bitmap.CompressFormat.JPEG,quality,bytes);quality-=10}while(bytes.size()>3*1024*1024 && quality>=45)
                if(bytes.size()>3*1024*1024)throw Exception("الصورة كبيرة جداً، اختر صورة أصغر")
                val payload=JSONObject().put("name",name).put("max_members",8)
                    .put("image_mime","image/jpeg").put("image_base64",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP))
                if(resized!==source)resized.recycle()
                if(!source.isRecycled)source.recycle()
                val result=AuthApi.post("/api/voice-rooms",payload,token)
                runOnUiThread{
                    if(result.status in 200..299){
                        Toast.makeText(this,"تم إنشاء الغرفة وحفظ صورتها",Toast.LENGTH_LONG).show()
                        loadRooms()
                    }else{
                        Toast.makeText(this,result.body.optString("error","تعذر إنشاء الغرفة"),Toast.LENGTH_LONG).show()
                    }
                }
            }catch(e:Exception){runOnUiThread{Toast.makeText(this,e.message?:"تعذر تجهيز الصورة",Toast.LENGTH_LONG).show()}}
        }
    }

    private fun openLogin(){startActivity(Intent(this,LoginActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}

    // ═══════════════════════════ Custom Views ═══════════════════════════

    /** أيقونات خطية للهيدر: search / mic */
    private class LineIcon(context:android.content.Context,private val kind:String,tint:Int):View(context){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            style=Paint.Style.STROKE;strokeWidth=2.1f
            strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;color=tint
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val w=width-paddingLeft-paddingRight
            val h=height-paddingTop-paddingBottom
            val side=minOf(w,h).toFloat()
            if(side<=0f)return
            canvas.save()
            canvas.translate(paddingLeft+(w-side)/2f,paddingTop+(h-side)/2f)
            canvas.scale(side/24f,side/24f)
            when(kind){
                "search"->{
                    canvas.drawCircle(10.5f,10.5f,6.5f,p)
                    canvas.drawLine(15.4f,15.4f,21f,21f,p)
                }
                "mic"->{
                    canvas.drawRoundRect(RectF(8.8f,2.5f,15.2f,13.5f),3.2f,3.2f,p)
                    canvas.drawArc(RectF(5.3f,7.5f,18.7f,19.5f),0f,180f,false,p)
                    canvas.drawLine(12f,19.5f,12f,22f,p)
                    canvas.drawLine(8.5f,22f,15.5f,22f,p)
                }
                else -> Unit
            }
            canvas.restore()
        }
    }

    /** أيقونات القائمة السفلية الذهبية: home / discover / messages / profile */
    internal class NavIcon(context:android.content.Context,private val kind:String,private val selected:Boolean):View(context){        private val fill=Paint(Paint.ANTI_ALIAS_FLAG)
        private val line=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=1.4f;strokeJoin=Paint.Join.ROUND;strokeCap=Paint.Cap.ROUND}
        private val detail=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val side=minOf(width,height).toFloat()
            if(side<=0f)return
            canvas.save()
            canvas.translate((width-side)/2f,(height-side)/2f)
            canvas.scale(side/24f,side/24f)
            val top=if(selected)Color.rgb(255,218,110) else Color.rgb(255,247,226)
            val bottom=if(selected)Color.rgb(226,142,26) else Color.rgb(236,208,150)
            fill.shader=LinearGradient(0f,2f,0f,22f,top,bottom,Shader.TileMode.CLAMP)
            line.color=if(selected)Color.rgb(150,88,10) else Color.rgb(176,138,76)
            detail.color=if(selected)Color.rgb(255,252,238) else Color.rgb(176,138,76)
            if(selected){
                val glow=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                    shader=RadialGradient(12f,12f,12f,0x55FFD36E,0x00FFD36E,Shader.TileMode.CLAMP)
                }
                canvas.drawCircle(12f,12f,12f,glow)
            }
            when(kind){
                "home"->{
                    val house=Path().apply{
                        moveTo(12f,2.8f);lineTo(21.8f,11f);lineTo(19.5f,11f);lineTo(19.5f,20f)
                        quadTo(19.5f,21.2f,18.3f,21.2f);lineTo(5.7f,21.2f)
                        quadTo(4.5f,21.2f,4.5f,20f);lineTo(4.5f,11f);lineTo(2.2f,11f);close()
                    }
                    canvas.drawPath(house,fill);canvas.drawPath(house,line)
                    canvas.drawRoundRect(RectF(9.8f,13.5f,14.2f,21.2f),1.2f,1.2f,detail)
                }
                "discover"->{
                    canvas.drawCircle(12f,12f,9.6f,fill);canvas.drawCircle(12f,12f,9.6f,line)
                    val needle=Path().apply{moveTo(16.2f,7.8f);lineTo(13.4f,13.4f);lineTo(7.8f,16.2f);lineTo(10.6f,10.6f);close()}
                    canvas.drawPath(needle,detail)
                    val dot=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=line.color}
                    canvas.drawCircle(12f,12f,1.2f,dot)
                }
                "messages"->{
                    val tail=Path().apply{moveTo(7.5f,16.5f);lineTo(6.5f,21.3f);lineTo(12.5f,16.5f);close()}
                    canvas.drawPath(tail,fill);canvas.drawPath(tail,line)
                    val body=RectF(2.8f,3.6f,21.2f,17f)
                    canvas.drawRoundRect(body,5f,5f,fill);canvas.drawRoundRect(body,5f,5f,line)
                    for(x in floatArrayOf(8f,12f,16f))canvas.drawCircle(x,10.3f,1.2f,detail)
                }
                "profile"->{
                    val torso=Path().apply{
                        moveTo(4.2f,21f);cubicTo(4.2f,15f,7.5f,13.2f,12f,13.2f)
                        cubicTo(16.5f,13.2f,19.8f,15f,19.8f,21f);close()
                    }
                    canvas.drawPath(torso,fill);canvas.drawPath(torso,line)
                    canvas.drawCircle(12f,7.6f,4.3f,fill);canvas.drawCircle(12f,7.6f,4.3f,line)
                }
            }
            canvas.restore()
        }
    }

    /** الزر المركزي الذهبي البارز: ميكروفون + علامة (+) */
    internal class CreateRoomButton(context:android.content.Context):View(context){
        private val d=resources.displayMetrics.density
        private val ring=Paint(Paint.ANTI_ALIAS_FLAG)
        private val inner=Paint(Paint.ANTI_ALIAS_FLAG)
        private val rim=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;color=Color.WHITE;strokeWidth=2f*d}
        private val iconFill=Paint(Paint.ANTI_ALIAS_FLAG)
        private val iconLine=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND}
        private fun drawMic(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){
            iconFill.color=color;iconLine.color=color;iconLine.strokeWidth=r*0.11f
            c.drawRoundRect(RectF(cx-r*0.17f,cy-r*0.52f,cx+r*0.17f,cy+r*0.10f),r*0.17f,r*0.17f,iconFill)
            c.drawArc(RectF(cx-r*0.34f,cy-r*0.30f,cx+r*0.34f,cy+r*0.38f),0f,180f,false,iconLine)
            c.drawLine(cx,cy+r*0.38f,cx,cy+r*0.54f,iconLine)
            c.drawLine(cx-r*0.18f,cy+r*0.54f,cx+r*0.18f,cy+r*0.54f,iconLine)
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val cx=width/2f;val cy=height/2f;val r=minOf(width,height)/2f-1f*d
            ring.shader=LinearGradient(cx-r,cy-r,cx+r,cy+r,Color.rgb(255,240,180),Color.rgb(214,142,30),Shader.TileMode.CLAMP)
            canvas.drawCircle(cx,cy,r,ring)
            inner.shader=LinearGradient(cx,cy-r,cx,cy+r,Color.rgb(255,214,110),Color.rgb(232,150,26),Shader.TileMode.CLAMP)
            canvas.drawCircle(cx,cy,r*0.82f,inner)
            canvas.drawCircle(cx,cy,r*0.82f,rim)
            drawMic(canvas,cx-r*0.04f,cy+r*0.02f+1.2f*d,r,Color.rgb(160,96,10))   // ظل
            drawMic(canvas,cx-r*0.04f,cy+r*0.02f,r,Color.WHITE)
            // علامة +
            val bx=cx+r*0.40f;val by=cy+r*0.40f;val br=r*0.24f
            iconFill.color=Color.WHITE;canvas.drawCircle(bx,by,br,iconFill)
            iconLine.color=Color.rgb(214,142,30);iconLine.strokeWidth=r*0.09f
            canvas.drawLine(bx-br*0.5f,by,bx+br*0.5f,by,iconLine)
            canvas.drawLine(bx,by-br*0.5f,bx,by+br*0.5f,iconLine)
        }
    }

    /** عدد المتواجدين + أيقونة شخص (أعلى يسار البطاقة) */
    private class RoomCountView(context:android.content.Context,private val memberCount:Int):View(context){
        private val d=resources.displayMetrics.density
        private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=Color.WHITE;textSize=13f*d;typeface=Typeface.create("sans-serif",Typeface.BOLD)
            setShadowLayer(2f*d,0f,1f*d,0x99000000.toInt())
        }
        private val ic=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=Color.WHITE;style=Paint.Style.STROKE;strokeWidth=1.5f*d;strokeCap=Paint.Cap.ROUND
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val cy=height/2f
            val label=memberCount.toString()
            canvas.drawText(label,0f,cy-text.ascent()/2f-text.descent()/2f,text)
            val x=text.measureText(label)+5f*d
            canvas.drawCircle(x+5f*d,cy-3f*d,2.6f*d,ic)
            canvas.drawArc(RectF(x,cy+1f*d,x+10f*d,cy+11f*d),180f,180f,false,ic)
        }
    }

    /** أعمدة الإشارة المتحركة (أعلى يمين البطاقة) */
    private class SignalBarsView(context:android.content.Context):View(context){
        private val d=resources.displayMetrics.density
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=Color.rgb(35,205,226);strokeWidth=3f*d;strokeCap=Paint.Cap.ROUND
        }
        private val started=android.os.SystemClock.uptimeMillis()
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val phase=(android.os.SystemClock.uptimeMillis()-started)/180.0
            val bottom=height-1.5f*d
            for(i in 0..2){
                val wave=(kotlin.math.sin(phase+i*1.3)+1.0)/2.0
                val h=((0.35+0.65*wave)*(height-3f*d)).toFloat()
                val x=(2.5f+i*6f)*d
                canvas.drawLine(x,bottom,x,bottom-h,paint)
            }
            postInvalidateDelayed(90)
        }
    }

}