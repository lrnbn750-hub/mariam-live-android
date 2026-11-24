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
import android.media.Ringtone
import android.media.RingtoneManager
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.view.Gravity
import android.view.MotionEvent
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.renderer.TextureViewRenderer
import livekit.org.webrtc.RendererCommon
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
import com.mariam.live.data.GiftRepository
import com.mariam.live.model.Gift
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.io.ByteArrayInputStream
import java.net.URL
import io.livekit.android.events.collect

class RoomActivity : Activity() {
 private lateinit var room: Room
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 private var connected=false; private var leaving=false; private var microphoneEnabled=true; private var isSeated=false
 private var canPublishVideo=false; private var cameraEnabled=false
 private val cameraTracks=HashMap<String,VideoTrack>()
 private val cameraRenderers=HashMap<String,TextureViewRenderer>()
 private val cameraRendererTracks=HashMap<String,VideoTrack>()
 private var handRaised=false; private var isOwner=false; private var ownerSeatSetupDone=false
 private var roomId=""; private var authToken=""; private var currentUserId=""
 private var seats=JSONArray(); private var cameraSeats=JSONArray(); private var hands=JSONArray()
 private var roomMode="audio"; private var cameraSeatCount=0; private var cameraMainUserId=""
 private var lastSeatRenderSignature=""
 private val bg=Color.rgb(241,243,238); private val purple=Color.rgb(83,126,94); private val gold=Color.rgb(83,126,94); private val mutedText=Color.rgb(103,111,101)
 private lateinit var status:TextView; private lateinit var grid:LinearLayout
 private lateinit var roomPage:LinearLayout
 private lateinit var roomBackgroundView:ImageView
 private lateinit var roomOverlayLayer:FrameLayout
 private var watchLayer:FrameLayout?=null
 private var watchPlayer:RoomWatchPlayer?=null
 private lateinit var roomBackgroundFillView:ImageView
 private lateinit var ownerAvatarView:ImageView
 private lateinit var ownerNameView:TextView
 private lateinit var ownerIdView:TextView
 private var ownerProfileRequested=false
 private var roomOwnerUserId=""
 private var currentUserLevel=1
 private var currentUserVipLevel=0
 private var followRequestSent=false
 private val memberFollowStates=HashMap<String,Boolean>()
 private var ownerFollowButton:TextView?=null
 private var loadedBackgroundUrl=""
 private var swipeDownX=0f; private var swipeDownY=0f; private var switchingRoom=false
 private var roomSwipeGestureEligible=false
 private var seatRefreshJob:Job?=null
 private var speakerEventsJob:Job?=null
 private val activeSpeakerLevels=HashMap<String,Float>()
 private val speakerWaveViews=HashMap<String,CircularAudioWaveView>()
 private val avatarBitmaps=HashMap<String,Bitmap>()
 private val seatFrameBytes=HashMap<String,ByteArray>()
 private val seatFrameLoading=HashSet<String>()
 private val seatFrameViews=HashMap<String,MutableList<RoyalGifFrame>>()
 private var currentUserEquippedFrameUrl=""
 private val chatBubbleStylesByUser=HashMap<String,String>()
 private var chatBubbleSelectionRefreshAt=0L
 private var lastRenderedChatSignature=""
 private val avatarLoading=HashSet<String>()
 private lateinit var mic:LinearLayout
 private lateinit var micControl:ImageView
 private lateinit var cameraControl:ImageView
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
 private lateinit var roomGiftButton:FrameLayout
 private lateinit var roomToolsButton:View
 private lateinit var controlsSpacer:View
 private var composerExpanded=false
 private lateinit var membersButton:LinearLayout
 private lateinit var memberAvatars:LinearLayout
 private var roomMembers=JSONArray()
 private val giftRepository=GiftRepository()
 private var giftPoll:Job?=null
 private var trendHeartbeatJob:Job?=null
 private var trendRefreshJob:Job?=null
 private var trendRequestInFlight=false
 private var trendButton:TextView?=null
 private var trendPoints=0L
 private var trendScoreLoaded=false
 private var trendAnimator:android.animation.ValueAnimator?=null
 private var lastGiftEventId=0L
 private var giftFeedInitialized=false
 private var giftSound:Ringtone?=null
 private var previousMemberIds:MutableSet<String>?=null
 private data class RoomJoinNotice(val name:String,val level:Int,val userId:String,val avatarUrl:String="",val createdAt:Long=System.currentTimeMillis(),val vipLevel:Int=0)
 private val joinNotices=ArrayList<RoomJoinNotice>()
 private val pendingJoinToasts=ArrayList<RoomJoinNotice>()
 private var joinToastShowing=false
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun defaultBackdrop():Drawable=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(58,40,30),Color.rgb(30,22,18),Color.rgb(12,10,9)))
 private fun glassBg(alpha:Int=110,radius:Int=22,strokeAlpha:Int=60)=GradientDrawable().apply{
  setColor(Color.argb(alpha,16,12,30));cornerRadius=dp(radius).toFloat();setStroke(dp(1),Color.argb(strokeAlpha,255,255,255))
 }
 private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER;typeface=android.graphics.Typeface.create("sans-serif",if(b)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)}
 private fun panel(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),Color.rgb(85,85,85))}
 private fun ctrlBg(active:Boolean=false,danger:Boolean=false):Drawable=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
  if(danger)intArrayOf(Color.rgb(255,92,110),Color.rgb(222,40,80))
  else if(active)intArrayOf(Color.rgb(255,140,60),Color.rgb(255,64,140))
  else intArrayOf(Color.argb(115,0,0,0),Color.argb(115,0,0,0))).apply{
  shape=GradientDrawable.OVAL
  setStroke(dp(1),Color.argb(0,255,255,255))
 }
 private fun pillBg(radius:Int=20,alpha:Int=150):Drawable=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
  intArrayOf(Color.argb(alpha,0,0,0),Color.argb(alpha,0,0,0))).apply{
  cornerRadius=dp(radius).toFloat();setStroke(dp(1),Color.argb(0,255,255,255))
 }
 private class SheetOption(val text:String,val style:Int,val action:()->Unit)
 private fun sheetBg()=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(38,38,42),Color.rgb(20,20,24))).apply{
  val r=dp(28).toFloat()
  cornerRadii=floatArrayOf(r,r,r,r,0f,0f,0f,0f)
  setStroke(dp(1),Color.argb(60,255,255,255))
 }
 private fun whiteSheetBg()=GradientDrawable().apply{
  val r=dp(26).toFloat()
  setColor(Color.WHITE)
  cornerRadii=floatArrayOf(r,r,r,r,0f,0f,0f,0f)
 }
 private fun whiteSheetRoot():LinearLayout=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL
  layoutDirection=View.LAYOUT_DIRECTION_RTL
  setPadding(dp(18),dp(10),dp(18),dp(18))
  background=whiteSheetBg()
  addView(View(this@RoomActivity).apply{
   background=GradientDrawable().apply{cornerRadius=dp(3).toFloat();setColor(Color.rgb(214,216,222))}
  },LinearLayout.LayoutParams(dp(40),dp(4)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(12)})
 }
 private fun sheetRoot():LinearLayout=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL
  layoutDirection=View.LAYOUT_DIRECTION_RTL
  setPadding(dp(18),dp(10),dp(18),dp(18))
  background=sheetBg()
  addView(View(this@RoomActivity).apply{
   background=GradientDrawable().apply{cornerRadius=dp(3).toFloat();setColor(Color.argb(90,255,255,255))}
  },LinearLayout.LayoutParams(dp(40),dp(4)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(12)})
 }
 private fun presentSheet(dialog:android.app.Dialog,root:View){
  dialog.setContentView(root)
  dialog.window?.apply{
   setBackgroundDrawableResource(android.R.color.transparent)
   setDimAmount(0.62f)
   setWindowAnimations(android.R.style.Animation_InputMethod)
  }
  dialog.show()
  dialog.window?.setLayout(-1,-2)
  dialog.window?.setGravity(Gravity.BOTTOM)
 }
 private fun showSheet(root:View):android.app.Dialog{
  val dialog=android.app.Dialog(this)
  presentSheet(dialog,root)
  return dialog
 }
 private fun showActionSheet(title:String,items:List<String>,onPick:(Int)->Unit){
  val root=sheetRoot()
  var dialogRef:android.app.Dialog?=null
  root.addView(label(title,17f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(30)).apply{bottomMargin=dp(10)})
  val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  items.forEachIndexed{index,text->
   list.addView(label(text,14f,Color.WHITE,true).apply{
    gravity=Gravity.CENTER_VERTICAL or Gravity.START
    setPadding(dp(16),0,dp(16),0)
    maxLines=1
    ellipsize=android.text.TextUtils.TruncateAt.END
    background=glassBg(60,16,40)
    isClickable=true
    setOnClickListener{dialogRef?.dismiss();onPick(index)}
   },LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(7)})
  }
  val scroll=ScrollView(this).apply{overScrollMode=View.OVER_SCROLL_NEVER;addView(list)}
  root.addView(scroll,LinearLayout.LayoutParams(-1,if(items.size>6)dp(340) else -2))
  root.addView(label("إلغاء",14f,Color.WHITE,true).apply{
   background=glassBg(40,24,70)
   isClickable=true
   setOnClickListener{dialogRef?.dismiss()}
  },LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(4)})
  dialogRef=showSheet(root)
 }
 private fun showChoiceSheet(title:String,message:String,options:List<SheetOption>){
  val root=sheetRoot()
  var dialogRef:android.app.Dialog?=null
  root.addView(label(title,18f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})
  root.addView(label(message,13f,Color.argb(205,255,255,255)).apply{setLineSpacing(0f,1.15f)},LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
  for(option in options){
   root.addView(label(option.text,14.5f,Color.WHITE,true).apply{
    background=when(option.style){
     0->GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,64,129),Color.rgb(255,140,60))).apply{cornerRadius=dp(24).toFloat()}
     1->GradientDrawable().apply{setColor(Color.argb(220,235,64,90));cornerRadius=dp(24).toFloat()}
     else->glassBg(40,24,70)
    }
    isClickable=true
    setOnClickListener{dialogRef?.dismiss();option.action()}
   },LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(8)})
  }
  dialogRef=showSheet(root)
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=Color.BLACK;window.navigationBarColor=Color.BLACK;window.setSoftInputMode(if(Build.VERSION.SDK_INT>=30) android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);window.decorView.systemUiVisibility=0
  roomId=intent.getStringExtra("room_id").orEmpty()
  // Keep the first joined room as the persistent return target while the user browses other screens/broadcasts.
  val roomPrefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
  if(roomPrefs.getString("active_room_id",null).isNullOrBlank()){
   roomPrefs.edit().putString("active_room_id",roomId).apply()
  }
  authToken=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null).orEmpty()
  roomPage=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(8),dp(12),dp(10));setBackgroundColor(Color.TRANSPARENT)}
  fun glassCircleButton(iconRes:Int,description:String,onClick:()->Unit)=FrameLayout(this).apply{
   isClickable=true;isFocusable=true;elevation=dp(4).toFloat()
   background=GradientDrawable().apply{
    shape=GradientDrawable.OVAL
    setColor(Color.argb(110,16,12,30))
    setStroke(dp(1),Color.argb(70,255,255,255))
   }
   addView(ImageView(this@RoomActivity).apply{
    setImageResource(iconRes)
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    setPadding(dp(9),dp(9),dp(9),dp(9))
    contentDescription=description
   },FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
   contentDescription=description
   setOnClickListener{onClick()}
  }
  fun headerIcon(kind:String,description:String,onClick:()->Unit)=FrameLayout(this).apply{
   isClickable=true;isFocusable=true;elevation=dp(3).toFloat()
   background=ctrlBg()
   addView(ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon(kind,Color.WHITE,dp(18)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    setPadding(dp(10),dp(10),dp(10),dp(10))
    contentDescription=description
   },FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
   contentDescription=description
   setOnClickListener{onClick()}
  }
  val top=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val topRow=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  topRow.addView(headerIcon("share","مشاركة البث"){shareRoom()},LinearLayout.LayoutParams(dp(38),dp(38)).apply{marginEnd=dp(6)})
  topRow.addView(headerIcon("leave","خروج من الغرفة"){leaveRoom()},LinearLayout.LayoutParams(dp(38),dp(38)).apply{marginEnd=dp(6)})
  topRow.addView(label("⌂",22f,Color.WHITE,true).apply{
   background=ctrlBg();isClickable=true;isFocusable=true
   contentDescription="تصغير الغرفة والعودة للرئيسية مع استمرار الصوت والمقعد"
   setOnClickListener{
    startActivity(Intent(this@RoomActivity,HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
   }
  },LinearLayout.LayoutParams(dp(38),dp(38)).apply{marginEnd=dp(6)})
  topRow.addView(buildPremiumRoomHeader(),LinearLayout.LayoutParams(0,dp(42),1f))
  top.addView(topRow,LinearLayout.LayoutParams(-1,dp(48)))
  // Members pill. Child 0 must stay the counter TextView: loadRoomMembers() updates it.
  membersButton=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(10),dp(3),dp(4),dp(3))
   elevation=dp(3).toFloat()
   background=pillBg(17,105)
   addView(label("0",12.5f,Color.WHITE,true).apply{
    gravity=Gravity.CENTER
    includeFontPadding=false
   },LinearLayout.LayoutParams(-2,dp(22)).apply{marginEnd=dp(6)})
   memberAvatars=LinearLayout(this@RoomActivity).apply{
    orientation=LinearLayout.HORIZONTAL
    gravity=Gravity.CENTER_VERTICAL
    layoutDirection=View.LAYOUT_DIRECTION_LTR
   }
   repeat(3){index->
    val thumb=ImageView(this@RoomActivity).apply{
     scaleType=ImageView.ScaleType.CENTER_CROP
     clipToOutline=true
     background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(83,83,83));setStroke(dp(2),Color.rgb(24,24,24))}
     setImageDrawable(VectorIcon("user",Color.rgb(255,190,220),dp(13)))
     contentDescription="صورة مصغرة لمشاهد الغرفة ${index+1}"
    }
    memberAvatars.addView(thumb,LinearLayout.LayoutParams(dp(26),dp(26)).apply{if(index>0)marginStart=dp(-8)})
   }
   addView(memberAvatars,LinearLayout.LayoutParams(dp(58),dp(26)))
   setOnClickListener{showRoomMembers()}
  }
  // Trend chip: small translucent pill (points + flame), same look as the reference design.
  val trendChip=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(12),dp(3),dp(10),dp(3))
   background=GradientDrawable().apply{cornerRadius=dp(17).toFloat();setColor(Color.argb(105,0,0,0))}
   isClickable=true
   isFocusable=true
   contentDescription="نقاط ترند الغرفة، اضغط لعرض قائمة المستخدمين المشهورين"
   setOnClickListener{showTrendingRooms()}
  }
  trendButton=label(String.format(java.util.Locale.US,"%,d",trendPoints),13f,Color.WHITE,true).apply{
   includeFontPadding=false
   gravity=Gravity.CENTER
  }
  trendChip.addView(trendButton,LinearLayout.LayoutParams(-2,-2).apply{marginEnd=dp(5)})
  val flame=ImageView(this).apply{
   setImageDrawable(VectorIcon("flame",Color.WHITE,dp(28)))
   scaleType=ImageView.ScaleType.FIT_CENTER
   contentDescription="مؤشر الترند المتحرك"
  }
  trendChip.addView(flame,LinearLayout.LayoutParams(dp(15),dp(18)))
  val flamePulse=android.animation.ValueAnimator.ofFloat(0.9f,1.12f).apply{
   duration=620L
   repeatCount=android.animation.ValueAnimator.INFINITE
   repeatMode=android.animation.ValueAnimator.REVERSE
   addUpdateListener{anim->
    val pulse=anim.animatedValue as Float
    flame.scaleX=pulse;flame.scaleY=pulse
    flame.rotation=(pulse-1f)*28f
   }
  }
  trendChip.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener{
   override fun onViewAttachedToWindow(v:View){if(!flamePulse.isStarted)flamePulse.start()}
   override fun onViewDetachedFromWindow(v:View){flamePulse.cancel()}
  })
  // Same layout as the reference: viewers on the left, trend points on the right.
  val statsRow=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  statsRow.addView(membersButton,LinearLayout.LayoutParams(-2,dp(34)))
  statsRow.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
  statsRow.addView(trendChip,LinearLayout.LayoutParams(-2,dp(34)))
  top.addView(statsRow,LinearLayout.LayoutParams(-1,dp(36)).apply{topMargin=dp(6)})
  roomPage.addView(top,LinearLayout.LayoutParams(-1,-2))
  // مكان مخصص لمشغل يوتيوب فوق المقاعد: ارتفاعه 0 ويُفتح فقط أثناء المشاهدة (يديره RoomWatchPlayer).
  val watchHost=FrameLayout(this).apply{isClickable=false;isFocusable=false}
  watchLayer=watchHost
  roomPage.addView(watchHost,LinearLayout.LayoutParams(-1,0))
  grid=LinearLayout(this).apply{orientation=1}
  roomPage.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
  status=label("",11f,Color.rgb(90,105,95)).apply{visibility=View.GONE}
  mic=action("المايك","mic",Color.rgb(83,126,94),Color.rgb(145,174,139)){toggleMic()}
  mic.visibility=View.GONE
  musicButton=action("موسيقى","music",Color.rgb(91,130,94),Color.rgb(157,178,145)){showMusicOptions()}
  musicButton.visibility=View.GONE
  roomPage.addView(buildInlineChat(),LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(4)})
  val roomRoot=FrameLayout(this).apply{setBackgroundColor(Color.rgb(20,18,28))}
  roomOverlayLayer=FrameLayout(this).apply{isClickable=false;isFocusable=false}
  // Single sharp edge-to-edge image. CENTER_CROP preserves proportions on all screens.
  roomBackgroundView=ImageView(this).apply{
   scaleType=ImageView.ScaleType.CENTER_CROP
   background=defaultBackdrop()
   contentDescription="خلفية الغرفة"
   adjustViewBounds=false
  }
  roomBackgroundFillView=ImageView(this).apply{
   visibility=View.GONE
   setImageDrawable(null)
   setBackgroundColor(Color.TRANSPARENT)
  }
  roomRoot.addView(roomBackgroundView,FrameLayout.LayoutParams(-1,-1))
  // Soft top/bottom scrims keep the header and chat readable over any background image.
  roomRoot.addView(View(this).apply{
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
    Color.argb(165,8,6,18),Color.argb(35,8,6,18),Color.argb(30,8,6,18),Color.argb(205,8,6,18)))
  },FrameLayout.LayoutParams(-1,-1))
  roomRoot.addView(roomPage,FrameLayout.LayoutParams(-1,-1))
  roomRoot.addView(roomOverlayLayer,FrameLayout.LayoutParams(-1,-1))
  setContentView(roomRoot)
  // Respect the real status-bar inset so the header never sits under the clock/battery icons.
  roomPage.setOnApplyWindowInsetsListener{v,insets->
   val topInset=if(Build.VERSION.SDK_INT>=30)insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
    else insets.systemWindowInsetTop
   v.setPadding(dp(12),topInset+dp(8),dp(12),dp(10))
   insets
  }
  roomPage.requestApplyInsets()
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
  watchLayer?.let{layer->watchPlayer=RoomWatchPlayer(this,layer,{grid},roomId,authToken,{isOwner}).also{it.start()}}
  // Ensure membership before LiveKit connection, including direct Activity entry points.
  scope.launch{
   try{
    val joined=withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/$roomId/join",JSONObject(),authToken)
    }
    if(joined.status !in 200..299){
     val reason=joined.body.optString("error","تعذر دخول الغرفة")
     status.text=reason
     Toast.makeText(this@RoomActivity,reason,Toast.LENGTH_LONG).show()
     return@launch
    }
    if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)connect()
    else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),4102)
   }catch(e:Exception){
    val reason=e.message?:"تعذر الاتصال بالخادم"
    status.text=reason
    Toast.makeText(this@RoomActivity,reason,Toast.LENGTH_LONG).show()
   }
  }
 }

 override fun onResume(){
  super.onResume()
  hidePhoneNavigation()
  watchPlayer?.onHostResume()
 }

 override fun onPause(){
  watchPlayer?.onHostPause()
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
    dismissComposerFromOutsideTap(event.rawX,event.rawY)
    swipeDownX=event.rawX
    swipeDownY=event.rawY
    // Room switching is allowed only when the gesture starts above the chat panel.
    // A touch that starts on a comment is always owned by the chat ScrollView.
    val chatTop=if(::chatPanel.isInitialized){
     val location=IntArray(2);chatPanel.getLocationOnScreen(location);location[1].toFloat()
    }else screenHeight
    roomSwipeGestureEligible=!switchingRoom && !composerExpanded && swipeDownY<chatTop &&
     watchPlayer?.containsTouch(swipeDownX,swipeDownY)!=true
    if(roomSwipeGestureEligible){
     roomPage.animate().cancel()
     roomPage.setLayerType(View.LAYER_TYPE_HARDWARE,null)
     roomPage.translationY=0f
    }
   }
   MotionEvent.ACTION_MOVE->{
    if(roomSwipeGestureEligible){
     val dx=event.rawX-swipeDownX
     val dy=event.rawY-swipeDownY
     if(kotlin.math.abs(dy)>dp(12) && kotlin.math.abs(dy)>kotlin.math.abs(dx)*1.35f){
      roomPage.translationY=dy*0.72f
      return true
     }
    }
   }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
    if(roomSwipeGestureEligible){
     val dx=event.rawX-swipeDownX
     val dy=event.rawY-swipeDownY
     val isVertical=kotlin.math.abs(dy)>kotlin.math.abs(dx)*1.35f
     if(isVertical && (roomPage.translationY!=0f || kotlin.math.abs(dy)>dp(18))){
      val commit=event.actionMasked==MotionEvent.ACTION_UP && kotlin.math.abs(dy)>dp(145)
      roomPage.animate().cancel()
      if(commit && !switchingRoom){
       val direction=if(dy<0)1 else -1
       roomPage.animate()
        .translationY(if(direction>0)-screenHeight else screenHeight)
        .setDuration(190)
        .setInterpolator(android.view.animation.DecelerateInterpolator(1.2f))
        .withEndAction{
         roomPage.translationY=0f
         roomPage.setLayerType(View.LAYER_TYPE_NONE,null)
         switchLiveRoom(direction)
        }.start()
      }else{
       roomPage.animate().translationY(0f)
        .setDuration(190)
        .setInterpolator(android.view.animation.DecelerateInterpolator(1.4f))
        .withEndAction{roomPage.setLayerType(View.LAYER_TYPE_NONE,null)}
        .start()
      }
      roomSwipeGestureEligible=false
      return true
     }
     roomPage.setLayerType(View.LAYER_TYPE_NONE,null)
    }
    roomSwipeGestureEligible=false
   }
  }
  return super.dispatchTouchEvent(event)
 }

 private fun buildPremiumRoomHeader():View{
   // Small transparent owner card: avatar + name + ID + tiny translucent follow button.
   val card=LinearLayout(this).apply{
    orientation=LinearLayout.HORIZONTAL
    layoutDirection=View.LAYOUT_DIRECTION_LTR
    gravity=Gravity.CENTER_VERTICAL
    setPadding(dp(5),dp(3),dp(4),dp(3))
    background=GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.argb(85,0,0,0))}
   }
   val avatarFrame=FrameLayout(this).apply{
    setPadding(dp(1),dp(1),dp(1),dp(1))
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT);setStroke(dp(1),Color.argb(210,255,196,64))}
   }
   ownerAvatarView=ImageView(this).apply{
    scaleType=ImageView.ScaleType.CENTER_CROP
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(48,48,52))}
    clipToOutline=true
    setImageDrawable(VectorIcon("user",Color.WHITE,dp(17)))
   }
   avatarFrame.addView(ownerAvatarView,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
   val details=LinearLayout(this).apply{
    orientation=LinearLayout.VERTICAL
    gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT
    layoutDirection=View.LAYOUT_DIRECTION_RTL
   }
   ownerNameView=label("صاحب البث",12.5f,Color.WHITE,true).apply{
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    textDirection=View.TEXT_DIRECTION_RTL
    maxLines=1
    ellipsize=android.text.TextUtils.TruncateAt.END
    includeFontPadding=false
   }
   details.addView(ownerNameView,LinearLayout.LayoutParams(-1,dp(17)))
   ownerIdView=label("ID: "+roomId,9f,Color.argb(190,255,255,255),false).apply{
    gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    textDirection=View.TEXT_DIRECTION_LTR
    maxLines=1
    includeFontPadding=false
   }
   details.addView(ownerIdView,LinearLayout.LayoutParams(-1,dp(13)))
   val follow=TextView(this).apply{
    text="+ متابعة";textSize=10f;gravity=Gravity.CENTER
    setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD
    includeFontPadding=false
    setPadding(dp(8),0,dp(8),0)
    background=GradientDrawable().apply{cornerRadius=dp(11).toFloat();setColor(Color.argb(55,255,255,255));setStroke(dp(1),Color.argb(110,255,255,255))}
    contentDescription="حالة متابعة صاحب البث"
    setOnClickListener{toggleOwnerFollow(this)}
   }
   ownerFollowButton=follow
   card.addView(follow,LinearLayout.LayoutParams(-2,dp(22)).apply{marginEnd=dp(5)})
   card.addView(details,LinearLayout.LayoutParams(0,-1,1f))
   card.addView(avatarFrame,LinearLayout.LayoutParams(dp(34),dp(34)).apply{marginStart=dp(5)})
   return card
  }
  private fun renderOwnerFollowState(button:TextView,following:Boolean){
  followRequestSent=following
  button.text=if(following)"✓ متابَع" else "+ متابعة"
  button.contentDescription=if(following)"إلغاء متابعة صاحب البث" else "متابعة صاحب البث"
  button.background=if(following)GradientDrawable().apply{
   cornerRadius=dp(11).toFloat();setColor(Color.argb(30,255,255,255));setStroke(dp(1),Color.argb(80,255,255,255))
  } else GradientDrawable().apply{cornerRadius=dp(11).toFloat();setColor(Color.argb(55,255,255,255));setStroke(dp(1),Color.argb(110,255,255,255))}
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
    isVerticalScrollBarEnabled=false
    isVerticalFadingEdgeEnabled=true
    setFadingEdgeLength(dp(28))
    addView(chatMessages,android.widget.FrameLayout.LayoutParams(-1,-2))
   }
   addView(chatScroll,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(2)})
   val composer=LinearLayout(this@RoomActivity).apply{
    orientation=0
    gravity=Gravity.CENTER_VERTICAL
    layoutDirection=View.LAYOUT_DIRECTION_LTR
   }
   roomComposer=composer
   micControl=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("mic",Color.WHITE,dp(18)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    setPadding(dp(11),dp(11),dp(11),dp(11))
    background=ctrlBg()
    contentDescription="تشغيل أو إيقاف المايك"
    setOnClickListener{toggleMic();refreshMicControl()}
   }
   composer.addView(micControl,LinearLayout.LayoutParams(dp(44),dp(44)).apply{marginEnd=dp(6)})
   cameraControl=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("camera",Color.WHITE,dp(18)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    setPadding(dp(11),dp(11),dp(11),dp(11))
    background=ctrlBg()
    contentDescription="تشغيل الكاميرا"
    visibility=View.GONE
    setOnClickListener{toggleCamera()}
   }
   composer.addView(cameraControl,LinearLayout.LayoutParams(dp(44),dp(44)).apply{marginEnd=dp(6)})
   chatInput=EditText(this@RoomActivity).apply{
    hint="اكتب تعليقاً...";textSize=14f;maxLines=1;setSingleLine(true)
    imeOptions=EditorInfo.IME_ACTION_SEND
    inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    gravity=Gravity.CENTER_VERTICAL or Gravity.END
    setOnEditorActionListener{_,actionId,event->
     val enterPressed=event?.keyCode==android.view.KeyEvent.KEYCODE_ENTER && event.action==android.view.KeyEvent.ACTION_DOWN
     if(actionId==EditorInfo.IME_ACTION_SEND || enterPressed){sendRoomMessage();true}else false
    }
    setTextColor(Color.WHITE);setHintTextColor(Color.argb(150,255,255,255))
    setPadding(dp(14),0,dp(14),0)
    background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(215,44,44,44),Color.argb(215,21,21,21))).apply{
     cornerRadius=dp(24).toFloat()
     setStroke(dp(1),Color.argb(150,255,92,170))
    }
    minHeight=dp(46)
    includeFontPadding=false
   }
   chatInput.visibility=View.GONE
   chatIcon=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("chat",Color.WHITE,dp(19)))
    setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN)
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    background=null
    contentDescription="فتح كتابة تعليق"
    setOnClickListener{expandRoomComposer()}
   }

   roomGiftButton=FrameLayout(this@RoomActivity).apply{
    isClickable=true;isFocusable=true
    background=null
    addView(object:View(this@RoomActivity){
     private var phase=0f
     private val motion=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
      duration=1800L
      repeatCount=android.animation.ValueAnimator.INFINITE
      interpolator=android.view.animation.LinearInterpolator()
      addUpdateListener{phase=it.animatedValue as Float;invalidate()}
     }
     private fun paint(color:Int,style:android.graphics.Paint.Style=android.graphics.Paint.Style.FILL)=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{this.color=color;this.style=style;strokeWidth=1.8f;strokeCap=android.graphics.Paint.Cap.ROUND;strokeJoin=android.graphics.Paint.Join.ROUND}
     override fun onAttachedToWindow(){super.onAttachedToWindow();if(!motion.isStarted)motion.start()}
     override fun onDetachedFromWindow(){motion.cancel();super.onDetachedFromWindow()}
     override fun onDraw(canvas:android.graphics.Canvas){
      super.onDraw(canvas)
      val d=resources.displayMetrics.density
      canvas.save();canvas.scale(d,d)
      val w=width/d;val h=height/d;val cx=w/2f;val cy=h/2f
      val bob=kotlin.math.sin(phase*6.2831853f).toFloat()*1.1f
      canvas.save();canvas.translate(cx,cy+bob)
      val glow=paint(android.graphics.Color.argb((35+20*kotlin.math.sin(phase*6.2831853f)).toInt(),255,93,174))
      glow.setShadowLayer(8f,0f,2f,android.graphics.Color.argb(100,255,80,170))
      canvas.drawCircle(0f,0f,19f,glow)
      val box=android.graphics.RectF(-13f,-5f,13f,14f)
      val body=paint(android.graphics.Color.WHITE)
      body.shader=android.graphics.LinearGradient(-13f,-5f,13f,14f,intArrayOf(android.graphics.Color.rgb(255,91,164),android.graphics.Color.rgb(255,140,60),android.graphics.Color.rgb(91,132,255)),null,android.graphics.Shader.TileMode.CLAMP)
      canvas.drawRoundRect(box,4f,4f,body)
      val lid=android.graphics.RectF(-15f,-11f,15f,-4f)
      canvas.drawRoundRect(lid,3.5f,3.5f,body)
      val ribbon=paint(android.graphics.Color.rgb(255,224,137))
      canvas.drawRoundRect(android.graphics.RectF(-2.3f,-10f,2.3f,13f),2f,2f,ribbon)
      canvas.drawRoundRect(android.graphics.RectF(-14f,-8.2f,14f,-5.7f),1.2f,1.2f,ribbon)
      val bow=paint(android.graphics.Color.rgb(255,232,165),android.graphics.Paint.Style.STROKE)
      bow.strokeWidth=2.5f
      val left=android.graphics.Path().apply{moveTo(0f,-10f);cubicTo(-15f,-23f,-18f,-7f,-3f,-7f)}
      val right=android.graphics.Path().apply{moveTo(0f,-10f);cubicTo(15f,-23f,18f,-7f,3f,-7f)}
      canvas.drawPath(left,bow);canvas.drawPath(right,bow)
      canvas.drawCircle(0f,-9f,2.1f,ribbon)
      val sparkle=paint(android.graphics.Color.WHITE)
      sparkle.alpha=(125+110*(0.5f+0.5f*kotlin.math.sin(phase*6.2831853f))).toInt()
      val angle=phase*6.2831853f
      for(i in 0..2){
       val a=angle+i*2.094f
       val x=kotlin.math.cos(a)*21f;val y=kotlin.math.sin(a)*17f-2f
       canvas.drawCircle(x,y,if(i==0)1.7f else 1.1f,sparkle)
       if(i==0){canvas.drawLine(x-3f,y,x+3f,y,sparkle);canvas.drawLine(x,y-3f,x,y+3f,sparkle)}
      }
      canvas.restore();canvas.restore()
     }
    },FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
    contentDescription="فتح الهدايا"
    setOnClickListener{showRoomGiftPicker()}
    val pulse=android.view.animation.ScaleAnimation(
     1f,1.10f,1f,1.10f,
     android.view.animation.Animation.RELATIVE_TO_SELF,0.5f,
     android.view.animation.Animation.RELATIVE_TO_SELF,0.5f
    ).apply{
     duration=850
     repeatCount=android.view.animation.Animation.INFINITE
     repeatMode=android.view.animation.Animation.REVERSE
     interpolator=android.view.animation.AccelerateDecelerateInterpolator()
    }
    startAnimation(pulse)
   }
   composer.addView(roomGiftButton,LinearLayout.LayoutParams(dp(54),dp(54)).apply{marginEnd=dp(6)})
   // Flexible gap keeps mic + gift anchored on the left and menu + chat anchored on the right.
   // The flexible middle area is now a "say something" pill that opens the composer.
   controlsSpacer=TextView(this@RoomActivity).apply{
    text="قل شيئاً..."
    textSize=13f
    gravity=Gravity.CENTER_VERTICAL or Gravity.END
    setTextColor(Color.argb(215,255,255,255))
    setPadding(dp(18),0,dp(18),0)
    includeFontPadding=false
    background=pillBg(22,150)
    contentDescription="فتح كتابة تعليق"
    isClickable=true;isFocusable=true
    setOnClickListener{expandRoomComposer()}
   }
   composer.addView(controlsSpacer,LinearLayout.LayoutParams(0,dp(44),1f).apply{marginEnd=dp(6)})
   composer.addView(chatInput,LinearLayout.LayoutParams(0,dp(48),1f).apply{marginEnd=dp(7)})
   sendButton=TextView(this@RoomActivity).apply{
    text="إرسال";textSize=13f;gravity=Gravity.CENTER
    typeface=android.graphics.Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE);setPadding(dp(18),0,dp(18),0)
    background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,64,140),Color.rgb(255,140,60))).apply{cornerRadius=dp(23).toFloat()}
    visibility=View.GONE
    setOnClickListener{sendRoomMessage()}
   }
   composer.addView(sendButton,LinearLayout.LayoutParams(-2,dp(46)))
   roomToolsButton=ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("grid",Color.WHITE,dp(20)))
    scaleType=ImageView.ScaleType.CENTER_INSIDE
    setPadding(dp(11),dp(11),dp(11),dp(11))
    contentDescription="إعدادات الغرفة"
    background=ctrlBg()
    isClickable=true
    isFocusable=true
    setOnClickListener{showRoomTools()}
   }
   composer.addView(roomToolsButton,LinearLayout.LayoutParams(dp(44),dp(44)).apply{marginStart=dp(2)})
   // chatIcon stays as an object (visibility toggles use it) but the pill above replaces it visually.
   addView(composer,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(4);bottomMargin=dp(2)})

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
  micControl.visibility=View.GONE
  cameraControl.visibility=View.GONE
  roomGiftButton.visibility=View.GONE
  roomToolsButton.visibility=View.GONE
  controlsSpacer.visibility=View.GONE
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
  micControl.visibility=View.VISIBLE
  cameraControl.visibility=if(isSeated && canPublishVideo)View.VISIBLE else View.GONE
  roomGiftButton.visibility=View.VISIBLE
  roomToolsButton.visibility=View.VISIBLE
  controlsSpacer.visibility=View.VISIBLE
  val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
  imm.hideSoftInputFromWindow(chatInput.windowToken,0)
 }

 // Close the keyboard when the user taps the room outside the message field.
 // Keep the unsent draft and never trigger the send action from this path.
 private fun dismissComposerFromOutsideTap(rawX:Float,rawY:Float){
  if(!composerExpanded || !::chatInput.isInitialized)return
  fun contains(view:View):Boolean{
   if(view.visibility!=View.VISIBLE)return false
   val p=IntArray(2);view.getLocationOnScreen(p)
   return rawX>=p[0] && rawX<=p[0]+view.width && rawY>=p[1] && rawY<=p[1]+view.height
  }
  if(contains(chatInput) || (::sendButton.isInitialized && contains(sendButton)))return
  composerExpanded=false
  chatInput.clearFocus()
  chatInput.visibility=View.GONE
  sendButton.visibility=View.GONE
  chatIcon.visibility=View.VISIBLE
  micControl.visibility=if(isSeated)View.VISIBLE else View.GONE
  cameraControl.visibility=if(isSeated && canPublishVideo)View.VISIBLE else View.GONE
  roomGiftButton.visibility=View.VISIBLE
  roomToolsButton.visibility=View.VISIBLE
  controlsSpacer.visibility=View.VISIBLE
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
    // Refresh the equipped bubble skins for visible senders at most every 15 seconds.
    val nowForBubbleSync=System.currentTimeMillis()
    if(nowForBubbleSync-chatBubbleSelectionRefreshAt>15000L && messages.length()>0){
     val senderIds=LinkedHashSet<String>()
     for(index in 0 until messages.length()){
      val senderId=messages.optJSONObject(index)?.let { msg -> msg.optString("user_id").ifBlank { msg.optString("sender_id").ifBlank { (msg.optJSONObject("user") ?: msg.optJSONObject("sender"))?.optString("id").orEmpty() } } }.orEmpty()
      if(senderId.isNotBlank())senderIds.add(senderId)
     }
     if(senderIds.isNotEmpty()){
      try{
       val selectionResult=withContext(Dispatchers.IO){
        AuthApi.get("/api/chat-bubbles/selected?user_ids="+senderIds.joinToString(","),authToken)
       }
       if(selectionResult.status in 200..299){
        val selections=selectionResult.body.optJSONArray("selections")?:JSONArray()
        val selectedUserIds=HashSet<String>()
        for(index in 0 until selections.length()){
         val selected=selections.optJSONObject(index)?:continue
         val userId=selected.optString("user_id")
         val itemId=selected.optString("item_id")
         if(userId.isNotBlank() && itemId.startsWith("chat-bubble-")){
          chatBubbleStylesByUser[userId]=itemId
          selectedUserIds.add(userId)
         }
        }
        for(userId in senderIds)if(!selectedUserIds.contains(userId))chatBubbleStylesByUser.remove(userId)
        chatBubbleSelectionRefreshAt=nowForBubbleSync
       }
      }catch(_:Exception){ /* Keep the last known skin if the optional lookup is unavailable. */ }
     }
    }
    val activeJoinNotices=joinNotices.filter{nowForBubbleSync-it.createdAt<=12000L}.takeLast(2)
    val chatRenderSignature=buildString{
     append(messages.toString())
     append('|')
     append(chatBubbleStylesByUser.toSortedMap().entries.joinToString(";"){it.key+":"+it.value})
     append('|')
     activeJoinNotices.forEach{append(it.userId).append(':').append(it.createdAt/1000L).append(';')}
    }
    // Polling should not tear down and recreate identical chat bubbles every 3 seconds.
    if(chatRenderSignature==lastRenderedChatSignature)return@launch
    // Preserve the reader's position during polling; only follow new messages
    // automatically when they were already at the bottom.
    val previousScrollY=chatScroll.scrollY
    val wasAtBottom=chatScroll.getChildAt(0)?.let{
     chatScroll.scrollY+chatScroll.height>=it.height-dp(28)
    }?:true
    chatMessages.removeAllViews()
    // Keep the live overlay compact so chat never climbs into the eighth seat.
    val firstVisibleMessage=(messages.length()-4).coerceAtLeast(0)
    for(i in firstVisibleMessage until messages.length()){
     val item=messages.optJSONObject(i)?:continue
     val sender=item.optString("display_name").ifBlank{item.optString("username","مستخدم")}
     val body=item.optString("message")
     val mine=item.optString("user_id")==currentUserId
     // Read level when the chat API supplies it, including nested user payloads.
     val senderUser=item.optJSONObject("user")?:item.optJSONObject("sender")
     val senderId=item.optString("user_id").ifBlank { item.optString("sender_id").ifBlank { senderUser?.optString("id").orEmpty() } }
     val senderLevel=if(senderId.isNotBlank() && senderId==currentUserId) currentUserLevel
      else (if(item.has("level"))item.optInt("level",1) else
       if(item.has("user_level"))item.optInt("user_level",1) else senderUser?.optInt("level",1)?:1).coerceIn(1,100)
     val isMaxLevel=senderLevel>=100
     // Full-width GIF frame with a dedicated text-safe panel over its center.
     val maxBubbleWidth=(resources.displayMetrics.widthPixels*0.82f).toInt()
     val selectedBubbleStyle=chatBubbleStylesByUser[senderId]
     val bubble=FrameLayout(this@RoomActivity).apply{
      layoutDirection=View.LAYOUT_DIRECTION_LTR
      background=when{
       !selectedBubbleStyle.isNullOrBlank()->ChatBubbleStyleDrawable(selectedBubbleStyle)
       else->ChatBubbleStyleDrawable("chat-bubble-dragon")
      }
      elevation=dp(2).toFloat()
     }
     val content=LinearLayout(this@RoomActivity).apply{
      orientation=LinearLayout.VERTICAL
      gravity=Gravity.CENTER
      layoutDirection=View.LAYOUT_DIRECTION_RTL
      setPadding(dp(3),dp(1),dp(3),dp(1))
     }
     val senderLine=LinearLayout(this@RoomActivity).apply{
      orientation=LinearLayout.HORIZONTAL
      gravity=Gravity.CENTER
      layoutDirection=View.LAYOUT_DIRECTION_RTL
     }
     val levelBadge=TextView(this@RoomActivity).apply{
      text="LV $senderLevel"
      textSize=9f
      setTextColor(Color.WHITE)
      typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
      gravity=Gravity.CENTER
      setPadding(dp(6),dp(1),dp(6),dp(1))
      background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
       if(isMaxLevel)intArrayOf(Color.rgb(255,210,92),Color.rgb(240,150,50)) else intArrayOf(Color.rgb(255,64,140),Color.rgb(255,140,60))).apply{
       cornerRadius=dp(9).toFloat()
       setStroke(dp(1),Color.argb(120,255,255,255))
      }
      if(isMaxLevel)setTextColor(Color.rgb(70,40,4))
     }
     senderLine.addView(levelBadge,LinearLayout.LayoutParams(-2,dp(16)).apply{marginEnd=dp(5)})
     senderLine.addView(TextView(this@RoomActivity).apply{
      text=sender
      textSize=10.5f
      setTextColor(Color.WHITE)
      typeface=android.graphics.Typeface.DEFAULT_BOLD
      maxLines=1
      ellipsize=android.text.TextUtils.TruncateAt.END
      maxWidth=(maxBubbleWidth*0.72f).toInt()
      includeFontPadding=false
      setShadowLayer(2f,0f,1f,Color.BLACK)
     },LinearLayout.LayoutParams(-2,dp(18)))
     content.addView(senderLine,LinearLayout.LayoutParams(-2,dp(21)))
     content.addView(TextView(this@RoomActivity).apply{
      text=body
      textSize=12f
      setTextColor(Color.WHITE)
      typeface=android.graphics.Typeface.create("sans-serif",if(isMaxLevel)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
      gravity=Gravity.CENTER
      textAlignment=View.TEXT_ALIGNMENT_CENTER
      textDirection=View.TEXT_DIRECTION_FIRST_STRONG_RTL
      maxWidth=(maxBubbleWidth*0.76f).toInt()
      maxLines=4
      ellipsize=android.text.TextUtils.TruncateAt.END
      includeFontPadding=false
      setLineSpacing(0f,1f)
      setShadowLayer(2f,0f,1f,Color.BLACK)
     },LinearLayout.LayoutParams(-2,-2))
     val contentParams=FrameLayout.LayoutParams(
      -2,-2,Gravity.CENTER
     ).apply{
      leftMargin=dp(10);rightMargin=dp(10)
      topMargin=dp(2);bottomMargin=dp(2)
     }
     bubble.addView(content,contentParams)
     val line=LinearLayout(this@RoomActivity).apply{
      orientation=LinearLayout.HORIZONTAL
      gravity=if(mine)Gravity.END else Gravity.START
      setPadding(0,dp(1),0,dp(1))
      addView(bubble,LinearLayout.LayoutParams(-2,-2).apply{
       marginStart=dp(2);marginEnd=dp(2)
      })
     }
     chatMessages.addView(line,LinearLayout.LayoutParams(-1,-2))
    }
    // Join notifications in chat stay ordinary text-only system comments.
    val now=System.currentTimeMillis()
    joinNotices.removeAll{now-it.createdAt>12000L}
    for(notice in joinNotices.takeLast(2)){
     val event=TextView(this@RoomActivity).apply{
      text="${notice.name} انضم إلى الغرفة"
      textSize=11.5f
      gravity=Gravity.CENTER
      setTextColor(Color.rgb(255,226,238))
      setPadding(dp(12),dp(5),dp(12),dp(5))
      background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(140,104,104,104),Color.argb(140,255,64,129))).apply{
       cornerRadius=dp(14).toFloat()
      }
     }
     chatMessages.addView(event,LinearLayout.LayoutParams(-2,-2).apply{gravity=Gravity.START;setMargins(dp(2),dp(3),dp(2),dp(3))})
    }
    chatScroll.post{
     if(wasAtBottom)chatScroll.fullScroll(View.FOCUS_DOWN)
     else chatScroll.scrollTo(0,previousScrollY.coerceAtMost(chatMessages.height-chatScroll.height).coerceAtLeast(0))
    }
    lastRenderedChatSignature=chatRenderSignature
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
   // These independent identity decorations were previously fetched one after
   // another. Run them concurrently without blocking the room's main thread.
   val responses=supervisorScope{
    val me=async(Dispatchers.IO){runCatching{AuthApi.get("/api/auth/me",authToken)}.getOrNull()}
    val level=async(Dispatchers.IO){runCatching{AuthApi.get("/api/levels/me",authToken)}.getOrNull()}
    val vip=async(Dispatchers.IO){runCatching{AuthApi.get("/api/vip/me",authToken)}.getOrNull()}
    val inventory=async(Dispatchers.IO){runCatching{AuthApi.get("/api/inventory",authToken)}.getOrNull()}
    listOf(me.await(),level.await(),vip.await(),inventory.await())
   }
   responses[0]?.let{r->
    if(r.status in 200..299)currentUserId=(r.body.optJSONObject("user")?:r.body).optString("id")
   }
   responses[1]?.let{r->
    if(r.status in 200..299)currentUserLevel=r.body.optInt("level",1).coerceIn(1,100)
   }
   responses[2]?.let{r->
    if(r.status in 200..299)currentUserVipLevel=readActiveVipLevel(r.body)
   }
   responses[3]?.let{r->
    if(r.status in 200..299){
     val items=r.body.optJSONArray("items")?:JSONArray()
     for(i in 0 until items.length()){
      val item=items.optJSONObject(i)?:continue
      if(item.optString("item_type")=="frame" && item.optBoolean("owned") && item.optBoolean("equipped")){
       currentUserEquippedFrameUrl=item.optString("asset_url").takeIf{it.startsWith("https://")}.orEmpty()
       break
      }
     }
    }
   }
   // Start chat only after identity and the authoritative level have loaded.
   chatPoll?.cancel()
   loadRoomMessages()
   chatPoll=launch{
    while(isActive){delay(3000);loadRoomMessages()}
   }
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
     // A seats endpoint denial alone is NOT proof that the room was closed.
     // Re-check the authoritative room membership before ending the LiveKit session.
     val membership=withContext(Dispatchers.IO){
      runCatching{AuthApi.get("/api/voice-rooms/$roomId/members",authToken)}.getOrNull()
     }
     if(membership==null || membership.status !in 200..299){
      status.text="تعذر تحديث المقاعد مؤقتاً؛ اتصالك بالغرفة مستمر"
      return@launch
     }
     val members=membership.body.optJSONArray("members")?:JSONArray()
     val stillMember=currentUserId.isNotBlank() && (0 until members.length()).any{index->
      members.optJSONObject(index)?.optString("user_id")==currentUserId
     }
     if(stillMember){
      // The user is still in the room; ignore this stale/temporary 403 or 404.
      status.text="تعذر تحديث المقاعد مؤقتاً؛ اتصالك بالغرفة مستمر"
      return@launch
     }
     // Membership is now authoritatively absent. This can mean a moderator
     // removed the user or the room ended; do not falsely blame the room owner.
     if(!leaving){
      leaving=true
      chatPoll?.cancel()
      membershipWatch?.cancel()
      seatRefreshJob?.cancel()
      try{if(connected)room.disconnect()}catch(_:Exception){}
      connected=false
      stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
      val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
      if(prefs.getString("active_room_id",null)==roomId)prefs.edit().remove("active_room_id").apply()
      AlertDialog.Builder(this@RoomActivity,android.R.style.Theme_DeviceDefault_Dialog_Alert)
       .setTitle("انتهى اتصالك بالغرفة")
       .setMessage("لم تعد عضويتك فعّالة في هذه الغرفة. ربما أُغلقت الغرفة أو أزالك المشرف.")
       .setPositiveButton("العودة للرئيسية"){_,_->finish()}
       .setCancelable(false).show()
     }
     return@launch
    }
    if(r.status !in 200..299){status.text=r.body.optString("error","تعذر تحميل المقاعد");return@launch}
    seats=r.body.optJSONArray("seats")?:JSONArray()
    cameraSeats=r.body.optJSONArray("cameraSeats")?:JSONArray()
    roomMode=r.body.optString("roomMode","audio").lowercase()
    cameraSeatCount=r.body.optInt("cameraSeatCount",if(cameraSeats.length()>0)cameraSeats.length() else 0)
    cameraMainUserId=r.body.optString("cameraMainUserId").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
    hands=r.body.optJSONArray("handRaises")?:JSONArray()
    // The seat endpoint is polled every 5 seconds. Avoid rebuilding the video
    // hierarchy when nothing visual changed; rebuilding TextureViews causes
    // the camera preview to blink like it is refreshing.
    val seatRenderSignature=listOf(
     roomMode,cameraSeatCount,cameraMainUserId,
     seats.toString(),cameraSeats.toString()
    ).joinToString("|")
    val seatsNeedRedraw=seatRenderSignature!=lastSeatRenderSignature
    isOwner=currentUserId.isNotBlank()&&currentUserId==r.body.optString("ownerId")
    val backgroundUrl=r.body.optString("backgroundUrl").takeIf{it.isNotBlank() && it!="null"}.orEmpty()
    // Audio rooms always use the fixed supplied artwork; camera rooms keep their uploaded background.
    if(roomMode=="audio"){
     if(loadedBackgroundUrl!="__fixed_audio_backdrop__") applyAudioRoomBackdrop()
    }else if(backgroundUrl!=loadedBackgroundUrl) applyRoomBackground(backgroundUrl)
    isSeated=if(roomMode=="camera") (0 until cameraSeats.length()).any{cameraSeats.optJSONObject(it)?.optString("user_id")==currentUserId&&currentUserId.isNotBlank()} else (0 until seats.length()).any{seats.optJSONObject(it)?.optString("user_id")==currentUserId&&currentUserId.isNotBlank()}
    // Owner identity comes from the room record, never from seat 1.
    // This keeps the creator's profile visible even after they leave the room or seat.
    val roomOwner=r.body.optJSONObject("owner")
    updatePremiumOwnerHeader(roomOwner)
    val ownerSeat=seats.optJSONObject(0)
    val ownerSeatUser=ownerSeat?.optString("user_id").orEmpty()
    if(roomMode=="audio" && isOwner && !ownerSeatSetupDone){
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
    if(::micControl.isInitialized){micControl.visibility=if(isSeated)View.VISIBLE else View.GONE;refreshMicControl()}
    if(::cameraControl.isInitialized)cameraControl.visibility=if(roomMode=="camera" && isSeated && canPublishVideo)View.VISIBLE else View.GONE
    musicButton.visibility=if(isOwner && connected)View.VISIBLE else View.GONE
    if(seatsNeedRedraw){
     lastSeatRenderSignature=seatRenderSignature
     drawSeats()
    }
   }catch(_:Exception){status.text="تعذر تحديث الغرفة"}
  }
 }

 private fun applyAudioRoomBackdrop(){
  loadedBackgroundUrl="__fixed_audio_backdrop__"
  roomBackgroundView.scaleType=ImageView.ScaleType.CENTER_CROP
  roomBackgroundView.setImageResource(R.drawable.bg_voice_room)
 }

 private fun applyRoomBackground(url:String){
  loadedBackgroundUrl=url
  if(url.isBlank()){
   roomBackgroundView.setImageDrawable(null)
   roomBackgroundView.background=defaultBackdrop()
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
     roomBackgroundView.background=defaultBackdrop()
    }
   }
  }
 }

 // Normalize VIP membership payloads returned by both seat and /api/vip/me endpoints.
 private fun readActiveVipLevel(payload:JSONObject?):Int{
  if(payload==null)return 0
  val nested=payload.optJSONObject("membership")
   ?:payload.optJSONObject("vip_membership")
   ?:payload.optJSONObject("vip")
  val source=nested?:payload
  val active=source.optBoolean("active",source.optBoolean("vip_active",payload.optBoolean("vip_active",true)))
  if(!active)return 0
  return source.optInt("vip_level",source.optInt("vipLevel",payload.optInt("vip_level",payload.optInt("vipLevel",0))))
   .coerceIn(0,12)
 }

 private fun drawSeats(){
  releaseCameraRenderers()
  grid.removeAllViews()
  speakerWaveViews.clear()
  val cameraMode=roomMode=="camera"
  if(cameraMode){drawCameraSeats();return}
  val visibleSeats=if(cameraMode)cameraSeats else seats
  val visibleCount=if(cameraMode)cameraSeatCount.coerceIn(4,6) else 8
  val columns=if(cameraMode && visibleCount==4)2 else if(cameraMode)3 else 4
  val rows=(visibleCount+columns-1)/columns
  for(row in 0 until rows){
   val line=LinearLayout(this).apply{
    orientation=LinearLayout.HORIZONTAL
    gravity=Gravity.CENTER
   }
   for(col in 0 until columns){
    val i=row*columns+col
    if(i>=visibleCount) continue
    val seat=visibleSeats.optJSONObject(i)?:JSONObject()
    val seatNumber=seat.optInt("seat_number",i+1)
    val uid=seat.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}
    val occupied=uid!=null
    val isHost=if(cameraMode)false else i==0
    val name=seat.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}
     ?:if(occupied)"متحدث" else "مقعد فارغ"
    val tile=LinearLayout(this).apply{
     orientation=LinearLayout.VERTICAL
     gravity=Gravity.CENTER_HORIZONTAL
     setPadding(dp(2),dp(4),dp(2),dp(4))
     isClickable=true
     isFocusable=true
     background=null
    }
    val avatar=FrameLayout(this).apply{
     clipChildren=false
     clipToPadding=false
     background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(occupied)Color.rgb(41,41,41) else Color.TRANSPARENT)}
     if(occupied){
      addView(label(name.take(1).uppercase(),18f,Color.rgb(205,210,225),true),
       FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
     }else{
      addView(ImageView(this@RoomActivity).apply{
       setImageDrawable(VectorIcon("plus",Color.argb(215,255,255,255),dp(22)))
       scaleType=ImageView.ScaleType.CENTER
      },FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
     }
    }
    val avatarUrl=resolveSeatAvatarUrl(seat)
    val seatUser=seat.optJSONObject("user")?:seat.optJSONObject("member")
    val nestedVip=seat.optJSONObject("vip_membership")
      ?:seat.optJSONObject("vip")
      ?:seatUser?.optJSONObject("vip_membership")
      ?:seatUser?.optJSONObject("vip")
      ?:seatUser?.optJSONObject("membership")
    val vipPayload=nestedVip?:seatUser?:seat
    val payloadVipLevel=readActiveVipLevel(vipPayload)
    val seatVipLevel=when {
     uid==currentUserId && currentUserVipLevel>0 -> currentUserVipLevel
     seat.has("vip_active") && !seat.optBoolean("vip_active") -> 0
     payloadVipLevel>0 -> payloadVipLevel
     else -> 0
    }
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
      setPadding(dp(2),dp(2),dp(2),dp(2))
      background=GradientDrawable(GradientDrawable.Orientation.TL_BR,when{
       isHost->intArrayOf(Color.rgb(255,226,138),Color.rgb(240,160,50))
       occupied->intArrayOf(Color.rgb(255,92,170),Color.rgb(255,140,60))
       else->intArrayOf(Color.argb(40,255,255,255),Color.argb(40,255,255,255))
      }).apply{
       shape=GradientDrawable.OVAL
       // Dashed ring invites people to take an empty seat.
       if(!occupied)setStroke(dp(1),Color.argb(150,255,255,255),dp(4).toFloat(),dp(3).toFloat())
      }
      addView(avatar,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
     }
     addView(photoFrame,FrameLayout.LayoutParams(dp(54),dp(54),Gravity.CENTER))
     val liveVideoTrack=if(occupied && uid!=null)videoTrackFor(uid) else null
     if(liveVideoTrack!=null){
      avatar.visibility=View.GONE
      val videoRenderer=obtainCameraRenderer(uid!!,liveVideoTrack)
      addView(videoRenderer,FrameLayout.LayoutParams(dp(50),dp(50),Gravity.CENTER))
     }
     val frameUrl=sequenceOf("equipped_frame_url","avatar_frame_url","frame_url","profile_frame_url")
      .map{seat.optString(it)}.firstOrNull{it.startsWith("https://")}
      ?:if(uid==currentUserId)currentUserEquippedFrameUrl else null
     if(occupied && !frameUrl.isNullOrBlank()){
      val frameView=RoyalGifFrame(this@RoomActivity).apply{
       visibility=View.VISIBLE
       tag=frameUrl
      }
      addView(frameView,FrameLayout.LayoutParams(dp(65),dp(65),Gravity.CENTER))
      seatFrameViews.getOrPut(frameUrl){mutableListOf()}.add(frameView)
      loadSeatFrame(frameView,frameUrl)
     }
     if(isHost){
      addView(label("المضيف",8.5f,Color.rgb(60,38,4),true).apply{
       includeFontPadding=false
       setPadding(dp(8),0,dp(8),0)
       background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,226,138),Color.rgb(240,172,60))).apply{cornerRadius=dp(8).toFloat()}
      },FrameLayout.LayoutParams(-2,dp(14),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
     }else{
      addView(label("$seatNumber",9f,Color.WHITE,true).apply{
       includeFontPadding=false
       background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(215,22,14,48));setStroke(dp(1),Color.argb(130,255,255,255))}
      },FrameLayout.LayoutParams(dp(17),dp(17),Gravity.BOTTOM or Gravity.END))
     }
    }
    tile.addView(avatarStage,LinearLayout.LayoutParams(dp(66),dp(66)))
    if(occupied){
     val seatLevel=(if(seat.has("level"))seat.optInt("level",1) else seat.optInt("user_level",1)).coerceIn(1,100)
     val nameLine=LinearLayout(this).apply{
      orientation=LinearLayout.HORIZONTAL
      gravity=Gravity.CENTER
      layoutDirection=View.LAYOUT_DIRECTION_LTR
     }
     nameLine.addView(label(name,10.5f,if(seatLevel>=100||isHost)Color.rgb(255,226,150) else Color.WHITE,true).apply{
      maxLines=1
      ellipsize=android.text.TextUtils.TruncateAt.END
      includeFontPadding=false
      setShadowLayer(3f,0f,1f,Color.argb(190,0,0,0))
     },LinearLayout.LayoutParams(0,dp(16),1f))
     if(seatLevel>=100){
      nameLine.addView(label("♛100",8f,Color.rgb(255,224,151),true).apply{
       background=GradientDrawable().apply{
        cornerRadius=dp(7).toFloat()
        setColor(Color.argb(155,95,65,18))
        setStroke(dp(1),Color.rgb(218,174,78))
       }
       setPadding(dp(3),0,dp(3),0)
      },LinearLayout.LayoutParams(-2,dp(14)))
     }
     tile.addView(nameLine,LinearLayout.LayoutParams(-1,dp(16)))
    }else{
     tile.addView(label("اجلس",10f,Color.argb(170,225,235,245),false).apply{
      includeFontPadding=false
     },LinearLayout.LayoutParams(-1,dp(16)))
    }
    if(occupied){
     tile.setOnClickListener{openSeatProfile(uid!!,seat,name,avatarUrl)}
     tile.setOnLongClickListener{
      seatClick(seat,seatNumber,uid,name)
      true
     }
    }else{
     tile.setOnClickListener{if(cameraMode)claimCameraSeat(seatNumber) else claimSeat(seatNumber)}
    }
    line.addView(tile,LinearLayout.LayoutParams(0,dp(96),1f).apply{
     setMargins(dp(3),dp(2),dp(3),dp(2))
     })
   }
   grid.addView(line,LinearLayout.LayoutParams(-1,-2))
  }
 }
 // ===================== Camera rooms: BIGO-style layouts =====================
 // 6 seats: big main camera (right, spans 2 rows) + 2 small seats (left) + 3 seats (bottom row).
 // 4 seats: big main camera (right) + 3 vertical seats (left).
 // All sizes derive from the screen size, so it adapts to any Android device.
 private fun drawCameraSeats(){
  val count=cameraSeatCount.coerceIn(4,6)
  val screen=resources.displayMetrics
  val gridW=(screen.widthPixels-dp(24)).coerceAtLeast(dp(240))
  val totalH=minOf(gridW,(screen.heightPixels*0.43f).toInt()).coerceAtLeast(dp(240))
  val rowH=totalH/3
  val gap=dp(4)
  val seatList=(0 until count).map{cameraSeats.optJSONObject(it)?:JSONObject()}
  // The main (big) camera is the user chosen by the owner; falls back to the first seat.
  var mainIdx=0
  if(cameraMainUserId.isNotBlank()){
   val found=seatList.indexOfFirst{it.optString("user_id")==cameraMainUserId}
   if(found>=0)mainIdx=found
  }
  val others=(0 until count).filter{it!=mainIdx}
  fun lp(w:Int,h:Int,weight:Float)=LinearLayout.LayoutParams(w,h,weight).apply{setMargins(gap,gap,gap,gap)}
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  if(count==4){
   val colW=gridW/4
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
   val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
   others.forEach{i->left.addView(buildCameraTile(seatList[i],i,false,colW,rowH),lp(-1,0,1f))}
   row.addView(left,LinearLayout.LayoutParams(0,-1,1f))
   row.addView(buildCameraTile(seatList[mainIdx],mainIdx,true,colW*3,totalH),lp(0,-1,3f))
   root.addView(row,LinearLayout.LayoutParams(-1,totalH))
  }else{
   val cell=gridW/3
   val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
   val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
   others.take(2).forEach{i->left.addView(buildCameraTile(seatList[i],i,false,cell,rowH),lp(-1,0,1f))}
   top.addView(left,LinearLayout.LayoutParams(0,-1,1f))
   top.addView(buildCameraTile(seatList[mainIdx],mainIdx,true,cell*2,rowH*2),lp(0,-1,2f))
   root.addView(top,LinearLayout.LayoutParams(-1,rowH*2))
   val bottomIds=others.drop(2)
   if(bottomIds.isNotEmpty()){
    val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
    val bw=gridW/bottomIds.size
    bottomIds.forEach{i->bottom.addView(buildCameraTile(seatList[i],i,false,bw,rowH),lp(0,-1,1f))}
    root.addView(bottom,LinearLayout.LayoutParams(-1,rowH))
   }
  }
  grid.addView(root,LinearLayout.LayoutParams(-1,-2))
 }

 private fun buildCameraTile(seat:JSONObject,index:Int,isMain:Boolean,tileW:Int,tileH:Int):FrameLayout{
  val seatNumber=seat.optInt("seat_number",index+1)
  val uid=seat.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}
  val occupied=uid!=null
  val name=seat.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}
   ?:if(occupied)"متحدث" else "مقعد فارغ"
  val seatMuted=seat.optBoolean("is_muted",false)
  val seatClosed=!seat.optBoolean("is_open",true)
  val radius=dp(if(isMain)18 else 14).toFloat()
  val tile=FrameLayout(this).apply{
   isClickable=true
   isFocusable=true
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
    intArrayOf(Color.argb(72,255,255,255),Color.argb(26,255,255,255))).apply{
    cornerRadius=radius
    setStroke(dp(1),if(occupied)Color.argb(125,255,255,255) else Color.argb(60,255,255,255))
   }
   clipToOutline=true
   outlineProvider=object:android.view.ViewOutlineProvider(){
    override fun getOutline(view:View,outline:Outline){outline.setRoundRect(0,0,view.width,view.height,radius)}
   }
  }
  // ---- same data extraction as the audio seats (avatar, VIP, frame) ----
  val avatarUrl=resolveSeatAvatarUrl(seat)
  val seatUser=seat.optJSONObject("user")?:seat.optJSONObject("member")
  val nestedVip=seat.optJSONObject("vip_membership")
   ?:seat.optJSONObject("vip")
   ?:seatUser?.optJSONObject("vip_membership")
   ?:seatUser?.optJSONObject("vip")
   ?:seatUser?.optJSONObject("membership")
  val payloadVipLevel=readActiveVipLevel(nestedVip?:seatUser?:seat)
  val seatVipLevel=when{
   uid==currentUserId && currentUserVipLevel>0->currentUserVipLevel
   seat.has("vip_active") && !seat.optBoolean("vip_active")->0
   payloadVipLevel>0->payloadVipLevel
   else->0
  }
  val seatLevel=(if(seat.has("level"))seat.optInt("level",1) else seat.optInt("user_level",1)).coerceIn(1,100)

  // ---- avatar stage (shown when there is no live video) ----
  val ringPx=(minOf(tileW,tileH)*if(isMain)0.39f else 0.48f).toInt().coerceIn(dp(32),dp(110))
  val stagePx=(ringPx*70f/60f).toInt()
  val innerPx=(ringPx*52f/60f).toInt()
  val avatarInner=FrameLayout(this).apply{
   clipChildren=false
   clipToPadding=false
   if(occupied){
    addView(label(name.take(1).uppercase(),if(isMain)26f else 16f,Color.rgb(205,210,225),true),FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
   }else{
    addView(label(if(seatClosed)"🔒" else "+",if(isMain)34f else 24f,Color.argb(230,255,255,255),true),FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
   }
  }
  if(occupied && avatarUrl.isNotBlank()){
   val photo=ImageView(this).apply{
    scaleType=ImageView.ScaleType.CENTER_CROP
    clipToOutline=true
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
    tag=avatarUrl
   }
   avatarInner.addView(photo,FrameLayout.LayoutParams(-1,-1))
   loadAvatarInto(photo,avatarUrl)
  }
  val stage=FrameLayout(this).apply{
   clipChildren=false
   clipToPadding=false
   val wave=CircularAudioWaveView(this@RoomActivity)
   if(uid!=null){
    speakerWaveViews[uid]=wave
    wave.setLevel(activeSpeakerLevels[uid]?:0f)
   }
   addView(wave,FrameLayout.LayoutParams(-1,-1))
   val ring=FrameLayout(this@RoomActivity).apply{
    clipChildren=false
    clipToPadding=false
    background=GradientDrawable().apply{
     shape=GradientDrawable.OVAL
     if(occupied){
      setColor(Color.argb(40,0,0,0))
      setStroke(dp(2),Color.argb(225,255,255,255))
     }else{
      setColor(Color.argb(40,255,255,255))
      setStroke(dp(1),Color.argb(150,255,255,255),dp(4).toFloat(),dp(3).toFloat())
     }
    }
    addView(avatarInner,FrameLayout.LayoutParams(innerPx,innerPx,Gravity.CENTER))
   }
   addView(ring,FrameLayout.LayoutParams(ringPx,ringPx,Gravity.CENTER))
   val frameUrl=sequenceOf("equipped_frame_url","avatar_frame_url","frame_url","profile_frame_url")
    .map{seat.optString(it)}.firstOrNull{it.startsWith("https://")}
    ?:if(uid==currentUserId)currentUserEquippedFrameUrl else null
   if(occupied && !frameUrl.isNullOrBlank()){
    val frameView=RoyalGifFrame(this@RoomActivity).apply{visibility=View.VISIBLE;tag=frameUrl}
    addView(frameView,FrameLayout.LayoutParams(stagePx,stagePx,Gravity.CENTER))
    seatFrameViews.getOrPut(frameUrl){mutableListOf()}.add(frameView)
    loadSeatFrame(frameView,frameUrl)
   }
  }
  tile.addView(stage,FrameLayout.LayoutParams(stagePx,stagePx,Gravity.CENTER))

  // ---- live camera video fills the whole tile ----
  val liveVideoTrack=if(occupied && uid!=null)videoTrackFor(uid) else null
  if(liveVideoTrack!=null){
   stage.visibility=View.GONE
   val videoRenderer=obtainCameraRenderer(uid!!,liveVideoTrack)
   tile.addView(videoRenderer,0,FrameLayout.LayoutParams(-1,-1))
  }

  // ---- top-right gift-points chip (reads optional fields, defaults to 0) ----
  val points=seat.optInt("gift_points",seat.optInt("score",0))
  val chip=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(5),0,dp(5),0)
   background=GradientDrawable().apply{cornerRadius=dp(9).toFloat();setColor(Color.argb(120,0,0,0))}
   addView(label("$points",10f,Color.WHITE,true).apply{includeFontPadding=false})
   addView(View(this@RoomActivity).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(255,196,40))}
   },LinearLayout.LayoutParams(dp(9),dp(9)).apply{marginStart=dp(3)})
  }
  tile.addView(chip,FrameLayout.LayoutParams(-2,dp(17),Gravity.TOP or Gravity.END).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})

  // ---- muted indicator (top-left) ----
  if(occupied && seatMuted){
   tile.addView(label("🔇",10f,Color.WHITE).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(140,0,0,0))}
   },FrameLayout.LayoutParams(dp(18),dp(18),Gravity.TOP or Gravity.START).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})
  }

  // ---- bottom bar: Host chip, VIP, name, level, seat number ----
  val bar=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(5),dp(10),dp(5),dp(4))
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.TRANSPARENT,Color.argb(170,0,0,0)))
  }
  if(!isMain){
   bar.addView(label("$seatNumber",10f,Color.WHITE,true).apply{
    includeFontPadding=false
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(140,0,0,0))}
   },LinearLayout.LayoutParams(dp(17),dp(17)).apply{marginEnd=dp(4)})
  }
  if(occupied){
   bar.addView(label(name,if(isMain)12f else 10f,if(seatLevel>=100)Color.rgb(255,220,142) else Color.WHITE,true).apply{
    maxLines=1
    ellipsize=android.text.TextUtils.TruncateAt.END
    includeFontPadding=false
    gravity=Gravity.CENTER_VERTICAL or Gravity.START
    setShadowLayer(3f,0f,1f,Color.argb(190,0,0,0))
   },LinearLayout.LayoutParams(0,-2,1f))
   if(seatVipLevel>0){
    bar.addView(label("VIP$seatVipLevel",8f,Color.rgb(60,38,4),true).apply{
     includeFontPadding=false
     setPadding(dp(4),dp(1),dp(4),dp(1))
     background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,226,138),Color.rgb(240,172,60))).apply{cornerRadius=dp(7).toFloat()}
    },LinearLayout.LayoutParams(-2,-2).apply{marginStart=dp(3)})
   }
   if(seatLevel>=100){
    bar.addView(label("♛100",8f,Color.rgb(255,224,151),true).apply{
     includeFontPadding=false
     background=GradientDrawable().apply{cornerRadius=dp(7).toFloat();setColor(Color.argb(155,95,65,18));setStroke(dp(1),Color.rgb(218,174,78))}
     setPadding(dp(3),0,dp(3),0)
    },LinearLayout.LayoutParams(-2,dp(14)).apply{marginStart=dp(3)})
   }
   if(isMain){
    bar.addView(label("المضيف",9f,Color.WHITE,true).apply{
     includeFontPadding=false
     setPadding(dp(6),dp(2),dp(6),dp(2))
     background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(Color.rgb(40,170,110))}
    },LinearLayout.LayoutParams(-2,-2).apply{marginStart=dp(4)})
   }
  }else{
   bar.addView(label(if(seatClosed)"مغلق" else "متاح",if(isMain)12f else 10f,Color.argb(200,225,235,245),false).apply{
    includeFontPadding=false
    gravity=Gravity.CENTER_VERTICAL or Gravity.START
   },LinearLayout.LayoutParams(0,-2,1f))
  }
  tile.addView(bar,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))

  // ---- same interactions as before ----
  if(occupied){
   tile.setOnClickListener{openSeatProfile(uid!!,seat,name,avatarUrl)}
   tile.setOnLongClickListener{seatClick(seat,seatNumber,uid,name);true}
  }else{
   tile.setOnClickListener{claimCameraSeat(seatNumber)}
  }
  return tile
 }
 private fun loadSeatFrame(view:RoyalGifFrame,url:String){
  val cached=seatFrameBytes[url]
  if(cached!=null){view.setGif(cached);view.startAnimation();return}
  if(!seatFrameLoading.add(url))return
  scope.launch{
   try{
    val bytes=withContext(Dispatchers.IO){
     java.net.URL(url).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{it.readBytes()}
    }
    if(bytes.isNotEmpty()){
     seatFrameBytes[url]=bytes
     seatFrameViews[url]?.toList()?.forEach{frame->
      if(frame.tag==url && frame.isAttachedToWindow){
       frame.setGif(bytes)
       frame.startAnimation()
      }
     }
    }
   }catch(e:Exception){
    android.util.Log.e("RoomAvatarFrame","Failed to load equipped frame",e)
   }finally{seatFrameLoading.remove(url)}
  }
 }
 private fun resolveSeatAvatarUrl(seat:JSONObject):String{
  fun first(obj:JSONObject?,keys:Array<String>):String{
   if(obj==null)return ""
   for(key in keys){
    val value=obj.optString(key).trim()
    if(value.isNotEmpty() && value!="null" && value!="undefined")return value
   }
   return ""
  }
  val keys=arrayOf("avatar_url","profile_image","avatar","image_url","photo_url","picture","user_avatar","profile_picture")
  val nested=arrayOf("user","member","profile","account")
  first(seat,keys).takeIf{it.isNotBlank()}?.let{return it}
  for(key in nested){
   val obj=seat.optJSONObject(key)?:continue
   first(obj,keys).takeIf{it.isNotBlank()}?.let{return it}
   first(obj.optJSONObject("profile"),keys).takeIf{it.isNotBlank()}?.let{return it}
  }
  return ""
 }

 private fun applyAvatarBitmapToTree(root:View,url:String,bitmap:Bitmap){
  if(root is ImageView && root.tag==url)root.setImageBitmap(bitmap)
  if(root is android.view.ViewGroup){
   for(index in 0 until root.childCount){
    applyAvatarBitmapToTree(root.getChildAt(index),url,bitmap)
   }
  }
 }

 private fun loadAvatarInto(view:ImageView,url:String){
  if(url.isBlank())return
  val cached=avatarBitmaps[url]
  if(cached!=null){
   view.setImageBitmap(cached)
   return
  }
  // A request already in progress will update every currently attached view
  // with this URL when it finishes, including views created by a seat redraw.
  if(!avatarLoading.add(url))return
  scope.launch{
   try{
    val bitmap=withContext(Dispatchers.IO){
     java.net.URL(url).openConnection().apply{
      connectTimeout=10000
      readTimeout=15000
      useCaches=true
     }.getInputStream().use{BitmapFactory.decodeStream(it)}
    }
    if(bitmap!=null){
     avatarBitmaps[url]=bitmap
     if(::grid.isInitialized)applyAvatarBitmapToTree(grid,url,bitmap)
     if(::memberAvatars.isInitialized)applyAvatarBitmapToTree(memberAvatars,url,bitmap)
     if(::ownerAvatarView.isInitialized && ownerAvatarView.tag==url)ownerAvatarView.setImageBitmap(bitmap)
     if(view.tag==url)view.setImageBitmap(bitmap)
    }
   }catch(_:Exception){}finally{avatarLoading.remove(url)}
  }
 }
 private fun openSeatProfile(uid:String,seat:JSONObject,fallbackName:String,fallbackAvatar:String){
  if(uid.isBlank()||uid=="null")return
  val loading=AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert)
   .setMessage("جارٍ تحميل الملف الشخصي...")
   .setNegativeButton("إغلاق",null).create()
  loading.show()
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){AuthApi.get("/api/users/$uid",authToken)}
    if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحميل الملف الشخصي"))
    val user=response.body.optJSONObject("user")
     ?:response.body.optJSONObject("profile")
     ?:response.body
    val merged=JSONObject(seat.toString())
    merged.put("user_id",uid)
    val keys=listOf(
     "display_name","username","public_id","account_id","avatar_url","profile_image",
     "total_xp","level","user_level","bio",
     "fans_count","fans","followers_count","followersCount",
     "following_count","following","follow_count","followings_count",
     "diamonds_sent","total_diamonds_sent","sent_diamonds","total_sent",
     "diamonds_received","total_diamonds_received","received_diamonds","total_received"
    )
    for(key in keys){
     if(user.has(key)&&user.opt(key)!=JSONObject.NULL)merged.put(key,user.opt(key))
    }
    val stats=user.optJSONObject("stats")
     ?:user.optJSONObject("statistics")
     ?:user.optJSONObject("profile_stats")
    if(stats!=null){
     for(key in keys)if(stats.has(key)&&stats.opt(key)!=JSONObject.NULL)merged.put(key,stats.opt(key))
    }
    if(merged.optString("display_name").isBlank()||merged.optString("display_name")=="null")
     merged.put("display_name",fallbackName)
    if(merged.optString("avatar_url").isBlank()||merged.optString("avatar_url")=="null")
     merged.put("avatar_url",fallbackAvatar)
    if(merged.optString("public_id").isBlank()||merged.optString("public_id")=="null"){
     val publicId=seat.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}
      ?:seat.optString("account_id").takeIf{it.isNotBlank()&&it!="null"}
     if(publicId!=null)merged.put("public_id",publicId)
    }
    loading.dismiss()
    renderMemberProfile(merged)
   }catch(e:Exception){
    loading.dismiss()
    Toast.makeText(this@RoomActivity,e.message?:"تعذر تحميل الملف الشخصي",Toast.LENGTH_SHORT).show()
   }
  }
 }
 private fun seatClick(s:JSONObject,n:Int,uid:String?,name:String){
  if(roomMode=="camera"){cameraSeatClick(s,n,uid,name);return}
  if(uid==null){claimSeat(n);return}
  if(uid==currentUserId){
   showActionSheet("إدارة مقعدك",listOf(if(microphoneEnabled)"كتم صوت المايك" else "تشغيل المايك","مغادرة المقعد")){which->
    if(which==0)toggleMic() else leaveSeat(n)
   }
   return
  }
  if(!isOwner){
   showMemberProfile(JSONObject(s.toString()).put("user_id",uid).put("display_name",name))
   return
  }
  val muted=s.optBoolean("is_muted")
  showActionSheet(name,listOf(if(muted)"إلغاء الكتم" else "كتم المتحدث","إزالة من المقعد")){which->
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
   }
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
 private fun cameraSeatClick(s:JSONObject,n:Int,uid:String?,name:String){
  if(uid==null){claimCameraSeat(n);return}
  val muted=s.optBoolean("is_muted",false)
  val isOpen=s.optBoolean("is_open",true)
  if(uid==currentUserId){
   showActionSheet("إدارة مقعد الكاميرا",listOf(if(microphoneEnabled)"كتم صوت المايك" else "تشغيل المايك","إيقاف الكاميرا ومغادرة المقعد")){which->
    if(which==0)toggleMic() else leaveCameraSeat(n)
   }
   return
  }
  if(!isOwner){
   showMemberProfile(JSONObject(s.toString()).put("user_id",uid).put("display_name",name))
   return
  }
  showActionSheet(name,listOf(if(muted)"إلغاء كتم المايك" else "كتم المايك", "إزالة من مقعد الكاميرا", if(isOpen)"إغلاق المقعد" else "فتح المقعد", "تعيين كاميرته رئيسية")){which->
   scope.launch{
    try{
     val result=withContext(Dispatchers.IO){when(which){
      0->AuthApi.post("/api/voice-rooms/$roomId/camera-seats/$n/mute",JSONObject().put("muted",!muted),authToken)
      1->AuthApi.delete("/api/voice-rooms/$roomId/camera-seats/$n",authToken)
      2->AuthApi.put("/api/voice-rooms/$roomId/camera-seats/$n/open",JSONObject().put("isOpen",!isOpen),authToken)
      else->AuthApi.put("/api/voice-rooms/$roomId/camera-main",JSONObject().put("userId",uid),authToken)
     }}
     if(result.status in 200..299){loadSeats();Toast.makeText(this@RoomActivity,"تم تحديث مقعد الكاميرا",Toast.LENGTH_SHORT).show()}
     else Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر تنفيذ العملية"),Toast.LENGTH_SHORT).show()
    }catch(_:Exception){Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}
   }
  }
 }
 private fun claimCameraSeat(n:Int){
  if(!connected){Toast.makeText(this,"انتظر اتصال الغرفة ثم حاول مجدداً",Toast.LENGTH_SHORT).show();return}
  scope.launch{
   try{
    val r=withContext(Dispatchers.IO){AuthApi.post("/api/voice-rooms/$roomId/camera-seats/$n/claim",JSONObject(),authToken)}
    if(r.status in 200..299){
     try{room.disconnect()}catch(_:Exception){}
     connected=false;isSeated=true;cameraEnabled=false
     connect()
     Toast.makeText(this@RoomActivity,"تم حجز مقعد الكاميرا $n",Toast.LENGTH_SHORT).show()
    }else Toast.makeText(this@RoomActivity,r.body.optString("error","تعذر حجز مقعد الكاميرا"),Toast.LENGTH_SHORT).show()
   }catch(_:Exception){Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}
  }
 }
 private fun leaveCameraSeat(n:Int){
  scope.launch{
   try{
    if(cameraEnabled){try{room.localParticipant.setCameraEnabled(false)}catch(_:Exception){};cameraEnabled=false}
    val result=withContext(Dispatchers.IO){AuthApi.delete("/api/voice-rooms/$roomId/camera-seats/$n",authToken)}
    if(result.status in 200..299){
     try{room.disconnect()}catch(_:Exception){}
     connected=false;isSeated=false;canPublishVideo=false
     loadSeats();connect()
    }else Toast.makeText(this@RoomActivity,result.body.optString("error","تعذر مغادرة مقعد الكاميرا"),Toast.LENGTH_SHORT).show()
   }catch(_:Exception){Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}
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
    canPublishVideo=r.body.optBoolean("canPublishVideo",false)
    cameraEnabled=false
    refreshCameraControl()
    microphoneEnabled=r.body.optBoolean("canPublish",false)
    room.localParticipant.setMicrophoneEnabled(microphoneEnabled)
    connected=true
    status.text=if(isSeated && microphoneEnabled) "متصل • أنت على المقعد والمايك جاهز" else "متصل كمستمع • اضغط على مقعد فارغ للجلوس"
    loadSeats()
    startRoomMembershipWatch()
    startRoomGiftWatch()
    startTrendRefresh()
    startTrendHeartbeat()
   } catch(e:Exception) {
    status.text=e.message?:"تعذر الاتصال"
   }
  }
 }
 private fun startSpeakerTracking(){
  speakerEventsJob?.cancel()
  speakerEventsJob=scope.launch{
   try{
    room.events.collect{event->
     when(event){
      is RoomEvent.TrackSubscribed->{
       val identity=event.participant.identity?.value
       val video=event.track as? VideoTrack
       if(identity!=null && video!=null && event.publication.source==Track.Source.CAMERA){cameraTracks[identity]=video;drawSeats()}
      }
      is RoomEvent.TrackUnsubscribed->{
       val identity=event.participant.identity?.value
       if(identity!=null && event.track is VideoTrack){cameraTracks.remove(identity);drawSeats()}
      }
      is RoomEvent.ActiveSpeakersChanged->{
      val fresh=HashMap<String,Float>()
      for(participant in event.speakers){
       val identity=participant.identity?.value ?: continue
       if(participant.isSpeaking){
        fresh[identity]=participant.audioLevel.toFloat().coerceIn(0f,1f).coerceAtLeast(0.18f)
       }
      }
      activeSpeakerLevels.clear()
      activeSpeakerLevels.putAll(fresh)
      for((userId,wave) in speakerWaveViews){wave.setLevel(activeSpeakerLevels[userId]?:0f)}
      }
      else->{ /* Ignore unrelated LiveKit room events. */ }
     }
    }
   }catch(_:CancellationException){}
   catch(_:Exception){}
  }
 }
 private var membershipWatch:Job?=null
 private fun showRoomJoinToast(notice:RoomJoinNotice){
  if(!::roomOverlayLayer.isInitialized || isFinishing || leaving)return
  pendingJoinToasts.add(notice)
  if(!joinToastShowing)showNextRoomJoinToast()
 }
 private fun showNextRoomJoinToast(){
  if(joinToastShowing || pendingJoinToasts.isEmpty() || isFinishing || leaving)return
  if(!::roomOverlayLayer.isInitialized || !::grid.isInitialized)return
  joinToastShowing=true
  val notice=pendingJoinToasts.removeAt(0)
  val isLevel100=notice.level>=100
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_RTL
   setPadding(dp(10),dp(8),dp(12),dp(8))
   background=if(isLevel100) GradientDrawable(
    GradientDrawable.Orientation.LEFT_RIGHT,
    intArrayOf(Color.rgb(51,51,51),Color.rgb(31,25,40),Color.rgb(79,53,21))
   ).apply{cornerRadius=dp(17).toFloat();setStroke(dp(1),Color.rgb(231,190,96))}
   else GradientDrawable(
    GradientDrawable.Orientation.LEFT_RIGHT,
    intArrayOf(Color.argb(225,94,94,94),Color.argb(225,255,64,129))
   ).apply{cornerRadius=dp(26).toFloat();setStroke(dp(1),Color.argb(120,255,255,255))}
   elevation=dp(if(isLevel100)7 else 3).toFloat()
   alpha=0f
   translationX=-(resources.displayMetrics.widthPixels.toFloat())
   contentDescription="${notice.name} انضم إلى الغرفة، المستوى ${notice.level}"
  }
  val avatar=ImageView(this).apply{
   scaleType=ImageView.ScaleType.CENTER_CROP
   background=GradientDrawable().apply{
    shape=GradientDrawable.OVAL
    setColor(Color.rgb(48,48,48))
    setStroke(dp(if(isLevel100)2 else 1),if(isLevel100)Color.rgb(244,205,115) else Color.argb(120,255,255,255))
   }
   clipToOutline=true
   if(notice.avatarUrl.isNotBlank()){tag=notice.avatarUrl;loadAvatarInto(this,notice.avatarUrl)}
   else setImageDrawable(VectorIcon("user",Color.LTGRAY,dp(20)))
  }
  card.addView(avatar,LinearLayout.LayoutParams(dp(38),dp(38)).apply{marginEnd=dp(8)})
  val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL}
  val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  line.addView(TextView(this).apply{
   text=notice.name;textSize=12.5f;maxLines=1
   ellipsize=android.text.TextUtils.TruncateAt.END
   setTextColor(if(isLevel100)Color.rgb(255,224,160) else Color.WHITE)
   typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
  },LinearLayout.LayoutParams(0,dp(21),1f))
  line.addView(TextView(this).apply{
   text="LV ${notice.level}";textSize=9.5f;gravity=Gravity.CENTER
   setTextColor(if(isLevel100)Color.rgb(255,228,166) else Color.rgb(224,224,224))
   typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
   setPadding(dp(7),dp(3),dp(7),dp(3))
   background=GradientDrawable().apply{
    cornerRadius=dp(9).toFloat()
    setColor(if(isLevel100)Color.argb(105,214,164,57) else Color.argb(85,130,130,130))
    setStroke(dp(1),if(isLevel100)Color.rgb(230,187,93) else Color.argb(110,202,202,202))
   }
  },LinearLayout.LayoutParams(-2,dp(21)))
  info.addView(line)
  info.addView(TextView(this).apply{
   text=if(isLevel100)"انضم إلى الغرفة" else "دخل الغرفة"
   textSize=10f
   setTextColor(if(isLevel100)Color.rgb(244,211,153) else Color.rgb(210,204,222))
   setPadding(0,dp(1),0,0)
  })
  card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
  val width=resources.displayMetrics.widthPixels-dp(42)
  val params=FrameLayout.LayoutParams(width,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{
   marginStart=dp(21);marginEnd=dp(21)
  }
  roomOverlayLayer.addView(card,params)
  // Anchor the flying banner just below the seats, not over the chat messages.
  roomOverlayLayer.post{
   if(card.parent==null)return@post
   val seatPos=IntArray(2);val overlayPos=IntArray(2)
   grid.getLocationOnScreen(seatPos);roomOverlayLayer.getLocationOnScreen(overlayPos)
   params.topMargin=(seatPos[1]-overlayPos[1]+(grid.height*grid.scaleY).toInt()+dp(7)).coerceAtLeast(dp(110))
   card.layoutParams=params
   card.animate().alpha(1f).translationX(0f).setDuration(430)
    .setInterpolator(android.view.animation.DecelerateInterpolator(1.4f))
    .withEndAction{
     card.postDelayed({
      if(card.parent==null)return@postDelayed
      card.animate().alpha(0f).translationX(-width.toFloat()).setDuration(430)
       .setInterpolator(android.view.animation.AccelerateInterpolator())
       .withEndAction{
        roomOverlayLayer.removeView(card)
        joinToastShowing=false
        showNextRoomJoinToast()
       }.start()
     },1500)
    }.start()
  }
 }
 private fun showRoomGiftPicker(){
  if(roomId.isBlank() || authToken.isBlank()){Toast.makeText(this,"تعذر التحقق من الغرفة أو تسجيل الدخول",Toast.LENGTH_SHORT).show();return}
  val recipients=(0 until roomMembers.length()).mapNotNull{roomMembers.optJSONObject(it)}
   .filter{it.optString("user_id").isNotBlank() && it.optString("user_id")!="null" && it.optString("user_id")!=currentUserId}
  if(recipients.isEmpty()){loadRoomMembers();Toast.makeText(this,"لا يوجد أعضاء آخرون في الغرفة حالياً",Toast.LENGTH_SHORT).show();return}
  val names=recipients.map{it.optString("display_name").ifBlank{it.optString("username","مستخدم")}}.toTypedArray()
  showActionSheet("إلى من ترسل الهدية؟",names.toList()){which->
   val selected=recipients[which];loadGiftCatalogAndChoose(selected.optString("user_id"),names[which])
  }
 }
 private fun loadGiftCatalogAndChoose(recipientId:String,recipientName:String){
  scope.launch{try{
   val response=withContext(Dispatchers.IO){giftRepository.loadCatalog(authToken)}
   if(response.status !in 200..299){Toast.makeText(this@RoomActivity,response.body.optString("error","تعذر تحميل الهدايا"),Toast.LENGTH_LONG).show();return@launch}
   val gifts=giftRepository.getGifts()
   if(gifts.isEmpty()){Toast.makeText(this@RoomActivity,"لا توجد هدايا متاحة حالياً",Toast.LENGTH_SHORT).show();return@launch}
   val walletResult=withContext(Dispatchers.IO){AuthApi.get("/api/wallet",authToken)}
   val wallet=if(walletResult.status in 200..299) walletResult.body.optJSONObject("wallet")?:walletResult.body else null
   val coins=wallet?.optLong("recharge_coins")
   showGiftCatalogDialog(gifts,recipientId,recipientName,coins)
  }catch(_:Exception){Toast.makeText(this@RoomActivity,"تعذر الاتصال بخادم الهدايا",Toast.LENGTH_LONG).show()}}
 }
 private fun giftArtwork(gift:Gift):Int=when(gift.id.lowercase()){
  "butterfly","crystal_butterfly"->R.drawable.gift_crystal_butterfly
  "rose","golden_rose","rose_bouquet"->R.drawable.gift_rose_bouquet
  "donut"->R.drawable.gift_donut
  "magic_star"->R.drawable.gift_magic_star
  "pink_kiss"->R.drawable.gift_pink_kiss
  "magic_lantern"->R.drawable.gift_magic_lantern
  "ruby_ring"->R.drawable.gift_ruby_ring
  "royal_crown"->R.drawable.gift_royal_crown
  else->0
 }
 private fun showGiftCatalogDialog(gifts:List<Gift>,recipientId:String,recipientName:String,coins:Long?){
  val ink=Color.argb(30,255,255,255)
  val purple=Color.rgb(255,64,129)
  val muted=Color.rgb(160,151,170)
  val gold=Color.rgb(255,196,49)
  val root=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(14),dp(10),dp(14),dp(14))
   background=sheetBg()
   addView(View(this@RoomActivity).apply{
    background=GradientDrawable().apply{cornerRadius=dp(3).toFloat();setColor(Color.argb(90,255,255,255))}
   },LinearLayout.LayoutParams(dp(40),dp(4)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(8)})
  }
  val heading=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  heading.addView(label("هدايا الغرفة",16f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(34),1f))
  heading.addView(label("إلى "+recipientName,11f,muted),LinearLayout.LayoutParams(-2,dp(34)))
  root.addView(heading)
  val tabs=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR;setPadding(0,dp(4),0,dp(8))}
  val all=TextView(this).apply{text="ALL";textSize=11f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(25,25,25));setStroke(dp(2),purple)}}
  tabs.addView(all,LinearLayout.LayoutParams(dp(42),dp(42)).apply{marginEnd=dp(10)})
  tabs.addView(label("الكل",12f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(42),1f))
  tabs.addView(label("🪙  "+(coins?.let{String.format(java.util.Locale.US,"%,d",it)}?:"—"),12f,gold,true),LinearLayout.LayoutParams(-2,dp(42)))
  root.addView(tabs)
  val grid=android.widget.GridLayout(this).apply{columnCount=4;useDefaultMargins=false;alignmentMode=android.widget.GridLayout.ALIGN_BOUNDS}
  var selected=gifts.first()
  var quantity=1
  val total=label(String.format(java.util.Locale.US,"%,d",selected.price)+ " 🪙",12f,gold,true).apply{gravity=Gravity.END or Gravity.CENTER_VERTICAL}
  val cards=ArrayList<LinearLayout>()
  val giftImages=ArrayList<ImageView>()
  val updateSelection:(Gift)->Unit={chosen->
   selected=chosen
   cards.forEachIndexed{index,card->
    val active=gifts[index].id==chosen.id
    card.background=GradientDrawable().apply{setColor(if(active)Color.argb(80,255,64,129) else ink);cornerRadius=dp(14).toFloat();if(active)setStroke(dp(2),purple) else setStroke(dp(1),Color.argb(30,255,255,255))}
    val art=giftImages.getOrNull(index)
    art?.animate()?.cancel()
    if(active && art!=null){
     art.animate().scaleX(1.12f).scaleY(1.12f).rotation(-4f).setDuration(150)
      .withEndAction{art.animate().scaleX(1f).scaleY(1f).rotation(0f).setDuration(180).start()}.start()
     card.animate().scaleX(1.025f).scaleY(1.025f).setDuration(150)
      .withEndAction{card.animate().scaleX(1f).scaleY(1f).setDuration(180).start()}.start()
    }else{
     art?.animate()?.scaleX(1f)?.scaleY(1f)?.rotation(0f)?.setDuration(120)?.start()
     card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
    }
   }
   total.text=String.format(java.util.Locale.US,"%,d",selected.price*quantity)+" 🪙"
  }
  gifts.forEachIndexed{index,gift->
   val card=LinearLayout(this).apply{
    orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(3),dp(5),dp(3),dp(4))
    background=GradientDrawable().apply{setColor(if(index==0)Color.argb(80,255,64,129) else ink);cornerRadius=dp(14).toFloat();setStroke(dp(if(index==0)2 else 1),if(index==0)purple else Color.argb(30,255,255,255))}
    isClickable=true;isFocusable=true
   }
   val image=ImageView(this).apply{
    val art=giftArtwork(gift)
    if(art!=0)setImageResource(art) else setImageDrawable(VectorIcon("gift",Color.rgb(222,222,222),dp(42)))
    scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=gift.name
   }
   giftImages.add(image)
   card.addView(image,LinearLayout.LayoutParams(-1,dp(76)))
   card.addView(label(gift.name,11f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(24)))
   val price=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
   price.addView(label(String.format(java.util.Locale.US,"%,d",gift.price),10f,muted),LinearLayout.LayoutParams(-2,dp(18)))
   price.addView(label(" 🪙",10f,gold,true),LinearLayout.LayoutParams(-2,dp(18)))
   card.addView(price)
   card.setOnClickListener{updateSelection(gift)}
   cards.add(card)
   val params=android.widget.GridLayout.LayoutParams().apply{width=0;height=dp(132);columnSpec=android.widget.GridLayout.spec(index%4,1f);rowSpec=android.widget.GridLayout.spec(index/4);setMargins(dp(2),dp(3),dp(2),dp(3))}
   grid.addView(card,params)
  }
  val scroll=ScrollView(this).apply{isFillViewport=false;overScrollMode=View.OVER_SCROLL_NEVER;addView(grid,android.view.ViewGroup.LayoutParams(-1,-2))}
  root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  root.addView(View(this).apply{setBackgroundColor(Color.argb(30,255,255,255))},LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(8);bottomMargin=dp(8)})
  val footer=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val send=TextView(this).apply{text="إرسال";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD;background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,64,129),Color.rgb(255,140,60))).apply{cornerRadius=dp(22).toFloat()}}
  footer.addView(send,LinearLayout.LayoutParams(dp(92),dp(44)).apply{marginEnd=dp(8)})
  val qtyBox=LinearLayout(this).apply{gravity=Gravity.CENTER;background=GradientDrawable().apply{setColor(Color.argb(40,255,255,255));cornerRadius=dp(22).toFloat();setStroke(dp(1),Color.argb(90,255,255,255))}}
  val minus=TextView(this).apply{text="−";textSize=21f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)}
  val qtyText=TextView(this).apply{text="1";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD}
  val plus=TextView(this).apply{text="+";textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)}
  qtyBox.addView(minus,LinearLayout.LayoutParams(dp(34),dp(42)));qtyBox.addView(qtyText,LinearLayout.LayoutParams(dp(28),dp(42)));qtyBox.addView(plus,LinearLayout.LayoutParams(dp(34),dp(42)))
  footer.addView(qtyBox,LinearLayout.LayoutParams(-2,dp(44)))
  footer.addView(total,LinearLayout.LayoutParams(0,dp(44),1f).apply{marginStart=dp(8)})
  fun refreshTotal(){total.text=String.format(java.util.Locale.US,"%,d",selected.price*quantity)+" 🪙";qtyText.text=quantity.toString()}
  minus.setOnClickListener{if(quantity>1){quantity--;refreshTotal()}}
  plus.setOnClickListener{if(quantity<99){quantity++;refreshTotal()}}
  send.setOnClickListener{confirmGiftSend(selected,recipientId,recipientName,quantity)}
  root.addView(footer)
  val dialog=AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(root).create()
  dialog.show()
  dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
  dialog.window?.setLayout(-1,(resources.displayMetrics.heightPixels*0.60f).toInt())
  dialog.window?.setGravity(Gravity.BOTTOM)
  dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
  val lp=dialog.window?.attributes
  if(lp!=null){lp.dimAmount=0.62f;dialog.window?.attributes=lp}
 }
 private fun showGiftQuantityDialog(gift:Gift,recipientId:String,recipientName:String){
  val quantityInput=EditText(this).apply{inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText("1");gravity=Gravity.CENTER;textSize=20f;setTextColor(Color.WHITE);setSelectAllOnFocus(true);filters=arrayOf(android.text.InputFilter.LengthFilter(2));background=GradientDrawable().apply{cornerRadius=dp(12).toFloat();setColor(Color.rgb(49,49,49));setStroke(dp(1),Color.rgb(114,114,114))}}
  val wrapper=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(8),dp(22),dp(4));addView(label("الهدية: ${gift.name}",14f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(32)));addView(label("السعر: ${gift.price} عملة للواحدة",12f,Color.rgb(255,213,145)),LinearLayout.LayoutParams(-1,dp(28)));addView(quantityInput,LinearLayout.LayoutParams(-1,dp(52)))}
  AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("كمية الهدية").setView(wrapper).setPositiveButton("متابعة"){_,_->val quantity=quantityInput.text.toString().toIntOrNull()?:0;if(quantity !in 1..99)Toast.makeText(this,"اختر كمية من 1 إلى 99",Toast.LENGTH_SHORT).show() else confirmGiftSend(gift,recipientId,recipientName,quantity)}.setNegativeButton("إلغاء",null).show()
 }
 private fun confirmGiftSend(gift:Gift,recipientId:String,recipientName:String,quantity:Int){
  val total=gift.price*quantity
  showChoiceSheet("تأكيد إرسال الهدية","الهدية: ${gift.name}\nالمستلم: $recipientName\nالكمية: $quantity\nالإجمالي: $total عملة\n\nهل تريد الإرسال؟",listOf(
   SheetOption("إرسال",0){sendRoomGift(gift,recipientId,quantity)},
   SheetOption("رجوع",2){}))
 }
 private fun sendRoomGift(gift:Gift,recipientId:String,quantity:Int){
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){giftRepository.sendGift(roomId,recipientId,gift.id,quantity,authToken)}
    if(response.status in 200..299){
     // Gift is committed before the API responds; fetch the authoritative room score now.
     loadTrendScore()
     Toast.makeText(this@RoomActivity,"تم إرسال ${gift.name} × $quantity بنجاح",Toast.LENGTH_LONG).show()
    }else Toast.makeText(this@RoomActivity,response.body.optString("error","تعذر إرسال الهدية"),Toast.LENGTH_LONG).show()
   }catch(_:Exception){
    Toast.makeText(this@RoomActivity,"تعذر الاتصال بالخادم، لم نؤكد إرسال الهدية",Toast.LENGTH_LONG).show()
   }
  }
 }
 /** Poll the authenticated room gift feed and animate only newly committed events. */
 private fun startRoomGiftWatch(){
  giftPoll?.cancel()
  giftFeedInitialized=false
  lastGiftEventId=0L
  giftPoll=scope.launch{
   while(isActive && connected && !leaving){
    try{
     val (response,events)=withContext(Dispatchers.IO){
      giftRepository.loadRoomEvents(roomId,lastGiftEventId,authToken)
     }
     if(response.status in 200..299){
      // The gift feed also carries the live decaying score, so every room
      // member sees the 5-second cooldown without an extra polling request.
      applyAuthoritativeTrendScore(response.body.optLong("trend_points",trendPoints))
      if(!giftFeedInitialized){
       // Establish a cursor without replaying gifts sent before this viewer joined.
       lastGiftEventId=events.maxOfOrNull{it.id}?:lastGiftEventId
       giftFeedInitialized=true
      }else{
       var receivedNewGift=false
       events.sortedBy{it.id}.forEach{event->
        if(event.id>lastGiftEventId){
         lastGiftEventId=event.id
         receivedNewGift=true
         showReceivedGiftAnimation(event)
        }
       }
       // Gift feed confirms a committed gift; refresh the room score for every viewer.
       if(receivedNewGift)loadTrendScore()
      }
     }
    }catch(_:CancellationException){break}
     catch(_:Exception){/* Retry next interval; room audio remains independent. */}
    delay(2200)
   }
  }
 }

 /** Native animated overlay using artwork already included in this app. */
 private fun showReceivedGiftAnimation(event:GiftRepository.GiftEvent){
  if(!::roomOverlayLayer.isInitialized || isFinishing || isDestroyed)return
  playGiftSound()
  val imageRes=when(event.giftId.lowercase()){
   "butterfly","crystal_butterfly"->R.drawable.gift_crystal_butterfly
   "rose","rose_bouquet"->R.drawable.gift_rose_bouquet
   "donut"->R.drawable.gift_donut
   "magic_star"->R.drawable.gift_magic_star
   "pink_kiss"->R.drawable.gift_pink_kiss
   "magic_lantern"->R.drawable.gift_magic_lantern
   "ruby_ring"->R.drawable.gift_ruby_ring
   "royal_crown"->R.drawable.gift_royal_crown
   else->0
  }
  val animationUrl=event.animationUrl.trim()
  val isSvga=animationUrl.substringBefore("?").endsWith(".svga",true)
  val animationView=if(animationUrl.isNotBlank() && !isSvga && animationUrl.substringBefore("?").endsWith(".json",true)){
   LottieAnimationView(this).apply{
    scaleType=ImageView.ScaleType.FIT_CENTER
    contentDescription="هدية ${event.name}"
    repeatCount=0
    setAnimationFromUrl(AuthApi.absoluteUrl(animationUrl))
    playAnimation()
    alpha=0f
   }
  }else null
  if(animationView!=null){
   roomOverlayLayer.addView(animationView,FrameLayout.LayoutParams(dp(280),dp(280),Gravity.CENTER))
   animationView.animate().alpha(1f).setDuration(220).start()
  }
  if(isSvga) showSvgaGiftOverlay(animationUrl,event.name)
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   layoutDirection=View.LAYOUT_DIRECTION_LTR
   setPadding(dp(16),dp(12),dp(16),dp(12))
   background=GradientDrawable().apply{
    cornerRadius=dp(22).toFloat()
    setColor(Color.argb(238,28,24,39))
    setStroke(dp(1),Color.rgb(226,190,105))
   }
   alpha=0f
   scaleX=0.72f
   scaleY=0.72f
   elevation=dp(12).toFloat()
  }
  val icon=ImageView(this).apply{
   if(imageRes!=0)setImageResource(imageRes) else setImageDrawable(VectorIcon("gift",Color.WHITE,dp(34)))
   scaleType=ImageView.ScaleType.FIT_CENTER
  }
  card.addView(icon,LinearLayout.LayoutParams(dp(64),dp(64)).apply{marginEnd=dp(12)})
  val details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL}
  details.addView(label("🎁 ${event.senderName} أرسل ${event.name}",15f,Color.WHITE,true))
  details.addView(label("إلى ${event.recipientName}  •  ×${event.quantity}",12f,Color.rgb(255,218,151),true))
  card.addView(details,LinearLayout.LayoutParams(0,-2,1f))
  val params=FrameLayout.LayoutParams((resources.displayMetrics.widthPixels-dp(36)).coerceAtLeast(dp(240)),-2,Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{
   bottomMargin=dp(105)
  }
  roomOverlayLayer.addView(card,params)
  // Native fallback motion when no licensed SVGA/Lottie animation is configured.
  // This animates the selected gift artwork itself without downloading third-party assets.
  icon.alpha=0f
  icon.scaleX=0.35f
  icon.scaleY=0.35f
  icon.rotation=-18f
  icon.translationY=dp(18).toFloat()
  icon.animate().alpha(1f).scaleX(1.16f).scaleY(1.16f).rotation(8f)
   .translationY(0f).setDuration(420)
   .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
   .withEndAction{icon.animate().scaleX(1f).scaleY(1f).rotation(0f).setDuration(220).start()}
   .start()
  card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360)
   .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
   .withEndAction{
    card.postDelayed({
     if(card.parent==null)return@postDelayed
     card.animate().alpha(0f).scaleX(0.82f).scaleY(0.82f).setDuration(260)
      .withEndAction{roomOverlayLayer.removeView(card)}.start()
     animationView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction{
      if(animationView.parent===roomOverlayLayer)roomOverlayLayer.removeView(animationView)
     }?.start()
    },2600)
   }.start()
 }
 
 /** Plays a server-hosted SVGA gift above the room without blocking room audio. */
 private fun playGiftSound(){
  try{
   giftSound?.stop()
   val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
   giftSound=RingtoneManager.getRingtone(applicationContext,uri)?.also{tone->if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P)tone.volume=0.72f;tone.play()}
  }catch(_:Exception){}
 }
 private fun loadGiftSvgaPreview(view:SVGAImageView,path:String){
  val absolute=AuthApi.absoluteUrl(path)
  scope.launch{try{
   val bytes=withContext(Dispatchers.IO){URL(absolute).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{it.readBytes()}}
   if(bytes.isEmpty())return@launch
   SVGAParser(this@RoomActivity).decodeFromInputStream(ByteArrayInputStream(bytes),"gift-preview-${absolute.hashCode()}",object:SVGAParser.ParseCompletion{
    override fun onComplete(videoItem:SVGAVideoEntity){view.post{if(view.parent!=null){view.setVideoItem(videoItem);view.startAnimation()}}}
    override fun onError(){}
   },true)
  }catch(_:Exception){}}
 }
  private fun showSvgaGiftOverlay(path:String,giftName:String){
  if(!::roomOverlayLayer.isInitialized || isFinishing || isDestroyed)return
  val player=SVGAImageView(this).apply{
   loops=1
   clearsAfterDetached=true
   setBackgroundColor(Color.TRANSPARENT)
   contentDescription="هدية متحركة $giftName"
  }
  val size=(resources.displayMetrics.widthPixels*0.88f).toInt().coerceAtMost(dp(440))
  val height=(resources.displayMetrics.heightPixels*0.58f).toInt().coerceAtMost(dp(520))
  player.scaleType=ImageView.ScaleType.FIT_CENTER
  roomOverlayLayer.addView(player,FrameLayout.LayoutParams(size,height,Gravity.CENTER))
  val absolute=AuthApi.absoluteUrl(path)
  scope.launch{
   try{
    val bytes=withContext(Dispatchers.IO){
     val connection=URL(absolute).openConnection().apply{
      connectTimeout=12000
      readTimeout=20000
      setRequestProperty("Accept","application/octet-stream")
     }
     connection.getInputStream().use{it.readBytes()}
    }
    if(bytes.isEmpty())throw IllegalStateException("ملف SVGA فارغ")
    val parser=SVGAParser(this@RoomActivity)
    player.callback=object:SVGACallback{
     override fun onFinished(){player.post{if(player.parent===roomOverlayLayer)roomOverlayLayer.removeView(player)}}
     override fun onPause()=Unit
     override fun onRepeat()=Unit
     override fun onStep(frame:Int,percentage:Double)=Unit
    }
    parser.decodeFromInputStream(
     ByteArrayInputStream(bytes),
     "bak-live-gift-${absolute.hashCode()}",
     object:SVGAParser.ParseCompletion{
      override fun onComplete(videoItem:SVGAVideoEntity){
       player.post{
        if(player.parent===roomOverlayLayer){
         player.setVideoItem(videoItem)
         player.startAnimation()
         player.alpha=0f
         player.animate().alpha(1f).setDuration(180).start()
        }
       }
      }
      override fun onError(){player.post{if(player.parent===roomOverlayLayer)roomOverlayLayer.removeView(player)}}
     },
     true
    )
   }catch(_:CancellationException){
    if(player.parent===roomOverlayLayer)roomOverlayLayer.removeView(player)
   }catch(_:Exception){    if(player.parent===roomOverlayLayer)roomOverlayLayer.removeView(player)
   }
  }
 }

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
     val myLevel=me?.optInt("level",currentUserLevel)?:currentUserLevel
     val myNotice=RoomJoinNotice(myName,myLevel.coerceIn(1,100),currentUserId,me?.optString("avatar_url").orEmpty(),vipLevel=(me?.optInt("vip_level",0)?:0).coerceIn(0,12))
     joinNotices.add(myNotice)
     // Show the entrance banner for the current user too; initial membership
     // is the first observed state, so it must not be treated as a silent baseline.
     showRoomJoinToast(myNotice)
    }
    if(previous!=null){
     for(i in 0 until fresh.length()){
      val member=fresh.optJSONObject(i)?:continue
      val id=member.optString("user_id")
      if(id.isNotBlank() && id!="null" && !previous.contains(id)){
       val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
       val nested=member.optJSONObject("user")?:member.optJSONObject("profile")
       val level=(if(member.has("level"))member.optInt("level",1) else
        if(member.has("user_level"))member.optInt("user_level",1) else nested?.optInt("level",1)?:1).coerceIn(1,100)
       val avatarUrl=member.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}
        ?:nested?.optString("avatar_url").orEmpty()
       val notice=RoomJoinNotice(name,level,id,avatarUrl,vipLevel=(if(member.has("vip_level"))member.optInt("vip_level",0) else nested?.optInt("vip_level",0)?:0).coerceIn(0,12))
       joinNotices.add(notice)
       showRoomJoinToast(notice)
      }
     }
     while(joinNotices.size>12)joinNotices.removeAt(0)
    }
    previousMemberIds=current
    roomMembers=fresh
    if(::membersButton.isInitialized){
     val count=fresh.length()
     (membersButton.getChildAt(0) as? TextView)?.text=count.toString()
     if(::memberAvatars.isInitialized){
      for(index in 0 until memberAvatars.childCount){
       val thumb=memberAvatars.getChildAt(index) as? ImageView ?: continue
       val member=fresh.optJSONObject(index)
       val nested=member?.optJSONObject("user")?:member?.optJSONObject("profile")
       val avatarUrl=member?.optString("avatar_url")?.takeIf{!it.isNullOrBlank()&&it!="null"}
         ?:member?.optString("profile_image")?.takeIf{!it.isNullOrBlank()&&it!="null"}
         ?:nested?.optString("avatar_url")?.takeIf{!it.isNullOrBlank()&&it!="null"}
         ?:nested?.optString("profile_image")?.takeIf{!it.isNullOrBlank()&&it!="null"}
       thumb.setImageDrawable(VectorIcon("user",Color.rgb(255,190,220),dp(13)))
       thumb.tag=avatarUrl.orEmpty()
       if(!avatarUrl.isNullOrBlank())loadAvatarInto(thumb,avatarUrl)
       thumb.visibility=if(member!=null)View.VISIBLE else View.INVISIBLE
      }
     }
    }
   }catch(_:Exception){}
  }
 }
 private fun showRoomMembers(){
  val dialog=android.app.Dialog(this)
  val root=sheetRoot()
  val heading=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  heading.addView(label("الموجودون في الغرفة",17f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(34),1f))
  heading.addView(label(roomMembers.length().toString(),12f,Color.rgb(255,226,151),true).apply{
   background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,64,129),Color.rgb(255,140,60))).apply{cornerRadius=dp(14).toFloat()}
   setPadding(dp(12),dp(5),dp(12),dp(5))
  })
  root.addView(heading,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
  val divider=View(this).apply{setBackgroundColor(Color.argb(30,255,255,255))}
  root.addView(divider,LinearLayout.LayoutParams(-1,dp(1)).apply{bottomMargin=dp(10)})
  val list=LinearLayout(this).apply{orientation=1}
  if(roomMembers.length()==0)list.addView(label("لا يوجد أعضاء لعرضهم حالياً",13f,Color.LTGRAY))
  for(i in 0 until roomMembers.length()){
   val member=roomMembers.optJSONObject(i)?:continue
   val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
   val username=member.optString("username")
   val account=member.optString("public_id").takeIf{it.isNotBlank()&&it!="null"}?:"—"
   val isOwnerMember=member.optBoolean("is_owner")
   val seat=member.optInt("seat_number",0)
   val role=if(isOwnerMember)"صاحب الغرفة" else if(seat>0)"متحدث • مقعد $seat" else "مستمع"
   val roleColor=if(isOwnerMember)Color.rgb(255,213,124) else if(seat>0)Color.rgb(91,221,239) else Color.rgb(196,196,196)
   val row=LinearLayout(this).apply{
    orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR
    setPadding(dp(10),dp(9),dp(10),dp(9))
    background=GradientDrawable().apply{
     cornerRadius=dp(17).toFloat()
     setColor(Color.argb(30,255,255,255))
     setStroke(dp(1),Color.argb(36,255,255,255))
    }
    isClickable=true
   }
   val avatarFrame=FrameLayout(this).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(51,51,51));setStroke(dp(1.5f.toInt()),roleColor)}
    clipToOutline=true
   }
   val avatar=ImageView(this).apply{
    scaleType=ImageView.ScaleType.CENTER_CROP
    setImageDrawable(VectorIcon("user",Color.WHITE,dp(22)))
   }
   avatarFrame.addView(avatar,FrameLayout.LayoutParams(dp(42),dp(42),Gravity.CENTER))
   row.addView(avatarFrame,LinearLayout.LayoutParams(dp(46),dp(46)).apply{marginEnd=dp(10)})
   val url=member.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
   if(url.isNotBlank())loadAvatarInto(avatar,url)
   val info=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL}
   info.addView(label(name,14f,Color.WHITE,true).apply{gravity=Gravity.START;maxLines=1})
   info.addView(label("@$username  •  ID: $account",10f,Color.rgb(185,185,185)).apply{gravity=Gravity.START;maxLines=1})
   row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
   row.addView(label(role,10f,roleColor,true).apply{
    background=GradientDrawable().apply{cornerRadius=dp(10).toFloat();setColor(Color.argb(45,Color.red(roleColor),Color.green(roleColor),Color.blue(roleColor)));setStroke(dp(1),Color.argb(150,Color.red(roleColor),Color.green(roleColor),Color.blue(roleColor)))}
    setPadding(dp(8),dp(5),dp(8),dp(5))
   })
   row.setOnClickListener{
    dialog.dismiss()
    showMemberProfile(member)
   }
   list.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
  }
  val scroll=ScrollView(this).apply{isFillViewport=false;overScrollMode=View.OVER_SCROLL_NEVER;addView(list)}
  root.addView(scroll,LinearLayout.LayoutParams(-1,dp(330)))
  val close=TextView(this).apply{
   text="إغلاق";textSize=14f;gravity=Gravity.CENTER
   setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD
   background=GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.argb(40,255,255,255));setStroke(dp(1),Color.argb(70,255,255,255))}
   setPadding(0,dp(11),0,dp(11))
   setOnClickListener{dialog.dismiss()}
  }
  root.addView(close,LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(10)})
  presentSheet(dialog,root)
 }
   private fun showMemberModeration(member:JSONObject){
   if(!isOwner)return
   val userId=member.optString("user_id")
   if(userId.isBlank() || userId=="null" || userId==currentUserId)return
   val name=member.optString("display_name").ifBlank{member.optString("username","مستخدم")}
   scope.launch{
    try{
     val response=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/$roomId/moderation",authToken)}
     if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحميل حالة العضو"))
     val restrictions=response.body.optJSONArray("restrictions")?:JSONArray()
     var chatBanned=false
     var roomBanned=false
     for(i in 0 until restrictions.length()){
      val item=restrictions.optJSONObject(i)?:continue
      if(item.optString("user_id")!=userId)continue
      when(item.optString("action_type")){"chat_ban"->chatBanned=true;"room_ban"->roomBanned=true}
     }
     showChoiceSheet("إدارة العضو",name,listOf(
      SheetOption("عرض الملف الشخصي",2){showMemberProfile(member)},
      SheetOption(if(chatBanned)"إلغاء حظر التعليقات" else "حظر التعليقات",if(chatBanned)2 else 0){applyRoomModeration(userId,"chat_ban",chatBanned)},
      SheetOption(if(roomBanned)"إلغاء الطرد من الغرفة" else "طرد من الغرفة",1){applyRoomModeration(userId,"room_ban",roomBanned)}
     ))
    }catch(e:Exception){Toast.makeText(this@RoomActivity,e.message?:"تعذر تحميل خيارات الإدارة",Toast.LENGTH_LONG).show()}
   }
  }

  private fun applyRoomModeration(userId:String,actionType:String,currentlyActive:Boolean){
   if(!isOwner || userId.isBlank() || userId==currentUserId)return
   scope.launch{
    try{
     val result=withContext(Dispatchers.IO){
      if(currentlyActive)AuthApi.delete("/api/voice-rooms/$roomId/moderation/$userId/$actionType",authToken)
      else AuthApi.post("/api/voice-rooms/$roomId/moderation",JSONObject().put("userId",userId).put("actionType",actionType),authToken)
     }
     if(result.status !in 200..299)throw Exception(result.body.optString("error","تعذر تنفيذ الإجراء"))
     val message=when(actionType){
      "chat_ban"->if(currentlyActive)"تم إلغاء حظر التعليقات" else "تم حظر العضو من التعليقات"
      else->if(currentlyActive)"تم إلغاء الطرد" else "تم طرد العضو من الغرفة"
     }
     Toast.makeText(this@RoomActivity,message,Toast.LENGTH_LONG).show()
     loadRoomMembers()
    }catch(e:Exception){Toast.makeText(this@RoomActivity,e.message?:"تعذر الاتصال بالخادم",Toast.LENGTH_LONG).show()}
   }
  }

  private fun showRoomModerationList(){
   if(!isOwner)return
   scope.launch{
    try{
     val response=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/$roomId/moderation",authToken)}
     if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحميل قائمة القيود"))
     val restrictions=response.body.optJSONArray("restrictions")?:JSONArray()
     val dialog=android.app.Dialog(this@RoomActivity)
     val root=sheetRoot()
     root.addView(label("المحظورون والمطرودون",18f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(34)))
     val list=LinearLayout(this@RoomActivity).apply{orientation=1}
     if(restrictions.length()==0)list.addView(label("لا توجد قيود على أعضاء هذه الغرفة",13f,Color.LTGRAY).apply{setPadding(0,dp(20),0,dp(20))})
     for(i in 0 until restrictions.length()){
      val item=restrictions.optJSONObject(i)?:continue
      val userId=item.optString("user_id")
      val actionType=item.optString("action_type")
      val displayName=item.optString("display_name").ifBlank{item.optString("username","مستخدم")}
      val isChat=actionType=="chat_ban"
      val row=LinearLayout(this@RoomActivity).apply{
       orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
       setPadding(dp(12),dp(10),dp(12),dp(10));background=glassBg(48,15,55);isClickable=true
      }
      val info=LinearLayout(this@RoomActivity).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL}
      info.addView(label(displayName,13.5f,Color.WHITE,true).apply{gravity=Gravity.START})
      info.addView(label(if(isChat)"حظر التعليقات" else "طرد من الغرفة",10.5f,if(isChat)Color.rgb(255,205,120) else Color.rgb(255,130,145)).apply{gravity=Gravity.START})
      row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
      row.addView(label("إلغاء",12f,Color.WHITE,true).apply{
       background=GradientDrawable().apply{setColor(Color.argb(70,90,190,130));cornerRadius=dp(14).toFloat()}
       setPadding(dp(12),dp(7),dp(12),dp(7))
      })
      row.setOnClickListener{
       dialog.dismiss()
       showChoiceSheet("إلغاء القيد","هل تريد إزالة هذا القيد عن $displayName؟",listOf(
        SheetOption("تأكيد الإلغاء",0){applyRoomModeration(userId,actionType,true)},
        SheetOption("رجوع",2){}
       ))
      }
      list.addView(row,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(7)})
     }
     root.addView(ScrollView(this@RoomActivity).apply{overScrollMode=View.OVER_SCROLL_NEVER;addView(list)},LinearLayout.LayoutParams(-1,dp(340)))
     root.addView(label("إغلاق",13f,Color.WHITE,true).apply{
      background=glassBg(40,20,60);setPadding(0,dp(10),0,dp(10));setOnClickListener{dialog.dismiss()}
     },LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(8)})
     presentSheet(dialog,root)
    }catch(e:Exception){Toast.makeText(this@RoomActivity,e.message?:"تعذر تحميل قائمة القيود",Toast.LENGTH_LONG).show()}
   }
  }

private fun memberLevelColors(level:Int):IntArray = when(level){
  in 1..10 -> intArrayOf(Color.rgb(54,37,25),Color.rgb(112,75,43),Color.rgb(43,29,22),Color.rgb(218,157,91),Color.rgb(255,220,174),Color.rgb(226,173,113),Color.rgb(88,61,39))
  in 11..20 -> intArrayOf(Color.rgb(37,43,55),Color.rgb(104,116,133),Color.rgb(34,39,51),Color.rgb(192,205,221),Color.rgb(245,249,255),Color.rgb(218,228,241),Color.rgb(91,103,122))
  in 21..30 -> intArrayOf(Color.rgb(55,39,10),Color.rgb(150,105,20),Color.rgb(64,43,10),Color.rgb(255,207,70),Color.rgb(255,239,164),Color.rgb(255,218,105),Color.rgb(111,76,16))
  in 31..40 -> intArrayOf(Color.rgb(9,55,43),Color.rgb(17,119,83),Color.rgb(8,45,39),Color.rgb(72,222,164),Color.rgb(190,255,225),Color.rgb(102,238,184),Color.rgb(20,105,78))
  in 41..50 -> intArrayOf(Color.rgb(9,35,75),Color.rgb(22,91,160),Color.rgb(8,29,67),Color.rgb(83,174,255),Color.rgb(196,231,255),Color.rgb(112,190,255),Color.rgb(26,87,145))
  in 51..60 -> intArrayOf(Color.rgb(33,33,33),Color.rgb(81,81,81),Color.rgb(27,27,27),Color.rgb(166,166,166),Color.rgb(221,221,221),Color.rgb(180,180,180),Color.rgb(72,72,72))
  in 61..70 -> intArrayOf(Color.rgb(75,12,28),Color.rgb(164,31,59),Color.rgb(58,10,27),Color.rgb(255,105,132),Color.rgb(255,204,213),Color.rgb(255,137,157),Color.rgb(130,27,57))
  in 71..80 -> intArrayOf(Color.rgb(13,48,61),Color.rgb(34,133,151),Color.rgb(9,37,53),Color.rgb(105,231,245),Color.rgb(213,255,255),Color.rgb(135,239,247),Color.rgb(38,119,139))
  in 81..90 -> intArrayOf(Color.rgb(31,31,31),Color.rgb(69,69,69),Color.rgb(23,16,61),Color.rgb(164,164,164),Color.rgb(228,228,228),Color.rgb(180,180,180),Color.rgb(75,75,75))
  in 91..99 -> intArrayOf(Color.rgb(67,13,24),Color.rgb(151,28,44),Color.rgb(42,12,28),Color.rgb(255,180,83),Color.rgb(255,235,187),Color.rgb(255,197,112),Color.rgb(154,48,51))
  else -> intArrayOf(Color.rgb(24,24,24),Color.rgb(126,25,91),Color.rgb(27,27,27),Color.rgb(255,210,92),Color.rgb(255,245,190),Color.rgb(255,221,128),Color.rgb(190,53,112))
 }
 private fun profileValue(item:JSONObject,keys:List<String>):String{
  for(key in keys){
   val value=item.opt(key)
   if(value!=null && value!=JSONObject.NULL){
    val text=value.toString()
    if(text.isNotBlank() && text!="null")return text
   }
  }
  return "0"
 }
 private fun compactProfileCount(raw:String):String{
  val value=raw.replace(",","").toDoubleOrNull() ?: return raw
  return when{
   value>=1_000_000_000 -> String.format(java.util.Locale.US,"%.1fB",value/1_000_000_000.0).replace(".0B","B")
   value>=1_000_000 -> String.format(java.util.Locale.US,"%.1fM",value/1_000_000.0).replace(".0M","M")
   value>=1_000 -> String.format(java.util.Locale.US,"%.1fK",value/1_000.0).replace(".0K","K")
   else -> String.format(java.util.Locale.US,"%.0f",value)
  }
 }
 private fun showMemberProfile(member:JSONObject){
  val uid=member.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}?:return
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){AuthApi.get("/api/users/$uid",authToken)}
    if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر تحديث بيانات الملف"))
    val full=response.body.optJSONObject("user")?:response.body.optJSONObject("profile")?:response.body
    val merged=JSONObject(member.toString())
    val keys=listOf("user_id","display_name","username","public_id","account_id","avatar_url","profile_image","total_xp","level","bio","fans_count","fans","followers_count","followersCount","following_count","following","follow_count","followings_count","diamonds_sent","total_diamonds_sent","sent_diamonds","total_sent","diamonds_received","total_diamonds_received","received_diamonds","total_received")
    for(key in keys)if(full.has(key)&&full.opt(key)!=JSONObject.NULL)merged.put(key,full.opt(key))
    val nested=full.optJSONObject("stats")?:full.optJSONObject("statistics")?:full.optJSONObject("profile_stats")
    if(nested!=null)for(key in keys)if(nested.has(key)&&nested.opt(key)!=JSONObject.NULL)merged.put(key,nested.opt(key))
    renderMemberProfile(merged)
   }catch(_:Exception){renderMemberProfile(member)}
  }
 }
 private fun renderMemberProfile(member:JSONObject){
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
  val root=FrameLayout(this).apply{
   setBackgroundColor(Color.TRANSPARENT)
   clipChildren=false
   clipToPadding=false
  }
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER_HORIZONTAL
   layoutDirection=View.LAYOUT_DIRECTION_RTL
   setPadding(dp(18),dp(12),dp(18),dp(20))
   background=GradientDrawable().apply{
    setColor(Color.WHITE)
    val r=dp(28).toFloat()
    cornerRadii=floatArrayOf(r,r,r,r,0f,0f,0f,0f)
    setStroke(dp(1),Color.rgb(235,237,240))
   }
   elevation=dp(8).toFloat()
  }
  root.addView(card,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply{topMargin=dp(54)})
  val header=FrameLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val close=ImageView(this).apply{
   setImageResource(R.drawable.ic_profile_close)
   setColorFilter(Color.rgb(65,68,74),PorterDuff.Mode.SRC_IN)
   setPadding(dp(8),dp(8),dp(8),dp(8))
   contentDescription="إغلاق الملف الشخصي"
   setOnClickListener{dialog.dismiss()}
  }
  header.addView(close,FrameLayout.LayoutParams(dp(34),dp(34),Gravity.START or Gravity.TOP))
  val reportChip=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER
   layoutDirection=View.LAYOUT_DIRECTION_RTL
   setPadding(dp(9),dp(5),dp(9),dp(5))
   background=GradientDrawable().apply{
    setColor(Color.rgb(248,249,251))
    cornerRadius=dp(16).toFloat()
    setStroke(dp(1),Color.rgb(226,229,234))
   }
   isClickable=true
   isFocusable=true
   contentDescription="إبلاغ عن المستخدم"
   addView(ImageView(this@RoomActivity).apply{
    setImageDrawable(VectorIcon("flag",Color.rgb(70,74,82),dp(14)))
   },LinearLayout.LayoutParams(dp(16),dp(16)).apply{marginEnd=dp(4)})
   addView(label("إبلاغ",11f,Color.rgb(70,74,82),true))
   setOnClickListener{reportRoomMember(uid,name,dialog)}
  }
  header.addView(reportChip,FrameLayout.LayoutParams(-2,dp(30),Gravity.END or Gravity.TOP))
  card.addView(header,LinearLayout.LayoutParams(-1,dp(32)))
  val avatar=ImageView(this).apply{
   scaleType=ImageView.ScaleType.CENTER_CROP
   background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.WHITE);setStroke(dp(3),Color.WHITE)}
   clipToOutline=true
   elevation=dp(8).toFloat()
   setImageDrawable(VectorIcon("user",Color.rgb(130,137,145),dp(28)))
  }
  if(avatarUrl.isNotBlank())loadAvatarInto(avatar,avatarUrl)
  avatar.isClickable=true
  avatar.isFocusable=true
  avatar.contentDescription="فتح الملف الشخصي الكامل"
  avatar.setOnClickListener{
   if(uid.isBlank() || uid=="null"){
    Toast.makeText(this@RoomActivity,"معرّف المستخدم غير متوفر",Toast.LENGTH_SHORT).show()
    return@setOnClickListener
   }
   try{
    // Launch first while the mini-profile is still attached. The room Activity stays
    // underneath in the same task, preserving its LiveKit room and seat.
    startActivity(
     Intent(this@RoomActivity,ProfileActivity::class.java)
      .putExtra("profile_user_id",uid)
      .putExtra("room_profile_source",true)
    )
    dialog.dismiss()
   }catch(e:Exception){
    android.util.Log.e("BAK_ROOM_PROFILE","Unable to open full profile for $uid",e)
    Toast.makeText(this@RoomActivity,"تعذر فتح الملف الشخصي",Toast.LENGTH_SHORT).show()
   }
  }
  root.addView(avatar,FrameLayout.LayoutParams(dp(104),dp(104),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
  val nameLabel=label(name,21f,Color.rgb(28,29,33),true).apply{gravity=Gravity.CENTER}
  val accountLabel=label("ID: $account",13f,Color.rgb(137,140,146)).apply{gravity=Gravity.CENTER}
  card.addView(View(this),LinearLayout.LayoutParams(1,dp(48)))
  card.addView(nameLabel,LinearLayout.LayoutParams(-1,dp(36)))
  card.addView(accountLabel,LinearLayout.LayoutParams(-1,dp(28)))
  val levelColors=memberLevelColors(level)
  val badges=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val levelBadge=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER
   setPadding(dp(16),dp(7),dp(16),dp(7))
   background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(levelColors[0],levelColors[1],levelColors[2])).apply{
    cornerRadius=dp(18).toFloat()
    setStroke(dp(1),levelColors[3])
   }
   val levelIcon=ImageView(this@RoomActivity).apply{
    setImageResource(R.drawable.ic_profile_level)
    setColorFilter(levelColors[4],PorterDuff.Mode.SRC_IN)
   }
   addView(levelIcon,LinearLayout.LayoutParams(dp(18),dp(18)).apply{marginEnd=dp(7)})
   addView(label("LV $level",14f,levelColors[4],true))
  }
  badges.addView(levelBadge)
  card.addView(badges,LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(2);bottomMargin=dp(10)})
  val stats=LinearLayout(this).apply{
    orientation=LinearLayout.HORIZONTAL
    gravity=Gravity.CENTER
    // Same physical column positions as the full profile page.
    layoutDirection=View.LAYOUT_DIRECTION_LTR
    clipChildren=false
   }
   fun metric(value:String,title:String){
    val col=LinearLayout(this@RoomActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(4),0,dp(4),0)}
    col.addView(label(value,17f,Color.rgb(27,29,34),true),LinearLayout.LayoutParams(-2,dp(27)))
    col.addView(label(title,10f,Color.rgb(151,153,158)),LinearLayout.LayoutParams(-2,dp(21)))
    stats.addView(col,LinearLayout.LayoutParams(0,dp(52),1f))
   }
   // Match the full profile statistics order exactly (left to right).
   metric(compactProfileCount(sent),"ألماس مرسل")
   metric(compactProfileCount(received),"ألماس مستلم")
   metric(compactProfileCount(followers),"متابعون")
   metric(compactProfileCount(fans),"معجبين")
   card.addView(stats,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(6);bottomMargin=dp(14)})
   val actions=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  fun actionButton(title:String,color:Int,textColor:Int=Color.WHITE,click:(TextView)->Unit):TextView{
   return label(title,14f,textColor,true).apply{
    gravity=Gravity.CENTER
    background=GradientDrawable().apply{setColor(color);cornerRadius=dp(26).toFloat()}
    setPadding(dp(10),dp(11),dp(10),dp(11));isClickable=true;setOnClickListener{click(this)}
   }
  }
  val message=actionButton("مراسلة",Color.rgb(235,236,239),Color.rgb(45,48,54)){openMemberConversation(uid,name)}
  actions.addView(message,LinearLayout.LayoutParams(0,dp(50),1f).apply{marginEnd=dp(6)})
  val gift=FrameLayout(this).apply{
   background=GradientDrawable().apply{setColor(Color.rgb(255,137,47));cornerRadius=dp(25).toFloat()}
   val giftIcon=ImageView(this@RoomActivity).apply{
    setImageResource(R.drawable.ic_profile_gift)
    setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN)
    setPadding(dp(14),dp(12),dp(14),dp(12))
    contentDescription="هدية"
   }
   addView(giftIcon,FrameLayout.LayoutParams(-1,-1))
   setOnClickListener{
    if(uid==currentUserId){Toast.makeText(this@RoomActivity,"لا يمكنك إرسال هدية لنفسك",Toast.LENGTH_SHORT).show()}
    else{dialog.dismiss();loadGiftCatalogAndChoose(uid,name)}
   }
  }
  actions.addView(gift,LinearLayout.LayoutParams(dp(58),dp(50)).apply{marginEnd=dp(6)})
  val follow=actionButton("جارٍ التحقق...",Color.rgb(31,169,132)){toggleMemberFollow(uid,name,it)}
  follow.isEnabled=false
  actions.addView(follow,LinearLayout.LayoutParams(0,dp(50),1f))
  loadMemberFollowState(uid,follow)
  card.addView(actions,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(5)})
  presentSheet(dialog,root)
 }
 private fun reportRoomMember(reportedUserId:String,reportedName:String,profileDialog:android.app.Dialog){
  if(reportedUserId.isBlank() || reportedUserId==currentUserId){
   Toast.makeText(this,"لا يمكنك الإبلاغ عن حسابك",Toast.LENGTH_SHORT).show()
   return
  }
  val labels=listOf("إساءة أو ألفاظ غير لائقة","محتوى غير مناسب","انتحال شخصية","احتيال أو نصب","مضايقة أو تحرش","غش أو مخالفة القوانين","سبب آخر")
  val keys=listOf("abuse","inappropriate_content","impersonation","fraud","harassment","cheating","other")
  val details=EditText(this).apply{hint="تفاصيل إضافية (اختياري، وإلزامي عند اختيار سبب آخر)";minLines=2;maxLines=4;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)}
  var selected=0
  val content=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(18),dp(8),dp(18),0)
   val group=android.widget.RadioGroup(this@RoomActivity).apply{
    orientation=android.widget.RadioGroup.VERTICAL
    labels.forEachIndexed{index,labelText->
     addView(android.widget.RadioButton(this@RoomActivity).apply{text=labelText;setTextColor(Color.WHITE);id=View.generateViewId();if(index==0)isChecked=true;tag=index})
    }
    setOnCheckedChangeListener{g,checkedId->
     selected=(0 until g.childCount).mapNotNull{g.getChildAt(it) as? android.widget.RadioButton}.firstOrNull{it.id==checkedId}?.tag as? Int ?: 0
    }
   }
   addView(group)
   addView(details,LinearLayout.LayoutParams(-1,dp(92)).apply{topMargin=dp(8)})
  }
  val reportDialog=AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert)
   .setTitle("إبلاغ عن $reportedName").setView(content).setNegativeButton("إلغاء",null).setPositiveButton("إرسال البلاغ",null).create()
  reportDialog.setOnShowListener{
   reportDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
    val note=details.text.toString().trim()
    if(keys[selected]=="other" && note.length<5){details.error="اكتب تفاصيل السبب (5 أحرف على الأقل)";return@setOnClickListener}
    reportDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
    scope.launch{
     try{
      // Room member IDs may be public IDs/identifiers; resolve the canonical users.id UUID first.
      val profileResponse=withContext(Dispatchers.IO){AuthApi.get("/api/users/$reportedUserId",authToken)}
      if(profileResponse.status !in 200..299)throw Exception(profileResponse.body.optString("error","تعذر التحقق من حساب المستخدم"))
      val profile=profileResponse.body.optJSONObject("user")?:profileResponse.body
      val canonicalUserId=sequenceOf("id","user_id","uuid").map{profile.optString(it)}.firstOrNull{it.matches(Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))}
       ?:throw Exception("لم يعثر التطبيق على المعرّف الداخلي الصحيح لهذا الحساب")
      val payload=JSONObject().put("reportedUserId",canonicalUserId).put("roomId",roomId).put("reason",keys[selected]).put("details",note)
      val response=withContext(Dispatchers.IO){AuthApi.post("/api/reports",payload,authToken)}
      if(response.status !in 200..299)throw Exception(response.body.optString("error","تعذر إرسال البلاغ"))
      reportDialog.dismiss();profileDialog.dismiss()
      Toast.makeText(this@RoomActivity,"تم إرسال البلاغ بنجاح",Toast.LENGTH_LONG).show()
     }catch(e:Exception){
      reportDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
      Toast.makeText(this@RoomActivity,e.message?:"تعذر إرسال البلاغ",Toast.LENGTH_LONG).show()
     }
    }
   }
  }
  reportDialog.show()
 }
 private fun renderMemberFollowState(userId:String,button:TextView,following:Boolean){
  memberFollowStates[userId]=following
  button.text=if(following)"✓ تتابعه" else "＋ متابعة"
  button.background=GradientDrawable().apply{setColor(if(following)Color.argb(210,60,170,120) else Color.rgb(254,44,85));cornerRadius=dp(24).toFloat()}
  button.isEnabled=true
 }
 private fun loadMemberFollowState(userId:String,button:TextView){
  if(userId==currentUserId){button.text="حسابك";button.isEnabled=false;return}
  button.isEnabled=false
  scope.launch{
   try{
    val result=withContext(Dispatchers.IO){AuthApi.get("/api/follows/status/$userId",authToken)}
    if(result.status in 200..299)renderMemberFollowState(userId,button,result.body.optBoolean("following",false))
    else{memberFollowStates.remove(userId);button.text="＋ متابعة";button.isEnabled=true}
   }catch(_:Exception){memberFollowStates.remove(userId);button.text="＋ متابعة";button.isEnabled=true}
  }
 }
 private fun toggleMemberFollow(userId:String,name:String,button:TextView){
   if(userId==currentUserId){Toast.makeText(this,"لا يمكنك متابعة نفسك",Toast.LENGTH_SHORT).show();return}
   val wasFollowing=memberFollowStates[userId] ?: button.text.toString().contains("تتابعه")
   if(wasFollowing){
    AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_Alert)
     .setTitle("تأكيد إلغاء المتابعة")
     .setMessage("هل تريد إلغاء متابعة $name؟")
     .setNegativeButton("إبقاء المتابعة",null)
     .setPositiveButton("إلغاء المتابعة"){_,_->submitMemberFollowChange(userId,false,button)}
     .show()
   }else submitMemberFollowChange(userId,true,button)
  }
  private fun submitMemberFollowChange(userId:String,shouldFollow:Boolean,button:TextView){
   button.isEnabled=false
   scope.launch{
    try{
     val result=withContext(Dispatchers.IO){
      if(shouldFollow)AuthApi.post("/api/follows/$userId",JSONObject(),authToken)
      else AuthApi.delete("/api/follows/$userId",authToken)
     }
     if(result.status in 200..299){
      renderMemberFollowState(userId,button,shouldFollow)
      Toast.makeText(this@RoomActivity,if(shouldFollow)"تمت المتابعة بنجاح" else "تم إلغاء المتابعة",Toast.LENGTH_SHORT).show()
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
    loadRoomMembers()
   }
  }
 }
 private fun showRoomTools(){
  val dialog=android.app.Dialog(this)
  val root=whiteSheetRoot()
  root.addView(label("أدوات الغرفة",18f,Color.rgb(28,30,36),true),LinearLayout.LayoutParams(-1,dp(30)))
  root.addView(label("تحكم سريع بالصوت والموسيقى ومظهر الغرفة",11f,Color.rgb(132,136,144)),LinearLayout.LayoutParams(-1,dp(22)).apply{bottomMargin=dp(14)})
  class RoomTool(val title:String,val icon:String,val accent:Int,val action:()->Unit)
  val tools=ArrayList<RoomTool>()
  fun addTool(title:String,subtitle:String,icon:String,accent:Int,action:()->Unit){tools.add(RoomTool(title,icon,accent,action))}
  if(isSeated)addTool(if(microphoneEnabled)"كتم المايك" else "تشغيل المايك",
   if(microphoneEnabled)"إيقاف صوتك مع البقاء داخل المقعد" else "السماح للآخرين بسماعك",
   "mic",Color.rgb(64,224,255)){toggleMic()}
  if(isOwner)addTool("موسيقى الغرفة","اختيار وتشغيل موسيقى يسمعها أعضاء الغرفة","music",Color.rgb(244,201,105)){showMusicOptions()}
  if(isOwner)addTool(
   if(roomMode=="camera")"غرفة صوتية" else "غرفة كاميرا",
   if(roomMode=="camera")"تغيير الغرفة من كاميرا إلى صوت فقط" else "تفعيل وضع الكاميرا ومقاعد البث",
   "camera",Color.rgb(111,198,232)
  ){confirmRoomModeSwitch()}
  addTool("مشاهدة جماعية","ابحث عن فيديو YouTube وشاهده مع أعضاء الغرفة","play",Color.rgb(255,75,90)){
   watchPlayer?.open()
  }
  if(isOwner)addTool("خلفية الغرفة","تغيير الصورة والخلفية الظاهرة للجميع","image",Color.rgb(111,198,232)){openRoomBackgroundEditor()}
  addTool("الأعضاء","عرض المستمعين والمتحدثين وملفاتهم الشخصية","users",Color.rgb(255,120,180)){loadRoomMembers();showRoomMembers()}
  if(isOwner)addTool("المحظورون","إدارة قيود التعليقات والطرد وإلغائها","ban",Color.rgb(255,92,110)){showRoomModerationList()}
  val columns=4
  var rowLayout:LinearLayout?=null
  tools.forEachIndexed{i,t->
   if(i%columns==0){
    rowLayout=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
    root.addView(rowLayout,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
   }
   val r=Color.red(t.accent);val g=Color.green(t.accent);val b=Color.blue(t.accent)
   val cell=LinearLayout(this).apply{
    orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
    isClickable=true;isFocusable=true
    setOnClickListener{dialog.dismiss();t.action()}
   }
   val iconColor=Color.rgb((r*0.78f).toInt(),(g*0.78f).toInt(),(b*0.78f).toInt())
   val box=FrameLayout(this).apply{
    background=GradientDrawable().apply{
     cornerRadius=dp(18).toFloat();setColor(Color.argb(40,r,g,b))
    }
    addView(ImageView(this@RoomActivity).apply{
     setImageDrawable(VectorIcon(t.icon,iconColor,dp(28)))
     scaleType=ImageView.ScaleType.CENTER
    },FrameLayout.LayoutParams(-1,-1))
   }
   cell.addView(box,LinearLayout.LayoutParams(dp(58),dp(58)))
   cell.addView(label(t.title,10.5f,Color.rgb(52,56,64),false).apply{
    maxLines=2;setPadding(dp(2),dp(6),dp(2),0)
   },LinearLayout.LayoutParams(-1,-2))
   rowLayout!!.addView(cell,LinearLayout.LayoutParams(0,-2,1f))
  }
  val remainder=tools.size%columns
  if(remainder!=0)repeat(columns-remainder){rowLayout!!.addView(View(this),LinearLayout.LayoutParams(0,1,1f))}
  val close=TextView(this).apply{
   text="إغلاق";textSize=13f;gravity=Gravity.CENTER
   setTextColor(Color.rgb(52,56,64));typeface=android.graphics.Typeface.DEFAULT_BOLD
   background=GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.rgb(240,241,244))}
   setOnClickListener{dialog.dismiss()}
  }
  root.addView(close,LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(2)})
  presentSheet(dialog,root)
 }

  private fun confirmRoomModeSwitch(){
   if(!isOwner || !connected){
    Toast.makeText(this,"تغيير وضع الغرفة متاح لصاحب الغرفة بعد الاتصال",Toast.LENGTH_SHORT).show()
    return
   }
   if(roomMode=="camera"){
    confirmRoomModeSwitch("audio",null)
    return
   }
   showChoiceSheet(
    "اختيار عدد مقاعد الكاميرا",
    "اختر عدد المقاعد التي تريد إتاحتها في الغرفة قبل التحويل.",
    listOf(
     SheetOption("4 مقاعد كاميرا",0){confirmRoomModeSwitch("camera",4)},
     SheetOption("6 مقاعد كاميرا",0){confirmRoomModeSwitch("camera",6)},
     SheetOption("إلغاء",2){}
    )
   )
  }

  private fun confirmRoomModeSwitch(targetMode:String,selectedCameraSeatCount:Int?){
   val targetLabel=if(targetMode=="camera")"غرفة كاميرا بـ $selectedCameraSeatCount مقاعد" else "غرفة صوتية"
   showChoiceSheet(
    "تأكيد تغيير نوع الغرفة",
    "هل تريد تحويل الغرفة الحالية إلى $targetLabel؟ سيتم تحديث وضع البث لجميع الموجودين.",
    listOf(
     SheetOption("تأكيد التحويل",0){applyRoomModeSwitch(targetMode,selectedCameraSeatCount)},
     SheetOption("رجوع",2){confirmRoomModeSwitch()}
    )
   )
  }

  private fun applyRoomModeSwitch(targetMode:String,selectedCameraSeatCount:Int?){
   if(!isOwner || !connected)return
   scope.launch{
    try{
     val payload=JSONObject().put("mode",targetMode)
     if(targetMode=="camera"){
      val count=selectedCameraSeatCount?.takeIf{it==4 || it==6}
       ?: throw Exception("اختر 4 أو 6 مقاعد للكاميرا")
      payload.put("camera_seat_count",count)
     }
     val response=withContext(Dispatchers.IO){
      AuthApi.put("/api/voice-rooms/$roomId/mode",payload,authToken)
     }
     if(response.status !in 200..299){
      throw Exception(response.body.optString("error","تعذر تغيير وضع الغرفة"))
     }

     stopRoomMusic(restoreMic=false)
     if(cameraEnabled){
      try{room.localParticipant.setCameraEnabled(false)}catch(_:Exception){}
     }
     try{room.disconnect()}catch(_:Exception){}
     connected=false
     cameraEnabled=false
     canPublishVideo=false
     lastSeatRenderSignature=""
     if(targetMode=="audio")ownerSeatSetupDone=false
     connect()
     Toast.makeText(
      this@RoomActivity,
      if(targetMode=="camera")"تم تحويل الغرفة إلى وضع الكاميرا" else "تم تحويل الغرفة إلى الوضع الصوتي",
      Toast.LENGTH_LONG
     ).show()
    }catch(e:Exception){
     Toast.makeText(this@RoomActivity,e.message?:"تعذر تغيير وضع الغرفة",Toast.LENGTH_LONG).show()
    }
   }
  }

  private fun openRoomBackgroundEditor(){
  if(!isOwner){Toast.makeText(this,"تغيير الخلفية متاح لصاحب الغرفة فقط",Toast.LENGTH_SHORT).show();return}
  startActivityForResult(Intent(this,RoomBackgroundEditorActivity::class.java).putExtra("room_id",roomId),4302)
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

 private fun videoTrackFor(userId:String):VideoTrack?{
  if(!::room.isInitialized || !connected)return null
  // A locally disabled camera must immediately reveal the profile avatar.
  // LiveKit may keep the last camera publication object briefly after disable.
  if(userId==currentUserId){
   if(!cameraEnabled)return null
   return room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack
  }
  cameraTracks[userId]?.let{return it}
  val participant=room.remoteParticipants.values.firstOrNull{it.identity?.value==userId}?:return null
  return participant.getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack
 }

 // Reuse one TextureView/EGL renderer per participant across seat redraws.
 private fun obtainCameraRenderer(userId:String,track:VideoTrack):TextureViewRenderer{
  var renderer=cameraRenderers[userId]
  if(renderer==null){
   renderer=TextureViewRenderer(this)
   room.initVideoRenderer(renderer)
   renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
   cameraRenderers[userId]=renderer
  }
  val previous=cameraRendererTracks[userId]
  if(previous!==track){
   try{previous?.removeRenderer(renderer)}catch(_:Exception){}
   track.addRenderer(renderer)
   cameraRendererTracks[userId]=track
  }
  renderer.setMirror(userId==currentUserId)
  return renderer
 }

 private fun releaseCameraRenderers(){
  for(renderer in cameraRenderers.values.toList()){
   (renderer.parent as? android.view.ViewGroup)?.removeView(renderer)
  }
 }

 private fun disposeCameraRenderers(){
  for((userId,renderer) in cameraRenderers.toMap()){
   try{cameraRendererTracks[userId]?.removeRenderer(renderer)}catch(_:Exception){}
   try{renderer.release()}catch(_:Exception){}
  }
  cameraRenderers.clear()
  cameraRendererTracks.clear()
 }

 private fun refreshCameraControl(){
  if(!::cameraControl.isInitialized)return
  cameraControl.setImageDrawable(VectorIcon(if(cameraEnabled)"camera_off" else "camera",Color.WHITE,dp(18)))
  cameraControl.background=if(cameraEnabled)ctrlBg(danger=true) else ctrlBg()
  cameraControl.contentDescription=if(cameraEnabled)"إيقاف الكاميرا" else "تشغيل الكاميرا"
 }

 private fun toggleCamera(){
  if(!connected || !isSeated || !canPublishVideo){Toast.makeText(this,"الكاميرا متاحة للأعضاء الجالسين فقط",Toast.LENGTH_SHORT).show();return}
  if(!cameraEnabled && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),4103);return}
  scope.launch{
   try{
    val enabled=!cameraEnabled
    room.localParticipant.setCameraEnabled(enabled)
    cameraEnabled=enabled
    if(enabled){val track=room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack;if(track!=null)cameraTracks[currentUserId]=track}
    else cameraTracks.remove(currentUserId)
    refreshCameraControl()
    drawSeats()
   }catch(e:Exception){Toast.makeText(this@RoomActivity,e.message?:"تعذر تشغيل الكاميرا",Toast.LENGTH_LONG).show()}
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
  micControl.setImageDrawable(VectorIcon(if(microphoneEnabled)"mic" else "mic_off",Color.WHITE,dp(18)))
  micControl.background=if(microphoneEnabled)ctrlBg() else ctrlBg(danger=true)
  micControl.contentDescription=if(microphoneEnabled)"إيقاف المايك" else "تشغيل المايك"
 }
 private fun showMusicOptions(){
  if(!isOwner || !connected){
   Toast.makeText(this,"الموسيقى متاحة لصاحب الغرفة بعد الاتصال",Toast.LENGTH_SHORT).show()
   return
  }
  val dialog=android.app.Dialog(this)
  val root=sheetRoot()
  root.addView(label("استوديو موسيقى الغرفة",19f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(30)))
  root.addView(label("الموسيقى تُرسل داخل الغرفة عبر اتصال الصوت الحالي",11f,Color.argb(170,255,255,255)),LinearLayout.LayoutParams(-1,dp(24)))
  val mixer=musicMixer
  val playing=mixer!=null
  val statusCard=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10))
   background=GradientDrawable().apply{cornerRadius=dp(15).toFloat();setColor(Color.argb(30,255,255,255));setStroke(dp(1),Color.argb(40,255,255,255))}
  }
  statusCard.addView(label(if(playing)"● الموسيقى تعمل" else "○ لا توجد موسيقى تعمل",13f,
   if(playing)Color.rgb(132,231,174) else Color.argb(210,255,255,255),true),LinearLayout.LayoutParams(0,dp(28),1f))
  statusCard.addView(label(if(playing)"متصلة بالغرفة" else "اختر ملفاً من جهازك",10f,Color.argb(160,255,255,255)))
  root.addView(statusCard,LinearLayout.LayoutParams(-1,dp(49)).apply{topMargin=dp(12);bottomMargin=dp(10)})
  fun option(title:String,detail:String,icon:String,accent:Int,action:()->Unit){
   val row=LinearLayout(this).apply{
    orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR
    setPadding(dp(12),dp(9),dp(12),dp(9))
    background=GradientDrawable().apply{cornerRadius=dp(15).toFloat();setColor(Color.argb(30,255,255,255));setStroke(dp(1),Color.argb(95,Color.red(accent),Color.green(accent),Color.blue(accent)))}
    isClickable=true;isFocusable=true
   }
   val iconBox=FrameLayout(this).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.argb(35,Color.red(accent),Color.green(accent),Color.blue(accent)))}
    addView(ImageView(this@RoomActivity).apply{setImageDrawable(VectorIcon(icon,accent,dp(21)));scaleType=ImageView.ScaleType.CENTER},FrameLayout.LayoutParams(-1,-1))
   }
   row.addView(iconBox,LinearLayout.LayoutParams(dp(40),dp(40)).apply{marginEnd=dp(10)})
   val labels=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL}
   labels.addView(label(title,13.5f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(21)))
   labels.addView(label(detail,10f,Color.argb(160,255,255,255)),LinearLayout.LayoutParams(-1,dp(17)))
   row.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
   row.setOnClickListener{dialog.dismiss();action()}
   root.addView(row,LinearLayout.LayoutParams(-1,dp(60)).apply{bottomMargin=dp(7)})
  }
  if(mixer==null){
   option("اختيار أغنية","اختر ملفاً صوتياً من الهاتف","music",Color.rgb(242,205,117)){chooseRoomMusic()}
  }else{
   option(if(mixer.paused)"استئناف الموسيقى" else "إيقاف مؤقت",
    if(mixer.paused)"متابعة تشغيل المقطع الحالي" else "إيقاف مؤقت دون إنهاء المقطع",
    if(mixer.paused)"play" else "pause",Color.rgb(132,224,171)){
     mixer.paused=!mixer.paused
     Toast.makeText(this,if(mixer.paused)"تم إيقاف الموسيقى مؤقتاً" else "استؤنفت الموسيقى",Toast.LENGTH_SHORT).show()
    }
   option("مستوى الصوت","تعديل مستوى الموسيقى المرسلة للغرفة","volume-2",Color.rgb(111,198,232)){showMusicVolume(mixer)}
   option("اختيار أغنية أخرى","استبدال المقطع الحالي بملف جديد","music",Color.rgb(242,205,117)){chooseRoomMusic()}
   option("إيقاف الموسيقى","إنهاء مشاركة الموسيقى في الغرفة","square",Color.rgb(239,132,132)){stopRoomMusic()}
  }
  val close=TextView(this).apply{
   text="إغلاق";textSize=13f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)
   background=GradientDrawable().apply{cornerRadius=dp(14).toFloat();setColor(Color.argb(45,255,255,255))}
   setPadding(0,dp(10),0,dp(10));setOnClickListener{dialog.dismiss()}
  }
  root.addView(close,LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(4)})
  presentSheet(dialog,root)
 }

  private fun showMusicVolume(mixer:RoomMusicMixer){
  val seek=SeekBar(this).apply{
   max=100
   progress=(mixer.volume*100f).toInt().coerceIn(0,100)
   setPadding(dp(22),dp(12),dp(22),dp(12))
  }
  AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert)
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
  if(requestCode==4302){
   loadSeats()
   return
  }
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
 @Deprecated("Deprecated in Android") override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==4102){if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)connect() else status.text="يجب السماح باستخدام المايك"}else if(requestCode==4103){if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)toggleCamera() else Toast.makeText(this,"يجب السماح باستخدام الكاميرا",Toast.LENGTH_SHORT).show()}}
 private fun shareRoom(){
  if(roomId.isBlank())return
  val roomName=intent.getStringExtra("room_name").orEmpty().ifBlank{"غرفة صوتية"}
  val shareText="تعال انضم إلينا في $roomName على BAK LIVE\nمعرّف الغرفة: $roomId"
  val send=Intent(Intent.ACTION_SEND).apply{
   type="text/plain"
   putExtra(Intent.EXTRA_TEXT,shareText)
  }
  startActivity(Intent.createChooser(send,"مشاركة البث"))
 }
 private fun leaveRoom(){
  if(leaving)return
  if(isOwner){
   showChoiceSheet("إدارة الغرفة","هل تريد إبقاء الغرفة مفتوحة والعودة إليها لاحقاً، أم إغلاقها نهائياً؟",listOf(
    SheetOption("إبقاء الغرفة",0){leaveRoomNow(false)},
    SheetOption("إغلاق الغرفة نهائياً",1){closeRoomNow()},
    SheetOption("إلغاء",2){}))
  }else{
   showChoiceSheet("مغادرة الغرفة","هل تريد الخروج من الغرفة؟",listOf(
    SheetOption("مغادرة",1){leaveRoomNow(false)},
    SheetOption("إلغاء",2){}))
  }
 }
 private fun leaveRoomNow(closeRoom:Boolean){
  if(leaving)return
  leaving=true
  scope.launch{
   try{
    stopRoomMusic(restoreMic=false)
    if(connected)room.disconnect()
    withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/$roomId/leave",JSONObject().put("keep_open",!closeRoom),authToken)
    }
    stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
   }catch(_:Exception){
    stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
   }
   finally{
    val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
    if(prefs.getString("active_room_id",null)==roomId)prefs.edit().remove("active_room_id").apply()
    finish()
   }
  }
 }
 private fun closeRoomNow(){
  if(leaving)return
  showChoiceSheet("إغلاق الغرفة نهائياً؟","سيتم إنهاء البث وإخراج جميع الموجودين. لا يمكن إعادة فتح هذه الجلسة.",listOf(
   SheetOption("تأكيد الإغلاق",1){
    leaving=true
    scope.launch{
     try{
      stopRoomMusic(restoreMic=false)
      if(connected)room.disconnect()
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
     stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java))
     val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
     if(prefs.getString("active_room_id",null)==roomId)prefs.edit().remove("active_room_id").apply()
     finish()
    }
   },
   SheetOption("رجوع",2){}))
 }

 override fun onBackPressed(){leaveRoom()}

 private fun startTrendHeartbeat(){
  trendHeartbeatJob?.cancel()
  trendHeartbeatJob=scope.launch{
   while(isActive && connected && !leaving){
    try{
     val response=withContext(Dispatchers.IO){
      AuthApi.post("/api/voice-rooms/$roomId/trend/heartbeat",JSONObject(),authToken)
     }
     if(response.status in 200..299 && response.body.optBoolean("credited"))loadTrendScore()
    }catch(_:CancellationException){break}catch(_:Exception){}
    // Check frequently; the server still awards only after 55 seconds of verified seating.
    delay(15000)
   }
  }
 }
 private fun animateTrendPoints(target:Long){
  val start=trendPoints
  if(start==target){
   trendButton?.text=String.format(java.util.Locale.US,"%,d",target)
   return
  }
  trendAnimator?.cancel()
  val distance=if(target>=start)target-start else start-target
  // The score is refreshed every second; finish each transition before the next sample.
  val animationDuration=900L
  trendAnimator=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
   duration=animationDuration
   interpolator=android.view.animation.LinearInterpolator()
   addUpdateListener{anim->
    val fraction=anim.animatedValue as Float
    val value=(start.toDouble()+(target.toDouble()-start.toDouble())*fraction).toLong()
    trendButton?.text=String.format(java.util.Locale.US,"%,d",value)
   }
   addListener(object:android.animation.AnimatorListenerAdapter(){
    override fun onAnimationEnd(animation:android.animation.Animator){
     trendPoints=target
     trendButton?.text=String.format(java.util.Locale.US,"%,d",target)
    }
   })
   start()
  }
 }
 private fun applyAuthoritativeTrendScore(latestValue:Long){
  val latest=latestValue.coerceAtLeast(0L)
  if(!trendScoreLoaded){
   // First room load: show the server's real score immediately, never count up from zero.
   trendAnimator?.cancel()
   trendPoints=latest
   trendButton?.text=String.format(java.util.Locale.US,"%,d",latest)
   trendScoreLoaded=true
  }else{
   animateTrendPoints(latest)
  }
 }
 private fun startTrendRefresh(){
   trendRefreshJob?.cancel()
   trendRefreshJob=scope.launch{
    while(isActive && connected && !leaving){
     refreshTrendScoreOnce()
     delay(1000)
    }
   }
  }
  private suspend fun refreshTrendScoreOnce(){
   if(trendRequestInFlight || !connected || leaving)return
   trendRequestInFlight=true
   try{
    val response=withContext(Dispatchers.IO){
     AuthApi.get("/api/voice-rooms/trending",authToken)
    }
    if(response.status in 200..299){
     val rooms=response.body.optJSONArray("rooms")?:JSONArray()
     for(i in 0 until rooms.length()){
      val item=rooms.optJSONObject(i)?:continue
      if(item.optString("id")==roomId){
       applyAuthoritativeTrendScore(item.optLong("trend_points"))
       break
      }
     }
    }
   }catch(e:CancellationException){
    throw e
   }catch(_:Exception){
    // Keep the last confirmed score; retry on the next one-second cycle.
   }finally{
    trendRequestInFlight=false
   }
  }
  private fun loadTrendScore(){
   scope.launch{refreshTrendScoreOnce()}
  }
  private fun openTrendingRoom(targetRoomId:String){
  if(switchingRoom||leaving||targetRoomId.isBlank()||targetRoomId==roomId)return
  switchingRoom=true
  scope.launch{
   try{
    // Leave the current room completely before opening the selected trend room.
    stopRoomMusic(restoreMic=false)
    if(connected){
     room.disconnect()
     connected=false
    }
    val left=withContext(Dispatchers.IO){
     AuthApi.post("/api/voice-rooms/$roomId/leave",JSONObject().put("keep_open",true),authToken)
    }
    if(left.status !in 200..299)throw Exception(left.body.optString("error","تعذر مغادرة البث الحالي"))
    val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
    if(prefs.getString("active_room_id",null)==roomId)prefs.edit().remove("active_room_id").apply()
    val next=Intent(this@RoomActivity,RoomActivity::class.java)
     .putExtra("room_id",targetRoomId)
    startActivity(next)
    finish()
   }catch(e:Exception){
    switchingRoom=false
    Toast.makeText(this@RoomActivity,e.message?:"تعذر الانتقال إلى البث المحدد",Toast.LENGTH_LONG).show()
    if(!isFinishing&&!isDestroyed&&!connected){
     try{connect()}catch(_:Exception){}
    }
   }
  }
 }

 private fun showTrendingRooms(){
  // Native views: RoomActivity extends android.app.Activity, so ComposeView is not available.
  scope.launch{
   try{
    val response=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/trending",authToken)}
    if(response.status !in 200..299){
     Toast.makeText(this@RoomActivity,"تعذر تحميل قائمة المستخدمين المشهورين",Toast.LENGTH_SHORT).show()
     return@launch
    }
    val rooms=response.body.optJSONArray("rooms")?:JSONArray()
    val items=ArrayList<JSONObject>()
    for(i in 0 until rooms.length()){
     val item=rooms.optJSONObject(i)?:continue
     if(item.optString("id").isNotBlank())items.add(item)
    }
    if(items.isEmpty()){
     Toast.makeText(this@RoomActivity,"لا توجد بثوث نشطة حالياً",Toast.LENGTH_SHORT).show()
     return@launch
    }
    val dialog=android.app.Dialog(this@RoomActivity)
    val density=resources.displayMetrics.density
    fun px(value:Int)=(value*density).toInt()
    val ink=Color.rgb(30,32,38)
    val soft=Color.rgb(140,144,152)
    fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this@RoomActivity).apply{
     text=value;textSize=size;setTextColor(color);gravity=Gravity.CENTER
     typeface=android.graphics.Typeface.create("sans-serif",if(bold)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
    fun ownerTitle(item:JSONObject):String=item.optString("owner_name").ifBlank{item.optString("name","مستخدم")}
    fun avatarUrl(item:JSONObject):String{
     val nested=item.optJSONObject("owner")?:item.optJSONObject("user")?:item.optJSONObject("profile")
     return item.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}
      ?:item.optString("owner_avatar_url").takeIf{it.isNotBlank()&&it!="null"}
      ?:nested?.optString("avatar_url")?.takeIf{it.isNotBlank()&&it!="null"}
      ?: ""
    }
    fun compact(value:Long):String=if(value>=1000L)String.format(java.util.Locale.US,"%.1fk",value/1000.0) else value.toString()
    fun makeAvatar(item:JSONObject?,ring:Int,ringWidth:Int):FrameLayout{
     val frame=FrameLayout(this@RoomActivity).apply{
      background=GradientDrawable().apply{
       shape=GradientDrawable.OVAL
       setColor(Color.rgb(236,238,242))
       if(ringWidth>0)setStroke(px(ringWidth),ring)
      }
      setPadding(px(ringWidth+1),px(ringWidth+1),px(ringWidth+1),px(ringWidth+1))
     }
     val photo=ImageView(this@RoomActivity).apply{
      scaleType=ImageView.ScaleType.CENTER_CROP
      background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
      clipToOutline=true
      setImageDrawable(VectorIcon("user",Color.rgb(170,174,184),dp(24)))
     }
     frame.addView(photo,FrameLayout.LayoutParams(-1,-1))
     val url=if(item!=null)avatarUrl(item) else ""
     if(url.isNotBlank()){photo.tag=url;loadAvatarInto(photo,url)}
     return frame
    }
    fun pointsRow(value:Long,size:Float):LinearLayout{
     val row=LinearLayout(this@RoomActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
     row.addView(text(compact(value),size,Color.rgb(54,57,66),false),LinearLayout.LayoutParams(-2,-2).apply{marginEnd=px(4)})
     row.addView(ImageView(this@RoomActivity).apply{
      setImageDrawable(VectorIcon("flame",Color.WHITE,px(28)))
      scaleType=ImageView.ScaleType.FIT_CENTER
     },LinearLayout.LayoutParams(px(15),px(18)))
     return row
    }
    fun openItem(item:JSONObject){
     val selectedId=item.optString("id")
     dialog.dismiss()
     if(selectedId.isNotBlank()&&selectedId!=roomId)openTrendingRoom(selectedId)
    }
    val root=LinearLayout(this@RoomActivity).apply{
     orientation=LinearLayout.VERTICAL
     layoutDirection=View.LAYOUT_DIRECTION_RTL
     background=GradientDrawable().apply{setColor(Color.WHITE);val r=px(24).toFloat();cornerRadii=floatArrayOf(r,r,r,r,0f,0f,0f,0f)}
    }
    // ---- header: close (left) + title with animated blue bars ----
    val header=FrameLayout(this@RoomActivity)
    val titleRow=LinearLayout(this@RoomActivity).apply{
     orientation=LinearLayout.HORIZONTAL
     gravity=Gravity.CENTER
     layoutDirection=View.LAYOUT_DIRECTION_LTR
    }
    titleRow.addView(text("قائمة المستخدمين المشهورين",16f,ink,true).apply{maxLines=1},LinearLayout.LayoutParams(-2,-2))
    titleRow.addView(EqualizerBarsView(this@RoomActivity),LinearLayout.LayoutParams(px(16),px(16)).apply{marginStart=px(6)})
    header.addView(titleRow,FrameLayout.LayoutParams(-2,-1,Gravity.CENTER))
    header.addView(ImageView(this@RoomActivity).apply{
     setImageDrawable(VectorIcon("close",Color.rgb(60,62,70),px(18)))
     scaleType=ImageView.ScaleType.CENTER
     isClickable=true
     contentDescription="إغلاق"
     setOnClickListener{dialog.dismiss()}
    },FrameLayout.LayoutParams(px(38),px(38),Gravity.LEFT or Gravity.CENTER_VERTICAL).apply{leftMargin=px(12)})
    root.addView(header,LinearLayout.LayoutParams(-1,px(54)).apply{topMargin=px(6)})
    root.addView(View(this@RoomActivity).apply{
     background=GradientDrawable().apply{cornerRadius=px(2).toFloat();setColor(ink)}
    },LinearLayout.LayoutParams(px(16),px(3)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=px(12)})
    // ---- scrollable ranking ----
    val scroll=ScrollView(this@RoomActivity).apply{isFillViewport=false;overScrollMode=View.OVER_SCROLL_NEVER}
    val content=LinearLayout(this@RoomActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(px(14),px(4),px(14),px(8))}
    val podium=LinearLayout(this@RoomActivity).apply{
     orientation=LinearLayout.HORIZONTAL
     gravity=Gravity.BOTTOM
     layoutDirection=View.LAYOUT_DIRECTION_RTL
    }
    val podiumIndexes=listOf(1,0,2)
    val heights=listOf(196,226,196)
    val ringColors=listOf(Color.rgb(255,196,40),Color.rgb(150,178,232),Color.rgb(218,142,104))
    val topColors=listOf(Color.rgb(255,230,138),Color.rgb(214,227,255),Color.rgb(255,222,210))
    val bottomColors=listOf(Color.rgb(255,249,222),Color.rgb(244,247,255),Color.rgb(255,244,240))
    for((slot,index) in podiumIndexes.withIndex()){
     val item=items.getOrNull(index)
     val card=LinearLayout(this@RoomActivity).apply{
      orientation=LinearLayout.VERTICAL
      gravity=Gravity.CENTER_HORIZONTAL
      setPadding(px(4),px(14),px(4),px(10))
      background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(topColors[index],bottomColors[index])).apply{cornerRadius=px(16).toFloat()}
     }
     if(item!=null){
      val badge=if(index==0)30 else 26
      card.addView(text((index+1).toString(),if(index==0)15f else 13f,Color.WHITE,true).apply{
       background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(ringColors[index])}
      },LinearLayout.LayoutParams(px(badge),px(badge)).apply{bottomMargin=px(8)})
      val size=if(index==0)78 else 64
      card.addView(makeAvatar(item,ringColors[index],3),LinearLayout.LayoutParams(px(size),px(size)).apply{gravity=Gravity.CENTER_HORIZONTAL})
      card.addView(text(ownerTitle(item),13f,ink,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(px(2),0,px(2),0)},LinearLayout.LayoutParams(-1,px(30)).apply{topMargin=px(6)})
      card.addView(pointsRow(item.optLong("trend_points"),12.5f),LinearLayout.LayoutParams(-1,px(22)))
      card.setOnClickListener{openItem(item)}
     }else{
      card.addView(text("—",20f,soft),LinearLayout.LayoutParams(-1,px(70)))
     }
     podium.addView(card,LinearLayout.LayoutParams(0,px(heights[slot]),if(index==0)1.1f else 1f).apply{
      if(slot>0)marginStart=px(8)
     })
    }
    content.addView(podium,LinearLayout.LayoutParams(-1,px(226)).apply{bottomMargin=px(6)})
    for(index in 3 until items.size){
     val item=items[index]
     val isCurrent=item.optString("id")==roomId
     val row=LinearLayout(this@RoomActivity).apply{
      orientation=LinearLayout.HORIZONTAL
      gravity=Gravity.CENTER_VERTICAL
      layoutDirection=View.LAYOUT_DIRECTION_RTL
      setPadding(px(4),px(6),px(4),px(6))
      isClickable=true
      background=GradientDrawable().apply{cornerRadius=px(14).toFloat();setColor(if(isCurrent)Color.rgb(255,246,222) else Color.WHITE)}
     }
     row.addView(text((index+1).toString(),15f,soft,false),LinearLayout.LayoutParams(px(40),px(52)))
     row.addView(makeAvatar(item,Color.TRANSPARENT,0),LinearLayout.LayoutParams(px(48),px(48)))
     row.addView(text(ownerTitle(item),14.5f,ink,false).apply{
      gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
     },LinearLayout.LayoutParams(0,px(52),1f).apply{marginStart=px(10);marginEnd=px(10)})
     row.addView(pointsRow(item.optLong("trend_points"),13.5f),LinearLayout.LayoutParams(-2,px(52)))
     row.setOnClickListener{openItem(item)}
     content.addView(row,LinearLayout.LayoutParams(-1,px(62)).apply{bottomMargin=px(6)})
    }
    scroll.addView(content)
    root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    // ---- fixed bottom bar: this room (rank + points) ----
    val myIndex=items.indexOfFirst{it.optString("id")==roomId}
    val myItem=items.getOrNull(myIndex)
    root.addView(View(this@RoomActivity).apply{setBackgroundColor(Color.rgb(236,238,242))},LinearLayout.LayoutParams(-1,px(1)))
    val bottom=LinearLayout(this@RoomActivity).apply{
     orientation=LinearLayout.HORIZONTAL
     gravity=Gravity.CENTER_VERTICAL
     layoutDirection=View.LAYOUT_DIRECTION_RTL
     setPadding(px(18),0,px(18),0)
     setBackgroundColor(Color.WHITE)
    }
    bottom.addView(text(if(myIndex>=0)(myIndex+1).toString() else "+999",14f,soft,true),LinearLayout.LayoutParams(px(52),-1))
    bottom.addView(makeAvatar(myItem,Color.TRANSPARENT,0),LinearLayout.LayoutParams(px(44),px(44)))
    bottom.addView(text(myItem?.let{ownerTitle(it)}?:ownerNameView.text.toString(),14.5f,ink,true).apply{
     gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
    },LinearLayout.LayoutParams(0,-1,1f).apply{marginStart=px(10);marginEnd=px(10)})
    bottom.addView(pointsRow(if(myItem!=null)myItem.optLong("trend_points") else trendPoints,14f),LinearLayout.LayoutParams(-2,-1))
    root.addView(bottom,LinearLayout.LayoutParams(-1,px(66)))
    dialog.setContentView(root)
    dialog.setCanceledOnTouchOutside(true)
    dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setDimAmount(0.45f)
    dialog.show()
    dialog.window?.setLayout(-1,(resources.displayMetrics.heightPixels*0.80f).toInt())
    dialog.window?.setGravity(Gravity.BOTTOM)
   }catch(e:Exception){
    android.util.Log.e("BAK_TREND","Failed to open trend ranking",e)
    Toast.makeText(this@RoomActivity,"تعذر تحميل الترند، حاول مرة أخرى",Toast.LENGTH_SHORT).show()
   }
  }
 }

 override fun onDestroy(){trendRefreshJob?.cancel();trendRefreshJob=null;trendHeartbeatJob?.cancel();trendHeartbeatJob=null;trendAnimator?.cancel();trendAnimator=null;try{giftSound?.stop()}catch(_:Exception){};giftSound=null
  stopRoomMusic(restoreMic=false)
  watchPlayer?.release();watchPlayer=null
  speakerEventsJob?.cancel()
  activeSpeakerLevels.clear()
  speakerWaveViews.clear()
  disposeCameraRenderers()
  cameraTracks.clear()
  seatRefreshJob?.cancel()
  giftPoll?.cancel()
  // Navigation or Activity recreation must never be treated as an explicit room leave.
  // Keep the LiveKit session and server membership independent from this screen lifecycle.
  scope.cancel()
  super.onDestroy()
 }
}

/** موجة صوتية دائرية بنفسجية حول صورة المتحدث، تتفاعل مع مستوى الصوت القادم من LiveKit. */
private class CircularAudioWaveView(context:android.content.Context):View(context){
 private val density=resources.displayMetrics.density
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
  color=Color.rgb(64,224,255)
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
private class VectorIcon(private val kind:String,private val tint:Int,private val size:Int):Drawable(){
 // All glyphs are drawn on a 32x32 grid and scaled to the drawable bounds.
 private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=tint;style=Paint.Style.STROKE;strokeWidth=2.6f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
 private val f=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=tint;style=Paint.Style.FILL}
 private val s=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.argb(105,Color.red(tint),Color.green(tint),Color.blue(tint));style=Paint.Style.FILL}
 override fun draw(c:Canvas){
  val b=bounds
  if(b.width()<=0||b.height()<=0)return
  val save=c.save()
  c.translate(b.left.toFloat(),b.top.toFloat())
  c.scale(b.width().toFloat()/32f,b.height().toFloat()/32f)
  val m=16f
  fun rr(l:Float,t:Float,r:Float,bt:Float,rad:Float)=Path().apply{addRoundRect(RectF(l,t,r,bt),rad,rad,Path.Direction.CW)}
  fun oval(cx:Float,cy:Float,r:Float)=Path().apply{addCircle(cx,cy,r,Path.Direction.CW)}
  val pw=Paint(p).apply{strokeWidth=3.4f}
  when(kind){
   "flame"->{
    val glow=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL;color=Color.argb(55,255,120,40)}
    c.drawCircle(m,20f,13f,glow)
    val outer=Path().apply{
     moveTo(m,1.5f)
     cubicTo(m+3f,7f,m+10f,11f,m+10f,19.5f)
     cubicTo(m+10f,26.5f,m+5f,31f,m,31f)
     cubicTo(m-5f,31f,m-10f,26.5f,m-10f,19.5f)
     cubicTo(m-10f,15f,m-7f,12f,m-5f,9f)
     cubicTo(m-4f,12.5f,m-2.5f,14f,m-1f,14.5f)
     cubicTo(m-2f,10f,m-1f,5f,m,1.5f)
     close()
    }
    val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply{
     style=Paint.Style.FILL
     shader=LinearGradient(0f,1.5f,0f,31f,intArrayOf(Color.rgb(255,196,60),Color.rgb(255,96,40),Color.rgb(214,32,70)),floatArrayOf(0f,0.5f,1f),Shader.TileMode.CLAMP)
    }
    c.drawPath(outer,fill)
    val mid=Path().apply{
     moveTo(m,12f)
     cubicTo(m+2f,15f,m+6.5f,18f,m+6.5f,23f)
     cubicTo(m+6.5f,27.5f,m+3.5f,30f,m,30f)
     cubicTo(m-3.5f,30f,m-6.5f,27.5f,m-6.5f,23f)
     cubicTo(m-6.5f,19f,m-3f,17f,m,12f)
     close()
    }
    fill.shader=LinearGradient(0f,12f,0f,30f,Color.rgb(255,226,110),Color.rgb(255,140,40),Shader.TileMode.CLAMP)
    c.drawPath(mid,fill)
    val core=Path().apply{
     moveTo(m,19f)
     cubicTo(m+1.8f,21.5f,m+3.6f,23f,m+3.6f,25.5f)
     cubicTo(m+3.6f,28f,m+2f,29.5f,m,29.5f)
     cubicTo(m-2f,29.5f,m-3.6f,28f,m-3.6f,25.5f)
     cubicTo(m-3.6f,23f,m-1.8f,21.5f,m,19f)
     close()
    }
    fill.shader=LinearGradient(0f,19f,0f,29.5f,Color.rgb(255,255,225),Color.rgb(255,226,120),Shader.TileMode.CLAMP)
    c.drawPath(core,fill)
   }
   "crown"->{
    val crown=Path().apply{moveTo(4f,25f);lineTo(3f,10f);lineTo(10f,16f);lineTo(m,5f);lineTo(22f,16f);lineTo(29f,10f);lineTo(28f,25f);close()}
    val cf=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL;shader=LinearGradient(0f,5f,0f,26f,Color.rgb(255,236,150),Color.rgb(240,160,50),Shader.TileMode.CLAMP)}
    c.drawPath(crown,cf)
    cf.shader=null;cf.color=Color.rgb(255,248,210)
    c.drawCircle(m,9.5f,1.6f,cf);c.drawCircle(5f,12.5f,1.3f,cf);c.drawCircle(27f,12.5f,1.3f,cf)
   }
   "user"->{
    c.drawCircle(m,10.5f,5.5f,f)
    c.drawPath(Path().apply{moveTo(5f,28f);quadTo(5f,18.5f,m,18.5f);quadTo(27f,18.5f,27f,28f);close()},f)
   }
   "users"->{
    c.drawCircle(22.5f,10.5f,3.8f,s)
    c.drawPath(Path().apply{moveTo(16.5f,26.5f);quadTo(16.5f,18f,22.5f,18f);quadTo(29f,18f,29f,26.5f);close()},s)
    c.drawCircle(12.5f,10f,4.8f,f)
    c.drawPath(Path().apply{moveTo(3f,27f);quadTo(3f,17.5f,12.5f,17.5f);quadTo(22f,17.5f,22f,27f);close()},f)
   }
   "mic"->{
    c.drawRoundRect(RectF(11f,2.5f,21f,18.5f),5f,5f,f)
    c.drawArc(RectF(6.5f,8f,25.5f,25f),0f,180f,false,p)
    c.drawLine(m,25f,m,29f,p);c.drawLine(11f,29.5f,21f,29.5f,p)
   }
   "mic_off"->{
    c.drawRoundRect(RectF(11f,2.5f,21f,18.5f),5f,5f,s)
    c.drawArc(RectF(6.5f,8f,25.5f,25f),0f,180f,false,p)
    c.drawLine(m,25f,m,29f,p);c.drawLine(11f,29.5f,21f,29.5f,p)
    c.drawLine(5f,4f,27f,28.5f,pw)
   }
   "camera"->{
    val body=rr(2.5f,9f,29.5f,27f,5.5f).apply{op(rr(10.5f,5f,21.5f,11f,2.5f),Path.Op.UNION);op(oval(m,18f,6.3f),Path.Op.DIFFERENCE)}
    c.drawPath(body,f);c.drawCircle(m,18f,3.4f,f)
   }
   "camera_off"->{
    val body=rr(2.5f,9f,29.5f,27f,5.5f).apply{op(rr(10.5f,5f,21.5f,11f,2.5f),Path.Op.UNION);op(oval(m,18f,6.3f),Path.Op.DIFFERENCE)}
    c.drawPath(body,s);c.drawCircle(m,18f,3.4f,s)
    c.drawLine(5f,4f,27f,29f,pw)
   }
   "grid"->{
    c.drawRoundRect(RectF(4.5f,4.5f,15f,15f),3.2f,3.2f,f)
    c.drawRoundRect(RectF(17f,4.5f,27.5f,15f),3.2f,3.2f,s)
    c.drawRoundRect(RectF(4.5f,17f,15f,27.5f),3.2f,3.2f,s)
    c.drawRoundRect(RectF(17f,17f,27.5f,27.5f),3.2f,3.2f,f)
   }
   "share"->{
    c.drawLine(7f,16f,25f,7f,p);c.drawLine(7f,16f,25f,25f,p)
    c.drawCircle(7f,16f,4f,f);c.drawCircle(25f,7f,4f,f);c.drawCircle(25f,25f,4f,f)
   }
   "leave"->{
    c.drawPath(Path().apply{moveTo(13f,5f);lineTo(7f,5f);quadTo(4.5f,5f,4.5f,7.5f);lineTo(4.5f,24.5f);quadTo(4.5f,27f,7f,27f);lineTo(13f,27f)},p)
    c.drawLine(12f,16f,23f,16f,p)
    c.drawPath(Path().apply{moveTo(21.5f,9.5f);lineTo(29.5f,16f);lineTo(21.5f,22.5f);close()},f)
   }
   "gift"->{
    val box=Path().apply{addPath(rr(5f,14.5f,27f,28.5f,3f));addPath(rr(3f,9.5f,29f,15.5f,2.5f))}
    box.op(Path().apply{addRect(14.4f,9f,17.6f,29f,Path.Direction.CW)},Path.Op.DIFFERENCE)
    c.drawPath(box,f)
    c.drawPath(Path().apply{moveTo(m,9.5f);quadTo(9f,0.5f,8.5f,5.5f);quadTo(8.5f,9.5f,m,9.5f)},p)
    c.drawPath(Path().apply{moveTo(m,9.5f);quadTo(23f,0.5f,23.5f,5.5f);quadTo(23.5f,9.5f,m,9.5f)},p)
   }
   "music"->{
    c.drawPath(Path().apply{moveTo(12f,6f);lineTo(27f,3f);lineTo(27f,10f);lineTo(12f,13f);close()},f)
    c.drawRoundRect(RectF(10.5f,6f,13.5f,23.5f),1.5f,1.5f,f)
    c.drawRoundRect(RectF(24.5f,3f,27.5f,20.5f),1.5f,1.5f,f)
    c.drawOval(RectF(4.5f,20.5f,13.5f,28f),f)
    c.drawOval(RectF(18.5f,17.5f,27.5f,25f),f)
   }
   "play"->{
    val tv=rr(2.5f,6.5f,29.5f,25.5f,6.5f)
    tv.op(Path().apply{moveTo(13f,11f);lineTo(22.5f,16f);lineTo(13f,21f);close()},Path.Op.DIFFERENCE)
    c.drawPath(tv,f)
    c.drawRoundRect(RectF(10f,27.5f,22f,29.5f),1f,1f,s)
   }
   "image"->{
    c.drawRoundRect(RectF(3.5f,5f,28.5f,27f),5.5f,5.5f,p)
    c.drawCircle(21.5f,12f,2.8f,f)
    c.drawPath(Path().apply{moveTo(6f,25f);lineTo(12f,16.5f);lineTo(17f,22f);lineTo(20.5f,18.5f);lineTo(26f,25f);close()},f)
   }
   "chat"->{
    val bub=rr(3f,4.5f,29f,23f,7f).apply{op(Path().apply{moveTo(9f,21f);lineTo(8f,29.5f);lineTo(17f,22.5f);close()},Path.Op.UNION)}
    for(x in listOf(10f,16f,22f))bub.op(oval(x,13.7f,1.8f),Path.Op.DIFFERENCE)
    c.drawPath(bub,f)
   }
   "pause"->{c.drawRoundRect(RectF(9f,6f,14f,26f),2f,2f,f);c.drawRoundRect(RectF(18f,6f,23f,26f),2f,2f,f)}
   "volume-2"->{
    c.drawPath(Path().apply{moveTo(4f,12f);lineTo(10f,12f);lineTo(17f,6f);lineTo(17f,26f);lineTo(10f,20f);lineTo(4f,20f);close()},f)
    c.drawArc(RectF(12f,10f,24f,22f),-50f,100f,false,p)
    c.drawArc(RectF(10f,5f,28f,27f),-50f,100f,false,p)
   }
   "square"->{c.drawRoundRect(RectF(8f,8f,24f,24f),3.5f,3.5f,f)}
   "plus"->{c.drawLine(m,6f,m,26f,pw);c.drawLine(6f,m,26f,m,pw)}
   "close"->{c.drawLine(8f,8f,24f,24f,pw);c.drawLine(24f,8f,8f,24f,pw)}
   "ban"->{c.drawCircle(m,m,11f,pw);c.drawLine(8.3f,8.3f,23.7f,23.7f,pw)}
   "hand"->{
    c.drawRoundRect(RectF(8f,14f,24f,29f),5f,5f,f)
    for((i,x) in listOf(8f,12.5f,17f,21.5f).withIndex())c.drawRoundRect(RectF(x,if(i==1||i==2)4f else 7f,x+3f,17f),1.5f,1.5f,f)
   }
   else->{c.drawArc(RectF(5f,5f,27f,27f),30f,290f,false,p);c.drawLine(21f,5f,28f,6f,p);c.drawLine(28f,6f,27f,13f,p)}
  }
  c.restoreToCount(save)
 }
 override fun setAlpha(a:Int){p.alpha=a;f.alpha=a;s.alpha=a*105/255}
 override fun setColorFilter(cf:ColorFilter?){p.colorFilter=cf;f.colorFilter=cf;s.colorFilter=cf}
 @Deprecated("Deprecated in Android")
 override fun getOpacity()=PixelFormat.TRANSLUCENT
 override fun getIntrinsicWidth()=size
 override fun getIntrinsicHeight()=size
}


/** Animated red chat bubble background; message text is drawn by live TextViews above it. */
private class ChatBubbleGifDrawable(
 private val context:android.content.Context,
 resourceId:Int
):Drawable(){
 private val movie:Movie?=try{
  context.resources.openRawResource(resourceId).use{Movie.decodeStream(it)}
 }catch(_:Exception){null}
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
 private var startedAt=0L
 private val frame=Runnable{invalidateSelf()}
 override fun draw(canvas:Canvas){
  val gif=movie?:return
  val b=bounds
  if(b.width()<=0||b.height()<=0)return
  if(startedAt==0L)startedAt=android.os.SystemClock.uptimeMillis()
  val duration=gif.duration().takeIf{it>0}?:1000
  gif.setTime(((android.os.SystemClock.uptimeMillis()-startedAt)%duration).toInt())
  val sx=b.width().toFloat()/gif.width().coerceAtLeast(1)
  val sy=b.height().toFloat()/gif.height().coerceAtLeast(1)
  val scale=maxOf(sx,sy)
  val save=canvas.save()
  canvas.clipRect(b)
  canvas.translate(b.left+(b.width()-gif.width()*scale)/2f,b.top+(b.height()-gif.height()*scale)/2f)
  canvas.scale(scale,scale)
  gif.draw(canvas,0f,0f,paint)
  canvas.restoreToCount(save)
  scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+33L)
 }
 override fun setAlpha(alpha:Int){paint.alpha=alpha}
 override fun setColorFilter(colorFilter:ColorFilter?){paint.colorFilter=colorFilter}
 @Deprecated("Deprecated in Android")
 override fun getOpacity()=PixelFormat.TRANSLUCENT
 override fun getIntrinsicWidth()=movie?.width()?:-1
 override fun getIntrinsicHeight()=movie?.height()?:-1
 override fun setVisible(visible:Boolean,restart:Boolean):Boolean{
  if(visible&&restart)startedAt=android.os.SystemClock.uptimeMillis()
  if(!visible)unscheduleSelf(frame)
  return super.setVisible(visible,restart)
 }
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

/** Three animated blue bars (equalizer) shown next to the trending users title. */
private class EqualizerBarsView(context:android.content.Context):View(context){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(0,170,255);style=Paint.Style.FILL}
 private var phase=0f
 private val animator=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
  duration=1000L
  repeatCount=android.animation.ValueAnimator.INFINITE
  interpolator=android.view.animation.LinearInterpolator()
  addUpdateListener{phase=it.animatedValue as Float;invalidate()}
 }
 override fun onAttachedToWindow(){super.onAttachedToWindow();if(!animator.isStarted)animator.start()}
 override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
 override fun onDraw(canvas:Canvas){
  super.onDraw(canvas)
  val w=width.toFloat();val h=height.toFloat()
  if(w<=0f||h<=0f)return
  val barW=w*0.22f
  val gap=(w-barW*3f)/2f
  for(i in 0 until 3){
   val wave=(Math.sin(phase*2.0*Math.PI+i*2.1)*0.5+0.5).toFloat()
   val barH=h*(0.35f+0.65f*wave)
   val left=i*(barW+gap)
   canvas.drawRoundRect(RectF(left,h-barH,left+barW,h),barW/2f,barW/2f,paint)
  }
 }
}