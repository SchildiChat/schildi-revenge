package chat.schildi.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.safeLookup
import chat.schildi.revenge.preferences.LocalScPreferencesStore
import chat.schildi.revenge.preferences.value

data class MessageStyle(
    val textStyle: TextStyle = TextStyle.Default.copy(textDirection = TextDirection.Content),
    val otherSideMargin: Dp = ScPrefs.MESSAGE_OTHER_SIDE_MARGIN.defaultValue.dp,
    val maxWidth: Dp = ScPrefs.MESSAGE_MAX_WIDTH.defaultValue.dp,
) {
    companion object {
        fun from(
            bodyMedium: TextStyle,
            lookup: (ScPref<*>) -> Any?
        ): MessageStyle {
            val fontSize = ScPrefs.MESSAGE_FONT_SIZE.safeLookup(lookup)
            val textStyle = bodyMedium.copy(
                fontSize = fontSize.sp,
                lineHeight = bodyMedium.lineHeight * (fontSize / bodyMedium.fontSize.value),
                textDirection = TextDirection.Content,
            )
            return MessageStyle(
                textStyle = textStyle,
                otherSideMargin = ScPrefs.MESSAGE_OTHER_SIDE_MARGIN.safeLookup(lookup).dp,
                maxWidth = ScPrefs.MESSAGE_MAX_WIDTH.safeLookup(lookup).dp,
            )
        }
    }
}

val LocalMessageStyle = compositionLocalOf { MessageStyle() }
internal val LocalScExposures = staticCompositionLocalOf { scdExposures }

fun getThemeExposures(darkTheme: Boolean) = when {
    darkTheme -> scdExposures
    else -> sclExposures
}

val MaterialTheme.scExposures: ScThemeExposures
    @Composable
    get() = LocalScExposures.current

@Composable
fun prefersDarkTheme(): Boolean {
    return if (ScPrefs.THEME_FOLLOW_SYSTEM.value()) {
        isSystemInDarkTheme()
    } else {
        ScPrefs.THEME_DARK.value()
    }
}

@Composable
fun ScTheme(
    darkTheme: Boolean = prefersDarkTheme(),
    content: @Composable () -> Unit,
) {
    val currentExposures = remember {
        scdExposures.copy()
    }.apply { updateColorsFrom(getThemeExposures(darkTheme)) }

    val colorScheme = if (darkTheme) scdMaterialColorScheme else sclMaterialColorScheme
    // Latest font wins
    val textStyle = TextStyle(fontFamily = rememberEmojiFontFamily())
            .merge(fontFamily = rememberInterFontFamily())
    val typography = MaterialTheme.typography.let {
        it.copy(
            displayLarge = it.displayLarge.merge(textStyle),
            displayMedium = it.displayMedium.merge(textStyle),
            displaySmall = it.displaySmall.merge(textStyle),
            headlineLarge = it.headlineLarge.merge(textStyle),
            headlineMedium = it.headlineMedium.merge(textStyle),
            headlineSmall = it.headlineSmall.merge(textStyle),
            titleLarge = it.titleLarge.merge(textStyle),
            titleMedium = it.titleMedium.merge(textStyle),
            titleSmall = it.titleSmall.merge(textStyle),
            bodyLarge = it.bodyLarge.merge(textStyle),
            bodyMedium = it.bodyMedium.merge(textStyle),
            bodySmall = it.bodySmall.merge(textStyle),
            labelLarge = it.labelLarge.merge(textStyle),
            labelMedium = it.labelMedium.merge(textStyle),
            labelSmall = it.labelSmall.merge(textStyle),
        )
    }

    // Message font size via setting
    val scPreferencesStore = LocalScPreferencesStore.current
    val bodyMedium = typography.bodyMedium
    val messageStyle = scPreferencesStore.combinedSettingFlow { lookup ->
        MessageStyle.from(bodyMedium, lookup)
    }.collectAsState(
        MessageStyle.from(bodyMedium) {
            scPreferencesStore.getCachedOrDefaultValue(it)
        }
    ).value

    CompositionLocalProvider(
        LocalScExposures provides currentExposures,
        LocalContentColor provides colorScheme.onSurfaceVariant,
        LocalMessageStyle provides messageStyle,
        //androidx.compose.material.LocalContentColor provides colorScheme.onSurfaceVariant,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
            typography = typography,
        )
    }
}

// Calculate the color as if with alpha on white background
fun Color.fakeAlpha(alpha: Float) = Color(
    1f - alpha * (1f - red),
    1f - alpha * (1f - green),
    1f - alpha * (1f - blue),
    1f,
)


@Composable
fun scLinkStyle() = TextLinkStyles(
    style = SpanStyle(
        color = MaterialTheme.scExposures.linkColor,
    )
)
