package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

// BAK LIVE creator earnings screen.
class CreatorCenterActivity : Activity() {
 private var token = ""
 private lateinit var content: LinearLayout
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun money(c:Long)=String.format(java.util.Locale.US,"%.2f",c/100.0)
 private fun label(s:String,n:Float,color:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(color);if(b) setTypeface(null,1)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty();window.statusBarColor=Color.rgb(18,13,28);window.navigationBarColor=Color.rgb(18,13,28)
 val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(18,13,28))}
 val head=TextView(this).apply{text="‹          مركز صُنّاع المحتوى";textSize=19f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(16),dp(10));setOnClickListener{finish()}};root.addView(head,LinearLayout.LayoutParams(-1,dp(56)))
 val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(24))};scroll.addView(content);root.addView(scroll);setContentView(root);load()
 }
 private fun load(){content.removeAllViews();content.addView(label("جارٍ تحميل بيانات الأرباح...",14f,Color.LTGRAY));thread{try{val r=AuthApi.get("/api/wallet",token);runOnUiThread{content.removeAllViews();if(r.status !in 200..299){content.addView(label(r.body.optString("error","تعذر تحميل الأرباح"),14f,Color.WHITE));return@runOnUiThread};val w=r.body.optJSONObject("wallet")?:r.body;show(w)}}}catch(e:Exception){runOnUiThread{content.removeAllViews();content.addView(label(e.message?:"تعذر الاتصال",14f,Color.WHITE))}}}}
 private fun show(w:org.json.JSONObject){
 val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(20),dp(20),dp(20));background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(91,38,128),Color.rgb(42,25,61))).apply{cornerRadius=dp(22).toFloat()}}
 hero.addView(label("الأرباح المتاحة للسحب",14f,Color.LTGRAY,true));hero.addView(label("$"+money(w.optLong("withdrawable_usd_cents")),36f,Color.rgb(104,232,177),true));hero.addView(label("إجمالي الأرباح: $"+money(w.optLong("lifetime_earned_usd_cents")),14f,Color.WHITE));content.addView(hero)
 val stats=LinearLayout(this).apply{orientation=0};stats.addView(label("💎  الألماس المكتسب\n"+w.optLong("earned_diamonds"),16f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(90),1f));stats.addView(label("50%\nحصة المذيع من الهدايا",16f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(90),1f));content.addView(stats)
 content.addView(label("تقسيم الأرباح: 50% للمذيع و50% للمنصة",13f,Color.rgb(220,190,240),true))
 content.addView(label("طلب سحب الأرباح متاح بعد ربط واعتماد نظام السحب بالدولار من الخادم.",13f,Color.LTGRAY))
 content.addView(TextView(this).apply{text="تحديث البيانات";gravity=Gravity.CENTER;textSize=15f;setTextColor(Color.WHITE);setPadding(dp(12),dp(14),dp(12),dp(14));setOnClickListener{load()};background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=dp(14).toFloat();setColor(Color.rgb(105,46,145))}},LinearLayout.LayoutParams(-1,dp(50)))
 }
}