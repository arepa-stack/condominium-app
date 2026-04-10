package com.example.condominio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Estos imports deberán ser actualizados cuando movamos las pantallas a commonMain
import com.example.condominio.ui.screens.auth.PendingApprovalScreen
import com.example.condominio.ui.screens.billing.InvoiceDetailScreen
import com.example.condominio.ui.screens.dashboard.DashboardScreen
import com.example.condominio.ui.screens.login.LoginScreen
import com.example.condominio.ui.screens.payment.CreatePaymentScreen
import com.example.condominio.ui.screens.payment.PaymentDetailScreen
import com.example.condominio.ui.screens.payment.PaymentHistoryScreen
import com.example.condominio.ui.screens.pettycash.PettyCashScreen
import com.example.condominio.ui.screens.profile.ChangePasswordScreen
import com.example.condominio.ui.screens.profile.EditProfileScreen
import com.example.condominio.ui.screens.profile.NotificationSettingsScreen
import com.example.condominio.ui.screens.profile.ProfileScreen
import com.example.condominio.ui.screens.register.RegisterScreen

@Composable
fun CondominioNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val viewModel: com.example.condominio.ui.screens.login.LoginViewModel = org.koin.compose.viewmodel.koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            LoginScreen(
                    uiState = uiState,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onLoginClick = viewModel::onLoginClick,
                    onClearDatabaseClick = viewModel::onClearDatabaseClick,
                    onLoginSuccess = { hasMultiple ->
                        if (hasMultiple) {
                            navController.navigate("unit_selection") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onPendingApproval = { navController.navigate("pending_approval") },
                    onRegisterClick = { navController.navigate("register") }
            )
        }
        composable("unit_selection") {
            val viewModel: com.example.condominio.ui.screens.UnitSelectionViewModel = org.koin.compose.viewmodel.koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            com.example.condominio.ui.screens.UnitSelectionScreen(
                    uiState = uiState,
                    onItemSelected = viewModel::onItemSelected,
                    onUnitSelected = {
                        navController.navigate("dashboard") {
                            popUpTo("unit_selection") { inclusive = true }
                        }
                    }
            )
        }
        composable("register") {
            RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("pending_approval") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onLoginClick = { navController.popBackStack() }
            )
        }
        composable("pending_approval") {
            PendingApprovalScreen(
                    onLogout = {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                    onPayClick = { navController.navigate("create_payment") },
                    onHistoryClick = { navController.navigate("payment_history") },
                    onPaymentClick = { paymentId ->
                        navController.navigate("payment_detail/$paymentId")
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onUnitClick = { navController.navigate("unit_selection") },
                    onSeeAllInvoicesClick = { navController.navigate("invoice_list") },
                    onPettyCashClick = { navController.navigate("petty_cash") }
            )
        }
        composable("invoice_list") {
            com.example.condominio.ui.screens.billing.InvoiceListScreen(
                    onBackClick = { navController.popBackStack() },
                    onInvoiceClick = { invoice ->
                        if (invoice.status ==
                                        com.example.condominio.data.model.InvoiceStatus.PAID ||
                                        invoice.paid > 0
                        ) {
                            navController.navigate("invoice_detail/${invoice.id}")
                        } else {
                            navController.navigate("create_payment?invoiceId=${invoice.id}")
                        }
                    }
            )
        }
        composable(
                route = "invoice_detail/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) {
            InvoiceDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onSeeAllPaymentsClick = { navController.navigate("payment_history") },
                    onSeeAllInvoicesClick = {
                        navController.navigate("invoice_list") {
                            popUpTo("invoice_list") { inclusive = true }
                        }
                    },
                    onPayRemainderClick = { invoiceId ->
                        navController.navigate("create_payment?invoiceId=$invoiceId")
                    },
                    onPaymentClick = { paymentId ->
                        navController.navigate("payment_detail/$paymentId")
                    }
            )
        }
        composable("payment_history") {
            PaymentHistoryScreen(
                    onBackClick = { navController.popBackStack() },
                    onPaymentClick = { paymentId ->
                        navController.navigate("payment_detail/$paymentId")
                    }
            )
        }
        composable(
                route = "create_payment?invoiceId={invoiceId}",
                arguments =
                        listOf(
                                navArgument("invoiceId") {
                                    nullable = true
                                    defaultValue = null
                                    type = NavType.StringType
                                }
                        )
        ) {
            CreatePaymentScreen(
                    onBackClick = { navController.popBackStack() },
                    onSubmitSuccess = { navController.popBackStack() }
            )
        }
        composable(
                route = "payment_detail/{paymentId}",
                arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
        ) {
            PaymentDetailScreen(onBackClick = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditProfileClick = { navController.navigate("edit_profile") },
                    onNotificationSettingsClick = {
                        navController.navigate("notification_settings")
                    },
                    onChangePasswordClick = { navController.navigate("change_password") },
                    onLogoutSuccess = {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
            )
        }
        composable("edit_profile") {
            EditProfileScreen(onBackClick = { navController.popBackStack() })
        }
        composable("notification_settings") {
            NotificationSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable("change_password") {
            ChangePasswordScreen(onBackClick = { navController.popBackStack() })
        }
        composable("petty_cash") { PettyCashScreen(onBackClick = { navController.popBackStack() }) }
    }
}
