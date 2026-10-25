# ProGuard & R8 Optimization / Hardening Rules for GSTU Go (ClassMate)

# Keep line numbers for symbolication in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Strip debug logs in production builds (OWASP M6) ---
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# --- Data Models & DTOs for Firebase Firestore Serialization ---
-keep class com.shuaib.classmate.models.** { *; }
-keepclassmembers class com.shuaib.classmate.models.** {
    <init>(...);
    public void set*(...);
    public <fields>;
}

# --- Room Persistence Library ---
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.shuaib.classmate.data.local.** { *; }

# --- Retrofit & OkHttp Network Models ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.shuaib.classmate.network.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# --- Gson / JSON Serialization ---
-keepattributes EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Glide Image Loading ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# --- ViewBinding ---
-keep class com.shuaib.classmate.databinding.** { *; }
