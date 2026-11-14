plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.dlebox.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dlebox.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REPOSITORY_URL", "\"https://github.com/adritankian-bit/dlebox-android\"")
        buildConfigField("String", "CATALOG_URL", "\"https://raw.githubusercontent.com/adritankian-bit/dlebox-android/main/catalog/games.json\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets["main"].assets.srcDir("../catalog")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    val signingFile = System.getenv("DLEBOX_KEYSTORE")
    if (!signingFile.isNullOrBlank()) {
        signingConfigs.create("stable") {
            storeFile = file(signingFile)
            storePassword = System.getenv("DLEBOX_STORE_PASSWORD")
            keyAlias = System.getenv("DLEBOX_KEY_ALIAS")
            keyPassword = System.getenv("DLEBOX_KEY_PASSWORD")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("stable")
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.browser:browser:1.8.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
