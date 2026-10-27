package com.example.condominio.ui.screens.profile

import com.example.condominio.ui.utils.emojiSafe
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.example.condominio.ui.utils.UiText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.example.condominio.data.model.User
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.AptoOnSurfaceVariant
import com.example.condominio.ui.theme.AptoOutlineVariant
import com.example.condominio.ui.locale.AppLanguage
import com.example.condominio.ui.locale.LocaleManager
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.theme.AptoBackground
import com.example.condominio.ui.theme.AptoError
import com.example.condominio.ui.theme.AptoGradientGold
import com.example.condominio.ui.theme.AptoOnSurface
import com.example.condominio.ui.theme.AptoSecondary
import com.example.condominio.ui.theme.AptoSurfaceContainerLowest
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.onLogoutClick()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            reason = deleteReason,
            onReasonChange = { deleteReason = it },
            isDeleting = uiState.isDeleting,
            errorMessage = uiState.deleteError,
            onConfirm = { viewModel.onDeleteAccountConfirm(deleteReason) },
            onDismiss = { if (!uiState.isDeleting) showDeleteDialog = false }
        )
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
            TopBarWithBack(
                title = stringResource(Res.string.profile_title),
                onBackClick = onBackClick
            )
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        if (uiState.isLoading && uiState.user == null) {
            LoadingState(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        if (isWide()) {
            ProfileWideContent(
                user = uiState.user,
                modifier = Modifier.padding(paddingValues),
                onEditProfileClick = onEditProfileClick,
                onNotificationSettingsClick = onNotificationSettingsClick,
                onLanguageClick = { showLanguageDialog = true },
                onChangePasswordClick = onChangePasswordClick,
                isLoggingOut = uiState.isLoggingOut,
                onLogoutClick = { showLogoutDialog = true },
                onDeleteAccountClick = { showDeleteDialog = true }
            )
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
                                AptoGradientGold,
                                MaterialTheme.colorScheme.primary
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
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.user?.name ?: stringResource(Res.string.loading),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Information Section
            Text(
                text = stringResource(Res.string.account_info_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            // Card list container with border
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    ProfileActionButton(
                        icon = Icons.Default.Edit,
                        text = stringResource(Res.string.edit_profile),
                        onClick = onEditProfileClick
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    ProfileActionButton(
                        icon = Icons.Default.Notifications,
                        text = stringResource(Res.string.notification_settings),
                        onClick = onNotificationSettingsClick
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    // Language Selector
                    ProfileActionButton(
                        icon = Icons.Default.Language,
                        text = stringResource(Res.string.language_settings),
                        subtitle = "${LocaleManager.currentLanguage.flag.emojiSafe()} ${LocaleManager.currentLanguage.displayName}",
                        onClick = { showLanguageDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                    ProfileActionButton(
                        icon = Icons.Default.Lock,
                        text = stringResource(Res.string.change_password),
                        onClick = onChangePasswordClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout: a calm outlined action (it is routine, not destructive); asks first and shows progress.
            LogoutButton(
                isLoggingOut = uiState.isLoggingOut,
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Delete account (soft delete) — required for Google Play Data Safety.
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AptoError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.delete_account),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AptoError
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Wide (Medium/Expanded) profile: identity card on the left, settings groups on the right
 * (stacked on Medium). Phone layout lives in [ProfileScreen] untouched.
 */
@Composable
private fun ProfileWideContent(
    user: User?,
    modifier: Modifier,
    onEditProfileClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val identity: @Composable ColumnScope.() -> Unit = { ProfileIdentityCard(user) }
    val settings: @Composable ColumnScope.() -> Unit = {
        ProfileSettingsCard(title = stringResource(Res.string.account_info_title)) {
            ProfileActionButton(
                icon = Icons.Default.Edit,
                text = stringResource(Res.string.edit_profile),
                onClick = onEditProfileClick
            )
        }
        ProfileSettingsCard(title = stringResource(Res.string.web_profile_preferences)) {
            ProfileActionButton(
                icon = Icons.Default.Notifications,
                text = stringResource(Res.string.notification_settings),
                onClick = onNotificationSettingsClick
            )
            WideDivider()
            ProfileActionButton(
                icon = Icons.Default.Language,
                text = stringResource(Res.string.language_settings),
                subtitle = "${LocaleManager.currentLanguage.flag.emojiSafe()} ${LocaleManager.currentLanguage.displayName}",
                onClick = onLanguageClick
            )
        }
        ProfileSettingsCard(title = stringResource(Res.string.web_profile_security)) {
            ProfileActionButton(
                icon = Icons.Default.Lock,
                text = stringResource(Res.string.change_password),
                onClick = onChangePasswordClick
            )
        }
        ProfileSettingsCard(
            title = stringResource(Res.string.web_profile_danger_zone),
            titleColor = AptoError
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDeleteAccountClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AptoError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.delete_account),
                        fontWeight = FontWeight.SemiBold,
                        color = AptoError
                    )
                }
                LogoutButton(
                    isLoggingOut = isLoggingOut,
                    onClick = onLogoutClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = pagePadding())
            .padding(top = 8.dp, bottom = 32.dp)
    ) {
        if (isExpanded()) {
            AdaptiveColumns(mainWeight = 2f, sideWeight = 3f, main = identity, side = settings)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                identity()
                settings()
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(user: User?) {
    WebCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AptoGradientGold, MaterialTheme.colorScheme.primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user?.name ?: stringResource(Res.string.loading),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = AptoOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user?.email ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.apt_label, user?.apartmentUnit?.ifBlank { null } ?: "-"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = AptoOutlineVariant, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        IdentityDetailRow(Icons.Default.Person, stringResource(Res.string.full_name), user?.name ?: "-")
        IdentityDetailRow(Icons.Default.Email, stringResource(Res.string.email), user?.email ?: "-")
        IdentityDetailRow(
            Icons.Default.Home,
            stringResource(Res.string.apartment_unit_label),
            user?.apartmentUnit?.ifBlank { null } ?: "-"
        )
        IdentityDetailRow(
            Icons.Default.Apartment,
            stringResource(Res.string.building_tower),
            user?.currentUnit?.buildingName?.ifBlank { null } ?: "-"
        )
    }
}

@Composable
private fun IdentityDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = AptoOnSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AptoOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileSettingsCard(
    title: String,
    titleColor: Color = AptoOnSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    WebCard(contentPadding = 0.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun WideDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = AptoOutlineVariant,
        thickness = 1.dp
    )
}

@Composable
private fun DeleteAccountDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    isDeleting: Boolean,
    errorMessage: UiText?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.delete_account_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AptoError
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.delete_account_message),
                    fontSize = 14.sp,
                    color = AptoOnSurface
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    enabled = !isDeleting,
                    label = { Text(stringResource(Res.string.delete_account_reason_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(text = errorMessage.asString(), fontSize = 12.sp, color = AptoError)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AptoError, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(Res.string.delete_account_confirm),
                        fontWeight = FontWeight.Bold,
                        color = AptoError
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(text = stringResource(Res.string.cancel), color = AptoSecondary)
            }
        },
        containerColor = AptoSurfaceContainerLowest,
        shape = RoundedCornerShape(24.dp)
    )
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
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
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
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = language == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Clip first so the ripple and focus ring follow the rounded corners (E-R3).
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .selectable(
                                selected = isSelected,
                                onClick = { onLanguageSelected(language) },
                                role = Role.RadioButton
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.flag.emojiSafe(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/** Logout is routine, so it is an outlined neutral button (not red, not filled); disabled with a spinner while it runs (E-R1). */
@Composable
private fun LogoutButton(isLoggingOut: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoggingOut,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(if (isLoggingOut) Res.string.logging_out else Res.string.logout),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.logout_confirm_title)) },
        text = { Text(stringResource(Res.string.logout_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.logout)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
