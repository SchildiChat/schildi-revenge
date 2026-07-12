package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

actual fun Modifier.horizontalScrollFromVerticalWheel(scrollState: ScrollState, scrollAmount: Dp): Modifier = this
