package chat.schildi.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
fun ScTheme(
    darkTheme: Boolean = true, // TODO
    content: @Composable () -> Unit,
) {
    val currentExposures = remember {
        scdExposures.copy()
    }.apply { updateColorsFrom(getThemeExposures(darkTheme)) }

    val colorScheme = if (darkTheme) scdMaterialColorScheme else sclMaterialColorScheme

    CompositionLocalProvider(
        LocalScExposures provides currentExposures,
        LocalContentColor provides colorScheme.onSurfaceVariant,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
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
