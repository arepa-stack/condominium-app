package com.example.condominio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class BrandPanelContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val l1 = maxOf(a.luminance(), b.luminance())
        val l2 = minOf(a.luminance(), b.luminance())
        return (l1 + 0.05f) / (l2 + 0.05f)
    }

    @Test fun `white text keeps 4_5 to 1 on every stop of the brand panel gradient`() {
        BrandPanelGradient.forEach { stop ->
            val ratio = contrast(BrandPanelTextColor, stop)
            assertTrue(ratio >= 4.5f, "contrast $ratio on $stop")
        }
    }
}
