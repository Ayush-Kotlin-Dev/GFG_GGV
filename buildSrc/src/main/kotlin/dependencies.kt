import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project


object Dependencies {
    //Kotlin stdlib
    const val composeMaterial = "androidx.compose.material3:material3:${Versions.composeMaterial3}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.compose}"
    const val composeUiGraphics = "androidx.compose.ui:ui-graphics:${Versions.compose}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val composeUiToolingPreview = "androidx.compose.ui:ui-tooling-preview:${Versions.compose}"
    const val composeRuntime = "androidx.compose.runtime:runtime:${Versions.compose}"

    //Hilt DI
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
    const val hiltAgp = "com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}"
    const val hiltCompose = "androidx.hilt:hilt-navigation-compose:1.2.0"

    // Room
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    const val roomPaging = "androidx.room:room-paging:${Versions.room}"

    // Preferences Datastore
    const val preferencesDatastore = "androidx.datastore:datastore-preferences:${Versions.preferencesDatastore}"

    //Voyager Navigator & Tab
    const val voyagerNavigator = "cafe.adriel.voyager:voyager-navigator:${Versions.voyagerVersion}"
    const val voyagerAnimation =  "cafe.adriel.voyager:voyager-transitions:${Versions.voyagerVersion}"
    const val voyagerTab =   "cafe.adriel.voyager:voyager-tab-navigator:${Versions.voyagerVersion}"

    //Lottie
    const val lottie = "com.airbnb.android:lottie-compose:${Versions.lottie}"

    //Firebase
    const val firebaseAuth = "com.google.firebase:firebase-auth-ktx:${Versions.firebaseAuth}"
    const val gmsPlayServices = "com.google.android.gms:play-services-auth:${Versions.firebaseGmsPlayServices}"
    const val firebaseAuthKtx = "com.google.firebase:firebase-auth-ktx:23.0.0"
    const val firebaseBom = "com.google.firebase:firebase-bom:33.4.0"
    const val fireStore = "com.google.firebase:firebase-firestore-ktx:25.1.0"
    const val firebaseStorage = "com.google.firebase:firebase-storage-ktx:21.0.1"




}

fun DependencyHandler.compose() {
    implementation(Dependencies.composeUi)
    implementation(Dependencies.composeRuntime)
    implementation(Dependencies.composeUiGraphics)
    implementation(Dependencies.composeUiTooling)
    implementation(Dependencies.composeMaterial)
    debugImplementation(Dependencies.composeUiToolingPreview)
}

fun DependencyHandler.hilt() {
    implementation(Dependencies.hiltAndroid)
    implementation(Dependencies.hiltCompose)
    kapt(Dependencies.hiltCompiler)
}


fun DependencyHandler.coil() {
    implementation("io.coil-kt:coil-compose:${Versions.coil}")
}

fun DependencyHandler.ktor() {
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-client-logging:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}")
}

fun DependencyHandler.serialization() {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.serialization}")
}

fun DependencyHandler.BottomNavigation() {

}

fun DependencyHandler.preferenceDataStore() {
    implementation(Dependencies.preferencesDatastore)
}

fun DependencyHandler.voyagerNavigator() {
    implementation(Dependencies.voyagerNavigator)
    implementation(Dependencies.voyagerAnimation)
    implementation(Dependencies.voyagerTab)
}

fun DependencyHandler.lottie() {
    implementation(Dependencies.lottie)
}

fun DependencyHandler.firebase() {
    implementation(Dependencies.firebaseAuth)
    implementation(Dependencies.gmsPlayServices)
    implementation(Dependencies.firebaseAuthKtx)
    implementation(platform(Dependencies.firebaseBom))
    implementation(Dependencies.fireStore)
    implementation(Dependencies.firebaseStorage)

}



fun DependencyHandler.data() {
    implementation(project(":data"))
}

fun DependencyHandler.domain() {
    implementation(project(":domain"))
}

fun DependencyHandler.room() {
    implementation(Dependencies.roomRuntime)
    implementation(Dependencies.roomKtx)
    kapt(Dependencies.roomCompiler)
    implementation(Dependencies.roomPaging)
}