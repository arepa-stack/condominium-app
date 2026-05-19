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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.ui.components.AnnouncementCategoryBadge
import com.example.condominio.ui.components.AnnouncementMetricItem
import com.example.condominio.ui.components.FullScreenImageDialog
import com.example.condominio.ui.components.announcementCategoryAccent
import com.example.condominio.ui.components.shimmerEffect
import com.example.condominio.ui.theme.AptoSuccess
import com.example.condominio.ui.theme.AptoSuccessContainer
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
                        text = stringResource(Res.string.billboard_detail_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                val accent = announcementCategoryAccent(announcement.category)
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AnnouncementCategoryBadge(category = announcement.category)
                        if (announcement.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(Res.string.billboard_pinned),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (announcement.readByCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AptoSuccessContainer,
                            ) {
                                Text(
                                    text = stringResource(Res.string.billboard_read).uppercase(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                    ),
                                    color = AptoSuccess,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ---- Title ----
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Spacer(Modifier.height(8.dp))

                    // ---- Date + metrics ----
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = announcement.createdAt.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(Res.string.billboard_reads_count, announcement.metrics.readsCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(24.dp))

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

                    // ---- Inline image attachment (shows in-app, no external intent) ----
                    if (attachmentUrl != null && isImage(attachmentUrl)) {
                        Spacer(Modifier.height(40.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.billboard_attachment_image),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = stringResource(Res.string.view_full),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showFullImage = true },
                            )
                        }
                        InlineImageAttachment(
                            url = attachmentUrl,
                            onClick = { showFullImage = true },
                        )
                    }

                    // ---- PDF / Generic file attachment button (Stitch-style card) ----
                    if (attachmentUrl != null && !isImage(attachmentUrl)) {
                        Spacer(Modifier.height(40.dp))
                        val isPdfFile = isPdf(attachmentUrl)
                        val attachmentIcon = if (isPdfFile) Icons.Default.PictureAsPdf else Icons.Default.AttachFile
                        val fileName = attachmentUrl.substringBefore('?').substringAfterLast('/').ifBlank {
                            if (isPdfFile) stringResource(Res.string.billboard_attachment_pdf)
                            else stringResource(Res.string.billboard_attachment)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { uriHandler.openUri(attachmentUrl) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    imageVector = attachmentIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isPdfFile) {
                                        Text(
                                            text = "PDF Documento",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Descargar",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // ---- Expiry warning (Stitch-style card) ----
                    if (announcement.expiresAt != null) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(50),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Aviso de Expiración",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = "Vence: ${announcement.expiresAt.take(10)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
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

    // Aspect ratio 16:9 para coincidir con el diseño de Stitch
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
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
                .clip(RoundedCornerShape(16.dp)),
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
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = onToggle,
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp),
                    colors = if (reacted) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    border = if (reacted) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), 
                            strokeWidth = 2.dp,
                            color = if (reacted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "👍",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.alpha(if (reacted) 0.6f else 1f)
                            )
                            Text(
                                text = if (reacted) "LEÍDO" else "CONFIRMAR LECTURA",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                modifier = Modifier.alpha(if (reacted) 0.6f else 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
