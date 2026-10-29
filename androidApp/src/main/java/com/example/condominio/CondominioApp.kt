package com.example.condominio

import android.app.Application
import com.example.condominio.di.appModule
import com.example.condominio.di.sharedModule
import com.example.condominio.telemetry.NoTelemetry
import com.example.condominio.telemetry.Telemetry
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

class CondominioApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val telemetryModule = module {
            single<Telemetry> {
                val ctx = this@CondominioApp
                if (FirebaseApp.getApps(ctx).isEmpty()) NoTelemetry else FirebaseTelemetry(ctx)
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@CondominioApp)
            modules(sharedModule, appModule, telemetryModule)
        }
    }
}
