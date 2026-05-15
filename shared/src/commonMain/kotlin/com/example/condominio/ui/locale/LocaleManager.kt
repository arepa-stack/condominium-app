package com.example.condominio.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Supported languages in the application.
 * Add new entries here to extend language support.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String
) {
    SPANISH("es", "Español", "🇪🇸"),
    ENGLISH("en", "English", "🇺🇸");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SPANISH
    }
}

/**
 * Centralized locale state manager for the application.
 *
 * Defaults to Spanish ("es") regardless of system language.
 *
 * // TODO: Para respetar el idioma del sistema en el futuro, cambiar
 * //  el valor por defecto a obtener el locale del sistema.
 */
object LocaleManager {

    /**
     * The currently active language. Defaults to Spanish.
     * Changes to this value trigger recomposition in the entire app tree
     * that is wrapped by [ProvideAppLocale].
     */
    var currentLanguage by mutableStateOf(AppLanguage.SPANISH)
        private set

    /**
     * Updates the application language. Triggers recomposition and
     * applies the locale at the platform level.
     */
    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }
}

/**
 * CompositionLocal that exposes the current [AppLanguage] to the Compose tree.
 * Used internally by [ProvideAppLocale].
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.SPANISH }

/**
 * Wraps [content] with the current locale applied at both the Compose level
 * and the underlying platform. Uses [key] to force full recomposition when
 * the language changes, ensuring all [stringResource] calls pick up the new locale.
 */
@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val language = LocaleManager.currentLanguage

    // Apply locale at the platform level (Android Context / iOS NSLocale)
    PlatformLocaleEffect(language.code)

    CompositionLocalProvider(LocalAppLanguage provides language) {
        // key() forces the entire subtree to recompose when language changes,
        // which ensures stringResource() re-reads from the correct values-XX folder.
        androidx.compose.runtime.key(language) {
            content()
        }
    }
}

/**
 * Platform-specific side effect that applies the given locale code
 * to the underlying system (Android Context configuration, iOS NSLocale, etc.).
 */
@Composable
expect fun PlatformLocaleEffect(languageCode: String)
