package com.mariam.live

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.PorterDuff
import android.widget.ImageView
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.view.Gravity
import android.view.MotionEvent
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.Track
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import kotlinx.coroutines.*
import io.livekit.android.events.collect

class RoomActivity : Activity() {
 private lateinit var room: Room
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 private var connected=false; private var leaving=false; private var microphoneEnabled=true; private var isSeated=false
 private var handRaised=false; private var isOwner=false; private var ownerSeatSetupDone=false
 private var roomId=""; private var authToken=""; private var currentUserId=""
 private var seats=JSONArray(); private var hands=JSONArray()
 private val bg=Color.rgb(241,243,238); private val purple=Color.rgb(83,126,94); private val gold=Color.rgb(83,126,94); private val mutedText=Color.rgb(103,111,101)
 private lateinit var status:TextView; private lateinit var grid:LinearLayout
 private lateinit var roomPage:LinearLayout
 private lateinit var roomBackgroundView:ImageView
 private lateinit var roomBackgroundFillView:ImageView
 private lateinit var ownerAvatarView:ImageView
 private lateinit var ownerNameView:TextView
 private lateinit var ownerIdView:TextView
 private var ownerProfileRequested=false
 private var roomOwnerUserId=""
 private var followRequestSent=false
 private var ownerFollowButton:TextView?=null
 private var loadedBackgroundUrl=""
 private var swipeDownX=0f; private var swipeDownY=0f; private var switchingRoom=false
 private var seatRefreshJob:Job?=null
 private var speakerEventsJob:Job?=null
 private val activeSpeakerLevels=HashMap<String,Float>()
 private val speakerWaveViews=HashMap<String,CircularAudioWaveView>()
 private val avatarBitmaps=HashMap<String,Bitmap>()
 private val avatarLoading=HashSet<String>()
 private lateinit var mic:LinearLayout
 private lateinit var micControl:ImageView
 private lateinit var musicButton:LinearLayout
 private var musicMixer:RoomMusicMixer?=null
 private lateinit var chatPanel:LinearLayout
 private lateinit var roomComposer:LinearLayout
 private var chatPoll:Job?=null
 private lateinit var chatMessages:LinearLayout
 private lateinit var chatScroll:ScrollView
 private lateinit var chatInput:EditText
 private lateinit var chatIcon:ImageView
 private lateinit var sendButton:TextView
 private var composerExpanded=false
 private lateinit var membersButton:LinearLayout
 private var roomMembers=JSONArray()
 private var previousMemberIds:MutableSet<String>?=null
 private val joinNotices=ArrayList<String>()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER;typeface=android.graphics.Typeface.create("sans-serif",if(b)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)}
 private fun panel(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),Color.rgb(91,72,145))}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);window.statusBarColor=Color.rgb(30,22,48);window.navigationBarColor=Color.rgb(18,15,28);window.setSoftInputMode(if(Build.VERSION.SDK_INT>=30) android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);window.decorView.systemUiVisibility=0
  roomId=intent.getStringExtra("room_id").orEmpty()
  authToken=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null).orEmpty()
  roomPage=LinearLayout(this).apply{orientation=1;setPadding(dp(10),dp(12),dp(10),dp(7));setBackgroundColor(Color.TRANSPARENT)}
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(View(this),LinearLayout.LayoutParams(0,dp(44),1f))
  top.addView(label("×",30f,Color.rgb(83,126,94),true).apply{
   setOnClickListener{leaveRoom()}
   background=GradientDrawable().apply{
    shape=GradientDrawable.OVAL
    setColor(Color.rgb(247,247,242))
    setStroke(dp(1),Color.rgb(215,220,209))
   }
   contentDescription="مغادرة البث"
  },LinearLayout.LayoutParams(dp(36),dp(36)).apply{topMargin=dp(7)})
  roomPage.addView(top,LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(5)})
  roomPage.addView(buildPremiumRoomHeader(),LinearLayout.LayoutParams(-1,dp(40)).apply{
   topMargin=dp(1);bottomMargin=dp(1)
  })
  membersButton=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(8),dp(3),dp(8),dp(3))
   background=GradientDrawable().apply{
    setColor(Color.argb(48,255,255,255))
    cornerRadius=dp(15).toFloat()
    setStroke(dp(1),Color.argb(55,220,194,255))
   }
   addView(label("0",13f,Color.WHITE,true).apply{
    gravity=Gravity.CENTER
    includeFontPadding=false
   },LinearLayout.LayoutParams(-2,dp(22)).apply{marginEnd=dp(5)})
   addView(ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("user",Color.rgb(220,194,255),dp(18)))
    contentDescription="عدد الموجودين"
    scaleType=ImageView.ScaleType.CENTER
   },LinearLayout.LayoutParams(dp(20),dp(22)))
   setOnClickListener{showRoomMembers()}
  }
  roomPage.addView(membersButton,LinearLayout.LayoutParams(-1,dp(28)).apply{gravity=Gravity.START;topMargin=dp(2);bottomMargin=dp(2)})
  grid=LinearLayout(this).apply{orientation=1}
  roomPage.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20)})
  status=label("",11f,Color.rgb(90,105,95)).apply{visibility=View.GONE}
  mic=action("المايك","mic",Color.rgb(83,126,94),Color.rgb(145,174,139)){toggleMic()}
  mic.visibility=View.GONE
  musicButton=action("موسيقى","music",Color.rgb(91,130,94),Color.rgb(157,178,145)){showMusicOptions()}
  musicButton.visibility=View.GONE
  roomPage.addView(buildInlineChat(),LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(4)})
  val roomRoot=FrameLayout(this).apply{setBackgroundColor(Color.rgb(20,18,28))}
  // Single sharp edge-to-edge image. CENTER_CROP preserves proportions on all screens.
  roomBackgroundView=ImageView(this).apply{
   scaleType=ImageView.ScaleType.CENTER_CROP
   setBackgroundColor(Color.rgb(18,16,25))
   contentDescription="خلفية الغرفة"
   adjustViewBounds=false
  }
  roomBackgroundFillView=ImageView(this).apply{
   visibility=View.GONE
   setImageDrawable(null)
   setBackgroundColor(Color.TRANSPARENT)
  }
  roomRoot.addView(roomBackgroundView,FrameLayout.LayoutParams(-1,-1))
  roomRoot.addView(View(this).apply{setBackgroundColor(Color.argb(42,0,0,0))},FrameLayout.LayoutParams(-1,-1))
  roomRoot.addView(roomPage,FrameLayout.LayoutParams(-1,-1))
  setContentView(roomRoot)
  // Android 11+: move the composer by the exact keyboard inset instead of
  // relying on resize while immersive navigation flags are active.
  if(Build.VERSION.SDK_INT>=30){
   roomRoot.setOnApplyWindowInsetsListener{_,insets->
    val imeVisible=insets.isVisible(android.view.WindowInsets.Type.ime())
    val imeBottom=insets.getInsets(android.view.WindowInsets.Type.ime()).bottom
    roomComposer.translationY= -imeBottom.toFloat()
    if(imeVisible) window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE
    else hidePhoneNavigation()
    insets
   }
   roomRoot.requestApplyInsets()
  }
  emptySeats()
  if(roomId.isBlank()||authToken.isBlank()){status.text="بيانات الدخول أو الغرفة غير متوفرة";return}
  room=LiveKit.create(applicationContext);loadIdentity()
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)connect() else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),4102)
 }

 override fun onResume(){
  super.onResume()
  hidePhoneNavigation()
 }

 override fun onPause(){
  window.decorView.systemUiVisibility =
   View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  super.onPause()
 }

 private fun hidePhoneNavigation(){
  window.decorView.systemUiVisibility =
   View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
   View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
   View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
   View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
 }

 private fun switchLiveRoom(direction:Int){
  if(switchingRoom || leaving || roomId.isBlank())return
  switchingRoom=true
  Toast.makeText(this,if(direction>0)"جارٍ الانتقال إلى البث التالي..." else "جارٍ الرجوع إلى البث السابق...",Toast.LENGTH_SHORT).show()
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms",authToken)}
    if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحميل الغرف"))
    val array=response.body.optJSONArray("rooms")?:JSONArray()
    val active=ArrayList<Pair<String,String>>()
    for(i in 0 until array.length()){
     val item=array.optJSONObject(i)?:continue
     val id=item.optString("id")
     val name=item.optString("name","غرفة صوتية")
     val count=item.optInt("member_count",0)
     if(id.isNotBlank())active.add(id to name)
    }
    val currentIndex=active.indexOfFirst{it.first==roomId}
    // The current room may be omitted by the API in some configurations; use the
    // returned active-room order and choose the first/last available destination.
    val targetIndex=if(currentIndex<0){if(direction>0)0 else active.lastIndex}
      else currentIndex+direction
    if(targetIndex !in active.indices){
     Toast.makeText(this@RoomActivity,"لا توجد غرف أخرى بهذا الاتجاه",Toast.LENGTH_SHORT).show()
     switchingRoom=false
     return@launch
    }
    val target=active[targetIndex]
    val joined=withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/${target.first}/join",JSONObject(),authToken)
    }
    if(joined.status !in 200..299)throw Exception(joined.body.optString("error","تعذر دخول البث الجديد"))
    try{
     stopRoomMusic(restoreMic=false)
     if(connected)room.disconnect()
     connected=false
     withContext(Dispatchers.IO){
      AuthApi.post("/api/voice-rooms/$roomId/leave",JSONObject().put("keep_open",true),authToken)
     }
     stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
     val next=Intent(this@RoomActivity,RoomActivity::class.java)
      .putExtra("room_id",target.first)
      .putExtra("room_name",target.second)
     startActivity(next)
     finish()
    }catch(e:Exception){
     withContext(Dispatchers.IO){
      AuthApi.post("/api/voice-rooms/${target.first}/leave",JSONObject().put("keep_open",true),authToken)
     }
     throw e
    }
   }catch(e:Exception){
    switchingRoom=false
    Toast.makeText(this@RoomActivity,e.message?:"تعذر الانتقال بين الغرف",Toast.LENGTH_LONG).show()
   }
  }
 }

 override fun dispatchTouchEvent(event:MotionEvent):Boolean{
  val screenHeight=resources.displayMetrics.heightPixels.toFloat()
  when(event.actionMasked){
   MotionEvent.ACTION_DOWN->{
    swipeDownX=event.rawX
    swipeDownY=event.rawY
    val chatTop=if(::chatPanel.isInitialized){
     val location=IntArray(2);chatPanel.getLocationOnScreen(location);location[1].toFloat()
    }else screenHeight
    if(!switchingRoom && swipeDownY<chatTop){
     roomPage.animate().cancel()
     roomPage.setLayerType(View.LAYER_TYPE_HARDWARE,null)
     roomPage.translationY=0f
    }
   }
   MotionEvent.ACTION_MOVE->{
    val dx=event.rawX-swipeDownX
    val dy=event.rawY-swipeDownY
    if(!switchingRoom && swipeDownY<screenHeight-dp(170) &&
      kotlin.math.abs(dy)>dp(8) && kotlin.math.abs(dy)>kotlin.math.abs(dx)*1.2f){
     // Translate only: avoid scaling/fading the full room hierarchy on every frame.
     roomPage.translationY=dy*0.88f
     return true
    }
   }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
    val dx=event.rawX-swipeDownX
    val dy=event.rawY-swipeDownY
    val isVertical=kotlin.math.abs(dy)>kotlin.math.abs(dx)*1.2f
    if(isVertical && (roomPage.translationY!=0f || kotlin.math.abs(dy)>dp(12))){
     val commit=event.actionMasked==MotionEvent.ACTION_UP && kotlin.math.abs(dy)>dp(125)
     roomPage.animate().cancel()
     if(commit && !switchingRoom){
      val direction=if(dy<0)1 else -1
      roomPage.animate()
       .translationY(if(direction>0)-screenHeight else screenHeight)
       .setDuration(150)
       .setInterpolator(android.view.animation.LinearInterpolator())
       .withEndAction{
        roomPage.translationY=0f
        roomPage.setLayerType(View.LAYER_TYPE_NONE,null)
        switchLiveRoom(direction)
       }.start()
     }else{
      roomPage.animate().translationY(0f)
       .setDuration(170)
       .setInterpolator(android.view.animation.DecelerateInterpolator(1.4f))
       .withEndAction{roomPage.setLayerType(View.LAYER_TYPE_NONE,null)}
       .start()
     }
     return true
    }else{
     roomPage.setLayerType(View.LAYER_TYPE_NONE,null)
    }
   }
  }
  return super.dispatchTouchEvent(event)
 }

 private fun buildPremiumRoomHeader():View{
   val card=LinearLayout(this).apply{
    orientation=LinearLayout.HORIZONTAL
    layoutDirection=View.LAYOUT_DIRECTION_LTR
    gravity=Gravity.CENTER_VERTICAL
    setPadding(0,0,0,0)
    background=null
    elevation=0f
   }
   val avatarFrame=FrameLayout(this).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(65,255,255,255));setStroke(dp(1),Color.argb(90,255,255,255))}
   }
   ownerAvatarView=ImageView(this).apply{
    scaleType=ImageView.ScaleType.CENTER_CROP
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(70,255,255,255))}
    clipToOutline=true
    setImageDrawable(VectorIcon("user",Color.WHITE,dp(19)))
   }
   avatarFrame.addView(ownerAvatarView,FrameLayout.LayoutParams(dp(32),dp(32),Gravity.CENTER))
   val details=LinearLayout(this).apply{
    orientation=LinearLayout.VERTICAL
    gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT
    layoutDirection=View.LAYOUT_DIRECTION_RTL
   }
   ownerNameView=label("صاحب البث",13f,Color.WHITE,true).apply{
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    textDirection=View.TEXT_DIRECTION_RTL
    maxLines=1
    ellipsize=android.text.TextUtils.TruncateAt.END
   }
   details.addView(ownerNameView,LinearLayout.LayoutParams(-1,dp(17)))
   ownerIdView=label("ID: "+roomId,10f,Color.argb(225,255,255,255),false).apply{
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    textDirection=View.TEXT_DIRECTION_LTR
    maxLines=1
   }
   details.addView(ownerIdView,LinearLayout.LayoutParams(-1,dp(14)))
   val follow=TextView(this).apply{
    text="+";textSize=23f;gravity=Gravity.CENTER
    setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(170,142,92,210));setStroke(dp(1),Color.argb(100,255,255,255))}
    contentDescription="حالة متابعة صاحب البث"
    setOnClickListener{toggleOwnerFollow(this)}
   }
   ownerFollowButton=follow
   card.addView(details,LinearLayout.LayoutParams(0,-1,1f))
   card.addView(avatarFrame,LinearLayout.LayoutParams(dp(38),dp(38)).apply{marginStart=dp(7)})
   card.addView(follow,LinearLayout.LayoutParams(dp(34),dp(34)).apply{marginStart=dp(5)})
   return card
  }
  private fun renderOwnerFollowState(button:TextView,following:Boolean){
  followRequestSent=following
  button.text=if(following)"✓" else "+"
  button.contentDescription=if(following)"إلغاء متابعة صاحب البث" else "متابعة صاحب البث"
  button.background=GradientDrawable().apply{
   shape=GradientDrawable.OVAL
   setColor(if(following)Color.rgb(91,139,101) else Color.argb(170,142,92,210))
   setStroke(dp(1),Color.argb(100,255,255,255))
  }
  button.isEnabled=true
 }
 private fun loadOwnerFollowState(){
  val userId=roomOwnerUserId
  val button=ownerFollowButton?:return
  if(userId.isBlank() || userId=="null")return
  if(userId==currentUserId){button.visibility=View.GONE;return}
  button.visibility=View.VISIBLE
  button.isEnabled=false
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){AuthApi.get("/api/follows/status/$userId",authToken)}
    if(result.status in 200..299 && roomOwnerUserId==userId){
     renderOwnerFollowState(button,result.body.optBoolean("following",false))
    }else button.isEnabled=true
   }catch(_:Exception){button.isEnabled=true}
  }
 }
 private fun toggleOwnerFollow(button:TextView){
  if(roomOwnerUserId.isBlank() || roomOwnerUserId=="null" || roomOwnerUserId==currentUserId){
   Toast.makeText(this,"لا يمكنك متابعة نفسك",Toast.LENGTH_SHORT).show();return
  }
  button.isEnabled=false
  scope.launch{
   try{
    val wasFollowing=followRequestSent
    val result=withContext(Dispatchers.IO){
     if(wasFollowing)AuthApi.delete("/api/follows/$roomOwnerUserId",authToken)
     else AuthApi.post("/api/follows/$roomOwnerUserId",JSONObject(),authToken)
    }
    if(result.status in 200..299){
     renderOwnerFollowState(button,!wasFollowing)
     Toast.makeText(this@RoomActivity,if(wasFollowing)"تم إلغاء المتابعة" else "أصبحت تتابع صاحب الغرفة",Toast.LENGTH_SHORT).show()
    }else{
     button.isEnabled=true
     Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر تحديث المتابعة"),Toast.LENGTH_SHORT).show()
    }
   }catch(_:Exception){
    button.isEnabled=true
    Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()
   }
  }
 }

 private fun updatePremiumOwnerHeader(owner:JSONObject?){
  if(!::ownerNameView.isInitialized || owner==null)return
  // Keep the last known owner identity visible when the owner temporarily leaves seat 1.
  val ownerUserId=owner.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}
  if(ownerUserId==null)return
  roomOwnerUserId=ownerUserId
  loadOwnerFollowState()
  val name=owner.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}
   ?:owner.optString("username").takeIf{it.isNotBlank()&&it!="null"}
   ?: "صاحب الغرفة"
  ownerNameView.text=name
  val accountId=owner.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}
   ?:owner.optString("account_id").takeIf{it.isNotBlank()&&it!="null"}
  ownerIdView.text=if(accountId!=null) "ID: $accountId" else "ID: ..."
  if(ownerUserId==currentUserId && !ownerProfileRequested){
   ownerProfileRequested=true
   scope.launch{
    try{
     val response=withContext(Dispatchers.IO){AuthApi.get("/api/profile",authToken)}
     if(response.status in 200..299){
      val profile=response.body.optJSONObject("profile")?:JSONObject()
      val publicId=profile.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}
      if(publicId!=null)ownerIdView.text="ID: $publicId"
      val profileName=profile.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}
      if(profileName!=null)ownerNameView.text=profileName
      val profileAvatar=profile.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
      if(profileAvatar.isNotBlank()){
       ownerAvatarView.tag=profileAvatar
       loadAvatarInto(ownerAvatarView,profileAvatar)
      }
     }
    }catch(_:Exception){ownerIdView.text="ID غير متاح"}
   }
  }
  val avatarUrl=owner.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
  if(avatarUrl.isNotBlank()){
   ownerAvatarView.tag=avatarUrl
   loadAvatarInto(ownerAvatarView,avatarUrl)
  }else{
   ownerAvatarView.setImageDrawable(VectorIcon("user",Color.rgb(220,190,135),dp(28)))
  }
 }

 private fun action(title:String,icon:String,color:Int,accent:Int,click:()->Unit)=LinearLayout(this).apply{
  orientation=1;gravity=Gravity.CENTER;isClickable=true;isFocusable=true
  elevation=dp(5).toFloat()
  val base=Color.rgb(
   (Color.red(color)*0.82f+Color.red(accent)*0.18f).toInt(),
   (Color.green(color)*0.82f+Color.green(accent)*0.18f).toInt(),
   (Color.blue(color)*0.82f+Color.blue(accent)*0.18f).toInt())
  background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
   Color.rgb(
    (Color.red(base)*0.78f+255*0.22f).toInt(),
    (Color.green(base)*0.78f+255*0.22f).toInt(),
    (Color.blue(base)*0.78f+255*0.22f).toInt()),base)).apply{
    cornerRadius=dp(21).toFloat()
    setStroke(dp(1),Color.argb(150,Color.red(accent),Color.green(accent),Color.blue(accent)))
  }
  val iconWrap=FrameLayout(this@RoomActivity).apply{
   background=GradientDrawable().apply{
    shape=GradientDrawable.OVAL
    setColor(Color.argb(48,255,255,255))
    setStroke(dp(1),Color.argb(155,255,255,255))
  }
   val iv=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon(icon,Color.rgb(247,247,242),dp(20)))
    setColorFilter(Color.rgb(247,247,242),PorterDuff.Mode.SRC_IN)
    scaleType=ImageView.ScaleType.FIT_CENTER
   }
   addView(iv,FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER))
  }
  addView(iconWrap,LinearLayout.LayoutParams(dp(38),dp(38)).apply{bottomMargin=dp(4)})
  addView(label(title,11f,Color.rgb(247,247,242),true))
  setOnClickListener{click()}
 }

 private fun buildInlineChat()=LinearLayout(this).apply{
   orientation=1
   setPadding(0,dp(2),0,0)
   background=null
   chatPanel=this
   chatMessages=LinearLayout(this@RoomActivity).apply{orientation=1;gravity=Gravity.BOTTOM;setPadding(0,0,0,dp(5))}
   chatScroll=ScrollView(this@RoomActivity).apply{
    isFillViewport=true
    overScrollMode=View.OVER_SCROLL_NEVER
    addView(chatMessages,android.widget.FrameLayout.LayoutParams(-1,-2))
   }
   addView(chatScroll,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(2)})
   val composer=LinearLayout(this@RoomActivity).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
   roomComposer=composer
   micControl=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("mic",Color.WHITE,dp(17)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(105,140,91,190));setStroke(dp(1),Color.argb(75,255,255,255))}
    contentDescription="تشغيل أو إيقاف المايك"
    setOnClickListener{toggleMic();refreshMicControl()}
   }
   composer.addView(micControl,LinearLayout.LayoutParams(dp(32),dp(32)).apply{marginEnd=dp(5)})
   chatInput=EditText(this@RoomActivity).apply{
    hint="اكتب تعليقاً...";textSize=14f;maxLines=1;setSingleLine(true)
    imeOptions=EditorInfo.IME_ACTION_SEND
    inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    gravity=Gravity.CENTER_VERTICAL or Gravity.END
    setOnEditorActionListener{_,actionId,event->
     val enterPressed=event?.keyCode==android.view.KeyEvent.KEYCODE_ENTER && event.action==android.view.KeyEvent.ACTION_DOWN
     if(actionId==EditorInfo.IME_ACTION_SEND || enterPressed){sendRoomMessage();true}else false
    }
    setTextColor(Color.rgb(45,43,50));setHintTextColor(Color.rgb(135,132,142))
    setPadding(dp(14),0,dp(14),0)
    background=GradientDrawable().apply{
     setColor(Color.rgb(245,245,247))
     cornerRadius=dp(24).toFloat()
     setStroke(dp(1),Color.rgb(225,225,230))
    }
    minHeight=dp(46)
    includeFontPadding=false
   }
   chatInput.visibility=View.GONE
   chatIcon=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("chat",Color.WHITE,dp(19)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(135,139,91,190))}
    contentDescription="فتح كتابة تعليق"
    setOnClickListener{expandRoomComposer()}
   }
   composer.addView(chatIcon,LinearLayout.LayoutParams(dp(32),dp(32)).apply{marginEnd=dp(5)})
   composer.addView(chatInput,LinearLayout.LayoutParams(0,dp(48),1f).apply{marginEnd=dp(7)})
   sendButton=TextView(this@RoomActivity).apply{
    text="➤";textSize=15f;gravity=Gravity.CENTER
    typeface=android.graphics.Typeface.DEFAULT_BOLD
    setTextColor(Color.rgb(75,75,82));setPadding(dp(8),0,dp(8),0)
    background=GradientDrawable().apply{setColor(Color.rgb(225,225,230));cornerRadius=dp(20).toFloat()}
    visibility=View.GONE
    setOnClickListener{sendRoomMessage()}
   }
   composer.addView(sendButton,LinearLayout.LayoutParams(dp(32),dp(32)))
   val toolsButton=TextView(this@RoomActivity).apply{
    text="⋯";textSize=21f;gravity=Gravity.CENTER
    setTextColor(Color.WHITE)
    background=GradientDrawable().apply{
     setColor(Color.argb(45,255,255,255));cornerRadius=dp(13).toFloat()
     setStroke(dp(1),Color.argb(65,255,255,255))
    }
    contentDescription="إعدادات الغرفة"
    setOnClickListener{showRoomTools()}
   }
   composer.addView(toolsButton,LinearLayout.LayoutParams(dp(30),dp(30)).apply{marginStart=dp(4)})
   addView(composer,LinearLayout.LayoutParams(-1,dp(40)).apply{topMargin=dp(4);bottomMargin=dp(2)})
   loadRoomMessages()
   chatPoll?.cancel()
   chatPoll=scope.launch{
    while(isActive){delay(3000);loadRoomMessages()}
   }
  }

 private fun expandRoomComposer(){
  if(composerExpanded){
   chatInput.requestFocus()
   val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
   imm.showSoftInput(chatInput,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
   return
  }
  composerExpanded=true
  window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE
  window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  chatIcon.visibility=View.GONE
  chatInput.visibility=View.VISIBLE
  sendButton.visibility=View.VISIBLE
  chatInput.requestFocus()
  chatInput.post{
   val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
   imm.showSoftInput(chatInput,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
  }
 }
 private fun collapseRoomComposer(){
  composerExpanded=false
  chatInput.setText("")
  chatInput.clearFocus()
  chatInput.visibility=View.GONE
  sendButton.visibility=View.GONE
  chatIcon.visibility=View.VISIBLE
  val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
  imm.hideSoftInputFromWindow(chatInput.windowToken,0)
 }
 private fun loadRoomMessages(){
  if(!isActiveChat())return
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){
     AuthApi.get("/api/voice-rooms/$roomId/messages",authToken)
    }
    if(result.status !in 200..299){
     if(chatMessages.childCount==0){
      chatMessages.addView(label(result.body.optString("error","تعذر تحميل الرسائل"),13f,Color.rgb(100,116,104)))
     }
     return@launch
    }
    val messages=result.body.optJSONArray("messages")?:JSONArray()
    chatMessages.removeAllViews()
    for(i in 0 until messages.length()){
     val item=messages.optJSONObject(i)?:continue
     val sender=item.optString("display_name").ifBlank{item.optString("username","مستخدم")}
     val body=item.optString("message")
     val mine=item.optString("user_id")==currentUserId
     val bubble=LinearLayout(this@RoomActivity).apply{
      orientation=1
      setPadding(dp(11),dp(7),dp(11),dp(7))
      background=GradientDrawable().apply{
       setColor(if(mine)Color.argb(105,93,58,130) else Color.argb(88,24,20,35))
       cornerRadius=dp(15).toFloat()
       setStroke(dp(1),Color.argb(22,255,255,255))
      }
     }
     bubble.addView(TextView(this@RoomActivity).apply{
      text=sender
      textSize=10f
      setTextColor(Color.rgb(226,205,255))
      typeface=android.graphics.Typeface.DEFAULT_BOLD
     })
     bubble.addView(TextView(this@RoomActivity).apply{
      text=body
      textSize=12f
      setTextColor(Color.WHITE)
      setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL)
     })
     val line=LinearLayout(this@RoomActivity).apply{
      orientation=0
      gravity=if(mine)Gravity.END else Gravity.START
      setPadding(0,dp(3),0,dp(3))
      addView(bubble,LinearLayout.LayoutParams(-2,-2))
     }
     chatMessages.addView(line,LinearLayout.LayoutParams(-1,-2))
    }
    for(notice in joinNotices.takeLast(12)){
     val event=TextView(this@RoomActivity).apply{
      text=notice;textSize=12f;gravity=Gravity.CENTER
      setTextColor(Color.rgb(77,91,76));setPadding(dp(8),dp(7),dp(8),dp(7))
      background=GradientDrawable().apply{setColor(Color.rgb(235,237,229));cornerRadius=dp(12).toFloat()}
     }
     chatMessages.addView(event,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(3),0,dp(3))})
    }
    chatScroll.post{chatScroll.fullScroll(View.FOCUS_DOWN)}
   }catch(_:Exception){
    if(chatMessages.childCount==0)chatMessages.addView(label("تعذر الاتصال بالدردشة",13f,Color.LTGRAY))
   }
  }
 }

 private fun sendRoomMessage(){
  val message=chatInput.text.toString().trim()
  if(message.isEmpty())return
  if(message.length>500){
   Toast.makeText(this,"الحد الأقصى 500 حرف",Toast.LENGTH_SHORT).show()
   return
  }
  chatInput.isEnabled=false
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/$roomId/messages",
      JSONObject().put("message",message),authToken)
    }
    if(result.status in 200..299){
     chatInput.setText("")
     collapseRoomComposer()
     loadRoomMessages()
    }else{
     Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر إرسال الرسالة"),Toast.LENGTH_SHORT).show()
    }
   }catch(_:Exception){
    Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()
   }finally{
    chatInput.isEnabled=true
   }
  }
 }

 private fun isActiveChat()=::chatPanel.isInitialized && roomId.isNotBlank() && authToken.isNotBlank()

 private fun loadIdentity(){
  scope.launch{
   try{
    val r=withContext(Dispatchers.IO){AuthApi.get("/api/auth/me",authToken)}
    currentUserId=(r.body.optJSONObject("user")?:r.body).optString("id")
   }catch(_:Exception){}
   loadSeats()
   loadRoomMembers()
   seatRefreshJob?.cancel()
   seatRefreshJob=launch{
    while(isActive && !leaving){
     delay(5000)
     loadSeats()
    }
   }
  }
 }
 private fun emptySeats(){seats=JSONArray();for(i in 1..8)seats.put(JSONObject().put("seat_number",i));drawSeats()}
 private fun loadSeats(){
  if(roomId.isBlank())return
  scope.launch{
   try{
    val r=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/$roomId/seats",authToken)}
    if(r.status==403 || r.status==404){
     if(!leaving){
      leaving=true
      chatPoll?.cancel()
      membershipWatch?.cancel()
      try{if(connected)room.disconnect()}catch(_:Exception){}
      connected=false
      stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
      AlertDialog.Builder(this@RoomActivity)
       .setTitle("انتهى البث")
       .setMessage("قام صاحب الغرفة بإغلاق الغرفة. تم إخراجك من البث.")
       .setPositiveButton("العودة للرئيسية"){_,_->finish()}
       .setCancelable(false).show()
     }
     return@launch
    }
    if(r.status !in 200..299){status.text=r.body.optString("error","تعذر تحميل المقاعد");return@launch}
    seats=r.body.optJSONArray("seats")?:JSONArray()
    hands=r.body.optJSONArray("handRaises")?:JSONArray()
    isOwner=currentUserId.isNotBlank()&&currentUserId==r.body.optString("ownerId")
    val backgroundUrl=r.body.optString("backgroundUrl").takeIf{it.isNotBlank() && it!="null"}.orEmpty()
    if(backgroundUrl!=loadedBackgroundUrl) applyRoomBackground(backgroundUrl)
    isSeated=(0 until seats.length()).any{seats.optJSONObject(it)?.optString("user_id")==currentUserId&&currentUserId.isNotBlank()}
    // Owner identity comes from the room record, never from seat 1.
    // This keeps the creator's profile visible even after they leave the room or seat.
    val roomOwner=r.body.optJSONObject("owner")
    updatePremiumOwnerHeader(roomOwner)
    val ownerSeat=seats.optJSONObject(0)
    val ownerSeatUser=ownerSeat?.optString("user_id").orEmpty()
    if(isOwner && !ownerSeatSetupDone){
     ownerSeatSetupDone=true
     if(ownerSeatUser.isBlank() || ownerSeatUser=="null"){
      val claim=withContext(Dispatchers.IO){AuthApi.post("/api/voice-rooms/$roomId/seats/1/claim",JSONObject(),authToken)}
      if(claim.status in 200..299){
       isSeated=true
       if(connected){try{room.disconnect()}catch(_:Exception){};connected=false;connect()}
       else if(!connected && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) connect()
      }else{
       ownerSeatSetupDone=false
       Toast.makeText(this@RoomActivity,claim.body.optString("error","تعذر حجز المقعد الأول للمالك"),Toast.LENGTH_LONG).show()
      }
     }else if(ownerSeatUser==currentUserId){
      isSeated=true
     }else{
      Toast.makeText(this@RoomActivity,"المقعد الأول محجوز؛ يرجى مراجعة حالة الغرفة",Toast.LENGTH_LONG).show()
     }
    }
    mic.visibility=View.GONE
    if(::micControl.isInitialized)micControl.visibility=if(isSeated)View.VISIBLE else View.GONE
    musicButton.visibility=if(isOwner && connected)View.VISIBLE else View.GONE
    drawSeats()
   }catch(_:Exception){status.text="تعذر تحديث الغرفة"}
  }
 }

 private fun applyRoomBackground(url:String){
  loadedBackgroundUrl=url
  if(url.isBlank()){
   roomBackgroundView.setImageDrawable(null)
   roomBackgroundView.setBackgroundColor(Color.rgb(20,18,28))
   return
  }
  scope.launch{
   try{
    val bitmap=withContext(Dispatchers.IO){
     val connection=java.net.URL(url).openConnection()
     connection.connectTimeout=10000
     connection.readTimeout=15000
     connection.getInputStream().use{android.graphics.BitmapFactory.decodeStream(it)}
    }
    if(bitmap!=null && loadedBackgroundUrl==url){
     // Keep the uploaded original crisp and proportionally cropped edge-to-edge.
     roomBackgroundView.scaleType=ImageView.ScaleType.CENTER_CROP
     roomBackgroundView.setImageBitmap(bitmap)
    }
   }catch(_:Exception){
    if(loadedBackgroundUrl==url){
     roomBackgroundView.setImageDrawable(null)
     roomBackgroundView.setBackgroundColor(Color.rgb(18,16,25))
    }
   }
  }
 }

 private fun drawSeats(){
  grid.removeAllViews()
  speakerWaveViews.clear()
  for(row in 0..1){
   val line=LinearLayout(this).apply{orientation=0}
   for(col in 0..3){
    val i=row*4+col
    val seat=seats.optJSONObject(i)?:JSONObject()
    val uid=seat.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}
    val occupied=uid!=null
    val name=seat.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}
     ?:if(occupied)"متحدث" else "مقعد فارغ"
    val publicId=seat.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}
     ?:seat.optString("account_id").takeIf{it.isNotBlank()&&it!="null"}
     ?: ""
    val tile=LinearLayout(this).apply{
     orientation=1
     gravity=Gravity.CENTER
     setPadding(0,0,0,0)
     background=null
     isClickable=true
     isFocusable=true
    }
    val avatar=FrameLayout(this).apply{
     clipChildren=false
     clipToPadding=false
     if(occupied){
      addView(label(name.take(1).uppercase(),18f,Color.rgb(105,112,105),true),
       FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
     }else{
      addView(label("+",24f,Color.rgb(125,132,125),false),
       FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
     }
    }
    val avatarUrl=seat.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
    if(occupied && avatarUrl.isNotBlank()){
     val photo=ImageView(this).apply{
      scaleType=ImageView.ScaleType.CENTER_CROP
      clipToOutline=true
      background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
      tag=avatarUrl
     }
     avatar.addView(photo,FrameLayout.LayoutParams(-1,-1))
     loadAvatarInto(photo,avatarUrl)
    }
    val avatarSize=if(occupied)dp(52) else dp(54)
    val avatarStage=FrameLayout(this).apply{
     clipChildren=false
     clipToPadding=false
     background=null
     val wave=CircularAudioWaveView(this@RoomActivity)
     if(uid!=null){
      speakerWaveViews[uid]=wave
      wave.setLevel(activeSpeakerLevels[uid]?:0f)
     }
     addView(wave,FrameLayout.LayoutParams(-1,-1))
     val photoFrame=FrameLayout(this@RoomActivity).apply{
      clipChildren=false
      clipToPadding=false
      background=GradientDrawable().apply{
       shape=GradientDrawable.OVAL
       setColor(Color.TRANSPARENT)
       setStroke(dp(1.5f.toInt()),Color.rgb(225,228,220))
      }
      addView(avatar,FrameLayout.LayoutParams(avatarSize,avatarSize,Gravity.CENTER))
     }
     addView(photoFrame,FrameLayout.LayoutParams(dp(58),dp(58),Gravity.CENTER))
    }
    tile.addView(avatarStage,LinearLayout.LayoutParams(dp(68),dp(68)).apply{bottomMargin=dp(1)})
    if(occupied){
     tile.addView(label(name,11f,Color.rgb(65,72,66),true).apply{
      maxLines=1
      ellipsize=android.text.TextUtils.TruncateAt.END
      gravity=Gravity.CENTER
      includeFontPadding=false
     },LinearLayout.LayoutParams(-1,dp(18)))
    }else{
     tile.addView(label((i+1).toString(),10f,Color.rgb(125,130,123),false))
    }
    tile.setOnClickListener{seatClick(seat,i+1,uid,name)}
    line.addView(tile,LinearLayout.LayoutParams(0,dp(78),1f).apply{
     setMargins(dp(1),dp(1),dp(1),dp(1))
    })
   }
   grid.addView(line,LinearLayout.LayoutParams(-1,-2))
  }
 }
 private fun loadAvatarInto(view:ImageView,url:String){
  val cached=avatarBitmaps[url]
  if(cached!=null){view.setImageBitmap(cached);return}
  if(!avatarLoading.add(url))return
  scope.launch{
   try{
    val bitmap=withContext(Dispatchers.IO){
     java.net.URL(url).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{BitmapFactory.decodeStream(it)}
    }
    if(bitmap!=null){
     avatarBitmaps[url]=bitmap
     if(view.tag==url)view.setImageBitmap(bitmap)
    }
   }catch(_:Exception){}finally{avatarLoading.remove(url)}
  }
 }
 private fun seatClick(s:JSONObject,n:Int,uid:String?,name:String){
  if(uid==null){claimSeat(n);return}
  if(uid==currentUserId){
   AlertDialog.Builder(this)
    .setTitle("إدارة مقعدك")
    .setItems(arrayOf(if(microphoneEnabled)"كتم صوت المايك" else "تشغيل المايك","مغادرة المقعد")){_,which->
     if(which==0)toggleMic() else leaveSeat(n)
    }
    .setNegativeButton("إغلاق",null).show()
   return
  }
  if(!isOwner){Toast.makeText(this,"هذا المقعد مشغول",Toast.LENGTH_SHORT).show();return}
  val muted=s.optBoolean("is_muted")
  AlertDialog.Builder(this).setTitle(name)
   .setItems(arrayOf(if(muted)"إلغاء الكتم" else "كتم المتحدث","إزالة من المقعد")){_,which->
    scope.launch {
     try {
      val result=withContext(Dispatchers.IO) {
       if(which==0) AuthApi.post("/api/voice-rooms/$roomId/seats/$n/mute",JSONObject().put("muted",!muted),authToken)
       else AuthApi.delete("/api/voice-rooms/$roomId/seats/$n",authToken)
      }
      if(result.status in 200..299) loadSeats()
      else Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر تنفيذ العملية"),Toast.LENGTH_SHORT).show()
     } catch(_:Exception) {
      Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()
     }
    }
   }.setNegativeButton("إغلاق",null).show()
 }
 private fun leaveSeat(n:Int){
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){AuthApi.delete("/api/voice-rooms/$roomId/seats/$n",authToken)}
    if(result.status in 200..299){
     try{room.disconnect()}catch(_:Exception){}
     connected=false;isSeated=false;microphoneEnabled=false
     loadSeats();connect()
     Toast.makeText(this@RoomActivity,"غادرت المقعد، يمكنك البقاء مستمعاً",Toast.LENGTH_SHORT).show()
    }else Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر مغادرة المقعد"),Toast.LENGTH_SHORT).show()
   }catch(_:Exception){
    Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()
   }
  }
 }
 private fun claimSeat(n:Int){
  if(!connected){Toast.makeText(this,"انتظر اتصال الغرفة ثم حاول مجدداً",Toast.LENGTH_SHORT).show();return}
  scope.launch{
   try{
    val r=withContext(Dispatchers.IO){AuthApi.post("/api/voice-rooms/$roomId/seats/$n/claim",JSONObject(),authToken)}
    if(r.status in 200..299){
     Toast.makeText(this@RoomActivity,"جلست على المقعد $n",Toast.LENGTH_SHORT).show()
     try{room.disconnect()}catch(_:Exception){}
     connected=false
     isSeated=true
     connect()
    }else Toast.makeText(this@RoomActivity,r.body.optString("error","تعذر الجلوس على المقعد"),Toast.LENGTH_SHORT).show()
   }catch(_:Exception){Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}
  }
 }
 private fun connect(){
  status.text="جارٍ الاتصال بخادم الصوت..."
  val service=Intent(this,VoiceForegroundService::class.java)
  if(Build.VERSION.SDK_INT>=26) startForegroundService(service) else startService(service)
  scope.launch {
   try {
    val r=withContext(Dispatchers.IO){AuthApi.post("/api/voice-rooms/$roomId/token",JSONObject(),authToken)}
    if(r.status !in 200..299){
     val detail=r.body.optString("stage")
     val code=r.body.optString("code")
     val reason=r.body.optString("error","تعذر إصدار رمز الصوت")
     throw Exception(reason + if(detail.isNotBlank()) " ($detail/$code)" else " (HTTP ${r.status})")
    }
    room.connect(r.body.optString("serverUrl"),r.body.optString("participantToken"))
    startSpeakerTracking()
    microphoneEnabled=r.body.optBoolean("canPublish",false)
    room.localParticipant.setMicrophoneEnabled(microphoneEnabled)
    connected=true
    status.text=if(isSeated && microphoneEnabled) "متصل • أنت على المقعد والمايك جاهز" else "متصل كمستمع • اضغط على مقعد فارغ للجلوس"
    loadSeats()
    startRoomMembershipWatch()
   } catch(e:Exception) {
    status.text=e.message?:"تعذر الاتصال"
    stopService(service)
   }
  }
 }
 private fun startSpeakerTracking(){
  speakerEventsJob?.cancel()
  speakerEventsJob=scope.launch{
   try{
    room.events.collect{event->
     if(event is RoomEvent.ActiveSpeakersChanged){
      val fresh=HashMap<String,Float>()
      for(participant in event.speakers){
       val identity=participant.identity?.value ?: continue
       if(participant.isSpeaking){
        fresh[identity]=participant.audioLevel.toFloat().coerceIn(0f,1f).coerceAtLeast(0.18f)
       }
      }
      activeSpeakerLevels.clear()
      activeSpeakerLevels.putAll(fresh)
      for((userId,wave) in speakerWaveViews){
       wave.setLevel(activeSpeakerLevels[userId]?:0f)
      }
     }
    }
   }catch(_:CancellationException){}
   catch(_:Exception){}
  }
 }
 private var membershipWatch:Job?=null
 private fun loadRoomMembers(){
  if(roomId.isBlank() || authToken.isBlank())return
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/$roomId/members",authToken)}
    if(response.status !in 200..299)return@launch
    val fresh=response.body.optJSONArray("members")?:JSONArray()
    val current=LinkedHashSet<String>()
    for(i in 0 until fresh.length()){
     val member=fresh.optJSONObject(i)?:continue
     val id=member.optString("user_id")
     if(id.isNotBlank() && id!="null")current.add(id)
    }
    val previous=previousMemberIds
    if(previous==null && currentUserId.isNotBlank() && current.contains(currentUserId)){
     val me=(0 until fresh.length()).mapNotNull{fresh.optJSONObject(it)}
      .firstOrNull{it.optString("user_id")==currentUserId}
     val myName=me?.optString("display_name").orEmpty().ifBlank{me?.optString("username").orEmpty()}.ifBlank{"مستخدم"}
     joinNotices.add("$myName • لقد انضممت إلى الغرفة")
    }
    if(previous!=null){
     for(i in 0 until fresh.length()){
      val member=fresh.optJSONObject(i)?:continue
      val id=member.optString("user_id")
      if(id.isNotBlank() && id!="null" && !previous.contains(id)){
       val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
       joinNotices.add("انضم إلى الغرفة  •  $name")
      }
     }
     while(joinNotices.size>12)joinNotices.removeAt(0)
    }
    previousMemberIds=current
    roomMembers=fresh
    if(::membersButton.isInitialized){
     val count=fresh.length()
     (membersButton.getChildAt(0) as? TextView)?.text=count.toString()
    }
   }catch(_:Exception){}
  }
 }
 private fun showRoomMembers(){
  val list=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(6),dp(12),dp(6))}
  if(roomMembers.length()==0)list.addView(label("لا يوجد أعضاء لعرضهم حالياً",13f,Color.LTGRAY))
  for(i in 0 until roomMembers.length()){
   val member=roomMembers.optJSONObject(i)?:continue
   val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
   val username=member.optString("username")
   val account=member.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}?:"—"
   val isOwnerMember=member.optBoolean("is_owner")
   val seat=member.optInt("seat_number",0)
   val role=if(isOwnerMember)"صاحب الغرفة" else if(seat>0)"متحدث • مقعد $seat" else "مستمع"
   val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(8));background=GradientDrawable().apply{setColor(Color.rgb(241,243,238));cornerRadius=dp(14).toFloat();setStroke(dp(1),Color.rgb(220,233,224))}}
   val avatar=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(226,232,221))};clipToOutline=true;setImageDrawable(VectorIcon("user",Color.rgb(247,247,242),dp(22)))}
   row.addView(avatar,LinearLayout.LayoutParams(dp(42),dp(42)).apply{marginEnd=dp(10)})
   val url=member.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
   if(url.isNotBlank())loadAvatarInto(avatar,url)
   val info=LinearLayout(this).apply{orientation=1}
   info.addView(label(name,14f,Color.rgb(37,64,45),true).apply{gravity=Gravity.START})
   info.addView(label("@$username  •  ID: $account",10f,Color.rgb(103,111,101)).apply{gravity=Gravity.START})
   row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
   row.addView(label(role,10f,if(isOwnerMember)Color.rgb(83,126,94) else Color.rgb(103,111,101),true))
   row.isClickable=true
   row.setOnClickListener{showMemberProfile(member)}
   list.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})
  }
  val scroll=ScrollView(this).apply{addView(list)}
  AlertDialog.Builder(this).setTitle("الموجودون في الغرفة • ${roomMembers.length()}").setView(scroll).setPositiveButton("إغلاق",null).show()
 }
 private fun memberLevelColors(level:Int):IntArray = when(level){
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
 private fun profileValue(item:JSONObject,keys:List<String>):String{
  for(key in keys){
   val value=item.opt(key)
   if(value!=null && value!=JSONObject.NULL){
    val text=value.toString()
    if(text.isNotBlank() && text!="null" && text!="0")return text
   }
  }
  return "—"
 }
 private fun showMemberProfile(member:JSONObject){
  val uid=member.optString("user_id")
  if(uid.isBlank() || uid=="null")return
  val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
  val account=member.optString("public_id").ifBlank{member.optString("account_id","—")}
  val avatarUrl=member.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
  val xp=member.optLong("total_xp",0L).coerceAtLeast(0L)
  val level=member.optInt("level",0).takeIf{it>0} ?: run {
   var calculated=1
   var threshold=100L
   while(calculated<100 && xp>=threshold){
    calculated++
    threshold=calculated.toLong()*calculated.toLong()*100L
   }
   calculated
  }
  val fans=profileValue(member,listOf("fans_count","fans","followers_count","followersCount"))
  val followers=profileValue(member,listOf("following_count","following","follow_count","followings_count"))
  val sent=profileValue(member,listOf("diamonds_sent","total_diamonds_sent","sent_diamonds","total_sent"))
  val received=profileValue(member,listOf("diamonds_received","total_diamonds_received","received_diamonds","total_received"))
  val dialog=android.app.Dialog(this)
  val root=LinearLayout(this).apply{
   orientation=1;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(18),dp(16),dp(18),dp(18))
   background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadii=floatArrayOf(dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),0f,0f,0f,0f)}
  }
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val report=LinearLayout(this).apply{
   gravity=Gravity.CENTER_VERTICAL
   val flag=ImageView(this@RoomActivity).apply{
    setImageResource(R.drawable.ic_profile_flag)
    setColorFilter(Color.rgb(125,130,135),PorterDuff.Mode.SRC_IN)
   }
   addView(flag,LinearLayout.LayoutParams(dp(14),dp(14)).apply{marginEnd=dp(5)})
   addView(label("إبلاغ",11f,Color.rgb(125,130,135),true))
   setOnClickListener{Toast.makeText(this@RoomActivity,"سيتم إضافة الإبلاغ من بطاقة المستخدم لاحقاً",Toast.LENGTH_SHORT).show()}
  }
  top.addView(report,LinearLayout.LayoutParams(0,dp(34),1f))
  top.addView(ImageView(this).apply{
   setImageResource(R.drawable.ic_profile_close)
   setColorFilter(Color.rgb(90,95,100),PorterDuff.Mode.SRC_IN)
   setPadding(dp(9),dp(9),dp(9),dp(9))
   contentDescription="إغلاق الملف الشخصي"
   setOnClickListener{dialog.dismiss()}
  },LinearLayout.LayoutParams(dp(38),dp(34)))
  root.addView(top)
  val avatar=ImageView(this).apply{
   scaleType=ImageView.ScaleType.CENTER_CROP
   background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(235,239,235));setStroke(dp(2),Color.rgb(52,196,187))}
   clipToOutline=true
   setImageDrawable(VectorIcon("user",Color.rgb(110,130,115),dp(28)))
  }
  root.addView(avatar,LinearLayout.LayoutParams(dp(104),dp(104)).apply{topMargin=dp(-2)})
  if(avatarUrl.isNotBlank())loadAvatarInto(avatar,avatarUrl)
  root.addView(label(name,20f,Color.rgb(35,36,43),true),LinearLayout.LayoutParams(-1,dp(42)))
  root.addView(label("ID: $account",13f,Color.rgb(139,142,150)),LinearLayout.LayoutParams(-1,dp(26)))
  val levelColors=memberLevelColors(level)
  val badges=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val levelBadge=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER
   setPadding(dp(16),dp(8),dp(16),dp(8))
   background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(levelColors[0],levelColors[1],levelColors[2])).apply{
    cornerRadius=dp(14).toFloat()
    setStroke(dp(1),levelColors[3])
   }
   val levelIcon=ImageView(this@RoomActivity).apply{
    setImageResource(R.drawable.ic_profile_level)
    setColorFilter(levelColors[4],PorterDuff.Mode.SRC_IN)
   }
   addView(levelIcon,LinearLayout.LayoutParams(dp(19),dp(19)).apply{marginEnd=dp(7)})
   addView(label("LV $level",14f,levelColors[4],true))
  }
  badges.addView(levelBadge)
  root.addView(badges,LinearLayout.LayoutParams(-1,dp(48)))
  val stats=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  fun metric(value:String,title:String){
   val col=LinearLayout(this@RoomActivity).apply{orientation=1;gravity=Gravity.CENTER}
   col.addView(label(value,19f,Color.rgb(35,36,43),true),LinearLayout.LayoutParams(-1,dp(30)))
   col.addView(label(title,11f,Color.rgb(150,151,156)),LinearLayout.LayoutParams(-1,dp(25)))
   stats.addView(col,LinearLayout.LayoutParams(0,dp(58),1f))
  }
  metric(fans,"المعجبون")
  metric(followers,"يتابعهم")
  metric(sent,"ماس أرسله")
  metric(received,"ماس استلمه")
  root.addView(stats,LinearLayout.LayoutParams(-1,dp(66)).apply{topMargin=dp(8)})
  val actions=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  fun actionButton(title:String,color:Int,click:(TextView)->Unit):TextView{
   return label(title,14f,Color.WHITE,true).apply{
    background=GradientDrawable().apply{setColor(color);cornerRadius=dp(24).toFloat()}
    setPadding(dp(12),dp(12),dp(12),dp(12));isClickable=true;setOnClickListener{click(this)}
   }
  }
  val message=actionButton("مراسلة",Color.rgb(145,153,165)){openMemberConversation(uid,name)}
  actions.addView(message,LinearLayout.LayoutParams(0,dp(48),1f).apply{marginEnd=dp(6)})
  val gift=FrameLayout(this).apply{
   background=GradientDrawable().apply{setColor(Color.rgb(171,92,196));cornerRadius=dp(24).toFloat()}
   val giftIcon=ImageView(this@RoomActivity).apply{
    setImageResource(R.drawable.ic_profile_gift)
    setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN)
    setPadding(dp(14),dp(12),dp(14),dp(12))
    contentDescription="هدية"
   }
   addView(giftIcon,FrameLayout.LayoutParams(-1,-1))
   setOnClickListener{Toast.makeText(this@RoomActivity,"إرسال الهدية يحتاج ربط متجر الهدايا بالخادم",Toast.LENGTH_SHORT).show()}
  }
  actions.addView(gift,LinearLayout.LayoutParams(dp(58),dp(48)).apply{marginEnd=dp(6)})
  val follow=actionButton("جارٍ التحقق...",Color.rgb(22,194,190)){toggleMemberFollow(uid,it)}
  follow.isEnabled=false
  actions.addView(follow,LinearLayout.LayoutParams(0,dp(48),1f))
  loadMemberFollowState(uid,follow)
  root.addView(actions,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(12)})
  dialog.setContentView(root)
  dialog.window?.apply{
   setBackgroundDrawableResource(android.R.color.transparent)
   setLayout(-1,-2)
   setGravity(Gravity.BOTTOM)
   addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
   attributes=attributes.apply{dimAmount=0.58f}
  }
  dialog.show()
  dialog.window?.setLayout(-1,-2)
  dialog.window?.setGravity(Gravity.BOTTOM)
 }
 private fun renderMemberFollowState(button:TextView,following:Boolean){
  button.text=if(following)"✓ تتابعه" else "＋ متابعة"
  button.setBackgroundColor(if(following)Color.rgb(80,145,105) else Color.rgb(22,194,190))
  button.isEnabled=true
 }
 private fun loadMemberFollowState(userId:String,button:TextView){
  if(userId==currentUserId){button.text="حسابك";button.isEnabled=false;return}
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){AuthApi.get("/api/follows/status/$userId",authToken)}
    if(result.status in 200..299)renderMemberFollowState(button,result.body.optBoolean("following",false))
    else{button.text="＋ متابعة";button.isEnabled=true}
   }catch(_:Exception){button.text="＋ متابعة";button.isEnabled=true}
  }
 }
 private fun toggleMemberFollow(userId:String,button:TextView){
  if(userId==currentUserId){Toast.makeText(this,"لا يمكنك متابعة نفسك",Toast.LENGTH_SHORT).show();return}
  val wasFollowing=button.text.toString().contains("تتابعه")
  button.isEnabled=false
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){
     if(wasFollowing)AuthApi.delete("/api/follows/$userId",authToken)
     else AuthApi.post("/api/follows/$userId",JSONObject(),authToken)
    }
    if(result.status in 200..299){
     renderMemberFollowState(button,!wasFollowing)
     Toast.makeText(this@RoomActivity,if(wasFollowing)"تم إلغاء المتابعة" else "تمت المتابعة بنجاح",Toast.LENGTH_SHORT).show()
    }else{
     button.isEnabled=true
     Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر تحديث المتابعة"),Toast.LENGTH_SHORT).show()
    }
   }catch(_:Exception){
    button.isEnabled=true
    Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()
   }
  }
 }

 private fun openMemberConversation(userId:String,name:String){
  if(userId==currentUserId){
   Toast.makeText(this,"هذا حسابك",Toast.LENGTH_SHORT).show()
   return
  }
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){
     AuthApi.post("/api/messages/conversations",JSONObject().put("friendId",userId),authToken)
    }
    if(result.status !in 200..299){
     throw Exception(result.body.optString("error","تعذر إنشاء المحادثة"))
    }
    val conversation=result.body.optJSONObject("conversation")
    val conversationId=result.body.optString("conversationId")
     .ifBlank{conversation?.optString("id").orEmpty()}
     .ifBlank{conversation?.optString("conversation_id").orEmpty()}
    if(conversationId.isBlank()){
     throw Exception("الخادم لم يُرجع رقم المحادثة")
    }
    startActivity(Intent(this@RoomActivity,DirectChatActivity::class.java)
     .putExtra("conversation_id",conversationId)
     .putExtra("other_name",name)
     .putExtra("other_user_id",userId))
   }catch(e:Exception){
    Toast.makeText(this@RoomActivity,e.message?:"تعذر فتح المحادثة",Toast.LENGTH_LONG).show()
   }
  }
 }
 private fun startRoomMembershipWatch(){
  membershipWatch?.cancel()
  membershipWatch=scope.launch{
   while(isActive && connected && !leaving){
    delay(4500)
    loadSeats()
    loadRoomMembers()
   }
  }
 }
 private fun showRoomTools(){
  val items=LinearLayout(this).apply{
   orientation=1
   setPadding(dp(10),dp(5),dp(10),dp(5))
  }
  fun addTool(title:String,icon:String,action:()->Unit){
   val row=LinearLayout(this).apply{
    orientation=0;gravity=Gravity.CENTER_VERTICAL
    setPadding(dp(9),dp(7),dp(9),dp(7))
    background=GradientDrawable().apply{
     setColor(Color.rgb(246,248,243));cornerRadius=dp(10).toFloat()
    }
    val image=ImageView(this@RoomActivity).apply{
     setImageDrawable(VectorIcon(icon,Color.rgb(83,126,94),dp(18)))
     scaleType=ImageView.ScaleType.FIT_CENTER
    }
    addView(image,LinearLayout.LayoutParams(dp(22),dp(22)).apply{marginEnd=dp(9)})
    addView(label(title,12f,Color.rgb(65,91,69),true),LinearLayout.LayoutParams(0,dp(26),1f).apply{gravity=Gravity.CENTER_VERTICAL})
    setOnClickListener{action()}
   }
   items.addView(row,LinearLayout.LayoutParams(-1,dp(40)).apply{bottomMargin=dp(5)})
  }
  if(isSeated)addTool(if(microphoneEnabled)"كتم المايك" else "تشغيل المايك","mic"){toggleMic()}
  if(isOwner)addTool("الموسيقى","music"){showMusicOptions()}
  if(isOwner)addTool("خلفية الغرفة","image"){chooseRoomBackground()}
  if(items.childCount==0){
   addTool("لا توجد أدوات متاحة","settings"){}
  }
  AlertDialog.Builder(this)
   .setTitle("أدوات الغرفة")
   .setView(items)
   .setNegativeButton("إغلاق",null)
   .show()
 }

 private fun chooseRoomBackground(){
  if(!isOwner){
   Toast.makeText(this,"تغيير الخلفية متاح لصاحب الغرفة فقط",Toast.LENGTH_SHORT).show()
   return
  }
  val picker=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
   addCategory(Intent.CATEGORY_OPENABLE)
   type="image/*"
  }
  try{startActivityForResult(picker,4202)}
  catch(_:Exception){Toast.makeText(this,"تعذر فتح الصور",Toast.LENGTH_SHORT).show()}
 }

 private fun uploadRoomBackground(uri:Uri){
  if(!isOwner){
   Toast.makeText(this,"تغيير الخلفية متاح لصاحب الغرفة فقط",Toast.LENGTH_SHORT).show()
   return
  }
  status.text="جارٍ تجهيز الخلفية ورفعها..."
  scope.launch{
   try{
    val payload=withContext(Dispatchers.IO){
     val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
     contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
     if(bounds.outWidth<=0 || bounds.outHeight<=0)throw Exception("تعذر قراءة أبعاد الصورة")
     var sample=1
     while(bounds.outWidth/sample>2048 || bounds.outHeight/sample>2048)sample*=2
     val options=BitmapFactory.Options().apply{inSampleSize=sample}
     val bitmap=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)}
      ?:throw Exception("تعذر قراءة الصورة")
     try{
      var bytes=ByteArrayOutputStream()
      var quality=88
      do{
       bytes=ByteArrayOutputStream()
       bitmap.compress(Bitmap.CompressFormat.JPEG,quality,bytes)
       quality-=8
      }while(bytes.size()>3*1024*1024 && quality>=56)
      if(bytes.size()>4*1024*1024)throw Exception("حجم الصورة كبير، اختر صورة أصغر")
      JSONObject()
       .put("background_base64",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP))
       .put("image_mime","image/jpeg")
     }finally{bitmap.recycle()}
    }
    val response=withContext(Dispatchers.IO){
     AuthApi.put("/api/voice-rooms/$roomId/background",payload,authToken)
    }
    if(response.status in 200..299){
     Toast.makeText(this@RoomActivity,"تم حفظ خلفية الغرفة للجميع",Toast.LENGTH_LONG).show()
     loadSeats()
    }else{
     Toast.makeText(this@RoomActivity,response.body.optString("error","تعذر حفظ الخلفية"),Toast.LENGTH_LONG).show()
    }
   }catch(e:Exception){
    Toast.makeText(this@RoomActivity,e.message?:"تعذر رفع الخلفية",Toast.LENGTH_LONG).show()
   }finally{status.text=""}
  }
 }

 private fun toggleMic(){
  if(!connected)return
  microphoneEnabled=!microphoneEnabled
  val mixer=musicMixer
  if(mixer!=null){
   mixer.microphoneEnabled=microphoneEnabled
   status.text=if(microphoneEnabled)"المايك يعمل مع الموسيقى" else "المايك مكتوم والموسيقى مستمرة"
   refreshMicControl()
   return
  }
  refreshMicControl()
  scope.launch{
   try{
    room.localParticipant.setMicrophoneEnabled(microphoneEnabled)
    status.text=if(microphoneEnabled)"المايك يعمل" else "المايك متوقف"
   }catch(_:Exception){}
  }
 }

 private fun refreshMicControl(){
  if(!::micControl.isInitialized)return
  micControl.setImageDrawable(VectorIcon("mic",Color.WHITE,dp(17)))
  micControl.background=GradientDrawable().apply{
   shape=GradientDrawable.OVAL
   setColor(if(microphoneEnabled)Color.argb(150,133,88,190) else Color.argb(185,160,65,78))
   setStroke(dp(1),Color.argb(75,255,255,255))
  }
  micControl.contentDescription=if(microphoneEnabled)"إيقاف المايك" else "تشغيل المايك"
 }
 private fun showMusicOptions(){
  if(!isOwner || !connected){
   Toast.makeText(this,"الموسيقى متاحة لصاحب الغرفة بعد الاتصال",Toast.LENGTH_SHORT).show()
   return
  }
  val mixer=musicMixer
  val options=if(mixer==null) arrayOf("اختيار أغنية من الهاتف")
   else arrayOf(if(mixer.paused)"استئناف الموسيقى" else "إيقاف مؤقت","إيقاف الموسيقى","اختيار أغنية أخرى","مستوى صوت الموسيقى","فحص حالة الموسيقى")
  AlertDialog.Builder(this)
   .setTitle("موسيقى الغرفة")
   .setItems(options){_,which->
    if(mixer==null || which==2){
     chooseRoomMusic()
    }else if(which==0){
     mixer.paused=!mixer.paused
     Toast.makeText(this,if(mixer.paused)"تم إيقاف الموسيقى مؤقتاً" else "استؤنفت الموسيقى",Toast.LENGTH_SHORT).show()
    }else if(which==3){
     showMusicVolume(mixer)
    }else if(which==4){
     AlertDialog.Builder(this)
      .setTitle("تشخيص بث الموسيقى")
      .setMessage(mixer.diagnostics())
      .setPositiveButton("حسناً",null)
      .show()
    }else{
     stopRoomMusic()
    }
   }
   .setNegativeButton("إغلاق",null)
   .show()
 }

 private fun showMusicVolume(mixer:RoomMusicMixer){
  val seek=SeekBar(this).apply{
   max=100
   progress=(mixer.volume*100f).toInt().coerceIn(0,100)
   setPadding(dp(22),dp(12),dp(22),dp(12))
  }
  AlertDialog.Builder(this)
   .setTitle("مستوى صوت الموسيقى")
   .setView(seek)
   .setPositiveButton("حفظ"){_,_->mixer.volume=seek.progress/100f}
   .setNegativeButton("إلغاء",null)
   .show()
 }

 private fun chooseRoomMusic(){
  val picker=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
   addCategory(Intent.CATEGORY_OPENABLE)
   type="audio/*"
  }
  try{startActivityForResult(picker,4201)}
  catch(_:Exception){Toast.makeText(this,"تعذر فتح ملفات الهاتف",Toast.LENGTH_SHORT).show()}
 }

 private fun startRoomMusic(uri:Uri){
  if(!isOwner || !connected)return
  scope.launch{
   try{
    stopRoomMusic(restoreMic=false)
    // Keep the published audio track alive even when the owner has muted their microphone.
    room.localParticipant.setMicrophoneEnabled(true)
    val track=room.localParticipant.getTrackPublication(Track.Source.MICROPHONE)?.track as? LocalAudioTrack
    if(track==null){
     Toast.makeText(this@RoomActivity,"مسار الصوت غير جاهز، أعد المحاولة",Toast.LENGTH_LONG).show()
     if(!microphoneEnabled)room.localParticipant.setMicrophoneEnabled(false)
     return@launch
    }
    val mixer=RoomMusicMixer(
     this@RoomActivity,uri,
     onError={message->runOnUiThread{
      stopRoomMusic()
      Toast.makeText(this@RoomActivity,message,Toast.LENGTH_LONG).show()
     }},
     onEnded={runOnUiThread{
      stopRoomMusic()
      Toast.makeText(this@RoomActivity,"انتهت الأغنية",Toast.LENGTH_SHORT).show()
     }}
    )
    mixer.microphoneEnabled=microphoneEnabled
    musicMixer=mixer
    track.setAudioBufferCallback(mixer)
    mixer.start()
    Toast.makeText(this@RoomActivity,"بدأ إرسال الموسيقى إلى الغرفة",Toast.LENGTH_SHORT).show()
   }catch(_:Exception){
    stopRoomMusic()
    Toast.makeText(this@RoomActivity,"تعذر تشغيل الموسيقى",Toast.LENGTH_LONG).show()
   }
  }
 }

 private fun stopRoomMusic(restoreMic:Boolean=true){
  val mixer=musicMixer
  musicMixer=null
  try{
   val track=if(::room.isInitialized) room.localParticipant.getTrackPublication(Track.Source.MICROPHONE)?.track as? LocalAudioTrack else null
   track?.setAudioBufferCallback(null)
  }catch(_:Exception){}
  mixer?.stop()
  if(restoreMic && connected && !microphoneEnabled){
   scope.launch{try{room.localParticipant.setMicrophoneEnabled(false)}catch(_:Exception){}}
  }
 }

 @Deprecated("Deprecated in Android")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=Activity.RESULT_OK)return
  val uri=data?.data?:return
  try{
   contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }catch(_:Exception){}
  if(requestCode==4201){
   startRoomMusic(uri)
  }else if(requestCode==4202){
   uploadRoomBackground(uri)
  }
 }
 @Deprecated("Deprecated in Android") override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==4102){if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)connect() else status.text="يجب السماح باستخدام المايك"}}
 private fun leaveRoom(){
  if(leaving)return
  if(isOwner){
   AlertDialog.Builder(this)
    .setTitle("إدارة الغرفة")
    .setMessage("هل تريد إبقاء الغرفة مفتوحة والعودة إليها لاحقاً، أم إغلاقها نهائياً؟")
    .setPositiveButton("إبقاء الغرفة"){_,_->leaveRoomNow(false)}
    .setNegativeButton("إغلاق الغرفة نهائياً"){_,_->closeRoomNow()}
    .setNeutralButton("إلغاء",null)
    .show()
  }else{
   AlertDialog.Builder(this)
    .setTitle("مغادرة الغرفة")
    .setMessage("هل تريد الخروج من الغرفة؟")
    .setPositiveButton("مغادرة"){_,_->leaveRoomNow(false)}
    .setNegativeButton("إلغاء",null)
    .show()
  }
 }
 private fun leaveRoomNow(closeRoom:Boolean){
  if(leaving)return
  leaving=true
  scope.launch{
   try{
    stopRoomMusic(restoreMic=false)
    if(connected)room.disconnect()
    stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
    withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/$roomId/leave",JSONObject().put("keep_open",!closeRoom),authToken)
    }
   }catch(_:Exception){}
   finally{finish()}
  }
 }
 private fun closeRoomNow(){
  if(leaving)return
  AlertDialog.Builder(this)
   .setTitle("إغلاق الغرفة نهائياً؟")
   .setMessage("سيتم إنهاء البث وإخراج جميع الموجودين. لا يمكن إعادة فتح هذه الجلسة.")
   .setPositiveButton("تأكيد الإغلاق"){_,_->
    leaving=true
    scope.launch{
     try{
      stopRoomMusic(restoreMic=false)
      if(connected)room.disconnect()
      stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
      val result=withContext(Dispatchers.IO){
       AuthApi.delete("/api/voice-rooms/$roomId",authToken)
      }
      if(result.status !in 200..299){
       leaving=false
       Toast.makeText(this@RoomActivity,result.body.optString("error","خادم الغرف لا يدعم الإغلاق النهائي بعد"),Toast.LENGTH_LONG).show()
       if(!isFinishing)connect()
       return@launch
      }
     }catch(_:Exception){
      leaving=false
      Toast.makeText(this@RoomActivity,"تعذر إغلاق الغرفة من الخادم",Toast.LENGTH_LONG).show()
      if(!isFinishing)connect()
      return@launch
     }
     finish()
    }
   }
   .setNegativeButton("رجوع",null).show()
 }

 override fun onBackPressed(){leaveRoom()}
 override fun onDestroy(){
  stopRoomMusic(restoreMic=false)
  speakerEventsJob?.cancel()
  activeSpeakerLevels.clear()
  speakerWaveViews.clear()
  seatRefreshJob?.cancel()
  scope.cancel()
  super.onDestroy()
 }
}

/** موجة صوتية دائرية بنفسجية حول صورة المتحدث، تتفاعل مع مستوى الصوت القادم من LiveKit. */
private class CircularAudioWaveView(context:android.content.Context):View(context){
 private val density=resources.displayMetrics.density
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
  color=Color.rgb(190,126,255)
  style=Paint.Style.STROKE
  strokeCap=Paint.Cap.ROUND
  strokeWidth=2.1f*density
 }
 private var level=0f
 private var startedAt=android.os.SystemClock.uptimeMillis()
 fun setLevel(value:Float){
  val next=value.coerceIn(0f,1f)
  if(kotlin.math.abs(level-next)<0.025f)return
  level=next
  if(level>0.02f)postInvalidateOnAnimation() else invalidate()
 }
 override fun onDraw(canvas:Canvas){
  super.onDraw(canvas)
  val strength=level
  if(strength<=0.02f)return
  val cx=width/2f
  val cy=height/2f
  val base=minOf(width,height)*0.405f
  val phase=(android.os.SystemClock.uptimeMillis()-startedAt)/175.0
  paint.alpha=(150+strength*105).toInt().coerceIn(0,255)
  paint.strokeWidth=2.0f*density
  for(i in 0 until 48){
   val angle=(Math.PI*2.0*i/48.0)-Math.PI/2.0
   val pulse=(kotlin.math.sin(phase+i*0.58)+1.0)/2.0
   val length=((2.0+strength*5.0+pulse*strength*3.0)*density).toFloat()
   val r1=base
   val r2=base+length
   val x1=cx+kotlin.math.cos(angle).toFloat()*r1
   val y1=cy+kotlin.math.sin(angle).toFloat()*r1
   val x2=cx+kotlin.math.cos(angle).toFloat()*r2
   val y2=cy+kotlin.math.sin(angle).toFloat()*r2
   canvas.drawLine(x1,y1,x2,y2,paint)
  }
  postInvalidateOnAnimation()
 }
}

/** أيقونات Vector مخصصة مرسومة على Canvas بدون رموز Emoji. */
private class VectorIcon(private val kind:String,private val color:Int,private val size:Int):Drawable(){
 private val p=Paint(3).apply{this.color=color;style=Paint.Style.STROKE;strokeWidth=size*.09f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
 override fun draw(c:Canvas){val b=bounds;val save=c.save();c.translate(b.left.toFloat(),b.top.toFloat());c.scale(b.width().toFloat()/size,b.height().toFloat()/size);val m=size/2f
 when(kind){
 "user"->{
  c.drawCircle(m,9f,5f,p)
  val body=Path().apply{moveTo(m-10f,27f);lineTo(m-10f,24f);quadTo(m-10f,17f,m,17f);quadTo(m+10f,17f,m+10f,24f);lineTo(m+10f,27f)}
  c.drawPath(body,p)
 }
 "mic"->{c.drawRoundRect(m-4f,2f,m+4f,17f,5f,5f,p);c.drawArc(RectF(m-10f,8f,m+10f,26f),0f,180f,false,p);c.drawLine(m,26f,m,30f,p);c.drawLine(m-6f,30f,m+6f,30f,p)}
 "hand"->{val q=Path().apply{moveTo(10f,27f);lineTo(7f,19f);lineTo(7f,15f);lineTo(11f,18f);lineTo(11f,7f);quadTo(15f,4f,16f,8f);lineTo(16f,16f);lineTo(19f,10f);quadTo(23f,9f,22f,14f);lineTo(22f,18f);lineTo(25f,14f);quadTo(29f,14f,27f,20f);lineTo(24f,27f);close()};c.drawPath(q,p)}
 "chat"->{c.drawRoundRect(3f,5f,29f,23f,5f,5f,p);c.drawLine(9f,23f,7f,29f,p);for(x in listOf(10f,16f,22f))c.drawCircle(x,14f,1f,p)}
 "share"->{c.drawLine(7f,16f,25f,7f,p);c.drawLine(7f,16f,25f,25f,p);c.drawCircle(6f,16f,3f,p);c.drawCircle(26f,6f,3f,p);c.drawCircle(26f,26f,3f,p)}
 "gift"->{c.drawRect(4f,13f,28f,28f,p);c.drawRect(2f,10f,30f,15f,p);c.drawLine(16f,10f,16f,28f,p);c.drawOval(RectF(6f,3f,16f,12f),p);c.drawOval(RectF(16f,3f,26f,12f),p)}
 "music"->{c.drawLine(12f,7f,12f,23f,p);c.drawLine(12f,7f,25f,4f,p);c.drawLine(25f,4f,25f,20f,p);c.drawOval(RectF(5f,21f,13f,28f),p);c.drawOval(RectF(18f,18f,26f,25f),p)}
 "leave"->{val q=Path().apply{moveTo(8f,5f);lineTo(4f,5f);lineTo(4f,27f);lineTo(8f,27f);moveTo(16f,10f);lineTo(27f,16f);lineTo(16f,22f);moveTo(27f,16f);lineTo(10f,16f)};c.drawPath(q,p)}
 else->{c.drawArc(RectF(5f,5f,27f,27f),30f,290f,false,p);c.drawLine(21f,5f,28f,6f,p);c.drawLine(28f,6f,27f,13f,p)}
 };c.restoreToCount(save)}
 override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f};override fun getOpacity()=PixelFormat.TRANSLUCENT;override fun getIntrinsicWidth()=size;override fun getIntrinsicHeight()=size
}


/** إطار GIF الملكي لمقعد صاحب المستوى 100. */
private class RoyalSeatGifView(context:android.content.Context):View(context){
 private val movie:Movie?=try{
  context.resources.openRawResource(R.drawable.royal100).use{Movie.decodeStream(it)}
 }catch(_:Exception){null}
 private var started=0L
 private var active=false
 fun start(){if(movie==null)return;active=true;started=android.os.SystemClock.uptimeMillis();postInvalidateOnAnimation()}
 override fun onDraw(canvas:Canvas){
  super.onDraw(canvas)
  val gif=movie?:return
  if(!active)return
  val duration=gif.duration().takeIf{it>0}?:1000
  gif.setTime(((android.os.SystemClock.uptimeMillis()-started)%duration).toInt())
  val scale=minOf(width.toFloat()/gif.width(),height.toFloat()/gif.height())
  val save=canvas.save()
  canvas.translate((width-gif.width()*scale)/2f,(height-gif.height()*scale)/2f)
  canvas.scale(scale,scale)
  gif.draw(canvas,0f,0f)
  canvas.restoreToCount(save)
  postInvalidateOnAnimation()
 }
}