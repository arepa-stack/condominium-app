package com.example.condominio.ui.screens.billboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.data.model.AnnouncementDto
import org.koin.compose.viewmodel.koinViewModel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------
// Colours per category
// ---------------------------------------------------------------------------

internal fun categoryColor(category: AnnouncementCategory): Color = when (category) {
    AnnouncementCategory.URGENT -> Color(0xFFD32F2F)
    AnnouncementCategory.FINANCIAL -> Color(0xFF1565C0)
    AnnouncementCategory.MAINTENANCE -> Color(0xFFE65100)
    AnnouncementCategory.NEWS -> Color(0xFF00695C)
    AnnouncementCategory.INFO -> Color(0xFF5E35B1)
}

internal fun categoryLabel(category: AnnouncementCategory): String = when (category) {
    AnnouncementCategory.URGENT -> "Urgente"
    AnnouncementCategory.FINANCIAL -> "Financiero"
    AnnouncementCategory.MAINTENANCE -> "Mantenimiento"
    AnnouncementCategory.NEWS -> "Noticias"
    AnnouncementCategory.INFO -> "Info"
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillboardListScreen(
    onBackClick: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    viewModel: BillboardListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Sort: pinned first, then by created_at desc (order from API is already correct)
    val sortedAnnouncements = remember(uiState.announcements) {
        uiState.announcements.sortedByDescending { it.isPinned }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.billboard_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Category filter chips
            CategoryFilterRow(
                selected = uiState.categoryFilter,
                onSelect = { viewModel.setCategoryFilter(it) },
            )

            when {
                uiState.isLoading && uiState.announcements.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.announcements.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.refresh() }) {
                                Text(stringResource(Res.string.billboard_retry))
                            }
                        }
                    }
                }

                sortedAnnouncements.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.billboard_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(sortedAnnouncements, key = { it.id }) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                onClick = { onAnnouncementClick(announcement.id) },
                            )
                        }
                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Category filter row
// ---------------------------------------------------------------------------

@Composable
private fun CategoryFilterRow(
    selected: AnnouncementCategory?,
    onSelect: (AnnouncementCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(Res.string.billboard_filter_all)) },
            )
        }
        items(AnnouncementCategory.entries) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(categoryLabel(category)) },
                leadingIcon = if (selected == category) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Announcement card
// ---------------------------------------------------------------------------

@Composable
fun AnnouncementCard(
    announcement: AnnouncementDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = categoryColor(announcement.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = categoryLabel(announcement.category),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Pinned indicator
                    if (announcement.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(Res.string.billboard_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    // Read indicator
                    if (!announcement.readByCurrentUser) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, CircleShape),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = announcement.contentPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Metrics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricChip(
                        icon = Icons.Default.Visibility,
                        count = announcement.metrics.readsCount,
                    )
                    MetricChip(
                        icon = Icons.Default.ThumbUp,
                        count = announcement.metrics.reactionsCount,
                    )
                }
                // Attachment indicator
                if (announcement.attachmentUrl != null) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = stringResource(Res.string.billboard_attachment),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}
