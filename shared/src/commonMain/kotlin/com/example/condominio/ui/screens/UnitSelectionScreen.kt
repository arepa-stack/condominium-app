package com.example.condominio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.UserUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelectionScreen(
    uiState: UnitSelectionUiState,
    onBuildingSelected: (BuildingGroup) -> Unit,
    onUnitSelected: (UserUnit) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    LaunchedEffect(uiState.unitSelected) {
        if (uiState.unitSelected) {
            onNavigateToDashboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Seleccionar Propiedad") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hola, ${uiState.userName}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Selecciona un edificio para continuar:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.buildings) { group ->
                            val isExpanded = uiState.expandedBuildingId == group.buildingId
                            BuildingCard(
                                group = group,
                                isExpanded = isExpanded,
                                onBuildingClick = { onBuildingSelected(group) },
                                onUnitClick = onUnitSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildingCard(
    group: BuildingGroup,
    isExpanded: Boolean,
    onBuildingClick: () -> Unit,
    onUnitClick: (UserUnit) -> Unit
) {
    val roleLabel = when (group.role) {
        "board" -> "Junta Directiva"
        "admin" -> "Administrador"
        else -> "Residente"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBuildingClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.buildingName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (group.role == "board" || group.role == "admin")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (group.units.size == 1) {
                        Text(
                            text = "Unidad: ${group.units.first().unitName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = "${group.units.size} unidades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Icon(
                    imageVector = if (group.units.size > 1 && isExpanded)
                        Icons.Default.ExpandMore
                    else
                        Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Expandable unit list for multi-unit buildings
            AnimatedVisibility(visible = isExpanded && group.units.size > 1) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecciona una unidad:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    group.units.forEach { unit ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUnitClick(unit) }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = unit.unitName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (unit.isPrimary) {
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                "Principal",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
