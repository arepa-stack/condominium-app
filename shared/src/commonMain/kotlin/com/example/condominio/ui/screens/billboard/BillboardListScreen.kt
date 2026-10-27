package com.example.condominio.ui.screens.billboard

import com.example.condominio.ui.components.WebEmptyFrame
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.ui.components.AnnouncementCard
import com.example.condominio.ui.components.EmptyState
import com.example.condominio.ui.components.FilterPillChip
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.components.announcementCategoryLabel
import com.example.condominio.ui.theme.*
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillboardListScreen(
    onBackClick: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    viewModel: BillboardListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val sortedAnnouncements = remember(uiState.announcements) {
        uiState.announcements.sortedByDescending { it.isPinned }
    }

    val wide = isWide()

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.billboard_title),
                onBackClick = onBackClick,
                actions = {
                    if (wide) {
                        // Web header action: labelled button on the right of the page title.
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            enabled = !uiState.isLoading,
                            border = BorderStroke(1.dp, AptoOutlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoOnSurface),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
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
            if (wide) {
                WideBillboardFilterRow(
                    selected = uiState.categoryFilter,
                    onSelect = { viewModel.setCategoryFilter(it) }
                )
            } else {
                BillboardFilterRow(
                    selected = uiState.categoryFilter,
                    onSelect = { viewModel.setCategoryFilter(it) }
                )
            }

            when {
                uiState.isLoading && uiState.announcements.isEmpty() -> {
                    LoadingState()
                }

                uiState.error != null && uiState.announcements.isEmpty() -> WebEmptyFrame {
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

                sortedAnnouncements.isEmpty() -> {
                    WebEmptyFrame { EmptyState(title = stringResource(Res.string.billboard_empty)) }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(340.dp),
                        contentPadding = if (wide) {
                            PaddingValues(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 32.dp)
                        } else {
                            PaddingValues(16.dp)
                        },
                        verticalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 16.dp)
                    ) {
                        items(sortedAnnouncements, key = { it.id }) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                onClick = { onAnnouncementClick(announcement.id) },
                                modifier = if (wide) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier
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

@Composable
private fun BillboardFilterRow(
    selected: AnnouncementCategory?,
    onSelect: (AnnouncementCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.selectableGroup(),
        // 6dp + the chips' 48dp touch target = the previous 12dp + 36dp pill: same row height.
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterPillChip(
                label = stringResource(Res.string.billboard_filter_all),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(AnnouncementCategory.entries) { category ->
            FilterPillChip(
                label = announcementCategoryLabel(category),
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) }
            )
        }
    }
}

/** Wide layouts: chips wrap in a row aligned with the page content (no horizontal scroll). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WideBillboardFilterRow(
    selected: AnnouncementCategory?,
    onSelect: (AnnouncementCategory?) -> Unit
) {
    val hand = Modifier.pointerHoverIcon(PointerIcon.Hand)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            // 8dp under the header; + the grid's / WebEmptyFrame's 8dp top = 16dp to the content.
            .padding(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPillChip(
            label = stringResource(Res.string.billboard_filter_all),
            selected = selected == null,
            onClick = { onSelect(null) },
            modifier = hand
        )
        AnnouncementCategory.entries.forEach { category ->
            FilterPillChip(
                label = announcementCategoryLabel(category),
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                modifier = hand
            )
        }
    }
}
