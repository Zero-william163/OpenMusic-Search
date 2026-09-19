# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.openmusic.search.data.remote.dto.** { *; }
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
