package com.example.condominio.ui.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReceiptHtmlTest {
    @Test
    fun apiTextIsEscapedAndEveryRowPrinted() {
        val html = receiptHtml(
            header = "Torre A · Apto 23",
            title = "Aporte <script>alert(1)</script>",
            rows = listOf("Total" to "$1.30", "Estado" to "PAGADA"),
            table = listOf(listOf("Fecha", "Aplicado"), listOf("28/10/2026", "$1.30")),
            footer = "Generado"
        )
        assertFalse("<script>" in html)
        assertTrue("Aporte &lt;script&gt;" in html)
        assertTrue("<td>Total</td><td>$1.30</td>" in html)
        assertTrue("<th>Fecha</th>" in html && "<td>28/10/2026</td>" in html)
    }

    @Test
    fun headerOnlyTableIsLeftOut() {
        val html = receiptHtml("h", "t", emptyList(), table = listOf(listOf("Fecha")), footer = "f")
        assertFalse("<th>" in html)
    }
}
