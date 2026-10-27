package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET api/v1/app/buildings/{id}/payment-info: where residents pay, set per building from the admin panel. */
@Serializable
data class BuildingPaymentInfoDto(
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_code") val bankCode: String? = null,
    @SerialName("account_holder") val accountHolder: String? = null,
    val rif: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("pago_movil_phone") val pagoMovilPhone: String? = null,
    val email: String? = null,
    /** Signed URL of the Pago Móvil QR image (expires in an hour). */
    @SerialName("qr_url") val qrUrl: String? = null,
    val configured: Boolean = false
)
