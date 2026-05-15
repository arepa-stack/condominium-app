package com.example.condominio.data.repository

import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.data.model.AnnouncementsPageDto
import com.example.condominio.data.model.ToggleReactionResponseDto
import com.example.condominio.data.remote.ApiService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AnnouncementErrorBody(
    val code: String? = null,
    val message: String? = null,
)

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun translateAnnouncementError(errorBody: String): String {
    if (errorBody.isBlank()) return errorBody
    val parsed = try {
        errorJson.decodeFromString<AnnouncementErrorBody>(errorBody)
    } catch (_: Exception) {
        null
    }
    return when (parsed?.code) {
        "ANNOUNCEMENT_NOT_FOUND" -> "El anuncio no fue encontrado."
        "FORBIDDEN" -> "No tenés acceso a este anuncio."
        else -> parsed?.message?.takeIf { it.isNotBlank() } ?: errorBody
    }
}

class RemoteAnnouncementsRepositoryImpl(
    private val apiService: ApiService,
) : AnnouncementsRepository {

    override suspend fun list(
        buildingId: String,
        category: AnnouncementCategory?,
        search: String?,
        isPinned: Boolean?,
        readStatus: String?,
        page: Int,
        limit: Int,
    ): Result<AnnouncementsPageDto> {
        return try {
            val response = apiService.listAnnouncements(
                buildingId = buildingId,
                category = category?.name,
                search = search,
                isPinned = isPinned,
                readStatus = readStatus,
                page = page,
                limit = limit,
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateAnnouncementError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun detail(id: String): Result<AnnouncementDto> {
        return try {
            val response = apiService.getAnnouncementDetail(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateAnnouncementError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markRead(id: String): Result<Unit> {
        return try {
            val response = apiService.markAnnouncementRead(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = translateAnnouncementError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReaction(id: String): Result<ToggleReactionResponseDto> {
        return try {
            val response = apiService.toggleAnnouncementReaction(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = translateAnnouncementError(response.errorBody().string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
