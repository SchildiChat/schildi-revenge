package chat.schildi.revenge.compose.destination.conversation.userlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.WithTooltip
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WithTooltip(reactionItem.shortcode) {
                        ReactionContent(
                            reaction = reactionItem.reaction,
                            reactionShortcode = reactionItem.shortcode,
                            baseStyle = if (reactionItem.reaction.length <= 4)
                                MaterialTheme.typography.headlineLarge
                            else
                                MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.width(Dimens.horizontalItemPadding))
        }
    ) {
        UserListTimestamp(reactionItem.reactionSender.timestamp)
    }
}
