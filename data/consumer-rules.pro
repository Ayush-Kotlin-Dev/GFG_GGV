# Keep model classes
-keep class com.ayush.data.datastore.UserSettings { *; }
-keep class com.ayush.data.datastore.User { *; }
-keep class com.ayush.data.datastore.UserRole { *; }
-keep class com.ayush.data.model.CreditLog { *; }
-keep class com.ayush.data.model.Event { *; }
-keep class com.ayush.data.model.Task { *; }
-keep class com.ayush.data.model.TaskStatus { *; }

# Keep Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializable class members
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JvmField annotations
-keepclassmembers class com.ayush.data.datastore.** {
    @kotlin.jvm.JvmField *;
}