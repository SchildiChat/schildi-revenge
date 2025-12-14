package chat.schildi.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Stable
class ScThemeExposures(
    horizontalDividerThickness: Dp,
    accentColor: Color,
    colorOnAccent: Color,
    bubbleBgIncoming: Color,
    bubbleBgOutgoing: Color,
    mentionBadgeColor: Color,
    notificationBadgeColor: Color,
    unreadBadgeColor: Color,
    unreadBadgeOnToolbarColor: Color,
    appBarBg: Color?,
    bubbleRadius: Dp,
    timestampRadius: Dp,
    commonLayoutRadius: Dp,
    timestampOverlayBg: Color,
    timestampOverlayFgOnBg: Color,
    unreadIndicatorLine: Color?,
    unreadIndicatorThickness: Dp,
    mentionFg: Color?,
    mentionBg: Color?,
    mentionBgOther: Color?,
    greenFg: Color?,
    greenBg: Color?,
    messageHighlightBg: Color?,
    composerBlockBg: Color?,
    composerBlockFg: Color?,
    spaceBarBg: Color?,
) {
    var horizontalDividerThickness by mutableStateOf(horizontalDividerThickness)
        private set
    var accentColor by mutableStateOf(accentColor)
        private set
    var colorOnAccent by mutableStateOf(colorOnAccent)
        private set
    var bubbleBgIncoming by mutableStateOf(bubbleBgIncoming)
        private set
    var bubbleBgOutgoing by mutableStateOf(bubbleBgOutgoing)
        private set
    var mentionBadgeColor by mutableStateOf(mentionBadgeColor)
        private set
    var notificationBadgeColor by mutableStateOf(notificationBadgeColor)
        private set
    var unreadBadgeColor by mutableStateOf(unreadBadgeColor)
        private set
    var unreadBadgeOnToolbarColor by mutableStateOf(unreadBadgeOnToolbarColor)
        private set
    var appBarBg by mutableStateOf(appBarBg)
        private set
    var bubbleRadius by mutableStateOf(bubbleRadius)
        private set
    var commonLayoutRadius by mutableStateOf(commonLayoutRadius)
        private set
    var timestampRadius by mutableStateOf(timestampRadius)
        private set
    var timestampOverlayBg by mutableStateOf(timestampOverlayBg)
        private set
    var timestampOverlayFgOnBg by mutableStateOf(timestampOverlayFgOnBg)
        private set
    var unreadIndicatorLine by mutableStateOf(unreadIndicatorLine)
        private set
    var unreadIndicatorThickness by mutableStateOf(unreadIndicatorThickness)
        private set
    var mentionFg by mutableStateOf(mentionFg)
        private set
    var mentionBg by mutableStateOf(mentionBg)
        private set
    var mentionBgOther by mutableStateOf(mentionBgOther)
        private set
    var greenFg by mutableStateOf(greenFg)
        private set
    var greenBg by mutableStateOf(greenBg)
        private set
    var messageHighlightBg by mutableStateOf(messageHighlightBg)
        private set
    var composerBlockBg by mutableStateOf(composerBlockBg)
        private set
    var composerBlockFg by mutableStateOf(composerBlockFg)
        private set
    var spaceBarBg by mutableStateOf(spaceBarBg)
        private set

    fun copy(
        horizontalDividerThickness: Dp = this.horizontalDividerThickness,
        accentColor: Color = this.accentColor,
        colorOnAccent: Color = this.colorOnAccent,
        bubbleBgIncoming: Color = this.bubbleBgIncoming,
        bubbleBgOutgoing: Color = this.bubbleBgOutgoing,
        mentionBadgeColor: Color = this.mentionBadgeColor,
        notificationBadgeColor: Color = this.notificationBadgeColor,
        unreadBadgeColor: Color = this.unreadBadgeColor,
        unreadBadgeOnToolbarColor: Color = this.unreadBadgeOnToolbarColor,
        appBarBg: Color? = this.appBarBg,
        bubbleRadius: Dp = this.bubbleRadius,
        commonLayoutRadius: Dp = this.commonLayoutRadius,
        timestampRadius: Dp = this.timestampRadius,
        timestampOverlayBg: Color = this.timestampOverlayBg,
        timestampOverlayFgOnBg: Color = this.timestampOverlayFgOnBg,
        unreadIndicatorLine: Color? = this.unreadIndicatorLine,
        unreadIndicatorThickness: Dp = this.unreadIndicatorThickness,
        mentionFg: Color? = this.mentionFg,
        mentionBg: Color? = this.mentionBg,
        mentionBgOther: Color? = this.mentionBgOther,
        greenFg: Color? = this.greenFg,
        greenBg: Color? = this.greenBg,
        messageHighlightBg: Color? = this.messageHighlightBg,
        composerBlockBg: Color? = this.composerBlockBg,
        composerBlockFg: Color? = this.composerBlockFg,
        spaceBarBg: Color? = this.spaceBarBg,
    ) = ScThemeExposures(
        horizontalDividerThickness = horizontalDividerThickness,
        accentColor = accentColor,
        colorOnAccent = colorOnAccent,
        bubbleBgIncoming = bubbleBgIncoming,
        bubbleBgOutgoing = bubbleBgOutgoing,
        mentionBadgeColor = mentionBadgeColor,
        notificationBadgeColor = notificationBadgeColor,
        unreadBadgeColor = unreadBadgeColor,
        unreadBadgeOnToolbarColor = unreadBadgeOnToolbarColor,
        appBarBg = appBarBg,
        bubbleRadius = bubbleRadius,
        commonLayoutRadius = commonLayoutRadius,
        timestampRadius = timestampRadius,
        timestampOverlayBg = timestampOverlayBg,
        timestampOverlayFgOnBg = timestampOverlayFgOnBg,
        unreadIndicatorLine = unreadIndicatorLine,
        unreadIndicatorThickness = unreadIndicatorThickness,
        mentionFg = mentionFg,
        mentionBg = mentionBg,
        mentionBgOther = mentionBgOther,
        greenFg = greenFg,
        greenBg = greenBg,
        messageHighlightBg = messageHighlightBg,
        composerBlockBg = composerBlockBg,
        composerBlockFg = composerBlockFg,
        spaceBarBg = spaceBarBg,
    )

    fun updateColorsFrom(other: ScThemeExposures) {
        horizontalDividerThickness = other.horizontalDividerThickness
        accentColor = other.accentColor
        colorOnAccent = other.colorOnAccent
        bubbleBgIncoming = other.bubbleBgIncoming
        bubbleBgOutgoing = other.bubbleBgOutgoing
        mentionBadgeColor = other.mentionBadgeColor
        notificationBadgeColor = other.notificationBadgeColor
        unreadBadgeColor = other.unreadBadgeColor
        unreadBadgeOnToolbarColor = other.unreadBadgeOnToolbarColor
        appBarBg = other.appBarBg
        bubbleRadius = other.bubbleRadius
        commonLayoutRadius = other.commonLayoutRadius
        timestampRadius = other.timestampRadius
        timestampOverlayBg = other.timestampOverlayBg
        timestampOverlayFgOnBg = other.timestampOverlayFgOnBg
        unreadIndicatorLine = other.unreadIndicatorLine
        unreadIndicatorThickness = other.unreadIndicatorThickness
        mentionFg = other.mentionFg
        mentionBg = other.mentionBg
        mentionBgOther = other.mentionBgOther
        greenFg = other.greenFg
        greenBg = other.greenBg
        messageHighlightBg = other.messageHighlightBg
        composerBlockBg = other.composerBlockBg
        composerBlockFg = other.composerBlockFg
        spaceBarBg = other.spaceBarBg
    }
}
