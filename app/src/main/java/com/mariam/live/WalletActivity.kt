package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import kotlin.concurrent.thread

class WalletActivity:Activity(){
 private val ink=Color.rgb(31,20,49)
 private val purple=Color.rgb(104,48,151)
 private val gold=Color.rgb(238,190,92)
 private val muted=Color.rgb(190,178,207)
 private var token=""
 private var packages=JSONArray()
 private var wallet=JSONObject()
 private var history=JSONArray()
 private lateinit var content:LinearLayout
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun money(cents:Long)=String.format(java.util.Locale.US,"%,.2f",cents/100.0)
 private fun bg(colors:IntArray,stroke:Int=Color.TRANSPARENT)=GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply{cornerRadius=dp(18).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
 private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)}
 private fun button(label:String,click:()->Unit)=TextView(this).apply{text=label;textSize=14f;gravity=android.view.Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(intArrayOf(purple,Color.rgb(67,34,105)),gold);setOnClickListener{click()}}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty()
  window.statusBarColor=ink;window.navigationBarColor=ink
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(17,12,27))}
  val header=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12))}
  header.addView(text("‹",34f,Color.WHITE,true).apply{setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(46)))
  header.addView(text("المحفظة",22f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(46),1f).apply{gravity=android.view.Gravity.CENTER})
  header.addView(text("؟",22f,gold,true).apply{setOnClickListener{AlertDialog.Builder(this@WalletActivity).setMessage("عملات الشحن تُستخدم داخل التطبيق. الألماس المكتسب من الهدايا منفصل عن رصيد الشحن، والأرباح القابلة للسحب تُعرض بالدولار. المبالغ المحجوزة لا يمكن سحبها مرة ثانية.").setPositiveButton("حسناً",null).show()}},LinearLayout.LayoutParams(dp(32),dp(46)))
  root.addView(header)
  val scroll=ScrollView(this).apply{isFillViewport=true;overScrollMode=ScrollView.OVER_SCROLL_NEVER}
  content=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(28))}
  scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root);loading();loadWallet()
 }
 private fun loading(){content.removeAllViews();content.addView(text("جارٍ تحميل المحفظة...",15f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(100)).apply{gravity=android.view.Gravity.CENTER})}
 private fun loadWallet(){
  if(token.isBlank()){showError("انتهت جلسة الدخول");return}
  thread{
   try{
    val w=AuthApi.get("/api/wallet",token)
    val p=AuthApi.get("/api/wallet/packages",token)
    val h=AuthApi.get("/api/wallet/transactions",token)
    runOnUiThread{
     if(w.status !in 200..299){showError(w.body.optString("error","تعذر تحميل المحفظة"));return@runOnUiThread}
     wallet=w.body.optJSONObject("wallet")?:w.body
     packages=p.body.optJSONArray("packages")?:JSONArray()
     history=h.body.optJSONArray("transactions")?:JSONArray()
     render()
    }
   }catch(e:Exception){runOnUiThread{showError(e.message?:"تعذر الاتصال بالخادم")}}
  }
 }
 private fun showError(message:String){content.removeAllViews();content.addView(text(message,14f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(90)));content.addView(button("إعادة المحاولة"){loadWallet()})}
 private fun section(title:String){content.addView(text(title,17f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(15)})}
 private fun metric(parent:LinearLayout,label:String,value:String,color:Int){parent.addView(text(label,12f,muted,true));parent.addView(text(value,23f,color,true),LinearLayout.LayoutParams(-1,dp(38)))}
 private fun render(){
  content.removeAllViews()
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(18),dp(20),dp(18));background=bg(intArrayOf(Color.rgb(77,27,111),Color.rgb(30,20,48)),Color.rgb(179,101,224))}
  metric(hero,"عملات الشحن",String.format("%,d",wallet.optLong("recharge_coins"))+" 🪙",Color.WHITE)
  metric(hero,"الألماس المكتسب",String.format("%,d",wallet.optLong("earned_diamonds"))+" 💎",gold)
  metric(hero,"الأرباح القابلة للسحب","$"+money(wallet.optLong("withdrawable_usd_cents")),Color.rgb(125,235,177))
  content.addView(hero)
  val info=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(intArrayOf(Color.rgb(35,27,46),Color.rgb(27,21,36)))}
  info.addView(text("المبلغ المحجوز للسحب: $"+money(wallet.optLong("reserved_usd_cents")),13f,muted))
  info.addView(text("إجمالي الأرباح مدى الحياة: $"+money(wallet.optLong("lifetime_earned_usd_cents")),13f,muted),LinearLayout.LayoutParams(-1,dp(28)))
  content.addView(info,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  val actions=LinearLayout(this).apply{orientation=0;gravity=android.view.Gravity.CENTER}
  actions.addView(button("طلب سحب الأرباح"){showWithdrawal()},LinearLayout.LayoutParams(0,dp(50),1f).apply{topMargin=dp(12);marginEnd=dp(5)})
  actions.addView(button("سجل العمليات"){showHistory()},LinearLayout.LayoutParams(0,dp(50),1f).apply{topMargin=dp(12);marginStart=dp(5)})
  content.addView(actions)
  section("باقات شحن العملات")
  if(packages.length()==0)content.addView(text("لا توجد باقات متاحة حالياً.",13f,muted))
  else for(i in 0 until packages.length()){
   val p=packages.optJSONObject(i)?:continue
   val card=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(intArrayOf(Color.rgb(44,29,61),Color.rgb(26,20,37)),Color.rgb(115,74,145))}
   val bonus=p.optLong("bonus_coins")
   card.addView(text(String.format("%,d",p.optLong("coins")+bonus)+" 🪙",20f,Color.WHITE,true))
   if(bonus>0)card.addView(text("مكافأة إضافية +"+String.format("%,d",bonus),12f,gold,true))
   card.addView(text("$"+money(p.optLong("price_usd_cents")),14f,muted))
   card.addView(text("الشحن غير متاح من التطبيق حالياً",11f,muted),LinearLayout.LayoutParams(-1,dp(26)))
   content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
  content.addView(text("لن تتم إضافة عملات إلى رصيدك إلا بعد تأكيد الدفع من الخادم.",11f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
 }
 private fun showWithdrawal(){
  val available=wallet.optLong("withdrawable_usd_cents")
  if(available<=0){AlertDialog.Builder(this).setMessage("لا يوجد رصيد قابل للسحب حالياً.").setPositiveButton("حسناً",null).show();return}
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),0)}
  box.addView(text("المتاح للسحب: $"+money(available),14f,ink,true))
  val amount=EditText(this).apply{hint="المبلغ بالدولار (مثال 10.50)";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL}
  val method=Spinner(this).apply{adapter=ArrayAdapter(this@WalletActivity,android.R.layout.simple_spinner_dropdown_item,listOf("زين كاش","آسيا حوالة","USDT"))}
  val destination=EditText(this).apply{hint="رقم المحفظة أو عنوان الاستلام"}
  box.addView(amount);box.addView(method);box.addView(destination)
  AlertDialog.Builder(this).setTitle("طلب سحب").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("إرسال الطلب"){_,_->
   val dollars=amount.text.toString().trim().replace(",",".").toBigDecimalOrNull()
   val cents=try{dollars?.movePointRight(2)?.longValueExact()}catch(_:Exception){null}
   if(cents==null||cents<=0||cents>available)AlertDialog.Builder(this).setMessage("أدخل مبلغاً صحيحاً لا يتجاوز الرصيد المتاح.").setPositiveButton("حسناً",null).show()
   else{
    val methods=listOf("zain_cash","asia_hawala","usdt")
    val payload=JSONObject().put("amount_usd_cents",cents).put("method",methods[method.selectedItemPosition]).put("destination",destination.text.toString().trim())
    submit("/api/wallet/withdrawals",payload,"تم إرسال طلب السحب للمراجعة")
   }
  }.show()
 }
 private fun showHistory(){
  val lines=ArrayList<String>()
  for(i in 0 until history.length()){
   val t=history.optJSONObject(i)?:continue
   val amount=t.optLong("amount")
   val currency=t.optString("currency")
   val value=if(currency.equals("usd_cents",true)||currency.equals("usd",true))"$"+money(amount) else String.format("%,d",amount)
   lines.add("$value • "+t.optString("transaction_type")+" • "+t.optString("status"))
  }
  if(lines.isEmpty())lines.add("لا توجد عمليات حتى الآن")
  AlertDialog.Builder(this).setTitle("سجل العمليات").setItems(lines.toTypedArray(),null).setPositiveButton("إغلاق",null).show()
 }
 private fun submit(path:String,payload:JSONObject,success:String){
  thread{
   try{
    val r=AuthApi.post(path,payload,token)
    runOnUiThread{
     val msg=if(r.status in 200..299)r.body.optString("message",success) else r.body.optString("error","تعذر تنفيذ الطلب")
     AlertDialog.Builder(this).setMessage(msg).setPositiveButton("حسناً"){_,_->loadWallet()}.show()
    }
   }catch(e:Exception){runOnUiThread{Toast.makeText(this,e.message?:"تعذر الاتصال",Toast.LENGTH_LONG).show()}}
  }
 }
}
