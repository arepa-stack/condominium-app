package com.example.condominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.theme.AptoOutlineVariant
import com.example.condominio.ui.theme.brand
import com.example.condominio.ui.theme.AptoSurfaceContainerLowest

/**
 * Responsive building blocks shared by every screen.
 *
 * Rule: adapt by available width, never by platform. A tablet and a desktop browser get the
 * same layout; a phone and the installed PWA on a phone get the mobile one (unchanged).
 *
 * - Compact  (< 600dp): phone. Original mobile UI, bottom bar.
 * - Medium   (600–1024dp): tablet / narrow window. Nav rail, single column with more room.
 * - Expanded (>= 1024dp): desktop web. Sidebar, page headers, multi-column layouts, cards on a canvas.
 */
enum class WidthClass { Compact, Medium, Expanded }

/** Provided by the nav shell from the window width. */
val LocalWidthClass = compositionLocalOf { WidthClass.Compact }

/** False on wide layouts for top-level tab screens (the sidebar replaces the back arrow). */
val LocalShowBack = compositionLocalOf { true }

/**
 * Login sets this to false once it knows there is no saved session. Until then the shell hides the
 * desktop brand panel, so a logged-in reload goes splash → dashboard without flashing the login split.
 */
val LocalLoginFormVisible = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(true) }

/** Max width of the shared page container (all app pages, wide layouts). */
val WebContainerWidth = 1440.dp

/** Max width for settings-style forms inside the container (left-aligned, never centered). */
val WebFormWidth = 720.dp

/**
 * Caps a settings-style form at [WebFormWidth] on wide layouts, keeping it on the container's
 * left edge (aligned with the page title). No-op on phone layouts.
 */
@Composable
fun Modifier.formWidth(): Modifier = if (isWide()) this.widthIn(max = WebFormWidth) else this

/** Gray canvas behind web pages; screens on wide layouts draw white [WebCard]s on top of it. */
val AptoWebCanvas = brand(0xFFF4F6F8, 0xFFF3F1ED) // web POC: condominium-admin --background

@Composable
fun isWide(): Boolean = LocalWidthClass.current != WidthClass.Compact

@Composable
fun isExpanded(): Boolean = LocalWidthClass.current == WidthClass.Expanded

/** Horizontal page padding: 16dp on phone, 32dp on wide layouts. */
@Composable
fun pagePadding(): Dp = if (isWide()) 32.dp else 16.dp

/**
 * Scaffold/background color for a screen: its mobile [default] on phone,
 * transparent on wide layouts so the shell canvas shows behind the cards.
 */
@Composable
fun screenBackground(default: Color): Color = if (isWide()) Color.Transparent else default

/**
 * White rounded card with a soft border: the basic web surface. Use it on wide layouts to
 * group sections (on phone screens keep the original look unless a card already existed).
 */
@Composable
fun WebCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    color: Color = AptoSurfaceContainerLowest,
    border: BorderStroke? = BorderStroke(1.dp, AptoOutlineVariant),
    shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color,
        border = border,
        shadowElevation = shadowElevation
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Main + side columns on Expanded (main gets [mainWeight], side [sideWeight], [spacing] between
 * and inside columns); stacked (main, then side, no added spacing) otherwise. Typical use: detail/form on the left,
 * summary/actions on the right.
 */
@Composable
fun AdaptiveColumns(
    modifier: Modifier = Modifier,
    spacing: Dp = 24.dp,
    mainWeight: Float = 3f,
    sideWeight: Float = 2f,
    main: @Composable ColumnScope.() -> Unit,
    side: @Composable ColumnScope.() -> Unit
) {
    if (isExpanded()) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(mainWeight), verticalArrangement = Arrangement.spacedBy(spacing), content = main)
            Column(Modifier.weight(sideWeight), verticalArrangement = Arrangement.spacedBy(spacing), content = side)
        }
    } else {
        // No extra spacing here: phone layouts keep their original Spacers untouched.
        Column(modifier.fillMaxWidth()) {
            main()
            side()
        }
    }
}

/**
 * Empty/error states on wide layouts: a compact card under the page header instead of text floating
 * in the middle of the canvas. Phone layouts render [content] unchanged.
 */
@Composable
fun WebEmptyFrame(content: @Composable () -> Unit) {
    if (!isWide()) {
        content()
        return
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = pagePadding(), vertical = 8.dp)) {
        WebCard(contentPadding = 0.dp) {
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}
