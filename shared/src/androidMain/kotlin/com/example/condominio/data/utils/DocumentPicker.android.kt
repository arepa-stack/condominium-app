package com.example.condominio.data.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDocumentPickerLauncher(onResult: (uri: String?, mimeType: String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            onResult(uri.toString(), mimeType)
        } else {
            onResult(null, null)
        }
    }
    return remember(launcher) {
        { launcher.launch(arrayOf("application/pdf", "image/*")) }
    }
}
