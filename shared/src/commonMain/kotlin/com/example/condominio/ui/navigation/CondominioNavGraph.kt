package com.example.condominio.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import com.example.condominio.data.remote.SessionExpiry
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.telemetry.Telemetry
import com.example.condominio.telemetry.screenName
import org.koin.compose.koinInject
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable as navComposable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
// Estos imports deberán ser actualizados cuando movamos las pantallas a commonMain
import com.example.condominio.ui.screens.auth.AdminBlockedScreen
import com.example.condominio.ui.screens.auth.PendingApprovalScreen
import com.example.condominio.ui.screens.billing.InvoiceDetailScreen
import com.example.condominio.ui.screens.dashboard.DashboardScreen
import com.example.condominio.ui.screens.login.LoginScreen
import com.example.condominio.ui.screens.payment.CreatePaymentScreen
import com.example.condominio.ui.screens.payment.PaymentDetailScreen
import com.example.condominio.ui.screens.payment.PaymentHistoryScreen
import com.example.condominio.ui.screens.profile.ChangePasswordScreen
import com.example.condominio.ui.screens.profile.EditProfileScreen
import com.example.condominio.ui.screens.profile.NotificationSettingsScreen
import com.example.condominio.ui.screens.profile.ProfileScreen
import com.example.condominio.ui.screens.decisions.DecisionsListScreen
import com.example.condominio.ui.screens.decisions.DecisionDetailScreen
import com.example.condominio.ui.screens.billboard.BillboardListScreen
import com.example.condominio.ui.screens.billboard.BillboardDetailScreen
import com.example.condominio.ui.screens.register.QrScannerScreen
import com.example.condominio.ui.screens.register.RegisterScreen
import com.example.condominio.ui.screens.splash.SplashScreen
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.components.AppNavRail
import com.example.condominio.ui.components.AppSidebar
import com.example.condominio.ui.components.AptoWebCanvas
import com.example.condominio.ui.components.AuthBrandPanel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.condominio.ui.components.LocalShowBack
import com.example.condominio.ui.components.LocalLoginFormVisible
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.example.condominio.ui.components.LocalWidthClass
import com.example.condominio.ui.components.WidthClass
import com.example.condominio.ui.components.WebContainerWidth
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.condominio.ui.components.AppBottomBar
import com.example.condominio.ui.components.BottomTab
import com.example.condominio.ui.utils.rememberReducedMotion

// Every app page shares ONE container (same max width, same left edge) so the page title never
// jumps when navigating. Narrow content (settings forms) is capped inside the page, left-aligned
// (see WebLayout.formWidth). Auth screens keep their own centered card / brand split.
private val tabRootRoutes = setOf("dashboard", "invoice_list", "billboard_list", "decisions_list", "profile")

private fun maxContentWidth(route: String?): Dp = when {
    route == null || route == "splash" -> Dp.Unspecified
    route in routesWithoutBottomBar -> 560.dp
    else -> WebContainerWidth
}

/** Max width of one page. Desktop auth/onboarding pages sit in the 480dp column next to the brand panel. */
@Composable
private fun pageMaxWidth(route: String, inApp: Boolean): Dp {
    val loginFormVisible = LocalLoginFormVisible.current.value
    val split = LocalWidthClass.current == WidthClass.Expanded && route in routesWithoutBottomBar &&
        route != "splash" && route != "unit_selection" && (route != "login" || loginFormVisible)
    return when {
        split -> 480.dp
        // Login still checking the session: keep full width so the splash fades out unclipped.
        route == "login" && !loginFormVisible -> Dp.Unspecified
        // Opened from the dashboard: a regular app page (same container as the rest).
        inApp -> WebContainerWidth
        else -> maxContentWidth(route)
    }
}

/** True while unit_selection was opened from inside the app (dashboard below it), not from login. */
private val LocalInAppUnitSelection = compositionLocalOf { false }

/**
 * Shadows navigation's composable(): wraps every destination in its own width cap, so the cap
 * travels with the page during transitions (instead of the NavHost resizing for the new route).
 */
private fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = navComposable(route, arguments) { entry ->
    val scope = this
    // Captured once per entry: the page keeps its width while fading out, even after the route changes.
    val inAppNow = route == "unit_selection" && LocalInAppUnitSelection.current
    val inApp = remember(entry) { inAppNow }
    Box(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = pageMaxWidth(route, inApp)).fillMaxWidth()) {
        scope.content(entry)
    }
}

private const val NavFadeMillis = 200

// Rutas del flujo de autenticación/onboarding donde no se muestra el bottom bar
private val routesWithoutBottomBar = setOf(
        "splash",
        "login",
        "admin_blocked",
        "pending_approval",
        "unit_selection",
        "register_scanner",
        "register_form/{buildingId}",
        "forgot_password",
        "first_login_change_password"
)

@Composable
fun CondominioNavGraph(
        navController: NavHostController = rememberNavController(),
        registerCode: String? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNav = currentRoute != null && currentRoute !in routesWithoutBottomBar
    val reducedMotion = rememberReducedMotion()
    // A 401 on any authenticated request (SessionExpiry already dropped the token): reset the in-memory session
    // and go to login from wherever the user is, clearing the back stack. splash/login handle it on their own.
    val sessionExpiry: SessionExpiry = koinInject()
    val authRepository: AuthRepository = koinInject()
    val telemetry: Telemetry = koinInject()
    LaunchedEffect(backStackEntry) { screenName(currentRoute)?.let(telemetry::screen) }
    // Expiry redirect: login replaces the screen at once, without the fade (reset once the user leaves login).
    var expiredRedirect by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) { if (currentRoute != "login") expiredRedirect = false }
    LaunchedEffect(navController) {
        sessionExpiry.events.collect {
            authRepository.logout()
            val route = navController.currentBackStackEntry?.destination?.route
            if (route != "splash" && route != "login") {
                expiredRedirect = true
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
        }
    }
    val scope = rememberCoroutineScope()
    val onTabSelected: (BottomTab) -> Unit = { tab ->
        // Token checked before opening the tab: if it is missing or expired, the collector above goes to login and
        // the tab is never drawn. Undispatched: on web the check doesn't suspend, so the tap navigates in the same frame.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (sessionExpiry.usableToken() == null) return@launch
            if (tab == BottomTab.HOME) {
                // Home is the stack root: just pop back to it. Navigating with popUpTo(dashboard){saveState} +
                // restoreState would save whatever sits above it (e.g. Perfil opened from the avatar) as Home's
                // state and restore it immediately, leaving the user stuck.
                if (!navController.popBackStack("dashboard", inclusive = false)) navController.navigate("dashboard")
            } else {
                navController.navigate(tab.route) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val widthClass = when {
        maxWidth < 600.dp -> WidthClass.Compact
        maxWidth < 1024.dp -> WidthClass.Medium
        else -> WidthClass.Expanded
    }
    val wide = widthClass != WidthClass.Compact
    // Switching building from the dashboard: keep the sidebar/rail on wide layouts so the shell doesn't
    // jump. From login (no dashboard below) it stays a standalone onboarding page. Phones unchanged.
    val inAppUnitSelection = currentRoute == "unit_selection" &&
        navController.previousBackStackEntry?.destination?.route == "dashboard"
    val showSideNav = showNav || (wide && inAppUnitSelection)
    val isAuth = currentRoute in routesWithoutBottomBar && currentRoute != "splash"
    // Desktop auth/onboarding: brand panel left, the mobile form centered on the right.
    val loginFormVisible = remember { mutableStateOf(false) }
    // unit_selection is also reached from the dashboard while logged in: no "sign in" brand panel there.
    val split = isAuth && widthClass == WidthClass.Expanded && currentRoute != "unit_selection" &&
        (currentRoute != "login" || loginFormVisible.value)
    CompositionLocalProvider(
        LocalWidthClass provides widthClass,
        LocalLoginFormVisible provides loginFormVisible,
        // Sidebar/rail replaces the back arrow on top-level sections.
        LocalShowBack provides !(wide && currentRoute in tabRootRoutes),
        LocalInAppUnitSelection provides inAppUnitSelection
    ) {
    Row(Modifier.fillMaxSize().background(if (wide) AptoWebCanvas else Color.Unspecified)) {
    if (showSideNav && widthClass == WidthClass.Expanded) AppSidebar(currentRoute = currentRoute, onTabSelected = onTabSelected)
    if (showSideNav && widthClass == WidthClass.Medium) AppNavRail(currentRoute = currentRoute, onTabSelected = onTabSelected)
    Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = if (wide) AptoWebCanvas else MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showNav && !wide) AppBottomBar(currentRoute = currentRoute, onTabSelected = onTabSelected)
            }
    ) { innerPadding ->
        val navHost = @Composable {
        NavHost(
                navController = navController,
                startDestination = "splash",
                // Short fade (NavHost's default is 700ms); none when the OS asks to reduce motion.
                // pop transitions default to these.
                enterTransition = {
                    if (reducedMotion || (expiredRedirect && targetState.destination.route == "login")) EnterTransition.None
                    else fadeIn(tween(NavFadeMillis))
                },
                exitTransition = {
                    if (reducedMotion || (expiredRedirect && targetState.destination.route == "login")) ExitTransition.None
                    else fadeOut(tween(NavFadeMillis))
                },
                // No max width here: each page caps its own (see composable() below), so a page fading
                // out keeps its width instead of being squeezed into the next route's (dashboard → unit_selection).
                modifier = Modifier
                        .padding(innerPadding)
                        // Screens have their own Scaffold/TopAppBar: mark the bars already padded here as consumed,
                        // or edge-to-edge (targetSdk 36) adds the status bar height twice on Android.
                        .consumeWindowInsets(innerPadding)
                        .fillMaxSize()
        ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                    // Deep link from a building QR: land on registration, with login behind it.
                    if (registerCode != null) navController.navigate(registerFormRoute(registerCode))
                }
            )
        }
        composable("login") {
            val viewModel: com.example.condominio.ui.screens.login.LoginViewModel = org.koin.compose.viewmodel.koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            LoginScreen(
                    uiState = uiState,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onLoginClick = viewModel::onLoginClick,
                    onClearDatabaseClick = viewModel::onClearDatabaseClick,
                    onRetrySessionCheck = viewModel::onRetrySessionCheck,
                    onUseAnotherAccount = viewModel::onUseAnotherAccount,
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
                    onAdminBlocked = {
                        navController.navigate("admin_blocked") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate("register_scanner") },
                    onForgotPasswordClick = { navController.navigate("forgot_password") },
                    onMustChangePassword = {
                        navController.navigate("first_login_change_password") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
            )
        }
        composable("forgot_password") {
            com.example.condominio.ui.screens.login.ForgotPasswordScreen(
                    onBackClick = { navController.popBackStack() }
            )
        }
        composable("first_login_change_password") {
            com.example.condominio.ui.screens.login.FirstLoginChangePasswordScreen(
                    onSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLoggedOut = {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
            )
        }
        composable("admin_blocked") {
            AdminBlockedScreen(
                    onBackToLogin = {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
            )
        }
        composable("unit_selection") {
            val viewModel: com.example.condominio.ui.screens.UnitSelectionViewModel = org.koin.compose.viewmodel.koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            com.example.condominio.ui.screens.UnitSelectionScreen(
                    uiState = uiState,
                    onBuildingSelected = viewModel::onBuildingSelected,
                    onUnitSelected = viewModel::onUnitSelected,
                    onRetry = viewModel::retry,
                    onNavigateToDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("unit_selection") { inclusive = true }
                        }
                    }
            )
        }
        composable("register_scanner") {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onScanned = { buildingId ->
                    navController.navigate(registerFormRoute(buildingId)) {
                        popUpTo("register_scanner") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "register_form/{buildingId}",
            arguments = listOf(navArgument("buildingId") { type = NavType.StringType })
        ) { backStack ->
            val buildingId = backStack.arguments?.read { getStringOrNull("buildingId") } ?: ""
            RegisterScreen(
                buildingId = buildingId,
                onRegisterSuccess = {
                    navController.navigate("pending_approval") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
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
                    onProfileClick = { onTabSelected(BottomTab.PROFILE) },
                    onUnitClick = { navController.navigate("unit_selection") },
                    onSeeAllInvoicesClick = { onTabSelected(BottomTab.PAYMENTS) },
                    onDecisionsClick = { onTabSelected(BottomTab.DECISIONS) },
                    onBillboardClick = { onTabSelected(BottomTab.BILLBOARD) }
            )
        }
        composable("invoice_list") {
            com.example.condominio.ui.screens.billing.InvoiceListScreen(
                    onBackClick = { navController.popBackStack() },
                    onInvoiceClick = { invoice ->
                        navController.navigate("invoice_detail/${invoice.id}")
                    },
                    onPayNowClick = { invoice ->
                        navController.navigate("create_payment?invoiceId=${invoice.id}")
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
        composable("decisions_list") {
            DecisionsListScreen(
                    onBackClick = { navController.popBackStack() },
                    onDecisionClick = { id -> navController.navigate("decision_detail/$id") }
            )
        }
        composable(
                route = "decision_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.read { getStringOrNull("id") } ?: ""
            DecisionDetailScreen(
                    decisionId = id,
                    onBackClick = { navController.popBackStack() }
            )
        }
        composable("billboard_list") {
            BillboardListScreen(
                    onBackClick = { navController.popBackStack() },
                    onAnnouncementClick = { id -> navController.navigate("billboard_detail/$id") }
            )
        }
        composable(
                route = "billboard_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.read { getStringOrNull("id") } ?: ""
            BillboardDetailScreen(
                    announcementId = id,
                    onBackClick = { navController.popBackStack() }
            )
        }
        }
        }
        // Same call site for navHost in both branches so the NavHost is never recreated.
        Row(Modifier.fillMaxSize()) {
            if (split) AuthBrandPanel(Modifier.weight(1f).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight().background(if (split) Color.White else Color.Unspecified)) { navHost() }
        }
    }
    }
    }
    }
}

