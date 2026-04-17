package com.example.condominio.ui.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A sealed class representing text that can be displayed in the UI.
 * This allows ViewModels in commonMain to emit strings or resource references
 * without depending on platform-specific APIs.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    
    class StringResource(
        val res: org.jetbrains.compose.resources.StringResource,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(res, *args)
        }
    }
}
