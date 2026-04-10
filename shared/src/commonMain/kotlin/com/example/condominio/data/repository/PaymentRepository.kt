package com.example.condominio.data.repository

import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.data.model.PaymentSummary
import com.example.condominio.data.model.SolvencyStatus
import kotlinx.coroutines.delay

interface PaymentRepository {
    suspend fun getPayments(unitId: String? = null): List<Payment>
    suspend fun getPayment(id: String): Payment?
// New method
    suspend fun getPaymentSummary(): Result<PaymentSummary>

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
        buildingId: String? = null
    ): Result<Payment>

    suspend fun getBalance(unitId: String): Result<com.example.condominio.data.model.Balance>
    
    suspend fun getInvoices(unitId: String, status: String? = null): Result<List<com.example.condominio.data.model.Invoice>>
    
    suspend fun getInvoice(id: String): Result<com.example.condominio.data.model.Invoice>
    
    suspend fun getInvoicePayments(invoiceId: String): Result<List<Payment>>
}


