# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontobfuscate  # 禁用重命名
-dontoptimize   # 禁用优化

# 保留所有JNI相关类和方法
-keep class com.manager.ssb.util.NativeFileOperation {
    *;
}

# 保留所有native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留内部类ProgressCallback
-keep class com.manager.ssb.util.NativeFileOperation$ProgressCallback {
    *;
}

# 保留JNI接口方法
-keepclassmembers class * {
    @androidx.annotation.Keep public void onProgress(java.lang.String, long, long);
}

# 保留所有可能被JNI调用的方法
-keepclassmembers class com.manager.ssb.** {
    public *;
}
