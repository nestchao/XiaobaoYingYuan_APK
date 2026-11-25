package com.xiaobaotv.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Brand palette
//
// "Aurora" design language: deep midnight canvas, electric periwinkle primary,
// violet/cyan glow accents and a warm amber highlight for ratings.
// The palette is intentionally low-chroma in the surfaces so that poster art
// remains the loudest thing on screen.
// ─────────────────────────────────────────────────────────────────────────────

// Brand hues
val BrandIndigo = Color(0xFF8CA5FF)      // dark-theme primary
val BrandIndigoDeep = Color(0xFF3D5BD9)  // light-theme primary
val BrandIndigoBright = Color(0xFF5B7CFA)
val BrandViolet = Color(0xFF9B7BFF)
val BrandVioletDeep = Color(0xFF7C4DFF)
val BrandCyan = Color(0xFF63D9EC)
val BrandAmber = Color(0xFFFFBE73)
val BrandAmberDeep = Color(0xFFE8890D)
val BrandMint = Color(0xFF63E6CE)
val BrandRose = Color(0xFFFF7A8A)

// Dark surfaces (midnight blue-black)
val Ink0 = Color(0xFF060A12)   // window / deepest canvas
val Ink1 = Color(0xFF0A101C)   // background
val Ink2 = Color(0xFF0E1626)   // surface
val Ink3 = Color(0xFF121C2F)   // surface container
val Ink4 = Color(0xFF17233A)   // surface container high
val Ink5 = Color(0xFF1D2C47)   // surface container highest

// Light surfaces (airy porcelain)
val Porcelain0 = Color(0xFFF5F7FC)
val Porcelain1 = Color(0xFFFAFBFE)
val Porcelain2 = Color(0xFFFFFFFF)
val Porcelain3 = Color(0xFFF0F3FA)
val Porcelain4 = Color(0xFFE9EEF8)
val Porcelain5 = Color(0xFFE1E8F4)

// Text
val TextHighDark = Color(0xFFEDF1FA)
val TextMidDark = Color(0xFF9AA8C0)
val TextLowDark = Color(0xFF6B7A93)
val TextHighLight = Color(0xFF111826)
val TextMidLight = Color(0xFF5A677D)
val TextLowLight = Color(0xFF8592A7)

// Semantic accents
val RatingGold = Color(0xFFFFC24B)
val Destructive = Color(0xFFFF6B6B)
val DestructiveContainerDark = Color(0xFF3A1620)
val DestructiveContainerLight = Color(0xFFFFE4E4)
val Success = Color(0xFF4ADE80)

// ─────────────────────────────────────────────────────────────────────────────
// Extended, non-Material tokens
//
// Material's ColorScheme is great for component defaults, but a "glass + glow"
// UI needs a few extra semantic colors. They live in a CompositionLocal so both
// themes can provide their own tone without branching at every call site.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class XiaobaoExtraColors(
    /** Main brand gradient for primary CTAs and the wordmark. */
    val brandGradient: List<Color>,
    /** Softer gradient for selected tabs / progress fills. */
    val brandGradientSoft: List<Color>,
    /** Radial glow blobs painted behind content. */
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    /** Floating chrome (navigation bar, rails) fill + hairline border. */
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassBorder: Color,
    /** Hairline border used on cards and posters. */
    val cardBorder: Color,
    /** Top-to-bottom scrim painted over hero imagery. */
    val heroScrim: List<Color>,
    /** Bottom scrim used over poster/hero art for text legibility. */
    val posterScrim: List<Color>,
    /** Selected nav indicator tint. */
    val navIndicator: Color,
    /** Skeleton shimmer colors. */
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    /** Track color behind progress bars / rings. */
    val progressTrack: Color,
    /** Rating star color. */
    val rating: Color,
    /** Scrim behind floating player controls. */
    val playerScrim: Color,
)

val DarkExtraColors = XiaobaoExtraColors(
    brandGradient = listOf(Color(0xFF5B7CFA), Color(0xFF8B5CF6)),
    brandGradientSoft = listOf(Color(0xFF6D8BFF), Color(0xFFA78BFA)),
    glowPrimary = Color(0xFF3D5AFE),
    glowSecondary = Color(0xFF8B5CF6),
    glowTertiary = Color(0xFF22D3EE),
    glassFill = Color(0xE6101A2C),
    glassFillStrong = Color(0xF5141E31),
    glassBorder = Color(0x2EFFFFFF),
    cardBorder = Color(0x14FFFFFF),
    heroScrim = listOf(
        Color.Transparent,
        Color(0x14000000),
        Color(0x8C050810),
        Color(0xF2050810)
    ),
    posterScrim = listOf(Color.Transparent, Color(0xA6030509)),
    navIndicator = Color(0xFF8CA5FF),
    shimmerBase = Color(0xFF16223A),
    shimmerHighlight = Color(0xFF23334F),
    progressTrack = Color(0x33FFFFFF),
    rating = RatingGold,
    playerScrim = Color(0xCC000000),
)

val LightExtraColors = XiaobaoExtraColors(
    brandGradient = listOf(Color(0xFF3D5BD9), Color(0xFF7C4DFF)),
    brandGradientSoft = listOf(Color(0xFF5B7CFA), Color(0xFF9B7BFF)),
    glowPrimary = Color(0xFF6D8BFF),
    glowSecondary = Color(0xFFA78BFA),
    glowTertiary = Color(0xFF67E8F9),
    glassFill = Color(0xE6FFFFFF),
    glassFillStrong = Color(0xFAFFFFFF),
    glassBorder = Color(0x240B1533),
    cardBorder = Color(0x14101826),
    heroScrim = listOf(
        Color.Transparent,
        Color(0x14000000),
        Color(0x99000000),
        Color(0xF2000000)
    ),
    posterScrim = listOf(Color.Transparent, Color(0xA6030509)),
    navIndicator = Color(0xFF3D5BD9),
    shimmerBase = Color(0xFFDCE4F2),
    shimmerHighlight = Color(0xFFF5F9FF),
    progressTrack = Color(0x1F0B1533),
    rating = Color(0xFFE8890D),
    playerScrim = Color(0xCC000000),
)

val LocalXiaobaoColors = staticCompositionLocalOf { DarkExtraColors }

// ─────────────────────────────────────────────────────────────────────────────
// Legacy aliases kept so older call sites keep compiling while screens migrate.
// ─────────────────────────────────────────────────────────────────────────────

val Primary = BrandIndigoDeep
val PrimaryDark = Color(0xFF2F49B8)
val PrimaryLight = BrandIndigo
val PrimaryContainer = Color(0xFFD7E1FF)
val OnPrimaryContainer = Color(0xFF0A1633)

val Secondary = BrandAmberDeep
val SecondaryContainer = Color(0xFFFFE7CC)
val OnSecondaryContainer = Color(0xFF331200)

val Tertiary = Color(0xFF0E8C8C)
val TertiaryContainer = Color(0xFFCEFAF5)
val OnTertiaryContainer = Color(0xFF00201D)

val SurfaceLight = Porcelain2
val SurfaceDark = Ink2
val SurfaceVariantLight = Porcelain4
val SurfaceVariantDark = Ink4

val BackgroundLight = Porcelain0
val BackgroundDark = Ink1

val NavBarBackground = Color(0xB30E1626)

val Error = Color(0xFFE5484D)
val ErrorContainer = DestructiveContainerLight
val OnErrorContainer = Color(0xFF410002)

val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackgroundLight = TextHighLight
val OnBackgroundDark = TextHighDark
val OnSurfaceLight = TextHighLight
val OnSurfaceDark = TextHighDark
val OnSurfaceVariantLight = TextMidLight
val OnSurfaceVariantDark = TextMidDark

val ScoreHigh = Success
val ScoreMedium = BrandAmberDeep
val ScoreLow = Color(0xFFF44336)
val ScoreStar = RatingGold

val BadgeUpdating = BrandIndigoBright
val BadgeCompleted = Success
val BadgeNew = BrandAmberDeep

val PromoBadge = Color(0xFFF59E0B)
val PromoBadgeText = Color.White
