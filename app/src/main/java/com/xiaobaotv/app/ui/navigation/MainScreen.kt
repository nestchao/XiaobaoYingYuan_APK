package com.xiaobaotv.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xiaobaotv.app.ui.components.GlassSurface
import com.xiaobaotv.app.ui.components.LayoutMetrics
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.pressScale
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii

@Stable
class FullScreenState {
    var isActive by mutableStateOf(false)
}

val LocalFullScreenState = staticCompositionLocalOf { FullScreenState() }

/** Kept for screens that only need a tablet/phone split. */
@Composable
fun isExpandedLayout(): Boolean = rememberWindowSize() != WindowSize.Compact

private fun NavHostController.navigateToTopLevel(route: String) {
    // Settings/detail/player can sit above a tab destination. Pop back to an
    // existing tab first so tapping a tab always changes the visible screen.
    if (popBackStack(route, inclusive = false)) return

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val fullScreenState = remember { FullScreenState() }
    val windowSize = rememberWindowSize()

    val selectedIndex = BottomNavItem.items.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val haptics = LocalHapticFeedback.current

    val onSelectTab: (Int) -> Unit = { index ->
        val item = BottomNavItem.items[index]
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        navController.navigateToTopLevel(item.route)
    }
    val onSettingsClick: () -> Unit = {
        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
    }

    CompositionLocalProvider(LocalFullScreenState provides fullScreenState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (windowSize != WindowSize.Compact) {
                    AnimatedVisibility(
                        visible = !fullScreenState.isActive,
                        enter = slideInHorizontally(
                            animationSpec = tween(Motion.Medium.toInt(), easing = Motion.EmphasizedDecelerate)
                        ) { -it } + fadeIn(tween(Motion.Medium.toInt())),
                        exit = slideOutHorizontally(
                            animationSpec = tween(Motion.Fast.toInt(), easing = Motion.EmphasizedAccelerate)
                        ) { -it } + fadeOut(tween(Motion.Fast.toInt()))
                    ) {
                        if (windowSize == WindowSize.Expanded) {
                            SideNavigation(
                                selectedIndex = selectedIndex,
                                onSelect = onSelectTab,
                                onSettingsClick = onSettingsClick
                            )
                        } else {
                            IconRail(
                                selectedIndex = selectedIndex,
                                onSelect = onSelectTab,
                                onSettingsClick = onSettingsClick
                            )
                        }
                    }
                }

                XiaobaoNavHost(
                    navController = navController,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            if (windowSize == WindowSize.Compact) {
                AnimatedVisibility(
                    visible = !fullScreenState.isActive && selectedIndex >= 0,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        animationSpec = tween(Motion.Medium.toInt(), easing = Motion.EmphasizedDecelerate)
                    ) { it } + fadeIn(tween(Motion.Medium.toInt())),
                    exit = slideOutVertically(
                        animationSpec = tween(Motion.Fast.toInt(), easing = Motion.EmphasizedAccelerate)
                    ) { it } + fadeOut(tween(Motion.Fast.toInt()))
                ) {
                    FloatingBottomNavigation(
                        selectedIndex = selectedIndex,
                        onSelect = onSelectTab
                    )
                }
            }

            // Soft top scrim keeps status bar icons legible while edge-to-edge
            // content scrolls underneath.
            if (!fullScreenState.isActive) {
                val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(statusTop + 12.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

// ─── Compact: floating glass navigation pill ────────────────────────────────

@Composable
private fun FloatingBottomNavigation(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val extras = LocalXiaobaoColors.current
    val items = BottomNavItem.items
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .height(LayoutMetrics.BottomBarHeight)
        ) {
            val barWidth = maxWidth
            val itemWidth = barWidth / items.size
            val safeIndex = selectedIndex.coerceAtLeast(0)
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * safeIndex,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f
                ),
                label = "navIndicatorOffset"
            )
            GlassSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 26.dp, shape = Radii.navBar),
                shape = Radii.navBar,
                strong = true
            ) {
                if (selectedIndex >= 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = indicatorOffset)
                            .padding(horizontal = 7.dp, vertical = 9.dp)
                            .width(itemWidth - 14.dp)
                            .fillMaxHeight()
                            .clip(Radii.navItem)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                                shape = Radii.navItem
                            )
                    )
                }
                Row(modifier = Modifier.fillMaxSize()) {
                    items.forEachIndexed { index, item ->
                        NavBarItem(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "navItemColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 0.96f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f),
        label = "navIconScale"
    )
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayerScale(iconScale)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
)

// ─── Medium: icon rail ──────────────────────────────────────────────────────

@Composable
private fun IconRail(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val extras = LocalXiaobaoColors.current
    Column(
        modifier = Modifier
            .width(LayoutMetrics.RailWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .drawBehind {
                drawLine(
                    color = extras.cardBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BrandMark(size = 40.dp)
        Spacer(Modifier.height(18.dp))
        BottomNavItem.items.forEachIndexed { index, item ->
            RailNavItem(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
        Spacer(Modifier.weight(1f))
        RailSettingsItem(onClick = onSettingsClick)
    }
}

@Composable
private fun RailNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "railItemColor"
    )
    Column(
        modifier = Modifier
            .width(68.dp)
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun RailSettingsItem(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(68.dp)
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "设置",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "设置",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ─── Expanded: labelled sidebar ────────────────────────────────────────────

@Composable
private fun SideNavigation(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val extras = LocalXiaobaoColors.current
    Column(
        modifier = Modifier
            .width(LayoutMetrics.SidebarWidth)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        extras.glassFill,
                        extras.glassFill.copy(alpha = 0.35f)
                    )
                )
            )
            .drawBehind {
                drawLine(
                    color = extras.cardBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 38.dp)
            Spacer(Modifier.width(11.dp))
            Column {
                GradientWordmark(style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "XIAOBAO CINEMA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.4.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(30.dp))
        Text(
            text = "浏览",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        BottomNavItem.items.forEachIndexed { index, item ->
            SideNavItem(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = extras.cardBorder, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        SideSettingsItem(onClick = onSettingsClick)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "小宝影院 · v1.2.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun SideNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else Color.Transparent,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "sideContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "sideContent"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(Radii.navItem)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = content,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(13.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(LocalXiaobaoColors.current.brandGradient))
            )
        }
    }
}

@Composable
private fun SideSettingsItem(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(Radii.navItem)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "设置",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(13.dp))
        Text(
            text = "设置与外观",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ─── Brand marks ────────────────────────────────────────────────────────────

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val extras = LocalXiaobaoColors.current
    val shape = RoundedCornerShape(size * 0.34f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(extras.brandGradient))
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
fun GradientWordmark(
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val extras = LocalXiaobaoColors.current
    Text(
        text = "小宝影院",
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            brush = Brush.horizontalGradient(extras.brandGradient),
            fontSize = style.fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = style.letterSpacing,
            lineHeight = style.lineHeight,
            fontFamily = style.fontFamily
        ),
        maxLines = 1
    )
}
