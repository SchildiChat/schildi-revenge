package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.pluralStringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.named_users_typing

@Composable
fun TypingUsersRow(
    typingUsers: List<UserId>,
    roomMembersById: ImmutableMap<UserId, RoomMember>,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        typingUsers,
        modifier,
        transitionSpec = {
            (
                fadeIn(
                    animationSpec = Dimens.tweenSmooth()
                ) togetherWith fadeOut(
                    animationSpec = Dimens.tweenSmooth()
                )
            ) using SizeTransform()
        },
    ) { typingUsers ->
        if (typingUsers.isEmpty()) {
            return@AnimatedContent
        }
        val text = pluralStringResource(
            Res.plurals.named_users_typing,
            typingUsers.size,
            typingUsers.joinToString {
                roomMembersById[it]?.disambiguatedDisplayName ?: it.value
            },
        )
        Text(
            text,
            Modifier.fillMaxWidth().padding(
                start = Dimens.windowPadding,
                end = Dimens.windowPadding,
                top = Dimens.listPadding,
            ),
            color = MaterialTheme.scExposures.accentColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
