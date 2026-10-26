package com.example.condominio.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        viewModel.onSubmitClick()
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.forgot_password_title),
                onBackClick = onBackClick
            )
        },
        containerColor = screenBackground(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        if (isWide()) {
            // Web: form vertically centered. Expanded sits on the white half next to the brand
            // panel (no card); Medium sits on the gray canvas, so it is grouped in a WebCard.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = pagePadding(), vertical = 32.dp)
                ) {
                    val content: @Composable ColumnScope.() -> Unit = {
                        if (uiState.isSuccess) {
                            ForgotPasswordSuccess(onBackClick = onBackClick, wide = true)
                        } else {
                            ForgotPasswordForm(
                                uiState = uiState,
                                onEmailChange = viewModel::onEmailChange,
                                onSubmit = submit,
                                onButtonClick = submit,
                                wide = true
                            )
                        }
                    }
                    if (isExpanded()) Column(content = content) else WebCard(contentPadding = 32.dp, content = content)
                }
            }
        } else {
            // Mobile: original layout, unchanged.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isSuccess) {
                    ForgotPasswordSuccess(
                        onBackClick = onBackClick,
                        wide = false,
                        modifier = Modifier.padding(top = 48.dp)
                    )
                    return@Column
                }

                ForgotPasswordForm(
                    uiState = uiState,
                    onEmailChange = viewModel::onEmailChange,
                    onSubmit = submit,
                    onButtonClick = viewModel::onSubmitClick,
                    wide = false
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ForgotPasswordSuccess(
    onBackClick: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.fillMaxWidth().then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.forgot_password_sent),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = stringResource(Res.string.back_to_login),
            onClick = onBackClick,
            modifier = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier
        )
    }
}

@Composable
private fun ForgotPasswordForm(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onButtonClick: () -> Unit,
    wide: Boolean
) {
    Text(
        text = stringResource(Res.string.forgot_password_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 24.dp)
    )

    LabeledOutlinedField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = stringResource(Res.string.email),
        placeholder = stringResource(Res.string.email_placeholder),
        enabled = !uiState.isLoading,
        isError = uiState.error != null,
        contentType = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() }
        ),
        // Web: physical Enter submits (IME actions are not reliable on desktop browsers).
        modifier = if (wide) {
            Modifier.onPreviewKeyEvent { event ->
                if ((event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                    event.type == KeyEventType.KeyDown
                ) {
                    if (!uiState.isLoading) onSubmit()
                    true
                } else {
                    false
                }
            }
        } else {
            Modifier
        }
    )

    uiState.error?.let { error ->
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    PrimaryButton(
        text = stringResource(Res.string.forgot_password_submit),
        onClick = onButtonClick,
        isLoading = uiState.isLoading,
        modifier = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier
    )
}
