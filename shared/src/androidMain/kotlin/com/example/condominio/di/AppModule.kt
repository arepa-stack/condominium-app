package com.example.condominio.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.condominio.ApiConfig
import com.example.condominio.data.local.DataStoreTokenManager
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.utils.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/** Android bindings for [sharedModule]. */
val appModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { androidContext().filesDir.resolve("auth_prefs.preferences_pb").absolutePath.toPath() }
        )
    }
    single<TokenManager> { DataStoreTokenManager(get()) }
    single<PlatformFileReader> { AndroidFileReader(androidContext()) }
    single<PdfService> { PdfServiceImpl(get()) }

    single<HttpClientEngine> {
        OkHttp.create {
            if (ApiConfig.NETWORK_LOGS) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
    }
}
