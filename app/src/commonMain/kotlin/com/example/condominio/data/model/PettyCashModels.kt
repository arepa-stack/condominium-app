package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PettyCashTransactionType {
    @SerialName("INCOME") INCOME,
    @SerialName("EXPENSE") EXPENSE
}

@Serializable
enum class PettyCashCategory(val displayName: String) {
    @SerialName("REPAIR") REPAIR("Reparación"),
    @SerialName("CLEANING") CLEANING("Limpieza"),
    @SerialName("EMERGENCY")
    EMERGENCY("Emergencia"),
    @SerialName("OFFICE") OFFICE("Oficina"),
    @SerialName("UTILITIES")
    UTILITIES("Servicios"),
    @SerialName("OTHER") OTHER("Otro")
}

@Serializable
data class PettyCashBalanceDto(
        @SerialName("current_balance") val currentBalance: Double,
        val currency: String,
        @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PettyCashTransactionDto(
        val id: String,
        val type: PettyCashTransactionType,
        val amount: Double,
        val description: String,
        val category: PettyCashCategory,
        @SerialName("evidence_url") val evidenceUrl: String? = null,
        @SerialName("created_at") val createdAt: String
)

@Serializable
data class RegisterIncomeRequest(
        @SerialName("building_id") val buildingId: String,
        val amount: Double,
        val description: String
)
