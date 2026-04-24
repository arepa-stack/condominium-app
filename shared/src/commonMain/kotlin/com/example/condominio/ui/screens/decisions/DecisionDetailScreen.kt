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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.datetime.Clock
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
                val rankByQuoteId: Map<String, Int> = remember(detail.tally) {
                    detail.tally.tallies
                        .sortedByDescending { it.votes }
                        .mapIndexedNotNull { index, t ->
                            if (t.votes > 0) t.quoteId to (index + 1) else null
                        }
                        .toMap()
                }
                val showRank = detail.decision.status == DecisionStatus.VOTING ||
                        detail.decision.status == DecisionStatus.TIEBREAK_PENDING
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
        val activeIso = when (decision.status) {
            DecisionStatus.RECEPTION -> decision.receptionDeadline
            DecisionStatus.VOTING, DecisionStatus.TIEBREAK_PENDING -> decision.votingDeadline
            else -> null
        }
        val hoursLeft = activeIso?.let { hoursUntilDeadline(it) }
        val deadlineLabel = when (decision.status) {
            DecisionStatus.RECEPTION ->
                receptionUntilFmt.format(formatIsoDeadline(decision.receptionDeadline))
            DecisionStatus.VOTING,
            DecisionStatus.TIEBREAK_PENDING ->
                votingUntilFmt.format(formatIsoDeadline(decision.votingDeadline))
            DecisionStatus.RESOLVED -> resolvedLabel
            DecisionStatus.CANCELLED -> cancelledLabel
        }
        val deadlineColor = when {
            hoursLeft == null -> MaterialTheme.colorScheme.onSurfaceVariant
            hoursLeft <= 24L -> MaterialTheme.colorScheme.error
            hoursLeft <= 72L -> Color(0xFFE65100)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = deadlineLabel,
            style = MaterialTheme.typography.bodySmall,
            color = deadlineColor,
            fontWeight = if (hoursLeft != null && hoursLeft <= 72L) FontWeight.SemiBold else FontWeight.Normal
        )
        if (hoursLeft != null && hoursLeft in 0L..24L) {
            Text(
                text = stringResource(Res.string.decisions_deadline_urgent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

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
    rank: Int? = null,
    isWinner: Boolean = false,
    onDelete: () -> Unit
) {
    val externalViewer = rememberExternalViewerLauncher()
    val isMine = quote.uploader?.id == currentUserId && currentUserId != null
    val canDelete = isMine && status == DecisionStatus.RECEPTION && quote.deletedAt == null
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val accent = when {
        isWinner -> Color(0xFF2E7D32)
        rank != null && rank <= rankPalette.size -> rankPalette[rank - 1]
        else -> null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) {
                Color(0xFF2E7D32).copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (accent != null) {
            BorderStroke(if (isWinner) 2.dp else 1.dp, accent.copy(alpha = if (isWinner) 1f else 0.5f))
        } else null
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
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.decisions_quote_rank_winner),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
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
                    val winnerGreen = Color(0xFF2E7D32)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(winnerGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = winnerGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.decisions_resolved_winner_title),
                                fontWeight = FontWeight.Bold,
                                color = winnerGreen
                            )
                            if (winner != null) {
                                Text(
                                    winner.providerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "$" + formatCurrency(winner.amount),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (detail.decision.resultingType != null) {
                        Spacer(Modifier.height(12.dp))
                        val typeLabel = when (detail.decision.resultingType) {
                            ResultingType.INVOICE -> stringResource(Res.string.decisions_resolved_charge_invoice)
                            ResultingType.ASSESSMENT -> stringResource(Res.string.decisions_resolved_charge_assessment)
                        }
                        Text(
                            stringResource(Res.string.decisions_resolved_charge_label, typeLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TallyBreakdown(detail.tally)
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
// TallyBreakdown — rich viz: animated bars + participation donut
// ---------------------------------------------------------------------------

private val rankPalette = listOf(
    Color(0xFFFFC107), // gold
    Color(0xFFB0BEC5), // silver
    Color(0xFFCD7F32)  // bronze
)
private val neutralBar = Color(0xFF90A4AE)

@Composable
private fun TallyBreakdown(tally: TallyDto) {
    Column {
        if (tally.totalVotes == 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.decisions_tally_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.decisions_tally_tie_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
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
                color = MaterialTheme.colorScheme.onSurface
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
        participationPct >= 75.0 -> Color(0xFF2E7D32)
        participationPct >= 50.0 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
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

private fun hoursUntilDeadline(iso: String): Long? {
    return try {
        val target = Instant.parse(iso)
        val now = Clock.System.now()
        (target - now).inWholeHours
    } catch (e: Exception) {
        null
    }
}
