package com.example.condominio.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.locale.AppLanguage
import com.example.condominio.ui.locale.LocaleManager
import com.example.condominio.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    // Language selection dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = LocaleManager.currentLanguage,
            onLanguageSelected = { language ->
                LocaleManager.setLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.profile_title), 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF09151A)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(Res.string.back),
                            tint = Color(0xFF09151A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Placeholder setting action */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Settings",
                            tint = Color(0xFF09151A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AptoBackground
                )
            )
        },
        bottomBar = {
            ProfileBottomNavBar(
                onHomeClick = onBackClick,
                onProfileClick = {}
            )
        },
        containerColor = AptoBackground
    ) { paddingValues ->
        if (uiState.isLoading && uiState.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AptoSecondaryContainer)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header Hero with Gradient and Glass effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF9E00), // golden orange
                                AptoSecondaryContainer // brand orange
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Circle with Glassmorphic border
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.user?.name?.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.user?.name ?: stringResource(Res.string.loading),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Glassmorphic Apartment Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.apt_label, uiState.user?.apartmentUnit ?: "-"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Information Section
            Text(
                text = stringResource(Res.string.account_info_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AptoOnSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileInfoRow(
                    icon = Icons.Default.Person,
                    label = stringResource(Res.string.full_name),
                    value = uiState.user?.name ?: "-"
                )

                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = stringResource(Res.string.email),
                    value = uiState.user?.email ?: "-"
                )

                ProfileInfoRow(
                    icon = Icons.Default.Home,
                    label = stringResource(Res.string.apartment_unit_label),
                    value = uiState.user?.apartmentUnit ?: "-"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Section
            Text(
                text = stringResource(Res.string.quick_actions_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AptoOnSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            // Card list container with border
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AptoSurfaceContainerLowest
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant)
            ) {
                Column {
                    ProfileActionButton(
                        icon = Icons.Default.Edit,
                        text = stringResource(Res.string.edit_profile),
                        onClick = onEditProfileClick
                    )
                    
                    HorizontalDivider(color = AptoOutlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    ProfileActionButton(
                        icon = Icons.Default.Notifications,
                        text = stringResource(Res.string.notification_settings),
                        onClick = onNotificationSettingsClick
                    )

                    HorizontalDivider(color = AptoOutlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    // Language Selector
                    ProfileActionButton(
                        icon = Icons.Default.Language,
                        text = stringResource(Res.string.language_settings),
                        subtitle = "${LocaleManager.currentLanguage.flag} ${LocaleManager.currentLanguage.displayName}",
                        onClick = { showLanguageDialog = true }
                    )

                    HorizontalDivider(color = AptoOutlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    ProfileActionButton(
                        icon = Icons.Default.Lock,
                        text = stringResource(Res.string.change_password),
                        onClick = onChangePasswordClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Button
            Button(
                onClick = viewModel::onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, AptoError.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AptoErrorContainer,
                    contentColor = AptoError
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.logout),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AptoSurfaceContainerLowest, RoundedCornerShape(12.dp))
            .border(1.dp, AptoOutlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AptoSurfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AptoSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AptoOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface
            )
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AptoSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AptoOnSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AptoOnSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AptoOutline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.language_dialog_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AptoOnSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = language == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) AptoPrimaryFixed else AptoSurfaceContainerLow,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onLanguageSelected(language) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.flag,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = language.displayName,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = AptoOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AptoSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.close),
                    fontWeight = FontWeight.Bold,
                    color = AptoSecondary
                )
            }
        },
        containerColor = AptoSurfaceContainerLowest,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ProfileBottomNavBar(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val activeColor = AptoSecondary
    val inactiveColor = AptoOutline

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AptoSurfaceContainerLowest,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (not active, clicks navigates back to Dashboard)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onHomeClick)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = "Inicio",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Home",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = inactiveColor
                )
            }

            // Requests
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Handyman,
                    contentDescription = "Solicitudes",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Requests",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = inactiveColor
                )
            }

            // Units
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Domain,
                    contentDescription = "Unidades",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Units",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = inactiveColor
                )
            }

            // Profile (Active)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Perfil",
                    tint = activeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Profile",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
            }
        }
    }
}

