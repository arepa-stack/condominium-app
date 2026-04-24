package com.example.condominio.data.repository

import com.example.condominio.data.model.DecisionDetailDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.model.DecisionsPageDto
import com.example.condominio.data.model.QuoteDto
import com.example.condominio.data.model.TallyDto
import com.example.condominio.data.model.VoteDto
import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.utils.PlatformFileReader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DecisionErrorBody(
    val code: String? = null,
    val message: String? = null,
    val error: ErrorEnvelope? = null
) {
    @Serializable
    data class ErrorEnvelope(val code: String? = null, val message: String? = null)
}

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun translateDecisionsError(errorBody: String): String {
    if (errorBody.isBlank()) return errorBody
    val parsed = try {
        errorJson.decodeFromString<DecisionErrorBody>(errorBody)
    } catch (_: Exception) {
        null
    }
    val code = parsed?.code ?: parsed?.error?.code
    val message = parsed?.message ?: parsed?.error?.message

    return when (code) {
        "QUOTE_DELETED" -> "El presupuesto fue eliminado."
        "QUOTE_NOT_IN_TIEBREAK" -> "Ese presupuesto no participa en la ronda de desempate."
        else -> message?.takeIf { it.isNotBlank() } ?: errorBody
    }
}

class RemoteDecisionsRepositoryImpl(
    private val apiService: ApiService,
    private val fileReader: PlatformFileReader
) : DecisionsRepository {

    override suspend fun list(
        buildingId: String,
        status: DecisionStatus?,
        search: String?,
        page: Int,
        limit: Int
    ): Result<DecisionsPageDto> {
        return try {
            val response = apiService.listDecisions(
                buildingId = buildingId,
                status = status?.name,
                search = search,
                page = page,
                limit = limit
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun detail(id: String): Result<DecisionDetailDto> {
        return try {
            val response = apiService.getDecisionDetail(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadQuote(
        decisionId: String,
        unitId: String,
        providerName: String,
        amount: Double,
        notes: String?,
        fileUri: String,
        mimeType: String
    ): Result<QuoteDto> {
        return try {
            val bytes = fileReader.readBytes(fileUri)
            if (bytes == null) {
                return Result.failure(Exception("No se pudo leer el archivo"))
            }
            val fileName = fileReader.getFileName(fileUri)
            val response = apiService.uploadDecisionQuote(
                decisionId = decisionId,
                unitId = unitId,
                providerName = providerName,
                amount = amount.toString(),
                notes = notes,
                fileBytes = bytes,
                fileName = fileName,
                mimeType = mimeType
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteQuote(decisionId: String, quoteId: String): Result<QuoteDto> {
        return try {
            val response = apiService.deleteDecisionQuote(decisionId, quoteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun vote(decisionId: String, unitId: String, quoteId: String): Result<VoteDto> {
        return try {
            val response = apiService.castVote(
                decisionId = decisionId,
                apartmentId = unitId,
                quoteId = quoteId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun results(decisionId: String, round: Int?): Result<TallyDto> {
        return try {
            val response = apiService.getDecisionResults(decisionId, round)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateDecisionsError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
