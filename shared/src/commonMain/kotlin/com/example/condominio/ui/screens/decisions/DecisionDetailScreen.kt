package com.example.condominio.ui.screens.decisions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.condominio.data.model.DecisionDetailDto
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.model.QuoteDto
import com.example.condominio.data.model.ResultingType
import com.example.condominio.data.model.TallyDto
import com.example.condominio.data.utils.rememberExternalViewerLauncher
import com.example.condominio.ui.components.DecisionStatusBadge
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.formatDecisionDeadline
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
        containerColor = AptoBackground,
        topBar = {
            TopBarWithBack(
                title = uiState.detail?.decision?.title
                    ?: stringResource(Res.string.decisions_detail_fallback_title),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            val status = uiState.detail?.decision?.status
            if (status == DecisionStatus.RECEPTION) {
                Surface(
                    color = AptoSurface,
                    shadowElevation = 8.dp
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.decisions_upload_quote_btn),
                        onClick = { viewModel.openUploadSheet() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        icon = Icons.Default.Add,
                        containerColor = AptoSecondaryContainer,
                        contentColor = AptoOnSecondary
                    )
                }
            }
        }
    ) { paddingValues ->
        val detail = uiState.detail
        when {
            uiState.isLoading && detail == null -> {
                LoadingState(modifier = Modifier.padding(paddingValues))
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
                        color = AptoStatusError
                    )
                }
            }

            detail != null -> {
                val rankByQuoteId: Map<String, Int> = remember(detail.tally) {
                    detail.tally.tallies
                        .sortedByDescending { it.votes }
                        .mapIndexedNotNull { index, t ->
                            if (t.votes > 0) t.quoteId to (index + 1) else null
                        }
                        .toMap()
                }
                val showRank = detail.decision.status == DecisionStatus.VOTING ||
                        detail.decision.status == DecisionStatus.TIEBREAK_PENDING ||
                        detail.decision.status == DecisionStatus.RESOLVED
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    item { HeaderSection(detail.decision) }
                    item { Spacer(Modifier.height(12.dp)) }

                    if (detail.decision.status == DecisionStatus.RESOLVED) {
                        val winnerQuote = detail.quotes.find { it.id == detail.decision.winnerQuoteId }
                        if (winnerQuote != null) {
                            item {
                                WinnerHeroCard(
                                    decision = detail.decision,
                                    winner = winnerQuote,
                                    tally = detail.tally
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }

                    val showDeadline = detail.decision.status == DecisionStatus.RECEPTION ||
                            detail.decision.status == DecisionStatus.VOTING ||
                            detail.decision.status == DecisionStatus.TIEBREAK_PENDING
                    if (showDeadline) {
                        item { DeadlineCard(detail.decision) }
                        item { Spacer(Modifier.height(12.dp)) }
                    }

                    if (detail.decision.status == DecisionStatus.RECEPTION ||
                        detail.decision.status == DecisionStatus.VOTING
                    ) {
                        item { PhaseInfoCard(detail.decision.status) }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

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

                    if (detail.quotes.isEmpty()) {
                        item { QuotesEmptyState() }
                    } else {
                        items(detail.quotes) { quote ->
                            val isWinner = detail.decision.winnerQuoteId == quote.id &&
                                    detail.decision.status == DecisionStatus.RESOLVED
                            val rank = if (showRank) rankByQuoteId[quote.id] else null
                            QuoteItem(
                                quote = quote,
                                currentUserId = uiState.currentUserId,
                                status = detail.decision.status,
                                rank = rank,
                                isWinner = isWinner,
                                onDelete = { viewModel.deleteOwnQuote(quote.id) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }

                    item {
                        VoteSectionByState(
                            detail = detail,
                            onVoteClick = { viewModel.openVoteSheet() }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
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
        DecisionStatusBadge(status = decision.status)

        Text(
            text = decision.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AptoOnSurface
        )

        if (!decision.description.isNullOrEmpty()) {
            Text(
                text = decision.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// DeadlineCard — visual countdown with icon + urgency badge
// ---------------------------------------------------------------------------

@Composable
private fun DeadlineCard(decision: DecisionDto) {
    val activeIso = when (decision.status) {
        DecisionStatus.RECEPTION -> decision.receptionDeadline
        DecisionStatus.VOTING, DecisionStatus.TIEBREAK_PENDING -> decision.votingDeadline
        else -> null
    } ?: return

    val hoursLeft = hoursUntilDeadline(activeIso)
    val color = when {
        hoursLeft == null -> AptoSecondary
        hoursLeft < 0L -> AptoStatusError
        hoursLeft <= 24L -> AptoStatusError
        hoursLeft <= 72L -> AptoSecondaryContainer
        else -> AptoSecondary
    }
    val urgent = hoursLeft != null && hoursLeft in 0L..24L
    val timeText = when {
        hoursLeft == null -> "—"
        hoursLeft < 0L -> stringResource(Res.string.decisions_deadline_passed)
        hoursLeft < 48L -> stringResource(Res.string.decisions_deadline_hours, hoursLeft.toInt())
        else -> stringResource(Res.string.decisions_deadline_days, (hoursLeft / 24L).toInt())
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.decisions_deadline_card_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = AptoOnSurfaceVariant
                )
                Text(
                    timeText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    formatDecisionDeadline(activeIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = AptoOnSurfaceVariant
                )
            }
            if (urgent) {
                Box(
                    modifier = Modifier
                        .background(color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(Res.string.decisions_deadline_urgent),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PhaseInfoCard — contextual explainer for active phase
// ---------------------------------------------------------------------------

@Composable
private fun PhaseInfoCard(status: DecisionStatus) {
    val title: String
    val body: String
    val accent: Color
    when (status) {
        DecisionStatus.RECEPTION -> {
            title = stringResource(Res.string.decisions_phase_reception_title)
            body = stringResource(Res.string.decisions_phase_reception_body)
            accent = AptoCategoryBlue
        }
        DecisionStatus.VOTING -> {
            title = stringResource(Res.string.decisions_phase_voting_title)
            body = stringResource(Res.string.decisions_phase_voting_body)
            accent = MaterialTheme.colorScheme.primary
        }
        else -> return
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = AptoOnSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// QuotesEmptyState — visual empty state when no quotes uploaded yet
// ---------------------------------------------------------------------------

@Composable
private fun QuotesEmptyState() {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AptoSurfaceContainerLow),
        border = BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AptoPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = AptoSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.decisions_quotes_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(Res.string.decisions_quotes_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// DecisionStatusBadge is imported from com.example.condominio.ui.components

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
    rank: Int? = null,
    isWinner: Boolean = false,
    onDelete: () -> Unit
) {
    val externalViewer = rememberExternalViewerLauncher()
    val isMine = quote.uploader?.id == currentUserId && currentUserId != null
    val canDelete = isMine && status == DecisionStatus.RECEPTION && quote.deletedAt == null
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val accent = when {
        isWinner -> AptoSuccess
        rank != null && rank <= rankPalette.size -> rankPalette[rank - 1]
        else -> null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) {
                AptoSuccess.copy(alpha = 0.08f)
            } else {
                AptoSurfaceContainerLowest
            }
        ),
        border = if (accent != null) {
            BorderStroke(if (isWinner) 2.dp else 1.dp, accent.copy(alpha = if (isWinner) 1f else 0.5f))
        } else {
            BorderStroke(1.dp, AptoOutlineVariant)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isWinner) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AptoSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.decisions_quote_rank_winner),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AptoSuccess
                    )
                }
            } else if (rank != null && accent != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    RankBadge(rank = rank, tint = accent)
                    if (rank == 1) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(Res.string.decisions_quote_rank_leading),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quote.providerName,
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                    Text(
                        "$" + formatCurrency(quote.amount),
                        color = AptoSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    val uploaderLabel = if (isMine) {
                        stringResource(Res.string.decisions_quote_uploaded_by_me)
                    } else {
                        stringResource(Res.string.decisions_quote_uploaded_by, quote.uploader?.name ?: "—")
                    }
                    Text(
                        uploaderLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoOnSurfaceVariant
                    )
                }
                if (isMine) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = AptoSecondaryFixed,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(Res.string.decisions_quote_mine_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AptoSecondary
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
                            contentColor = AptoStatusError
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AptoSurfaceContainerLowest),
        border = BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (status) {
                DecisionStatus.RECEPTION -> {
                    Text(
                        stringResource(Res.string.decisions_vote_section_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                    Text(
                        stringResource(Res.string.decisions_vote_opens_soon),
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoOnSurfaceVariant
                    )
                }

                DecisionStatus.VOTING -> {
                    Text(
                        stringResource(Res.string.decisions_vote_in_progress_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                    if (myVote == null) {
                        Text(
                            stringResource(Res.string.decisions_vote_not_cast_yet),
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = AptoOnSurfaceVariant
                        )
                        Button(
                            onClick = onVoteClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AptoSecondaryContainer,
                                contentColor = AptoOnSecondary
                            )
                        ) {
                            Text(stringResource(Res.string.decisions_vote_btn))
                        }
                    } else {
                        val provider = detail.quotes.find { it.id == myVote.quoteId }?.providerName ?: "—"
                        Text(
                            stringResource(Res.string.decisions_my_vote_label, provider),
                            color = AptoSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TallyBreakdown(tally)
                }

                DecisionStatus.TIEBREAK_PENDING -> {
                    Text(
                        stringResource(Res.string.decisions_tiebreak_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoStatusWarning
                    )
                    Text(
                        stringResource(Res.string.decisions_tiebreak_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoOnSurfaceVariant
                    )
                }

                DecisionStatus.RESOLVED -> {
                    Text(
                        stringResource(Res.string.decisions_resolved_results_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoOnSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    TallyBreakdown(detail.tally)
                    if (detail.decision.resultingType != null) {
                        Spacer(Modifier.height(12.dp))
                        val typeLabel = when (detail.decision.resultingType) {
                            ResultingType.INVOICE -> stringResource(Res.string.decisions_resolved_charge_invoice)
                            ResultingType.ASSESSMENT -> stringResource(Res.string.decisions_resolved_charge_assessment)
                        }
                        Text(
                            stringResource(Res.string.decisions_resolved_charge_label, typeLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = AptoOnSurfaceVariant
                        )
                    }
                }

                DecisionStatus.CANCELLED -> {
                    Text(
                        stringResource(Res.string.decisions_cancelled_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoStatusError
                    )
                    detail.decision.cancelReason?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.decisions_cancelled_reason_label, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = AptoOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TallyBreakdown — rich viz: animated bars + participation donut
// ---------------------------------------------------------------------------

private val rankPalette = listOf(
    AptoMedalGold, // gold
    AptoMedalSilver, // silver
    AptoMedalBronze  // bronze
)
private val neutralBar = AptoMedalNeutral

@Composable
private fun TallyBreakdown(tally: TallyDto) {
    Column {
        if (tally.totalVotes == 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = AptoOutline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.decisions_tally_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = AptoOnSurfaceVariant
                )
            }
        } else {
            val sorted = tally.tallies.sortedByDescending { it.votes }
            val maxPct = (sorted.firstOrNull()?.pct ?: 0.0).coerceAtLeast(1.0)
            sorted.forEachIndexed { index, item ->
                TallyBar(
                    rank = index + 1,
                    providerName = item.providerName,
                    votes = item.votes,
                    pct = item.pct,
                    relativeFill = (item.pct / maxPct).toFloat().coerceIn(0f, 1f),
                    barColor = if (index < rankPalette.size) rankPalette[index] else neutralBar
                )
                Spacer(Modifier.height(8.dp))
            }
            if (tally.isTied) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AptoStatusWarning, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.decisions_tally_tie_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoStatusWarning,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ParticipationDonut(
            totalVotes = tally.totalVotes,
            totalApartments = tally.totalApartments,
            participationPct = tally.participationPct
        )
    }
}

@Composable
private fun TallyBar(
    rank: Int,
    providerName: String,
    votes: Int,
    pct: Double,
    relativeFill: Float,
    barColor: Color
) {
    val animatedFill by animateFloatAsState(
        targetValue = relativeFill,
        animationSpec = tween(durationMillis = 700),
        label = "tally-bar-$rank"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank = rank, tint = barColor)
            Spacer(Modifier.width(8.dp))
            Text(
                providerName,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(Res.string.decisions_tally_votes_pct).format(votes, pct),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AptoOnSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(barColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFill)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.75f), barColor)
                        )
                    )
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int, tint: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(tint.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "#$rank",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun ParticipationDonut(
    totalVotes: Int,
    totalApartments: Int,
    participationPct: Double
) {
    val target = (participationPct / 100.0).toFloat().coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900),
        label = "participation-donut"
    )
    val ringColor = when {
        participationPct >= 75.0 -> AptoSuccess
        participationPct >= 50.0 -> AptoSecondaryContainer
        else -> AptoOutline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = ringColor.copy(alpha = 0.15f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    style = stroke
                )
            }
            Text(
                "${participationPct.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(Res.string.decisions_tally_participation_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(
                    Res.string.decisions_tally_participation_count,
                    totalVotes,
                    totalApartments
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// WinnerHeroCard — celebratory top card for RESOLVED state
// ---------------------------------------------------------------------------

@Composable
private fun WinnerHeroCard(
    decision: DecisionDto,
    winner: QuoteDto,
    tally: TallyDto
) {
    val gold = AptoMedalGold
    val green = AptoSuccess
    val winnerVotes = tally.tallies.find { it.quoteId == winner.id }
    val votes = winnerVotes?.votes ?: 0
    val pct = winnerVotes?.pct ?: 0.0

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = green.copy(alpha = 0.10f)),
        border = BorderStroke(2.dp, green.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            gold.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(gold.copy(alpha = 0.55f), gold.copy(alpha = 0.15f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AptoMedalGoldDark,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .background(green, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(Res.string.decisions_resolved_hero_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                winner.providerName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "$" + formatCurrency(winner.amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AptoSecondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(Res.string.decisions_resolved_winner_votes, votes, pct.toInt().toString()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface
            )

            decision.finalizedAt?.let { iso ->
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        Res.string.decisions_resolved_finalized_at,
                        formatDecisionDeadline(iso)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AptoOnSurfaceVariant
                )
            }
        }
    }
}

// formatDecisionDeadline is imported from com.example.condominio.ui.components

private fun hoursUntilDeadline(iso: String): Long? {
    return try {
        val target = Instant.parse(iso)
        val now = Clock.System.now()
        (target - now).inWholeHours
    } catch (e: Exception) {
        null
    }
}
