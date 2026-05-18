package com.example.condominio.ui.screens.register

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.theme.BrandOrange
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val ScannerCornerColor = BrandOrange
private val ViewfinderSize = 280.dp
private val CornerSize = 40.dp
private val CornerThickness = 4.dp
private val ViewfinderShape = RoundedCornerShape(24.dp)

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var alreadyScanned by remember { mutableStateOf(false) }

    val handleScan: (String) -> Unit = { rawValue ->
        if (!alreadyScanned) {
            val code = extractBuildingCode(rawValue)
            if (code.isNotBlank()) {
                alreadyScanned = true
                onScanned(code)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "Apto",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            // Viewfinder area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.qr_scan_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.qr_scan_subtitle),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Viewfinder box
                Box(
                    modifier = Modifier
                        .size(ViewfinderSize)
                        .clip(ViewfinderShape)
                ) {
                    // Camera feed
                    QrScannerView(
                        modifier = Modifier.fillMaxSize(),
                        onQrScanned = handleScan
                    )

                    // Animated scan line
                    ScanLine()

                    // Corner decorations
                    ScannerCorners()

                    // Scanning label
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.qr_scan_scanning),
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }

            // Bottom footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = stringResource(Res.string.qr_enter_manually),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Apto v1.0 • Sistema de Gestión Residencial",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }

    // Manual code dialog
    if (showManualDialog) {
        ManualCodeDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { code ->
                showManualDialog = false
                if (code.isNotBlank()) handleScan(code.trim())
            }
        )
    }
}

@Composable
private fun ScanLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val offsetFraction by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopStart)
                .offset(y = ViewfinderSize * offsetFraction)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BrandOrange,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun ScannerCorners() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left
        Box(
            modifier = Modifier
                .size(CornerSize)
                .align(Alignment.TopStart)
                .border(
                    width = CornerThickness,
                    color = ScannerCornerColor,
                    shape = RoundedCornerShape(topStart = 12.dp)
                )
        )
        // Top-right
        Box(
            modifier = Modifier
                .size(CornerSize)
                .align(Alignment.TopEnd)
                .border(
                    width = CornerThickness,
                    color = ScannerCornerColor,
                    shape = RoundedCornerShape(topEnd = 12.dp)
                )
        )
        // Bottom-left
        Box(
            modifier = Modifier
                .size(CornerSize)
                .align(Alignment.BottomStart)
                .border(
                    width = CornerThickness,
                    color = ScannerCornerColor,
                    shape = RoundedCornerShape(bottomStart = 12.dp)
                )
        )
        // Bottom-right
        Box(
            modifier = Modifier
                .size(CornerSize)
                .align(Alignment.BottomEnd)
                .border(
                    width = CornerThickness,
                    color = ScannerCornerColor,
                    shape = RoundedCornerShape(bottomEnd = 12.dp)
                )
        )
    }
}

/**
 * Extracts the building code from a QR value which may be:
 * - A full URL: https://...?code=COND-XXXX  → returns "COND-XXXX"
 * - A plain code: COND-XXXX                 → returns "COND-XXXX"
 */
fun extractBuildingCode(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.contains("code=") -> trimmed.substringAfter("code=").substringBefore("&").trim()
        else -> trimmed
    }
}

@Composable
private fun ManualCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.qr_manual_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text(stringResource(Res.string.qr_manual_dialog_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Text(stringResource(Res.string.accept), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
