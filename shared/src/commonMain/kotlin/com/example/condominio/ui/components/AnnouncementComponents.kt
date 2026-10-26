package com.example.condominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.ui.theme.*
import condominio.shared.generated.resources.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------
// Category theming helpers — public so Detail screen can reuse them
// ---------------------------------------------------------------------------

fun announcementCategoryAccent(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT -> AptoStatusError
    AnnouncementCategory.FINANCIAL -> brand(0xFF1565C0, 0xFF0369A1)
    AnnouncementCategory.MAINTENANCE -> brand(0xFF2E7D32, 0xFF15803D)
    AnnouncementCategory.NEWS -> brand(0xFF00695C, 0xFF0F766E)
    AnnouncementCategory.INFO -> brand(0xFF546066, 0xFF6A6662)
}

fun announcementCategoryLabelRes(category: AnnouncementCategory): StringResource = when (category) {
    AnnouncementCategory.URGENT -> Res.string.billboard_category_urgent
    AnnouncementCategory.FINANCIAL -> Res.string.billboard_category_financial
    AnnouncementCategory.MAINTENANCE -> Res.string.billboard_category_maintenance
    AnnouncementCategory.NEWS -> Res.string.billboard_category_news
    AnnouncementCategory.INFO -> Res.string.billboard_category_info
}

@Composable
fun announcementCategoryLabel(category: AnnouncementCategory): String =
    stringResource(announcementCategoryLabelRes(category))

private fun categoryBadgeBg(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT -> AptoStatusError
    AnnouncementCategory.FINANCIAL -> Color(0xFF1565C0)
    AnnouncementCategory.MAINTENANCE -> Color(0xFFE8F5E9)
    AnnouncementCategory.NEWS -> Color(0xFFE0F2F1)
    AnnouncementCategory.INFO -> AptoPrimaryFixed
}

private fun categoryBadgeTextColor(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT, AnnouncementCategory.FINANCIAL -> Color.White
    AnnouncementCategory.MAINTENANCE -> Color(0xFF2E7D32)
    AnnouncementCategory.NEWS -> Color(0xFF00695C)
    AnnouncementCategory.INFO -> Color(0xFF3D494E)
}

fun announcementCardBackground(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT -> brand(0xFFFFF5F2, 0xFFFAEFEA)
    else -> AptoSurfaceContainerLowest
}

fun announcementCardBorderColor(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT -> AptoStatusError.copy(alpha = 0.2f)
    else -> AptoOutlineVariant
}

// ---------------------------------------------------------------------------
// AnnouncementCategoryBadge
// ---------------------------------------------------------------------------

@Composable
fun AnnouncementCategoryBadge(
    category: AnnouncementCategory,
    modifier: Modifier = Modifier
) {
    val accent = announcementCategoryAccent(category)
    Surface(
        color = badgeBg(categoryBadgeBg(category), accent),
        shape = badgeShape(RoundedCornerShape(8.dp)),
        border = badgeBorder(accent),
        modifier = modifier
    ) {
        Text(
            text = announcementCategoryLabel(category),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = badgeText(categoryBadgeTextColor(category), accent)
        )
    }
}

// ---------------------------------------------------------------------------
// AnnouncementMetricItem  — icon + count used in list cards and detail
// ---------------------------------------------------------------------------

@Composable
fun AnnouncementMetricItem(
    icon: ImageVector,
    count: Int,
    description: String,
    tint: Color = AptoOutline,
    modifier: Modifier = Modifier
) {
    Row(
        // E-C4: the metric is read as "5 lecturas", not as a bare number.
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

// ---------------------------------------------------------------------------
// AnnouncementCard  — used in BillboardListScreen
// ---------------------------------------------------------------------------

@Composable
fun AnnouncementCard(
    announcement: AnnouncementDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = announcementCategoryAccent(announcement.category)
    val metricTint = if (announcement.category == AnnouncementCategory.URGENT)
        AptoOnSurfaceVariant else AptoOutline
    val dividerColor = if (announcement.category == AnnouncementCategory.URGENT)
        AptoStatusError.copy(alpha = 0.1f) else AptoSurfaceContainerLow

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = announcementCardBackground(announcement.category)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, announcementCardBorderColor(announcement.category))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: badge + optional NUEVO pill + pin icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnnouncementCategoryBadge(category = announcement.category)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!announcement.readByCurrentUser) {
                        Surface(
                            shape = CircleShape,
                            color = accent
                        ) {
                            Text(
                                text = stringResource(Res.string.billboard_new).uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    if (announcement.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = AptoSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // Content preview
            Text(
                text = announcement.contentPreview,
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // Footer: divider + metrics row + attachment indicator
            HorizontalDivider(color = dividerColor)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnnouncementMetricItem(
                        icon = Icons.Default.Visibility,
                        count = announcement.metrics.readsCount,
                        description = pluralStringResource(Res.plurals.billboard_reads_count, announcement.metrics.readsCount, announcement.metrics.readsCount),
                        tint = metricTint
                    )
                    AnnouncementMetricItem(
                        icon = Icons.Default.ThumbUp,
                        count = announcement.metrics.reactionsCount,
                        description = pluralStringResource(Res.plurals.billboard_understood_count, announcement.metrics.reactionsCount, announcement.metrics.reactionsCount),
                        tint = metricTint
                    )
                }
                if (announcement.attachmentUrl != null) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = metricTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// FilterPillChip  — generic pill-shaped filter chip, reusable across screens
// ---------------------------------------------------------------------------

@Composable
fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) AptoSecondaryContainer else AptoSurfaceContainerLow
    val contentColor = if (selected) AptoOnSecondary else AptoOnSurfaceVariant

    Row(
        modifier = modifier
            // 48dp touch target around the 36dp pill (the pill itself keeps its size).
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(containerColor, CircleShape)
            .then(
                if (!selected) Modifier.border(1.dp, AptoOutlineVariant, CircleShape)
                else Modifier
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Check mark: selection is not shown by color alone.
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
