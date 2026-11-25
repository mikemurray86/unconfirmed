# kotlinx-serialization ships its own R8 rules, which cover the @Serializable classes in :shared.
# These keep the saved games readable if R8 ever renames a serialized class's companion.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
