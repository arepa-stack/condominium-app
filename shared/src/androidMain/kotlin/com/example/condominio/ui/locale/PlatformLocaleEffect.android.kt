package com.example.condominio.ui.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android 13+: per-app language (the system treats the app as [languageCode]; survives configuration
 * changes and external activities, which otherwise reset the process locale to the system language).
 * Changing it recreates the activity once. Older versions only get the process defaults.
 */
private fun applyAppLocale(context: Context, languageCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val manager = context.getSystemService(android.app.LocaleManager::class.java)
        if (manager.applicationLocales.isEmpty || manager.applicationLocales[0].language != languageCode) {
            manager.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    }
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    LocaleList.setDefault(LocaleList(locale))
    val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

@Composable
actual fun PlatformLocaleEffect(languageCode: String) {
    val context = LocalContext.current
    // During composition (not in an effect) so it is in place before the content composes.
    remember(languageCode) { applyAppLocale(context, languageCode) }
}

/** Re-provides LocalConfiguration with the app locale for anything that reads it directly. */
@Composable
actual fun ProvidePlatformLocale(languageCode: String, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val configuration = remember(base, languageCode) {
        Configuration(base).apply { setLocale(Locale(languageCode)) }
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
