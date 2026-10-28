package com.example.condominio.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvoiceTitleTest {
    @Test
    fun keepsOnlyTheConcept() {
        assertEquals(
            "Cuota Especial: Reparaciones Mayores y Pintura",
            invoiceTitle("Cuota Especial: Reparaciones Mayores y Pintura - Base: \$3100.00 (Alícuota: 0.4567%) | Pintura fachada")
        )
        assertEquals("Agua", invoiceTitle("Agua | Pagado 10/10"))
        assertEquals("Cuota de mantenimiento", invoiceTitle("  Cuota de mantenimiento  "))
    }

    @Test
    fun nullWhenEmpty() {
        assertNull(invoiceTitle(null))
        assertNull(invoiceTitle("   "))
    }
}
