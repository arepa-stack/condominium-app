package com.example.condominio.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android implementation: updates the [android.content.Context] configuration
 * to use the requested locale. This makes [stringResource()] from
 * Compose Resources resolve from the correct values-XX folder.
 */
@Composable
actual fun PlatformLocaleEffect(languageCode: String) {
    val context = LocalContext.current

    LaunchedEffect(languageCode) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
