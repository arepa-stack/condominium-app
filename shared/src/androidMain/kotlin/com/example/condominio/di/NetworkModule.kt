package com.example.condominio.di

import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.remote.ApiServiceImpl
import com.example.condominio.data.remote.AuthInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    singleOf(::AuthInterceptor)
    
    single {
        val authInterceptor = get<AuthInterceptor>()
        val loggingInterceptor = get<HttpLoggingInterceptor>()
        
        HttpClient(OkHttp) {
            engine {
                addInterceptor(authInterceptor)
                addInterceptor(loggingInterceptor)
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
            defaultRequest {
                url("https://condominium-api.nibs-tech.com/")
            }
        }
    }
    
    singleOf(::ApiServiceImpl) { bind<ApiService>() }
}
