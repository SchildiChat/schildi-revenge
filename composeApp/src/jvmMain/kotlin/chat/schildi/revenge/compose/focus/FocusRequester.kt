package chat.schildi.revenge.compose.focus

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusRequester

interface AbstractFocusRequester {
    fun requestFocus(focusDirection: FocusDirection = Enter): Boolean
}

class FocusRequesterWrapper(
    val instance: FocusRequester,
) : AbstractFocusRequester {
    override fun requestFocus(focusDirection: FocusDirection) = instance.requestFocus(focusDirection)
}
