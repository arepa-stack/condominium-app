package com.example.condominio.data.repository

import com.example.condominio.data.remote.rethrowIfCancellation
import com.example.condominio.data.model.RegistrationRequestBody
import com.example.condominio.data.model.RegistrationRequestResponse
import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.remote.toFailure

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
            // Typed: the form maps 409/404/400 to its own (translated) texts.
            Result.failure(r.toFailure())
        }
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        Result.failure(e)
    }
}
