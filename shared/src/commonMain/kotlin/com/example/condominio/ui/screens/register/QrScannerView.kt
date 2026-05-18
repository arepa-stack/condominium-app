package com.example.condominio.ui.screens.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrScannerView(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
)
