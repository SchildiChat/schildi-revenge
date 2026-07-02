package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.conversation.ConversationViewModel
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_view
import shire.res.generated.resources.hint_composer_room_upgraded
import shire.res.generated.resources.hint_composer_room_upgraded_with_reason

@Composable
fun RoomUpgradeRow(
    viewModel: ConversationViewModel,
    modifier: Modifier = Modifier,
) {
    val info = viewModel.roomInfo.collectAsState().value
    val successorRoom = info?.successorRoom ?: return
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Dimens.horizontalItemPaddingBig, vertical = Dimens.listPadding),
        verticalArrangement = Dimens.verticalArrangement,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Dimens.horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val text = successorRoom.reason?.let { reason ->
                stringResource(Res.string.hint_composer_room_upgraded_with_reason, reason)
            } ?: stringResource(Res.string.hint_composer_room_upgraded)
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            val destinationStateHolder = LocalDestinationState.current
            fun viewSuccessor(): Boolean {
                return destinationStateHolder?.navigate(
                    viewModel.successorRoomDestination.value ?: Destination.Conversation(
                        sessionId = viewModel.sessionId,
                        roomId = successorRoom.roomId,
                    ),
                ) != null
            }
            Button(
                onClick = ::viewSuccessor,
                modifier = Modifier
                    .keyFocusable(
                        role = FocusRole.NESTED_AUX_ITEM,
                        actionProvider = actionProvider(
                            primaryAction = InteractionAction.Invoke(::viewSuccessor),
                        ),
                        addClickListener = false,
                    ),
            ) {
                Text(stringResource(Res.string.action_view))
            }
        }
    }
}
