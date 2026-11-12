package com.mariam.live

import android.app.Activity
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
        val ink=Color.rgb(43,47,53)
        val muted=Color.rgb(132,137,145)
        val green=Color.rgb(38,155,112)
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(12),dp(18),dp(20))
            setBackgroundColor(Color.rgb(247,248,250))
        }
        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val back=TextView(this).apply{
            text="رجوع";textSize=15f;setTextColor(ink);gravity=Gravity.CENTER
            setOnClickListener{finish()}
        }
        header.addView(back,LinearLayout.LayoutParams(dp(65),dp(48)))
        header.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        val title=TextView(this).apply{
            text="الملف الشخصي";textSize=19f;setTextColor(ink);typeface=Typeface.DEFAULT_BOLD
        }
        header.addView(title)
        root.addView(header)
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            setPadding(dp(18),dp(30),dp(18),dp(28))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.WHITE);cornerRadius=dp(24).toFloat()
                setStroke(dp(1),Color.rgb(232,234,237))
            }
            elevation=dp(2).toFloat()
        }
        val avatar=ImageView(this).apply{
            scaleType=ImageView.ScaleType.CENTER_CROP
            background=android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(230,233,236));setStroke(dp(3),green)
            }
            clipToOutline=true
            setImageDrawable(android.graphics.drawable.GradientDrawable().apply{
                shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(Color.rgb(230,233,236))
            })
        }
        card.addView(avatar,LinearLayout.LayoutParams(dp(132),dp(132)))
        val name=TextView(this).apply{
            text=intent.getStringExtra("display_name").orEmpty().ifBlank{"مستخدم مريم"}
            textSize=22f;setTextColor(ink);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER
        }
        card.addView(name,LinearLayout.LayoutParams(-1,dp(58)))
        val publicId=intent.getStringExtra("public_id").orEmpty()
        val idRow=LinearLayout(this).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
        idRow.addView(TextView(this).apply{
            text=if(publicId.isBlank()||publicId=="حسابك")"ID: غير متوفر" else "ID: $publicId"
            textSize=14f;setTextColor(muted);gravity=Gravity.CENTER
        })
        if(publicId.isNotBlank()&&publicId!="حسابك"){
            idRow.addView(TextView(this).apply{
                text="  نسخ";textSize=13f;setTextColor(green);gravity=Gravity.CENTER
                setOnClickListener{
                    val clipboard=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Mariam ID",publicId))
                    Toast.makeText(this@MyProfileShowcaseActivity,"تم نسخ ID",Toast.LENGTH_SHORT).show()
                }
            })
        }
        card.addView(idRow,LinearLayout.LayoutParams(-1,dp(38)))
        root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)})
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
