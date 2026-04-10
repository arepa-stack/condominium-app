package com.example.condominio.data.remote

import com.example.condominio.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    
    private suspend inline fun <reified T> safeRequest(block: () -> io.ktor.client.statement.HttpResponse): Response<T> {
        return try {
            val response = block()
            if (response.status.isSuccess()) {
                Response.success(response.body<T>())
            } else {
                Response.error(response.status.value, "Error from server: ${response.status}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Response.error(500, e.message ?: "Unknown error")
        }
    }

    override suspend fun register(request: Map<String, String>): Response<RegisterResponse> = safeRequest {
        client.post("auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun login(credentials: Map<String, String>): Response<LoginResponse> = safeRequest {
        client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }
    }

    override suspend fun getCurrentUser(): Response<UserProfile> = safeRequest {
        client.get("users/me")
    }

    override suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile> = safeRequest {
        client.patch("users/me") {
            contentType(ContentType.Application.Json)
            setBody(updates)
        }
    }

    override suspend fun getUserUnits(id: String): Response<List<UserUnitDto>> = safeRequest {
        client.get("users/$id/units")
    }

    override suspend fun getBuildings(): Response<List<Building>> = safeRequest {
        client.get("buildings")
    }

    override suspend fun getBuilding(id: String): Response<Building> = safeRequest {
        client.get("buildings/$id")
    }

    override suspend fun getBuildingUnits(id: String): Response<List<UnitDto>> = safeRequest {
        client.get("buildings/$id/units")
    }

    override suspend fun getUnitDetails(id: String): Response<UnitDto> = safeRequest {
        client.get("buildings/units/$id")
    }

    override suspend fun getPaymentSummary(): Response<PaymentSummaryDto> = safeRequest {
        client.get("payments/summary")
    }

    override suspend fun getPayments(unitId: String?, year: Int?): Response<List<PaymentDto>> = safeRequest {
        client.get("payments") {
            unitId?.let { parameter("unit_id", it) }
            year?.let { parameter("year", it) }
        }
    }

    override suspend fun getPayment(id: String): Response<PaymentDto> = safeRequest {
        client.get("payments/$id")
    }

    override suspend fun createPaymentMultipart(
        amount: String,
        unitId: String,
        buildingId: String?,
        method: String,
        date: String,
        notes: String?,
        reference: String?,
        bank: String?,
        allocations: String?,
        proofImage: ByteArray?,
        fileName: String?
    ): Response<PaymentDto> = safeRequest {
        client.post("payments") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("amount", amount)
                        append("unit_id", unitId)
                        append("method", method)
                        append("date", date)
                        if (buildingId != null) append("building_id", buildingId)
                        if (notes != null) append("notes", notes)
                        if (reference != null) append("reference", reference)
                        if (bank != null) append("bank", bank)
                        if (allocations != null) append("allocations", allocations)
                        
                        if (proofImage != null && fileName != null) {
                            append("proof_image", proofImage, io.ktor.http.Headers.build {
                                append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                            })
                        }
                    }
                )
            )
        }
    }

    override suspend fun createPayment(request: CreatePaymentRequest): Response<PaymentDto> = safeRequest {
        client.post("payments") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getBalance(unitId: String): Response<BalanceDto> = safeRequest {
        client.get("billing/units/$unitId/balance")
    }

    override suspend fun getInvoices(unitId: String, status: String?): Response<List<InvoiceDto>> = safeRequest {
        client.get("billing/units/$unitId/invoices") {
            status?.let { parameter("status", it) }
        }
    }

    override suspend fun getInvoice(id: String): Response<InvoiceDto> = safeRequest {
        client.get("billing/invoices/$id")
    }

    override suspend fun getInvoicePayments(id: String): Response<List<PaymentDto>> = safeRequest {
        client.get("billing/invoices/$id/payments")
    }

    override suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto> = safeRequest {
        client.get("petty-cash/balance/$buildingId")
    }

    override suspend fun getPettyCashHistory(
        buildingId: String,
        type: String?,
        category: String?,
        page: Int,
        limit: Int
    ): Response<List<PettyCashTransactionDto>> = safeRequest {
        client.get("petty-cash/history/$buildingId") {
            type?.let { parameter("type", it) }
            category?.let { parameter("category", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    override suspend fun registerPettyCashIncome(request: RegisterIncomeRequest): Response<PettyCashTransactionDto> = safeRequest {
        client.post("petty-cash/income") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun registerPettyCashExpense(
        buildingId: String,
        amount: String,
        description: String,
        category: String,
        evidenceImage: ByteArray?,
        fileName: String?
    ): Response<PettyCashTransactionDto> = safeRequest {
        client.post("petty-cash/expense") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("building_id", buildingId)
                        append("amount", amount)
                        append("description", description)
                        append("category", category)
                        if (evidenceImage != null && fileName != null) {
                            append("evidence_image", evidenceImage, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                            })
                        }
                    }
                )
            )
        }
    }
}
