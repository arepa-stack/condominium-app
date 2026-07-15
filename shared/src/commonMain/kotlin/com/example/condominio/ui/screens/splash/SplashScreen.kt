package com.example.condominio.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.theme.BrandDark
import com.example.condominio.ui.theme.BrandOrange
import com.example.condominio.ui.theme.SubtitleGray
import com.example.condominio.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        // Radial gradient background accent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandOrange.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Apto",
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "Apto",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandDark,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "VIVIENDA ELEVADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SubtitleGray,
                letterSpacing = 3.sp
            )
        }

        // Bottom footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LoadingBar()
            Text(
                text = "Apto by Nibs",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SubtitleGray,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun LoadingBar() {
    val totalWidth = 140.dp
    val barWidth = 42.dp

    val infiniteTransition = rememberInfiniteTransition(label = "loadingBar")
    val offsetFraction by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingBarOffset"
    )

    Box(
        modifier = Modifier
            .width(totalWidth)
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.Black.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight()
                .offset(x = totalWidth * offsetFraction)
                .clip(RoundedCornerShape(99.dp))
                .background(BrandOrange)
        )
    }
}
