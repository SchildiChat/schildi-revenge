package chat.schildi.theme

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val scl_fgPrimary = ScColors.colorBlackAlpha_de
val scl_fgSecondary = ScColors.colorBlackAlpha_8a
val scl_fgTertiary = ScColors.colorBlackAlpha_4c
val scl_fgHint = ScColors.colorBlackAlpha_4c
val scl_fgDisabled = ScColors.colorBlackAlpha_4c
val scl_bg = ScColors.colorWhite_fa
val scl_bgFloating = ScColors.colorWhite
val scl_bgDarker = ScColors.colorWhite_fa
val scl_bgBlack = ScColors.colorWhite_ee
val scl_divider = ScColors.colorBlackAlpha_1f
val scl_accent = ScColors.colorAccentGreen
const val scl_icon_alpha = 0.5f

internal val sclMaterialColorScheme = lightColorScheme(
    primary = scl_fgPrimary,
    onPrimary = scd_fgPrimary,
    primaryContainer = scl_bg,
    onPrimaryContainer = scl_fgPrimary,
    inversePrimary = scl_bg,

    secondary = scl_fgSecondary,
    onSecondary = scd_fgPrimary,
    secondaryContainer = scl_bgBlack,
    onSecondaryContainer = scl_fgSecondary,

    tertiary = scl_fgSecondary,
    onTertiary = scl_fgTertiary,
    tertiaryContainer = scl_bgBlack,
    onTertiaryContainer = scl_fgTertiary,

    background = scl_bg,
    onBackground = scl_fgPrimary,
    surface = scl_bg,
    onSurface = scl_fgPrimary,
    surfaceVariant = scl_bgFloating,
    onSurfaceVariant = scl_fgSecondary,
    surfaceTint = scl_bgFloating,
    inverseSurface = scd_bg,
    inverseOnSurface = scd_fgPrimary,

    error = ScColors.colorAccentRed,
    onError = scl_fgPrimary,
    errorContainer = ScColors.colorAccentRed,
    onErrorContainer = scl_fgPrimary,
    outline = scl_fgTertiary,
    outlineVariant = scl_divider, // This is the divider color, as per androidx.compose.material3.DividerTokens (propagated to androidx.compose.material3.DividerDefaults.color)
    scrim = ScColors.colorBlackAlpha_1f,
)

internal val sclExposures = ScThemeExposures(
    horizontalDividerThickness = DividerDefaults.Thickness,
    accentColor = scl_accent,
    colorOnAccent = ScColors.colorWhite,
    bubbleBgIncoming = ScColors.colorWhite_ee,
    bubbleBgOutgoing = scl_accent.fakeAlpha(0.12f),
    mentionBadgeColor = Color(0xffd51928),
    notificationBadgeColor = ScColors.colorAccentGreen,
    unreadBadgeColor = ScColors.colorGray_73,
    unreadBadgeOnToolbarColor = ScColors.colorGray_73,
    appBarBg = scl_bg,
    bubbleRadius = 10.dp,
    commonLayoutRadius = 10.dp,
    timestampRadius = 6.dp,
    timestampOverlayBg = ScColors.colorBlackAlpha_80,
    timestampOverlayFgOnBg = scd_fgSecondary,
    unreadIndicatorLine = ScColors.colorAccentGreen,
    unreadIndicatorThickness = 2.dp,
    mentionFg = ScColors.colorWhite,
    mentionBg = ScColors.colorAccentRed,
    mentionBgOther = ScColors.colorWhite_cf,
    greenFg = ScColors.colorAccentGreen,
    greenBg = ScColors.colorAccentGreenAlpha_21,
    messageHighlightBg = ScColors.colorAccentGreenAlpha_80,
    composerBlockBg = null,
    composerBlockFg = null,
    spaceBarBg = ScColors.colorWhite_ee,
)
