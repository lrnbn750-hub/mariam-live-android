package com.mariam.live

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class FollowListActivity : Activity() {
    private val io = Executors.newFixedThreadPool(4)
    private val ink = Color.rgb(28, 30, 38)
    private val muted = Color.rgb(150, 154, 164)
    private val pale = Color.rgb(244, 245, 249)
    private lateinit var rows: LinearLayout
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private var all = JSONArray()
    private var type = "followers"
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text=value; textSize=size; setTextColor(color); gravity=Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun pill(fill: Int) = GradientDrawable().apply { setColor(fill); cornerRadius=dp(28).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,true)
        window.statusBarColor=Color.WHITE; window.navigationBarColor=Color.rgb(242,243,246)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        type=intent.getStringExtra("list_type") ?: "followers"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); layoutDirection=View.LAYOUT_DIRECTION_RTL; setPadding(dp(18),dp(8),dp(18),dp(8)) }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_LTR }
        top.addView(label("‹",38f,ink).apply { setOnClickListener { finish() } },LinearLayout.LayoutParams(dp(48),dp(54)))
        top.addView(label(if(type=="followers") "المتابعون" else "أتابعهم",21f,ink,true),LinearLayout.LayoutParams(0,dp(54),1f))
        top.addView(View(this),LinearLayout.LayoutParams(dp(48),dp(54)))
        root.addView(top)
        val tabs=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; layoutDirection=View.LAYOUT_DIRECTION_RTL; setPadding(0,dp(10),0,dp(12)) }
        tabs.addView(label("الكل",17f,ink,true),LinearLayout.LayoutParams(0,dp(48),1f))
        tabs.addView(label("متابعة خاصة (0)",16f,muted),LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(tabs)
        search=EditText(this).apply { hint="البحث"; textSize=15f; setTextColor(ink); setHintTextColor(muted); singleLine=true; background=pill(pale); setPadding(dp(18),0,dp(18),0); layoutDirection=View.LAYOUT_DIRECTION_RTL }
        root.addView(search,LinearLayout.LayoutParams(-1,dp(50)).apply { bottomMargin=dp(12) })
        val scroll=ScrollView(this).apply { isFillViewport=true }
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        rows=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        empty=label("جارٍ تحميل القائمة...",15f,muted)
        body.addView(rows); body.addView(empty,LinearLayout.LayoutParams(-1,dp(100))); scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
        search.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){ render() }
            override fun afterTextChanged(s:Editable?){}
        })
        load()
    }

    private fun load() {
        val token=getSharedPreferences("MariamLive",MODE_PRIVATE).getString("token",null)
        if(token.isNullOrBlank()){ finish(); return }
        io.execute {
            try {
                val result=AuthApi.get("/api/follows/me",token)
                if(result.status !in 200..299) throw Exception(result.body.optString("error","تعذر تحميل القائمة"))
                all=result.body.optJSONArray(type) ?: JSONArray()
                runOnUiThread { if(!isFinishing) render() }
            } catch(e:Exception) {
                runOnUiThread { if(!isFinishing){ empty.text=e.message ?: "تعذر تحميل القائمة"; Toast.makeText(this,empty.text,Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    private fun render() {
        rows.removeAllViews()
        val q=search.text?.toString()?.trim().orEmpty()
        var shown=0
        for(i in 0 until all.length()) {
            val item=all.optJSONObject(i) ?: continue
            val name=item.optString("display_name").takeIf { it.isNotBlank() && it!="null" }
                ?: item.optString("username").takeIf { it.isNotBlank() && it!="null" } ?: "مستخدم"
            if(q.isNotBlank() && !name.contains(q,true) && !item.optString("username").contains(q,true)) continue
            shown++; rows.addView(makeRow(item,name))
        }
        empty.visibility=if(shown==0) View.VISIBLE else View.GONE
        empty.text=if(all.length()==0) "لا توجد حسابات في هذه القائمة بعد" else "لا توجد نتائج مطابقة"
    }

    private fun makeRow(item:JSONObject,name:String):View {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_LTR; setPadding(dp(4),dp(12),dp(4),dp(12)); isClickable=true
            setOnClickListener { val id=item.optString("id"); if(id.isNotBlank()) startActivity(android.content.Intent(this@FollowListActivity,ProfileActivity::class.java).putExtra("profile_user_id",id)) }
        }
        val action=TextView(this).apply { text=if(type=="followers") "يتابعك" else "تتابعه"; textSize=14f; setTextColor(Color.rgb(139,145,157)); gravity=Gravity.CENTER; background=pill(pale) }
        row.addView(action,LinearLayout.LayoutParams(dp(104),dp(42)))
        row.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        val nameView=TextView(this).apply { text=name; textSize=16f; setTextColor(ink); setTypeface(typeface,Typeface.MEDIUM); gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT; maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END }
        row.addView(nameView,LinearLayout.LayoutParams(0,dp(54),1f).apply { marginEnd=dp(10) })
        val avatar=ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP; background=pill(pale); clipToOutline=true; setImageDrawable(android.graphics.drawable.ColorDrawable(Color.rgb(230,232,238))) }
        row.addView(avatar,LinearLayout.LayoutParams(dp(54),dp(54)))
        val url=item.optString("avatar_url")
        if(url.startsWith("https://") || url.startsWith("http://")) io.execute {
            try { val bmp=URL(url).openStream().use { BitmapFactory.decodeStream(it) }; if(bmp!=null) runOnUiThread { if(!isFinishing) avatar.setImageBitmap(bmp) } } catch(_:Exception){}
        }
        return row
    }
    override fun onDestroy(){ io.shutdownNow(); super.onDestroy() }
}