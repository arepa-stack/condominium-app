import java.util.Properties

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // lifecycle runtime & viewmodel are provided transitively by Compose Multiplatform
            // (org.jetbrains.androidx.lifecycle). Adding them directly causes KLIB duplicate warnings.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.mikepenz.markdown.m3)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp.logging.interceptor)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }


}

android {
    namespace = "com.example.condominio"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.srcDirs("src/main/res")
            // No need to manually add java dirs here for KMP
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.androidx.ui.tooling)
}


// API endpoint per environment, read from local.properties (not versioned):
//   API_ENV=DEV|PROD (default DEV; override with -PapiEnv=PROD), DEV_API_URL / PROD_API_URL, ENABLE_NETWORK_LOGS.
// Generates com.example.condominio.ApiConfig for commonMain, so Android and web hit the same backend.
// Release builds must use -PapiEnv=PROD (or API_ENV=PROD) to point at production.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiEnv = ((findProperty("apiEnv") as String?) ?: localProps.getProperty("API_ENV") ?: "DEV").uppercase()
val apiBaseUrl = localProps.getProperty("${apiEnv}_API_URL") ?: "https://condominium-api.nibs-tech.com/"
val networkLogs = localProps.getProperty("ENABLE_NETWORK_LOGS")?.toBoolean() ?: false

val generateApiConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/apiConfig")
    inputs.property("env", apiEnv)
    inputs.property("url", apiBaseUrl)
    inputs.property("logs", networkLogs)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/example/condominio/ApiConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.example.condominio
            |
            |// Generated from local.properties by :shared:generateApiConfig. Do not edit.
            |object ApiConfig {
            |    const val ENV = "$apiEnv"
            |    const val BASE_URL = "$apiBaseUrl"
            |    const val NETWORK_LOGS = $networkLogs
            |}
            |""".trimMargin()
        )
    }
}
kotlin.sourceSets.commonMain { kotlin.srcDir(generateApiConfig) }
