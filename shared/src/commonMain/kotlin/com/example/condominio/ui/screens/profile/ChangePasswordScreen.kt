package com.example.condominio.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.formWidth
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.AptoOnSurfaceVariant
import com.example.condominio.ui.theme.AptoSuccess
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Mirrors the minimum length enforced by [ChangePasswordViewModel]. */
private const val MIN_PASSWORD_LENGTH = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val changedMessage = stringResource(Res.string.password_changed)

    // Typed passwords: ask before leaving by the top bar or the system back (E-R4).
    val askBeforeLeaving = uiState.hasChanges && !uiState.isLoading
    val onBack: () -> Unit = {
        if (askBeforeLeaving) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }
    BackHandler(enabled = askBeforeLeaving) { showDiscardDialog = true }

    // The change is confirmed here; the form is already wiped by the ViewModel.
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.onSuccessShown()
            snackbarHostState.showSnackbar(changedMessage)
        }
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = {
                showDiscardDialog = false
                onBackClick()
            },
            onKeepEditing = { showDiscardDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.change_password),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = screenBackground(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        if (isWide()) {
            WideChangePasswordContent(uiState, viewModel, Modifier.padding(paddingValues))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.change_password_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CurrentPasswordField(uiState, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            NewPasswordField(uiState, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            ConfirmPasswordField(uiState, viewModel)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.password_min_length),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error Message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Change Password Button
            PrimaryButton(
                text = stringResource(Res.string.change_password),
                onClick = viewModel::onChangePasswordClick,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Web/tablet layout: the form inside a [WebCard], new + confirm side by side on Expanded. */
@Composable
private fun WideChangePasswordContent(
    uiState: ChangePasswordUiState,
    viewModel: ChangePasswordViewModel,
    modifier: Modifier = Modifier
) {
    val submitOnEnter = KeyboardActions(onDone = {
        if (!uiState.isLoading) viewModel.onChangePasswordClick()
    })

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = pagePadding())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        WebCard(modifier = Modifier.formWidth()) {
            Text(
                text = stringResource(Res.string.change_password_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            CurrentPasswordField(uiState, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            if (isExpanded()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NewPasswordField(uiState, viewModel, Modifier.weight(1f))
                    ConfirmPasswordField(uiState, viewModel, Modifier.weight(1f), submitOnEnter)
                }
            } else {
                NewPasswordField(uiState, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                ConfirmPasswordField(uiState, viewModel, keyboardActions = submitOnEnter)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password requirements: turns green once satisfied.
            val meetsMinLength = uiState.newPassword.length >= MIN_PASSWORD_LENGTH
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (meetsMinLength) AptoSuccess else AptoOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.password_min_length),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (meetsMinLength) AptoSuccess else AptoOnSurfaceVariant
                )
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PrimaryButton(
                    text = stringResource(Res.string.change_password),
                    onClick = viewModel::onChangePasswordClick,
                    isLoading = uiState.isLoading,
                    // PrimaryButton applies fillMaxWidth internally; bound it to its natural width
                    // (with a minimum so it doesn't shrink while the spinner shows).
                    modifier = Modifier
                        .widthIn(min = 200.dp)
                        .width(IntrinsicSize.Max)
                        .pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CurrentPasswordField(
    uiState: ChangePasswordUiState,
    viewModel: ChangePasswordViewModel,
    modifier: Modifier = Modifier
) {
    PasswordField(
        value = uiState.currentPassword,
        onValueChange = viewModel::onCurrentPasswordChange,
        label = stringResource(Res.string.current_password),
        enabled = !uiState.isLoading,
        visible = uiState.currentPasswordVisible,
        onToggleVisibility = viewModel::toggleCurrentPasswordVisibility,
        contentType = ContentType.Password,
        isError = uiState.currentPasswordError,
        modifier = modifier
    )
}

@Composable
private fun NewPasswordField(
    uiState: ChangePasswordUiState,
    viewModel: ChangePasswordViewModel,
    modifier: Modifier = Modifier
) {
    PasswordField(
        value = uiState.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        label = stringResource(Res.string.new_password),
        enabled = !uiState.isLoading,
        visible = uiState.newPasswordVisible,
        onToggleVisibility = viewModel::toggleNewPasswordVisibility,
        contentType = ContentType.NewPassword,
        isError = uiState.newPasswordError,
        modifier = modifier
    )
}

@Composable
private fun ConfirmPasswordField(
    uiState: ChangePasswordUiState,
    viewModel: ChangePasswordViewModel,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions? = null
) {
    PasswordField(
        value = uiState.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        label = stringResource(Res.string.confirm_new_password),
        enabled = !uiState.isLoading,
        visible = uiState.confirmPasswordVisible,
        onToggleVisibility = viewModel::toggleConfirmPasswordVisibility,
        contentType = ContentType.NewPassword,
        isError = uiState.confirmPasswordError,
        modifier = modifier,
        keyboardActions = keyboardActions
    )
}

/**
 * Password field with show/hide toggle. With [keyboardActions] == null it renders exactly as the
 * original mobile field (no IME action); otherwise it uses ImeAction.Done with those actions.
 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    contentType: ContentType,
    isError: Boolean,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions? = null
) {
    LabeledOutlinedField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        contentType = contentType,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = if (keyboardActions == null) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        },
        keyboardActions = keyboardActions ?: KeyboardActions.Default,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) stringResource(Res.string.hide_password) else stringResource(Res.string.show_password)
                )
            }
        }
    )
}
