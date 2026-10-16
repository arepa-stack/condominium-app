package com.example.condominio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.condominio.ui.components.formWidth
import androidx.compose.ui.graphics.compositeOver
import com.example.condominio.ui.utils.isWebPlatform
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.data.model.UserUnit
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.*
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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

    if (isWide()) {
        WideUnitSelection(
            uiState = uiState,
            onBuildingSelected = onBuildingSelected,
            onUnitSelected = onUnitSelected
        )
        return
    }

    Scaffold(
        containerColor = AptoBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.select_property_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AptoSurface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GreetingHeader(userName = uiState.userName)
                        Spacer(Modifier.height(8.dp))
                    }

                    items(uiState.buildings) { group ->
                        BuildingCard(
                            group = group,
                            isExpanded = uiState.expandedBuildingId == group.buildingId,
                            onBuildingClick = { onBuildingSelected(group) },
                            onUnitClick = onUnitSelected
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GreetingHeader
// ---------------------------------------------------------------------------

@Composable
private fun GreetingHeader(userName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(Res.string.greeting_user, userName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AptoOnSurface
        )
        Text(
            text = stringResource(Res.string.select_building_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AptoOnSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// BuildingCard
// ---------------------------------------------------------------------------

@Composable
fun BuildingCard(
    group: BuildingGroup,
    isExpanded: Boolean,
    onBuildingClick: () -> Unit,
    onUnitClick: (UserUnit) -> Unit
) {
    val roleLabel = when (group.role) {
        "board" -> stringResource(Res.string.role_board)
        "admin" -> stringResource(Res.string.role_admin)
        else -> stringResource(Res.string.role_resident)
    }
    val isPrivileged = group.role == "board" || group.role == "admin"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBuildingClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AptoSurfaceContainerLowest),
        border = BorderStroke(1.dp, AptoOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Main row: icon + info + chevron ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Building icon avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isPrivileged) AptoSecondaryFixed else AptoPrimaryFixed,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = if (isPrivileged) AptoSecondary else AptoOutline,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                    Spacer(Modifier.height(2.dp))

                    // Role badge
                    Surface(
                        color = if (isPrivileged) AptoSecondaryFixed else AptoSurfaceContainerHigh,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPrivileged) AptoSecondary else AptoOnSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (group.units.size == 1)
                            stringResource(Res.string.unit_label_short, group.units.first().unitName)
                        else
                            stringResource(Res.string.units_count_label, group.units.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoOnSurfaceVariant
                    )
                }

                // Chevron
                Icon(
                    imageVector = when {
                        group.units.size > 1 && isExpanded -> Icons.Default.ExpandLess
                        group.units.size > 1 -> Icons.Default.ExpandMore
                        else -> Icons.Default.ChevronRight
                    },
                    contentDescription = null,
                    tint = AptoSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            // ── Expandable unit list ──────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded && group.units.size > 1,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = AptoOutlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.select_unit_hint),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AptoOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.units.forEach { unit ->
                            UnitRow(unit = unit, onClick = { onUnitClick(unit) })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// UnitRow — single selectable unit inside an expanded building card
// ---------------------------------------------------------------------------

@Composable
private fun UnitRow(unit: UserUnit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AptoSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AptoPrimaryFixed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = null,
                tint = AptoOutline,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = unit.unitName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AptoOnSurface,
            modifier = Modifier.weight(1f)
        )

        if (unit.isPrimary) {
            Surface(
                color = AptoSecondaryFixed,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.primary_label),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AptoSecondary
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AptoSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Wide layouts (Medium / Expanded): page header + selectable tiles on the web canvas.
// Compact keeps the original TopAppBar + LazyColumn above, untouched.
// ---------------------------------------------------------------------------

@Composable
private fun WideUnitSelection(
    uiState: UnitSelectionUiState,
    onBuildingSelected: (BuildingGroup) -> Unit,
    onUnitSelected: (UserUnit) -> Unit
) {
    Scaffold(containerColor = screenBackground(AptoBackground)) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingState()
            } else {
                val selectedGroup = uiState.buildings.firstOrNull { it.buildingId == uiState.expandedBuildingId }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = pagePadding(), vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Page header (replaces the mobile TopAppBar)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(Res.string.select_property_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AptoOnSurface
                        )
                        Text(
                            text = stringResource(Res.string.greeting_user, uiState.userName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AptoOnSurface
                        )
                        Text(
                            text = stringResource(Res.string.select_building_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AptoOnSurfaceVariant
                        )
                    }

                    // Tiles and the units card share one width (formWidth): nothing jumps when a building expands.
                    AdaptiveTileGrid(items = uiState.buildings, minCellWidth = 190.dp, modifier = Modifier.formWidth()) { group ->
                        BuildingTile(
                            group = group,
                            isSelected = group.buildingId == uiState.expandedBuildingId,
                            onClick = { onBuildingSelected(group) }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedGroup != null && selectedGroup.units.size > 1,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        if (selectedGroup != null) {
                            WebCard(modifier = Modifier.formWidth()) {
                                Text(
                                    text = selectedGroup.buildingName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AptoOnSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.select_unit_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AptoOnSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                AdaptiveTileGrid(items = selectedGroup.units, minCellWidth = 160.dp) { unit ->
                                    UnitTile(unit = unit, onClick = { onUnitSelected(unit) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Two columns when both fit at [minCellWidth], one otherwise; cells in a row share height.
 * A single item takes the whole row, so it lines up with the full-width card below it.
 */
@Composable
private fun <T> AdaptiveTileGrid(
    items: List<T>,
    minCellWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    cell: @Composable (T) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (items.size > 1 && maxWidth >= minCellWidth * 2 + spacing) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowItems.forEach { item ->
                        Box(Modifier.weight(1f).fillMaxHeight()) { cell(item) }
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BuildingTile(
    group: BuildingGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val roleLabel = when (group.role) {
        "board" -> stringResource(Res.string.role_board)
        "admin" -> stringResource(Res.string.role_admin)
        else -> stringResource(Res.string.role_resident)
    }
    val isPrivileged = group.role == "board" || group.role == "admin"
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AptoSecondaryFixed.copy(alpha = 0.35f).compositeOver(Color.White) else AptoSurfaceContainerLowest
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> AptoSecondaryContainer
                isHovered -> if (isWebPlatform) AptoSecondaryContainer.copy(alpha = 0.4f) else AptoOutline
                else -> AptoOutlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered && !isSelected) (if (isWebPlatform) 1.dp else 3.dp) else 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isPrivileged) AptoSecondaryFixed else AptoPrimaryFixed,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = if (isPrivileged) AptoSecondary else AptoOutline,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = when {
                        isSelected -> Icons.Default.CheckCircle
                        group.units.size > 1 -> Icons.Default.ExpandMore
                        else -> Icons.Default.ChevronRight
                    },
                    contentDescription = null,
                    tint = AptoSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = group.buildingName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AptoOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                color = if (isPrivileged) AptoSecondaryFixed else AptoSurfaceContainerHigh,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = roleLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPrivileged) AptoSecondary else AptoOnSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (group.units.size == 1)
                    stringResource(Res.string.unit_label_short, group.units.first().unitName)
                else
                    stringResource(Res.string.units_count_label, group.units.size),
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnitTile(unit: UserUnit, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            // Opaque on purpose: Card adds a hover shadow, which shows through a translucent container.
            containerColor = if (isHovered) AptoSecondaryFixed.copy(alpha = 0.35f).compositeOver(Color.White) else AptoSurfaceContainerLow
        ),
        border = BorderStroke(1.dp, if (isHovered) AptoSecondaryContainer else AptoOutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AptoPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = AptoOutline,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.unitName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AptoOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isPrimary) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = AptoSecondaryFixed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.primary_label),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AptoSecondary
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AptoSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
