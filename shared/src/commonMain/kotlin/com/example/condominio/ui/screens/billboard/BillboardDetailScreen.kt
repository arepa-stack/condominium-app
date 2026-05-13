package com.example.condominio.ui.screens.billboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.ui.components.FullScreenImageDialog
import com.example.condominio.ui.components.shimmerEffect
import org.koin.compose.viewmodel.koinViewModel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------
// Helpers: attachment type detection
// ---------------------------------------------------------------------------

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private val PDF_EXTENSIONS = setOf("pdf")

private fun attachmentExtension(url: String): String {
    val path = url.substringBefore('?')
    val filename = path.substringAfterLast('/')
    if (!filename.contains('.')) return ""
    return filename.substringAfterLast('.').lowercase()
}

private fun isImage(url: String): Boolean {
    val ext = attachmentExtension(url)
    // Asumimos que si no hay extensión explícita (ej. UUID en storage), es probable que sea una imagen (cámara/galería)
    return ext.isEmpty() || ext in IMAGE_EXTENSIONS
}
private fun isPdf(url: String) = attachmentExtension(url) in PDF_EXTENSIONS

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillboardDetailScreen(
    announcementId: String,
    onBackClick: () -> Unit,
    viewModel: BillboardDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showFullImage by remember { mutableStateOf(false) }

    LaunchedEffect(announcementId) {
        viewModel.loadAnnouncement(announcementId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.announcement?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
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
            )
        },
        bottomBar = {
            if (uiState.announcement != null) {
                ReactionBar(
                    reacted = uiState.reacted,
                    reactionsCount = uiState.reactionsCount,
                    isLoading = uiState.isTogglingReaction,
                    onToggle = { viewModel.toggleReaction() },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.announcement == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.announcement != null -> {
                val announcement = uiState.announcement!!
                val accent = categoryColor(announcement.category)
                val attachmentUrl = announcement.attachmentUrl

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // ---- Category / Pinned / Read badges ----
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = categoryLabel(announcement.category),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                            )
                        }
                        if (announcement.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(Res.string.billboard_pinned),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (announcement.readByCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = stringResource(Res.string.billboard_read),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF4CAF50),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ---- Title ----
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )

                    Spacer(Modifier.height(6.dp))

                    // ---- Date + metrics ----
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = announcement.createdAt.take(10),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(Res.string.billboard_reads_count, announcement.metrics.readsCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Spacer(Modifier.height(20.dp))

                    // ---- Inline image attachment (shows in-app, no external intent) ----
                    if (attachmentUrl != null && isImage(attachmentUrl)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = stringResource(Res.string.billboard_attachment_image),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = stringResource(Res.string.view_full),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showFullImage = true },
                            )
                        }
                        InlineImageAttachment(
                            url = attachmentUrl,
                            onClick = { showFullImage = true },
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    // ---- Content: rendered as Markdown ----
                    Markdown(
                        content = announcement.content,
                        colors = markdownColor(
                            text = MaterialTheme.colorScheme.onSurface,
                            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        typography = markdownTypography(
                            text = MaterialTheme.typography.bodyLarge,
                            h1 = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            h2 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            h3 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            h4 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            h5 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            h6 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            bullet = MaterialTheme.typography.bodyLarge,
                            ordered = MaterialTheme.typography.bodyLarge,
                            code = MaterialTheme.typography.bodyMedium,
                        ),
                    )

                    // ---- PDF / Generic file attachment button ----
                    if (attachmentUrl != null && !isImage(attachmentUrl)) {
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))

                        val attachmentLabel = when {
                            isPdf(attachmentUrl) -> stringResource(Res.string.billboard_attachment_pdf)
                            else -> stringResource(Res.string.billboard_attachment)
                        }
                        val attachmentIcon = when {
                            isPdf(attachmentUrl) -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.AttachFile
                        }

                        OutlinedButton(
                            onClick = { uriHandler.openUri(attachmentUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = attachmentIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(attachmentLabel)
                        }
                    }

                    // ---- Expiry warning ----
                    if (announcement.expiresAt != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Vence: ${announcement.expiresAt.take(10)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // Bottom spacing for reaction bar
                    Spacer(Modifier.height(80.dp))
                }

                // Full-screen image viewer
                val attachmentUrlForViewer = uiState.announcement?.attachmentUrl
                if (showFullImage && attachmentUrlForViewer != null && isImage(attachmentUrlForViewer)) {
                    FullScreenImageDialog(
                        imageUrl = attachmentUrlForViewer,
                        onDismiss = { showFullImage = false },
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error?.asString() ?: "No se pudo cargar el anuncio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inline image component — shows in-app like PaymentDetailScreen receipt
// ---------------------------------------------------------------------------

@Composable
private fun InlineImageAttachment(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalPlatformContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        coil3.compose.SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(Res.string.billboard_attachment_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            loading = {
                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Reaction bar
// ---------------------------------------------------------------------------

@Composable
private fun ReactionBar(
    reacted: Boolean,
    reactionsCount: Int,
    isLoading: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = onToggle,
                enabled = !isLoading,
                colors = if (reacted) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f),
                border = if (!reacted) {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    )
                } else null,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (reacted) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.billboard_understood_count, reactionsCount),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}
