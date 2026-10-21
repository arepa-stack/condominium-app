package com.example.condominio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
    strokeWidth: Dp = 4.dp,
    fullScreen: Boolean = true,
) {
    val baseModifier = if (fullScreen) Modifier.fillMaxSize() else Modifier.wrapContentSize()
    val loadingLabel = stringResource(Res.string.loading)
    Box(
        modifier = baseModifier.then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = color,
            // Announced by screen readers ("Cargando…") instead of a silent spinner.
            modifier = Modifier.size(size).semantics {
                contentDescription = loadingLabel
                liveRegion = LiveRegionMode.Polite
            },
            strokeWidth = strokeWidth,
        )
    }
}
