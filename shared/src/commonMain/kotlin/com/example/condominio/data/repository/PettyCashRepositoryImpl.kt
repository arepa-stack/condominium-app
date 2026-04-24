package com.example.condominio.data.repository

import com.example.condominio.data.model.PettyCashBalanceDto
import com.example.condominio.data.remote.ApiService

class PettyCashRepositoryImpl(
    private val apiService: ApiService
) : PettyCashRepository {

    override suspend fun getBalance(buildingId: String): Result<PettyCashBalanceDto> {
        return try {
            val response = apiService.getPettyCashBalance(buildingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody().string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
