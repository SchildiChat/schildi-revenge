package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.destination.conversation.event.reaction.ReactionContent
import chat.schildi.revenge.model.userlist.UserReactionItem
import chat.schildi.revenge.model.userlist.MessageReactionListViewModel

@Composable
fun UserReactionRow(
    reactionItem: UserReactionItem,
    viewModel: MessageReactionListViewModel,
    modifier: Modifier = Modifier,
) {
    UserListRow(
        item = reactionItem,
        viewModel = viewModel,
        modifier = modifier,
        leadingContent = {
            SelectionContainer {
                ReactionContent(
                    reaction = reactionItem.reaction,
                    baseStyle = MaterialTheme.typography.headlineLarge,
                )
            }
            Spacer(Modifier.width(Dimens.horizontalItemPadding))
        }
    ) {
        Text(
            DateTimeFormat.formatTimeOrDateTime(reactionItem.reactionSender.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
