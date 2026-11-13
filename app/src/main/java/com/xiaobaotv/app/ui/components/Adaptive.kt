package com.xiaobaotv.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classes used across the app.
 *
 * Compact  : phones in portrait                 (< 600dp)
 * Medium   : tablets portrait / foldables       (600dp – 840dp)
 * Expanded : tablets landscape / desktop        (>= 840dp)
 */
enum class WindowSize { Compact, Medium, Expanded }

val LocalWindowSize = compositionLocalOf { WindowSize.Compact }

@Composable
fun rememberWindowSize(): WindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> WindowSize.Expanded
        widthDp >= 600 -> WindowSize.Medium
        else -> WindowSize.Compact
    }
}

/** True on tablets (both medium and expanded). */
@Composable
fun rememberIsExpanded(): Boolean = rememberWindowSize() != WindowSize.Compact

/** Shared layout metrics so every screen insets consistently. */
object LayoutMetrics {
    val CompactGutter = 18.dp
    val MediumGutter = 28.dp
    val ExpandedGutter = 36.dp

    /** Height of the floating bottom navigation pill. */
    val BottomBarHeight = 66.dp

    /**
     * Space a scrollable screen should leave at the bottom so content can scroll
     * clear of the floating bottom bar / navigation insets.
     */
    val BottomContentClearance = 124.dp

    val RailWidth = 92.dp
    val SidebarWidth = 244.dp

    /** Content is centred on very wide screens for readability. */
    val MaxContentWidth = 1480.dp

    /** Hero card height per size class. */
    val CompactHeroHeight = 452.dp
    val MediumHeroHeight = 380.dp
    val ExpandedHeroHeight = 420.dp
}

@Composable
fun screenGutter(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> LayoutMetrics.CompactGutter
    WindowSize.Medium -> LayoutMetrics.MediumGutter
    WindowSize.Expanded -> LayoutMetrics.ExpandedGutter
}

/** Poster width used by horizontal content rails. */
@Composable
fun railPosterWidth(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 124.dp
    WindowSize.Medium -> 146.dp
    WindowSize.Expanded -> 158.dp
}

/** Width for wide 16:9 "continue watching" style cards. */
@Composable
fun railWideCardWidth(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 236.dp
    WindowSize.Medium -> 268.dp
    WindowSize.Expanded -> 292.dp
}

/** Number of columns for browsable poster grids. */
@Composable
fun posterGridColumns(): Int = when (rememberWindowSize()) {
    WindowSize.Compact -> 3
    WindowSize.Medium -> 4
    WindowSize.Expanded -> 6
}

/**
 * Centers content at a comfortable maximum width on large displays while
 * letting compact layouts run edge to edge.
 */
@Composable
fun ContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LayoutMetrics.MaxContentWidth,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}
