import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is configured from a gitignored `keystore.properties` (copy from
// keystore.properties.example). Absent the file (e.g. CI or a dev machine without the
// upload keystore), the release signingConfig is simply not created and an unsigned
// release is produced — debug builds are unaffected.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

// Certificate pinning for the release backend, from a gitignored `cert-pins.properties` (copy
// from cert-pins.properties.example) so no real pin is hardcoded in source. Absent the file (no
// production host/cert yet, or a dev machine), the pin fields are empty and RetrofitInstance
// skips pinning entirely — debug/local builds are never pinned.
val certPinsFile = rootProject.file("cert-pins.properties")
val certPinsProperties = Properties().apply {
    if (certPinsFile.exists()) {
        load(FileInputStream(certPinsFile))
    }
}
fun certPin(key: String): String = certPinsProperties.getProperty(key, "")

// Crash reporting (Sentry) DSN, from a gitignored `sentry.properties` (copy from
// sentry.properties.example). Empty (the default without the file) means CrashReporting.init
// is a no-op — no Sentry project is provisioned for this repo yet.
val sentryPropertiesFile = rootProject.file("sentry.properties")
val sentryProperties = Properties().apply {
    if (sentryPropertiesFile.exists()) {
        load(FileInputStream(sentryPropertiesFile))
    }
}
val sentryDsn = sentryProperties.getProperty("dsn", "")

android {
    namespace = "com.adhamamr.passwordy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adhamamr.passwordy"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Defaults so BuildConfig.CERT_PIN_* exist on every variant; release overrides both.
        // Debug is never pinned (it talks to the emulator's plaintext localhost alias).
        buildConfigField("String", "CERT_PIN_PRIMARY", "\"\"")
        buildConfigField("String", "CERT_PIN_BACKUP", "\"\"")

        // Crash reporting DSN — same on every variant; CrashReporting.init still gates on
        // !DEBUG so a developer's local crashes are never reported, DSN or not.
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Emulator alias for the host's localhost; talks to a locally-run backend.
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Production backend (HTTPS). Override the placeholder before publishing.
            buildConfigField("String", "BASE_URL", "\"https://api.passwordy.example/\"")
            // SHA-256 SPKI pins (SubjectPublicKeyInfo) for the production TLS cert, base64 —
            // see cert-pins.properties.example for how to compute them. A backup pin is required
            // so the cert can be rotated without an app update becoming un-updatable. Both empty
            // (the default without cert-pins.properties) disables pinning.
            buildConfigField("String", "CERT_PIN_PRIMARY", "\"${certPin("primaryPin")}\"")
            buildConfigField("String", "CERT_PIN_BACKUP", "\"${certPin("backupPin")}\"")
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    // Process-wide lifecycle (app foreground/background) for the inactivity auto-lock.
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Retrofit (API calls)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // DataStore (Store JWT token)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security Crypto (Encrypted storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Biometric (Fingerprint authentication)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Crash/ANR reporting (Sentry). Initialized only when a DSN is configured — see
    // cert-pins.properties-style gating in CrashReporting.kt. No Sentry Gradle plugin (that's
    // for source-context/proguard-mapping upload, which needs an org/project/auth token this
    // repo doesn't have configured); the SDK alone is enough for crash/ANR capture.
    implementation("io.sentry:sentry-android:7.20.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}