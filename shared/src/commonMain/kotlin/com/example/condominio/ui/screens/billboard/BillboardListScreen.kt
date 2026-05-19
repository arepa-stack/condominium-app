package com.example.condominio.ui.screens.billboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.ui.components.AnnouncementCard
import com.example.condominio.ui.components.FilterPillChip
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
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

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.billboard_title),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        containerColor = AptoBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BillboardFilterRow(
                selected = uiState.categoryFilter,
                onSelect = { viewModel.setCategoryFilter(it) }
            )

            when {
                uiState.isLoading && uiState.announcements.isEmpty() -> {
                    LoadingState()
                }

                uiState.error != null && uiState.announcements.isEmpty() -> {
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
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.billboard_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AptoOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(sortedAnnouncements, key = { it.id }) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                onClick = { onAnnouncementClick(announcement.id) }
                            )
                        }
                        if (uiState.isLoading) {
                            item {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
