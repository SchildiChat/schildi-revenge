package chat.schildi.revenge.compose.mouse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import chat.schildi.revenge.Anim
import chat.schildi.revenge.actions.LocalKeyboardActionHandler

@Composable
fun TopNavigation(content: @Composable () -> Unit) {
    val keyboardActionHandler = LocalKeyboardActionHandler.current
    AnimatedVisibility(
        visible = !keyboardActionHandler.keyboardPrimary.collectAsState().value,
        enter = slideInVertically(tween(Anim.DURATION)) { -it } +
                expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Top),
        exit = slideOutVertically(tween(Anim.DURATION)) { -it } +
                shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Top),
    ) {
        content()
    }
}
