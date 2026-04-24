package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDocumentPickerLauncher(onResult: (uri: String?, mimeType: String?) -> Unit): () -> Unit {
    return { onResult(null, null) }
}
