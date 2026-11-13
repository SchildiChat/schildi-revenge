package chat.schildi.revenge.actions

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

internal actual val KeyEvent.layoutAwareKey: Key
    get() = key
