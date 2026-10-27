package com.example.condominio.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatBsTest {
    @Test
    fun groupsThousandsWithDotsAndRoundsToCents() {
        assertEquals("0,00", formatBs(0.0))
        assertEquals("216,37", formatBs(216.37))
        assertEquals("9.901,09", formatBs(45.76 * 216.37)) // 9901.0912
        assertEquals("1.234.567,89", formatBs(1234567.891))
        assertEquals("1.000,00", formatBs(999.999))
    }
}
