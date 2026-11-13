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
 private val ink=Color.rgb(31,20,49)
 private val purple=Color.rgb(104,48,151)
 private val gold=Color.rgb(238,190,92)
 private val muted=Color.rgb(190,178,207)
 private var token=""
 private var packages=JSONArray()
 private var selectedPackage=0
 private var selectedMethod="zain_cash"
 private lateinit var content:LinearLayout
 private lateinit var balanceView:TextView
 private lateinit var earningsView:TextView
 private lateinit var status:TextView
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun bg(colors:IntArray,stroke:Int=Color.TRANSPARENT)=GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply{cornerRadius=dp(18).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
 private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)}
 private fun button(label:String,click:()->Unit)=TextView(this).apply{
  text=label;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD
  setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(intArrayOf(purple,Color.rgb(67,34,105)),gold)
  setOnClickListener{click()}
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty()
  window.statusBarColor=ink;window.navigationBarColor=ink
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(17,12,27))}
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12))}
  val back=text("‹",34f,Color.WHITE,true).apply{setOnClickListener{finish()}}
  header.addView(back,LinearLayout.LayoutParams(dp(42),dp(46)))
  header.addView(text("المحفظة",22f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(46),1f).apply{gravity=Gravity.CENTER})
  header.addView(text("؟",22f,gold,true).apply{setOnClickListener{AlertDialog.Builder(this@WalletActivity).setMessage("رصيد الماسات مخصص للهدايا داخل التطبيق، وأرباح المستخدمين رصيد منفصل قابل لطلب السحب بعد المراجعة.").setPositiveButton("حسناً",null).show()}},LinearLayout.LayoutParams(dp(32),dp(46)))
  root.addView(header)
  val scroll=ScrollView(this).apply{isFillViewport=true;overScrollMode=View.OVER_SCROLL_NEVER}
  content=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(28))}
  scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root)
  buildLoading()
  loadWallet()
 }
 private fun buildLoading(){
  content.removeAllViews()
  content.addView(text("جارٍ تحميل المحفظة...",15f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(100)).apply{gravity=Gravity.CENTER})
 }
 private fun loadWallet(){
  if(token.isBlank()){showError("انتهت جلسة الدخول");return}
  thread{
   try{
    val w=AuthApi.get("/api/wallet",token)
    val h=AuthApi.get("/api/wallet/transactions",token)
    runOnUiThread{
     if(w.status !in 200..299){showError(w.body.optString("error","تعذر تحميل المحفظة"));return@runOnUiThread}
     val wallet=w.body.optJSONObject("wallet")?:JSONObject()
     packages=w.body.optJSONArray("packages")?:JSONArray()
     val history=h.body.optJSONArray("transactions")?:JSONArray()
     render(wallet,history,w.body.optInt("cashout_min_iqd",25000))
    }
   }catch(e:Exception){runOnUiThread{showError(e.message?:"تعذر الاتصال بالخادم")}}
  }
 }
 private fun showError(message:String){
  content.removeAllViews();content.addView(text(message,14f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(90)))
  content.addView(button("إعادة المحاولة"){loadWallet()})
 }
 private fun section(title:String){content.addView(text(title,17f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(15)})}
 private fun render(wallet:JSONObject,history:JSONArray,minCashout:Int){
  content.removeAllViews()
  val hero=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(18),dp(20),dp(18));background=bg(intArrayOf(Color.rgb(77,27,111),Color.rgb(30,20,48)),Color.rgb(179,101,224))}
  hero.addView(text("رصيد الماسات 💎",14f,Color.rgb(239,220,255),true))
  balanceView=text(String.format("%,d",wallet.optLong("diamonds_balance"))+" 💎",32f,Color.WHITE,true)
  hero.addView(balanceView,LinearLayout.LayoutParams(-1,dp(54)))
  hero.addView(text("رصيد الأرباح القابل للسحب",13f,muted,true))
  earningsView=text(String.format("%,d",wallet.optLong("earnings_iqd"))+" د.ع",22f,gold,true)
  hero.addView(earningsView,LinearLayout.LayoutParams(-1,dp(40)))
  content.addView(hero,LinearLayout.LayoutParams(-1,-2))
  val actions=LinearLayout(this).apply{gravity=Gravity.CENTER;orientation=0}
  actions.addView(button("＋ شحن الرصيد"){showTopup()},LinearLayout.LayoutParams(0,dp(52),1f).apply{marginEnd=dp(6);topMargin=dp(12)})
  actions.addView(button("سجل العمليات"){showHistory(history)},LinearLayout.LayoutParams(0,dp(52),1f).apply{marginStart=dp(6);topMargin=dp(12)})
  content.addView(actions)
  content.addView(button("طلب سحب الأرباح"){showCashout(minCashout,wallet.optLong("earnings_iqd"))},LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(8)})
  section("باقات شحن الماسات")
  val grid=LinearLayout(this).apply{orientation=1}
  var row:LinearLayout?=null
  for(i in 0 until packages.length()){
   if(i%2==0){row=LinearLayout(this).apply{orientation=0};grid.addView(row,LinearLayout.LayoutParams(-1,-2))}
   val p=packages.optJSONObject(i)?:continue
   val card=LinearLayout(this).apply{
    orientation=1;gravity=Gravity.CENTER;setPadding(dp(8),dp(12),dp(8),dp(12))
    background=bg(intArrayOf(Color.rgb(44,29,61),Color.rgb(26,20,37)),Color.rgb(115,74,145))
    addView(text(String.format("%,d",p.optLong("diamonds"))+" 💎",20f,Color.WHITE,true))
    if(p.optInt("bonus")>0)addView(text("هدية إضافية +"+p.optInt("bonus"),11f,gold,true))
    addView(text(String.format("%,d",p.optLong("amount_iqd"))+" د.ع",13f,muted))
    addView(button("اختيار الباقة"){selectedPackage=i;showTopup() },LinearLayout.LayoutParams(-1,dp(38)).apply{topMargin=dp(8)})
   }
   row?.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5);marginStart=dp(5);topMargin=dp(5);bottomMargin=dp(5)})
  }
  content.addView(grid)
  section("طرق الدفع")
  val methods= listOf("زين كاش — تحويل يدوي","آسيا حوالة — تحويل يدوي","USDT — تحويل يدوي","بوابة دفع تلقائية — تتطلب ربط حساب التاجر")
  methods.forEach{m->content.addView(text("•  $m",14f,Color.rgb(224,211,239)),LinearLayout.LayoutParams(-1,dp(30)))}
  content.addView(text("طلبات الشحن اليدوية تبقى معلّقة حتى تتحقق الإدارة من التحويل. الدفع التلقائي لا يُفعّل قبل ربط بوابة دفع معتمدة.",11f,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
 }
 private fun showTopup(){
  if(packages.length()==0)return
  val p=packages.optJSONObject(selectedPackage.coerceIn(0,packages.length()-1))?:return
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(10),dp(18),dp(4))}
  box.addView(text("الباقة: "+String.format("%,d",p.optLong("diamonds"))+" ماسة",16f,ink,true))
  box.addView(text("المبلغ: "+String.format("%,d",p.optLong("amount_iqd"))+" دينار عراقي",14f,ink))
  val method=Spinner(this)
  val labels=listOf("زين كاش","آسيا حوالة","USDT","بوابة دفع تلقائية")
  method.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,labels)
  method.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
   override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
   override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long){selectedMethod=listOf("zain_cash","asia_hawala","usdt","gateway")[position]}
  }
  box.addView(text("اختر طريقة الدفع",13f,ink,true));box.addView(method)
  val ref=EditText(this).apply{hint="رقم الحوالة / مرجع التحويل";textSize=14f}
  box.addView(ref)
  AlertDialog.Builder(this).setTitle("طلب شحن").setView(box)
   .setNegativeButton("إلغاء",null).setPositiveButton("إرسال الطلب"){_,_->
    val payload=JSONObject().put("package_id",p.optString("id")).put("payment_method",selectedMethod).put("payment_reference",ref.text.toString().trim())
    submit("/api/wallet/topups",payload,"تم إرسال طلب الشحن للمراجعة")
   }.show()
 }
 private fun showCashout(min:Int,balance:Long){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),0)}
  val amount=EditText(this).apply{hint="المبلغ بالدينار العراقي";inputType=android.text.InputType.TYPE_CLASS_NUMBER}
  val account=EditText(this).apply{hint="رقم المحفظة أو الحساب"}
  val method=Spinner(this).apply{adapter=ArrayAdapter(this@WalletActivity,android.R.layout.simple_spinner_dropdown_item,listOf("زين كاش","آسيا حوالة","حساب بنكي"))}
  box.addView(text("المتاح: $balance د.ع — الحد الأدنى: $min د.ع",13f,ink))
  box.addView(amount);box.addView(method);box.addView(account)
  AlertDialog.Builder(this).setTitle("سحب الأرباح").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("إرسال الطلب"){_,_->
   val methods=listOf("zain_cash","asia_hawala","bank")
   val payload=JSONObject().put("amount_iqd",amount.text.toString().toLongOrNull()?:0).put("payout_method",methods[method.selectedItemPosition]).put("payout_account",account.text.toString())
   submit("/api/wallet/cashouts",payload,"تم إرسال طلب السحب للمراجعة")
  }.show()
 }
 private fun showHistory(history:JSONArray){
  val lines=ArrayList<String>()
  for(i in 0 until history.length()){
   val t=history.optJSONObject(i)?:continue
   val sign=if(t.optString("transaction_type")=="topup"||t.optString("transaction_type")=="gift_received")"+" else "−"
   val unit=if(t.optString("currency")=="iqd")"د.ع" else "💎"
   lines.add("$sign${String.format("%,d",t.optLong("amount"))} $unit  •  ${t.optString("description")}  •  ${t.optString("status")}")
  }
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
