# Proguard Rules
#
#
#
-keepattributes Signature
-keepattributes *Annotation*
-keep class se.zinokader.service.** {*;}
-keep class com.spotify.sdk.android.** {*;}
-keep class kaaes.** {*;}
-keep class com.wnafee.vector.** {*;}
-keep class retrofit.** { *; }
-keep class br.com.mauker.MsvAuthority
-keep class com.yalantis.ucrop** { *; }
-keep class rx.**
-keep class okio.**
-keep class com.squareup.okhttp.*
-keep class retrofit.appengine.UrlFetchClient
-keep interface com.yalantis.ucrop** { *; }

# RXJAVA RULES START
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontwarn sun.misc.**
# RXJAVA RULES END

-keepclassmembers class br.com.mauker.** { *; }
-keepclassmembers class se.zinokader.spotiq.model.** {*;}
-keepclasseswithmembers class * {@retrofit.http.* <methods>;}

-dontwarn okio.**
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit.http.** <methods>;
}

-dontwarn com.squareup.okhttp.**
