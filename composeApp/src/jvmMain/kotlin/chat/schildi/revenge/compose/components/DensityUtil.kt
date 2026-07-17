package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.toPx() = LocalDensity.current.run { toPx() }
