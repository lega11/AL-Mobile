import java.util.Properties

// Reads secrets (like the Google Maps API key) from local.properties, which is not committed to Git.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Release signing details from keystore.properties (kept out of Git, like the keystore itself).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // Kotlin support is built into Android Gradle Plugin 9, so no kotlin-android plugin is needed.

    // Google services Gradle plugin (reads google-services.json for Firebase)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.deedtrack"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.deedtrack"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google Maps API key → AndroidManifest.xml (placeholder until a real key is added)
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY")?.takeIf { it.isNotBlank() } ?: "YOUR_MAPS_API_KEY"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Loads the deed photos
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Import the Firebase BoM (keeps all Firebase library versions compatible)
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))

    // Firebase products (no versions needed when using the BoM)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")       // borrower sign up / log in
    implementation("com.google.firebase:firebase-firestore")  // applications & profile data
    implementation("com.google.firebase:firebase-storage")    // uploaded documents

    // Google Maps for Jetpack Compose (Explore properties map)
    implementation("com.google.maps.android:maps-compose:8.4.0")

    // Lets Kotlin coroutines wait for Firebase tasks (.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ----- Tests -----
    // Unit tests (app/src/test): validation, drafts, status and mortgage maths
    testImplementation("junit:junit:4.13.2")

    // Compose UI tests (app/src/androidTest): run on an emulator or phone
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
