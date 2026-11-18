package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import java.util.concurrent.Executors

class AgencyAnalyticsActivity : Activity() {
 private val worker=Executors.newSingleThreadExecutor()
 private val bg=Color.rgb(12,10,20); private val panel=Color.rgb(28,22,42); private val ink=Color.WHITE; private val muted=Color.rgb(167,157,185)
 private lateinit var body:LinearLayout; private lateinit var status:TextView
 private var members=JSONArray(); private var invitations=JSONArray()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun token()=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
 private fun cardBg()=GradientDrawable().apply{cornerRadius=dp(16).toFloat();setColor(panel);setStroke(dp(1),Color.rgb(58,43,76))}
 private fun t(s:String,z:Float=14f,c:Int=ink,b:Boolean=false)=TextView(this).apply{text=s;textSize=z;setTextColor(c);if(b)typeface=Typeface.DEFAULT_BOLD}
 private fun metric(title:String,value:String,color:Int)=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(14),dp(14),dp(14));background=cardBg();addView(t(title,12f,muted,true));addView(t(value,23f,color,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})}
 private fun row(title:String,detail:String,color:Int=muted)=LinearLayout(this).apply{orientation=1;setPadding(dp(13),dp(12),dp(13),dp(12));background=cardBg();addView(t(title,14f,ink,true));addView(t(detail,12f,color),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})}
 private fun section(s:String){body.addView(t(s,17f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18);bottomMargin=dp(8)})}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg)}
  val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(8),dp(16),dp(8))}
  head.addView(t("‹",32f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
  head.addView(t("إحصائيات الوكالة",19f,ink,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f));head.addView(t("BAK",12f,Color.rgb(172,104,245),true));root.addView(head)
  val scroll=ScrollView(this);body=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(26))};scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);loading();load()
 }
 private fun loading(){body.removeAllViews();val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(18));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(107,47,158),Color.rgb(36,22,58))).apply{cornerRadius=dp(22).toFloat()}}
 hero.addView(t("BAK LIVE • AGENCY INSIGHTS",10f,Color.rgb(228,204,248),true));hero.addView(t("أداء وكالتك في مكان واحد",19f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(9)});hero.addView(t("إحصائيات من بيانات الوكالة المسجلة",12f,Color.rgb(220,205,236)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)});body.addView(hero)
 status=t("جارٍ تحميل البيانات...",12f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(8),dp(12),dp(8),dp(12))};body.addView(status)
 }
 private fun load(){val tok=token()?:run{status.text="يرجى تسجيل الدخول مجدداً";return};status.text="جارٍ تحديث البيانات..."
 worker.execute{try{val m=AuthApi.get("/api/agency/members",tok);val i=AuthApi.get("/api/agency/invitations",tok);if(m.status !in 200..299)throw Exception(m.body.optString("error","تعذر تحميل الأعضاء"));if(i.status !in 200..299)throw Exception(i.body.optString("error","تعذر تحميل الدعوات"));val mm=m.body.optJSONArray("members")?:JSONArray();val ii=i.body.optJSONArray("invitations")?:JSONArray();runOnUiThread{members=mm;invitations=ii;render()}}catch(e:Exception){runOnUiThread{status.text=e.message?: "تعذر الاتصال";val retry=t("إعادة المحاولة",14f,ink,true).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));background=cardBg();setOnClickListener{load()}};body.addView(retry,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(8)}})}}
 }
 private fun count(s:String)=(0 until invitations.length()).count{invitations.optJSONObject(it)?.optString("status")==s}
 private fun render(){body.removeAllViews();loading();body.removeAllViews()
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(18));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(107,47,158),Color.rgb(36,22,58))).apply{cornerRadius=dp(22).toFloat()}}
  hero.addView(t("BAK LIVE • AGENCY INSIGHTS",10f,Color.rgb(228,204,248),true));hero.addView(t("لوحة أداء الوكالة",20f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)});hero.addView(t("تحديث مباشر من بيانات الحساب",12f,Color.rgb(220,205,236)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)});body.addView(hero)
  status=t("تم تحديث البيانات • ${members.length()} عضو",12f,Color.rgb(130,220,170)).apply{gravity=Gravity.CENTER;setPadding(dp(8),dp(12),dp(8),dp(12))};body.addView(status)
  val a=LinearLayout(this).apply{orientation=0};a.addView(metric("الأعضاء الفعليون",members.length().toString(),Color.rgb(130,220,170)),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)});a.addView(metric("دعوات معلّقة",count("pending").toString(),Color.rgb(230,190,255)),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5)});body.addView(a)
  val b=LinearLayout(this).apply{orientation=0};b.addView(metric("دعوات مقبولة",count("accepted").toString(),Color.rgb(130,220,170)),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5);topMargin=dp(10)});b.addView(metric("دعوات مرفوضة",count("rejected").toString(),Color.rgb(255,145,145)),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5);topMargin=dp(10)});body.addView(b)
  val c=LinearLayout(this).apply{orientation=0};c.addView(metric("دعوات ملغاة",count("cancelled").toString(),muted),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5);topMargin=dp(10)});c.addView(metric("إجمالي الدعوات",invitations.length().toString(),Color.rgb(220,190,245)),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5);topMargin=dp(10)});body.addView(c)
  val refresh=t("⟳  تحديث الإحصائيات",14f,ink,true).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(13),dp(12),dp(13));background=GradientDrawable().apply{cornerRadius=dp(14).toFloat();setColor(Color.rgb(112,57,168))};setOnClickListener{load()}};body.addView(refresh,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(14)})
  section("أعضاء الوكالة")
  if(members.length()==0)body.addView(row("لا يوجد أعضاء حالياً","عند انضمام صانع محتوى سيظهر هنا")) else for(n in 0 until members.length()){val o=members.optJSONObject(n)?:continue;val id=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $id"};body.addView(row(name,"ID $id • عضو فعّال",Color.rgb(130,220,170)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})}
  section("آخر الدعوات")
  if(invitations.length()==0)body.addView(row("لا توجد دعوات","لم يتم إرسال دعوات حتى الآن")) else for(n in 0 until invitations.length()){val o=invitations.optJSONObject(n)?:continue;val id=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $id"};val st=when(o.optString("status")){"pending"->"بانتظار الرد";"accepted"->"مقبولة";"rejected"->"مرفوضة";"cancelled"->"ملغاة";else->o.optString("status","غير معروف")};body.addView(row(name,"ID $id • $st"),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})}
  body.addView(t("هذه الشاشة تعرض بيانات الأعضاء والدعوات فقط. الأرباح والألماس تحتاج إلى مصدر بيانات مستقل.",11f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
 }
}
