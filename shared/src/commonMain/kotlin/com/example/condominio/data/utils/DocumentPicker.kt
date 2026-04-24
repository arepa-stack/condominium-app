package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberDocumentPickerLauncher(onResult: (uri: String?, mimeType: String?) -> Unit): () -> Unit
