package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PettyCashBalanceDto(
    val id: String? = null,
    @SerialName("building_id") val buildingId: String? = null,
    @SerialName("current_balance") val currentBalance: Double,
    val currency: String = "USD",
    @SerialName("updated_at") val updatedAt: String? = null
)
