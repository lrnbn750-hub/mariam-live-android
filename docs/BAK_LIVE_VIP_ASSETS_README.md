# BAK LIVE - VIP 1-12 Asset Pack

12 مجموعات كاملة (تصميم وأصول فقط - لا تغيير في النظام أو الأسعار أو Backend).

## البنية
- `assets/vip/vip_XX/` : frame.png/.webp (512x512، فتحة الصورة الشخصية شفافة، قطر الفتحة ~58%)، badge.png/.webp/.svg (256)،
  bubble.png/.webp + bubble.9.png (Nine-Patch جاهز)، icon.png/.svg (96)، particles.png/.webp (سبرايت جزيئات)،
  entrance.json (تأثير دخول الغرفة 720x160)، appear.json (انفجار ضوئي حول الصورة عند ظهور المستخدم 400x400).
- `android_res/drawable-nodpi` و `android_res/raw`: نسخة بأسماء متوافقة مع Android (vip_05_frame.png ... vip_05_entrance.json) للنسخ المباشر إلى `res/`.
- `preview/` : لوحة المقارنة (خلفية داكنة وفاتحة).
- `manifest.json` : الألوان + لون النص المقترح داخل الفقاعة لكل مستوى (تباين 4.5:1 محسوب).
- `tools/` : سكربت التوليد لإعادة إنتاج الأصول أو تعديل الألوان.

## ملاحظات الدمج (Kotlin / Views)
- الإطار: ضع الصورة الشخصية داخل FrameLayout وفوقها ImageView للإطار بحجم ~1.7x من حجم الصورة (الفتحة تغطي ~58% من عرض الإطار).
- الفقاعة: `textView.setBackgroundResource(R.drawable.vip_05_bubble)` (ملف .9.png). لون النص من manifest.json (bubble_text_color).
- المؤثرات: مكتبة Lottie (`com.airbnb.android:lottie`) - `animationView.setAnimation(R.raw.vip_05_entrance)`.
  مؤثرات Lottie بدون نصوص مدمجة (اسم المستخدم والشارة يضعهما التطبيق فوقها).
- SVG: للشارة والأيقونة؛ الأرقام محولة إلى مسارات (لا تحتاج خطوطاً). استورد عبر Vector Asset Studio (التدرجات تتطلب API 24+).
