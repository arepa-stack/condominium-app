package com.example.condominio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

import com.example.condominio.data.local.dao.PaymentDao
import com.example.condominio.data.local.dao.UserDao
import com.example.condominio.data.local.entity.PaymentEntity
import com.example.condominio.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, PaymentEntity::class],
    version = 4,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun paymentDao(): PaymentDao
    
}
