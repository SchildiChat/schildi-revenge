package chat.schildi.revenge.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import chat.schildi.revenge.actions.LocalKeyboardActionHandler

@Composable
fun SelectionContainerUnlessTouch(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isTouch = LocalKeyboardActionHandler.current.lastPointerType.collectAsState().value == PointerType.Touch
    if (isTouch) {
        Box(modifier) {
            content()
        }
    } else {
        SelectionContainer(modifier, content)
    }
}
