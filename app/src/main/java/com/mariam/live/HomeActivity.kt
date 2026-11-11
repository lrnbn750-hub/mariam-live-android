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
        val pageBg=Color.rgb(247,246,251)
        val ink=Color.rgb(34,27,54)
        val muted=Color.rgb(137,130,151)
        val accent=Color.rgb(119,74,242)
        window.statusBarColor=pageBg
        window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(pageBg)}
        val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=false}
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(18),dp(12),dp(18),dp(22));setBackgroundColor(pageBg)}
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        brand.addView(TextView(this).apply{text="MARIAM LIVE ✦";textSize=21f;typeface=Typeface.create("sans-serif",Typeface.BOLD);setTextColor(ink);letterSpacing=0.02f})
        brand.addView(TextView(this).apply{text="مساحتك للأصدقاء والأحاديث";textSize=11f;setTextColor(muted);setPadding(0,dp(2),0,0)})
        header.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(TextView(this).apply{text="⌕";textSize=27f;gravity=Gravity.CENTER;setTextColor(ink);background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(15).toFloat();setStroke(dp(1),Color.rgb(233,230,241))};setOnClickListener{showSearchDialog()}},LinearLayout.LayoutParams(dp(44),dp(44)).apply{marginEnd=dp(9)})
        header.addView(TextView(this).apply{text="●";textSize=18f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(accent,Color.rgb(174,112,246))).apply{shape=GradientDrawable.OVAL};setOnClickListener{startActivity(Intent(this@HomeActivity,ProfileActivity::class.java))},LinearLayout.LayoutParams(dp(44),dp(44)))
        content.addView(header,LinearLayout.LayoutParams(-1,dp(54)))
        val welcome=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(17),dp(18),dp(17));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(45,32,78),Color.rgb(105,68,169),Color.rgb(139,83,190))).apply{cornerRadius=dp(22).toFloat()};elevation=dp(3).toFloat()}
        welcome.addView(TextView(this).apply{text="أهلاً بك في مريم لايف";textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.RIGHT})
        welcome.addView(TextView(this).apply{text="اكتشف غرفة تناسبك أو ابدأ جلسة مع أصدقائك";textSize=12f;setTextColor(Color.rgb(235,226,250));setPadding(0,dp(5),0,dp(13))})
        welcome.addView(TextView(this).apply{text="＋  أنشئ غرفة صوتية";textSize=13f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.rgb(64,42,104));background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(13).toFloat()};setPadding(dp(16),dp(11),dp(16),dp(11));setOnClickListener{showCreateRoomDialog()}},LinearLayout.LayoutParams(-2,dp(42)))
        content.addView(welcome,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)})
        val section=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(0,dp(23),0,dp(10))}
        section.addView(TextView(this).apply{text="الغرف الصوتية";textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)},LinearLayout.LayoutParams(0,dp(30),1f))
        section.addView(TextView(this).apply{text="● مباشر الآن";textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(31,165,112))})
        content.addView(section)
        val filters=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER_VERTICAL}
        lateinit var refreshFilters:()->Unit
        fun filterChip(title:String,mode:String):TextView=TextView(this).apply{
            text=title;textSize=12f;gravity=Gravity.CENTER;setPadding(dp(16),dp(9),dp(16),dp(9))
            background=GradientDrawable().apply{setColor(if(filterMode==mode)accent else Color.WHITE);cornerRadius=dp(18).toFloat();if(filterMode!=mode)setStroke(dp(1),Color.rgb(232,229,240))}
            setTextColor(if(filterMode==mode)Color.WHITE else muted);typeface=if(filterMode==mode)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setOnClickListener{filterMode=mode;refreshFilters();buildCards()}
        }
        refreshFilters={filters.removeAllViews();filters.addView(filterChip("المتصلون","online"));filters.addView(filterChip("الأكثر شعبية","popular"),LinearLayout.LayoutParams(-2,-2).apply{marginStart=dp(8)})}
        refreshFilters()
        content.addView(filters)
        grid=GridLayout(this).apply{columnCount=1;useDefaultMargins=false;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        content.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
        roomStatus=TextView(this).apply{text="جارٍ تحميل الغرف...";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER}
        content.addView(roomStatus,LinearLayout.LayoutParams(-1,dp(52)))
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER;background=ColorDrawable(Color.WHITE);setPadding(dp(5),dp(5),dp(5),dp(5));elevation=dp(12).toFloat()}
        fun navItem(symbol:String,title:String,selected:Boolean=false)=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            addView(iconView(symbol,if(selected)accent else Color.rgb(157,151,169)),LinearLayout.LayoutParams(dp(27),dp(29)))
            addView(TextView(this@HomeActivity).apply{text=title;textSize=10f;gravity=Gravity.CENTER;setTextColor(if(selected)accent else Color.rgb(157,151,169));typeface=if(selected)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;setSingleLine(true)},LinearLayout.LayoutParams(-1,dp(19)))
        }
        nav.addView(navItem("profile","حسابي").apply{setOnClickListener{startActivity(Intent(this@HomeActivity,ProfileActivity::class.java))}},LinearLayout.LayoutParams(0,dp(58),1f))
        nav.addView(navItem("message","رسائل"),LinearLayout.LayoutParams(0,dp(58),1f))
        nav.addView(FrameLayout(this).apply{addView(TextView(this@HomeActivity).apply{text="＋";textSize=29f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(244,91,163),accent)).apply{cornerRadius=dp(20).toFloat()};elevation=dp(5).toFloat();setOnClickListener{showCreateRoomDialog()}},FrameLayout.LayoutParams(dp(49),dp(49),Gravity.CENTER))},LinearLayout.LayoutParams(0,dp(58),1f))
        nav.addView(navItem("compass","اكتشف"),LinearLayout.LayoutParams(0,dp(58),1f))
        nav.addView(navItem("home","الرئيسية",true),LinearLayout.LayoutParams(0,dp(58),1f))
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(68)))
        setContentView(root)
        verifyAccount()
    }

    override fun onResume(){
        super.onResume()
        if(::grid.isInitialized && ::roomStatus.isInitialized){
            loadRooms()
        }
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
        val filtered=liveRooms.filter{it.name.contains(searchQuery,true)}
        val visible=if(filterMode=="popular")filtered.sortedByDescending{it.count}else filtered.filter{it.count>0}
        if(visible.isEmpty()){
            roomStatus.text=if(searchQuery.isNotBlank())"لا توجد غرف تطابق البحث" else "لا توجد غرف نشطة حالياً. كن أول من يبدأ الحديث!"
            return
        }
        roomStatus.text=" \${visible.size} غرفة متاحة"
        visible.forEach { room ->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10));background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(18).toFloat();setStroke(dp(1),Color.rgb(235,232,243))};elevation=dp(2).toFloat();isClickable=true}
            val cover=FrameLayout(this).apply{background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(52,38,88),Color.rgb(119,74,177),Color.rgb(172,112,197))).apply{cornerRadius=dp(14).toFloat()};clipToOutline=true}
            if(!room.imageUrl.isNullOrBlank()){val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};cover.addView(image,FrameLayout.LayoutParams(-1,-1));loadRoomCover(image,room.imageUrl)}
            cover.addView(View(this).apply{background=GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,intArrayOf(0x990D071B.toInt(),0x000D071B))},FrameLayout.LayoutParams(-1,-1))
            cover.addView(TextView(this).apply{text="● مباشر";textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(dp(9),dp(6),dp(9),dp(6));background=GradientDrawable().apply{setColor(0xCCDF477B.toInt());cornerRadius=dp(13).toFloat()}},FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.START).apply{setMargins(dp(10),dp(10),0,0)})
            cover.addView(TextView(this).apply{text="🎙  \${room.count} / \${room.max}";textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(dp(9),dp(6),dp(9),dp(6));background=GradientDrawable().apply{setColor(0x990D071B.toInt());cornerRadius=dp(13).toFloat()}},FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(10),dp(10))})
            cover.addView(TextView(this).apply{text="♫";textSize=35f;gravity=Gravity.CENTER;setTextColor(0x88FFFFFF.toInt())},FrameLayout.LayoutParams(dp(60),dp(60),Gravity.CENTER))
            card.addView(cover,LinearLayout.LayoutParams(-1,dp(126)))
            val details=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(2),dp(10),dp(2),0)}
            val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            info.addView(TextView(this).apply{text=room.name;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(43,35,62));maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END})
            info.addView(TextView(this).apply{text="انضم إلى الحديث الآن";textSize=11f;setTextColor(Color.rgb(143,135,157));setPadding(0,dp(3),0,0)})
            details.addView(info,LinearLayout.LayoutParams(0,-2,1f))
            details.addView(TextView(this).apply{text="دخول";textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(128,82,239),Color.rgb(103,64,207))).apply{cornerRadius=dp(12).toFloat()};setPadding(dp(16),dp(9),dp(16),dp(9))})
            card.addView(details)
            card.setOnClickListener{joinRoom(room.id,room.name)}
            grid.addView(card,GridLayout.LayoutParams().apply{width=-1;height=-2;setMargins(0,dp(5),0,dp(7))})
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
                            roomImageUrl(item)
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
    private fun roomImageUrl(item:JSONObject):String?{
        val image=item.optJSONObject("image")
        val raw=sequenceOf(
            item.optString("image_url"),
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
                    connectTimeout=15000
                    readTimeout=20000
                    instanceFollowRedirects=true
                    setRequestProperty("Accept","image/avif,image/webp,image/png,image/jpeg,*/*")
                    setRequestProperty("User-Agent","MariamLive-Android")
                    connect()
                }
                if(connection.responseCode !in 200..299)throw Exception("HTTP ${connection.responseCode}")
                val bitmap=connection.inputStream.use{BitmapFactory.decodeStream(it)}
                    ?: throw Exception("Invalid room image")
                runOnUiThread{if(!isFinishing && !isDestroyed)view.setImageBitmap(bitmap) else bitmap.recycle()}
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
                        // Refresh the home list immediately after the server confirms creation.
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
}
