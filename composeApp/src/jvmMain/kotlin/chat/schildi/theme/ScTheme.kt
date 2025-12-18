package chat.schildi.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value

// Element defaults to light compound colors, so follow that as fallback default for exposures as well
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
    val textStyle = TextStyle(fontFamily = rememberInterFontFamily())
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

    CompositionLocalProvider(
        LocalScExposures provides currentExposures,
        LocalContentColor provides colorScheme.onSurfaceVariant,
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
