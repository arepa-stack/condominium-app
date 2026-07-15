package com.example.condominio

import android.app.Application
import com.example.condominio.di.appModule
import com.example.condominio.di.networkModule
import com.google.android.gms.ads.MobileAds
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import kotlin.concurrent.thread

class CondominioApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@CondominioApp)
            modules(appModule, networkModule)
        }

        // AdMob SDK init fuera del hilo principal (recomendación de Google)
        thread { MobileAds.initialize(this) }
    }
}
