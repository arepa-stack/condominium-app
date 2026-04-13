package com.example.condominio.data.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        onResult(uri?.toString())
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    }
}
