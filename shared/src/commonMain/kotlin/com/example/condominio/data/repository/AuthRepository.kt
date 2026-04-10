package com.example.condominio.data.repository

import com.example.condominio.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, unitId: String, building: String, password: String): Result<User>
    suspend fun getUnits(buildingId: String): Result<List<com.example.condominio.data.model.UnitDto>>
    suspend fun getUnit(unitId: String): Result<com.example.condominio.data.model.UnitDto>
    suspend fun logout()
    suspend fun fetchCurrentUser(): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    fun setCurrentUnit(unit: com.example.condominio.data.model.UserUnit)
}


