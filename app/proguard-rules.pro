# Keep line numbers for readable Play Console crash stack traces; hide source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Release builds carry no debug/verbose/info logging. Warnings and errors stay for crash reports.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ML Kit pulls in WorkManager 2.7 with Room 2.2.5, which creates WorkDatabase_Impl by reflection
# (Class.forName(...).newInstance()). Room 2.2.5's own rule keeps the class but not its constructor, and
# R8 full mode drops it, so every process start crashed in WorkManagerInitializer (1.2).
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# ML Kit finds its components (CommonComponentRegistrar, DigitalInkRecognitionRegistrar) through manifest
# metadata and creates them by reflection. Without their constructors the handwriting model download fails.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
