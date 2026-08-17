# Keep kotlinx.serialization generated serializers.
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.ledger.app.**$$serializer { *; }
-keepclassmembers class com.ledger.app.** { *** Companion; }
-keepclasseswithmembers class com.ledger.app.** { kotlinx.serialization.KSerializer serializer(...); }
