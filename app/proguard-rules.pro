# Keep Room entities/DAOs and AccessibilityService classes intact
-keep class com.autoswipe.ai.studio.service.** { *; }
-keep class com.autoswipe.ai.studio.data.** { *; }
-keep class com.autoswipe.ai.studio.model.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
