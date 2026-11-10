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
 private var handRaised=false; private var isOwner=false
 private var roomId=""; private var authToken=""; private var currentUserId=""
 private var seats=JSONArray(); private var hands=JSONArray()
 private val bg=Color.rgb(16,10,28); private val purple=Color.rgb(164,76,239); private val gold=Color.rgb(255,195,83); private val mutedText=Color.rgb(207,190,220)
 private lateinit var status:TextView; private lateinit var grid:LinearLayout
 private lateinit var mic:LinearLayout
 private lateinit var chatPanel:LinearLayout
 private var chatPoll:Job?=null
 private lateinit var chatMessages:LinearLayout
 private lateinit var chatScroll:ScrollView
 private lateinit var chatInput:EditText
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER;if(b)typeface=android.graphics.Typeface.DEFAULT_BOLD}
 private fun panel(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),Color.rgb(111,67,145))}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
  roomId=intent.getStringExtra("room_id").orEmpty()
  authToken=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null).orEmpty()
  val page=LinearLayout(this).apply{orientation=1;minimumHeight=resources.displayMetrics.heightPixels;setPadding(dp(15),dp(12),dp(15),dp(12));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(38,15,57),bg,Color.rgb(28,9,37)))}
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(label("MARIAM LIVE",17f,gold,true),LinearLayout.LayoutParams(0,dp(42),1f))
  top.addView(action("تحديث","refresh",purple,gold){loadSeats()},LinearLayout.LayoutParams(dp(85),dp(42)));page.addView(top)
  page.addView(label(intent.getStringExtra("room_name")?: "غرفة صوتية",23f,Color.WHITE,true).apply{setPadding(0,dp(12),0,dp(4))})
  page.addView(label("غرفة صوتية مباشرة  •  8 مقاعد",12f,Color.LTGRAY).apply{setPadding(0,0,0,dp(14))})
  val host=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));background=panel(Color.rgb(57,29,78),22)}
  host.addView(label("M",32f,gold,true).apply{background=GradientDrawable().apply{shape=1;setColor(Color.rgb(85,43,113));setStroke(dp(3),gold)}},LinearLayout.LayoutParams(dp(68),dp(68)))
  host.addView(label("صاحب الغرفة",15f,Color.WHITE,true));host.addView(label("المضيف • المقعد الرئيسي",11f,gold))
  page.addView(host,LinearLayout.LayoutParams(-1,dp(118)))
  page.addView(label("مقاعد المتحدثين",17f,Color.WHITE,true).apply{setPadding(0,dp(18),0,dp(8))})
  grid=LinearLayout(this).apply{orientation=1};page.addView(grid)
   page.addView(buildInlineChat(),LinearLayout.LayoutParams(-1,dp(245)).apply{topMargin=dp(12)})
  status=label("جارٍ الاتصال...",12f,Color.LTGRAY).apply{setPadding(0,dp(12),0,dp(8))};page.addView(status)
  val bar=LinearLayout(this).apply{gravity=Gravity.CENTER;orientation=0;setPadding(0,dp(4),0,dp(4))}
    val share=action("مشاركة","share",Color.rgb(65,58,105),Color.rgb(117,103,190)){val i=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"انضم إلى غرفتي في Mariam Live")};startActivity(Intent.createChooser(i,"مشاركة"))}
  mic=action("إيقاف المايك","mic",Color.rgb(83,48,125),Color.rgb(169,91,221)){toggleMic()}
  mic.visibility=View.GONE
  val gift=action("الهدايا","gift",Color.rgb(111,43,104),Color.rgb(190,76,151)){Toast.makeText(this,"متجر الهدايا والمحفظة ضمن المرحلة التالية",Toast.LENGTH_SHORT).show()}
  val exit=action("مغادرة","leave",Color.rgb(125,39,66),Color.rgb(208,70,103)){leaveRoom()}
  listOf(share,mic,gift,exit).forEach{bar.addView(it,LinearLayout.LayoutParams(0,dp(82),1f).apply{setMargins(dp(3),0,dp(3),0)})};page.addView(bar)
  setContentView(ScrollView(this).apply{isFillViewport=true;setBackgroundColor(bg);addView(page)})
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
   setPadding(dp(12),dp(10),dp(12),dp(10))
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
    intArrayOf(Color.rgb(54,29,75),Color.rgb(29,16,42))).apply{
     cornerRadius=dp(20).toFloat()
     setStroke(dp(1),Color.rgb(111,67,145))
   }
   chatPanel=this
   val header=LinearLayout(this@RoomActivity).apply{gravity=Gravity.CENTER_VERTICAL}
   header.addView(label("دردشة الغرفة",15f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(28),1f))
   header.addView(label("● مباشر",10f,Color.rgb(113,220,166),true))
   addView(header,LinearLayout.LayoutParams(-1,-2))
   chatMessages=LinearLayout(this@RoomActivity).apply{orientation=1}
   chatScroll=ScrollView(this@RoomActivity).apply{
    isFillViewport=true
    overScrollMode=View.OVER_SCROLL_NEVER
    addView(chatMessages,android.widget.FrameLayout.LayoutParams(-1,-2))
   }
   addView(chatScroll,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(6)})
   val composer=LinearLayout(this@RoomActivity).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
   chatInput=EditText(this@RoomActivity).apply{
    hint="اكتب رسالتك...";textSize=13f;maxLines=2
    setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
    setPadding(dp(12),dp(7),dp(12),dp(7))
    background=GradientDrawable().apply{
     setColor(Color.rgb(57,39,70));cornerRadius=dp(16).toFloat()
     setStroke(dp(1),Color.rgb(137,89,190))
    }
   }
   composer.addView(chatInput,LinearLayout.LayoutParams(0,dp(43),1f).apply{marginEnd=dp(7)})
   val send=TextView(this@RoomActivity).apply{
    text="إرسال";textSize=13f;gravity=Gravity.CENTER
    typeface=android.graphics.Typeface.DEFAULT_BOLD
    setTextColor(Color.WHITE);setPadding(dp(13),0,dp(13),0)
    background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
     intArrayOf(Color.rgb(178,105,226),Color.rgb(111,55,160))).apply{cornerRadius=dp(15).toFloat()}
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
       setColor(if(mine)Color.rgb(91,48,122) else Color.rgb(54,42,65))
       cornerRadius=dp(14).toFloat()
       setStroke(dp(1),if(mine)Color.rgb(174,119,214) else Color.rgb(95,74,110))
      }
     }
     bubble.addView(TextView(this@RoomActivity).apply{
      text=sender
      textSize=11f
      setTextColor(if(mine)gold else Color.rgb(204,164,235))
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

 private fun loadIdentity(){scope.launch{try{val r=withContext(Dispatchers.IO){AuthApi.get("/api/auth/me",authToken)};currentUserId=(r.body.optJSONObject("user")?:r.body).optString("id")}catch(_:Exception){};loadSeats()}}
 private fun emptySeats(){seats=JSONArray();for(i in 1..8)seats.put(JSONObject().put("seat_number",i));drawSeats()}
 private fun loadSeats(){
  if(roomId.isBlank())return
  scope.launch{
   try{
    val r=withContext(Dispatchers.IO){AuthApi.get("/api/voice-rooms/$roomId/seats",authToken)}
    if(r.status !in 200..299){status.text=r.body.optString("error","تعذر تحميل المقاعد");return@launch}
    seats=r.body.optJSONArray("seats")?:JSONArray()
    hands=r.body.optJSONArray("handRaises")?:JSONArray()
    isOwner=currentUserId.isNotBlank()&&currentUserId==r.body.optString("ownerId")
    isSeated=(0 until seats.length()).any{seats.optJSONObject(it)?.optString("user_id")==currentUserId&&currentUserId.isNotBlank()}
mic.visibility=if(isSeated)View.VISIBLE else View.GONE
    drawSeats()
   }catch(_:Exception){status.text="تعذر تحديث الغرفة"}
  }
 }
 private fun drawSeats(){grid.removeAllViews();for(row in 0..1){val line=LinearLayout(this);for(col in 0..3){val i=row*4+col;val s=seats.optJSONObject(i)?:JSONObject();val uid=s.optString("user_id").takeIf{it.isNotBlank()&&it!="null"};val name=s.optString("display_name").takeIf{it.isNotBlank()&&it!="null"}?:if(uid==null)"مقعد فارغ" else "متحدث";val tile=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(2),dp(8),dp(2),dp(7));background=panel(if(uid==null)Color.rgb(34,21,49) else Color.rgb(65,34,85),15)}
  tile.addView(label(if(uid==null)"+" else name.take(1).uppercase(),24f,if(uid==null)purple else gold,true).apply{background=GradientDrawable().apply{shape=1;setColor(Color.rgb(53,31,72));setStroke(dp(2),if(uid==null)purple else gold)}},LinearLayout.LayoutParams(dp(50),dp(50)))
  tile.addView(label(name,10f,Color.WHITE,true));tile.addView(label(if(s.optBoolean("is_muted"))"مكتوم" else if(uid==null)"متاح" else "على المايك",9f,Color.LTGRAY))
  tile.setOnClickListener{seatClick(s,i+1,uid,name)}
  line.addView(tile,LinearLayout.LayoutParams(0,dp(112),1f).apply{setMargins(dp(2),dp(3),dp(2),dp(5))})
 };grid.addView(line)}}
 private fun seatClick(s:JSONObject,n:Int,uid:String?,name:String){
  if(uid==null){claimSeat(n);return}
  if(uid==currentUserId){Toast.makeText(this,"أنت جالس على هذا المقعد",Toast.LENGTH_SHORT).show();return}
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
   } catch(e:Exception) {
    status.text=e.message?:"تعذر الاتصال"
    stopService(service)
   }
  }
 }
 private fun toggleMic(){if(!connected)return;scope.launch{try{microphoneEnabled=!microphoneEnabled;room.localParticipant.setMicrophoneEnabled(microphoneEnabled);status.text=if(microphoneEnabled)"المايك يعمل" else "المايك متوقف"}catch(_:Exception){}}}
 @Deprecated("Deprecated in Android") override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==4102){if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)connect() else status.text="يجب السماح باستخدام المايك"}}
 private fun leaveRoom(){if(leaving)return;leaving=true;scope.launch{try{if(connected)room.disconnect();stopService(Intent(this@RoomActivity,VoiceForegroundService::class.java));withContext(Dispatchers.IO){AuthApi.post("/api/voice-rooms/$roomId/leave",JSONObject(),authToken)}}catch(_:Exception){}finally{finish()}}}
 override fun onBackPressed(){leaveRoom()}
 override fun onDestroy(){scope.cancel();super.onDestroy()}
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
