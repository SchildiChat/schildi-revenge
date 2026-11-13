package chat.schildi.revenge.actions

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyLocation
import java.awt.event.KeyEvent.VK_UNDEFINED

internal actual val KeyEvent.layoutAwareKey: Key
    get() {
        val extendedKeyCode = awtEventOrNull?.extendedKeyCode
            ?.takeUnless { it == VK_UNDEFINED } ?: return key
        // AWT's legacy keyCode can disagree with the active XKB layout under Xwayland.
        return Key(nativeKeyCode = extendedKeyCode, nativeKeyLocation = key.nativeKeyLocation)
    }
