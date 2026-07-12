package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value

@Composable
internal fun rememberScaledDensity(): Density {
    val renderScale = ScPrefs.RENDER_SCALE.value()
    val fontScale = ScPrefs.FONT_SCALE.value()
    val rootDensity = LocalDensity.current
    return remember(rootDensity, renderScale, fontScale) {
        if (renderScale == 1f && fontScale == 1f) {
            rootDensity
        } else {
            Density(
                density = rootDensity.density * renderScale,
                fontScale = rootDensity.fontScale * fontScale,
            )
        }
    }
}
