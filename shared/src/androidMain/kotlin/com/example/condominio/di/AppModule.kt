package com.example.condominio.di

import androidx.room.Room
import com.example.condominio.data.local.AppDatabase
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.data.repository.PaymentRepository
import com.example.condominio.data.repository.PettyCashRepository
import com.example.condominio.data.repository.PettyCashRepositoryImpl
import com.example.condominio.data.repository.RemoteAuthRepository
import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.repository.*
import com.example.condominio.data.utils.*
import com.example.condominio.ui.screens.*
import com.example.condominio.ui.screens.billing.*
import com.example.condominio.ui.screens.dashboard.*
import com.example.condominio.ui.screens.login.*
import com.example.condominio.ui.screens.payment.*
import com.example.condominio.ui.screens.pettycash.*
import com.example.condominio.ui.screens.profile.*
import com.example.condominio.ui.screens.register.*
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.*
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "condominio_db"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }
    
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().paymentDao() }
    
    // Services / Managers
    singleOf(::TokenManager)
    single<PlatformFileReader> { AndroidFileReader(androidContext()) }
    single<PdfService> { PdfServiceImpl(get()) }
    
    // DataStore Preference provider
    single<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>> {
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
            produceFile = { androidContext().filesDir.resolve("auth_prefs.preferences_pb").absolutePath.toPath() }
        )
    }
    
    // Repositories
    single<AuthRepository> { RemoteAuthRepository(get(), get()) }
    single<PaymentRepository> { RemotePaymentRepository(get(), get()) }
    single<BuildingRepository> { RemoteBuildingRepository(get()) }
    single<PettyCashRepository> { PettyCashRepositoryImpl(get(), get()) }
    
    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::UnitSelectionViewModel)
    viewModelOf(::InvoiceDetailViewModel)
    viewModelOf(::InvoiceListViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::CreatePaymentViewModel)
    viewModelOf(::PaymentDetailViewModel)
    viewModelOf(::PaymentHistoryViewModel)
    viewModelOf(::PettyCashViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::RegisterViewModel)
}
