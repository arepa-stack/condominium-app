plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.condominio.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.condominio"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
