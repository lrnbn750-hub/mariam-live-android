package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.Intent
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

class ProfileActivity : Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    private val purple=Color.rgb(112,75,246)
    private var userId=""
    private lateinit var displayName:EditText
    private lateinit var bio:EditText
    private lateinit var country:EditText
    private lateinit var avatar:EditText
    private lateinit var status:TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun field(hintText:String)=EditText(this).apply{
        hint=hintText;textSize=15f;setTextColor(Color.rgb(35,34,53))
        setPadding(dp(14),dp(12),dp(14),dp(12))
        background=android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.WHITE);cornerRadius=dp(13).toFloat()
            setStroke(dp(1),Color.rgb(226,222,238))
        }
    }
    private fun caption(s:String)=TextView(this).apply{
        text=s;textSize=13f;setTextColor(Color.rgb(85,80,105));typeface=android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0,dp(12),0,dp(5))
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(250,249,253);window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(18),dp(20),dp(22));setBackgroundColor(Color.rgb(250,249,253))}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val back=TextView(this).apply{text="‹";textSize=30f;setTextColor(purple);gravity=Gravity.CENTER;setOnClickListener{finish()}}
        top.addView(back,LinearLayout.LayoutParams(dp(42),dp(48)))
        top.addView(TextView(this).apply{text="الملف الشخصي";textSize=20f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(35,34,53))},LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(top)
        val card=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(18));background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(20).toFloat()}}
        card.addView(TextView(this).apply{text="معلومات حسابك";textSize=18f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(35,34,53))})
        card.addView(caption("رقم الحساب (ID)"))
        val idView=TextView(this).apply{text="جارٍ تحميل المعرّف...";textSize=13f;setTextColor(purple);setPadding(dp(12),dp(13),dp(12),dp(13));background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(246,243,255));cornerRadius=dp(12).toFloat()}}
        card.addView(idView,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("اسم العرض"))
        displayName=field("اكتب الاسم الذي يظهر للناس");card.addView(displayName,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("نبذة عنك"))
        bio=field("اكتب نبذة قصيرة");card.addView(bio,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("الدولة"))
        country=field("الدولة");card.addView(country,LinearLayout.LayoutParams(-1,-2))
        card.addView(caption("رابط صورة الملف الشخصي"))
        avatar=field("https://...");card.addView(avatar,LinearLayout.LayoutParams(-1,-2))
        card.addView(TextView(this).apply{text="ملاحظة: رفع الصور من الهاتف يحتاج خدمة رفع ملفات على الخادم؛ هذا الحقل يقبل رابط صورة فقط حالياً.";textSize=11f;setTextColor(Color.rgb(135,130,150));setPadding(0,dp(7),0,0)})
        status=TextView(this).apply{textSize=12f;setTextColor(Color.rgb(180,65,75));setPadding(0,dp(10),0,dp(4))}
        card.addView(status)
        val save=TextView(this).apply{text="حفظ التعديلات";textSize=15f;gravity=Gravity.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=android.graphics.drawable.GradientDrawable().apply{setColor(purple);cornerRadius=dp(14).toFloat()};setPadding(0,dp(15),0,dp(15));setOnClickListener{saveProfile()}}
        card.addView(save,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
        setContentView(ScrollView(this).apply{addView(root)})
        loadProfile(idView)
    }
    private fun loadProfile(idView:TextView){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){finish();return}
        status.text="جارٍ تحميل الملف الشخصي..."
        executor.execute{
            try{
                val me=AuthApi.get("/api/auth/me",token)
                if(me.status !in 200..299)throw Exception(me.body.optString("error","تعذر تحميل الحساب"))
                val user=me.body.optJSONObject("user")?:me.body
                userId=user.optString("id")
                val profile=AuthApi.get("/api/users/$userId",token)
                val data=if(profile.status in 200..299)(profile.body.optJSONObject("user")?:profile.body)else user
                runOnUiThread{
                    idView.text=if(userId.isNotBlank())userId else "غير متوفر"
                    displayName.setText(data.optString("display_name").takeUnless{it=="null"}?:user.optString("username"))
                    bio.setText(data.optString("bio").takeUnless{it=="null"}?:"")
                    country.setText(data.optString("country").takeUnless{it=="null"}?:"")
                    avatar.setText(data.optString("avatar_url").takeUnless{it=="null"}?:"")
                    status.text=""
                }
            }catch(e:Exception){runOnUiThread{status.text=e.message?:"تعذر تحميل البيانات"}}
        }
    }
    private fun saveProfile(){
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)?:return
        val name=displayName.text.toString().trim()
        if(name.isBlank()||name.length>60){status.text="اكتب اسماً من 1 إلى 60 حرفاً";return}
        val payload=JSONObject().put("display_name",name)
            .put("bio",bio.text.toString().trim())
            .put("country",country.text.toString().trim())
            .put("avatar_url",avatar.text.toString().trim())
        status.text="جارٍ حفظ التعديلات..."
        executor.execute{
            try{
                val result=AuthApi.put("/api/profile",payload,token)
                runOnUiThread{status.text=if(result.status in 200..299)"تم حفظ الملف الشخصي بنجاح" else result.body.optString("error","تعذر حفظ التعديلات")}
            }catch(_:Exception){runOnUiThread{status.text="تعذر الاتصال بالخادم"}}
        }
    }
    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
