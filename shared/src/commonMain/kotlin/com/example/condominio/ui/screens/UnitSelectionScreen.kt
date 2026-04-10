package com.example.condominio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelectionScreen(
    uiState: UnitSelectionUiState,
    onItemSelected: (SelectionItem) -> Unit,
    onUnitSelected: () -> Unit
) {
    LaunchedEffect(uiState.unitSelected) {
        if (uiState.unitSelected) {
            onUnitSelected()
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Por favor selecciona una unidad :",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.items) { item ->
                            val onClick = { onItemSelected(item) }
                            when (item) {
                                is SelectionItem.UnitItem -> {
                                    UnitCard(
                                        title = item.unit.unitName,
                                        subtitle = item.unit.buildingName,
                                        role = "Residente",
                                        onClick = onClick
                                    )
                                }
                                is SelectionItem.RoleItem -> {
                                    UnitCard(
                                        title = "Administración",
                                        subtitle = "Edificio ${item.role.buildingId.take(8)}",
                                        role = item.role.role,
                                        onClick = onClick
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

@Composable
fun UnitCard(title: String, subtitle: String, role: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Rol: $role",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
