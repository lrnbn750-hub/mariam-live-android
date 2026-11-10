package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
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
    private data class DemoRoom(val title:String,val tag:String,val count:Int,val image:String)
    private val demos = listOf(
        DemoRoom("سهرة الأصدقاء 🎙","دردشة",28,"https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=700&h=700&fit=crop"),
        DemoRoom("مجلس الموسيقى 🎵","موسيقى",19,"https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=700&h=700&fit=crop"),
        DemoRoom("تعرف وتواصل","عشوائي",34,"https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=700&h=700&fit=crop"),
        DemoRoom("ليلة عربية ✨","مباشر",12,"https://images.unsplash.com/photo-1506157786151-b8491531f063?w=700&h=700&fit=crop"),
        DemoRoom("أصدقاء حول العالم","دردشة",21,"https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=700&h=700&fit=crop"),
        DemoRoom("استراحة ووناسة","عشوائي",16,"https://images.unsplash.com/photo-1527529482837-4698179dc6ce?w=700&h=700&fit=crop")
    )
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
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(250,250,252))}
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(root)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(16),dp(8));setBackgroundColor(cream)}
        header.addView(label("◉",27f,purple,true),LinearLayout.LayoutParams(dp(42),dp(45)))
        header.addView(label("⌕",34f,ink),LinearLayout.LayoutParams(dp(42),dp(45)))
        header.addView(label("شعبي",17f,ink,true),LinearLayout.LayoutParams(0,dp(45),1f))
        header.addView(label("المجاورون",14f,muted))
        header.addView(label("  متصل",14f,muted))
        root.addView(header)
        val banner=FrameLayout(this).apply{
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(190,124,246),Color.rgb(103,85,224))).apply{cornerRadius=dp(16).toFloat()}
        }
        val bt=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        bt.addView(label("MARIAM LIVE",25f,Color.WHITE,true).apply{gravity=Gravity.CENTER})
        bt.addView(label("عِش اللحظة… وتعرّف على أصدقاء جدد",13f,Color.WHITE).apply{gravity=Gravity.CENTER})
        banner.addView(bt,FrameLayout.LayoutParams(-1,-1))
        root.addView(banner,LinearLayout.LayoutParams(-1,dp(132)).apply{setMargins(dp(16),dp(8),dp(16),dp(14))})
        val hsv=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false}
        val chips=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(14),0,dp(14),dp(4))}
        listOf("🇮🇶 العراق","🇪🇬 مصر","🇸🇦 السعودية","🌍 الكل").forEachIndexed{i,s->
            chips.addView(TextView(this).apply{text=s;textSize=12f;gravity=Gravity.CENTER;setTextColor(if(i==0)Color.rgb(31,160,100)else muted);background=shape(if(i==0)Color.rgb(226,248,236)else Color.WHITE,24);setPadding(dp(17),dp(10),dp(17),dp(10))},LinearLayout.LayoutParams(-2,dp(42)).apply{marginEnd=dp(8)})
        }
        hsv.addView(chips);root.addView(hsv)
        val heading=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(10))}
        heading.addView(label("الغرف والمجتمعات",18f,ink,true),LinearLayout.LayoutParams(0,dp(34),1f))
        heading.addView(label("اكتشف المزيد  ›",12f,purple,true));root.addView(heading)
        grid=GridLayout(this).apply{columnCount=2;useDefaultMargins=false}
        root.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(10),0,dp(10),dp(8))})
        roomStatus=label("صور تجريبية للمعاينة",12f,muted).apply{gravity=Gravity.CENTER}
        root.addView(roomStatus,LinearLayout.LayoutParams(-1,dp(32)))
        buildCards()
        val create=TextView(this).apply{text="＋  إنشاء غرفة صوتية";textSize=14f;gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=shape(purple,16);setOnClickListener{showCreateRoomDialog()}}
        root.addView(create,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(dp(16),dp(5),dp(16),dp(16))})
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=ColorDrawable(Color.WHITE);setPadding(dp(5),dp(8),dp(5),dp(8))}
        listOf("♙\nحسابي","☏\nرسائل","☆\nالمفضلة","◎\nاكتشف","⌂\nالرئيسية").forEachIndexed{i,s->
            nav.addView(label(s,11f,if(i==4)purple else ink,i==4).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
        }
        root.addView(nav);setContentView(scroll)
        verifyAccount();loadRooms()
    }
    private fun buildCards(){
        grid.removeAllViews()
        val w=(resources.displayMetrics.widthPixels-dp(34))/2
        demos.forEachIndexed{i,r->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(5),dp(5),dp(9))}
            val frame=FrameLayout(this).apply{background=shape(Color.rgb(232,227,243),13);clipToOutline=true}
            val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;setBackgroundColor(Color.rgb(225,220,239))}
            frame.addView(image,FrameLayout.LayoutParams(-1,dp(158)))
            frame.addView(TextView(this).apply{text=r.tag;textSize=10f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=shape(if(i%2==0)Color.rgb(0,174,190)else purple,7);setPadding(dp(8),dp(5),dp(8),dp(5))},FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(7),dp(7))})
            frame.addView(TextView(this).apply{text="＋  ◉ ◉ ◉ ◉  •••";textSize=11f;setTextColor(Color.WHITE);background=shape(0x99000000.toInt(),18);setPadding(dp(7),dp(5),dp(7),dp(5))},FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(7),0,0,dp(7))})
            card.addView(frame)
            card.addView(label(r.title,13f,ink,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(4),dp(7),dp(4),0)})
            card.addView(label("●  ${r.count} متصل الآن",10f,Color.rgb(26,169,113)).apply{setPadding(dp(4),dp(4),0,0)})
            grid.addView(card,GridLayout.LayoutParams().apply{width=w;height=-2;setMargins(dp(3),dp(3),dp(3),dp(5))})
            executor.execute{try{val conn=URL(r.image).openConnection();conn.connectTimeout=8000;conn.readTimeout=8000;val bmp=conn.getInputStream().use{BitmapFactory.decodeStream(it)};runOnUiThread{image.setImageBitmap(bmp)}}catch(_:Exception){}}
        }
    }
    private fun verifyAccount(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{try{val r=AuthApi.get("/api/auth/me",token);if(r.status==401||r.status==403){getSharedPreferences("MariamLive",MODE_PRIVATE).edit().remove("token").apply();runOnUiThread{openLogin()}}}catch(_:Exception){}}
    }
    private fun loadRooms(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{try{val r=AuthApi.get("/api/voice-rooms",token);val rooms=r.body.optJSONArray("rooms");runOnUiThread{roomStatus.text=if(r.status in 200..299&&rooms!=null)"الغرف الفعلية المتاحة: ${rooms.length()}  •  الصور أعلاه تجريبية" else "صور تجريبية للمعاينة — تعذّر تحديث الغرف"}}catch(_:Exception){runOnUiThread{roomStatus.text="صور تجريبية للمعاينة — لا يوجد اتصال بالغرف"}}}
    }
    private fun showCreateRoomDialog(){
        val input=EditText(this).apply{hint="اسم الغرفة";setSingleLine(true)}
        android.app.AlertDialog.Builder(this).setTitle("إنشاء غرفة صوتية").setView(input).setNegativeButton("إلغاء",null).setPositiveButton("إنشاء"){_,_->val n=input.text.toString().trim();if(n.length<2)Toast.makeText(this,"اكتب اسماً من حرفين على الأقل",Toast.LENGTH_SHORT).show()else createRoom(n)}.show()
    }
    private fun createRoom(name:String){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        executor.execute{try{val r=AuthApi.post("/api/voice-rooms",JSONObject().put("name",name).put("max_members",8),token);runOnUiThread{Toast.makeText(this,if(r.status in 200..299)"تم إنشاء الغرفة" else r.body.optString("error","تعذر إنشاء الغرفة"),Toast.LENGTH_LONG).show();loadRooms()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"تعذر الاتصال بالخادم",Toast.LENGTH_SHORT).show()}}}
    }
    private fun openLogin(){startActivity(Intent(this,LoginActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
