package com.example.condominio.data.repository

import com.example.condominio.data.model.Building
import com.example.condominio.data.remote.ApiService

interface BuildingRepository {
    suspend fun getBuildings(): Result<List<Building>>
    suspend fun getBuilding(id: String): Result<Building>
}

class RemoteBuildingRepository (
    private val apiService: ApiService
) : BuildingRepository {
    
    override suspend fun getBuildings(): Result<List<Building>> {
        return try {
            val response = apiService.getBuildings()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch buildings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBuilding(id: String): Result<Building> {
        return try {
            val response = apiService.getBuilding(id)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch building"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
