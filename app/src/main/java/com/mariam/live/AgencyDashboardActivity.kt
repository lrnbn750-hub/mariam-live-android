package com.mariam.live
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class AgencyDashboardActivity: Activity() {
 private val worker=Executors.newSingleThreadExecutor()
 private val bg=Color.rgb(12,10,20); private val panel=Color.rgb(28,22,42); private val ink=Color.WHITE; private val muted=Color.rgb(167,157,185)
 private lateinit var body:LinearLayout; private lateinit var input:EditText; private lateinit var send:TextView; private lateinit var status:TextView; private lateinit var invitesBox:LinearLayout; private lateinit var membersBox:LinearLayout; private lateinit var stats:LinearLayout; private lateinit var analyticsText:TextView; private lateinit var memberSearch:EditText; private lateinit var inviteSearch:EditText
 private var invites=JSONArray(); private var members=JSONArray(); private var agencyName="وكالتك"
 private fun token()=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun card()=GradientDrawable().apply{cornerRadius=dp(16).toFloat();setColor(panel);setStroke(dp(1),Color.rgb(58,43,76))}
 private fun t(s:String,z:Float=14f,c:Int=ink,b:Boolean=false)=TextView(this).apply{text=s;textSize=z;setTextColor(c);if(b)typeface=android.graphics.Typeface.DEFAULT_BOLD}
 private fun button(s:String,action:()->Unit)=t(s,14f,ink,true).apply{gravity=Gravity.CENTER;setPadding(dp(10),dp(14),dp(10),dp(14));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(145,69,214),Color.rgb(91,45,149))).apply{cornerRadius=dp(13).toFloat()};setOnClickListener{action()}}
 private fun section(s:String,sub:String=""){
  body.addView(t(s,17f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20);bottomMargin=dp(5)})
  if(sub.isNotBlank()) body.addView(t(sub,12f,muted))
 }
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg)}
  val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(8),dp(16),dp(8))}
  head.addView(t("‹",32f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
  head.addView(t("لوحة الوكالة",19f,ink,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f));head.addView(t("BAK",12f,Color.rgb(172,104,245),true));root.addView(head)
  val scroll=ScrollView(this);body=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(26))};scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(18),dp(18),dp(18));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(107,47,158),Color.rgb(36,22,58))).apply{cornerRadius=dp(22).toFloat()}}
  hero.addView(t("AGENCY • PARTNER DASHBOARD",10f,Color.rgb(228,204,248),true));hero.addView(t("إدارة وكالتك وصُنّاع المحتوى",19f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)});body.addView(hero)
  section("ملخص الوكالة","بيانات حقيقية من السيرفر")
  stats=LinearLayout(this).apply{orientation=0}
  stats.addView(statCard("الأعضاء","—"),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)})
  stats.addView(statCard("دعوات معلّقة","—"),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(5)})
  body.addView(stats)
  analyticsText=t("مقبولة: —   •   مرفوضة: —   •   ملغاة: —",12f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));background=card()};body.addView(analyticsText,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
  status=t("جارٍ تحميل البيانات...",12f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(10),dp(10),dp(10),dp(10))};body.addView(status)
  body.addView(button("↻ تحديث بيانات الوكالة"){loadData()},LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(5)})
  section("دعوة صانع محتوى","أدخل ID المستخدم لإرسال دعوة حقيقية")
  input=EditText(this).apply{hint="معرّف المستخدم (ID)";setSingleLine();textSize=15f;setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),dp(10),dp(14),dp(10));background=card()}
  body.addView(input,LinearLayout.LayoutParams(-1,dp(52)))
  send=button("إرسال دعوة الوكالة"){sendInvitation()};body.addView(send,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(9)})
  invitesBox=LinearLayout(this).apply{orientation=1};body.addView(invitesBox,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
  section("دعوات الوكالة","حالات الدعوات المحفوظة")
  inviteSearch=EditText(this).apply{hint="بحث بالدعوة: الاسم أو ID أو الحالة";setSingleLine();textSize=14f;setTextColor(ink);setHintTextColor(muted);setPadding(dp(12),dp(10),dp(12),dp(10));background=card()};body.addView(inviteSearch,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(8)})
  section("أعضاء الوكالة","المستخدمون الذين قبلوا الدعوة")
  memberSearch=EditText(this).apply{hint="بحث بالاسم أو ID";setSingleLine();textSize=14f;setTextColor(ink);setHintTextColor(muted);setPadding(dp(12),dp(10),dp(12),dp(10));background=card()};body.addView(memberSearch,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(8)})
  membersBox=LinearLayout(this).apply{orientation=1};body.addView(membersBox)
  inviteSearch.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){renderInvites()};override fun afterTextChanged(s:android.text.Editable?){}})
  memberSearch.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){renderMembers()};override fun afterTextChanged(s:android.text.Editable?){}})
  body.addView(t("بيانات الدعوات والأعضاء من قاعدة البيانات. تقارير الألماس والأرباح ستُربط عند توفر مصدرها الحقيقي.",11f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(14),dp(14),dp(14),dp(14));background=card()},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
  loadData()
 }
 private fun statCard(title:String,value:String)=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(14),dp(14),dp(14));background=card();addView(t(title,12f,muted,true));addView(t(value,22f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})}
 private fun loadData(){val tok=token()?:run{status.text="يرجى تسجيل الدخول مجدداً";return};status.text="جارٍ تحديث البيانات..."
 worker.execute{try{
  val m=AuthApi.get("/api/agency/members",tok);val i=AuthApi.get("/api/agency/invitations",tok)
  if(m.status !in 200..299)throw Exception(m.body.optString("error","فشل تحميل الأعضاء"))
  if(i.status !in 200..299)throw Exception(i.body.optString("error","فشل تحميل الدعوات"))
  val mm=m.body.optJSONArray("members")?:JSONArray();val ii=i.body.optJSONArray("invitations")?:JSONArray();val ag=m.body.optJSONObject("agency")
  runOnUiThread{members=mm;invites=ii;agencyName=ag?.optString("agency_name")?.takeIf{it.isNotBlank()}?:agencyName;status.text="تم تحديث $agencyName";render()}
 }catch(e:Exception){runOnUiThread{status.text="تعذر تحميل البيانات: ${e.message}";render()}}}
 }
 private fun sendInvitation(){val id=input.text.toString().trim();if(id.isBlank()||id.length>40){input.error="أدخل ID صحيح";return};val tok=token()?:run{status.text="يرجى تسجيل الدخول";return};send.isEnabled=false;status.text="جارٍ إرسال الدعوة..."
 worker.execute{try{val r=AuthApi.post("/api/agency/invitations",JSONObject().put("public_id",id),tok);if(r.status !in 200..299)throw Exception(r.body.optString("error","تعذر إرسال الدعوة"));runOnUiThread{input.setText("");status.text="تم إرسال الدعوة إلى ID $id";send.isEnabled=true};loadData()}catch(e:Exception){runOnUiThread{status.text=e.message?: "تعذر إرسال الدعوة";send.isEnabled=true}}}
 }
 private fun renderInvites(){if(!::invitesBox.isInitialized)return;invitesBox.removeAllViews();val q=if(::inviteSearch.isInitialized)inviteSearch.text.toString().trim().lowercase() else "";val filtered=(0 until invites.length()).mapNotNull{invites.optJSONObject(it)}.filter{o->val id=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $id"};val status=o.optString("status");val arabicStatus=when(status){"pending"->"بانتظار الرد";"accepted"->"مقبولة";"rejected"->"مرفوضة";"cancelled"->"ملغاة";else->status};name.lowercase().contains(q)||id.lowercase().contains(q)||status.lowercase().contains(q)||arabicStatus.lowercase().contains(q)};if(filtered.isEmpty()){invitesBox.addView(t(if(q.isNotBlank())"لا توجد دعوات مطابقة" else "لا توجد دعوات",12f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12))});return};for(o in filtered){val uid=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $uid"};val st=when(o.optString("status")){"pending"->"بانتظار الرد";"accepted"->"مقبولة";"rejected"->"مرفوضة";"cancelled"->"ملغاة";else->o.optString("status")};val row=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(12),dp(12),dp(12));background=card()};row.addView(t(name,14f,ink,true));row.addView(t("ID $uid • $st",12f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});invitesBox.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})}}
 private fun renderMembers(){if(!::membersBox.isInitialized)return;membersBox.removeAllViews();val query=if(::memberSearch.isInitialized)memberSearch.text.toString().trim().lowercase() else "";val filtered=(0 until members.length()).mapNotNull{members.optJSONObject(it)}.filter{ o-> val id=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $id"};name.lowercase().contains(query)||id.lowercase().contains(query)};if(filtered.isEmpty()){membersBox.addView(t(if(query.isNotBlank())"لا توجد نتائج مطابقة" else "لا يوجد أعضاء منضمّون بعد",12f,muted).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12))});return};for(o in filtered){val id=o.optString("public_id","—");val name=o.optString("display_name").ifBlank{"ID $id"};val row=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(12),dp(12),dp(12));background=card()};row.addView(t(name,14f,ink,true));row.addView(t("ID $id • عضو فعلي",12f,Color.rgb(130,220,170)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});membersBox.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})}}
 private fun render(){if(!::invitesBox.isInitialized||!::membersBox.isInitialized)return;invitesBox.removeAllViews();membersBox.removeAllViews()
  val pending=(0 until invites.length()).count{invites.optJSONObject(it)?.optString("status")=="pending"}; val accepted=(0 until invites.length()).count{invites.optJSONObject(it)?.optString("status")=="accepted"}; val rejected=(0 until invites.length()).count{invites.optJSONObject(it)?.optString("status")=="rejected"}; val cancelled=(0 until invites.length()).count{invites.optJSONObject(it)?.optString("status")=="cancelled"}; analyticsText.text="مقبولة: $accepted   •   مرفوضة: $rejected   •   ملغاة: $cancelled   •   إجمالي الدعوات: ${invites.length()}"
  (stats.getChildAt(0) as? LinearLayout)?.let{(it.getChildAt(1) as? TextView)?.text=members.length().toString()}
  (stats.getChildAt(1) as? LinearLayout)?.let{(it.getChildAt(1) as? TextView)?.text=pending.toString()}
  renderInvites()
    renderMembers()
 }
}
