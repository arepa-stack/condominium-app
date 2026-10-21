package com.example.condominio.ui.screens.decisions

import com.example.condominio.ui.components.WebEmptyFrame
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.ui.components.DecisionStatusBadge
import com.example.condominio.ui.components.FilterPillChip
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.example.condominio.ui.components.formatDecisionDate
import com.example.condominio.ui.components.formatDecisionDeadline
import com.example.condominio.ui.theme.*
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.example.condominio.ui.utils.formatArgs
import org.koin.compose.viewmodel.koinViewModel

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionsListScreen(
    onBackClick: () -> Unit,
    onDecisionClick: (String) -> Unit,
    viewModel: DecisionsListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.decisions_title),
                onBackClick = onBackClick,
                actions = {
                    if (isExpanded()) {
                        // Web: labeled action in the page header
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            border = BorderStroke(1.dp, AptoOutlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoOnSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.refresh))
                        }
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.refresh),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DecisionFilterRow(
                selected = uiState.statusFilter,
                onSelect = { viewModel.setStatusFilter(it) }
            )

            when {
                uiState.isLoading && uiState.decisions.isEmpty() -> {
                    LoadingState()
                }

                uiState.error != null && uiState.decisions.isEmpty() -> WebEmptyFrame {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.error!!.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AptoStatusError,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = { viewModel.refresh() },
                                border = BorderStroke(1.dp, AptoSecondary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoSecondary)
                            ) {
                                Text(stringResource(Res.string.billboard_retry))
                            }
                        }
                    }
                }

                uiState.decisions.isEmpty() -> WebEmptyFrame {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.decisions_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AptoOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    val wide = isWide()
                    val gridSpacing = if (wide) 24.dp else 16.dp
                    val cardModifier = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(340.dp),
                        contentPadding = if (wide) {
                            PaddingValues(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 32.dp)
                        } else {
                            PaddingValues(16.dp)
                        },
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                    ) {
                        items(uiState.decisions) { decision ->
                            DecisionCard(
                                decision = decision,
                                onClick = { onDecisionClick(decision.id) },
                                modifier = cardModifier
                            )
                        }
                        if (uiState.isLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LoadingState(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    size = 24.dp,
                                    fullScreen = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Filter row — reuses FilterPillChip from components
// ---------------------------------------------------------------------------

@Composable
private fun DecisionFilterRow(
    selected: DecisionStatus?,
    onSelect: (DecisionStatus?) -> Unit
) {
    if (isWide()) {
        // Web: every chip visible in one static row, aligned with the page content
        val hand = Modifier.pointerHoverIcon(PointerIcon.Hand)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                // 8dp under the header; + the grid's / WebEmptyFrame's 8dp top = 16dp to the content.
                .padding(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_all),
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = hand
            )
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_voting),
                selected = selected == DecisionStatus.VOTING || selected == DecisionStatus.TIEBREAK_PENDING,
                onClick = { onSelect(if (selected == DecisionStatus.VOTING) null else DecisionStatus.VOTING) },
                modifier = hand
            )
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_resolved),
                selected = selected == DecisionStatus.RESOLVED,
                onClick = { onSelect(if (selected == DecisionStatus.RESOLVED) null else DecisionStatus.RESOLVED) },
                modifier = hand
            )
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_reception),
                selected = selected == DecisionStatus.RECEPTION,
                onClick = { onSelect(if (selected == DecisionStatus.RECEPTION) null else DecisionStatus.RECEPTION) },
                modifier = hand
            )
        }
        return
    }

    LazyRow(
        modifier = Modifier.selectableGroup(),
        // 6dp + the chips' 48dp touch target = the previous 12dp + 36dp pill: same row height.
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_all),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        item {
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_voting),
                selected = selected == DecisionStatus.VOTING || selected == DecisionStatus.TIEBREAK_PENDING,
                onClick = {
                    onSelect(if (selected == DecisionStatus.VOTING) null else DecisionStatus.VOTING)
                }
            )
        }
        item {
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_resolved),
                selected = selected == DecisionStatus.RESOLVED,
                onClick = {
                    onSelect(if (selected == DecisionStatus.RESOLVED) null else DecisionStatus.RESOLVED)
                }
            )
        }
        item {
            FilterPillChip(
                label = stringResource(Res.string.decisions_filter_reception),
                selected = selected == DecisionStatus.RECEPTION,
                onClick = {
                    onSelect(if (selected == DecisionStatus.RECEPTION) null else DecisionStatus.RECEPTION)
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// DecisionCard
// ---------------------------------------------------------------------------

@Composable
private fun DecisionCard(
    decision: DecisionDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = decision.status == DecisionStatus.VOTING ||
            decision.status == DecisionStatus.RECEPTION ||
            decision.status == DecisionStatus.TIEBREAK_PENDING

    ListItemCard(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {

            // Header: title + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = decision.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AptoOnSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                DecisionStatusBadge(status = decision.status)
            }

            Spacer(Modifier.height(4.dp))

            // Subtitle: deadline or finalization date
            Text(
                text = buildDeadlineLabel(decision),
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Footer: quote chip + divider + action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quote count chip (left)
                DecisionQuoteChip(
                    count = decision.quoteCount,
                    resolved = decision.status == DecisionStatus.RESOLVED
                )

                // Thin divider line (fill remaining space)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(color = AptoOutlineVariant)
                }

                // Right action: urgent warning OR "Ver detalle" link
                if (isActive && decision.isDeadlinePassed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AptoStatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.decisions_urgent),
                            style = MaterialTheme.typography.labelLarge,
                            color = AptoStatusWarning
                        )
                    }
                } else if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.decisions_see_detail),
                            style = MaterialTheme.typography.labelLarge,
                            color = AptoSecondary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AptoSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// DecisionStatusBadge is imported from com.example.condominio.ui.components

// ---------------------------------------------------------------------------
// DecisionQuoteChip
// ---------------------------------------------------------------------------

@Composable
private fun DecisionQuoteChip(count: Int, resolved: Boolean) {
    val bgColor = if (resolved) AptoSurfaceContainerLow else AptoPrimaryFixed
    val contentColor = if (resolved) AptoOnSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = pluralStringResource(Res.plurals.decisions_quote_count, count, count),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun buildDeadlineLabel(decision: DecisionDto): String {
    val receptionFmt = stringResource(Res.string.decisions_reception_until)
    val votingFmt = stringResource(Res.string.decisions_voting_until)
    val resolvedFmt = stringResource(Res.string.decisions_finalized_on)
    val cancelledLabel = stringResource(Res.string.decisions_status_cancelled)

    return when (decision.status) {
        DecisionStatus.RECEPTION ->
            receptionFmt.formatArgs(formatDecisionDeadline(decision.receptionDeadline))
        DecisionStatus.VOTING,
        DecisionStatus.TIEBREAK_PENDING ->
            votingFmt.formatArgs(formatDecisionDeadline(decision.votingDeadline))
        DecisionStatus.RESOLVED ->
            resolvedFmt.formatArgs(
                decision.finalizedAt?.let { formatDecisionDate(it) }
                    ?: formatDecisionDeadline(decision.votingDeadline)
            )
        DecisionStatus.CANCELLED -> cancelledLabel
    }
}

