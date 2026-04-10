package com.example.condominio.data.repository

import com.example.condominio.data.local.dao.UserDao
import com.example.condominio.data.local.dao.PaymentDao
import com.example.condominio.data.local.entity.*
import com.example.condominio.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

class RoomAuthRepository (
    private val userDao: UserDao,
    private val paymentDao: PaymentDao
) : AuthRepository {

    override val currentUser: Flow<User?> = userDao.getUser().map { it?.toDomain() }

    override suspend fun login(email: String, password: String): Result<User> {
        delay(1000) // Simulate network delay
        
        // Check if user already exists
        val existingUser = userDao.getUser().firstOrNull()
        
        // Simple validation (in real app, this would be a backend call)
        if (email.isNotBlank() && password.length >= 6) {
            // If user exists, return it; otherwise create new one
            val demoUnit = com.example.condominio.data.model.UserUnit(
                unitId = "u1",
                buildingId = "b1",
                unitName = "4B",
                buildingName = "Torre Este",
                isPrimary = true
            )
            val user = existingUser?.toDomain() ?: User(
                id = "user_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
                name = "Demo User",
                email = email,
                units = listOf(demoUnit),
                currentUnit = demoUnit
            )
            
            // Only insert if it's a new user
            if (existingUser == null) {
                userDao.insertUser(user.toEntity())
                seedDemoPayments()
            }
            
            return Result.success(user)
        }
        return Result.failure(Exception("Invalid credentials"))
    }

    private suspend fun seedDemoPayments() {
        // Clear existing payments first
        // paymentDao.clearPayments() // Assuming we want a clean state for the demo

        val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val currentYear = now.year
        val currentMonth = now.monthNumber // 1-indexed

        // Requirement: 2 months paid, 1 month overdue, current month pending (blinking)
        // M = Current (Pending)
        // M-1 = Overdue (No payment)
        // M-2 = Paid
        // M-3 = Paid
        
        val monthsToSeed = listOf(currentMonth - 2, currentMonth - 3)
        
        monthsToSeed.forEach { month ->
            // Handle year rollover if needed (e.g. Current=Jan(1), M-1=Dec(12))
            var year = currentYear
            var m = month
            while (m <= 0) {
                m += 12
                year -= 1
            }
            
            val periodId = "${year}-${m.toString().padStart(2, '0')}"
            
            val payment = com.example.condominio.data.model.Payment(
                id = "seed_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_$m",
                amount = 45.0,
                date = kotlinx.datetime.LocalDate(year, m, 5).atStartOfDayIn(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds(),
                status = com.example.condominio.data.model.PaymentStatus.APPROVED,
                description = "Condo Fee ${monthName(m)}",
                method = com.example.condominio.data.model.PaymentMethod.PAGO_MOVIL
            )
            
            paymentDao.insertPayment(payment.toEntity())
        }
    }
    
    private fun monthName(month: Int): String {
        return when(month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Month $month"
        }
    }

    override suspend fun register(name: String, email: String, unitId: String, building: String, password: String): Result<User> {
        delay(1000)
        
        if (name.isBlank() || email.isBlank() || unitId.isBlank() || building.isBlank() || password.length < 6) {
            return Result.failure(Exception("Invalid input"))
        }
        
        // Delete any existing user before creating new one (for demo purposes)
        val existingUser = userDao.getUser().firstOrNull()
        existingUser?.let {
            userDao.deleteUser(it)
        }
        
        val demoUnit = com.example.condominio.data.model.UserUnit(
            unitId = "u_demo",
            buildingId = "b_demo",
            unitName = "Demo Unit",
            buildingName = building,
            isPrimary = true
        )

        val user = User(
            id = "user_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
            name = name,
            email = email,
            units = listOf(demoUnit),
            currentUnit = demoUnit
        )
        
        userDao.insertUser(user.toEntity())
        return Result.success(user)
    }

    override suspend fun logout() {
        // In a real app, you might clear the user session
        // For now, we'll keep the user in the database
    }

    override suspend fun fetchCurrentUser(): Result<User> {
        val userEntity = userDao.getUser().firstOrNull()
        return if (userEntity != null) {
            Result.success(userEntity.toDomain())
        } else {
            Result.failure(Exception("No user logged in"))
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            delay(500)
            userDao.insertUser(user.toEntity()) // Insert/Update
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        delay(500)
        
        // In a real app, you would verify currentPassword against stored password
        // For this demo, we'll just validate the new password
        return if (newPassword.length >= 6) {
            // Password would be updated in backend/database
            Result.success(Unit)
        } else {
            Result.failure(Exception("Password must be at least 6 characters"))
        }
    }

    override suspend fun getUnits(buildingId: String): Result<List<com.example.condominio.data.model.UnitDto>> {
        delay(500)
        // Return dummy units for demo purposes
        return Result.success(listOf(
            com.example.condominio.data.model.UnitDto("u1", buildingId, "1A", "1", 0.0),
            com.example.condominio.data.model.UnitDto("u2", buildingId, "1B", "1", 0.0),
            com.example.condominio.data.model.UnitDto("u3", buildingId, "2A", "2", 0.0),
            com.example.condominio.data.model.UnitDto("u4", buildingId, "2B", "2", 0.0)
        ))
    }

    override suspend fun getUnit(unitId: String): Result<com.example.condominio.data.model.UnitDto> {
        delay(500)
        return Result.success(com.example.condominio.data.model.UnitDto(unitId, "building_id", "Demo Unit", "1", 0.0))
    }

    override fun setCurrentUnit(unit: com.example.condominio.data.model.UserUnit) {
        // In Room impl, we might update the current user in DB or just in memory
        // For now, it's a no-op or just updates local flow if we were caching it
    }
}
