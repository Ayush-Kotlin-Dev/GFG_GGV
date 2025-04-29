import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Load signing configuration
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
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
        vectorDrawables.useSupportLibrary = true
    }

    // Signing configuration for release builds
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
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
            versionNameSuffix = "-dev"
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
            signingConfig = signingConfigs.getByName("release")
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
        // Enable core library desugaring for better backward compatibility
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    kapt {
        correctErrorTypes = true
    }
    
    // Configure bundle for Play Store
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    
    // Lint options for release builds
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += "MissingTranslation"
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

    // Updated Dagger Hilt configuration
    implementation(libs.dagger.hilt)
    implementation(libs.hilt.compose.navigation)
    kapt(libs.dagger.kapt)
    
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
    
    // Add core library desugaring for backward compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
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

