plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "1.9.22"
    kotlin("kapt")
    id("com.google.gms.google-services")
}
android {
    namespace = "com.ayush.geeksforgeeks"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ayush.geeksforgeeks"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlinOptions {
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
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

}

tasks.register("copyProguardRules", Copy::class) {
    from("proguard-rules.pro")
    into("$buildDir/intermediates/proguard-files/")
}

tasks.named("preBuild") {
    dependsOn("copyProguardRules")
}