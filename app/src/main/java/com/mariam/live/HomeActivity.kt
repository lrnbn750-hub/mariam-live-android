package com.mariam.live
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
import android.graphics.Shader
import android.graphics.RadialGradient
import android.graphics.LinearGradient
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
    private val purple = Color.rgb(34,158,92)
    private val ink = Color.rgb(35,45,39)
    private val muted = Color.rgb(116,130,120)
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
        val pageBg=Color.rgb(255,255,255)
        val ink=Color.rgb(35,45,39)
        val muted=Color.rgb(116,130,120)
        val accent=Color.rgb(34,158,92)
        window.statusBarColor=pageBg
        window.navigationBarColor=Color.rgb(255,255,255)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(pageBg)}
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(16),dp(12),dp(16),dp(18));setBackgroundColor(pageBg)}
        val scroll=ScrollView(this).apply{isFillViewport=false;clipToPadding=false;addView(content)}
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        // Top controls: search plus quick return to the currently open room.
        val header=FrameLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_LTR}
        fun headerAction(symbol:String,description:String)=TextView(this).apply{
            text=symbol;textSize=27f;gravity=Gravity.CENTER
            setTextColor(Color.rgb(34,158,92))
            background=GradientDrawable().apply{
                setColor(Color.rgb(242,250,245));cornerRadius=dp(15).toFloat()
                setStroke(dp(1),Color.rgb(190,225,202))
            }
            elevation=dp(2).toFloat()
            contentDescription=description
        }
        val searchButton=headerAction("⌕","بحث عن غرفة").apply{setOnClickListener{showSearchDialog()}}
        header.addView(searchButton,FrameLayout.LayoutParams(dp(46),dp(46),Gravity.TOP or Gravity.LEFT).apply{
            leftMargin=dp(8);topMargin=dp(26)
        })
        val returnRoomButton=headerAction("🎙","العودة إلى غرفتك").apply{
            tag="return_room_button"
            val prefs=getSharedPreferences("MariamLive",MODE_PRIVATE)
            val activeId=prefs.getString("active_room_id",null)
            visibility=if(activeId.isNullOrBlank())View.GONE else View.VISIBLE
            setOnClickListener{
                val id=prefs.getString("active_room_id",null).orEmpty()
                if(id.isNotBlank())startActivity(Intent(this@HomeActivity,RoomActivity::class.java).putExtra("room_id",id))
                else visibility=View.GONE
            }
        }
        header.addView(returnRoomButton,FrameLayout.LayoutParams(dp(46),dp(46),Gravity.TOP or Gravity.LEFT).apply{
            leftMargin=dp(62);topMargin=dp(26)
        })
        content.addView(header,LinearLayout.LayoutParams(-1,dp(82)))

        val palestineBanner=PalestineBannerView(this)
        content.addView(palestineBanner,LinearLayout.LayoutParams(-1,dp(112)).apply{
            setMargins(dp(1),dp(12),dp(1),dp(9))
        })

        grid=GridLayout(this).apply{columnCount=2;useDefaultMargins=false;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        content.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{setMargins(-dp(5),dp(8),-dp(5),0)})
        roomStatus=TextView(this).apply{text="جارٍ تحميل الغرف...";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER}
        content.addView(roomStatus,LinearLayout.LayoutParams(-1,dp(42)))
        val nav=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(53,16,91),Color.rgb(24,7,45))).apply{
                setCornerRadii(floatArrayOf(dp(25).toFloat(),dp(25).toFloat(),dp(25).toFloat(),dp(25).toFloat(),0f,0f,0f,0f))
                setStroke(dp(1),0x99D9AD63.toInt())
            }
            setPadding(dp(5),dp(4),dp(5),dp(4))
            elevation=dp(14).toFloat()
        }
        fun navItem(kind:String,title:String,selected:Boolean=false,onTap:()->Unit)=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            isClickable=true
            isFocusable=true
            background=if(selected) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x553D1B5C,0x00200A38)).apply{
                cornerRadius=dp(14).toFloat()
                setStroke(dp(1),0x44E9BE73)
            } else ColorDrawable(Color.TRANSPARENT)
            addView(PremiumNavIcon(this@HomeActivity,kind,selected),LinearLayout.LayoutParams(dp(28),dp(28)))
            addView(TextView(this@HomeActivity).apply{
                text=title;textSize=10.5f;gravity=Gravity.CENTER
                setTextColor(if(selected)Color.rgb(255,218,139) else Color.rgb(239,224,255))
                typeface=if(selected)Typeface.create("sans-serif",Typeface.BOLD) else Typeface.create("sans-serif-medium",Typeface.NORMAL)
                setShadowLayer(if(selected)dp(3).toFloat() else 0f,0f,dp(1).toFloat(),0xAA000000.toInt())
                setSingleLine(true)
            },LinearLayout.LayoutParams(-1,dp(16)))
            setOnClickListener{onTap()}
            contentDescription=title
        }
        nav.addView(navItem("profile","حسابي"){
            startActivity(Intent(this@HomeActivity,ProfileActivity::class.java))
        },LinearLayout.LayoutParams(0,dp(54),1f))
        nav.addView(navItem("messages","رسائلي"){
            startActivity(Intent(this@HomeActivity,MessagesActivity::class.java))
        },LinearLayout.LayoutParams(0,dp(54),1f))
        val createSlot=FrameLayout(this).apply{
            val halo=View(this@HomeActivity).apply{
                background=GradientDrawable().apply{
                    shape=GradientDrawable.OVAL
                    setColor(0x33F2B5FF)
                }
            }
            addView(halo,FrameLayout.LayoutParams(dp(58),dp(58),Gravity.CENTER))
            val create=TextView(this@HomeActivity).apply{
                text="🎂";textSize=31f;gravity=Gravity.CENTER
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.rgb(255,105,174),Color.rgb(128,49,178),Color.rgb(61,31,123))).apply{
                    shape=GradientDrawable.OVAL
                    setStroke(dp(2),Color.rgb(255,218,132))
                }
                elevation=dp(9).toFloat()
                setShadowLayer(dp(8).toFloat(),0f,dp(3).toFloat(),0xAAE65ACB.toInt())
                contentDescription="إنشاء غرفة"
                setOnClickListener{showCreateRoomDialog()}
            }
            addView(create,FrameLayout.LayoutParams(dp(46),dp(46),Gravity.CENTER))
            ObjectAnimator.ofFloat(create,View.SCALE_X,1f,1.055f).apply{duration=1100;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE;start()}
            ObjectAnimator.ofFloat(create,View.SCALE_Y,1f,1.055f).apply{duration=1100;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE;start()}
        }
        nav.addView(createSlot,LinearLayout.LayoutParams(0,dp(51),1.08f))
        nav.addView(navItem("discover","اكتشف"){
            searchQuery=""
            loadRooms()
            Toast.makeText(this@HomeActivity,"تحديث الغرف المباشرة",Toast.LENGTH_SHORT).show()
        },LinearLayout.LayoutParams(0,dp(54),1f))
        nav.addView(navItem("home","الرئيسية",true){
            searchQuery=""
            loadRooms()
        },LinearLayout.LayoutParams(0,dp(65),1f))
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(64)))
        ViewCompat.setOnApplyWindowInsetsListener(nav){ view,insets->
            val bottomInset=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp=view.layoutParams
            val targetHeight=dp(64)+bottomInset
            if(lp.height!=targetHeight){lp.height=targetHeight;view.layoutParams=lp}
            view.setPadding(dp(5),dp(3),dp(5),bottomInset)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(nav)
        verifyAccount()
    }

    override fun onResume(){
        super.onResume()
        val activeId=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("active_room_id",null)
        window.decorView.findViewWithTag<View>("return_room_button")?.visibility=if(activeId.isNullOrBlank())View.GONE else View.VISIBLE
        if(::grid.isInitialized && ::roomStatus.isInitialized)loadRooms()
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

private class PremiumNavIcon(context:android.content.Context,private val kind:String,private val selected:Boolean):View(context){
    private val density=resources.displayMetrics.density
    private fun dp(v:Float)=v*density
    private fun dp(v:Int)=v*density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold=Color.rgb(255,211,126)
    private val paleGold=Color.rgb(255,239,190)
    private val violet=Color.rgb(161,91,255)
    private val pink=Color.rgb(255,112,190)
    private val cyan=Color.rgb(91,218,255)
    private fun gradient(top:Int,bottom:Int,rect:RectF)=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        shader=LinearGradient(0f,rect.top,0f,rect.bottom,top,bottom,Shader.TileMode.CLAMP)
    }
    private fun stroke(color:Int,width:Float)=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        style=Paint.Style.STROKE;strokeWidth=dp(width);strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;this.color=color
    }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val cx=width/2f;val cy=height/2f
        val aura=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            shader=RadialGradient(cx,cy,dp(16),intArrayOf(if(selected)0x55FFD77D else 0x334A9BFF,0x00200A38),null,Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx,cy,dp(16),aura)
        canvas.save()
        canvas.translate(cx,cy)
        val s=dp(1)
        canvas.scale(s,s)
        val outline=stroke(0xCCFFE0A0.toInt(),1.25f)
        when(kind){
            "profile"->{
                val head=RectF(-5.2f,-12f,5.2f,-1.6f)
                canvas.drawOval(head,gradient(Color.WHITE,pink,head))
                canvas.drawOval(head,outline)
                val body=Path().apply{
                    moveTo(-12.5f,12f);lineTo(-12.5f,8.5f)
                    cubicTo(-12.5f,2f,-7.5f,-1f,0f,-1f)
                    cubicTo(7.5f,-1f,12.5f,2f,12.5f,8.5f)
                    lineTo(12.5f,12f);close()
                }
                val bodyRect=RectF(-13f,-2f,13f,13f)
                canvas.drawPath(body,gradient(Color.rgb(255,220,151),Color.rgb(224,143,57),bodyRect))
                canvas.drawPath(body,outline)
                canvas.drawArc(RectF(-4f,3f,4f,13f),180f,180f,false,stroke(0xAAFFFFFF.toInt(),1f))
                canvas.drawCircle(0f,-7f,1.4f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
            }
            "messages"->{
                val back=RectF(-12f,-10f,11f,7f)
                canvas.drawRoundRect(back,5f,5f,gradient(Color.rgb(87,203,255),Color.rgb(50,104,220),back))
                canvas.drawRoundRect(back,5f,5f,outline)
                val front=RectF(-8f,-7f,13f,11f)
                canvas.drawRoundRect(front,5f,5f,gradient(Color.rgb(255,158,218),Color.rgb(139,65,210),front))
                canvas.drawRoundRect(front,5f,5f,outline)
                val tail=Path().apply{moveTo(5f,10f);lineTo(3f,15f);lineTo(10f,10f);close()}
                paint.color=gold;canvas.drawPath(tail,paint)
                for(x in floatArrayOf(-2f,3f,8f)){
                    canvas.drawCircle(x,1.5f,1.35f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
                }
                canvas.drawLine(-4f,-4f,7f,-4f,stroke(0x66FFFFFF,0.8f))
            }
            "discover"->{
                canvas.drawCircle(0f,0f,12.5f,Paint(Paint.ANTI_ALIAS_FLAG).apply{
                    shader=LinearGradient(-10f,-12f,10f,12f,cyan,violet,Shader.TileMode.CLAMP)
                })
                canvas.drawCircle(0f,0f,12.5f,outline)
                val north=Path().apply{moveTo(0f,-10f);lineTo(3.2f,0f);lineTo(0f,10f);lineTo(-3.2f,0f);close()}
                canvas.drawPath(north,Paint(Paint.ANTI_ALIAS_FLAG).apply{shader=LinearGradient(0f,-10f,0f,10f,paleGold,Color.rgb(194,108,36),Shader.TileMode.CLAMP)})
                val east=Path().apply{moveTo(10f,0f);lineTo(0f,3.2f);lineTo(-10f,0f);lineTo(0f,-3.2f);close()}
                canvas.drawPath(east,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=0xCCFFFFFF.toInt()})
                canvas.drawCircle(0f,0f,2.2f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=gold})
                canvas.drawCircle(0f,0f,1f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
            }
            "home"->{
                val roof=Path().apply{moveTo(-14f,-2f);lineTo(0f,-14f);lineTo(14f,-2f);lineTo(11f,1f);lineTo(0f,-8f);lineTo(-11f,1f);close()}
                canvas.drawPath(roof,Paint(Paint.ANTI_ALIAS_FLAG).apply{shader=LinearGradient(0f,-14f,0f,1f,paleGold,Color.rgb(195,111,35),Shader.TileMode.CLAMP)})
                canvas.drawPath(roof,outline)
                val house=RectF(-10f,-2f,10f,12f)
                canvas.drawRoundRect(house,2f,2f,gradient(Color.rgb(255,224,155),Color.rgb(197,113,37),house))
                canvas.drawRoundRect(house,2f,2f,outline)
                canvas.drawRoundRect(RectF(-2.4f,4f,2.4f,12f),1.5f,1.5f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(75,28,105)})
                canvas.drawRoundRect(RectF(-7f,1f,-4f,5f),1f,1f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
                canvas.drawRoundRect(RectF(4f,1f,7f,5f),1f,1f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
                canvas.drawCircle(7f,-8f,1.5f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE})
            }
        }
        canvas.restore()
        if(selected){
            val dot=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=gold}
            canvas.drawCircle(cx,height-dp(1.5f),dp(1.5f),dot)
        }
    }
}
    private fun buildCards(){
        grid.removeAllViews()
        val width=(resources.displayMetrics.widthPixels-dp(42))/2
        val imageHeight=(width*1.00f).toInt()
        val filtered=liveRooms.filter{it.name.contains(searchQuery,true)}
        val visible=filtered.sortedByDescending{it.count}
        if(visible.isEmpty()){
            roomStatus.visibility=View.VISIBLE
            roomStatus.text=if(searchQuery.isNotBlank())"لا توجد غرف تطابق البحث" else "لا توجد غرف نشطة حالياً. أنشئ أول غرفة!"
            return
        }
        roomStatus.visibility=View.GONE
        visible.forEach { room ->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(5),dp(5),dp(11));isClickable=true;background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(19).toFloat();setStroke(dp(1),Color.rgb(225,237,228))}}
            val frame=FrameLayout(this).apply{
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(226,245,233),Color.rgb(205,237,216),Color.rgb(190,228,202))).apply{cornerRadius=dp(17).toFloat();setStroke(dp(1),Color.rgb(175,218,188))}
                clipToOutline=true
            }
            val cover=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
            frame.addView(cover,FrameLayout.LayoutParams(-1,imageHeight))
            val presence=RoomPresenceView(this,room.count)
            frame.addView(presence,FrameLayout.LayoutParams(dp(40),dp(24),Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(8),0,0,dp(8))})
            if(!room.imageUrl.isNullOrBlank()){
                loadRoomCover(cover,room.imageUrl)
            }
            card.addView(frame)
            card.addView(label(room.name,14f,ink,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(4),dp(7),dp(4),0)})
            // Online count label removed from room cards.
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

/** Blue animated voice bars with the live room member count. */
private class RoomPresenceView(context:android.content.Context,private val memberCount:Int):View(context){
    private val density=resources.displayMetrics.density
    private val barPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        color=Color.rgb(35,205,226)
        strokeWidth=2.8f*density
        strokeCap=Paint.Cap.ROUND
    }
    private val countPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        color=Color.WHITE
        textSize=13f*density
        typeface=Typeface.create("sans-serif",Typeface.BOLD)
    }
    private val started=android.os.SystemClock.uptimeMillis()
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val phase=(android.os.SystemClock.uptimeMillis()-started)/145.0
        val centerY=height/2f
        val xs=floatArrayOf(4f,10f,16f)
        for(i in xs.indices){
            val wave=(kotlin.math.sin(phase+i*1.15)+1.0)/2.0
            val h=(5.0+wave*13.0).toFloat()*density
            val x=xs[i]*density
            canvas.drawLine(x,centerY-h/2f,x,centerY+h/2f,barPaint)
        }
        canvas.drawText(memberCount.toString(),24f*density,
            centerY-countPaint.ascent()/2f-countPaint.descent()/2f,countPaint)
        postInvalidateOnAnimation()
    }
}

/** Festive Palestine banner drawn natively, with no external image dependency. */
private class PalestineBannerView(context:android.content.Context):View(context){
    private val d=resources.displayMetrics.density
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(255,211,92);style=Paint.Style.STROKE;strokeWidth=1.5f*d}
    private val title=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(255,224,125);textSize=20f*d;typeface=Typeface.create("sans-serif",Typeface.BOLD);textAlign=Paint.Align.CENTER;setShadowLayer(3f*d,0f,2f*d,Color.BLACK)}
    private val subtitle=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=18f*d;typeface=Typeface.DEFAULT_BOLD;textAlign=Paint.Align.CENTER;setShadowLayer(2f*d,0f,1f*d,Color.BLACK)}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val w=width.toFloat();val h=height.toFloat()
        val rect=RectF(0f,0f,w,h)
        val clip=Path().apply{addRoundRect(rect,14f*d,14f*d,Path.Direction.CW)}
        val save=canvas.save();canvas.clipPath(clip)
        p.shader=android.graphics.LinearGradient(0f,0f,w,h,intArrayOf(Color.rgb(7,24,63),Color.rgb(10,73,78),Color.rgb(5,35,63)),null,android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(rect,p);p.shader=null
        // Firework rays and gold border.
        val cx=w*.80f;val cy=h*.22f
        p.color=0x55FFD66B;canvas.drawCircle(cx,cy,24f*d,p)
        for(i in 0 until 16){
            val a=i*Math.PI/8.0
            canvas.drawLine(cx+(7*d*kotlin.math.cos(a)).toFloat(),cy+(7*d*kotlin.math.sin(a)).toFloat(),cx+(23*d*kotlin.math.cos(a)).toFloat(),cy+(23*d*kotlin.math.sin(a)).toFloat(),gold)
        }
        canvas.drawRoundRect(rect,14f*d,14f*d,gold)
        // Palestinian flag.
        val fx=16f*d;val fy=24f*d;val fw=58f*d;val fh=36f*d
        p.color=Color.WHITE;canvas.drawRect(fx,fy,fx+fw,fy+fh,p)
        p.color=Color.BLACK;canvas.drawRect(fx,fy,fx+fw,fy+fh/3f,p)
        p.color=Color.rgb(0,145,75);canvas.drawRect(fx,fy+fh*2f/3f,fx+fw,fy+fh,p)
        p.color=Color.rgb(210,30,45)
        val flag=Path().apply{moveTo(fx,fy);lineTo(fx+fw*.43f,fy+fh/2f);lineTo(fx,fy+fh);close()}
        canvas.drawPath(flag,p)
        canvas.drawText("عيد وطني سعيد",w*.60f,h*.48f,title)
        canvas.drawText("فلسطين",w*.60f,h*.77f,subtitle)
        for(i in 0..8)canvas.drawCircle(w*(.10f+.10f*i),h*.91f,1.5f*d,gold)
        canvas.restoreToCount(save)
    }
}
    private fun openLogin(){startActivity(Intent(this,LoginActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}