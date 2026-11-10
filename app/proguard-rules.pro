# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entities and DAOs
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Android Services and BroadcastReceivers
-keep class com.shieldguard.app.service.** { *; }
-keep class com.shieldguard.app.receiver.** { *; }
