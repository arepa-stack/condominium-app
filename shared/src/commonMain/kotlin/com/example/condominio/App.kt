package com.example.condominio

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.example.condominio.ui.navigation.CondominioNavGraph
import com.example.condominio.ui.theme.CondominioTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    CondominioTheme {
        CondominioNavGraph()
    }
}
