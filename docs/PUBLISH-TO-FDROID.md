# نشر Metrolist على F-Droid - دليل سريع

## 📋 الملخص التنفيذي

تم إعداد جميع الملفات المطلوبة لنشر Metrolist على F-Droid. اتبع الخطوات التالية:

## 🚀 خطوات النشر

### 1. إنشاء حساب GitLab (إذا لم يكن موجود)
- اذهب إلى https://gitlab.com
- أنشئ حساب جديد أو سجل دخول

### 2. Fork مستودع F-Droid Data
- اذهب إلى https://gitlab.com/fdroid/fdroiddata
- اضغط على زر "Fork" في الأعلى
- انتظر حتى يكتمل الـ fork

### 3. إضافة ملف Metadata
- في مستودعك المنسوخ، اذهب إلى مجلد `metadata/`
- أنشئ ملف جديد باسم `com.metrolist.music.yml`
- انسخ محتوى الملف من `metadata/com.metrolist.music.yml` في هذا المشروع

### 4. إنشاء Merge Request
- اذهب إلى مستودعك المنسوخ على GitLab
- اضغط "Create merge request"
- اكتب عنوان: `Add Metrolist music player`
- اكتب وصف:
  ```
  Material 3 YouTube Music client for Android
  
  Features:
  - Play any song or video from YT Music
  - Background playback
  - Library management
  - Download and cache songs for offline playback
  - Live lyrics
  - YouTube Music account login support
  - Material 3 design
  
  License: GPL-3.0-only
  Source: https://github.com/mostafaalagamy/Metrolist
  ```

### 5. المتابعة
- انتظر مراجعة فريق F-Droid (قد تستغرق أسابيع)
- رد على أي تعليقات أو طلبات تعديل
- بعد الموافقة، سيظهر التطبيق في متجر F-Droid

## 📄 الملفات المُعدة

✅ `metadata/com.metrolist.music.yml` - ملف البيانات الوصفية  
✅ `docs/F-DROID-SUBMISSION.md` - دليل شامل  
✅ `fastlane/metadata/` - وصف التطبيق والصور  
✅ `README.md` - تم إضافة badge لـ F-Droid  

## 🔍 معلومات مهمة

- **Package ID**: `com.metrolist.music`
- **Current Version**: 12.2.0 (123)
- **License**: GPL-3.0-only
- **Category**: Multimedia
- **Build**: Gradle (universal flavor)

## ⚠️ ملاحظات

1. تأكد من أن التطبيق يبنى بنجاح من المصدر
2. لا تستخدم مكونات proprietary
3. احرص على تحديث النسخ مستقبلياً عبر Git tags
4. اقرأ تعليقات المراجعين بعناية

## 📞 المساعدة

- دليل F-Droid الرسمي: https://f-droid.org/docs/
- مجتمع F-Droid: https://forum.f-droid.org/
- Matrix chat: #fdroid:f-droid.org