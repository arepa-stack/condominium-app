package com.example.condominio.di

import com.example.condominio.ApiConfig
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.remote.ApiServiceImpl
import com.example.condominio.data.remote.SessionExpiry
import com.example.condominio.data.repository.*
import com.example.condominio.domain.usecase.billboard.GetUnreadAnnouncementsCountUseCase
import com.example.condominio.ui.screens.UnitSelectionViewModel
import com.example.condominio.ui.screens.billboard.BillboardDetailViewModel
import com.example.condominio.ui.screens.billboard.BillboardListViewModel
import com.example.condominio.ui.screens.billing.*
import com.example.condominio.ui.screens.dashboard.*
import com.example.condominio.ui.screens.decisions.DecisionDetailViewModel
import com.example.condominio.ui.screens.decisions.DecisionsListViewModel
import com.example.condominio.ui.screens.login.*
import com.example.condominio.ui.screens.payment.*
import com.example.condominio.ui.screens.profile.*
import com.example.condominio.ui.screens.register.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.*
import org.koin.dsl.module

/**
 * Platform-agnostic graph. Each platform adds a module providing:
 * [HttpClientEngine], [TokenManager] and PlatformFileReader.
 */
val sharedModule = module {
    single {
        val tokenManager = get<TokenManager>()
        HttpClient(get<HttpClientEngine>()) {
            install(createClientPlugin("BearerAuth") {
                onRequest { request, _ ->
                    tokenManager.getToken()?.let { request.headers.append(HttpHeaders.Authorization, "Bearer $it") }
                }
            })
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
            defaultRequest {
                url(ApiConfig.BASE_URL) // from local.properties (API_ENV)
            }
        }
    }
    single { SessionExpiry(get()) }
    singleOf(::ApiServiceImpl) { bind<ApiService>() }

    // Repositories
    single<AuthRepository> { RemoteAuthRepository(get(), get()) }
    single<PaymentRepository> { RemotePaymentRepository(get(), get()) }
    single<BuildingRepository> { RemoteBuildingRepository(get()) }
    single<OnboardingRepository> { RemoteOnboardingRepository(get()) }
    single<PettyCashRepository> { PettyCashRepositoryImpl(get()) }
    single<DecisionsRepository> { RemoteDecisionsRepositoryImpl(get(), get()) }
    single<AnnouncementsRepository> { RemoteAnnouncementsRepositoryImpl(get()) }
    factory { GetUnreadAnnouncementsCountUseCase(get()) }

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::QrScannerViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::FirstLoginChangePasswordViewModel)
    viewModelOf(::UnitSelectionViewModel)
    viewModelOf(::InvoiceDetailViewModel)
    viewModelOf(::InvoiceListViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::CreatePaymentViewModel)
    viewModelOf(::PaymentDetailViewModel)
    viewModelOf(::PaymentHistoryViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::DecisionsListViewModel)
    viewModelOf(::DecisionDetailViewModel)
    viewModelOf(::BillboardListViewModel)
    viewModelOf(::BillboardDetailViewModel)
}
