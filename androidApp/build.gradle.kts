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

android {
    namespace = "com.example.condominio.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nibs.aptocondominios"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
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
    // Core Android app dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    // Splash screen
    implementation(libs.core.splashscreen)

    // Google AdMob
    implementation(libs.play.services.ads)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
