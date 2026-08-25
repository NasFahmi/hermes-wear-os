# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class kotlinx.serialization.json.** { *; }
-keep class com.hermes.wearos.data.models.** { *; }
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
