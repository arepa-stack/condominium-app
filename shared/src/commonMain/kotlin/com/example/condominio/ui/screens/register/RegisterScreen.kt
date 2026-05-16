package com.example.condominio.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.theme.BrandDark
import com.example.condominio.ui.theme.BrandOrange
import com.example.condominio.ui.theme.BorderGray
import com.example.condominio.ui.theme.OrangeShadow
import com.example.condominio.ui.theme.SubtitleGray
import com.example.condominio.ui.theme.SurfaceWhite
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

    LaunchedEffect(buildingId) {
        if (buildingId.isNotBlank() && uiState.buildingCode != buildingId) {
            viewModel.onBuildingScanned(buildingId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceWhite,
        topBar = {
            Surface(
                color = SurfaceWhite,
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
                            tint = BrandDark
                        )
                    }
                    Text(
                        text = "Apto",
                        color = BrandOrange,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(40.dp))
                }
                HorizontalDivider(color = BorderGray)
            }
        }
    ) { paddingValues ->
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.register_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandDark,
                    lineHeight = 34.sp
                )
                Text(
                    text = stringResource(Res.string.register_subtitle),
                    fontSize = 14.sp,
                    color = SubtitleGray
                )
            }

            // Building card
            if (uiState.isLoadingBuilding) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandOrange, modifier = Modifier.size(24.dp))
                }
            } else if (uiState.buildingName.isNotBlank()) {
                BuildingCard(name = uiState.buildingName)
            }

            // Unit selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.register_select_unit_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SubtitleGray,
                    letterSpacing = 0.05.sp,
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
                                color = SubtitleGray
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                                }
                            )
                        }
                    }
                }
            }

            // Name row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.register_first_name_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SubtitleGray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::onFirstNameChange,
                        placeholder = { Text(stringResource(Res.string.register_first_name_hint), color = SubtitleGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FieldShape,
                        colors = registerFieldColors(),
                        singleLine = true
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.register_last_name_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SubtitleGray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChange,
                        placeholder = { Text(stringResource(Res.string.register_last_name_hint), color = SubtitleGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FieldShape,
                        colors = registerFieldColors(),
                        singleLine = true
                    )
                }
            }

            // Email
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.register_email_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SubtitleGray,
                    modifier = Modifier.padding(start = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = { Text(stringResource(Res.string.register_email_hint), color = SubtitleGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    colors = registerFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            // Document ID (cédula / pasaporte)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.register_document_id_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SubtitleGray,
                    modifier = Modifier.padding(start = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.documentId,
                    onValueChange = viewModel::onDocumentIdChange,
                    placeholder = { Text(stringResource(Res.string.register_document_id_hint), color = SubtitleGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    colors = registerFieldColors(),
                    singleLine = true
                )
            }

            // Error
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Submit button
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, FieldShape, ambientColor = OrangeShadow, spotColor = OrangeShadow)
                ) {
                    Button(
                        onClick = viewModel::onRegisterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = FieldShape,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandOrange,
                            contentColor = Color.White,
                            disabledContainerColor = BrandOrange.copy(alpha = 0.6f),
                            disabledContentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.register_submit),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(Res.string.register_terms_notice),
                    fontSize = 12.sp,
                    color = SubtitleGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BuildingCard(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F4F6))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandOrange.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏢", fontSize = 22.sp)
        }
        Column {
            Text(
                text = "EDIFICIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SubtitleGray,
                letterSpacing = 0.05.sp
            )
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandDark
            )
        }
    }
}

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandOrange,
    unfocusedBorderColor = BorderGray,
    cursorColor = BrandOrange,
    focusedTextColor = BrandDark,
    unfocusedTextColor = BrandDark,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite
)
