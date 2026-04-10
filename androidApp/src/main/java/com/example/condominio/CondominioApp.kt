package com.example.condominio

import android.app.Application
import com.example.condominio.di.appModule
import com.example.condominio.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CondominioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@CondominioApp)
            modules(appModule, networkModule)
        }
    }
}
