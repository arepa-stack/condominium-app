package com.example.condominio.data.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberExternalViewerLauncher(): (url: String) -> Unit
