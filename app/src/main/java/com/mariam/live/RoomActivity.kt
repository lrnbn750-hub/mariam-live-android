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
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.*

class RoomActivity : Activity() {
 private lateinit var room: Room
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 private var connected=false; private var leaving=false; private var microphoneEnabled=true; private var isSeated=false
 private var handRaised=false; private var isOwner=false; private var ownerSeatSetupDone=false
 private var roomId=""; private var authToken=""; private var currentUserId=""
 private var seats=JSONArray(); private var hands=JSONArray()
 private val bg=Color.rgb(13,14,31); private val purple=Color.rgb(151,112,245); private val gold=Color.rgb(244,197,106); private val mutedText=Color.rgb(181,177,202)
 private lateinit var status:TextView; private lateinit var grid:LinearLayout
 private lateinit var roomPage:LinearLayout
 private var loadedBackgroundUrl=""
 private var seatRefreshJob:Job?=null
 private val avatarBitmaps=HashMap<String,Bitmap>()
 private val avatarLoading=HashSet<String>()
 private lateinit var mic:LinearLayout
 private lateinit var chatPanel:LinearLayout
 private var chatPoll:Job?=null
 private lateinit var chatMessages:LinearLayout
 private lateinit var chatScroll:ScrollView
 private lateinit var chatInput:EditText
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER;typeface=android.graphics.Typeface.create("sans-serif",if(b)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)}
 private fun panel(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),Color.rgb(91,72,145))}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
  roomId=intent.getStringExtra("room_id").orEmpty()
  authToken=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null).orEmpty()
  roomPage=LinearLayout(this).apply{orientation=1;setPadding(dp(15),dp(12),dp(15),dp(12));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(28,23,55),bg,Color.rgb(13,14,31)))}
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(label("MARIAM LIVE  ✦",17f,gold,true),LinearLayout.LayoutParams(0,dp(42),1f))
  top.addView(action("تحديث","refresh",purple,gold){loadSeats()},LinearLayout.LayoutParams(dp(85),dp(42)))
  top.addView(label("×",30f,Color.WHITE,true).apply{
   setOnClickListener{leaveRoom()}
   background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(91,45,75));setStroke(dp(1),Color.rgb(198,105,151))}
  },LinearLayout.LayoutParams(dp(42),dp(42)).apply{marginStart=dp(8)})
  roomPage.addView(top)
  roomPage.addView(label(intent.getStringExtra("room_name")?: "غرفة صوتية",23f,Color.WHITE,true).apply{setPadding(0,dp(12),0,dp(4))})
  grid=LinearLayout(this).apply{orientation=1}
  roomPage.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(26)})
   roomPage.addView(buildInlineChat(),LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(12)})
  status=label("",11f,Color.LTGRAY).apply{visibility=View.GONE}
  mic=action("المايك","mic",Color.rgb(91,65,160),Color.rgb(188,153,255)){toggleMic()}
  mic.visibility=View.GONE
  val roomRoot=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg)}
  roomRoot.addView(roomPage,LinearLayout.LayoutParams(-1,-1))
  setContentView(roomRoot)
  emptySeats()
  if(roomId.isBlank()||authToken.isBlank()){status.text="بيانات الدخول أو الغرفة غير متوفرة";return}
  room=LiveKit.create(applicationContext);loadIdentity()
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)connect() else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),4102)
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
    setImageDrawable(VectorIcon(icon,Color.WHITE,dp(25)))
    setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN)
    scaleType=ImageView.ScaleType.FIT_CENTER
   }
   addView(iv,FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER))
  }
  addView(iconWrap,LinearLayout.LayoutParams(dp(47),dp(47)).apply{bottomMargin=dp(7)})
  addView(label(title,11f,Color.WHITE,true))
  setOnClickListener{click()}
 }

 private fun buildInlineChat()=LinearLayout(this).apply{
   orientation=1
   setPadding(0,dp(8),0,0)
   background=null
   chatPanel=this
   chatMessages=LinearLayout(this@RoomActivity).apply{orientation=1}
   chatScroll=ScrollView(this@RoomActivity).apply{
    isFillViewport=true
    overScrollMode=View.OVER_SCROLL_NEVER
    addView(chatMessages,android.widget.FrameLayout.LayoutParams(-1,-2))
   }
   addView(chatScroll,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(6)})
   val composer=LinearLayout(this@RoomActivity).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
   chatInput=EditText(this@RoomActivity).apply{
    hint="اكتب رسالتك...";textSize=13f;maxLines=1;setSingleLine(true)
    imeOptions=EditorInfo.IME_ACTION_SEND
    inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    setOnEditorActionListener{_,actionId,event->
     val enterPressed=event?.keyCode==android.view.KeyEvent.KEYCODE_ENTER && event.action==android.view.KeyEvent.ACTION_DOWN
     if(actionId==EditorInfo.IME_ACTION_SEND || enterPressed){sendRoomMessage();true}else false
    }
    setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
    setPadding(dp(12),dp(7),dp(12),dp(7))
    background=GradientDrawable().apply{
     setColor(Color.argb(18,111,231,220));cornerRadius=dp(7).toFloat()
     setStroke(dp(1),Color.argb(115,111,231,220))
    }
   }
   composer.addView(chatInput,LinearLayout.LayoutParams(0,dp(43),1f).apply{marginEnd=dp(7)})
   val send=TextView(this@RoomActivity).apply{
    text="إرسال";textSize=13f;gravity=Gravity.CENTER
    typeface=android.graphics.Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE);setPadding(dp(13),0,dp(13),0)
    background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
     intArrayOf(Color.rgb(151,112,245),Color.rgb(105,73,193))).apply{cornerRadius=dp(15).toFloat()}
    setOnClickListener{sendRoomMessage()}
   }
   composer.addView(send,LinearLayout.LayoutParams(-2,dp(42)))
   addView(composer,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})
   loadRoomMessages()
   chatPoll?.cancel()
   chatPoll=scope.launch{
    while(isActive){delay(3000);loadRoomMessages()}
   }
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
      chatMessages.addView(label(result.body.optString("error","تعذر تحميل الرسائل"),13f,Color.LTGRAY))
     }
     return@launch
    }
    val messages=result.body.optJSONArray("messages")?:JSONArray()
    chatMessages.removeAllViews()
    if(messages.length()==0){
     chatMessages.addView(label("لا توجد رسائل بعد، كن أول من يرسل رسالة",13f,Color.LTGRAY).apply{
      setPadding(dp(8),dp(22),dp(8),dp(22))
     })
    }
    for(i in 0 until messages.length()){
     val item=messages.optJSONObject(i)?:continue
     val sender=item.optString("display_name").ifBlank{item.optString("username","مستخدم")}
     val body=item.optString("message")
     val mine=item.optString("user_id")==currentUserId
     val bubble=LinearLayout(this@RoomActivity).apply{
      orientation=1
      setPadding(dp(11),dp(7),dp(11),dp(7))
      background=GradientDrawable().apply{
       setColor(if(mine)Color.rgb(91,65,160) else Color.rgb(42,34,76))
       cornerRadius=dp(14).toFloat()
       setStroke(dp(1),if(mine)Color.rgb(151,112,245) else Color.rgb(91,72,145))
      }
     }
     bubble.addView(TextView(this@RoomActivity).apply{
      text=sender
      textSize=11f
      setTextColor(if(mine)gold else Color.rgb(161,220,225))
      typeface=android.graphics.Typeface.DEFAULT_BOLD
     })
     bubble.addView(TextView(this@RoomActivity).apply{
      text=body
      textSize=14f
      setTextColor(Color.WHITE)
     })
     val line=LinearLayout(this@RoomActivity).apply{
      orientation=0
      gravity=if(mine)Gravity.END else Gravity.START
      setPadding(0,dp(3),0,dp(3))
      addView(bubble,LinearLayout.LayoutParams(-2,-2))
     }
     chatMessages.addView(line,LinearLayout.LayoutParams(-1,-2))
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
    mic.visibility=if(isSeated)View.VISIBLE else View.GONE
    drawSeats()
   }catch(_:Exception){status.text="تعذر تحديث الغرفة"}
  }
 }

 private fun applyRoomBackground(url:String){
  loadedBackgroundUrl=url
  if(url.isBlank()){
   roomPage.background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(28,23,55),bg,Color.rgb(13,14,31)))
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
     val photo=android.graphics.drawable.BitmapDrawable(resources,bitmap)
     photo.alpha=255
     roomPage.background=android.graphics.drawable.LayerDrawable(arrayOf(photo,GradientDrawable().apply{setColor(Color.argb(82,5,7,20))}))
    }
   }catch(_:Exception){
    if(loadedBackgroundUrl==url) roomPage.background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(28,23,55),bg,Color.rgb(13,14,31)))
   }
  }
 }

 private fun drawSeats(){
  grid.removeAllViews()
  for(row in 0..1){
   val line=LinearLayout(this).apply{orientation=0}
   for(col in 0..3){
    val i=row*4+col
    val s=seats.optJSONObject(i)?:JSONObject()
    val uid=s.optString("user_id").takeIf{it.isNotBlank()&&it!="null"}
    val name=s.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}?:if(uid==null)"مقعد فارغ" else "متحدث"
    val occupied=uid!=null
    // ألوان متناسقة ومختلفة لكل مقعد
    val seatColors=arrayOf(
     intArrayOf(Color.rgb(91,65,160),Color.rgb(43,34,91)),   // بنفسجي
     intArrayOf(Color.rgb(28,119,135),Color.rgb(24,55,83)),  // تركوازي
     intArrayOf(Color.rgb(153,75,117),Color.rgb(75,38,82)),  // وردي
     intArrayOf(Color.rgb(54,94,159),Color.rgb(31,43,82)),   // أزرق
     intArrayOf(Color.rgb(151,104,48),Color.rgb(74,48,43)), // ذهبي
     intArrayOf(Color.rgb(65,125,103),Color.rgb(29,65,65)), // أخضر
     intArrayOf(Color.rgb(117,76,153),Color.rgb(55,39,91)), // بنفسجي فاتح
     intArrayOf(Color.rgb(153,83,65),Color.rgb(76,42,54))   // مرجاني
    )
    val seatColor=seatColors[i%seatColors.size]
    val tile=LinearLayout(this).apply{
     orientation=1;gravity=Gravity.CENTER
     setPadding(dp(3),dp(9),dp(3),dp(8))
     background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      if(occupied)seatColor else intArrayOf(
       Color.rgb((Color.red(seatColor[0])*0.42f).toInt(),(Color.green(seatColor[0])*0.42f).toInt(),(Color.blue(seatColor[0])*0.42f).toInt()),
       Color.rgb((Color.red(seatColor[1])*0.62f).toInt(),(Color.green(seatColor[1])*0.62f).toInt(),(Color.blue(seatColor[1])*0.62f).toInt())
      )).apply{
       cornerRadius=dp(18).toFloat()
       setStroke(dp(1),if(occupied)Color.rgb(226,205,255) else Color.rgb(
        (Color.red(seatColor[0])*0.85f).toInt(),(Color.green(seatColor[0])*0.85f).toInt(),(Color.blue(seatColor[0])*0.85f).toInt()))
      }
     elevation=dp(3).toFloat()
     isClickable=true;isFocusable=true
    }
    val avatar=FrameLayout(this).apply{
     background=GradientDrawable().apply{
      shape=1
      setColor(if(occupied)Color.rgb(104,73,161) else Color.rgb(42,34,76))
      setStroke(dp(2),if(occupied)gold else purple)
     }
     addView(label(if(occupied)name.take(1).uppercase() else "+",
      if(occupied)21f else 25f,if(occupied)gold else purple,true),
      FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
    }
    val avatarUrl=s.optString("avatar_url").takeIf{it.isNotBlank()&&it!="null"}.orEmpty()
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
    // إطار الصورة أكبر قليلاً، وإطار التاج الملكي يظهر حصراً لمستوى 100.
    val avatarStage=FrameLayout(this).apply{
     clipChildren=false;clipToPadding=false
     addView(avatar,FrameLayout.LayoutParams(dp(56),dp(56),Gravity.CENTER))
    }
    val totalXp=s.optLong("total_xp",0L)
    if(occupied && totalXp>=980100L){
     val royal=RoyalSeatGifView(this)
     avatarStage.addView(royal,FrameLayout.LayoutParams(dp(78),dp(78),Gravity.CENTER))
     royal.start()
    }
    tile.addView(avatarStage,LinearLayout.LayoutParams(dp(80),dp(80)).apply{bottomMargin=dp(2)})
    tile.addView(label(name,10f,Color.WHITE,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END})
    val caption=if(s.optBoolean("is_muted"))"مكتوم" else if(!occupied)"اضغط للجلوس" else "على المايك"
    tile.addView(label(caption,9f,if(!occupied)Color.rgb(181,177,202) else Color.rgb(244,197,106)))
    tile.setOnClickListener{seatClick(s,i+1,uid,name)}
    line.addView(tile,LinearLayout.LayoutParams(0,dp(132),1f).apply{
     setMargins(dp(3),dp(4),dp(3),dp(4))
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
 private var membershipWatch:Job?=null
 private fun startRoomMembershipWatch(){
  membershipWatch?.cancel()
  membershipWatch=scope.launch{
   while(isActive && connected && !leaving){
    delay(4500)
    loadSeats()
   }
  }
 }
 private fun toggleMic(){if(!connected)return;scope.launch{try{microphoneEnabled=!microphoneEnabled;room.localParticipant.setMicrophoneEnabled(microphoneEnabled);status.text=if(microphoneEnabled)"المايك يعمل" else "المايك متوقف"}catch(_:Exception){}}}
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
  seatRefreshJob?.cancel()
  scope.cancel()
  super.onDestroy()
 }
}

/** أيقونات Vector مخصصة مرسومة على Canvas بدون رموز Emoji. */
private class VectorIcon(private val kind:String,private val color:Int,private val size:Int):Drawable(){
 private val p=Paint(3).apply{this.color=color;style=Paint.Style.STROKE;strokeWidth=size*.09f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
 override fun draw(c:Canvas){val b=bounds;val save=c.save();c.translate(b.left.toFloat(),b.top.toFloat());c.scale(b.width().toFloat()/size,b.height().toFloat()/size);val m=size/2f
 when(kind){
 "mic"->{c.drawRoundRect(m-4f,2f,m+4f,17f,5f,5f,p);c.drawArc(RectF(m-10f,8f,m+10f,26f),0f,180f,false,p);c.drawLine(m,26f,m,30f,p);c.drawLine(m-6f,30f,m+6f,30f,p)}
 "hand"->{val q=Path().apply{moveTo(10f,27f);lineTo(7f,19f);lineTo(7f,15f);lineTo(11f,18f);lineTo(11f,7f);quadTo(15f,4f,16f,8f);lineTo(16f,16f);lineTo(19f,10f);quadTo(23f,9f,22f,14f);lineTo(22f,18f);lineTo(25f,14f);quadTo(29f,14f,27f,20f);lineTo(24f,27f);close()};c.drawPath(q,p)}
 "chat"->{c.drawRoundRect(3f,5f,29f,23f,5f,5f,p);c.drawLine(9f,23f,7f,29f,p);for(x in listOf(10f,16f,22f))c.drawCircle(x,14f,1f,p)}
 "share"->{c.drawLine(7f,16f,25f,7f,p);c.drawLine(7f,16f,25f,25f,p);c.drawCircle(6f,16f,3f,p);c.drawCircle(26f,6f,3f,p);c.drawCircle(26f,26f,3f,p)}
 "gift"->{c.drawRect(4f,13f,28f,28f,p);c.drawRect(2f,10f,30f,15f,p);c.drawLine(16f,10f,16f,28f,p);c.drawOval(RectF(6f,3f,16f,12f),p);c.drawOval(RectF(16f,3f,26f,12f),p)}
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
