package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.util.appendUrlText
import chat.schildi.theme.scLinkStyle
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.encryption.identity.IdentityState
import io.element.android.libraries.matrix.api.encryption.identity.IdentityStateChange
import io.element.android.libraries.matrix.api.room.RoomMember
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_acknowledge
import shire.composeapp.generated.resources.common_learn_more
import shire.composeapp.generated.resources.named_user_verification_pin_violation_summary
import shire.composeapp.generated.resources.named_user_verification_pinned_summary
import shire.composeapp.generated.resources.named_user_verification_verified_summary
import shire.composeapp.generated.resources.named_user_verification_violation_summary

@Composable
fun IdentityStateChangesRow(
    identityStateChanges: ImmutableList<IdentityStateChange>,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    acknowledge: (IdentityStateChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (identityStateChanges.isEmpty()) return
    Column(
        modifier.fillMaxWidth().padding(Dimens.Conversation.bottomStickyItemPadding),
        verticalArrangement = Dimens.verticalArrangement,
    ) {
        identityStateChanges.forEach { change ->
            val userName = roomMembersById[change.userId]?.displayName?.let { displayName ->
                if (displayName == change.userId.value) {
                    displayName
                } else {
                    buildString {
                        append(displayName)
                        append(" (")
                        append(change.userId.value)
                        append(")")
                    }
                }
            } ?: change.userId.value
            key(change.userId) {
                val message = when (change.identityState) {
                    IdentityState.Verified -> stringResource(Res.string.named_user_verification_verified_summary, userName)
                    IdentityState.Pinned -> stringResource(Res.string.named_user_verification_pinned_summary, userName)
                    IdentityState.PinViolation -> stringResource(Res.string.named_user_verification_pin_violation_summary, userName)
                    IdentityState.VerificationViolation -> stringResource(Res.string.named_user_verification_violation_summary, userName)
                }
                val text = buildAnnotatedString {
                    append(message)
                    append(" ")
                    appendUrlText(
                        "https://element.io/en/help#encryption18",
                        stringResource(Res.string.common_learn_more),
                        scLinkStyle()
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Dimens.horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { acknowledge(change) },
                        modifier = Modifier
                            .keyFocusable(
                                role = FocusRole.NESTED_AUX_ITEM,
                                actionProvider = actionProvider(
                                    primaryAction = InteractionAction.Invoke {
                                        acknowledge(change)
                                        true
                                    },
                                ),
                                addClickListener = false,
                            ),
                    ) {
                        Text(stringResource(Res.string.action_acknowledge))
                    }
                }
            }
        }
    }
}
