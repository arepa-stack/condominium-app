package com.example.condominio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.example.condominio.ui.locale.ProvideAppLocale
import com.example.condominio.ui.navigation.CondominioNavGraph
import com.example.condominio.ui.theme.CondominioTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * @param registerCode building code from a deep link; opens registration after the splash.
 * @param onNavHostReady lets the web target bind the NavController to browser history.
 */
@Composable
@Preview
fun App(
    registerCode: String? = null,
    onNavHostReady: suspend (NavHostController) -> Unit = {}
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    val navController = rememberNavController()
    LaunchedEffect(navController) { onNavHostReady(navController) }

    // ProvideAppLocale forces the app locale (Spanish by default)
    // and triggers recomposition when the language is changed from ProfileScreen.
    ProvideAppLocale {
        CondominioTheme {
            // Phone-first layout: cap the width on desktop browsers/tablets.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                    CondominioNavGraph(navController = navController, registerCode = registerCode)
                }
            }
        }
    }
}
