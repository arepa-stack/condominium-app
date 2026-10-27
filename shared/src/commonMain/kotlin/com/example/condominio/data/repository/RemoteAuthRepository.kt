package com.example.condominio.data.repository

import com.example.condominio.data.remote.rethrowIfCancellation
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.model.*
import com.example.condominio.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

/** How long a successful users/me answer is reused by [RemoteAuthRepository.fetchCurrentUser]. */
private val UserCacheWindow = 10.seconds

class RemoteAuthRepository
(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val timeSource: kotlin.time.TimeSource = kotlin.time.TimeSource.Monotonic
) :
        AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    /**
     * When users/me last answered OK. Screens opened right after the startup check (dashboard, history…)
     * call [fetchCurrentUser] again; within [UserCacheWindow] they reuse the user instead of asking again (A-17).
     * Any change to the session or the profile resets it.
     */
    private var lastFetch: TimeMark? = null

    // No fetch on init: the login screen's startup check is the one that loads the user (A-17).

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
                    .copy(mustChangePassword = loginResponse.mustChangePassword ?: false)

                // First login: skip unit enrichment; user must change password before using the app.
                if (!user.mustChangePassword) {
                    user = enrichUserWithUnits(user)
                }
                _currentUser.value = user
                lastFetch = null

                Result.success(user)
            } else {
                val errorBody = response.errorBody().string()
                if (response.code == 401 && errorBody.contains("pending", ignoreCase = true)) {
                    println("RemoteAuthRepo: User account is pending approval")
                    Result.failure(
                            com.example.condominio.data.model.UserPendingException(
                                    "Your account is pending approval"
                            )
                    )
                } else {
                    println("RemoteAuthRepo: Login failed (${response.code})")
                    Result.failure(response.toFailure())
                }
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            println("RemoteAuthRepo: Exception during login")
            Result.failure(e)
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
                lastFetch = null

                Result.success(user)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val response = apiService.resetPassword(email)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun changePasswordFirstLogin(newPassword: String): Result<Unit> {
        return try {
            val response = apiService.changePasswordFirstLogin(newPassword)
            if (response.isSuccessful && response.body() != null) {
                // Backend returns a FRESH session; the old (temp) token is now invalid.
                tokenManager.saveToken(response.body()!!.accessToken)
                loadCurrentUser(force = true) // repopulate currentUser with must_change_password cleared
                Result.success(Unit)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun fetchCurrentUser(): Result<User> = loadCurrentUser(force = false)

    private suspend fun loadCurrentUser(force: Boolean): Result<User> {
        if (!force) {
            val cached = _currentUser.value
            val mark = lastFetch
            if (cached != null && mark != null && mark.elapsedNow() < UserCacheWindow) return Result.success(cached)
        }
        return try {
            val response = apiService.getCurrentUser()

            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                var user = profile.toDomain()
                user = enrichUserWithUnits(user)

                _currentUser.value = user
                lastFetch = timeSource.markNow()
                Result.success(user)
            } else if (response.code == 401) {
                // Expired/invalid token: drop it so the app shows login instead of an empty dashboard.
                tokenManager.clearToken()
                lastFetch = null
                Result.failure(SessionExpiredException())
            } else {
                // Network or server failure: the token may still be good, so it stays.
                Result.failure(Exception("Failed to fetch user"))
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
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
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
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
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        _currentUser.value = null
        lastFetch = null
    }

    override suspend fun hasValidSession(): Boolean {
        return tokenManager.getToken() != null
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // PATCH users/me accepts name, phone and settings, never the unit (A-10); the app has no phone yet.
            val request = com.example.condominio.data.model.UpdateUserRequest(name = user.name)
            val response = apiService.updateUser(request)
            if (response.isSuccessful) {
                lastFetch = null // the profile changed: the next fetch must see it
                // Screens already open (Profile) read currentUser: show the new name without another request.
                _currentUser.value = _currentUser.value?.copy(name = user.name)
                Result.success(Unit)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun changePassword(
            currentPassword: String,
            newPassword: String
    ): Result<Unit> {
        // ponytail: backend verifies via session token, not currentPassword; client-side check only
        return try {
            val response = apiService.changePassword(newPassword)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(reason: String?): Result<Unit> {
        return try {
            val response = apiService.deleteAccount(reason)
            if (response.isSuccessful) {
                // Account is now banned server-side; clear local session too.
                tokenManager.clearToken()
                _currentUser.value = null
                lastFetch = null
                Result.success(Unit)
            } else {
                Result.failure(response.toFailure())
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    private suspend fun enrichUserWithUnits(user: User): User {
        val enrichedUnits = mutableListOf<com.example.condominio.data.model.UserUnit>()
        for (unit in user.units) {
            var uName = unit.unitName
            var bName = unit.buildingName
            var bId = unit.buildingId

            if (uName.isEmpty() || bName.isEmpty()) {
                // Fetch unit details
                val unitDetailsResp =
                        try {
                            apiService.getUnitDetails(unit.unitId)
                        } catch (e: Throwable) {
                            e.rethrowIfCancellation()
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
                        } catch (e: Throwable) {
                            e.rethrowIfCancellation()
                            null
                        }
                bName = buildingResp?.body()?.name ?: "Building"
            }

            enrichedUnits.add(unit.copy(unitName = uName, buildingName = bName, buildingId = bId))
        }

        val current = _currentUser.value?.currentUnit
        val newCurrent =
                if (current != null && enrichedUnits.any { it.unitId == current.unitId }) {
                    enrichedUnits.find { it.unitId == current.unitId }
                } else {
                    enrichedUnits.find { it.isPrimary } ?: enrichedUnits.firstOrNull()
                }

        return user.copy(units = enrichedUnits, currentUnit = newCurrent)
    }
}

private fun UserProfile.toDomain(): User {
    val domainUnits =
            units?.map { dto ->
                com.example.condominio.data.model.UserUnit(
                        unitId = dto.unitId,
                        buildingId = dto.buildingId,
                        unitName = dto.unitName ?: "",
                        buildingName = dto.buildingName ?: "",
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

    return User(
            id = id ?: "",
            name = name ?: "Unknown",
            email = email ?: "",
            appRole = appRole ?: "user",
            status = status ?: "active",
            units = domainUnits,
            buildingRoles = domainBuildingRoles,
            currentUnit = domainUnits.firstOrNull(),
            mustChangePassword = mustChangePassword ?: false
    )
}
