package com.mariam.live

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable
import org.json.JSONObject
import org.json.JSONArray
import java.net.URL
import java.util.concurrent.Executors

class InventoryActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val purple = Color.rgb(112,75,246)
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(250,249,253)
        window.navigationBarColor=Color.WHITE
        val root=LinearLayout(this).apply {
            orientation=1
            setPadding(dp(18),dp(14),dp(18),dp(18))
            setBackgroundColor(Color.rgb(250,249,253))
        }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val back=TextView(this).apply {
            text="‹";textSize=32f;setTextColor(purple);gravity=Gravity.CENTER
            setOnClickListener{finish()}
        }
        header.addView(back,LinearLayout.LayoutParams(dp(44),dp(48)))
        header.addView(TextView(this).apply {
            text="حقيبتي 🎒";textSize=22f;setTextColor(Color.rgb(35,34,53))
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(header)
        root.addView(TextView(this).apply {
            text="إطارات وخلفيات ملفك الشخصي";textSize=14f
            setTextColor(Color.rgb(100,94,120));setPadding(0,dp(5),0,dp(12))
        })
        status=TextView(this).apply { textSize=13f;setTextColor(purple);setPadding(0,dp(4),0,dp(8)) }
        root.addView(status)
        list=LinearLayout(this).apply{orientation=1}
        root.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        loadInventory()
    }

    private fun loadInventory() {
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){finish();return}
        status.text="جارٍ تحميل الحقيبة..."
        executor.execute {
            try {
                val response=AuthApi.get("/api/inventory",token)
                if(response.status !in 200..299) throw Exception(response.body.optString("error","تعذر تحميل الحقيبة"))
                val items=response.body.optJSONArray("items")?:JSONArray()
                runOnUiThread {
                    list.removeAllViews()
                    if(items.length()==0) {
                        list.addView(TextView(this).apply {
                            text="حقيبتك فارغة حالياً 🎒\nستظهر هنا الإطارات والخلفيات التي تحصل عليها من الإدارة أو العروض أو المتجر."
                            textSize=15f;gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER
                            setTextColor(Color.rgb(95,88,116));setPadding(dp(20),dp(40),dp(20),dp(40))
                        })
                    } else {
                        for(i in 0 until items.length()) addItem(items.optJSONObject(i)?:continue,token)
                    }
                    status.text="عدد العناصر: "+items.length()
                }
            } catch(e:Exception) {
                runOnUiThread{status.text=e.message?:"تعذر الاتصال بالخادم"}
            }
        }
    }

    private fun addItem(item:JSONObject,token:String) {
        val id=item.optString("item_id")
        val name=item.optString("name","عنصر")
        val type=item.optString("item_type")
        val url=item.optString("asset_url")
        val owned=item.optBoolean("owned")
        val equipped=item.optBoolean("equipped")
        val card=LinearLayout(this).apply {
            orientation=0;gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(12),dp(12),dp(12),dp(12))
            background=GradientDrawable().apply {
                setColor(Color.WHITE);cornerRadius=dp(16).toFloat()
                setStroke(dp(1),Color.rgb(229,224,240))
            }
        }
        val preview=AnimatedGifPreview(this).apply {
            background=GradientDrawable().apply {
                setColor(Color.rgb(246,243,255));cornerRadius=dp(12).toFloat()
            }
        }
        card.addView(preview,LinearLayout.LayoutParams(dp(76),dp(76)))
        if(type=="chat_bubble") {
            preview.setBubbleStyle(id)
        } else if(url.startsWith("https://")) executor.execute {
            try {
                val bytes=URL(url).openConnection().apply{connectTimeout=12000;readTimeout=15000}.getInputStream().use{it.readBytes()}
                runOnUiThread{if(!isFinishing)preview.setGif(bytes)}
            }catch(e:Exception){
                android.util.Log.e("InventoryPreview","GIF load failed for $id",e)
            }
        }
        val details=LinearLayout(this).apply { orientation=1;setPadding(dp(12),0,0,0) }
        details.addView(TextView(this).apply {
            text=name;textSize=15f;setTextColor(Color.rgb(35,34,53))
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        })
        details.addView(TextView(this).apply {
            text=when(type) {
                "frame" -> "إطار صورة"
                "background" -> "خلفية ملف شخصي"
                "chat_bubble" -> "فقاعة دردشة للغرفة"
                else -> "عنصر تجميلي"
            }
            textSize=12f;setTextColor(Color.rgb(110,103,130));setPadding(0,dp(4),0,dp(5))
        })
        val action=TextView(this).apply {
            text=when { equipped->"مركّب ✓"; owned->"تركيب"; else->"غير متوفر" }
            textSize=12f;gravity=Gravity.CENTER;setPadding(dp(10),dp(8),dp(10),dp(8))
            setTextColor(if(owned)Color.WHITE else Color.GRAY)
            background=GradientDrawable().apply {
                setColor(if(owned)purple else Color.rgb(239,237,243));cornerRadius=dp(10).toFloat()
            }
            // Same button toggles off when the item is already equipped.
            isEnabled=owned
            alpha=if(owned)1f else 0.65f
            setOnClickListener{toggleItem(id,type,equipped,token)}
        }
        details.addView(action)
        card.addView(details,LinearLayout.LayoutParams(0,-2,1f))
        list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
    }

    private fun toggleItem(itemId:String,itemType:String,equipped:Boolean,token:String) {
        status.text=if(equipped)"جارٍ إيقاف العنصر..." else "جارٍ تركيب العنصر..."
        executor.execute {
            try {
                val response=if(equipped){
                    AuthApi.post("/api/inventory/unequip",JSONObject().put("item_type",itemType),token)
                }else{
                    AuthApi.post("/api/inventory/equip",JSONObject().put("item_id",itemId),token)
                }
                if(response.status !in 200..299) throw Exception(response.body.optString("error","تعذر تحديث العنصر"))
                runOnUiThread{
                    status.text=if(equipped)"تم إيقاف العنصر، ويمكنك تركيبه مجدداً" else "تم تركيب العنصر بنجاح"
                    loadInventory()
                }
            } catch(e:Exception) {
                runOnUiThread{status.text=e.message?:"تعذر الاتصال بالخادم"}
            }
        }
    }

    override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}


/** Small animated GIF preview. BitmapFactory does not reliably decode animated GIFs. */
private class AnimatedGifPreview(context: android.content.Context) : View(context) {
    private var movie: Movie? = null
    private var bubbleStyle: ChatBubbleStyleDrawable? = null
    private var startedAt = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun setBubbleStyle(styleId: String) {
        movie = null
        bubbleStyle = ChatBubbleStyleDrawable(styleId).also { it.callback = this }
        invalidate()
    }

    fun setGif(bytes: ByteArray) {
        bubbleStyle = null
        movie = Movie.decodeByteArray(bytes, 0, bytes.size)
        startedAt = SystemClock.uptimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val style = bubbleStyle
        if (style != null) {
            style.bounds = android.graphics.Rect(0, 0, width, height)
            style.draw(canvas)
            postInvalidateOnAnimation()
            return
        }
        val gif = movie ?: return
        val duration = gif.duration().takeIf { it > 0 } ?: 1000
        val elapsed = ((SystemClock.uptimeMillis() - startedAt) % duration).toInt()
        gif.setTime(elapsed)
        val sx = width.toFloat() / gif.width().coerceAtLeast(1)
        val sy = height.toFloat() / gif.height().coerceAtLeast(1)
        val scale = minOf(sx, sy)
        val save = canvas.save()
        canvas.translate((width - gif.width() * scale) / 2f, (height - gif.height() * scale) / 2f)
        canvas.scale(scale, scale)
        gif.draw(canvas, 0f, 0f, paint)
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }
}
