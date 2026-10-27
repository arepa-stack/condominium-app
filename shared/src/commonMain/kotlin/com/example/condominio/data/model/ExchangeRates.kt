package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET api/v1/app/exchange-rates (same endpoint the admin uses). Only the BCV dollar rate is read. */
@Serializable
data class RateSetDto(
    @SerialName("rate_date") val rateDate: String? = null,
    val rates: RatesDto = RatesDto()
)

@Serializable
data class RatesDto(
    /** BCV official dollar rate. */
    @SerialName("dolar_oficial") val dolarOficial: ExchangeRateDto? = null
)

@Serializable
data class ExchangeRateDto(
    @SerialName("rate_date") val rateDate: String,
    @SerialName("bs_per_unit") val bsPerUnit: Double
)
