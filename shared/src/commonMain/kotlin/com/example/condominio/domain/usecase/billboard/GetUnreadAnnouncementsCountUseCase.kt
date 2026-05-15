package com.example.condominio.domain.usecase.billboard

import com.example.condominio.data.repository.AnnouncementsRepository

class GetUnreadAnnouncementsCountUseCase(
    private val repository: AnnouncementsRepository,
) {
    suspend operator fun invoke(buildingId: String): Int {
        return repository
            .list(
                buildingId = buildingId,
                readStatus = "unread",
                limit = 1,
            )
            .getOrNull()
            ?.metadata
            ?.total
            ?: 0
    }
}
