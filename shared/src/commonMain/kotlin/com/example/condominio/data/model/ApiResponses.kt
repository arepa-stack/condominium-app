package com.example.condominio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

@Serializable
data class AuthToken(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class LoginResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Int,
        val user: UserProfile
)

// Register returns just the user profile, no token
// User needs to login after registration
// Update RegisterResponse to match new backend (returns AuthSession)
@Serializable
data class RegisterResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Int,
        @SerialName("user") val user: UserProfile
)

@Serializable
data class UserProfile(
        @SerialName("id")
        val id: String? = null,
        val name: String? = null,
        val email: String? = null,
        @SerialName("app_role") val appRole: String? = null,
        val status: String? = null,
        val units: List<UserUnitDto>? = null,
        @SerialName("buildingRoles") val buildingRoles: List<BuildingRoleDto>? = null,
        val phone: String? = null,
        val settings: Map<String, JsonElement>? = null
)

@Serializable
data class UserUnitDto(
        @SerialName("unit_id") val unitId: String,
        @SerialName("building_id") val buildingId: String,
        @SerialName("is_primary") val isPrimary: Boolean,
        @SerialName("unit_name") val unitName: String? = null,
        @SerialName("building_name") val buildingName: String? = null
)

@Serializable
data class BuildingRoleDto(
        @SerialName("building_id") val buildingId: String,
        val role: String
)

@Serializable
data class BalanceDto(
        val unit: String,
        @SerialName("total_debt") val totalDebt: Double,
        @SerialName("pending_invoices") val pendingInvoicesCount: Int,
        val details: List<BalanceInvoiceDto>
)

@Serializable
data class BalanceInvoiceDto(
        @SerialName("invoice_id") val invoiceId: String,
        val amount: Double,
        val paid: Double = 0.0,
        val remaining: Double = 0.0,
        val period: String,
        val status: String
)

@Serializable
data class InvoiceDto(
        @SerialName("id") val id: String,
        val period: String,
        val amount: Double,
        @SerialName("paid_amount") val paid: Double? = null,
        val status: String,
        val tag: String? = null, // New field for v2
        val description: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        @SerialName("unit_id") val unitId: String? = null
)

fun InvoiceDto.toDomain(): Invoice {
    val invoiceStatus =
            try {
                when (status.uppercase()) {
                    "PENDING" -> InvoiceStatus.PENDING
                    "PARTIAL" -> InvoiceStatus.PARTIAL
                    "PAID" -> InvoiceStatus.PAID
                    "CANCELLED" -> InvoiceStatus.CANCELLED
                    "OVERDUE" -> InvoiceStatus.OVERDUE
                    else -> InvoiceStatus.PENDING
                }
            } catch (e: Exception) {
                InvoiceStatus.PENDING
            }

    val date =
            try {
                dueDate?.let {
                    LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                }
            } catch (e: Exception) {
                null
            }

    return Invoice(
            id = id,
            period = period,
            amount = amount,
            paid = paid ?: 0.0,
            remaining = amount - (paid ?: 0.0),
            status = invoiceStatus,
            description = description,
            dueDate = date
    )
}

@Serializable
data class PaymentDto(
        @SerialName("id") val id: String,
        @SerialName("payment_id") val paymentId: String? = null,
        val amount: Double,
        val date: String? = null, // Made nullable to handle partial responses
        val method: String? = null,
        val status: String? = null,
        val description: String? = null,
        val reference: String? = null,
        val bank: String? = null,
        @SerialName("unit_id") val unitId: String? = null,
        @SerialName("building_id") val buildingId: String? = null,
        @SerialName("proof_url") val proofUrl: String? = null,
        val allocations: List<AllocationDto>? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("processed_at") val processedAt: String? = null,
        @SerialName("processed_by") val processedBy: String? = null,
        val processor: ProcessorDto? = null,
        val notes: String? = null,
        @SerialName("allocated_amount") val allocatedAmount: Double? = null,
        @SerialName("allocation_id") val allocationId: String? = null,
        @SerialName("allocated_at") val allocatedAt: String? = null,
        val user: PaymentUserDto? = null
)

@Serializable
data class PaymentUserDto(val id: String, val name: String)

@Serializable
data class ProcessorDto(val name: String)

fun PaymentDto.toDomain(): Payment {
    val finalId = paymentId ?: id

    val paymentStatus =
            try {
                if (status != null) PaymentStatus.valueOf(status.uppercase())
                else
                        PaymentStatus
                                .APPROVED // Default to APPROVED if missing (likely approved in join
                // table)
            } catch (e: Exception) {
                PaymentStatus.PENDING
            }

    val paymentMethod =
            try {
                if (method != null) {
                    when (method.uppercase()) {
                        "TRANSFERENCIA", "TRANSFER" -> PaymentMethod.TRANSFER
                        "EFECTIVO", "CASH" -> PaymentMethod.CASH
                        else -> PaymentMethod.valueOf(method.uppercase())
                    }
                } else {
                    PaymentMethod.TRANSFER
                }
            } catch (e: Exception) {
                PaymentMethod.TRANSFER
            }

    val dateObj =
            try {
                val dateStr = date ?: createdAt // Fallback to created_at if date is missing
                dateStr?.let {
                    try {
                        LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    } catch (e: Exception) {
                        try {
                            kotlinx.datetime.Instant.parse(it).toEpochMilliseconds()
                        } catch (e2: Exception) {
                            null
                        }
                    }
                }
                        ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            } catch (e: Exception) {
                kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            }

    // Determine description: prioritize notes, then backend description (which might be periods),
    // then fallback
    val finalDescription =
            if (!notes.isNullOrEmpty()) {
                notes
            } else if (!description.isNullOrEmpty()) {
                description
            } else {
                "Pago"
            }

    return Payment(
            id = finalId,
            amount = amount,
            date = dateObj,
            status = paymentStatus,
            description = finalDescription,
            method = paymentMethod,
            reference = reference,
            bank = bank,
            proofUrl = proofUrl,
            allocations = allocations?.map { it.toDomain() } ?: emptyList(),
            userName = user?.name,
            processedAt = processedAt?.let {
                try {
                    kotlinx.datetime.Instant.parse(it).toEpochMilliseconds()
                } catch (e: Exception) {
                    null
                }
            },
            processorName = processor?.name
    )
}

@Serializable
data class AllocationDto(@SerialName("invoice_id") val invoiceId: String, val amount: Double)

fun AllocationDto.toDomain(): PaymentAllocation {
    return PaymentAllocation(invoiceId = invoiceId, amount = amount)
}

@Serializable
data class CreatePaymentRequest(
        val amount: Double,
        @SerialName("unit_id") val unitId: String,
        @SerialName("building_id") val buildingId: String? = null,
        val method: String,
        val reference: String? = null,
        val date: String,
        val allocations: List<AllocationDto>? = null,
        val notes: String? = null,
        val bank: String? = null,
        @SerialName("amount_currency") val amountCurrency: String = "USD",
        @SerialName("proof_url") val proofUrl: String? = null
)

@Serializable
data class UpdateUserRequest(
        val name: String? = null,
        val phone: String? = null,
        val unit: String? = null
)

@Serializable
data class ApiError(val message: String, val code: String? = null)
