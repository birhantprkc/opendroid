# ProGuard rules for OpenDroid app

# Protect LLM providers and serializable structures
-keep class com.opendroid.ai.core.llm.** { *; }
-keep class com.opendroid.ai.data.models.** { *; }
-keep class com.opendroid.ai.data.db.entities.** { *; }

# Keep Hilt and Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn com.google.dagger.hilt.processor.**

# Keep standard Retrofit/Gson structures
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }

# Gson specific rules
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
