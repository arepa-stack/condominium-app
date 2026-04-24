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
        val currentUnit: UserUnit? = units.firstOrNull()
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
        val userName: String? = null
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
        val type: InvoiceType = InvoiceType.COMMON
)

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
