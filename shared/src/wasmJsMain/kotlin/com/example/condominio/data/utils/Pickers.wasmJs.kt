package com.example.condominio.data.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window

@Composable
actual fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return remember { { pickFile("image/*") { url, _ -> callback.value(url) } } }
}

@Composable
actual fun rememberDocumentPickerLauncher(onResult: (uri: String?, mimeType: String?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return remember { { pickFile("image/*,application/pdf") { url, mime -> callback.value(url, mime) } } }
}

@Composable
actual fun rememberExternalViewerLauncher(): (url: String) -> Unit =
    remember { { url -> window.open(url, "_blank") } }
