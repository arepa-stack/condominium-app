package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnitDto(
    @SerialName("id") val id: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("name") val name: String,
    @SerialName("floor") val floor: String?,
    @SerialName("aliquot") val aliquot: Double?
)
