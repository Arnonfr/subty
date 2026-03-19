# ──────────────────────────────────────────────────────────────────────────────
# SubTranslate ProGuard / R8 rules
# ──────────────────────────────────────────────────────────────────────────────

# Keep app entry points
-keep class com.subtranslate.** { *; }

# Hilt / Dagger — keep generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Retrofit + Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Tink (used by security-crypto 1.1.0) — suppress annotation stubs
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# Coil
-dontwarn coil.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Generic rules for reflection
-keepattributes EnclosingMethod
-keepattributes InnerClasses
