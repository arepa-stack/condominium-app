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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.AptoOnSurface
import com.example.condominio.ui.theme.AptoOnSurfaceVariant
import com.example.condominio.ui.utils.rememberReducedMotion
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val ViewfinderSize = 280.dp
private val WideViewfinderSize = 220.dp
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

    if (isWide()) {
        WideQrScannerContent(onBack = onBack, onCode = handleScan)
        return
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
                    Text("✕", color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Light))
                }
                Text(
                    text = "Apto",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.qr_scan_subtitle),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
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
                            text = stringResource(Res.string.qr_scan_scanning).uppercase(),
                            color = Color.White.copy(alpha = 0.2f),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp)
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
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = "Apto v1.0 • Sistema de Gestión Residencial",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
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

/**
 * Wide (tablet / desktop web) layout: light page with title, a smaller viewfinder and the
 * manual code entry always visible (no dialog). Enter submits.
 */
@Composable
private fun WideQrScannerContent(
    onBack: () -> Unit,
    onCode: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val submit = { if (code.isNotBlank()) onCode(code.trim()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenBackground(Color.Black)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding(), vertical = 24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AptoOnSurface)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.qr_scan_title),
                color = AptoOnSurface,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.qr_scan_subtitle),
                color = AptoOnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))

            // Viewfinder (camera on tablets, "not available" notice on web)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(WideViewfinderSize)
                    .clip(ViewfinderShape)
                    .background(Color.Black)
            ) {
                QrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onQrScanned = onCode
                )
                ScanLine(travel = WideViewfinderSize)
                ScannerCorners()
            }
            Spacer(Modifier.height(32.dp))

            // Manual entry, always visible
            Text(
                text = stringResource(Res.string.qr_manual_dialog_title),
                color = AptoOnSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    placeholder = { Text(stringResource(Res.string.qr_manual_dialog_hint)) },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                submit()
                                true
                            } else {
                                false
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                Button(
                    onClick = submit,
                    enabled = code.isNotBlank(),
                    modifier = Modifier
                        .height(56.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(Res.string.accept), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ScanLine(travel: Dp = ViewfinderSize) {
    // Reduced motion: the line rests in the middle of the viewfinder.
    val offsetFraction = if (rememberReducedMotion()) 0.5f else {
        val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
        infiniteTransition.animateFloat(
            initialValue = 0.10f,
            targetValue = 0.90f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanLineOffset"
        ).value
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopStart)
                .offset(y = travel * offsetFraction)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun ScannerCorners() {
    val cornerColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left
        Box(
            modifier = Modifier
                .size(CornerSize)
                .align(Alignment.TopStart)
                .border(
                    width = CornerThickness,
                    color = cornerColor,
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
                    color = cornerColor,
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
                    color = cornerColor,
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
                    color = cornerColor,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.accept), color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
