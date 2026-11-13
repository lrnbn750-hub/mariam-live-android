package com.mariam.live

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import kotlin.concurrent.thread

class WalletActivity:Activity(){
 private val ink=Color.rgb(18,13,28)
 private val panel=Color.rgb(35,25,49)
 private val purple=Color.rgb(111,52,157)
 private val gold=Color.rgb(239,194,91)
 private val muted=Color.rgb(174,163,190)
 private val mint=Color.rgb(104,232,177)
 private var token=""
 private var packages=JSONArray()
 private var wallet=JSONObject()
 private var history=JSONArray()
 private lateinit var content:LinearLayout
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun money(cents:Long)=String.format(java.util.Locale.US,"%,.2f",cents/100.0)
 private fun shape(colors:IntArray,stroke:Int=Color.TRANSPARENT,radius:Int=20)=GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply{cornerRadius=dp(radius).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
 private fun label(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL);includeFontPadding=true}
 private fun actionButton(value:String,click:()->Unit,primary:Boolean=true)=TextView(this).apply{
  text=value;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD
  setPadding(dp(12),dp(14),dp(12),dp(14))
  background=if(primary)shape(intArrayOf(Color.rgb(125,57,174),Color.rgb(76,35,112)),gold,16) else shape(intArrayOf(Color.rgb(48,36,61),Color.rgb(38,29,50)),Color.rgb(91,70,111),16)
  setOnClickListener{click()}
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty()
  window.statusBarColor=ink;window.navigationBarColor=ink
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(ink)}
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(10),dp(18),dp(10))}
  header.addView(label("‹",34f,Color.WHITE,true).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
  header.addView(label("المحفظة",20f,Color.WHITE,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
  header.addView(label("؟",21f,gold,true).apply{gravity=Gravity.CENTER;setOnClickListener{showHelp()}},LinearLayout.LayoutParams(dp(38),dp(48)))
  root.addView(header)
  val scroll=ScrollView(this).apply{isFillViewport=true;overScrollMode=ScrollView.OVER_SCROLL_NEVER}
  content=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(8),dp(18),dp(30))}
  scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root);loading();loadWallet()
 }
 private fun showHelp(){
  AlertDialog.Builder(this).setTitle("عن المحفظة").setMessage("عملات الشحن تُستخدم لإرسال الهدايا داخل التطبيق. الألماس هو ما تكسبه من الهدايا. الأرباح القابلة للسحب رصيد منفصل بالدولار. المبالغ المحجوزة مرتبطة بطلبات السحب قيد المراجعة.").setPositiveButton("فهمت",null).show()
 }
 private fun loading(){content.removeAllViews();content.addView(label("جارٍ تحميل محفظتك...",14f,muted),LinearLayout.LayoutParams(-1,dp(100)).apply{gravity=Gravity.CENTER})}
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
 private fun showError(message:String){
  content.removeAllViews()
  content.addView(label(message,14f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(80)))
  content.addView(actionButton("إعادة المحاولة",{loadWallet()}))
 }
 private fun section(title:String,subtitle:String=""){
  val row=LinearLayout(this).apply{orientation=1}
  row.addView(label(title,18f,Color.WHITE,true))
  if(subtitle.isNotBlank())row.addView(label(subtitle,11f,muted),LinearLayout.LayoutParams(-1,dp(24)))
  content.addView(row,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(22);bottomMargin=dp(10)})
 }
 private fun smallBalance(title:String,value:String,icon:String,accent:Int):LinearLayout{
  val card=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(12),dp(15),dp(12),dp(15));background=shape(intArrayOf(Color.rgb(47,34,62),Color.rgb(31,24,42)),Color.rgb(83,62,101),17)}
  card.addView(label(icon,23f,accent,true).apply{gravity=Gravity.CENTER})
  card.addView(label(title,11f,muted,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(25)))
  card.addView(label(value,19f,Color.WHITE,true).apply{gravity=Gravity.CENTER})
  return card
 }
 private fun render(){
  content.removeAllViews()
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(20),dp(20),dp(20));background=shape(intArrayOf(Color.rgb(93,38,130),Color.rgb(43,25,62)),Color.rgb(157,89,194),24)}
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(label("رصيد الأرباح",13f,Color.rgb(232,213,245),true),LinearLayout.LayoutParams(0,dp(28),1f))
  top.addView(label("BAK LIVE  •  WALLET",9f,Color.rgb(218,190,233),true))
  hero.addView(top)
  hero.addView(label("$"+money(wallet.optLong("withdrawable_usd_cents")),36f,mint,true),LinearLayout.LayoutParams(-1,dp(58)))
  hero.addView(label("الرصيد المتاح للسحب",12f,Color.rgb(228,211,239),true))
  val divider=View(this).apply{setBackgroundColor(Color.rgb(112,73,137))}
  hero.addView(divider,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(16);bottomMargin=dp(13)})
  val sub=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  val reserved=LinearLayout(this).apply{orientation=1}
  reserved.addView(label("المبلغ المحجوز",11f,Color.rgb(211,190,225)))
  reserved.addView(label("$"+money(wallet.optLong("reserved_usd_cents")),16f,Color.WHITE,true))
  val lifetime=LinearLayout(this).apply{orientation=1;gravity=Gravity.END}
  lifetime.addView(label("إجمالي الأرباح",11f,Color.rgb(211,190,225)))
  lifetime.addView(label("$"+money(wallet.optLong("lifetime_earned_usd_cents")),16f,Color.WHITE,true))
  sub.addView(reserved,LinearLayout.LayoutParams(0,-2,1f));sub.addView(lifetime,LinearLayout.LayoutParams(0,-2,1f))
  hero.addView(sub)
  content.addView(hero,LinearLayout.LayoutParams(-1,-2))
  val balances=LinearLayout(this).apply{orientation=0}
  balances.addView(smallBalance("عملات الشحن",String.format("%,d",wallet.optLong("recharge_coins")),"🪙",gold),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(6)})
  balances.addView(smallBalance("الألماس المكتسب",String.format("%,d",wallet.optLong("earned_diamonds")),"💎",Color.rgb(116,203,255)),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(6)})
  content.addView(balances,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
  val buttons=LinearLayout(this).apply{orientation=0}
  buttons.addView(actionButton("سحب الأرباح",{showWithdrawal()}),LinearLayout.LayoutParams(0,dp(52),1f).apply{marginEnd=dp(6);topMargin=dp(14)})
  buttons.addView(actionButton("سجل العمليات",{showHistory()},false),LinearLayout.LayoutParams(0,dp(52),1f).apply{marginStart=dp(6);topMargin=dp(14)})
  content.addView(buttons)
  section("باقات شحن العملات","اختر الباقة المناسبة لك")
  if(packages.length()==0){
   val empty=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setPadding(dp(16),dp(24),dp(16),dp(24));background=shape(intArrayOf(panel,panel),Color.rgb(77,58,94),18)}
   empty.addView(label("🪙",28f,gold,true).apply{gravity=Gravity.CENTER})
   empty.addView(label("لا توجد باقات متاحة حالياً",13f,muted).apply{gravity=Gravity.CENTER})
   content.addView(empty)
  }else for(i in 0 until packages.length()){
   val p=packages.optJSONObject(i)?:continue
   val card=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));background=shape(intArrayOf(Color.rgb(43,31,56),Color.rgb(30,23,41)),Color.rgb(82,61,99),18)}
   val info=LinearLayout(this).apply{orientation=1}
   val total=p.optLong("coins")+p.optLong("bonus_coins")
   info.addView(label(String.format("%,d",total)+" 🪙",20f,Color.WHITE,true))
   if(p.optLong("bonus_coins")>0)info.addView(label("مكافأة +"+String.format("%,d",p.optLong("bonus_coins")),11f,gold,true))
   info.addView(label("$"+money(p.optLong("price_usd_cents")),13f,muted),LinearLayout.LayoutParams(-1,dp(24)))
   card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
   val soon=TextView(this).apply{text="قريباً";textSize=11f;setTextColor(gold);gravity=Gravity.CENTER;padding=dp(10);background=shape(intArrayOf(Color.rgb(74,51,89),Color.rgb(74,51,89)),Color.rgb(135,97,154),30)}
   card.addView(soon)
   content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
  content.addView(label("تُضاف العملات بعد تأكيد الدفع من الخادم فقط.",11f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
 }
 private fun showWithdrawal(){
  val available=wallet.optLong("withdrawable_usd_cents")
  if(available<=0){AlertDialog.Builder(this).setMessage("لا يوجد رصيد قابل للسحب حالياً.").setPositiveButton("حسناً",null).show();return}
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),0)}
  box.addView(label("المتاح للسحب: $"+money(available),14f,ink,true))
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
   val amount=t.optLong("amount");val currency=t.optString("currency")
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
