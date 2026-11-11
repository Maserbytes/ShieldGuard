# Room: keep database, entities and DAOs reachable by reflection.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keep class com.shieldguard.app.data.local.entity.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Enums are resolved by name (RuleType.valueOf / RuleAction.valueOf) when loading the bundled
# and community rule JSON. R8 must not fold or strip their value tables.
-keepclassmembers enum com.shieldguard.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Android Services and BroadcastReceivers
-keep class com.shieldguard.app.service.** { *; }
-keep class com.shieldguard.app.receiver.** { *; }

# The pure decision core is invoked from services only; nothing to keep, but the reasons are
# shown verbatim in the UI so string literals must survive optimization.
-keepclassmembers class com.shieldguard.app.engine.SpamPolicy { *; }
