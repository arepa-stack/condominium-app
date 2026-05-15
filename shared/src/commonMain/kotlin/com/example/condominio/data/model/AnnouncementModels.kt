package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AnnouncementCategory {
    @SerialName("INFO") INFO,
    @SerialName("URGENT") URGENT,
    @SerialName("FINANCIAL") FINANCIAL,
    @SerialName("MAINTENANCE") MAINTENANCE,
    @SerialName("NEWS") NEWS,
}

@Serializable
data class AnnouncementMetricsDto(
    @SerialName("reads_count") val readsCount: Int,
    @SerialName("reactions_count") val reactionsCount: Int,
)

@Serializable
data class AnnouncementDto(
    val id: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("author_id") val authorId: String? = null,
    val title: String,
    val content: String,
    @SerialName("content_preview") val contentPreview: String,
    val category: AnnouncementCategory,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("is_pinned") val isPinned: Boolean,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("read_by_current_user") val readByCurrentUser: Boolean,
    @SerialName("reacted_by_current_user") val reactedByCurrentUser: Boolean,
    val metrics: AnnouncementMetricsDto,
)

@Serializable
data class AnnouncementsPageDto(
    val data: List<AnnouncementDto>,
    val metadata: PaginationMetadata,
)

@Serializable
data class ReactionDto(
    @SerialName("announcement_id") val announcementId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reaction_type") val reactionType: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ToggleReactionResponseDto(
    val reacted: Boolean,
    val reaction: ReactionDto? = null,
)

@Serializable
data class SuccessDto(
    val success: Boolean,
)
