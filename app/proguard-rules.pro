-verbose
-optimizationpasses 5

-dontwarn org.conscrypt.**
-dontwarn kotlinx.serialization.**

# Keep DataStore fields
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* {
   <fields>;
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}