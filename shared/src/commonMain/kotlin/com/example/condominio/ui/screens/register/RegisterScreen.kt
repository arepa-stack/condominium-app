package com.example.condominio.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.AptoSurfaceContainerLow
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val FieldShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    buildingId: String,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    LaunchedEffect(buildingId) {
        if (buildingId.isNotBlank() && uiState.buildingCode != buildingId) {
            viewModel.onBuildingScanned(buildingId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    val wide = isWide()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenBackground(MaterialTheme.colorScheme.surface),
        topBar = {
            if (wide) {
                // Web: the brand panel / nav shell already shows the brand; keep only "back".
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Apto",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.size(40.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            }
        }
    ) { paddingValues ->
        if (uiState.loadFailed) {
            ErrorState(
                title = stringResource(Res.string.error_loading_building),
                onRetry = viewModel::retryLoad,
                modifier = Modifier.padding(paddingValues)
            )
            return@Scaffold
        }

        if (wide) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                RegisterTitle()
                WebCard {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        RegisterFormBody(uiState, viewModel, shadowColor, wide = true)
                    }
                }
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            RegisterTitle()

            RegisterFormBody(uiState, viewModel, shadowColor, wide = false)
        }
        }
    }
}

@Composable
private fun RegisterTitle() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(Res.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Building card, unit selector, fields, error and submit. Emits its items straight into the
 * caller's Column (no wrapper) so the phone layout keeps its original 20dp spacing.
 * [wide] only adds web affordances: hand cursor and Enter-to-submit on the last field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterFormBody(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    shadowColor: Color,
    wide: Boolean
) {
    val hand = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier

    // Building card
    if (uiState.isLoadingBuilding) {
        LoadingState(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            size = 24.dp,
            fullScreen = false
        )
    } else if (uiState.buildingName.isNotBlank()) {
        BuildingCard(name = uiState.buildingName)
    }

    // Unit selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.register_select_unit_label).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        var unitExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = unitExpanded,
            onExpandedChange = {
                if (!uiState.isLoadingUnits && uiState.units.isNotEmpty()) {
                    unitExpanded = !unitExpanded
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.unit,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        if (uiState.isLoadingUnits) stringResource(Res.string.loading)
                        else stringResource(Res.string.select_unit_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = RegisterField.Unit in uiState.missingFields,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).then(hand),
                shape = FieldShape,
                colors = registerFieldColors(),
                enabled = !uiState.isLoadingUnits && uiState.units.isNotEmpty()
            )
            ExposedDropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false }
            ) {
                uiState.units.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.name) },
                        onClick = {
                            viewModel.onUnitChange(unit.id, unit.name)
                            unitExpanded = false
                        },
                        modifier = hand
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabeledOutlinedField(
            value = uiState.firstName,
            onValueChange = viewModel::onFirstNameChange,
            label = stringResource(Res.string.register_first_name_label).uppercase(),
            placeholder = stringResource(Res.string.register_first_name_hint),
            isError = RegisterField.FirstName in uiState.missingFields,
            contentType = ContentType.PersonFirstName,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f)
        )
        LabeledOutlinedField(
            value = uiState.lastName,
            onValueChange = viewModel::onLastNameChange,
            label = stringResource(Res.string.register_last_name_label).uppercase(),
            placeholder = stringResource(Res.string.register_last_name_hint),
            isError = RegisterField.LastName in uiState.missingFields,
            contentType = ContentType.PersonLastName,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f)
        )
    }

    LabeledOutlinedField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        label = stringResource(Res.string.register_email_label).uppercase(),
        placeholder = stringResource(Res.string.register_email_hint),
        isError = RegisterField.Email in uiState.missingFields,
        contentType = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next
        )
    )

    // Enter / the keyboard's Done on the last field submits, on phone and web (E-L3).
    LabeledOutlinedField(
        value = uiState.documentId,
        onValueChange = viewModel::onDocumentIdChange,
        label = stringResource(Res.string.register_document_id_label).uppercase(),
        placeholder = stringResource(Res.string.register_document_id_hint),
        isError = RegisterField.DocumentId in uiState.missingFields,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            if (!uiState.isLoading) viewModel.onRegisterClick()
        })
    )

    // Error
    if (uiState.error != null) {
        Text(
            text = uiState.error!!.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            // Announced when it appears, without moving focus (E-L4).
            modifier = Modifier.padding(start = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }

    // Submit button
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, FieldShape, ambientColor = shadowColor, spotColor = shadowColor)
        ) {
            PrimaryButton(
                text = stringResource(Res.string.register_submit),
                onClick = viewModel::onRegisterClick,
                isLoading = uiState.isLoading,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = hand
            )
        }

        Text(
            text = stringResource(Res.string.register_terms_notice),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BuildingCard(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AptoSurfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏢", style = MaterialTheme.typography.titleLarge)
        }
        Column {
            Text(
                text = stringResource(Res.string.register_building_label).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)
