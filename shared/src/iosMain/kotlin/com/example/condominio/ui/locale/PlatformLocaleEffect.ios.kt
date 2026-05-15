package com.example.condominio.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

/**
 * iOS implementation: sets the preferred language via NSUserDefaults
 * so that Compose Resources resolves from the correct values-XX folder.
 */
@Composable
actual fun PlatformLocaleEffect(languageCode: String) {
    LaunchedEffect(languageCode) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
        defaults.synchronize()
    }
}
