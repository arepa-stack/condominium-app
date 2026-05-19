package com.example.condominio.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.edit_profile),
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.edit_profile_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LabeledOutlinedField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(Res.string.full_name),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            LabeledOutlinedField(
                value = uiState.apartmentUnit,
                onValueChange = viewModel::onApartmentUnitChange,
                label = stringResource(Res.string.apartment_unit_label),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            LabeledOutlinedField(
                value = uiState.email,
                onValueChange = {},
                label = stringResource(Res.string.email),
                enabled = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.email_cannot_be_changed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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

            // Save Button
            PrimaryButton(
                text = stringResource(Res.string.save_changes),
                onClick = viewModel::onSaveClick,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

