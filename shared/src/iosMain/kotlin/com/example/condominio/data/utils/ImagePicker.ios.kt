package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit {
    return { onResult(null) }
}
