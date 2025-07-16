# ✅ حل مشكلة تشغيل الأغاني المقيدة بالعمر في تطبيقات YouTube Music

## 🎯 المشكلة
كان التطبيق يفشل في تشغيل الأغاني المقيدة بالعمر مع أخطاء مثل:
- `"this video can be inapropriate for some users"`
- `"Sign in to confirm your age"`
- `"This video is restricted"`

## 🔬 السبب الجذري
التطبيق كان يستخدم عملاء YouTube عاديين فقط، بينما SimpMusic يستخدم عملاء خاصين محددين لتجاوز قيود العمر.

## 🛠️ الحل المطبق
بناءً على بحوث **YouTube-Internal-Clients** و **yt-dlp**، تم تطبيق نفس الآلية المستخدمة في SimpMusic:

### 1. إضافة عملاء YouTube متخصصين لتجاوز قيود العمر

```kotlin
// في YouTubeClient.kt - العملاء الأكثر فعالية
val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
    clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
    clientVersion = "2.0",
    clientId = "85",
    userAgent = USER_AGENT_WEB,
    loginSupported = false,
    useSignatureTimestamp = false
)

val ANDROID_EMBEDDED_PLAYER = YouTubeClient(
    clientName = "ANDROID_EMBEDDED_PLAYER", 
    clientVersion = "19.13.36",
    clientId = "55",
    userAgent = "com.google.android.youtube/19.13.36 (Linux; U; Android 11) gzip",
    loginSupported = false,
    useSignatureTimestamp = false
)
```

### 2. ترتيب العملاء حسب الفعالية

```kotlin
// في YTPlayerUtils.kt - ترتيب محسن بناءً على البحث
private val AGE_RESTRICTION_BYPASS_CLIENTS: Array<YouTubeClient> = arrayOf(
    TVHTML5_SIMPLY_EMBEDDED_PLAYER, // أقوى عميل لتجاوز قيود العمر
    ANDROID_EMBEDDED_PLAYER,        // عميل مدمج أندرويد فعال جداً
    ANDROID_TESTSUITE,              // عميل اختبار أندرويد
    WEB_EMBEDDED,                   // عميل ويب مدمج
    TVHTML5,                       // عميل تلفاز HTML5
    WEB_MUSIC_ANALYTICS,           // عميل التحليلات
    ANDROID_VR_NO_AUTH             // عميل VR كاحتياطي
)
```

### 3. كشف محسن لأخطاء قيود العمر

```kotlin
// كشف شامل يشمل النص المحدد الذي ذكره المستخدم
val isAgeRestricted = errorReason?.let { reason ->
    // النص المحدد الذي ذكره المستخدم
    reason.contains("this video can be inapropriate for some users", ignoreCase = true) ||
    reason.contains("this video may be inappropriate for some users", ignoreCase = true) ||
    // أخطاء قيود العمر الشائعة
    reason.contains("age", ignoreCase = true) ||
    reason.contains("sign in", ignoreCase = true) ||
    reason.contains("restricted", ignoreCase = true) ||
    reason.contains("verification", ignoreCase = true) ||
    reason.contains("confirm your age", ignoreCase = true) ||
    reason.contains("inappropriate", ignoreCase = true) ||
    reason.contains("inapropriate", ignoreCase = true) ||  // خطأ إملائي في YouTube
    reason.contains("content warning", ignoreCase = true) ||
    reason.contains("some users", ignoreCase = true) ||
    reason.contains("mature content", ignoreCase = true) ||
    reason.contains("adult content", ignoreCase = true) ||
    reason.contains("explicit content", ignoreCase = true) ||
    // حالات الحالة المختلفة
    streamPlayerResponse.playabilityStatus.status.equals("LOGIN_REQUIRED", ignoreCase = true) ||
    streamPlayerResponse.playabilityStatus.status.equals("UNPLAYABLE", ignoreCase = true) ||
    streamPlayerResponse.playabilityStatus.status.equals("CONTENT_CHECK_REQUIRED", ignoreCase = true)
} == true
```

### 4. آلية تجاوز قيود العمر الذكية

```kotlin
if (isAgeRestricted) {
    Timber.tag(logTag).d("Age restriction detected: $errorReason. Trying bypass clients...")
    
    // جرب عملاء تجاوز قيود العمر بالترتيب
    for (bypassClient in AGE_RESTRICTION_BYPASS_CLIENTS) {
        try {
            Timber.tag(logTag).d("Attempting age bypass with client: ${bypassClient.clientName}")
            val bypassResponse = YouTube.player(videoId, playlistId, bypassClient, signatureTimestamp).getOrNull()
            
            if (bypassResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Age restriction bypassed successfully with client: ${bypassClient.clientName}")
                
                // تحديث جميع البيانات المطلوبة
                streamPlayerResponse = bypassResponse
                format = findBestAudioFormat(bypassResponse)
                streamUrl = format?.url
                streamExpiresInSeconds = bypassResponse.streamingData?.expiresInSeconds
                
                // الخروج من الحلقة عند النجاح
                break
            }
        } catch (e: Exception) {
            Timber.tag(logTag).w("Bypass attempt failed with ${bypassClient.clientName}: ${e.message}")
        }
    }
}
```

## 📊 ميزات الحل

### ✅ المزايا:
1. **فعالية عالية**: استخدام نفس العملاء المستخدمة في SimpMusic
2. **تغطية شاملة**: كشف جميع أنواع أخطاء قيود العمر
3. **نظام احتياطي**: عدة عملاء للتجاوز بترتيب الفعالية
4. **تسجيل مفصل**: لمساعدة في التشخيص
5. **محاولة أخيرة**: يجرب عملاء التجاوز حتى لو لم يكتشف قيد العمر صراحة

### 🔧 التحسينات المضافة:
1. **إعادة ترتيب العملاء**: `TVHTML5_SIMPLY_EMBEDDED_PLAYER` كأول عميل
2. **كشف أدق**: النص المحدد "inapropriate" (بالخطأ الإملائي)
3. **معالجة شاملة**: تحديث جميع البيانات المطلوبة بعد النجاح
4. **تسجيل محسن**: متابعة دقيقة لعملية التجاوز

## 🎯 النتيجة المتوقعة

بعد تطبيق هذا الحل، يجب أن يصبح التطبيق قادراً على:
- ✅ تشغيل الأغاني المقيدة بالعمر بنجاح
- ✅ التعامل مع جميع أنواع أخطاء قيود العمر
- ✅ استخدام نفس الآلية المطبقة في SimpMusic
- ✅ تقديم تجربة سلسة للمستخدم

## 🔍 استكشاف الأخطاء

إذا استمرت المشكلة، تحقق من:
1. **Logs**: ابحث عن `"YTPlayerUtils"` في logcat
2. **العملاء**: تأكد من استيراد العملاء الجديدة
3. **النسخة**: تأكد من أن التطبيق محدث بالكود الجديد

## 📝 المراجع
- [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients)
- [yt-dlp Age Bypass Implementation](https://github.com/yt-dlp/yt-dlp/commit/c888ffb95ab0ab4f4cd1d6c93eda014f80479551)
- [SimpMusic Project](https://github.com/maxrave-dev/SimpMusic)

---

## 🏁 ملخص التغييرات

### ملفات تم تعديلها:
1. `innertube/src/main/kotlin/com/metrolist/innertube/models/YouTubeClient.kt`
   - إضافة `TVHTML5_SIMPLY_EMBEDDED_PLAYER`
   - إضافة `ANDROID_EMBEDDED_PLAYER`

2. `app/src/main/kotlin/com/metrolist/music/utils/YTPlayerUtils.kt`
   - إضافة imports للعملاء الجديدة
   - تحديث `AGE_RESTRICTION_BYPASS_CLIENTS`
   - تحسين كشف أخطاء قيود العمر
   - إعادة ترتيب `STREAM_FALLBACK_CLIENTS`

هذا الحل مبني على نفس الأسس المستخدمة في SimpMusic ويجب أن يحل المشكلة تماماً. 🎵