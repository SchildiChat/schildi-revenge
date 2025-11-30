package chat.schildi.theme

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val scd_fgPrimary = ScColors.colorWhite
val scd_fgSecondary = ScColors.colorWhiteAlpha_b3
val scd_fgTertiary = ScColors.colorWhiteAlpha_80
val scd_fgHint = ScColors.colorWhiteAlpha_80
val scd_fgDisabled = ScColors.colorWhiteAlpha_80
val scd_bg = ScColors.colorGray_30
val scd_bgFloating = ScColors.colorGray_42
val scd_bgDarker = ScColors.colorGray_21
val scd_bgBlack = ScColors.colorBlack
val scd_divider = ScColors.colorWhiteAlpha_1f
val scd_accent = ScColors.colorAccentGreen
const val scd_icon_alpha = 0.5f

internal val scdMaterialColorScheme = darkColorScheme(
    primary = scd_fgPrimary,
    onPrimary = scl_fgPrimary,
    primaryContainer = scd_bgDarker,
    onPrimaryContainer = scd_fgPrimary,
    inversePrimary = scl_fgPrimary,

    secondary = scd_fgSecondary,
    onSecondary = scl_fgPrimary,
    secondaryContainer = scd_bg,
    onSecondaryContainer = scd_fgSecondary,

    tertiary = scd_fgTertiary,
    onTertiary = scl_fgTertiary,
    tertiaryContainer = scd_bgBlack,
    onTertiaryContainer = scd_fgTertiary,

    background = scd_bgDarker,
    onBackground = scd_fgPrimary,
    surface = scd_bgDarker,
    onSurface = scd_fgPrimary,
    surfaceVariant = scd_bgFloating,
    onSurfaceVariant = scd_fgSecondary,
    surfaceTint = scd_bgFloating,
    inverseSurface = scl_bgFloating,
    inverseOnSurface = scl_fgPrimary,

    error = ScColors.colorAccentRed,
    onError = scd_fgPrimary,
    errorContainer = ScColors.colorAccentRed,
    onErrorContainer = scd_fgPrimary,
    outline = scd_fgTertiary,
    outlineVariant = scd_divider, // This is the divider color, as per androidx.compose.material3.DividerTokens (propagated to androidx.compose.material3.DividerDefaults.color)
    scrim = ScColors.colorBlackAlpha_1f,
)

internal val scdExposures = ScThemeExposures(
    horizontalDividerThickness = DividerDefaults.Thickness,
    accentColor = scd_accent,
    colorOnAccent = ScColors.colorWhite,
    bubbleBgIncoming = scd_bgFloating,
    bubbleBgOutgoing = scd_bg,
    mentionBadgeColor = Color(0xffd51928),
    notificationBadgeColor = ScColors.colorAccentGreen,
    unreadBadgeColor = scd_bgFloating,
    unreadBadgeOnToolbarColor = ScColors.colorGray_61,
    appBarBg = scd_bg,
    bubbleRadius = 10.dp,
    commonLayoutRadius = 10.dp,
    timestampRadius = 6.dp,
    timestampOverlayBg = ScColors.colorBlackAlpha_80,
    unreadIndicatorLine = ScColors.colorAccentGreen,
    unreadIndicatorThickness = 2.dp,
    mentionFg = ScColors.colorWhite,
    mentionBg = ScColors.colorAccentRed,
    mentionBgOther = ScColors.colorGray_61,
    greenFg = ScColors.colorAccentGreen,
    greenBg = ScColors.colorAccentGreenAlpha_30,
    messageHighlightBg = ScColors.colorAccentGreenAlpha_80,
    composerBlockBg = scd_bgFloating,
    composerBlockFg = scd_fgPrimary,
    spaceBarBg = scd_bg,
)
