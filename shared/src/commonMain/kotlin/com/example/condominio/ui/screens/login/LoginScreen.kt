package com.example.condominio.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.LocalLoginFormVisible
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val FieldShape = RoundedCornerShape(8.dp)

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onClearDatabaseClick: () -> Unit,
    onRetrySessionCheck: () -> Unit,
    onUseAnotherAccount: () -> Unit,
    onLoginSuccess: (Boolean) -> Unit,
    onPendingApproval: () -> Unit,
    onAdminBlocked: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onMustChangePassword: () -> Unit
) {
    LaunchedEffect(uiState.isSuccess, uiState.isPending, uiState.isAdminBlocked, uiState.mustChangePassword) {
        if (uiState.mustChangePassword) {
            onMustChangePassword()
        } else if (uiState.isSuccess) {
            onLoginSuccess(uiState.hasMultipleUnits)
        } else if (uiState.isPending) {
            onPendingApproval()
        } else if (uiState.isAdminBlocked) {
            onAdminBlocked()
        }
    }

    val busy = uiState.isCheckingSession || uiState.isSuccess || uiState.isPending || uiState.isAdminBlocked || uiState.mustChangePassword
    val loginFormVisible = LocalLoginFormVisible.current
    SideEffect { loginFormVisible.value = !busy && !uiState.sessionCheckFailed }
    DisposableEffect(Unit) { onDispose { loginFormVisible.value = false } }

    if (busy) {
        LoadingState()
        return
    }

    // A saved session we could not verify (offline / server down): not a logout, so offer to retry (A-17).
    if (uiState.sessionCheckFailed) {
        Column(modifier = Modifier.fillMaxSize()) {
            ErrorState(
                title = stringResource(Res.string.session_check_failed),
                onRetry = onRetrySessionCheck,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onUseAnotherAccount,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(Res.string.use_another_account))
            }
        }
        return
    }

    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    val wide = isWide()
    val expanded = isExpanded()
    // Hand cursor on clickable elements for web/desktop widths; no-op on phones.
    val hand = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenBackground(MaterialTheme.colorScheme.surface)
    ) { paddingValues ->
        if (wide) {
            // Web: form vertically centered (scrollable when the window is short).
            // Expanded: plain form on the white half next to the brand panel.
            // Medium: form inside a WebCard on the gray canvas.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minHeight)
                        .padding(horizontal = if (expanded) 24.dp else pagePadding(), vertical = 32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (expanded) {
                        LoginFormContent(
                            uiState = uiState,
                            wide = true,
                            showSubtitle = false, // already shown by the brand panel
                            hand = hand,
                            passwordFocusRequester = passwordFocusRequester,
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                            shadowColor = shadowColor,
                            onEmailChange = onEmailChange,
                            onPasswordChange = onPasswordChange,
                            onSubmit = {
                                focusManager.clearFocus()
                                onLoginClick()
                            },
                            onLoginClick = onLoginClick,
                            onForgotPasswordClick = onForgotPasswordClick,
                            onRegisterClick = onRegisterClick
                        )
                    } else {
                        WebCard(contentPadding = 32.dp) {
                            LoginFormContent(
                                uiState = uiState,
                                wide = true,
                                showSubtitle = true,
                                hand = hand,
                                passwordFocusRequester = passwordFocusRequester,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                shadowColor = shadowColor,
                                onEmailChange = onEmailChange,
                                onPasswordChange = onPasswordChange,
                                onSubmit = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                },
                                onLoginClick = onLoginClick,
                                onForgotPasswordClick = onForgotPasswordClick,
                                onRegisterClick = onRegisterClick
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                LoginFormContent(
                    uiState = uiState,
                    wide = false,
                    showSubtitle = true,
                    hand = hand,
                    passwordFocusRequester = passwordFocusRequester,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    shadowColor = shadowColor,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = {
                        focusManager.clearFocus()
                        onLoginClick()
                    },
                    onLoginClick = onLoginClick,
                    onForgotPasswordClick = onForgotPasswordClick,
                    onRegisterClick = onRegisterClick
                )
            }
        }
    }
}

/**
 * Login form body. Shared by the phone layout (unchanged from the original) and the web layouts.
 * Only [wide] alters spacing/cursor; on Compact it renders exactly the original UI.
 */
@Composable
private fun LoginFormContent(
    uiState: LoginUiState,
    wide: Boolean,
    showSubtitle: Boolean,
    hand: Modifier,
    passwordFocusRequester: FocusRequester,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    shadowColor: Color,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    // Header
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (wide) 0.dp else 64.dp,
                bottom = if (wide) 32.dp else 48.dp
            )
    ) {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    LabeledOutlinedField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = stringResource(Res.string.email),
        placeholder = stringResource(Res.string.email_placeholder),
        shape = FieldShape,
        isError = uiState.error != null,
        contentType = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { passwordFocusRequester.requestFocus() }
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    LabeledOutlinedField(
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = stringResource(Res.string.password),
        modifier = Modifier.focusRequester(passwordFocusRequester),
        placeholder = "••••••••",
        shape = FieldShape,
        isError = uiState.error != null,
        contentType = ContentType.Password,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisible, modifier = hand) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (passwordVisible)
                        stringResource(Res.string.hide_password)
                    else
                        stringResource(Res.string.show_password),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (!uiState.isLoading) onSubmit() },
            onGo = { if (!uiState.isLoading) onSubmit() }
        )
    )

    // Error message
    if (uiState.error != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = uiState.error.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            // Announced when it appears, without moving focus (E-L4).
            modifier = Modifier.padding(start = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Login button with orange shadow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = FieldShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
    ) {
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(hand),
            shape = FieldShape,
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.login),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Footer links
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onForgotPasswordClick,
            modifier = hand,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(Res.string.forgot_password_link),
                style = MaterialTheme.typography.labelLarge
            )
        }
        TextButton(
            onClick = onRegisterClick,
            modifier = hand,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(Res.string.dont_have_account),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
