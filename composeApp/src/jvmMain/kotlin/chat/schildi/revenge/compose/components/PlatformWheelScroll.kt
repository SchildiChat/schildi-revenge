package chat.schildi.revenge.compose.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

expect fun Modifier.horizontalScrollFromVerticalWheel(
    scrollState: ScrollState,
    scrollAmount: Dp = 88.dp,
): Modifier
