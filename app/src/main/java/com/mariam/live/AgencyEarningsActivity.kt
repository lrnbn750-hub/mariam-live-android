package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class AgencyEarningsActivity : Activity() {
 private val bg=Color.rgb(12,10,20); private val panel=Color.rgb(29,22,40); private val muted=Color.rgb(169,157,185); private val accent=Color.rgb(120,226,181)
 private lateinit var body:LinearLayout; private var token=""; private var overview=JSONObject(); private var members=JSONArray(); private var history=JSONArray()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun money(c:Long)=String.format(java.util.Locale.US,"$%.2f",c/100.0)
 private fun card()=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(42,31,55),panel)).apply{cornerRadius=dp(17).toFloat();setStroke(dp(1),Color.rgb(76,57,92))}
 private fun t(s:String,z:Float,c:Int=Color.WHITE,b:Boolean=false)=TextView(this).apply{text=s;textSize=z;setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
 private fun section(s:String,sub:String=""){body.addView(t(s,17f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20);bottomMargin=dp(5)});if(sub.isNotBlank())body.addView(t(sub,11f,muted))}
 private fun stat(title:String,value:String,icon:String)=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(10),dp(14),dp(10),dp(14));background=card();addView(t(icon,22f,accent,true));addView(t(title,11f,muted,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});addView(t(value,18f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty();window.statusBarColor=bg;window.navigationBarColor=bg
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg)}
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(8),dp(16),dp(8))}
  header.addView(t("‹",32f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
  header.addView(t("أرباح الوكالة",19f,Color.WHITE,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f));header.addView(t("BAK",12f,accent,true));root.addView(header)
  val scroll=ScrollView(this);body=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(28)};scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);load()
 }
 private fun load(){body.removeAllViews();body.addView(t("جارٍ تحميل الأرباح...",13f,muted));if(token.isBlank()){error("يرجى تسجيل الدخول مجدداً");return}
  thread{try{val o=AuthApi.get("/api/agency/earnings",token);val m=AuthApi.get("/api/agency/earnings/members",token);val h=AuthApi.get("/api/agency/earnings/history",token)
   if(o.status !in 200..299)throw Exception(o.body.optString("error","تعذر تحميل الأرباح"));if(m.status !in 200..299)throw Exception(m.body.optString("error","تعذر تحميل الأعضاء"));if(h.status !in 200..299)throw Exception(h.body.optString("error","تعذر تحميل السجل"))
   val ov=o.body.optJSONObject("earnings")?:o.body.optJSONObject("overview")?:o.body;val mm=m.body.optJSONArray("members")?:JSONArray();val hh=h.body.optJSONArray("history")?:h.body.optJSONArray("earnings")?:JSONArray()
   runOnUiThread{overview=ov;members=mm;history=hh;render()}
  }catch(e:Exception){runOnUiThread{error(e.message?: "تعذر الاتصال بالخادم")}}}
 }
 private fun error(msg:String){body.removeAllViews();body.addView(t(msg,14f));body.addView(t("إعادة المحاولة",14f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setPadding(dp(14),dp(14),dp(14),dp(14));background=card();setOnClickListener{load()}},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})}
 private fun v(vararg keys:String):Long{for(k in keys)if(overview.has(k)&&!overview.isNull(k))return overview.optLong(k,0);return 0}
 private fun render(){body.removeAllViews()
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(20),dp(18),dp(20));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(105,44,150),Color.rgb(34,22,52))).apply{cornerRadius=dp(22).toFloat()}}
  hero.addView(t("BAK LIVE • AGENCY PARTNER",10f,Color.rgb(226,205,244),true));hero.addView(t("إجمالي أرباح الوكالة",15f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)});hero.addView(t(money(v("total_cents","total_earnings_cents","total_usd_cents")),34f,accent,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)});hero.addView(t("عمولة الوكالة 12% من هدايا الأعضاء المؤهلين",12f,Color.rgb(232,214,245)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)});body.addView(hero)
  val s1=LinearLayout(this).apply{orientation=0};s1.addView(stat("المتاح",money(v("available_cents","available_earnings_cents")),"💰"),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)});s1.addView(stat("المدفوع",money(v("paid_cents","paid_earnings_cents")),"✓"),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5)});body.addView(s1,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  val s2=LinearLayout(this).apply{orientation=0};s2.addView(stat("اليوم",money(v("today_cents","today_earnings_cents")),"☀"),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)});s2.addView(stat("7 أيام",money(v("last_7_days_cents","week_cents")),"↗"),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5)});body.addView(s2,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  body.addView(stat("هذا الشهر",money(v("month_cents","monthly_cents","month_earnings_cents")),"▣"),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  section("أعضاء الوكالة","ترتيب حسب إجمالي أرباح العضو")
  if(members.length()==0)body.addView(t("لا توجد أرباح مسجلة للأعضاء حتى الآن",12f,muted).apply{setPadding(dp(12),dp(18),dp(12),dp(18)})
  for(i in 0 until members.length()){val o=members.optJSONObject(i)?:continue;val name=o.optString("display_name").ifBlank{o.optString("username").ifBlank{"ID "+o.optString("public_id","—")}};val total=o.optLong("total_cents",o.optLong("total_earnings_cents",0));val avail=o.optLong("available_cents",0);val row=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(13),dp(14),dp(13));background=card()};row.addView(t(name,14f,Color.WHITE,true));row.addView(t("إجمالي "+money(total)+" • متاح "+money(avail),12f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)});body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})}
  section("سجل العمولات","عمولات الهدايا الجديدة فقط")
  if(history.length()==0)body.addView(t("لا توجد عمليات أرباح بعد",12f,muted).apply{setPadding(dp(12),dp(18),dp(12),dp(18)})
  for(i in 0 until history.length()){val o=history.optJSONObject(i)?:continue;val who=o.optString("display_name").ifBlank{o.optString("username").ifBlank{"عضو الوكالة"}};val gift=o.optString("gift_id").ifBlank{"هدية"};val amount=o.optLong("amount_cents",o.optLong("amount",0));val time=o.optString("created_at","");val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=card()};val info=LinearLayout(this).apply{orientation=1};info.addView(t(who+" • "+gift,13f,Color.WHITE,true));if(time.isNotBlank())info.addView(t(time,10f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});row.addView(info,LinearLayout.LayoutParams(0,-2,1f));row.addView(t("+"+money(amount),15f,accent,true));body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})}
  body.addView(t("تحديث البيانات",14f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setPadding(dp(14),dp(14),dp(14),dp(14));background=card();setOnClickListener{load()}},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
 }
}
