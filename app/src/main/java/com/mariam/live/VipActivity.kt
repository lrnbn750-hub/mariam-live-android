package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.Executors

class VipActivity:Activity(){
 private val worker=Executors.newSingleThreadExecutor()
 private val green=Color.rgb(25,151,83); private val dark=Color.rgb(25,38,31); private val muted=Color.rgb(112,128,117)
 private var token=""; private var packages=JSONArray(); private var balance=0L; private var selected=1; private var membership=JSONObject()
 private lateinit var body:LinearLayout; private lateinit var status:TextView
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun txt(s:String,n:Float,c:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
 private fun bg(c:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply{setColor(c);cornerRadius=dp(17).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);token=getSharedPreferences("MariamLive",0).getString("token","").orEmpty()
 window.statusBarColor=Color.rgb(246,250,247);window.navigationBarColor=Color.WHITE;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
 val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(246,250,247))}
 val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(6),dp(14),dp(6))}
 head.addView(txt("‹",34f,dark).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
 head.addView(txt("عضوية VIP",20f,dark,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f));root.addView(head)
 val scroll=ScrollView(this);body=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(24))};scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);load()
 }
 private fun load(){worker.execute{try{val p=AuthApi.get("/api/vip/packages",token);val w=AuthApi.get("/api/wallet",token);val m=AuthApi.get("/api/vip/me",token)
 if(p.status !in 200..299)throw Exception(p.body.optString("error","تعذر تحميل VIP"))
 val wa=w.body.optJSONObject("wallet")?:w.body;val mem=m.body.optJSONObject("membership")?:m.body.optJSONObject("vip")?:JSONObject()
 runOnUiThread{packages=p.body.optJSONArray("packages")?:JSONArray();balance=wa.optLong("recharge_coins",0);membership=mem;render()}
 }catch(e:Exception){runOnUiThread{body.removeAllViews();body.addView(txt(e.message?:"تعذر الاتصال",14f,dark))}}}}
 private fun button(s:String,click:()->Unit)=txt(s,14f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setPadding(dp(14),dp(14),dp(14),dp(14));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(34,174,99),green)).apply{cornerRadius=dp(15).toFloat()};setOnClickListener{click()}}
 private fun render(){body.removeAllViews()
 val hero=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(18),dp(22),dp(18),dp(22));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(18,77,48),Color.rgb(24,133,72))).apply{cornerRadius=dp(23).toFloat();setStroke(dp(1),0x66DDBB72)}}
 hero.addView(txt("✦  BAK LIVE  ✦",12f,Color.rgb(255,224,156),true));hero.addView(txt("VIP",42f,Color.WHITE,true));hero.addView(txt("مزايا حصرية • عضوية لمدة 30 يوماً",12f,Color.WHITE));body.addView(hero)
 val wallet=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));background=bg(Color.WHITE,Color.rgb(220,233,223))}
 wallet.addView(txt("رصيد محفظة الشحن\n"+String.format("%,d",balance)+" 💎",17f,dark,true),LinearLayout.LayoutParams(0,-2,1f));wallet.addView(txt("💎",26f,green));body.addView(wallet,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
 val cur=membership.optInt("vip_level",0);if(cur>0)body.addView(txt("عضويتك الحالية VIP $cur • تنتهي "+membership.optString("expires_at",""),13f,green,true))
 body.addView(txt("اختر مستوى VIP",19f,dark,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20);bottomMargin=dp(10)})
 for(i in 0 until packages.length()){val p=packages.optJSONObject(i)?:continue;val l=p.optInt("vip_level",i+1);val price=p.optLong("price_diamonds",0);val active=p.optBoolean("active",true);val enough=balance>=price&&price>0&&active
 val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=bg(if(selected==l)Color.rgb(239,249,241) else Color.WHITE,if(selected==l)green else Color.rgb(225,232,226));isClickable=true;setOnClickListener{selected=l;render()}}
 row.addView(txt("VIP $l",16f,dark,true),LinearLayout.LayoutParams(dp(75),dp(35)))
 row.addView(txt(String.format("%,d",price)+" ماسة\n"+if(enough)"متاح للشراء" else if(active)"رصيد غير كافٍ" else "غير متاح",12f,if(enough)green else muted),LinearLayout.LayoutParams(0,-2,1f))
 row.addView(txt(if(selected==l)"●" else "○",20f,green,true));body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
 }
 val p=find(selected);val price=p?.optLong("price_diamonds",0)?:0L;val share=price*35/100;val cashback=price-share
 val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(13),dp(14),dp(13));background=bg(Color.WHITE,Color.rgb(225,232,226))}
 panel.addView(txt("تفاصيل شراء VIP $selected",14f,dark,true));panel.addView(txt("السعر: "+String.format("%,d",price)+" ماسة",13f,dark));panel.addView(txt("حصة BAK LIVE 35%: "+String.format("%,d",share),12f,muted));panel.addView(txt("يرجع للمحفظة 65%: "+String.format("%,d",cashback),12f,green,true));panel.addView(txt("الاستقطاع من رصيد محفظة الشحن فقط، وليس عند الشحن.",11f,muted));body.addView(panel,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
 status=txt("",12f,green);body.addView(status,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
 body.addView(button("شراء VIP $selected • 30 يوم"){if(p==null||price<=0||!p.optBoolean("active",true))status.text="الباقة غير متاحة" else if(balance<price)status.text="رصيدك غير كافٍ" else AlertDialog.Builder(this).setTitle("تأكيد الشراء").setMessage("سيتم خصم سعر الباقة من محفظتك، واحتساب 35% حصة للتطبيق وإرجاع 65% إلى محفظتك.").setNegativeButton("إلغاء",null).setPositiveButton("تأكيد"){_,_->buy()}.show()},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
 }
 private fun find(l:Int):JSONObject?{for(i in 0 until packages.length()){val p=packages.optJSONObject(i)?:continue;if(p.optInt("vip_level")==l)return p};return null}
 private fun buy(){status.text="جارٍ تنفيذ الشراء...";worker.execute{try{val r=AuthApi.post("/api/vip/purchase",JSONObject().put("vip_level",selected),token);runOnUiThread{if(r.status in 200..299&&r.body.optBoolean("success",true)){Toast.makeText(this,"تم تفعيل VIP $selected لمدة 30 يوماً",Toast.LENGTH_LONG).show();load()}else status.text=r.body.optString("error","فشل الشراء")}}catch(e:Exception){runOnUiThread{status.text=e.message?:"تعذر الاتصال"}}}}
 override fun onDestroy(){worker.shutdownNow();super.onDestroy()}
}