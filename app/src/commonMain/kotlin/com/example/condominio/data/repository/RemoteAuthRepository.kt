package com.example.condominio.data.repository

import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.model.*
import com.example.condominio.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class RemoteAuthRepository
(private val apiService: ApiService, private val tokenManager: TokenManager) :
        AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    init {
        // Load user on init if token exists
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val token = tokenManager.getToken()
            if (token != null) {
                // Token exists, fetch current user
                fetchCurrentUser()
            }
        }
    }

    override fun setCurrentUnit(unit: com.example.condominio.data.model.UserUnit) {
        _currentUser.value = _currentUser.value?.copy(currentUnit = unit)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
                println("RemoteAuthRepo: Attempting login for: $email")
            val response = apiService.login(mapOf("email" to email, "password" to password))

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                println("RemoteAuthRepo: Login successful, token: ${loginResponse.accessToken.take(20)}...")
                tokenManager.saveToken(loginResponse.accessToken)

                var user = loginResponse.user.toDomain()
                user = enrichUserWithUnits(user)
                _currentUser.value = user

                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                if (response.code == 401 && errorBody.contains("pending", ignoreCase = true)) {
                    println("RemoteAuthRepo: User account is pending approval")
                    Result.failure(
                            com.example.condominio.data.model.UserPendingException(
                                    "Your account is pending approval"
                            )
                    )
                } else {
                    val errorMsg =
                            "Login failed (${response.code}): ${if (errorBody.isNotEmpty()) errorBody else "Unknown error"}"
                    println("RemoteAuthRepo: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("RemoteAuthRepo: Exception during login")
            Result.failure(Exception("Login error: ${e.message}"))
        }
    }

    override suspend fun register(
            name: String,
            email: String,
            unitId: String,
            building: String,
            password: String
    ): Result<User> {
        return try {
            val response =
                    apiService.register(
                            mapOf(
                                    "name" to name,
                                    "email" to email,
                                    "unit_id" to unitId,
                                    "building_id" to building,
                                    "password" to password
                            )
                    )

            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                tokenManager.saveToken(registerResponse.accessToken)

                var user = registerResponse.user.toDomain()
                user = enrichUserWithUnits(user)
                _currentUser.value = user

                Result.success(user)
            } else {
                val errorMsg =
                        "Registration failed: ${response.errorBody()?.string() ?: "Unknown error"}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val response = apiService.getCurrentUser()

            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                var user = profile.toDomain()
                user = enrichUserWithUnits(user)

                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnits(
            buildingId: String
    ): Result<List<com.example.condominio.data.model.UnitDto>> {
        return try {
            val response = apiService.getBuildingUnits(buildingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch units for building $buildingId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnit(
            unitId: String
    ): Result<com.example.condominio.data.model.UnitDto> {
        return try {
            val response = apiService.getUnitDetails(unitId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch unit details for $unitId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        _currentUser.value = null
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val request =
                    com.example.condominio.data.model.UpdateUserRequest(
                            name = user.name
                            // phone = ... if available in User model
                            )
            val response = apiService.updateUser(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(
            currentPassword: String,
            newPassword: String
    ): Result<Unit> {
        // changePassword NOT IMPLEMENTED in ApiService yet
        return Result.failure(Exception("Change password not implemented in backend API yet"))
    }

    private suspend fun enrichUserWithUnits(user: User): User {
        // Start with existing units from the User object (mapped from Profile)
        var currentUnits = user.units

        // If no units found, fallback to fetching from API
        if (currentUnits.isEmpty()) {
            val unitsResponse =
                    try {
                        apiService.getUserUnits(user.id)
                    } catch (e: Exception) {
                        null
                    }

            if (unitsResponse?.isSuccessful == true && unitsResponse.body() != null) {
                currentUnits =
                        unitsResponse.body()!!.map { dto ->
                            com.example.condominio.data.model.UserUnit(
                                    unitId = dto.unitId,
                                    buildingId = dto.buildingId,
                                    unitName = "", // Needs enrichment
                                    buildingName = "", // Needs enrichment
                                    isPrimary = dto.isPrimary
                            )
                        }
            }
        }

        // Now enrich names
        val enrichedUnits = mutableListOf<com.example.condominio.data.model.UserUnit>()
        for (unit in currentUnits) {
            // If we already have names (unlikely from plain DTO), skip
            var uName = unit.unitName
            var bName = unit.buildingName
            var bId = unit.buildingId

            if (uName.isEmpty() || bName.isEmpty()) {
                // Fetch unit details
                val unitDetailsResp =
                        try {
                            apiService.getUnitDetails(unit.unitId)
                        } catch (e: Exception) {
                            null
                        }
                if (unitDetailsResp?.body() != null) {
                    val det = unitDetailsResp.body()!!
                    uName = det.name
                    bId = det.buildingId // Confirm building ID
                } else {
                    uName = "Unit ${unit.unitId.take(4)}"
                }

                // Fetch building name
                val buildingResp =
                        try {
                            apiService.getBuilding(bId)
                        } catch (e: Exception) {
                            null
                        }
                bName = buildingResp?.body()?.name ?: "Building"
            }

            enrichedUnits.add(unit.copy(unitName = uName, buildingName = bName, buildingId = bId))
        }

        // Determine current unit
        val current = _currentUser.value?.currentUnit
        val newCurrent =
                if (current != null && enrichedUnits.any { it.unitId == current.unitId }) {
                    enrichedUnits.find { it.unitId == current.unitId }
                } else {
                    enrichedUnits.find { it.isPrimary } ?: enrichedUnits.firstOrNull()
                }

        // If still no units, try legacy enrichment (for backward compatibility or odd data shapes)
        if (enrichedUnits.isEmpty()) {
            return enrichUserWithLegacyUnit(user)
        }

        return user.copy(units = enrichedUnits, currentUnit = newCurrent)
    }

    private suspend fun enrichUserWithLegacyUnit(user: User): User {
        // Minimal fallback logic, kept simple
        return user
    }
}

// Extension function to convert API UserProfile to domain User
private fun UserProfile.toDomain(): User {
    // 1. Map new 'units' list if available
    val domainUnits =
            units?.map { dto ->
                com.example.condominio.data.model.UserUnit(
                        unitId = dto.unitId,
                        buildingId = dto.buildingId,
                        unitName = "", // Placeholder, will be enriched
                        buildingName = "", // Placeholder
                        isPrimary = dto.isPrimary
                )
            }
                    ?: emptyList()

    val domainBuildingRoles =
            buildingRoles?.map { dto ->
                com.example.condominio.data.model.BuildingRole(
                        buildingId = dto.buildingId,
                        role = dto.role
                )
            }
                    ?: emptyList()

    // 2. Legacy fallback logic (try to extract from 'unit' field if list is empty)
    var finalUnits = domainUnits
    if (finalUnits.isEmpty()) {
        // Attempt to extract unit ID and name from legacy 'unit' field
        var unitId = ""
        var unitName = this.unitName ?: ""

        if (unit != null) {
            if (unit is JsonPrimitive) {
                val s = unit.content
                // If it looks like UUID, it's ID. Else name.
                if (s.length == 36 && s.contains("-")) unitId = s else unitName = s
            } else if (unit is JsonObject) {
                unitId = unit["id"]?.jsonPrimitive?.content ?: ""
                unitName = unit["name"]?.jsonPrimitive?.content ?: unitName
            }
        }

        if (unitId.isNotEmpty()) {
            val legacyUnit =
                    com.example.condominio.data.model.UserUnit(
                            unitId = unitId,
                            buildingId = buildingId ?: "",
                            unitName = unitName.ifEmpty { "Unit" },
                            buildingName = buildingName ?: "Building",
                            isPrimary = true
                    )
            finalUnits = listOf(legacyUnit)
        }
    }

    return User(
            id = id ?: "",
            name = name ?: "Unknown",
            email = email ?: "",
            role = role ?: "resident", // Map role from profile
            status = status ?: "active", // Map status
            units = finalUnits,
            buildingRoles = domainBuildingRoles,
            currentUnit = finalUnits.firstOrNull(),
            profileBuildingId = buildingId
    )
}
