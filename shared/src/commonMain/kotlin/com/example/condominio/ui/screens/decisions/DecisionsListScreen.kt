package com.example.condominio.ui.screens.decisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
            TopAppBar(
                title = { Text(stringResource(Res.string.decisions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text(stringResource(Res.string.decisions_filter_all)) }
                )
                FilterChip(
                    selected = uiState.statusFilter == DecisionStatus.RECEPTION,
                    onClick = { viewModel.setStatusFilter(DecisionStatus.RECEPTION) },
                    label = { Text(stringResource(Res.string.decisions_filter_reception)) }
                )
                FilterChip(
                    selected = uiState.statusFilter == DecisionStatus.VOTING,
                    onClick = { viewModel.setStatusFilter(DecisionStatus.VOTING) },
                    label = { Text(stringResource(Res.string.decisions_filter_voting)) }
                )
                FilterChip(
                    selected = uiState.statusFilter == DecisionStatus.RESOLVED,
                    onClick = { viewModel.setStatusFilter(DecisionStatus.RESOLVED) },
                    label = { Text(stringResource(Res.string.decisions_filter_resolved)) }
                )
            }

            // Content area
            when {
                uiState.isLoading && uiState.decisions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.decisions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!.asString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.decisions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(Res.string.decisions_empty))
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.decisions) { decision ->
                            DecisionCard(
                                decision = decision,
                                onClick = { onDecisionClick(decision.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DecisionCard — private helper composable
// ---------------------------------------------------------------------------

@Composable
private fun DecisionCard(
    decision: DecisionDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: title + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = decision.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                StatusBadge(status = decision.status)
            }

            // Row 2: deadline text
            val receptionUntilFmt = stringResource(Res.string.decisions_reception_until)
            val votingUntilFmt = stringResource(Res.string.decisions_voting_until)
            val resolvedLabel = stringResource(Res.string.decisions_status_resolved)
            val cancelledLabel = stringResource(Res.string.decisions_status_cancelled)
            val deadlineLabel = when (decision.status) {
                DecisionStatus.RECEPTION ->
                    receptionUntilFmt.format(formatIsoDeadline(decision.receptionDeadline))
                DecisionStatus.VOTING,
                DecisionStatus.TIEBREAK_PENDING ->
                    votingUntilFmt.format(formatIsoDeadline(decision.votingDeadline))
                DecisionStatus.RESOLVED ->
                    resolvedLabel
                DecisionStatus.CANCELLED ->
                    cancelledLabel
            }
            Text(
                text = deadlineLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Row 3: quote count chip + vote count (if status != RECEPTION)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuoteCountChip(count = decision.quoteCount)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StatusBadge — private helper composable
// ---------------------------------------------------------------------------

@Composable
private fun StatusBadge(status: DecisionStatus) {
    val label = when (status) {
        DecisionStatus.RECEPTION -> stringResource(Res.string.decisions_status_reception)
        DecisionStatus.VOTING -> stringResource(Res.string.decisions_status_voting)
        DecisionStatus.TIEBREAK_PENDING -> stringResource(Res.string.decisions_status_tiebreak)
        DecisionStatus.RESOLVED -> stringResource(Res.string.decisions_status_resolved)
        DecisionStatus.CANCELLED -> stringResource(Res.string.decisions_status_cancelled)
    }
    val color = when (status) {
        DecisionStatus.RECEPTION -> Color(0xFF1565C0)
        DecisionStatus.VOTING -> Color(0xFFE65100)
        DecisionStatus.TIEBREAK_PENDING -> Color(0xFFF9A825)
        DecisionStatus.RESOLVED -> Color(0xFF2E7D32)
        DecisionStatus.CANCELLED -> Color(0xFF757575)
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ---------------------------------------------------------------------------
// QuoteCountChip — private helper composable
// ---------------------------------------------------------------------------

@Composable
private fun QuoteCountChip(count: Int) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(Res.string.decisions_quote_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ---------------------------------------------------------------------------
// formatIsoDeadline — private utility
// ---------------------------------------------------------------------------

/**
 * Parses an ISO-8601 string and returns a "dd/MM HH:mm" formatted string.
 * Falls back to the raw string on parse failure.
 */
private fun formatIsoDeadline(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        val hour = dt.hour.toString().padStart(2, '0')
        val minute = dt.minute.toString().padStart(2, '0')
        "$day/$month $hour:$minute"
    } catch (e: Exception) {
        iso
    }
}
