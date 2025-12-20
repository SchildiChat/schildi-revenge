package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.AppMessage
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppMessages(messages: ImmutableList<AppMessage>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(messages, key = { it.uniqueId ?: it }) { message ->
            AnimatedVisibility(
                visible = message.dismissedTimestamp == null,
                enter = slideInVertically(tween(Anim.DURATION)) { it } +
                        expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                        shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
            ) {
                Text(
                    message.message,
                    color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Dimens.listPadding)
                )
            }
        }
    }
}
