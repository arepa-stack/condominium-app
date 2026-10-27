package com.example.condominio.ui.screens.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplashTimingTest {
    @Test fun `web never waits on the splash`() {
        assertEquals(0L, splashDelayMillis(isWeb = true, reducedMotion = false))
    }

    @Test fun `reduced motion skips the wait`() {
        assertEquals(0L, splashDelayMillis(isWeb = false, reducedMotion = true))
    }

    @Test fun `phones keep a short brand beat well under the old 2400ms`() {
        val ms = splashDelayMillis(isWeb = false, reducedMotion = false)
        assertTrue(ms in 1..1000, "was $ms")
    }
}
