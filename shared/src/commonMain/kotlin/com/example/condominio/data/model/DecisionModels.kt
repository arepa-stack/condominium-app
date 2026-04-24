package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DecisionStatus {
    @SerialName("RECEPTION") RECEPTION,
    @SerialName("VOTING") VOTING,
    @SerialName("TIEBREAK_PENDING") TIEBREAK_PENDING,
    @SerialName("RESOLVED") RESOLVED,
    @SerialName("CANCELLED") CANCELLED
}

@Serializable
enum class ResultingType {
    @SerialName("INVOICE") INVOICE,
    @SerialName("ASSESSMENT") ASSESSMENT
}

@Serializable
data class UserRefDto(
    val id: String,
    val name: String
)

@Serializable
data class DecisionDto(
    val id: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("created_by") val createdBy: UserRefDto? = null,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: DecisionStatus,
    @SerialName("current_round") val currentRound: Int,
    @SerialName("reception_deadline") val receptionDeadline: String,
    @SerialName("voting_deadline") val votingDeadline: String,
    @SerialName("tiebreak_duration_hours") val tiebreakDurationHours: Int,
    @SerialName("winner_quote_id") val winnerQuoteId: String? = null,
    @SerialName("resulting_type") val resultingType: ResultingType? = null,
    @SerialName("resulting_id") val resultingId: String? = null,
    @SerialName("finalized_at") val finalizedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancel_reason") val cancelReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("quote_count") val quoteCount: Int,
    @SerialName("is_deadline_passed") val isDeadlinePassed: Boolean
)

@Serializable
data class QuoteDto(
    val id: String,
    @SerialName("decision_id") val decisionId: String,
    val uploader: UserRefDto? = null,
    @SerialName("uploader_unit_id") val uploaderUnitId: String? = null,
    @SerialName("provider_name") val providerName: String,
    val amount: Double,
    val notes: String? = null,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deleted_by") val deletedBy: UserRefDto? = null,
    @SerialName("deletion_reason") val deletionReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class VoteDto(
    val id: String,
    @SerialName("decision_id") val decisionId: String,
    val round: Int,
    @SerialName("apartment_id") val apartmentId: String,
    @SerialName("apartment_label") val apartmentLabel: String? = null,
    @SerialName("quote_id") val quoteId: String,
    @SerialName("voted_by") val votedBy: UserRefDto? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TallyItemDto(
    @SerialName("quote_id") val quoteId: String,
    @SerialName("provider_name") val providerName: String,
    val amount: Double,
    val votes: Int,
    val pct: Double
)

@Serializable
data class TallyDto(
    val round: Int,
    val status: String,
    @SerialName("total_apartments") val totalApartments: Int,
    @SerialName("total_votes") val totalVotes: Int,
    @SerialName("participation_pct") val participationPct: Double,
    val tallies: List<TallyItemDto>,
    @SerialName("winner_quote_id") val winnerQuoteId: String? = null,
    @SerialName("is_tied") val isTied: Boolean
)

@Serializable
data class DecisionDetailDto(
    val decision: DecisionDto,
    val quotes: List<QuoteDto>,
    val tally: TallyDto,
    @SerialName("my_vote") val myVote: VoteDto? = null
)

@Serializable
data class PaginationMetadata(
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
    @SerialName("has_prev_page") val hasPrevPage: Boolean = false
)

@Serializable
data class DecisionsPageDto(
    val data: List<DecisionDto>,
    val metadata: PaginationMetadata
)

@Serializable
data class CastVoteRequest(
    @SerialName("apartment_id") val apartmentId: String,
    @SerialName("quote_id") val quoteId: String
)
