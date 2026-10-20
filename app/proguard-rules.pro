# Keep line numbers for readable Play Console crash stack traces; hide source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Release builds carry no debug/verbose/info logging. Warnings and errors stay for crash reports.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
