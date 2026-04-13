package com.example.condominio.data.repository

import com.example.condominio.data.model.*
import com.example.condominio.data.remote.ApiService
import com.example.condominio.ui.utils.formatDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.condominio.data.utils.PlatformFileReader


class RemotePaymentRepository(
    private val apiService: ApiService,
    private val fileReader: PlatformFileReader
) : PaymentRepository {

    override suspend fun getPayments(unitId: String?): List<Payment> {
        return try {
            val response = apiService.getPayments(unitId = unitId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error fetching payments: ${e.message}")
            emptyList()
        }
    }

    fun getPaymentsStream(): Flow<List<Payment>> = flow {
        try {
            val response = apiService.getPayments()
            if (response.isSuccessful && response.body() != null) {
                emit(response.body()!!.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            println("Error fetching payments stream: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getPayment(id: String): Payment? {
        return try {
            val response = apiService.getPayment(id)
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching payment $id: ${e.message}")
            null
        }
    }

    override suspend fun getPaymentSummary():
            Result<com.example.condominio.data.model.PaymentSummary> {
        return try {
            val response = apiService.getPaymentSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                val errorMsg = response.errorBody().string()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("Error fetching summary: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun createPayment(
            amount: Double,
            date: Long,
            description: String,
            method: PaymentMethod,
            unitId: String,
            allocations: List<com.example.condominio.data.model.PaymentAllocation>,
            reference: String?,
            bank: String?,
            phone: String?,
            proofUrl: String?,
            buildingId: String?
    ): Result<Payment> {
        return try {
            val allocationDtos =
                    allocations.map {
                        com.example.condominio.data.model.AllocationDto(
                                invoiceId = it.invoiceId,
                                amount = it.amount
                        )
                    }

            val dateStr = formatDate(date, "yyyy-MM-dd")

            val response =
                    if (!proofUrl.isNullOrEmpty()) {
                        // Multipart Flow
                        val bytes = fileReader.readBytes(proofUrl)
                        val fName = fileReader.getFileName(proofUrl)
                        println("[CREATE_PAYMENT] multipart proofUrl=$proofUrl bytes.size=${bytes?.size} fileName=$fName")

                        apiService.createPaymentMultipart(
                                amount = amount.toString(),
                                unitId = unitId,
                                buildingId = buildingId,
                                method = method.name,
                                date = dateStr,
                                notes = description,
                                reference = reference,
                                bank = bank,
                                allocations = Json.encodeToString(allocationDtos),
                                proofImage = bytes,
                                fileName = fName
                        )
                    } else {
                        // JSON Flow
                        val request =
                                com.example.condominio.data.model.CreatePaymentRequest(
                                        unitId = unitId,
                                        buildingId = buildingId,
                                        amount = amount,
                                        amountCurrency = "USD",
                                        date = dateStr,
                                        method = method.name,
                                        reference = reference,
                                        bank = bank,
                                        notes = description,
                                        allocations = allocationDtos,
                                        proofUrl = null
                                )
                        apiService.createPayment(request)
                    }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                val errorMsg = response.errorBody().string()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBalance(
            unitId: String
    ): Result<com.example.condominio.data.model.Balance> {
        return try {
            val response = apiService.getBalance(unitId)
            if (response.isSuccessful && response.body() != null) {
                // Map BalanceDto to Balance
                val dto = response.body()!!
                val balance =
                        com.example.condominio.data.model.Balance(
                                unitId = dto.unit,
                                totalDebt = dto.totalDebt,
                                pendingInvoices = dto.details.map { detail ->
                                    com.example.condominio.data.model.Invoice(
                                            id = detail.invoiceId,
                                            period = detail.period,
                                            amount = detail.amount,
                                            paid = detail.paid,
                                            remaining = detail.remaining,
                                            status = try {
                                                when (detail.status.uppercase()) {
                                                    "PENDING" -> com.example.condominio.data.model.InvoiceStatus.PENDING
                                                    "PAID" -> com.example.condominio.data.model.InvoiceStatus.PAID
                                                    "OVERDUE" -> com.example.condominio.data.model.InvoiceStatus.OVERDUE
                                                    "CANCELLED" -> com.example.condominio.data.model.InvoiceStatus.CANCELLED
                                                    else -> com.example.condominio.data.model.InvoiceStatus.PENDING
                                                }
                                            } catch (e: Exception) {
                                                com.example.condominio.data.model.InvoiceStatus.PENDING
                                            }
                                    )
                                }
                        )
                Result.success(balance)
            } else {
                val errorMsg = response.errorBody().string()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvoices(unitId: String, status: String?): Result<List<Invoice>> {
        return try {
            val response = apiService.getInvoices(unitId, status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                val errorMsg = response.errorBody().string()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvoice(id: String): Result<Invoice> {
        return try {
            val response = apiService.getInvoice(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to fetch invoice details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvoicePayments(invoiceId: String): Result<List<Payment>> {
        return try {
            val response = apiService.getInvoicePayments(invoiceId)
            if (response.isSuccessful && response.body() != null) {
                val payments =
                        response.body()!!.map { dto ->
                            val domainPayment = dto.toDomain()
                            // If the DTO has an allocated amount for this specific invoice, ensure
                            // it's in the allocations list
                            if (dto.allocatedAmount != null && dto.allocatedAmount > 0) {
                                val specificAllocation =
                                        com.example.condominio.data.model.PaymentAllocation(
                                                invoiceId = invoiceId,
                                                amount = dto.allocatedAmount
                                        )
                                // Prepend or add to existing allocations
                                domainPayment.copy(
                                        allocations =
                                                listOf(specificAllocation) +
                                                        domainPayment.allocations
                                )
                            } else {
                                domainPayment
                            }
                        }
                Result.success(payments)
            } else {
                Result.failure(Exception("Failed to fetch invoice payments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
