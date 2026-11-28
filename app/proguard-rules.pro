# kotlinx-serialization ships its own R8 rules, which cover the @Serializable classes in :shared.
# These keep the saved games readable if R8 ever renames a serialized class's companion.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# play-services-ads brings in WorkManager 2.7, built on Room 2.2. Room makes each database's
# generated _Impl class by reflection, through its no-argument constructor, but Room 2.2's own rule
# keeps only the class. R8's full mode then strips the constructor, and WorkManager's startup
# initializer crashes the release build at launch ("Failed to create an instance of WorkDatabase").
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
