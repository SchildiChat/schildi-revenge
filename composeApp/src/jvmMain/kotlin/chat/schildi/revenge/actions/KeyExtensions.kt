package chat.schildi.revenge.actions

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed

val KeyEvent.isOnlyShiftPressed: Boolean
    get() = isShiftPressed && !isCtrlPressed && !isAltPressed && !isMetaPressed
val KeyEvent.isOnlyCtrlPressed: Boolean
    get() = isCtrlPressed && !isShiftPressed && !isAltPressed && !isMetaPressed
val KeyEvent.isNoModifierPressed: Boolean
    get() = !isShiftPressed && !isCtrlPressed && !isAltPressed && !isMetaPressed
