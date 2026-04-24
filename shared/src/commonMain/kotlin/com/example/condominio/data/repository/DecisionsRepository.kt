package com.example.condominio.data.repository

import com.example.condominio.data.model.DecisionDetailDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.model.DecisionsPageDto
import com.example.condominio.data.model.QuoteDto
import com.example.condominio.data.model.TallyDto
import com.example.condominio.data.model.VoteDto

interface DecisionsRepository {
    suspend fun list(
        buildingId: String,
        status: DecisionStatus? = null,
        search: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<DecisionsPageDto>

    suspend fun detail(id: String): Result<DecisionDetailDto>

    suspend fun uploadQuote(
        decisionId: String,
        unitId: String,
        providerName: String,
        amount: Double,
        notes: String?,
        fileUri: String,
        mimeType: String
    ): Result<QuoteDto>

    suspend fun deleteQuote(decisionId: String, quoteId: String): Result<QuoteDto>

    suspend fun vote(decisionId: String, unitId: String, quoteId: String): Result<VoteDto>

    suspend fun results(decisionId: String, round: Int? = null): Result<TallyDto>
}
