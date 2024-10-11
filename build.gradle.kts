buildscript {
    repositories {
        google()
        mavenCentral()
    }
    extra.apply {
        set("hiltVersion", "2.50")
        set("serializationVersion", "1.5.1") // Use the latest stable version

    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}")
    }
}
