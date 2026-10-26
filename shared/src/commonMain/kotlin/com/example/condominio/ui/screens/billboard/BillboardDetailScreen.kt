package com.example.condominio.ui.screens.billboard

import androidx.compose.material.icons.filled.ThumbUp
import com.example.condominio.ui.utils.platformRendersEmoji
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.formatDecisionDate
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
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.AnnouncementCategoryBadge
import com.example.condominio.ui.components.AnnouncementMetricItem
import com.example.condominio.ui.components.FullScreenImageDialog
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.components.announcementCategoryAccent
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.components.shimmerEffect
import com.example.condominio.ui.theme.AptoOnSurface
import com.example.condominio.ui.theme.AptoOnSurfaceVariant
import com.example.condominio.ui.theme.AptoOutlineVariant
import com.example.condominio.ui.theme.AptoSuccess
import com.example.condominio.ui.theme.AptoSuccessContainer
import org.koin.compose.viewmodel.koinViewModel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------
// Helpers: attachment type detection
// ---------------------------------------------------------------------------

/** Hand cursor on wide (web) layouts only; no-op on phone. */
@Composable
private fun Modifier.handCursorIfWide(): Modifier =
    if (isWide()) this.pointerHoverIcon(PointerIcon.Hand) else this

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
    // APP-M3: announcement links (attachment and Markdown) only open as http/https.
    val baseUriHandler = LocalUriHandler.current
    val uriHandler = remember(baseUriHandler) { safeUriHandler(baseUriHandler) }
    var showFullImage by remember { mutableStateOf(false) }
    val wide = isWide()

    LaunchedEffect(announcementId) {
        viewModel.loadAnnouncement(announcementId)
    }

    // A failed reaction (or reload) with the announcement already on screen is reported in a snackbar (A-22).
    val snackbarHostState = remember { SnackbarHostState() }
    val inlineError = if (uiState.announcement != null) uiState.error?.asString() else null
    LaunchedEffect(inlineError) {
        if (inlineError != null) {
            snackbarHostState.showSnackbar(inlineError)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.billboard_detail_title),
                onBackClick = onBackClick,
                actions = {
                    // Wide layouts: primary action lives in the page header (top right).
                    if (wide && uiState.announcement != null) {
                        ReactionButton(
                            reacted = uiState.reacted,
                            isLoading = uiState.isTogglingReaction,
                            onToggle = { viewModel.toggleReaction() },
                            compact = true,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!wide && uiState.announcement != null) {
                ReactionBar(
                    reacted = uiState.reacted,
                    reactionsCount = uiState.reactionsCount,
                    isLoading = uiState.isTogglingReaction,
                    onToggle = { viewModel.toggleReaction() },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = screenBackground(MaterialTheme.colorScheme.background),
    ) { paddingValues ->
        CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        when {
            uiState.isLoading && uiState.announcement == null -> {
                LoadingState(modifier = Modifier.padding(paddingValues))
            }

            uiState.announcement != null -> {
                val announcement = uiState.announcement!!
                val attachmentUrl = announcement.attachmentUrl

                if (wide) {
                    WideAnnouncementDetail(
                        announcement = announcement,
                        reactionsCount = uiState.reactionsCount,
                        paddingValues = paddingValues,
                        onViewFullImage = { showFullImage = true },
                        onOpenAttachment = { uriHandler.openUri(it) },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        // ---- Category / Pinned / Read badges ----
                        AnnouncementBadgesRow(announcement)

                        Spacer(Modifier.height(12.dp))

                        // ---- Title ----
                        AnnouncementTitle(announcement.title)

                        Spacer(Modifier.height(8.dp))

                        // ---- Date + metrics ----
                        AnnouncementDateMetricsRow(announcement, uiState.reactionsCount)

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(24.dp))

                        // ---- Content: rendered as Markdown ----
                        AnnouncementMarkdown(announcement.content)

                        // ---- Inline image attachment (shows in-app, no external intent) ----
                        if (attachmentUrl != null && isImage(attachmentUrl)) {
                            Spacer(Modifier.height(40.dp))
                            ImageAttachmentSection(
                                url = attachmentUrl,
                                onViewFull = { showFullImage = true },
                            )
                        }

                        // ---- PDF / Generic file attachment button (Stitch-style card) ----
                        if (attachmentUrl != null && !isImage(attachmentUrl)) {
                            Spacer(Modifier.height(40.dp))
                            FileAttachmentCard(
                                url = attachmentUrl,
                                onOpen = { uriHandler.openUri(attachmentUrl) },
                            )
                        }

                        // ---- Expiry warning (Stitch-style card) ----
                        if (announcement.expiresAt != null) {
                            Spacer(Modifier.height(24.dp))
                            ExpiryWarningCard(announcement.expiresAt)
                        }
                    }
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
                val errorContent: @Composable () -> Unit = {
                    ErrorState(
                        title = uiState.error?.asString() ?: stringResource(Res.string.error_loading_announcement),
                        onRetry = { viewModel.loadAnnouncement(announcementId) },
                    )
                }
                if (wide) {
                    // Wide: compact card under the header, on the container's left edge.
                    Box(Modifier.padding(paddingValues)) { WebEmptyFrame(errorContent) }
                } else {
                    errorContent()
                }
            }
        }
        }
    }
}

// ---------------------------------------------------------------------------
// Wide (web) layout: article on the left, metadata / attachments on the right
// ---------------------------------------------------------------------------

@Composable
private fun WideAnnouncementDetail(
    announcement: AnnouncementDto,
    reactionsCount: Int,
    paddingValues: PaddingValues,
    onViewFullImage: () -> Unit,
    onOpenAttachment: (String) -> Unit,
) {
    val attachmentUrl = announcement.attachmentUrl
    val expanded = isExpanded()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = pagePadding())
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        AdaptiveColumns(
            mainWeight = 3f,
            sideWeight = 1.4f,
            main = {
                // ---- Article ----
                WebCard(contentPadding = 32.dp) {
                    // Reading width: capped at 760dp, pinned to the card's left edge (never centered).
                    Column(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .widthIn(max = 760.dp)
                            .fillMaxWidth(),
                    ) {
                        AnnouncementTitle(announcement.title)

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = AptoOutlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(24.dp))

                        AnnouncementMarkdown(announcement.content)

                        if (attachmentUrl != null && isImage(attachmentUrl)) {
                            Spacer(Modifier.height(32.dp))
                            ImageAttachmentSection(
                                url = attachmentUrl,
                                onViewFull = onViewFullImage,
                            )
                        }
                    }
                }
            },
            side = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Stacked (Medium): AdaptiveColumns adds no gap between main and side.
                    if (!expanded) Spacer(Modifier.height(0.dp))

                    // ---- Metadata ----
                    WebCard {
                        AnnouncementBadgesRow(announcement)
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = AptoOutlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SideMetaRow(
                                icon = Icons.Default.CalendarToday,
                                text = formatDecisionDate(announcement.createdAt),
                            )
                            SideMetaRow(
                                icon = Icons.Default.Visibility,
                                text = pluralStringResource(Res.plurals.billboard_reads_count, announcement.metrics.readsCount, announcement.metrics.readsCount),
                            )
                            SideMetaRow(
                                icon = Icons.Default.ThumbUp,
                                text = pluralStringResource(Res.plurals.billboard_understood_count, reactionsCount, reactionsCount),
                            )
                            if (announcement.isPinned) {
                                SideMetaRow(
                                    icon = Icons.Default.PushPin,
                                    text = stringResource(Res.string.billboard_pinned),
                                )
                            }
                        }
                    }

                    // ---- Expiry ----
                    if (announcement.expiresAt != null) {
                        ExpiryWarningCard(announcement.expiresAt)
                    }

                    // ---- File attachment (PDF / generic) ----
                    if (attachmentUrl != null && !isImage(attachmentUrl)) {
                        FileAttachmentCard(
                            url = attachmentUrl,
                            onOpen = { onOpenAttachment(attachmentUrl) },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun SideMetaRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AptoOnSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = AptoOnSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Sections shared by phone and wide layouts
// ---------------------------------------------------------------------------

@Composable
private fun AnnouncementBadgesRow(announcement: AnnouncementDto) {
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
}

@Composable
private fun AnnouncementTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun AnnouncementDateMetricsRow(announcement: AnnouncementDto, reactionsCount: Int) {
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
                text = formatDecisionDate(announcement.createdAt),
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
                text = pluralStringResource(Res.plurals.billboard_reads_count, announcement.metrics.readsCount, announcement.metrics.readsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // E-C2: the live "understood" count (it follows the reaction button), also on phone.
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = pluralStringResource(Res.plurals.billboard_understood_count, reactionsCount, reactionsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnnouncementMarkdown(content: String) {
    Markdown(
        content = content,
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
}

@Composable
private fun ImageAttachmentSection(
    url: String,
    onViewFull: () -> Unit,
) {
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
            modifier = Modifier
                .handCursorIfWide()
                .clickable(role = Role.Button) { onViewFull() },
        )
    }
    InlineImageAttachment(
        url = url,
        onClick = onViewFull,
    )
}

@Composable
private fun FileAttachmentCard(
    url: String,
    onOpen: () -> Unit,
) {
    val isPdfFile = isPdf(url)
    val attachmentIcon = if (isPdfFile) Icons.Default.PictureAsPdf else Icons.Default.AttachFile
    val fileName = attachmentFileName(url).ifBlank {
        if (isPdfFile) stringResource(Res.string.billboard_attachment_pdf)
        else stringResource(Res.string.billboard_attachment)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .handCursorIfWide()
            .clickable { onOpen() },
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
                        text = stringResource(Res.string.billboard_pdf_document),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(Res.string.billboard_download),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ExpiryWarningCard(expiresAt: String) {
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
                text = stringResource(Res.string.billboard_expiry_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(Res.string.billboard_expires_on, formatDecisionDate(expiresAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
    // E-C1: a failed load shows an explicit state (and nothing to open full screen).
    var failed by remember(url) { mutableStateOf(false) }

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
            .then(if (failed) Modifier else Modifier.handCursorIfWide().clickable(role = Role.Button, onClick = onClick)),
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
            },
            error = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.billboard_image_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onError = { failed = true },
        )
    }
}

// ---------------------------------------------------------------------------
// Reaction bar (phone) / reaction button (shared)
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
                ReactionButton(
                    reacted = reacted,
                    isLoading = isLoading,
                    onToggle = onToggle,
                )
            }
        }
    }
}

/**
 * "Confirmar lectura" pill. [compact] = header variant for wide layouts
 * (no elevation, tighter padding, hand cursor); phone uses the original look.
 */
@Composable
private fun ReactionButton(
    reacted: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    compact: Boolean = false,
) {
    Button(
        onClick = onToggle,
        enabled = !isLoading,
        elevation = if (compact) {
            ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        } else {
            ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
        },
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
        // E-C4: the toggle state is announced ("on"/"off"), not only shown by color.
        modifier = (if (compact) {
            Modifier.wrapContentWidth().pointerHoverIcon(PointerIcon.Hand)
        } else {
            Modifier.wrapContentWidth()
        }).semantics { toggleableState = if (reacted) ToggleableState.On else ToggleableState.Off },
        contentPadding = if (compact) {
            PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        } else {
            PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        },
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
                if (platformRendersEmoji) {
                    Text(
                        text = "👍",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.alpha(if (reacted) 0.6f else 1f)
                    )
                } else {
                    // Web has no emoji font: same meaning with a vector icon.
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).alpha(if (reacted) 0.6f else 1f)
                    )
                }
                Text(
                    text = stringResource(if (reacted) Res.string.billboard_read else Res.string.billboard_confirm_read).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    modifier = Modifier.alpha(if (reacted) 0.6f else 1f)
                )
            }
        }
    }
}
