package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.AbstractAppMessage
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.ConfirmActionAppMessage
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppMessages(messages: ImmutableList<AbstractAppMessage>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(messages, key = { it.uniqueId ?: it }) { message ->
            AnimatedVisibility(
                visible = message.dismissedTimestamp == null,
                enter = slideInVertically(tween(Anim.DURATION)) { it } +
                        expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Bottom),
                exit = slideOutVertically(tween(Anim.DURATION)) { it } +
                        shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Bottom),
            ) {
                Row(
                    Modifier.padding(Dimens.listPadding),
                    horizontalArrangement = Dimens.horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isError = (message as? AppMessage)?.isError == true
                    Text(
                        message.message.render(),
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (message is ConfirmActionAppMessage) {
                        val keyHandler = LocalKeyboardActionHandler.current
                        Button(
                            onClick = message.action,
                            modifier = Modifier.keyFocusable(
                                actionProvider = defaultActionProvider(
                                    primaryAction = InteractionAction.Invoke {
                                        message.action()
                                        true
                                    }
                                )
                            ),
                        ) {
                            Text(message.confirmText.render())
                        }
                        Button(
                            onClick = {
                                keyHandler.dismissMessage(message.uniqueId)
                            },
                            modifier = Modifier.keyFocusable(
                                actionProvider = defaultActionProvider(
                                    primaryAction = InteractionAction.Invoke {
                                        keyHandler.dismissMessage(message.uniqueId)
                                        true
                                    }
                                )
                            ),
                        ) {
                            Text(message.cancelText.render())
                        }
                    }
                }
            }
        }
    }
}
