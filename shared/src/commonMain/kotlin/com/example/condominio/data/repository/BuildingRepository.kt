package com.example.condominio.data.repository

import com.example.condominio.data.model.Building
import com.example.condominio.data.model.UnitDto
import com.example.condominio.data.remote.ApiService

interface BuildingRepository {
    suspend fun getBuildings(): Result<List<Building>>
    suspend fun getBuilding(id: String): Result<Building>
    suspend fun getBuildingByCode(code: String): Result<Building>
    suspend fun getBuildingUnitsByCode(code: String): Result<List<UnitDto>>
}

class RemoteBuildingRepository(
    private val apiService: ApiService
) : BuildingRepository {

    override suspend fun getBuildings(): Result<List<Building>> = try {
        val r = apiService.getBuildings()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Failed to fetch buildings"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getBuilding(id: String): Result<Building> = try {
        val r = apiService.getBuilding(id)
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Failed to fetch building"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getBuildingByCode(code: String): Result<Building> = try {
        val r = apiService.getBuildingByCode(code)
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Building not found for code: $code"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getBuildingUnitsByCode(code: String): Result<List<UnitDto>> = try {
        val r = apiService.getBuildingUnitsByCode(code)
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception("Failed to fetch units for code: $code"))
    } catch (e: Exception) { Result.failure(e) }
}
