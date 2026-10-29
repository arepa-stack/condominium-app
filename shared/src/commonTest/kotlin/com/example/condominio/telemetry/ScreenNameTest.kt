package com.example.condominio.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScreenNameTest {
    @Test fun `strips path argument template from route`() {
        assertEquals("payment_detail", screenName("payment_detail/{paymentId}"))
    }

    @Test fun `strips query argument template from route`() {
        assertEquals("create_payment", screenName("create_payment?invoiceId={invoiceId}"))
    }

    @Test fun `keeps plain route unchanged`() {
        assertEquals("dashboard", screenName("dashboard"))
    }

    @Test fun `returns null for null or blank route`() {
        assertNull(screenName(null))
        assertNull(screenName(""))
        assertNull(screenName("   "))
    }
}
