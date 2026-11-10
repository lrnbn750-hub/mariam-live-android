package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class HomeActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(4)
    private val purple = Color.rgb(112,75,246)
    private val ink = Color.rgb(35,34,53)
    private val muted = Color.rgb(139,139,153)
    private lateinit var grid: GridLayout
    private lateinit var roomStatus: TextView
    private var filterMode = "online"
    private var searchQuery = ""
    private var pendingRoomName = ""
    private val roomImagePicker = 7314
    private data class RoomItem(val id:String,val name:String,val count:Int,val max:Int,val imageUrl:String?)
    private val liveRooms= mutableListOf<RoomItem>()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun label(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{
        text=s;textSize=n;setTextColor(c);gravity=Gravity.CENTER_VERTICAL
        if(b)typeface=Typeface.DEFAULT_BOLD
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val cream=Color.rgb(255,255,249)
        window.statusBarColor=cream;window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(Color.rgb(250,250,252))}
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(Color.rgb(250,250,252))}
        val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;addView(content)}
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val header=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(7),dp(12),dp(7))
            setBackgroundColor(cream)
        }
        fun topTab(title:String,mode:String)=TextView(this).apply{
            text=title;textSize=15f;gravity=Gravity.CENTER
            setTextColor(if(filterMode==mode)ink else muted)
            typeface=if(filterMode==mode)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setSingleLine(true)
            setOnClickListener{filterMode=mode;buildCards()}
        }
        header.addView(topTab("متصل","online"),LinearLayout.LayoutParams(0,dp(48),1f))
        header.addView(topTab("شعبي","popular"),LinearLayout.LayoutParams(0,dp(48),1f))
        val searchButton=TextView(this).apply{
            text="⌕";textSize=32f;gravity=Gravity.CENTER;setTextColor(ink)
            setPadding(dp(5),0,dp(5),0)
            setOnClickListener{showSearchDialog()}
        }
        header.addView(searchButton,LinearLayout.LayoutParams(dp(44),dp(48)))
        content.addView(header,LinearLayout.LayoutParams(-1,dp(60)))
        val sectionTitle=label("الغرف الاجتماعية",19f,ink,true).apply{setPadding(dp(18),dp(15),dp(18),dp(10))}
        content.addView(sectionTitle,LinearLayout.LayoutParams(-1,dp(54)))
        grid=GridLayout(this).apply{columnCount=2;useDefaultMargins=false;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        content.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(10),0,dp(10),dp(8))})
        roomStatus=label("جارٍ تحميل الغرف...",12f,muted).apply{gravity=Gravity.CENTER}
        content.addView(roomStatus,LinearLayout.LayoutParams(-1,dp(32)))
        roomStatus.text="جارٍ تحميل الغرف الحقيقية..."
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER;background=ColorDrawable(Color.WHITE);setPadding(dp(5),dp(6),dp(5),dp(6));elevation=dp(8).toFloat()}
        fun navItem(symbol:String,title:String,selected:Boolean=false)=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            addView(iconView(symbol,if(selected)purple else ink),LinearLayout.LayoutParams(dp(28),dp(30)))
            addView(TextView(this@HomeActivity).apply{
                text=title;textSize=12f;gravity=Gravity.CENTER
                setTextColor(if(selected)purple else muted);setSingleLine(true)
            },LinearLayout.LayoutParams(-1,dp(20)))
        }
        nav.addView(navItem("profile","حسابي").apply{setOnClickListener{startActivity(Intent(this@HomeActivity,ProfileActivity::class.java))}},LinearLayout.LayoutParams(0,dp(68),1f))
        nav.addView(navItem("message","رسائل"),LinearLayout.LayoutParams(0,dp(68),1f))
        val addRoom=FrameLayout(this).apply{
            val circle=TextView(this@HomeActivity).apply{
                text="+";textSize=31f;gravity=Gravity.CENTER
                setTextColor(Color.WHITE);background=shape(purple,30);elevation=dp(6).toFloat()
                setOnClickListener{showCreateRoomDialog()}
            }
            addView(circle,FrameLayout.LayoutParams(dp(58),dp(58),Gravity.CENTER))
        }
        nav.addView(addRoom,LinearLayout.LayoutParams(0,dp(68),1f))
        nav.addView(navItem("compass","اكتشف"),LinearLayout.LayoutParams(0,dp(68),1f))
        nav.addView(navItem("home","الرئيسية",true),LinearLayout.LayoutParams(0,dp(68),1f))
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(82)));setContentView(root)
        verifyAccount();loadRooms()
    }
    private fun iconView(kind:String,tint:Int):View = object:View(this){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            style=Paint.Style.STROKE;strokeWidth=dp(2).toFloat()
            strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;setColor(tint)
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            val side=dp(26).toFloat();val x=(width-side)/2f;val y=(height-side)/2f
            canvas.save();canvas.translate(x,y);canvas.scale(side/24f,side/24f)
            when(kind){
                "profile"->{
                    canvas.drawCircle(12f,7f,4f,p)
                    canvas.drawArc(RectF(4f,13f,20f,25f),180f,180f,false,p)
                }
                "message"->{
                    val r=RectF(2.5f,3f,21.5f,18f);canvas.drawRoundRect(r,5f,5f,p)
                    val tail=Path().apply{moveTo(7f,18f);lineTo(5f,22f);lineTo(12f,18f)}
                    canvas.drawPath(tail,p)
                    canvas.drawCircle(8f,10.5f,0.8f,p);canvas.drawCircle(12f,10.5f,0.8f,p);canvas.drawCircle(16f,10.5f,0.8f,p)
                }
                "star"->{
                    val path=Path()
                    for(i in 0..9){val a=Math.PI/2+i*Math.PI/5;val r=if(i%2==0)10.5 else 4.8;val px=(12+Math.cos(a)*r).toFloat();val py=(12-Math.sin(a)*r).toFloat();if(i==0)path.moveTo(px,py)else path.lineTo(px,py)}
                    path.close();canvas.drawPath(path,p)
                }
                "compass"->{
                    canvas.drawCircle(12f,12f,9.5f,p)
                    val needle=Path().apply{moveTo(15.5f,7f);lineTo(13f,13f);lineTo(8f,16.5f);lineTo(10.5f,10.5f);close()}
                    canvas.drawPath(needle,p)
                }
                "home"->{
                    val path=Path().apply{moveTo(2f,11f);lineTo(12f,3f);lineTo(22f,11f);moveTo(5f,10f);lineTo(5f,21f);lineTo(19f,21f);lineTo(19f,10f);moveTo(10f,21f);lineTo(10f,15f);lineTo(14f,15f);lineTo(14f,21f)}
                    canvas.drawPath(path,p)
                }
            }
            canvas.restore()
        }
    }
    private fun buildCards(){
        grid.removeAllViews()
        val width=(resources.displayMetrics.widthPixels-dp(32))/2
        val imageHeight=(width*0.78f).toInt()
        val filtered=liveRooms.filter{it.name.contains(searchQuery,true)}
        val visible=if(filterMode=="popular")filtered.sortedByDescending{it.count}else filtered.filter{it.count>0}
        if(visible.isEmpty()){
            roomStatus.text=if(searchQuery.isNotBlank())"لا توجد غرف تطابق البحث" else "لا توجد غرف نشطة حالياً. أنشئ أول غرفة!"
            return
        }
        roomStatus.text="الغرف الحقيقية: ${visible.size}"
        visible.forEach { room ->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(5),dp(5),dp(10));isClickable=true}
            val frame=FrameLayout(this).apply{
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(117,78,211),Color.rgb(36,31,62))).apply{cornerRadius=dp(13).toFloat()}
                clipToOutline=true
            }
            val cover=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
            frame.addView(cover,FrameLayout.LayoutParams(-1,imageHeight))
            val overlay=TextView(this).apply{
                text="🎙  ${room.count}/${room.max}";textSize=12f;setTextColor(Color.WHITE)
                setPadding(dp(9),dp(6),dp(9),dp(6));background=shape(0x99000000.toInt(),16)
            }
            frame.addView(overlay,FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(8),0,0,dp(8))})
            if(!room.imageUrl.isNullOrBlank()){
                executor.execute{try{val con=URL(room.imageUrl).openConnection();con.connectTimeout=8000;con.readTimeout=8000;val bmp=con.getInputStream().use{BitmapFactory.decodeStream(it)};runOnUiThread{cover.setImageBitmap(bmp)}}catch(_:Exception){}}
            }
            card.addView(frame)
            card.addView(label(room.name,14f,ink,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(4),dp(7),dp(4),0)})
            card.addView(label("●  ${room.count} متصل الآن",11f,Color.rgb(26,169,113)).apply{setPadding(dp(4),dp(4),0,0)})
            card.setOnClickListener{joinRoom(room.id,room.name)}
            grid.addView(card,GridLayout.LayoutParams().apply{this.width=width;height=-2;setMargins(dp(3),dp(3),dp(3),dp(5))})
        }
    }
    private fun verifyAccount(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{try{val r=AuthApi.get("/api/auth/me",token);if(r.status==401||r.status==403){getSharedPreferences("MariamLive",MODE_PRIVATE).edit().remove("token").apply();runOnUiThread{openLogin()}}}catch(_:Exception){}}
    }
    private fun loadRooms(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){roomStatus.text="سجّل الدخول لعرض الغرف";return}
        roomStatus.text="جارٍ تحميل الغرف الحقيقية..."
        executor.execute{
            try{
                val result=AuthApi.get("/api/voice-rooms",token)
                val array=result.body.optJSONArray("rooms")
                val parsed= mutableListOf<RoomItem>()
                if(result.status in 200..299 && array!=null){
                    for(i in 0 until array.length()){
                        val item=array.optJSONObject(i)?:continue
                        parsed.add(RoomItem(
                            item.optString("id"),
                            item.optString("name","غرفة صوتية"),
                            item.optInt("member_count",0),
                            item.optInt("max_members",8),
                            item.optString("image_url").takeIf{it.isNotBlank()&&it!="null"}
                        ))
                    }
                }
                runOnUiThread{
                    liveRooms.clear();liveRooms.addAll(parsed);buildCards()
                    if(result.status !in 200..299)roomStatus.text=result.body.optString("error","تعذر تحميل الغرف")
                }
            }catch(_:Exception){runOnUiThread{liveRooms.clear();buildCards();roomStatus.text="تعذر الاتصال بالخادم"}}
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
        val input=EditText(this).apply{hint="اسم الغرفة";setSingleLine(true)}
        android.app.AlertDialog.Builder(this).setTitle("إنشاء غرفة صوتية")
            .setMessage("اختر صورة من هاتفك لتظهر على بطاقة الغرفة في الصفحة الرئيسية.")
            .setView(input).setNegativeButton("إلغاء",null)
            .setPositiveButton("اختيار الصورة"){_,_->
                val name=input.text.toString().trim()
                if(name.length<2){Toast.makeText(this,"اكتب اسماً من حرفين على الأقل",Toast.LENGTH_SHORT).show();return@setPositiveButton}
                pendingRoomName=name
                val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)}
                startActivityForResult(pick,roomImagePicker)
            }.show()
    }
    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==roomImagePicker && resultCode==RESULT_OK){
            val uri=data?.data ?: return
            createRoomWithImage(pendingRoomName,uri)
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
                    if(result.status in 200..299){Toast.makeText(this,"تم إنشاء الغرفة وحفظ صورتها",Toast.LENGTH_LONG).show();loadRooms()}
                    else Toast.makeText(this,result.body.optString("error","تعذر إنشاء الغرفة"),Toast.LENGTH_LONG).show()
                }
            }catch(e:Exception){runOnUiThread{Toast.makeText(this,e.message?:"تعذر تجهيز الصورة",Toast.LENGTH_LONG).show()}}
        }
    }
    private fun openLogin(){startActivity(Intent(this,LoginActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
