import com.android.build.gradle.ProguardFiles.getDefaultProguardFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ayush.geeksforgeeks"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ayush.geeksforgeeks"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("environment")
    
    productFlavors {
        create("prod") {
            dimension = "environment"
            applicationId = "com.ayush.geeksforgeeks"
            buildConfigField("String", "BUILD_VARIANT", "\"Prod\"")
            resValue("string", "app_name", "GFG Prod")
        }
        create("dev") {
            dimension = "environment"
            applicationId = "com.ayush.geeksforgeeks.dev"
            buildConfigField("String", "BUILD_VARIANT", "\"dev\"")
            resValue("string", "app_name", "GFG GGV (Dev)")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            packaging {
                resources {
                    excludes += listOf(
                        "META-INF/*.kotlin_module",
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE",
                        "META-INF/LICENSE.txt",
                        "META-INF/license.txt",
                        "META-INF/NOTICE",
                        "META-INF/NOTICE.txt",
                        "META-INF/notice.txt",
                        "META-INF/*.version",
                        "META-INF/versions/**"
                    )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.palette.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    implementation(platform(libs.compose.bom))

    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.core)
    
    implementation(libs.bundles.compose)
    implementation(libs.coil.compose)
    implementation(libs.bundles.hilt)
    kapt(libs.hilt.compiler)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.voyager)
    implementation(libs.lottie)
    
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    
    implementation(libs.vico.compose)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.apache.poi)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.preferences.datastore)
    implementation(libs.fig)
}

tasks.register("copyProguardRules", Copy::class) {
    from("proguard-rules.pro")
    into("$buildDir/intermediates/proguard-files/")
}

tasks.register("printBuildInfo") {
    doLast {
        println("=== Build Information ===")
        android.applicationVariants.forEach { variant ->
            println("Variant: ${variant.name}")
            println("Application ID: ${variant.applicationId}")
            println("Version Code: ${variant.versionCode}")
            println("Version Name: ${variant.versionName}")
            println("----------------------")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyProguardRules")
}