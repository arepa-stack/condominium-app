package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberExternalViewerLauncher(): (url: String) -> Unit {
    return { _ -> }
}
