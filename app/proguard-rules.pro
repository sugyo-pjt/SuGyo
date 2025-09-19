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

# WebView JavaScript Interface 규칙
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ThreeDHandCanvas WebView 관련 클래스 보호
-keep class com.ssafy.a602.web.** { *; }
-keep class com.ssafy.a602.chatbot.HandFrame3D { *; }
-keep class com.ssafy.a602.chatbot.Pt3D { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile