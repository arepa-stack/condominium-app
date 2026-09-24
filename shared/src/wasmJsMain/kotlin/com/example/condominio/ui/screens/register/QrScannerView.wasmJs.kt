package com.example.condominio.ui.screens.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ponytail: no in-browser scanner. Users type the code, or scan a `https://<app>/?code=...` QR
// with the phone camera (handled at startup in main.kt). Add zxing-js if that falls short.
@Composable
actual fun QrScannerView(
    modifier: Modifier,
    onQrScanned: (String) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Escáner no disponible en la web.\nIngresa el código manualmente.", color = Color.White, textAlign = TextAlign.Center)
    }
}
