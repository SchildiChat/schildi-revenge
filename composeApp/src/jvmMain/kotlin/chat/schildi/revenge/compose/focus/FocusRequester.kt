package chat.schildi.revenge.compose.focus

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import chat.schildi.revenge.actions.KeyboardActionHandler
import java.util.UUID

sealed interface AbstractFocusRequester {
    fun requestFocus(focusDirection: FocusDirection = Enter): Boolean
}

class FocusRequesterWrapper(
    val instance: FocusRequester,
) : AbstractFocusRequester {
    override fun requestFocus(focusDirection: FocusDirection) = instance.requestFocus(focusDirection)
}

class FakeFocusRequester(val keyHandler: KeyboardActionHandler, val id: UUID) : AbstractFocusRequester {
    override fun requestFocus(focusDirection: FocusDirection): Boolean {
        keyHandler.onFocusChanged(id, object : FocusState {
            override val isFocused = true
            override val hasFocus = false
            override val isCaptured = false

        })
        return true
    }
}
