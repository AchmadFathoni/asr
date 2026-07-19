# ProGuard rules for ASR
-keepattributes *Annotation*

# Kotlin serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.asr.**$$serializer { *; }
-keepclassmembers class com.asr.** { *** Companion; }
-keepclasseswithmembers class com.asr.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room3.RoomDatabase
-keep @androidx.room3.Entity class *
-dontwarn androidx.room3.paging.**

# WorkManager
-keep class * extends androidx.work.impl.WorkDatabase { <init>(); }
-keep class androidx.work.** { *; }

# Glance
-keep class com.asr.widget.** { *; }
