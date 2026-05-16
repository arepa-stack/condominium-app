package com.example.condominio.data.repository

import com.example.condominio.data.model.RegistrationRequestBody
import com.example.condominio.data.model.RegistrationRequestResponse
import com.example.condominio.data.remote.ApiService

interface OnboardingRepository {
    suspend fun submitRegistrationRequest(
        buildingCode: String,
        unitId: String,
        email: String,
        firstName: String,
        lastName: String,
        documentId: String,
        phone: String? = null
    ): Result<RegistrationRequestResponse>
}

class RemoteOnboardingRepository(
    private val apiService: ApiService
) : OnboardingRepository {

    override suspend fun submitRegistrationRequest(
        buildingCode: String,
        unitId: String,
        email: String,
        firstName: String,
        lastName: String,
        documentId: String,
        phone: String?
    ): Result<RegistrationRequestResponse> = try {
        val r = apiService.submitRegistrationRequest(
            RegistrationRequestBody(
                buildingCode = buildingCode,
                unitId = unitId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                documentId = documentId,
                phone = phone
            )
        )
        if (r.isSuccessful && r.body() != null) {
            Result.success(r.body()!!)
        } else {
            val msg = when (r.code) {
                409 -> "Ya existe una solicitud pendiente con este correo o la unidad alcanzó su capacidad máxima."
                404 -> "Edificio o unidad no encontrados. Verifica el código QR."
                400 -> "Datos inválidos. Revisa los campos e intenta de nuevo."
                else -> r.string().ifBlank { "Error del servidor (${r.code})" }
            }
            Result.failure(Exception(msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
