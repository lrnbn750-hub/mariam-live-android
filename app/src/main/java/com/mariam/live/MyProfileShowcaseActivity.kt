package com.mariam.live

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.URL
import java.util.concurrent.Executors

class MyProfileShowcaseActivity : Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val ink=Color.rgb(43,45,55)
        val muted=Color.rgb(135,137,148)
        val gold=Color.rgb(213,169,83)
        fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
            text=value;textSize=size;setTextColor(color);gravity=Gravity.CENTER
            typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        fun panel()=android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.WHITE);cornerRadius=dp(18).toFloat()
            setStroke(dp(1),Color.rgb(235,232,225))
        }
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(10),dp(18),dp(20))
            setBackgroundColor(Color.rgb(247,248,250))
        }
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        top.addView(text("رجوع",15f,ink,true).apply{setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(64),dp(48)))
        top.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        top.addView(text("الملف الشخصي",18f,ink,true),LinearLayout.LayoutParams(-2,dp(48)))
        root.addView(top)
        val hero=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            setPadding(dp(16),dp(25),dp(16),dp(24));background=panel()
        }
        val avatar=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(229,231,235));setStroke(dp(4),gold)
            }
            clipToOutline=true
            setImageDrawable(android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(229,231,235))
            })
        }
        hero.addView(avatar,LinearLayout.LayoutParams(dp(116),dp(116)))
        val name=intent.getStringExtra("display_name").orEmpty().ifBlank{"مستخدم مريم"}
        val id=intent.getStringExtra("public_id").orEmpty()
        hero.addView(text(name,21f,ink,true),LinearLayout.LayoutParams(-1,dp(48)))
        hero.addView(text(if(id.isBlank())"ID: غير متوفر" else "ID: $id",14f,muted),LinearLayout.LayoutParams(-1,dp(30)))
        root.addView(hero,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
        val stats=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));background=panel()
        }
        fun stat(label:String)=LinearLayout(this).apply{
            gravity=Gravity.CENTER_VERTICAL
            addView(text(label,15f,ink,true),LinearLayout.LayoutParams(0,dp(54),1f))
            addView(text("—",20f,gold,true),LinearLayout.LayoutParams(dp(60),dp(54)))
        }
        stats.addView(stat("إجمالي الدعم الذي أرسلته"))
        val line=View(this).apply{setBackgroundColor(Color.rgb(240,240,242))}
        stats.addView(line,LinearLayout.LayoutParams(-1,dp(1)))
        stats.addView(stat("إجمالي الدعم الذي استلمته"))
        root.addView(stats)
        root.addView(text("بيانات الدعم ستظهر بعد ربطها بإحصائيات الخادم.",12f,muted).apply{
            setPadding(dp(8),dp(12),dp(8),dp(12))
        },LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
        val url=intent.getStringExtra("avatar_url").orEmpty()
        if(url.startsWith("https://")){
            executor.execute{
                try{
                    val bitmap=URL(url).openConnection().apply{connectTimeout=10000;readTimeout=15000}.getInputStream().use{BitmapFactory.decodeStream(it)}
                    if(bitmap!=null)runOnUiThread{if(!isFinishing)avatar.setImageBitmap(bitmap)}
                }catch(_:Exception){}
            }
        }
    }
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
