package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class CreatorCenterActivity : Activity() {
 private val bg=Color.rgb(15,11,23); private val panel=Color.rgb(35,25,48); private val muted=Color.rgb(177,165,191); private val mint=Color.rgb(99,231,178); private val violet=Color.rgb(120,64,164)
 private var token=""; private var wallet=JSONObject(); private var history=JSONArray(); private var incomingInvitations=JSONArray(); private var invitationLoadError=""; private lateinit var content:LinearLayout
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun money(c:Long)=String.format(java.util.Locale.US,"%,.2f",c/100.0)
 private fun shape(colors:IntArray,stroke:Int=Color.TRANSPARENT,r:Int=20)=GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply{cornerRadius=dp(r).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
 private fun label(s:String,n:Float,color:Int,b:Boolean=false)=TextView(this).apply{text=s;textSize=n;setTextColor(color);typeface=Typeface.create("sans-serif",if(b)Typeface.BOLD else Typeface.NORMAL)}
 private fun button(s:String,click:()->Unit)=TextView(this).apply{text=s;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;setPadding(dp(12),dp(14),dp(12),dp(14));background=shape(intArrayOf(Color.rgb(126,59,174),Color.rgb(75,34,111)),Color.rgb(155,104,184),15);setOnClickListener{click()}}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty();window.statusBarColor=bg;window.navigationBarColor=bg
 val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg)}
 val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(8),dp(18),dp(8))}
 header.addView(label("‹",34f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
 header.addView(label("مركز صُنّاع المحتوى",20f,Color.WHITE,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
 header.addView(label("BAK",12f,mint,true));root.addView(header)
 val scroll=ScrollView(this).apply{isFillViewport=true;overScrollMode=ScrollView.OVER_SCROLL_NEVER};content=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(10),dp(18),dp(28))};scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);load()
 }
 private fun load(){content.removeAllViews();content.addView(label("جارٍ تحميل مركز الأرباح...",14f,muted));thread{try{val w=AuthApi.get("/api/wallet",token);val h=AuthApi.get("/api/wallet/transactions?limit=100",token);val invites=AuthApi.get("/api/agency/invitations/incoming",token);runOnUiThread{if(w.status !in 200..299){showError(w.body.optString("error","تعذر تحميل البيانات"));return@runOnUiThread};wallet=w.body.optJSONObject("wallet")?:w.body;history=h.body.optJSONArray("transactions")?:JSONArray();incomingInvitations=if(invites.status in 200..299) invites.body.optJSONArray("invitations")?:JSONArray() else JSONArray();invitationLoadError=if(invites.status in 200..299) "" else invites.body.optString("error","تعذر تحميل دعوات الوكالة");render()}}catch(e:Exception){runOnUiThread{showError(e.message?:"تعذر الاتصال بالخادم")}}}}
 private fun showError(s:String){content.removeAllViews();content.addView(label(s,14f,Color.WHITE));content.addView(button("إعادة المحاولة"){load()})}
 private fun section(s:String,sub:String=""){content.addView(label(s,18f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(22);bottomMargin=dp(6)});if(sub.isNotBlank())content.addView(label(sub,11f,muted))}
 private fun stat(icon:String,title:String,value:String):LinearLayout=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(10),dp(16),dp(10),dp(16));background=shape(intArrayOf(Color.rgb(46,32,59),Color.rgb(29,22,40)),Color.rgb(83,62,101),18);addView(label(icon,24f,mint,true));addView(label(title,11f,muted,true));addView(label(value,19f,Color.WHITE,true))}
 private fun render(){content.removeAllViews()
 val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(20),dp(20),dp(20));background=shape(intArrayOf(Color.rgb(103,43,143),Color.rgb(38,24,55)),Color.rgb(150,91,185),24)}
 hero.addView(label("BAK LIVE  •  CREATOR STUDIO",10f,Color.rgb(230,207,242),true));hero.addView(label("لوحة أرباح صانع المحتوى",15f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(34)));hero.addView(label("$"+money(wallet.optLong("withdrawable_usd_cents")),38f,mint,true),LinearLayout.LayoutParams(-1,dp(58)));hero.addView(label("الرصيد المتاح للسحب",12f,Color.rgb(232,213,244),true));
 val line=View(this).apply{setBackgroundColor(Color.rgb(123,78,148))};hero.addView(line,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(15);bottomMargin=dp(13)});hero.addView(label("إجمالي الأرباح المكتسبة  $"+money(wallet.optLong("lifetime_earned_usd_cents")),13f,Color.WHITE,true));content.addView(hero)
 val stats=LinearLayout(this).apply{orientation=0};stats.addView(stat("💎","إجمالي الألماس المكتسب",String.format(java.util.Locale.US,"%,d",wallet.optLong("earned_diamonds"))),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(6)});stats.addView(stat("50%","حصة المذيع", "من قيمة الهدايا"),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(6)});content.addView(stats,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
 val split=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=shape(intArrayOf(panel,panel),Color.rgb(76,57,92),16)};split.addView(label("المذيع 50%",13f,mint,true),LinearLayout.LayoutParams(0,-2,1f));split.addView(label("المنصة 50%",13f,Color.rgb(222,191,240),true));content.addView(split,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
 // Live incoming agency invitations fetched from the backend.
 section("دعوات الوكالة","راجع الدعوات المرسلة إليك من الوكالات")
 if(invitationLoadError.isNotBlank()){
   val errorCard=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(16),dp(16),dp(16));background=shape(intArrayOf(panel,panel),Color.rgb(76,57,92),16)}
   errorCard.addView(label("تعذر تحميل الدعوات",14f,Color.WHITE,true))
   errorCard.addView(label(invitationLoadError,12f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
   errorCard.addView(button("إعادة تحميل الدعوات"){load()},LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(10)})
   content.addView(errorCard,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
 }else if(incomingInvitations.length()==0){
   val agencyInvite=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(16),dp(22),dp(16),dp(22));background=shape(intArrayOf(panel,panel),Color.rgb(76,57,92),16)}
   agencyInvite.addView(label("🏢",26f,mint,true).apply{gravity=Gravity.CENTER})
   agencyInvite.addView(label("لا توجد دعوات وكالة حالياً",14f,Color.WHITE,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
   agencyInvite.addView(label("عند وصول دعوة، ستظهر تفاصيل الوكالة هنا ويمكنك قبولها أو رفضها.",12f,muted).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
   content.addView(agencyInvite,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
 }else{
   for(i in 0 until incomingInvitations.length()){
     val invite=incomingInvitations.optJSONObject(i)?:continue
     val inviteId=invite.optLong("id")
     val agencyName=invite.optString("agency_name").ifBlank{"وكالة BAK LIVE"}
     val created=invite.optString("created_at")
     val card=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(14),dp(16),dp(14));background=shape(intArrayOf(Color.rgb(46,32,59),Color.rgb(29,22,40)),Color.rgb(83,62,101),18)}
     card.addView(label("🏢  $agencyName",16f,Color.WHITE,true))
     card.addView(label("دعوة للانضمام إلى الوكالة",12f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
     if(created.isNotBlank())card.addView(label(created,10f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
     val actions=LinearLayout(this).apply{orientation=0}
     actions.addView(button("قبول الدعوة"){respondInvitation(inviteId,"accept")},LinearLayout.LayoutParams(0,dp(46),1f).apply{marginEnd=dp(6);topMargin=dp(12)})
     actions.addView(button("رفض"){respondInvitation(inviteId,"reject")},LinearLayout.LayoutParams(0,dp(46),1f).apply{marginStart=dp(6);topMargin=dp(12)})
     card.addView(actions)
     content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
   }
 }

 section("أرباح الهدايا","آخر العمليات التي حققت لك ألماساً وأرباحاً")
 var found=0;for(i in 0 until history.length()){val t=history.optJSONObject(i)?:continue;if(t.optString("transaction_type")!="gift_received")continue;found++;val card=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=shape(intArrayOf(Color.rgb(42,31,55),Color.rgb(29,22,39)),Color.rgb(76,57,92),15)};val info=LinearLayout(this).apply{orientation=1};info.addView(label("هدية مستلمة",14f,Color.WHITE,true));info.addView(label(t.optString("created_at"),10f,muted));card.addView(info,LinearLayout.LayoutParams(0,-2,1f));card.addView(label("+"+String.format(java.util.Locale.US,"%,d",t.optLong("amount"))+" 💎",15f,mint,true));content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})}
 if(found==0){val empty=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(18),dp(24),dp(18),dp(24));background=shape(intArrayOf(panel,panel),Color.rgb(76,57,92),16)};empty.addView(label("💎",30f,mint,true));empty.addView(label("لا توجد هدايا مكتسبة حتى الآن",13f,muted,true));content.addView(empty)}
 section("السحب والأمان","تُتاح طلبات السحب بعد تفعيل إعدادات الأرباح وخدمة الدفع على الخادم.")
 content.addView(button("تحديث البيانات"){load()},LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(10)})
 }
}