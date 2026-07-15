package com.example.condominio

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * Muestra un único anuncio "App Open" por sesión (proceso), al iniciar la app.
 * Es el formato menos intrusivo de anuncio a pantalla completa: el usuario lo
 * ve una sola vez al abrir y puede cerrarlo de inmediato.
 */
object AppOpenAdManager {

    // IMPORTANTE: este es el ad unit de PRUEBA de Google para App Open.
    // Reemplazar por tu ad unit real de AdMob (crear bloque de anuncios tipo
    // "Anuncio de apertura de aplicación"), formato ca-app-pub-XXXX/YYYY
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    private var shownThisSession = false

    fun loadAndShowOnce(activity: Activity) {
        if (shownThisSession) return
        AppOpenAd.load(
            activity,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (shownThisSession || activity.isFinishing || activity.isDestroyed) return
                    shownThisSession = true
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Sin anuncio esta sesión (sin red, sin inventario, etc.). No reintentar.
                }
            }
        )
    }
}
