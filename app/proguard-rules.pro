-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Models
-keep class com.flowdroid.launcher.data.models.** { *; }

# Kotlin serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
