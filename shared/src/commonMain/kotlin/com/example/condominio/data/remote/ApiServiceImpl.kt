package com.example.condominio.data.remote

import com.example.condominio.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.serialization.json.JsonElement

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
        client.get("api/v1/app/users/me")
    }

    override suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile> = safeRequest {
        client.patch("api/v1/app/users/me") {
            contentType(ContentType.Application.Json)
            setBody(updates)
        }
    }

    override suspend fun getBuildings(): Response<List<Building>> = safeRequest {
        client.get("api/v1/app/buildings")
    }

    override suspend fun getBuilding(id: String): Response<Building> = safeRequest {
        client.get("api/v1/app/buildings/$id")
    }

    override suspend fun getBuildingUnits(id: String): Response<List<UnitDto>> = safeRequest {
        client.get("api/v1/app/buildings/$id/units")
    }

    override suspend fun getUnitDetails(id: String): Response<UnitDto> = safeRequest {
        client.get("api/v1/app/buildings/units/$id")
    }

    override suspend fun getPayments(unitId: String?, year: Int?): Response<List<PaymentDto>> = safeRequest {
        client.get("api/v1/app/payments") {
            unitId?.let { parameter("unit_id", it) }
            year?.let { parameter("year", it) }
        }
    }

    override suspend fun getPayment(id: String): Response<PaymentDto> = safeRequest {
        client.get("api/v1/app/payments/$id")
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
        val basicParts = formData {
            append("amount", amount)
            append("unit_id", unitId)
            append("method", method)
            append("date", date)
            if (buildingId != null) append("building_id", buildingId)
            if (notes != null) append("notes", notes)
            if (reference != null) append("reference", reference)
            if (bank != null) append("bank", bank)
            if (allocations != null) append("allocations", allocations)
        }

        val allParts = if (proofImage != null && fileName != null) {
            basicParts + PartData.FileItem(
                provider = { ByteReadPacket(proofImage) },
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"proof_image\"; filename=\"${fileName}\""
                    )
                }
            )
        } else {
            basicParts
        }

        client.post("api/v1/app/payments") {
            setBody(MultiPartFormDataContent(allParts))
        }
    }

    override suspend fun createPayment(request: CreatePaymentRequest): Response<PaymentDto> = safeRequest {
        client.post("api/v1/app/payments") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getBalance(unitId: String): Response<BalanceDto> = safeRequest {
        client.get("api/v1/app/billing/units/$unitId/balance")
    }

    override suspend fun getInvoices(unitId: String, status: String?): Response<List<InvoiceDto>> = safeRequest {
        client.get("api/v1/app/billing/units/$unitId/invoices") {
            status?.let { parameter("status", it) }
        }
    }

    override suspend fun getInvoice(id: String): Response<InvoiceDto> = safeRequest {
        client.get("api/v1/app/billing/invoices/$id")
    }

    override suspend fun getInvoicePayments(id: String): Response<List<PaymentDto>> = safeRequest {
        client.get("api/v1/app/billing/invoices/$id/payments")
    }

    override suspend fun getCredits(unitId: String): Response<JsonElement> = safeRequest {
        client.get("api/v1/app/billing/units/$unitId/credit")
    }

    override suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto> = safeRequest {
        client.get("api/v1/app/petty-cash/funds/$buildingId")
    }

    override suspend fun listDecisions(
        buildingId: String?,
        status: String?,
        search: String?,
        page: Int?,
        limit: Int?
    ): Response<DecisionsPageDto> = safeRequest {
        client.get("api/v1/app/decisions/decisions") {
            buildingId?.let { parameter("building_id", it) }
            status?.let { parameter("status", it) }
            search?.let { parameter("search", it) }
            page?.let { parameter("page", it) }
            limit?.let { parameter("limit", it) }
        }
    }

    override suspend fun getDecisionDetail(id: String): Response<DecisionDetailDto> = safeRequest {
        client.get("api/v1/app/decisions/decisions/$id")
    }

    override suspend fun uploadDecisionQuote(
        decisionId: String,
        unitId: String,
        providerName: String,
        amount: String,
        notes: String?,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Response<QuoteDto> = safeRequest {
        val parts = formData {
            append("unit_id", unitId)
            append("provider_name", providerName)
            append("amount", amount)
            if (notes != null) append("notes", notes)
            append(
                "file",
                fileBytes,
                Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, mimeType)
                }
            )
        }
        client.post("api/v1/app/decisions/decisions/$decisionId/quotes") {
            setBody(MultiPartFormDataContent(parts))
        }
    }

    override suspend fun deleteDecisionQuote(decisionId: String, quoteId: String): Response<QuoteDto> = safeRequest {
        client.delete("api/v1/app/decisions/decisions/$decisionId/quotes/$quoteId")
    }

    override suspend fun castVote(decisionId: String, apartmentId: String, quoteId: String): Response<VoteDto> = safeRequest {
        client.post("api/v1/app/decisions/decisions/$decisionId/votes") {
            contentType(ContentType.Application.Json)
            setBody(CastVoteRequest(apartmentId = apartmentId, quoteId = quoteId))
        }
    }

    override suspend fun getDecisionResults(id: String, round: Int?): Response<TallyDto> = safeRequest {
        client.get("api/v1/app/decisions/decisions/$id/results") {
            round?.let { parameter("round", it) }
        }
    }
}
