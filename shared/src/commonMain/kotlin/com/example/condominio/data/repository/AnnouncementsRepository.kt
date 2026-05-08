package com.example.condominio.data.repository

import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.data.model.AnnouncementsPageDto
import com.example.condominio.data.model.ToggleReactionResponseDto

interface AnnouncementsRepository {
    suspend fun list(
        buildingId: String,
        category: AnnouncementCategory? = null,
        search: String? = null,
        isPinned: Boolean? = null,
        readStatus: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Result<AnnouncementsPageDto>

    suspend fun detail(id: String): Result<AnnouncementDto>

    suspend fun markRead(id: String): Result<Unit>

    suspend fun toggleReaction(id: String): Result<ToggleReactionResponseDto>
}
