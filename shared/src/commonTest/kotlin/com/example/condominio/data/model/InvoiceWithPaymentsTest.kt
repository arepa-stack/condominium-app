package com.example.condominio.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvoiceWithPaymentsTest {
    private val invoice = Invoice(id = "sep", period = "2026-09", amount = 50.0, paid = 0.0, remaining = 50.0, status = InvoiceStatus.PENDING)

    private fun payment(status: PaymentStatus, createdAt: Long, invoiceId: String = "sep", amount: Double = 50.0) = Payment(
        id = "p$createdAt", amount = amount, date = 0, status = status, description = "", method = PaymentMethod.PAGO_MOVIL,
        allocations = listOf(PaymentAllocation(invoiceId, amount)), createdAt = createdAt
    )

    @Test
    fun paymentInReviewCoversTheInvoice() {
        val result = invoice.withPayments(listOf(payment(PaymentStatus.PENDING, 1)))
        assertEquals(0.0, result.remaining)
        assertEquals(PaymentStatus.PENDING, result.lastPaymentStatus)
    }

    @Test
    fun rejectedPaymentLeavesItPayable() {
        val result = invoice.withPayments(listOf(payment(PaymentStatus.REJECTED, 2)))
        assertEquals(50.0, result.remaining)
        assertEquals(PaymentStatus.REJECTED, result.lastPaymentStatus)
    }

    @Test
    fun latestPaymentWinsAndPartialReviewOnlyCoversItsPart() {
        val result = invoice.withPayments(listOf(payment(PaymentStatus.REJECTED, 1), payment(PaymentStatus.PENDING, 2, amount = 20.0)))
        assertEquals(30.0, result.remaining)
        assertEquals(PaymentStatus.PENDING, result.lastPaymentStatus)
    }

    @Test
    fun otherInvoicesPaymentsAreIgnored() {
        val result = invoice.withPayments(listOf(payment(PaymentStatus.PENDING, 1, invoiceId = "oct")))
        assertEquals(50.0, result.remaining)
        assertNull(result.lastPaymentStatus)
    }
}
