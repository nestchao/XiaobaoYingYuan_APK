package com.xiaobaotv.app.ui.profile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaobaotv.app.BuildConfig
import com.xiaobaotv.app.data.local.ThemeMode
import com.xiaobaotv.app.ui.components.AuroraBackground
import com.xiaobaotv.app.ui.components.ContentContainer
import com.xiaobaotv.app.ui.components.GlassSurface
import com.xiaobaotv.app.ui.components.MetaChip
import com.xiaobaotv.app.ui.components.XiaobaoTopBar
import com.xiaobaotv.app.ui.components.pressScale
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.navigation.BrandMark
import com.xiaobaotv.app.ui.navigation.GradientWordmark
import com.xiaobaotv.app.ui.theme.BrandIndigoBright
import com.xiaobaotv.app.ui.theme.BrandViolet
import com.xiaobaotv.app.ui.theme.Ink0
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Porcelain1
import com.xiaobaotv.app.ui.theme.TextHighDark
import com.xiaobaotv.app.ui.theme.TextHighLight
import kotlinx.coroutines.delay
import java.time.Year

// ─────────────────────────────────────────────────────────────────────────────
// 设置
//
// Three frosted-glass groups — appearance, playback and about — over the same
// aurora canvas as the rest of the app. The theme picker renders true light /
// dark mini-mockups with fixed palettes so the previews stay honest while the
// app itself re-themes live.
// ─────────────────────────────────────────────────────────────────────────────

private val SettingsMaxWidth = 720.dp
private val SettingsCardShape = RoundedCornerShape(26.dp)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gutter = screenGutter()

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ContentContainer(maxWidth = SettingsMaxWidth) {
                XiaobaoTopBar(
                    title = "设置",
                    subtitle = "个性化你的观影体验",
                    onBackClick = onBackClick
                )
            }

            ContentContainer(
                modifier = Modifier.weight(1f),
                maxWidth = SettingsMaxWidth
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(
                            start = gutter,
                            end = gutter,
                            top = 6.dp,
                            bottom = 40.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StaggeredSection(index = 0) {
                        AppearanceSection(
                            themeMode = uiState.themeMode,
                            onThemeSelected = viewModel::setThemeMode
                        )
                    }
                    StaggeredSection(index = 1) {
                        PlaybackSection(
                            autoNextEnabled = uiState.autoNextEnabled,
                            onAutoNextChange = viewModel::setAutoNextEnabled
                        )
                    }
                    StaggeredSection(index = 2) {
                        AboutSection()
                    }
                }
            }
        }
    }
}

// ─── Stagger ────────────────────────────────────────────────────────────────

/**
 * Fade + slide entrance for a settings group. The whole screen is a single
 * scrolling Column (not a lazy list), so each section animates exactly once.
 */
@Composable
private fun StaggeredSection(
    index: Int,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L + 40L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(Motion.Slow.toInt(), easing = Motion.EmphasizedDecelerate)
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 26.dp.toPx()
        }
    ) {
        content()
    }
}

// ─── 外观 ───────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
) {
    val selectedOption = remember(themeMode) {
        ThemeOption.entries.firstOrNull { it.mode == themeMode } ?: ThemeOption.SYSTEM
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel(
            title = "外观",
            caption = "选择适合你的界面，设置会自动保存"
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsCardShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOption.entries.forEach { option ->
                        ThemeOptionCard(
                            option = option,
                            selected = option.mode == themeMode,
                            onClick = { onThemeSelected(option.mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    LocalXiaobaoColors.current.brandGradient
                                )
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Crossfade(
                        targetState = selectedOption.description,
                        modifier = Modifier.weight(1f),
                        animationSpec = tween(Motion.Medium.toInt()),
                        label = "themeDescription"
                    ) { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else extras.glassBorder,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "themeOptionBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "themeOptionBorderWidth"
    )
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.Fast.toInt()),
        label = "themeOptionContainer"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.Fast.toInt()),
        label = "themeOptionLabel"
    )
    // The check badge is always composed and simply scales/fades in, which avoids
    // relying on scoped AnimatedVisibility overloads inside a nested layout scope.
    val checkProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "themeOptionCheck"
    )

    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(shape)
            .background(container)
            .border(borderWidth, borderColor, shape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ThemeMock(
                mode = option.mode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = checkProgress
                        scaleY = checkProgress
                        alpha = checkProgress
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A miniature streaming UI used as the theme switcher's preview artwork. */
@Composable
private fun ThemeMock(
    mode: ThemeMode,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(LightMockPalette.canvas)
    ) {
        when (mode) {
            ThemeMode.SYSTEM -> Row(modifier = Modifier.fillMaxSize()) {
                MiniMock(
                    palette = LightMockPalette,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                MiniMock(
                    palette = DarkMockPalette,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            ThemeMode.LIGHT -> MiniMock(
                palette = LightMockPalette,
                modifier = Modifier.fillMaxSize()
            )

            ThemeMode.DARK -> MiniMock(
                palette = DarkMockPalette,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = shape
                )
        )
    }
}

@Composable
private fun MiniMock(
    palette: MockPalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(palette.canvas)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(MockAccentGradient))
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(palette.text.copy(alpha = 0.35f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MockAccentGradient.first().copy(alpha = 0.92f),
                            MockAccentGradient.last().copy(alpha = 0.92f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(14.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.text.copy(alpha = 0.82f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.text.copy(alpha = 0.32f))
        )
    }
}

// ─── 播放 ───────────────────────────────────────────────────────────────────

@Composable
private fun PlaybackSection(
    autoNextEnabled: Boolean,
    onAutoNextChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel(
            title = "播放",
            caption = "让追剧更顺滑"
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interaction, pressedScale = 0.985f),
            shape = SettingsCardShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = autoNextEnabled,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onAutoNextChange
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动播放下一集",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "当前集结束后自动连播下一集",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = autoNextEnabled,
                    onCheckedChange = null
                )
            }
        }
    }
}

// ─── 关于 ───────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection() {
    val extras = LocalXiaobaoColors.current
    val year = remember { Year.now().value }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel(
            title = "关于",
            caption = "认识小宝影院"
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsCardShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        GradientWordmark(style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "极简观影体验",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MetaChip(
                        text = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE",
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = extras.cardBorder, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))
                AboutInfoRow(label = "版本", value = "v${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(12.dp))
                AboutInfoRow(
                    label = "构建类型",
                    value = if (BuildConfig.DEBUG) "调试版 · Debug" else "正式版 · Release"
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "© $year 小宝影院 · 所有内容来源于公开网络，仅供学习与交流",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Shared bits ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(extras.brandGradient))
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// ─── Theme model & fixed preview palettes ───────────────────────────────────

private enum class ThemeOption(
    val mode: ThemeMode,
    val label: String,
    val icon: ImageVector,
    val description: String,
) {
    SYSTEM(
        mode = ThemeMode.SYSTEM,
        label = "跟随系统",
        icon = Icons.Rounded.BrightnessAuto,
        description = "与系统的深色模式保持同步，日夜自动切换"
    ),
    LIGHT(
        mode = ThemeMode.LIGHT,
        label = "浅色",
        icon = Icons.Rounded.LightMode,
        description = "明亮通透的瓷白界面，适合白天使用"
    ),
    DARK(
        mode = ThemeMode.DARK,
        label = "深色",
        icon = Icons.Rounded.DarkMode,
        description = "午夜极光配色，适合夜间沉浸观影"
    );
}

/**
 * The mini-mockups deliberately use the fixed light/dark design tokens instead
 * of the active MaterialTheme so each preview always shows its true appearance,
 * even while the app is cross-fading into the newly selected theme.
 */
private data class MockPalette(
    val canvas: Color,
    val text: Color,
)

private val LightMockPalette = MockPalette(canvas = Porcelain1, text = TextHighLight)
private val DarkMockPalette = MockPalette(canvas = Ink0, text = TextHighDark)
private val MockAccentGradient = listOf(BrandIndigoBright, BrandViolet)
