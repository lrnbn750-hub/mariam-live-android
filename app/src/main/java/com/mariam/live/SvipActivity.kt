package com.mariam.live

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.util.Log
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Fully native SVIP screen. The dimensions, palette, section order and interactions
 * mirror app/src/main/assets/vip/index.html; the HTML/WebView is not used at runtime.
 */
class SvipActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val ink = Color.rgb(22,27,39)
    private val deep = Color.rgb(27,33,48)
    private val panel = Color.rgb(35,41,56)
    private val line = Color.rgb(58,65,84)
    private val cream = Color.rgb(239,223,192)
    private val muted = Color.rgb(127,136,155)
    private val ok = Color.rgb(95,214,164)
    private val tiers = arrayOf(
        intArrayOf(0xffc98a54.toInt(),0xffff9a4a.toInt(),0xff7a3b1a.toInt()),
        intArrayOf(0xffc3ccd8.toInt(),0xff5ec8ff.toInt(),0xff2c4a6b.toInt()),
        intArrayOf(0xffe9b94a.toInt(),0xff3ddc97.toInt(),0xff1f6b4a.toInt()),
        intArrayOf(0xffefc15a.toInt(),0xff4aa3ff.toInt(),0xff24407a.toInt()),
        intArrayOf(0xfff2c660.toInt(),0xffb36bff.toInt(),0xff5a2a8a.toInt()),
        intArrayOf(0xfff4cb68.toInt(),0xffff5fa8.toInt(),0xff8a1f55.toInt()),
        intArrayOf(0xfff6d070.toInt(),0xffff3b4a.toInt(),0xff9c1424.toInt())
    )
    private data class Pack(val level:Int,val price:Long,val active:Boolean)
    private data class Perk(val icon:String,val min:Int,val label:(Int)->String)
    private val perks = listOf(
        Perk("entry",1){"مؤثرات الدخول"}, Perk("frame",2){"براويز مميزة"},
        Perk("vip",1){"شارة SVIP$it"}, Perk("follow",1){"المتابعة *${it*100}%"},
        Perk("friends",2){"مزيد من الاصدقاء"}, Perk("car",3){"احصل/ي على هذه السيارة"},
        Perk("support",2){"دعم متخصص"}, Perk("bubble",3){"فقاعات دردشة"},
        Perk("card",3){"بطاقة تعريف مميزة"}, Perk("palette",2){"ألوان دردشة مميزة"},
        Perk("highlight",4){"رسائل بارزة في الدردشة"}, Perk("shield",3){"منع الطرد"},
        Perk("mute",3){"منع الكتم من التعليقات"}, Perk("star",6){"أولوية الظهور"},
        Perk("door",6){"غرف SVIP حصرية"}
    )
    private var token=""
    private var balance=0L
    private var owned=0
    private var expiresAt=""
    private var selected=1
    private var loading=false
    private var packs= mutableListOf<Pack>()
    private lateinit var tabRow:LinearLayout
    private lateinit var balanceText:TextView
    private lateinit var memberEntry:TextView
    private lateinit var badge:BadgeView
    private lateinit var elite:LinearLayout
    private lateinit var eliteIcon:IconView
    private lateinit var grid:LinearLayout
    private lateinit var priceDetail:TextView
    private lateinit var bottomPrice:TextView
    private lateinit var bottomState:TextView
    private lateinit var bottomBack:TextView
    private lateinit var buy:TextView
    private lateinit var scrollTabs:HorizontalScrollView
    private var toastView:TextView?=null

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token","").orEmpty()
        if(token.isBlank()){Toast.makeText(this,"يرجى تسجيل الدخول أولاً",Toast.LENGTH_LONG).show();finish();return}
        window.statusBarColor=ink; window.navigationBarColor=ink
        buildNativePage()
        applyCached()
        loadData()
    }
    private fun dp(v:Float)= (v*resources.displayMetrics.density).roundToInt()
    private fun dp(v:Int)=dp(v.toFloat())
    private fun fmt(v:Long)=java.text.NumberFormat.getNumberInstance(Locale.US).format(v)
    private fun lp(w:Int,h:Int,weight:Float=0f)=LinearLayout.LayoutParams(w,h,weight)
    private fun bg(color:Int,r:Float,stroke:Int=Color.TRANSPARENT):GradientDrawable =
        GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat();if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
    private fun label(s:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
        text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
        includeFontPadding=true; if(bold)setTypeface(typeface,Typeface.BOLD)
    }
    private fun row():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
    private fun column():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}

    private fun buildNativePage(){
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(ink)
            // Keep all controls below the status bar and above the gesture/navigation area.
            setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(v.paddingLeft, insets.systemWindowInsetTop, v.paddingRight, insets.systemWindowInsetBottom)
                insets
            }
        }
        // Header inspired by the supplied reference: title on the right, no wallet balance.
        val head=row().apply{
            setPadding(dp(18),0,dp(14),0)
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            clipChildren=false
            clipToPadding=false
        }
        val headerArrow=TextView(this).apply{
            text="›";textSize=34f;setTextColor(0xffc8cbd4.toInt())
            gravity=Gravity.CENTER
            setOnClickListener{finish()}
        }
        head.addView(headerArrow,lp(dp(34),dp(48)))
        head.addView(label("مركز SVIP",17f,0xffd8dbe3.toInt(),true),lp(-2,dp(48)).apply{marginEnd=dp(5)})
        root.addView(head,lp(-1,dp(54)))

        scrollTabs=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=View.OVER_SCROLL_NEVER}
        tabRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(6),dp(2),dp(6),dp(2))}
        scrollTabs.addView(tabRow);root.addView(scrollTabs,lp(-1,dp(37)))

        val sc=ScrollView(this).apply{isFillViewport=false;overScrollMode=View.OVER_SCROLL_NEVER;clipToPadding=false;setPadding(0,0,0,dp(18))}
        val content=column().apply{setBackgroundColor(ink)}
        badge=BadgeView(this).apply{
            setOnTouchListener(object:View.OnTouchListener{
                var down=0f
                override fun onTouch(v:View,e:MotionEvent):Boolean{
                    when(e.action){MotionEvent.ACTION_DOWN->{down=e.x;return true}
                        MotionEvent.ACTION_UP->{val dx=e.x-down;if(abs(dx)>dp(45))select(selected+if(dx>0)1 else -1);return true}}
                    return true
                }
            })
        }
        content.addView(badge,lp(-1,dp(152)))
        val panelWrap=column().apply{
            background=GradientDrawable().apply{setColor(panel);cornerRadii=floatArrayOf(dp(34f).toFloat(),dp(34f).toFloat(),dp(34f).toFloat(),dp(34f).toFloat(),0f,0f,0f,0f)}
            setPadding(dp(12),dp(16),dp(12),dp(18))
            translationY=-dp(16).toFloat()
        }
        val title=TextView(this).apply{text="الامتيازات";textSize=14f;setTextColor(0xfff3e7cf.toInt());gravity=Gravity.CENTER}
        panelWrap.addView(title,lp(-1,dp(30)))
        elite=column().apply{
            gravity=Gravity.CENTER;setPadding(dp(10),dp(32),dp(10),dp(12))
            background=bg(0xff212736.toInt(),12f,0xff565e72.toInt())
            setOnClickListener{showEliteInfo()}
        }
        val ribbon=TextView(this).apply{text="امتيازات النخبة";textSize=13f;setTextColor(0xff3a2d12.toInt());setTypeface(typeface,Typeface.BOLD);gravity=Gravity.CENTER
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xfff4e5c6.toInt(),0xffd8be8d.toInt())).apply{cornerRadius=dp(4f).toFloat()}
            setPadding(dp(29),dp(4),dp(29),dp(8))}
        elite.addView(ribbon,lp(-2,dp(28)))
        val q=TextView(this).apply{text="?";textSize=16f;setTextColor(cream);gravity=Gravity.CENTER;background=bg(0xff212736.toInt(),30f,cream);setOnClickListener{showEliteInfo()}}
        elite.addView(q,lp(dp(26),dp(26)).apply{gravity=Gravity.END;topMargin=dp(-26)})
        eliteIcon=IconView(this,"mask")
        elite.addView(eliteIcon,lp(dp(53),dp(53)).apply{topMargin=dp(6)})
        elite.addView(label("دخول رجل غامض الغرفة للتو.",11f,0xff6f778a.toInt()).apply{gravity=Gravity.CENTER},lp(-1,dp(28)))
        panelWrap.addView(elite,lp(-1,-2).apply{topMargin=dp(8)})
        grid=column().apply{setPadding(0,dp(22),0,0)}
        panelWrap.addView(grid,lp(-1,-2))
        val details=column().apply{background=bg(0xff202635.toInt(),12f,line);clipToOutline=true}
        details.addView(label("تفاصيل العضوية",13.5f,Color.WHITE,true),lp(-1,dp(42)).apply{leftMargin=dp(14);rightMargin=dp(14)})
        val pr=row().apply{setPadding(dp(14),dp(10),dp(14),dp(10))}
        pr.addView(label("سعر الباقة",12f,0xffaab2c3.toInt()),lp(0,dp(24),1f));priceDetail=label("—",13f,Color.WHITE,true);pr.addView(priceDetail);details.addView(pr)
        val dur=row().apply{setPadding(dp(14),dp(10),dp(14),dp(10))}
        dur.addView(label("مدة العضوية",12f,0xffaab2c3.toInt()),lp(0,dp(24),1f));dur.addView(label("30 يوماً",13f,Color.WHITE,true));details.addView(dur)
        details.addView(label("يتم التحقق من السعر والرصيد وتنفيذ الاشتراك من خادم BAK LIVE. مدة العضوية 30 يوماً.",11.5f,muted).apply{setPadding(dp(16),dp(4),dp(16),dp(16));setLineSpacing(dp(2).toFloat(),1.15f)},lp(-1,-2))
        panelWrap.addView(details,lp(-1,-2).apply{topMargin=dp(18)})
        content.addView(panelWrap,lp(-1,-2))
        sc.addView(content);root.addView(sc,lp(-1,0,1f))

        val footer=row().apply{background=bg(deep,0f,0xff2c3345.toInt());setPadding(dp(14),dp(10),dp(14),dp(10));minimumHeight=dp(82);clipChildren=false;clipToPadding=false}
        val priceCol=column()
        val priceLine=row()
        bottomPrice=label("—",17f,Color.WHITE,true);priceLine.addView(bottomPrice)
        priceLine.addView(DiamondView(this),lp(dp(16),dp(16)).apply{marginStart=dp(5)})
        priceLine.addView(label("/ 30 يوم",13f,0xff8a93a6.toInt()).apply{layoutDirection=View.LAYOUT_DIRECTION_LTR},lp(-2,-2).apply{marginStart=dp(5)})
        priceCol.addView(priceLine)
        bottomState=label("جارٍ تحميل الباقات…",12f,0xffaab2c3.toInt());priceCol.addView(bottomState,lp(-1,-2).apply{topMargin=dp(3)})
        bottomBack=label("",11f,0xffaab2c3.toInt());priceCol.addView(bottomBack)
        footer.addView(priceCol,lp(0,-2,1f))
        buy=label("شراء",14f,0xff2d2411.toInt(),true).apply{gravity=Gravity.CENTER;background=bg(cream,22f);setPadding(dp(17),0,dp(17),0);minWidth=dp(78);minHeight=dp(44);setOnClickListener{confirmPurchase()}}
        footer.addView(buy,lp(-2,dp(44)).apply{marginStart=dp(10)})
        root.addView(footer,lp(-1,-2))
        setContentView(root)
        root.requestApplyInsets()
    }

    private fun renderTabs(){
        tabRow.removeAllViews()
        packs.sortedBy{it.level}.forEach{p->
            val t=TextView(this).apply{
                text="SVIP ${p.level}";textSize=13f;gravity=Gravity.CENTER;setTextColor(if(p.level==selected)Color.WHITE else 0xff6b7385.toInt())
                setTypeface(typeface,if(p.level==selected)Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(8),dp(5),dp(8),dp(8))
                if(p.level==selected)background=GradientDrawable().apply{setColor(Color.TRANSPARENT);setStroke(0,Color.TRANSPARENT)}
                setOnClickListener{select(p.level)}
            }
            tabRow.addView(t,lp(-2,dp(33)))
        }
    }
    private fun select(level:Int){
        if(packs.none{it.level==level})return
        selected=level;renderTabs();render()
    }
    private fun render(){
        val p=packs.firstOrNull{it.level==selected}?:return
        badge.level=selected;badge.invalidate()
        balanceText.text=fmt(balance)
        memberEntry.visibility=if(owned>0)View.VISIBLE else View.GONE
        elite.alpha=if(selected>=4)1f else .62f
        eliteIcon.active=selected>=4;eliteIcon.invalidate()
        grid.removeAllViews()
        perks.chunked(3).forEach{chunk->
            val r=row().apply{gravity=Gravity.CENTER}
            chunk.forEach{perk->
                val active=selected>=perk.min
                val cell=column().apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;alpha=if(active)1f else .48f}
                val circle=FrameLayout(this@SvipActivity).apply{
                    background=bg(if(active)0xff262c3b.toInt() else 0xff262c3b.toInt(),40f,
                        if(active)mix(tiers[selected-1][1],Color.WHITE,.10f) else 0xff565e72.toInt())
                }
                val icon=IconView(this@SvipActivity,perk.icon).apply{this.active=active}
                circle.addView(icon,FrameLayout.LayoutParams(dp(29),dp(29),Gravity.CENTER))
                if(!active){
                    val lock=TextView(this@SvipActivity).apply{text="⌑";textSize=12f;gravity=Gravity.CENTER;setTextColor(0xff7a8296.toInt());background=bg(panel,20f,0xff565e72.toInt())}
                    circle.addView(lock,FrameLayout.LayoutParams(dp(20),dp(20),Gravity.BOTTOM or Gravity.LEFT))
                }
                cell.addView(circle,lp(dp(58),dp(58)))
                cell.addView(label(perk.label(selected),10.5f,if(active)0xffb4bccb.toInt() else 0xff59617a.toInt()).apply{
                    gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;textAlignment=View.TEXT_ALIGNMENT_CENTER;maxLines=2
                },lp(-1,dp(34)).apply{topMargin=dp(6)})
                if(!active)cell.addView(label("من SVIP${perk.min}",10.5f,0xff4d556b.toInt()).apply{gravity=Gravity.CENTER},lp(-1,dp(18)))
                r.addView(cell,lp(0,-2,1f).apply{marginStart=dp(2);marginEnd=dp(2)})
            }
            repeat(3-chunk.size){r.addView(View(this),lp(0,1,1f))}
            grid.addView(r,lp(-1,-2).apply{bottomMargin=dp(12)})
        }
        val cashback=p.price*40/100
        priceDetail.text=fmt(p.price)+"  ◇"
        bottomPrice.text=fmt(p.price)
        bottomBack.text="يرجعلك ${fmt(cashback)} عملة (40%)"
        val date=if(owned==selected&&expiresAt.isNotBlank())try{
            val d=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US).parse(expiresAt.take(19))
            d?.let{" · تنتهي "+SimpleDateFormat("yyyy/MM/dd",Locale("ar")).format(it)}.orEmpty()
        }catch(_:Exception){""}else""
        bottomState.text=if(owned==selected)"عضويتك فعّالة$date" else "أنت لست SVIP$selected"
        bottomState.setTextColor(if(owned==selected)ok else 0xff6c7488.toInt())
        buy.text=if(owned==selected)"تجديد" else "شراء"
        buy.isEnabled=p.active&&p.price>0
        buy.alpha=if(buy.isEnabled)1f else .45f
    }
    private fun mix(a:Int,b:Int,t:Float)=Color.rgb((Color.red(a)*(1-t)+Color.red(b)*t).toInt(),(Color.green(a)*(1-t)+Color.green(b)*t).toInt(),(Color.blue(a)*(1-t)+Color.blue(b)*t).toInt())

    private fun confirmPurchase(){
        val p=packs.firstOrNull{it.level==selected}?:run{showToast("لم يتم تحميل باقات SVIP بعد");return}
        val back=p.price*40/100;val enough=balance>=p.price&&p.active
        val d=Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box=column().apply{background=GradientDrawable().apply{setColor(0xff202635.toInt());cornerRadii=floatArrayOf(dp(22f).toFloat(),dp(22f).toFloat(),dp(22f).toFloat(),dp(22f).toFloat(),0f,0f,0f,0f)};setPadding(dp(18),dp(10),dp(18),dp(22))}
        val grab=TextView(this).apply{text="";background=bg(0xff464e63.toInt(),5f);layoutParams=lp(dp(38),dp(4)).apply{gravity=Gravity.CENTER;bottomMargin=dp(14)}}
        box.addView(grab)
        box.addView(label("${if(owned==p.level)"تجديد" else "شراء"} SVIP ${p.level}",18f,Color.WHITE,true),lp(-1,dp(34)))
        box.addView(label("رصيدك الحالي ${fmt(balance)} ماسة. كل ترقية تُحاسب بالسعر الكامل للمستوى الجديد وتبدأ مدة 30 يوماً جديدة.",12.5f,muted).apply{setLineSpacing(dp(2).toFloat(),1.15f)},lp(-1,-2).apply{bottomMargin=dp(14)})
        val breakdown=column().apply{background=bg(0xff202635.toInt(),12f,line)}
        fun detailRow(title:String,value:String,color:Int=0xffe9ecf3.toInt()){
            val rr=row().apply{setPadding(dp(14),dp(10),dp(14),dp(10))}
            rr.addView(label(title,12f,0xffaab2c3.toInt()),lp(0,dp(22),1f));rr.addView(label(value,13f,color,true));breakdown.addView(rr)
        }
        detailRow("سعر الباقة",fmt(p.price)+" ◇")
        detailRow("مردود المحفظة (40%)","+ "+fmt(back)+" ◇",ok)
        detailRow("الصافي بعد المردود",fmt(p.price-back)+" ◇")
        detailRow("رصيدك بعد الشراء",if(enough)fmt(balance-p.price+back)+" ◇" else "—")
        detailRow("مدة العضوية","30 يوماً")
        box.addView(breakdown,lp(-1,-2))
        if(!enough)box.addView(label("رصيدك لا يكفي. تحتاج إلى ${fmt((p.price-balance).coerceAtLeast(0))} ماسة إضافية.",12.5f,0xffff7a86.toInt()),lp(-1,-2).apply{topMargin=dp(10)})
        val actions=row().apply{layoutDirection=View.LAYOUT_DIRECTION_LTR}
        val cancel=label("إلغاء",13f,0xffc4cbda.toInt(),true).apply{gravity=Gravity.CENTER;background=bg(Color.TRANSPARENT,19f,0xff4a5268.toInt());setOnClickListener{d.dismiss()}}
        val confirm=label("تأكيد الشراء",13f,0xff2d2411.toInt(),true).apply{gravity=Gravity.CENTER;background=bg(cream,19f);alpha=if(enough)1f else .45f;setOnClickListener{if(enough){d.dismiss();purchase(p.level)}}}
        actions.addView(cancel,lp(0,dp(38),1f));actions.addView(confirm,lp(0,dp(38),1f).apply{marginStart=dp(10)})
        box.addView(actions,lp(-1,dp(38)).apply{topMargin=dp(16)})
        d.setContentView(box)
        d.window?.apply{setBackgroundDrawableResource(android.R.color.transparent);setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);setGravity(Gravity.BOTTOM);attributes=attributes.apply{width=ViewGroup.LayoutParams.MATCH_PARENT}}
        d.show();d.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private fun showEliteInfo(){
        android.app.AlertDialog.Builder(this).setTitle("امتيازات النخبة")
            .setMessage("تبدأ من VIP4. عند دخولك الغرفة يظهر اسمك كرجل غامض بدل ظهور الإشعار المعتاد، فتدخل دون لفت الانتباه.")
            .setPositiveButton("حسناً",null).show()
    }
    private fun purchase(level:Int){
        buy.isEnabled=false
        io.execute{try{
            val r=AuthApi.post("/api/svip/purchase",JSONObject().put("level",level),token)
            if(r.status !in 200..299||!r.body.optBoolean("success",false)){
                val msg=r.body.optString("error",r.body.optString("message","تعذر إتمام شراء SVIP"))
                runOnUiThread{showToast(msg);render()};return@execute
            }
            loadData(r.body.optString("message","تم تفعيل عضوية SVIP $level بنجاح"))
        }catch(e:Exception){runOnUiThread{showToast(e.message?:"تعذر الاتصال بالخادم");render()}}
    }}
    private fun applyCached(){
        try{
            val cached=getSharedPreferences("SVIP_FAST_CACHE",MODE_PRIVATE).getString("page_data",null)?:return
            applyData(JSONObject(cached))
        }catch(e:Exception){
            Log.e("SvipActivity","Cached SVIP render failed",e)
            bottomState.text="جار تحديث بيانات SVIP..."
        }
    }
    private fun responseData(body:JSONObject):JSONObject =
        body.optJSONObject("data") ?: body.optJSONObject("result") ?: body

    private fun hasApiError(result:AuthApi.Result):Boolean =
        result.status !in 200..299 || !result.body.optBoolean("success",true)

    private fun loadData(successMessage:String?=null){
        if(loading&&successMessage==null)return
        loading=true
        bottomState.text="جار تحميل الباقات..."
        io.execute{
            try{
                // Keep the exact endpoints and authorization used by the original SVIP page.
                val levelsFuture=CompletableFuture.supplyAsync{AuthApi.get("/api/svip/levels",token)}
                val meFuture=CompletableFuture.supplyAsync{AuthApi.get("/api/svip/me",token)}
                val walletFuture=CompletableFuture.supplyAsync{AuthApi.get("/api/wallet",token)}
                val lr=levelsFuture.get(20,TimeUnit.SECONDS)
                val mr=meFuture.get(20,TimeUnit.SECONDS)
                val wr=walletFuture.get(20,TimeUnit.SECONDS)

                if(hasApiError(lr)) throw IllegalStateException(lr.body.optString("error",lr.body.optString("message","تعذر تحميل مستويات SVIP (${lr.status})")))
                if(hasApiError(mr)) throw IllegalStateException(mr.body.optString("error",mr.body.optString("message","تعذر تحميل عضويتك الحالية (${mr.status})")))
                if(hasApiError(wr)) throw IllegalStateException(wr.body.optString("error",wr.body.optString("message","تعذر تحميل رصيد المحفظة (${wr.status})")))

                val levelBody=responseData(lr.body)
                val raw=levelBody.optJSONArray("levels")
                    ?:levelBody.optJSONArray("packages")
                    ?:levelBody.optJSONArray("vip_levels")
                    ?:lr.body.optJSONArray("levels")
                    ?:lr.body.optJSONArray("packages")
                    ?:JSONArray()
                val arr=JSONArray()
                for(i in 0 until raw.length()){
                    val x=raw.optJSONObject(i)?:continue
                    val n=x.optInt("level",x.optInt("vip_level",x.optInt("tier",0)))
                    val price=x.optLong("price_coins",
                        x.optLong("price_diamonds",x.optLong("price",x.optLong("coins",x.optLong("diamonds",0)))))
                    val active=x.optBoolean("active",x.optBoolean("is_active",true)) && price>0
                    if(n in 1..7){
                        arr.put(JSONObject()
                            .put("level",n)
                            .put("price",price)
                            .put("active",active)
                            .put("name",x.optString("name","SVIP $n")))
                    }
                }

                val meBody=responseData(mr.body)
                val sub=meBody.optJSONObject("subscription")
                    ?:meBody.optJSONObject("membership")
                    ?:meBody.optJSONObject("svip")
                    ?:mr.body.optJSONObject("subscription")
                    ?:JSONObject()
                val memberLevel=sub.optInt("level",sub.optInt("vip_level",
                    meBody.optInt("vip_level",meBody.optInt("level",0))))
                val membership=JSONObject()
                    .put("vip_level",memberLevel)
                    .put("expires_at",sub.optString("expires_at",meBody.optString("expires_at","")))

                val walletBody=responseData(wr.body)
                val wallet=walletBody.optJSONObject("wallet")?:walletBody
                val balance=wallet.optLong("recharge_coins",
                    wallet.optLong("diamonds",wallet.optLong("balance",wallet.optLong("coins",0))))

                if(arr.length()==0) throw IllegalStateException("وصل رد مستويات SVIP لكن لم نعثر على باقات بأسعار صالحة")
                val data=JSONObject()
                    .put("packages",arr)
                    .put("membership",membership)
                    .put("balance",balance)

                getSharedPreferences("SVIP_FAST_CACHE",MODE_PRIVATE).edit()
                    .putString("page_data",data.toString())
                    .putLong("saved_at",System.currentTimeMillis())
                    .apply()

                runOnUiThread{
                    loading=false
                    if(!isFinishing&&!isDestroyed){
                        try {
                            applyData(data)
                            if(!successMessage.isNullOrBlank())showToast(successMessage)
                        } catch (uiError:Exception) {
                            Log.e("SvipActivity","SVIP UI update failed",uiError)
                            bottomState.text="تعذر عرض بيانات SVIP"
                            bottomBack.text=uiError.message ?: uiError.javaClass.simpleName
                            buy.isEnabled=false
                            showToast("حدث خطأ أثناء عرض SVIP: ${uiError.message ?: uiError.javaClass.simpleName}")
                        }
                    }
                }
            }catch(e:Exception){
                val message=e.cause?.message?:e.message?:"تعذر تحميل بيانات SVIP"
                runOnUiThread{
                    loading=false
                    if(!isFinishing&&!isDestroyed){
                        bottomState.text="تعذر تحديث الباقات"
                        bottomBack.text=message
                        if(packs.isEmpty())buy.isEnabled=false
                        showToast(message)
                    }
                }
            }
        }
    }
    private fun applyData(data:JSONObject){
        val raw=data.optJSONArray("packages")?:JSONArray();val next= mutableListOf<Pack>()
        for(i in 0 until raw.length()){val p=raw.optJSONObject(i)?:continue;val n=p.optInt("level",p.optInt("vip_level",0));val price=p.optLong("price",p.optLong("price_diamonds",0));if(n in 1..7&&price>0)next.add(Pack(n,price,p.optBoolean("active",true)))}
        packs=next.sortedBy{it.level}.toMutableList();balance=data.optLong("balance",0)
        val m=data.optJSONObject("membership")?:JSONObject();owned=m.optInt("vip_level",m.optInt("level",0));expiresAt=m.optString("expires_at","")
        if(packs.isNotEmpty()){selected=if(packs.any{it.level==owned})owned else if(packs.any{it.level==selected})selected else packs.first().level;renderTabs();render()}
        else{bottomState.text="لا توجد مستويات SVIP متاحة حالياً";buy.isEnabled=false}
    }
    private fun showToast(s:String){
        val t=Toast.makeText(this,s,Toast.LENGTH_LONG);t.show()
    }
    override fun onDestroy(){io.shutdownNow();super.onDestroy()}

    private class DiamondView(c:android.content.Context):View(c){
        private val p=Paint(3)
        override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();val path=Path().apply{moveTo(w*.25f,h*.12f);lineTo(w*.75f,h*.12f);lineTo(w*.96f,h*.42f);lineTo(w*.5f,h*.94f);lineTo(w*.04f,h*.42f);close();moveTo(w*.04f,h*.42f);lineTo(w*.96f,h*.42f);moveTo(w*.25f,h*.12f);lineTo(w*.37f,h*.42f);lineTo(w*.5f,h*.94f);lineTo(w*.63f,h*.42f);lineTo(w*.75f,h*.12f)};p.color=0xff7fd8ff.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=dp(1f);c.drawPath(path,p)}
        private fun dp(v:Float)=(v*resources.displayMetrics.density)
    }

    private class IconView(c:android.content.Context,private val kind:String):View(c){
        var active=true
        private val p=Paint(3)
        override fun onDraw(c:Canvas){
            val s=min(width,height)*.78f;c.save();c.translate((width-s)/2,(height-s)/2);c.scale(s/24f,s/24f)
            p.style=Paint.Style.STROKE;p.strokeWidth=1.5f;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND;p.color=if(active)0xfff1ecdd.toInt() else 0xff8b93a6.toInt()
            val path=Path()
            when(kind){
                "entry"->{path.moveTo(13f,3f);path.lineTo(5f,13f);path.lineTo(11f,13f);path.lineTo(10f,21f);path.lineTo(18f,10f);path.lineTo(12f,10f)}
                "frame"->{c.drawCircle(12f,13.5f,6.5f,p);c.drawCircle(12f,13.5f,2.8f,p);path.moveTo(8f,4.5f);path.lineTo(10f,6.5f);path.lineTo(14f,6.5f);path.lineTo(16f,4.5f)}
                "vip"->{c.drawRoundRect(2.5f,6f,21.5f,18f,6f,6f,p);p.textSize=6.6f;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;c.drawText("VIP",12f,14.6f,p)}
                "follow","friends" -> {
                    c.drawCircle(10f,8f,3.6f,p)
                    path.moveTo(3.5f,20f)
                    path.cubicTo(3.5f,12f,16.5f,12f,16.5f,20f)
                    if(kind=="follow") {
                        path.moveTo(16f,11.5f); path.lineTo(18f,13.5f); path.lineTo(21.5f,9.5f)
                    } else {
                        path.moveTo(18f,20f); path.lineTo(15f,17f)
                        path.quadTo(14f,14f,17f,15f); path.lineTo(18f,16f)
                        path.lineTo(19f,15f); path.quadTo(22f,14f,21f,17f); path.close()
                    }
                }
                "car"->{path.moveTo(3.5f,16.5f);path.lineTo(3.5f,12.5f);path.lineTo(6f,8f);path.lineTo(18f,8f);path.lineTo(20.5f,12.5f);path.lineTo(20.5f,16.5f);path.close();c.drawCircle(7.5f,16.8f,1.9f,p);c.drawCircle(16.5f,16.8f,1.9f,p)}
                "support"->{path.moveTo(4f,14f);path.lineTo(4f,12f);path.cubicTo(4f,1f,20f,1f,20f,12f);path.lineTo(20f,14f);c.drawRoundRect(3f,13f,7f,19f,1.5f,1.5f,p);c.drawRoundRect(17f,13f,21f,19f,1.5f,1.5f,p);path.moveTo(19f,19f);path.quadTo(18f,22f,14f,21f)}
                "bubble"->{c.drawRoundRect(2.5f,6f,21.5f,18f,6f,6f,p);path.moveTo(10f,10f);path.lineTo(14f,10f);path.moveTo(12f,10f);path.lineTo(12f,14f);path.moveTo(17f,10f);path.lineTo(18.5f,10f);path.moveTo(17f,13.5f);path.lineTo(18.5f,13.5f)}
                "card"->{c.drawRoundRect(3f,7.5f,21f,20f,3f,3f,p);path.moveTo(7f,12.5f);path.lineTo(13f,12.5f);path.moveTo(7f,16f);path.lineTo(11f,16f);path.moveTo(15f,6f);path.lineTo(16.2f,3.8f);path.lineTo(17.5f,6f);path.lineTo(18.8f,3.8f);path.lineTo(19.5f,7f)}
                "palette"->{c.drawCircle(12f,12f,9f,p);c.drawCircle(7.5f,11f,1f,p);c.drawCircle(10f,7.5f,1f,p);c.drawCircle(14.5f,7.5f,1f,p)}
                "highlight"->{c.drawRoundRect(3f,5.5f,21f,17f,4f,4f,p);path.moveTo(8f,17f);path.lineTo(7f,20.5f);path.lineTo(11.5f,17f);path.moveTo(12f,8.3f);path.lineTo(13f,10.3f);path.lineTo(15.2f,10.6f);path.lineTo(13.6f,12.1f);path.lineTo(14f,14.3f);path.lineTo(12f,13.2f);path.lineTo(10f,14.3f);path.lineTo(10.4f,12.1f);path.lineTo(8.8f,10.6f);path.lineTo(11f,10.3f);path.close()}
                "shield"->{path.moveTo(12f,3f);path.lineTo(19f,6f);path.lineTo(19f,11.5f);path.quadTo(19f,18f,12f,21f);path.quadTo(5f,18f,5f,11.5f);path.close();c.drawRoundRect(9f,11f,15f,16f,1f,1f,p);path.moveTo(10f,11f);path.lineTo(10f,9.6f);path.quadTo(12f,6f,14f,9.6f);path.lineTo(14f,11f)}
                "mute"->{c.drawRoundRect(3f,5.5f,21f,17.5f,5f,5f,p);path.moveTo(8.5f,17.5f);path.lineTo(7f,21f);path.lineTo(11.5f,17.5f);path.moveTo(4f,3.5f);path.lineTo(20f,20.5f)}
                "star"->{for(i in 0..9){val a=(-90+i*36)*Math.PI/180;val r=if(i%2==0)9.5 else 4.3;val x=12+cos(a)*r;val y=12+sin(a)*r;if(i==0)path.moveTo(x.toFloat(),y.toFloat())else path.lineTo(x.toFloat(),y.toFloat())};path.close()}
                "door"->{c.drawRoundRect(5.5f,3f,16.5f,21f,1.5f,1.5f,p);c.drawCircle(13f,12f,.9f,p);path.moveTo(16.5f,8f);path.lineTo(20f,8f);path.moveTo(16.5f,16f);path.lineTo(20f,16f);path.moveTo(3f,21f);path.lineTo(21f,21f)}
                "mask"->{path.moveTo(3f,8.5f);path.cubicTo(6f,6.5f,9f,6.5f,12f,8.5f);path.cubicTo(15f,6.5f,18f,6.5f,21f,8.5f);path.cubicTo(21f,14f,18.5f,17f,15.5f,17f);path.quadTo(13f,17f,12f,15.8f);path.quadTo(10.2f,17f,8.5f,17f);path.cubicTo(5.5f,17f,3f,14f,3f,8.5f);path.moveTo(6.5f,11.5f);path.quadTo(8.5f,10.7f,9.7f,11.5f);path.moveTo(14.3f,11.5f);path.quadTo(16.5f,10.7f,17.5f,11.5f)}
            }
            c.drawPath(path,p);c.restore()
        }
    }

    /** Detailed metallic shield emblem, drawn natively (no SVG/WebView). */
    private class BadgeView(c:android.content.Context):View(c){
        var level=1
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        private fun shade(c:Int,t:Float):Int {
            fun channel(v:Int):Int {
                val target=if(t>0f) 255f else 0f
                return (v+(target-v)*abs(t)).roundToInt().coerceIn(0,255)
            }
            return Color.rgb(channel(Color.red(c)),channel(Color.green(c)),channel(Color.blue(c)))
        }
        private fun fill(shader:Shader?=null,color:Int=Color.WHITE){p.style=Paint.Style.FILL;p.shader=shader;p.color=color}
        private fun stroke(color:Int,w:Float){p.style=Paint.Style.STROKE;p.shader=null;p.color=color;p.strokeWidth=w;p.strokeJoin=Paint.Join.ROUND;p.strokeCap=Paint.Cap.ROUND}
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas);val w=width.toFloat();val h=height.toFloat();val scale=min(w/240f,h/272f)*.86f
            canvas.save();canvas.translate((w-240*scale)/2,(h-272*scale)/2);canvas.scale(scale,scale)
            val t=SVIP_TIERS[(level-1).coerceIn(0,6)];val metal=t[0];val gem=t[1];val rib=t[2]
            val hi=shade(metal,.62f);val lo=shade(metal,-.4f);val dk=shade(metal,-.68f)
            val gHi=shade(gem,.6f);val gLo=shade(gem,-.5f)
            // radial aura and tier rays
            fill(android.graphics.RadialGradient(120f,138f,108f,intArrayOf(Color.argb(150,Color.red(hi),Color.green(hi),Color.blue(hi)),Color.TRANSPARENT),null,Shader.TileMode.CLAMP))
            canvas.drawCircle(120f,138f,108f,p)
            if(level>=4){stroke(Color.argb(70,Color.red(hi),Color.green(hi),Color.blue(hi)),5f);for(i in 0 until 6+level){val a=i*2*PI/(6+level);canvas.drawLine(120f,138f,120f+102f*cos(a).toFloat(),138f+102f*sin(a).toFloat(),p)}}
            // symmetrical wings
            val wing=Path().apply{moveTo(128f,172f);cubicTo(200f,168f,198f,138f,182f,84f);cubicTo(168f,114f,150f,112f,128f,118f);close()}
            fill(LinearGradient(120f,80f,195f,180f,intArrayOf(hi,metal,lo,dk),null,Shader.TileMode.CLAMP));canvas.drawPath(wing,p)
            canvas.save();canvas.scale(-1f,1f,120f,0f);canvas.drawPath(wing,p);canvas.restore()
            stroke(dk,1.3f);canvas.drawPath(wing,p);canvas.save();canvas.scale(-1f,1f,120f,0f);canvas.drawPath(wing,p);canvas.restore()
            // crown tiers
            val crown=Path().apply{
                if(level>=7){moveTo(90f,54f);lineTo(88f,26f);lineTo(102f,41f);lineTo(110f,20f);lineTo(120f,37f);lineTo(130f,20f);lineTo(138f,41f);lineTo(152f,26f);lineTo(150f,54f)}
                else if(level>=4){moveTo(98f,54f);lineTo(96f,32f);lineTo(108f,44f);lineTo(120f,26f);lineTo(132f,44f);lineTo(144f,32f);lineTo(142f,54f)}
                else{moveTo(104f,54f);lineTo(120f,28f);lineTo(136f,54f)};close()
            }
            fill(LinearGradient(100f,20f,140f,55f,intArrayOf(hi,metal,lo),null,Shader.TileMode.CLAMP));canvas.drawPath(crown,p);stroke(dk,1.3f);canvas.drawPath(crown,p)
            if(level>=4){fill(null,gem);listOf(120f to 30f,110f to 20f,130f to 20f).forEach{canvas.drawCircle(it.first,it.second,3.4f,p)}}
            // shield body with three metallic layers
            val sh=Path().apply{moveTo(120f,46f);lineTo(178f,70f);lineTo(178f,144f);cubicTo(178f,182f,150f,208f,120f,234f);cubicTo(90f,208f,62f,182f,62f,144f);lineTo(62f,70f);close()}
            fill(LinearGradient(62f,60f,178f,220f,intArrayOf(lo,hi,metal,lo),null,Shader.TileMode.CLAMP));canvas.drawPath(sh,p)
            stroke(dk,1.8f);canvas.drawPath(sh,p)
            canvas.save();canvas.translate(120f,140f);canvas.scale(.9f,.9f);canvas.translate(-120f,-140f);fill(LinearGradient(80f,50f,160f,230f,intArrayOf(hi,metal,lo,dk),null,Shader.TileMode.CLAMP));canvas.drawPath(sh,p);canvas.restore()
            canvas.save();canvas.translate(120f,140f);canvas.scale(.8f,.8f);canvas.translate(-120f,-140f);fill(LinearGradient(70f,65f,170f,215f,intArrayOf(0xff232c40.toInt(),0xff0e131f.toInt()),null,Shader.TileMode.CLAMP));canvas.drawPath(sh,p);canvas.restore()
            // jewel
            fill(android.graphics.RadialGradient(120f,130f,48f,intArrayOf(Color.argb(150,Color.red(gem),Color.green(gem),Color.blue(gem)),Color.TRANSPARENT),null,Shader.TileMode.CLAMP));canvas.drawCircle(120f,130f,48f,p)
            val dia=Path().apply{moveTo(120f,88f);lineTo(152f,130f);lineTo(120f,172f);lineTo(88f,130f);close()}
            fill(LinearGradient(88f,88f,152f,172f,intArrayOf(gHi,gem,gLo),null,Shader.TileMode.CLAMP));canvas.drawPath(dia,p);stroke(hi,1.5f);canvas.drawPath(dia,p)
            fill(null,Color.argb(45,255,255,255));val facet=Path().apply{moveTo(120f,88f);lineTo(136f,130f);lineTo(120f,172f);close()};canvas.drawPath(facet,p)
            stroke(Color.argb(150,255,255,255),1f);canvas.drawLine(88f,130f,152f,130f,p)
            // ribbon tails and plate
            fill(null,rib);val tail=Path().apply{moveTo(54f,190f);lineTo(70f,182f);lineTo(70f,212f);lineTo(54f,212f);lineTo(63f,201f);close()};canvas.drawPath(tail,p);canvas.save();canvas.scale(-1f,1f,120f,0f);canvas.drawPath(tail,p);canvas.restore()
            fill(LinearGradient(66f,178f,66f,208f,intArrayOf(shade(rib,.35f),rib,shade(rib,-.45f)),null,Shader.TileMode.CLAMP));canvas.drawRect(66f,178f,174f,208f,p);stroke(hi,2f);canvas.drawRect(66f,178f,174f,208f,p)
            stroke(hi,1f);canvas.drawLine(71f,183f,169f,183f,p);canvas.drawLine(71f,203f,169f,203f,p)
            fill(null,0xfffffbe6.toInt());p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create("serif",Typeface.BOLD);p.textSize=20f;canvas.drawText("SVIP $level",120f,199.5f,p)
            // rivets and sparkles
            fill(null,hi);listOf(72f to 84f,168f to 84f,120f to 60f,66f to 118f,174f to 118f).forEach{canvas.drawCircle(it.first,it.second,2.4f,p)}
            fill(null,gHi);val points=listOf(44f to 70f,198f to 62f,36f to 170f,206f to 170f,62f to 36f,180f to 36f,28f to 120f,212f to 118f,120f to 12f)
            points.take(3+level/2).forEachIndexed{i,pt->val x=pt.first;val y=pt.second;val sp=Path().apply{moveTo(x,y-5);lineTo(x+1.4f,y-1.4f);lineTo(x+5f,y);lineTo(x+1.4f,y+1.4f);lineTo(x,y+5);lineTo(x-1.4f,y+1.4f);lineTo(x-5f,y);lineTo(x-1.4f,y-1.4f);close()};canvas.drawPath(sp,p)}
            canvas.restore()
        }
        companion object { val SVIP_TIERS=arrayOf(
            intArrayOf(0xffc98a54.toInt(),0xffff9a4a.toInt(),0xff7a3b1a.toInt()),
            intArrayOf(0xffc3ccd8.toInt(),0xff5ec8ff.toInt(),0xff2c4a6b.toInt()),
            intArrayOf(0xffe9b94a.toInt(),0xff3ddc97.toInt(),0xff1f6b4a.toInt()),
            intArrayOf(0xffefc15a.toInt(),0xff4aa3ff.toInt(),0xff24407a.toInt()),
            intArrayOf(0xfff2c660.toInt(),0xffb36bff.toInt(),0xff5a2a8a.toInt()),
            intArrayOf(0xfff4cb68.toInt(),0xffff5fa8.toInt(),0xff8a1f55.toInt()),
            intArrayOf(0xfff6d070.toInt(),0xffff3b4a.toInt(),0xff9c1424.toInt())
        ) }
    }
}
