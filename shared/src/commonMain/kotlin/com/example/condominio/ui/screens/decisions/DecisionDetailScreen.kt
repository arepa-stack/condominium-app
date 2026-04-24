package com.example.condominio.ui.screens.decisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.condominio.data.model.DecisionDetailDto
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.model.QuoteDto
import com.example.condominio.data.model.ResultingType
import com.example.condominio.data.model.TallyDto
import com.example.condominio.data.utils.rememberExternalViewerLauncher
import com.example.condominio.ui.utils.formatCurrency
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// ---------------------------------------------------------------------------
// DecisionDetailScreen — task 4.3
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionDetailScreen(
    decisionId: String,
    onBackClick: () -> Unit,
    viewModel: DecisionDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(decisionId) {
        viewModel.loadDetail(decisionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.decision?.title ?: stringResource(Res.string.decisions_detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            val status = uiState.detail?.decision?.status
            if (status == DecisionStatus.RECEPTION) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openUploadSheet() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.decisions_upload_quote_btn)) }
                )
            }
        }
    ) { paddingValues ->
        val detail = uiState.detail
        when {
            uiState.isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error!!.asString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            detail != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    item { HeaderSection(detail.decision) }
                    item { Spacer(Modifier.height(16.dp)) }

                    if (!detail.decision.photoUrl.isNullOrEmpty()) {
                        item { PhotoSection(detail.decision.photoUrl!!) }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    item {
                        Text(
                            text = stringResource(Res.string.decisions_quotes_section_title, detail.quotes.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }

                    items(detail.quotes) { quote ->
                        QuoteItem(
                            quote = quote,
                            currentUserId = uiState.currentUserId,
                            status = detail.decision.status,
                            onDelete = { viewModel.deleteOwnQuote(quote.id) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item { Spacer(Modifier.height(24.dp)) }

                    item {
                        VoteSectionByState(
                            detail = detail,
                            onVoteClick = { viewModel.openVoteSheet() }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB breathing room
                }
            }
        }

        // Sheets — real implementations created in Phase 4c; stubs compile-safe
        if (uiState.showUploadSheet) {
            UploadQuoteSheet(
                isSubmitting = uiState.isSubmitting,
                error = uiState.submissionError,
                onDismiss = { viewModel.closeUploadSheet() },
                onSubmit = { provider, amount, notes, fileUri, mimeType ->
                    viewModel.submitQuote(provider, amount, notes, fileUri, mimeType)
                }
            )
        }
        if (uiState.showVoteSheet) {
            VoteSheet(
                quotes = uiState.detail?.quotes.orEmpty(),
                isSubmitting = uiState.isSubmitting,
                error = uiState.submissionError,
                onDismiss = { viewModel.closeVoteSheet() },
                onConfirm = { quoteId -> viewModel.submitVote(quoteId) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HeaderSection — private helper
// ---------------------------------------------------------------------------

@Composable
private fun HeaderSection(decision: DecisionDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = decision.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (!decision.description.isNullOrEmpty()) {
            Text(
                text = decision.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
            DecisionStatus.RESOLVED -> resolvedLabel
            DecisionStatus.CANCELLED -> cancelledLabel
        }
        Text(
            text = deadlineLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Status badge — duplicated from DecisionsListScreen.kt
        // TODO: move StatusBadge to a common file shared between screens
        DetailStatusBadge(status = decision.status)
    }
}

// ---------------------------------------------------------------------------
// DetailStatusBadge — minimal duplicate of StatusBadge (private in list screen)
// TODO: extract to common file in Phase 6 refactor
// ---------------------------------------------------------------------------

@Composable
private fun DetailStatusBadge(status: DecisionStatus) {
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
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
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
// PhotoSection — private helper
// ---------------------------------------------------------------------------

@Composable
private fun PhotoSection(photoUrl: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
    }
}

// ---------------------------------------------------------------------------
// QuoteItem — task 4.4 (private)
// ---------------------------------------------------------------------------

@Composable
private fun QuoteItem(
    quote: QuoteDto,
    currentUserId: String?,
    status: DecisionStatus,
    onDelete: () -> Unit
) {
    val externalViewer = rememberExternalViewerLauncher()
    val isMine = quote.uploader?.id == currentUserId && currentUserId != null
    val canDelete = isMine && status == DecisionStatus.RECEPTION && quote.deletedAt == null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(quote.providerName, fontWeight = FontWeight.Bold)
                    Text(
                        "$" + formatCurrency(quote.amount),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val uploaderLabel = if (isMine) {
                        stringResource(Res.string.decisions_quote_uploaded_by_me)
                    } else {
                        stringResource(Res.string.decisions_quote_uploaded_by, quote.uploader?.name ?: "—")
                    }
                    Text(
                        uploaderLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (isMine) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(Res.string.decisions_quote_mine_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!quote.notes.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(quote.notes, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { externalViewer(quote.fileUrl) }) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.decisions_view_file_btn))
                }
                if (canDelete) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.decisions_delete_quote_btn))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.decisions_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.decisions_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(Res.string.decisions_delete_quote_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// VoteSectionByState — task 4.5 (private)
// ---------------------------------------------------------------------------

@Composable
private fun VoteSectionByState(
    detail: DecisionDetailDto,
    onVoteClick: () -> Unit
) {
    val status = detail.decision.status
    val myVote = detail.myVote
    val tally = detail.tally

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (status) {
                DecisionStatus.RECEPTION -> {
                    Text(stringResource(Res.string.decisions_vote_section_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(Res.string.decisions_vote_opens_soon),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                DecisionStatus.VOTING -> {
                    Text(stringResource(Res.string.decisions_vote_in_progress_title), fontWeight = FontWeight.Bold)
                    if (myVote == null) {
                        Text(
                            stringResource(Res.string.decisions_vote_not_cast_yet),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Button(onClick = onVoteClick) {
                            Text(stringResource(Res.string.decisions_vote_btn))
                        }
                    } else {
                        val provider = detail.quotes.find { it.id == myVote.quoteId }?.providerName ?: "—"
                        Text(
                            stringResource(Res.string.decisions_my_vote_label, provider),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TallyBreakdown(tally)
                }

                DecisionStatus.TIEBREAK_PENDING -> {
                    Text(
                        stringResource(Res.string.decisions_tiebreak_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        stringResource(Res.string.decisions_tiebreak_body),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                DecisionStatus.RESOLVED -> {
                    val winner = detail.quotes.find { it.id == detail.decision.winnerQuoteId }
                    Text(
                        stringResource(Res.string.decisions_resolved_winner_title),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    if (winner != null) {
                        Text(winner.providerName)
                        Text(
                            "$" + formatCurrency(winner.amount),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (detail.decision.resultingType != null) {
                        Spacer(Modifier.height(8.dp))
                        val typeLabel = when (detail.decision.resultingType) {
                            ResultingType.INVOICE -> stringResource(Res.string.decisions_resolved_charge_invoice)
                            ResultingType.ASSESSMENT -> stringResource(Res.string.decisions_resolved_charge_assessment)
                        }
                        Text(
                            stringResource(Res.string.decisions_resolved_charge_label, typeLabel),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                DecisionStatus.CANCELLED -> {
                    Text(
                        stringResource(Res.string.decisions_cancelled_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    detail.decision.cancelReason?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.decisions_cancelled_reason_label, it),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TallyBreakdown — private helper (part of task 4.5)
// ---------------------------------------------------------------------------

@Composable
private fun TallyBreakdown(tally: TallyDto) {
    Column {
        if (tally.totalVotes == 0) {
            Text(
                stringResource(Res.string.decisions_tally_waiting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            val votesPctFmt = stringResource(Res.string.decisions_tally_votes_pct)
            tally.tallies.sortedByDescending { it.votes }.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.providerName, modifier = Modifier.weight(1f))
                    Text(
                        votesPctFmt.format(item.votes, item.pct),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (tally.isTied && tally.totalVotes > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.decisions_tally_tie_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.decisions_tally_participation, tally.totalVotes, tally.totalApartments, tally.participationPct.toString()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ---------------------------------------------------------------------------
// formatIsoDeadline — private utility (mirrors DecisionsListScreen.kt)
// ---------------------------------------------------------------------------

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
