package com.example.condominio.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

// Compose resources read the locale from navigator.language(s); override both so
// stringResource() follows the in-app language picker instead of the browser.
private fun overrideBrowserLanguage(code: String) {
    js("""
        document.documentElement.lang = code;
        Object.defineProperty(navigator, 'language', { get: () => code, configurable: true });
        Object.defineProperty(navigator, 'languages', { get: () => [code], configurable: true });
    """)
}

@Composable
actual fun PlatformLocaleEffect(languageCode: String) {
    // SideEffect (not LaunchedEffect) so it applies before the keyed subtree reads strings.
    SideEffect { overrideBrowserLanguage(languageCode) }
}

@Composable
actual fun ProvidePlatformLocale(languageCode: String, content: @Composable () -> Unit) = content()
