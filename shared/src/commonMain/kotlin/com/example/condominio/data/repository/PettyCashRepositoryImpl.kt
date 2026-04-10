package com.example.condominio.data.repository

import com.example.condominio.data.model.*
import com.example.condominio.data.remote.ApiService

import com.example.condominio.data.utils.PlatformFileReader

class PettyCashRepositoryImpl (
    private val apiService: ApiService,
    private val fileReader: PlatformFileReader
) : PettyCashRepository {

    override suspend fun getBalance(buildingId: String): Result<PettyCashBalanceDto> {
        return try {
            val response = apiService.getPettyCashBalance(buildingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get balance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(
        buildingId: String,
        type: PettyCashTransactionType?,
        category: PettyCashCategory?,
        page: Int,
        limit: Int
    ): Result<List<PettyCashTransactionDto>> {
        return try {
            val response = apiService.getPettyCashHistory(
                buildingId = buildingId,
                type = type?.name,
                category = category?.name,
                page = page,
                limit = limit
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerIncome(
        buildingId: String,
        amount: Double,
        description: String
    ): Result<PettyCashTransactionDto> {
        return try {
            val request = RegisterIncomeRequest(buildingId, amount, description)
            val response = apiService.registerPettyCashIncome(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to register income"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerExpense(
        buildingId: String,
        amount: Double,
        description: String,
        category: PettyCashCategory,
        evidencePath: String?
    ): Result<PettyCashTransactionDto> {
        return try {
            val evidenceBytes = evidencePath?.let { fileReader.readBytes(it) }
            val evidenceName = evidencePath?.let { fileReader.getFileName(it) }

            val response = apiService.registerPettyCashExpense(
                buildingId,
                amount.toString(),
                description,
                category.name,
                evidenceBytes,
                evidenceName
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to register expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
