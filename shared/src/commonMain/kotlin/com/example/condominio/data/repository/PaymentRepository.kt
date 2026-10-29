package com.example.condominio.data.repository

import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.data.model.withPayments
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface PaymentRepository {
    suspend fun getPayments(unitId: String? = null): List<Payment>
    suspend fun getPayment(id: String): Payment?

    /** Like [getPayments]/[getPayment] but keeps the failure, so screens can show an error instead of "empty". */
    suspend fun fetchPayments(unitId: String? = null): Result<List<Payment>>
    suspend fun fetchPayment(id: String): Result<Payment>

    suspend fun createPayment(
        amount: Double, 
        date: Long, 
        description: String,
        method: PaymentMethod,
        unitId: String,
        allocations: List<com.example.condominio.data.model.PaymentAllocation> = emptyList(),
        reference: String? = null,
        bank: String? = null,
        phone: String? = null,
        proofUrl: String? = null,
        buildingId: String? = null,
        /** Bs actually transferred (Pago Móvil / transfer): registered as a VES payment so the rate is kept. */
        amountBs: Double? = null
    ): Result<Payment>

    suspend fun getBalance(unitId: String): Result<com.example.condominio.data.model.Balance>
    
    suspend fun getInvoices(unitId: String, status: String? = null): Result<List<com.example.condominio.data.model.Invoice>>
    
    suspend fun getInvoice(id: String): Result<com.example.condominio.data.model.Invoice>
    
    suspend fun getInvoicePayments(invoiceId: String): Result<List<Payment>>

    /** Today's BCV dollar rate (Bs per USD): condominium fees are paid in Bs at this rate. */
    suspend fun getBcvRate(date: Long? = null): Result<com.example.condominio.data.model.ExchangeRateDto>

    /** Bank account, Pago Móvil and QR of the building, as set in the admin panel. */
    suspend fun getPaymentInfo(buildingId: String): Result<com.example.condominio.data.model.BuildingPaymentInfoDto>
}


/**
 * Applies each unpaid invoice's own payments ([Invoice.withPayments]). Per invoice because the unit's payment
 * list comes without allocations; one failed request just leaves that invoice as the API sent it.
 */
// ponytail: one request per unpaid invoice (usually a handful); a payments list with allocations would make it one.
suspend fun PaymentRepository.applyInvoicePayments(invoices: List<Invoice>): List<Invoice> = coroutineScope {
    invoices.map { invoice ->
        async {
            if (invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.CANCELLED) invoice
            else invoice.withPayments(getInvoicePayments(invoice.id).getOrDefault(emptyList()))
        }
    }.awaitAll()
}
