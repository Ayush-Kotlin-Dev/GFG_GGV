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

# Keep Serializable classes and their members
-keep class com.ayush.data.datastore.UserSettings { *; }
-keep class com.ayush.data.datastore.User { *; }
-keep class com.ayush.data.datastore.UserRole { *; }

# Keep Serialization-related stuff
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Firestore field names
-keepclassmembers class com.ayush.data.datastore.** {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.Exclude *;
}

# Keep JvmField annotations
-keepclassmembers class com.ayush.data.datastore.** {
    @kotlin.jvm.JvmField *;
}

# Suppress warnings
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn kotlinx.serialization.**