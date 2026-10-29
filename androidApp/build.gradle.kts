import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore/keystore.properties")
    if (file.exists()) load(file.inputStream())
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiEnv = ((findProperty("apiEnv") as String?) ?: localProps.getProperty("API_ENV") ?: System.getenv("API_ENV") ?: "DEV").uppercase()

// Firebase per environment (androidApp/firebase/<env>/google-services.json): copied to androidApp/google-services.json
// where the plugin reads it. Without the file, plugins are skipped, any stale copy is removed, and the app uses NoTelemetry.
val envGoogleServices = file("firebase/${apiEnv.lowercase()}/google-services.json")
val activeGoogleServices = file("google-services.json")
if (envGoogleServices.exists()) {
    envGoogleServices.copyTo(activeGoogleServices, overwrite = true)
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else {
    activeGoogleServices.delete()
}

android {
    namespace = "com.example.condominio.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nibs.aptocondominios"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        manifestPlaceholders["telemetryEnabled"] = "true"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProperties["storeFile"] as? String ?: "keystore/apto-release.jks")
            storePassword = keystoreProperties["storePassword"] as? String
            keyAlias = keystoreProperties["keyAlias"] as? String
            keyPassword = keystoreProperties["keyPassword"] as? String
        }
    }

    buildTypes {
        getByName("debug") {
            // Debug builds only send telemetry when targeting DEV; debug against PROD stays silent.
            manifestPlaceholders["telemetryEnabled"] = (apiEnv == "DEV").toString()
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Removed deprecated kotlinOptions
}

dependencies {
    implementation(project(":shared"))
    
    // Core Android app dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    // Splash screen
    implementation(libs.core.splashscreen)

    // Firebase (Analytics + Crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
