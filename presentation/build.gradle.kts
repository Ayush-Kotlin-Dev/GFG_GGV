import com.android.build.gradle.ProguardFiles.getDefaultProguardFile

//This is build.gradle.kts for presentation Module
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "1.9.22"
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "3.0.3"
}
android {
    namespace = "com.ayush.geeksforgeeks"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ayush.geeksforgeeks"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions("environment")
    
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
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlinOptions {
        jvmTarget = "18"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.palette:palette-ktx:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    val coroutinesVersion = "1.8.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    compose()
    coil()
    hilt()
    serialization()
    voyagerNavigator()
    lottie()
    data()
    firebase()
    implementation("com.patrykandpatrick.vico:compose:1.6.5")
    implementation ("org.apache.poi:poi-ooxml:5.2.4")
    implementation ("org.apache.poi:poi:5.2.4")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("com.github.theapache64:fig:0.0.3")
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