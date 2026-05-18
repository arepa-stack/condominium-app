package com.example.condominio.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.ui.theme.*
import condominio.shared.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------
// Status theming helpers (shared between list and detail screens)
// ---------------------------------------------------------------------------

fun decisionStatusAccent(status: DecisionStatus): Color = when (status) {
    DecisionStatus.RECEPTION -> Color(0xFF1565C0)
    DecisionStatus.VOTING -> AptoSecondary
    DecisionStatus.TIEBREAK_PENDING -> AptoStatusWarning
    DecisionStatus.RESOLVED -> AptoStatusSuccess
    DecisionStatus.CANCELLED -> AptoOutline
}

fun decisionStatusBg(status: DecisionStatus): Color = when (status) {
    DecisionStatus.RECEPTION -> AptoPrimaryFixed
    DecisionStatus.VOTING -> AptoSecondaryFixed
    DecisionStatus.TIEBREAK_PENDING -> AptoStatusWarning.copy(alpha = 0.15f)
    DecisionStatus.RESOLVED -> AptoStatusSuccess.copy(alpha = 0.1f)
    DecisionStatus.CANCELLED -> AptoOutlineVariant
}

fun decisionStatusTextColor(status: DecisionStatus): Color = when (status) {
    DecisionStatus.RECEPTION -> Color(0xFF3D494E)
    DecisionStatus.VOTING -> AptoSecondary
    DecisionStatus.TIEBREAK_PENDING -> AptoStatusWarning
    DecisionStatus.RESOLVED -> AptoStatusSuccess
    DecisionStatus.CANCELLED -> AptoOnSurfaceVariant
}

// ---------------------------------------------------------------------------
// DecisionStatusBadge — shared composable for list and detail screens
// ---------------------------------------------------------------------------

@Composable
fun DecisionStatusBadge(status: DecisionStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        DecisionStatus.RECEPTION -> stringResource(Res.string.decisions_status_reception)
        DecisionStatus.VOTING -> stringResource(Res.string.decisions_status_voting)
        DecisionStatus.TIEBREAK_PENDING -> stringResource(Res.string.decisions_status_tiebreak)
        DecisionStatus.RESOLVED -> stringResource(Res.string.decisions_status_resolved)
        DecisionStatus.CANCELLED -> stringResource(Res.string.decisions_status_cancelled)
    }

    Surface(
        color = decisionStatusBg(status),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = decisionStatusTextColor(status),
            letterSpacing = TextUnit(0.8f, TextUnitType.Sp)
        )
    }
}

// ---------------------------------------------------------------------------
// formatDecisionDeadline — shared utility "ISO → dd/MM HH:mm"
// ---------------------------------------------------------------------------

fun formatDecisionDeadline(iso: String?): String {
    if (iso == null) return "—"
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

// ---------------------------------------------------------------------------
// formatDecisionDate — shared utility "ISO → dd/MM/yyyy"
// ---------------------------------------------------------------------------

fun formatDecisionDate(iso: String?): String {
    if (iso == null) return "—"
    return try {
        val instant = Instant.parse(iso)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        "${day}/${month}/${dt.year}"
    } catch (e: Exception) {
        iso
    }
}
