package com.example.condominio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
        val id: String,
        val name: String,
        val email: String,
        val appRole: String = "user",
        val status: String = "active",
        val units: List<UserUnit> = emptyList(),
        val buildingRoles: List<BuildingRole> = emptyList(),
        val currentUnit: UserUnit? = units.firstOrNull(),
        val mustChangePassword: Boolean = false
) {
        val isAdmin: Boolean get() = appRole == "admin"

        fun hasBoardRoleIn(buildingId: String): Boolean =
                buildingRoles.any { it.buildingId == buildingId && it.role == "board" }

        val apartmentUnit: String
                get() = currentUnit?.unitName ?: ""

        val buildingId: String
                get() = currentUnit?.buildingId ?: ""
}

@Serializable
data class UserUnit(
        val unitId: String,
        val buildingId: String,
        val unitName: String,
        val buildingName: String,
        val isPrimary: Boolean = false
)

@Serializable
data class BuildingRole(
        val buildingId: String,
        val role: String
)

@Serializable
data class Payment(
        val id: String,
        val amount: Double,
        val date: Long, // Cambiado de Date a Long para KMP
        val status: PaymentStatus,
        val description: String,
        val method: PaymentMethod,
        val reference: String? = null,
        val bank: String? = null,
        val phone: String? = null,
        val proofUrl: String? = null,
        val allocations: List<PaymentAllocation> = emptyList(),
        val createdAt: Long? = null,
        val processedAt: Long? = null,
        val processorName: String? = null,
        val userName: String? = null,
        /** What the resident actually transferred in Bs, and the rate/day applied; null for USD payments. */
        val amountBs: Double? = null,
        val exchangeRate: Double? = null,
        val rateDate: String? = null
)

@Serializable
data class PaymentAllocation(
        val invoiceId: String,
        val amount: Double,
        val invoicePeriod: String? = null
)

@Serializable
data class Balance(
        val unitId: String,
        val totalDebt: Double,
        val currency: String = "USD",
        val pendingInvoices: List<Invoice>
)

@Serializable
data class Invoice(
        val id: String,
        val period: String,
        val amount: Double,
        val paid: Double,
        val remaining: Double,
        val status: InvoiceStatus,
        val description: String? = null,
        val dueDate: Long? = null,
        val type: InvoiceType = InvoiceType.COMMON,
        /** Status of the latest payment reported for this invoice (set from the payments list, not the API). */
        val lastPaymentStatus: PaymentStatus? = null
)

/**
 * Applies the unit's payments to the invoice: remaining minus what payments still awaiting admin approval
 * cover (so a reported payment hides "Pay now" instead of inviting a second one), plus the latest payment status.
 */
fun Invoice.withPayments(payments: List<Payment>): Invoice {
        val mine = payments.filter { p -> p.allocations.any { it.invoiceId == id } }
        if (mine.isEmpty()) return this
        val inReview = mine.filter { it.status == PaymentStatus.PENDING }
                .sumOf { p -> p.allocations.filter { it.invoiceId == id }.sumOf { it.amount } }
        return copy(
                remaining = (remaining - inReview).coerceAtLeast(0.0),
                lastPaymentStatus = mine.maxBy { it.createdAt ?: it.date }.status
        )
}

@Serializable
enum class InvoiceStatus {
        PENDING, PARTIAL, PAID, CANCELLED, OVERDUE
}

@Serializable
enum class InvoiceType {
        COMMON, PETTY_CASH_REPLENISHMENT
}

@Serializable
data class DashboardSummary(
        val solvencyStatus: String,
        val lastPaymentDate: Long?,
        val recentTransactions: List<Payment>
)

@Serializable
enum class SolvencyStatus(val label: String) {
        SOLVENT("Al día"),
        PENDING("Pagos Pendientes")
}

@Serializable
enum class PaymentMethod(val label: String) {
        PAGO_MOVIL("Pago Móvil"),
        TRANSFER("Transferencia"),
        CASH("Efectivo")
}

@Serializable
enum class PaymentStatus {
        PENDING, APPROVED, REJECTED
}

class UserPendingException(message: String) : Exception(message)

/** users/me answered 401: the saved token is no longer valid (unlike a network or server failure). */
class SessionExpiredException : Exception("Session expired")
