package com.example.condominio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.PaymentStatus

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val date: Long, // Room stores Date as Long usually
    val status: String, // Store ENUM as String
    val description: String,
    val method: String, // Store ENUM as String
    val reference: String? = null,
    val bank: String? = null,
    val phone: String? = null,
    val proofUrl: String? = null
)

fun PaymentEntity.toDomain() = Payment(
    id = id,
    amount = amount,
    date = date,
    status = PaymentStatus.valueOf(status),
    description = description,
    method = PaymentMethod.valueOf(method),
    reference = reference,
    bank = bank,
    phone = phone,
    proofUrl = proofUrl
)

fun Payment.toEntity() = PaymentEntity(
    id = id,
    amount = amount,
    date = date,
    status = status.name,
    description = description,
    method = method.name,
    reference = reference,
    bank = bank,
    phone = phone,
    proofUrl = proofUrl
)
