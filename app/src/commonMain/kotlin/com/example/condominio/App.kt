package com.example.condominio

import androidx.compose.runtime.Composable
import com.example.condominio.ui.navigation.CondominioNavGraph
import com.example.condominio.ui.theme.CondominioTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    CondominioTheme {
        CondominioNavGraph()
    }
}
