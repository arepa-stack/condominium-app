package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit
