package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.conversation.ConversationScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.destination.split.SplitHorizontal
import chat.schildi.revenge.compose.destination.split.SplitVertical
import chat.schildi.revenge.Destination
import chat.schildi.revenge.compose.components.AdaptiveSplitLayoutModifierPair
import chat.schildi.revenge.compose.components.prefWidthModifiers
import chat.schildi.revenge.compose.destination.AboutScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.MessageReactionsScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.RoomMembersScreen
import chat.schildi.revenge.compose.destination.settings.SettingsScreen

val LocalDestinationDepth = compositionLocalOf { 0 }

@Composable
fun DestinationContent(destinationHolder: DestinationStateHolder, modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalDestinationState provides destinationHolder,
        LocalDestinationDepth provides LocalDestinationDepth.current + 1,
    ) {
        val destination = destinationHolder.state.collectAsState().value.destination
        val (baseModifier, contentModifier) = modifier.forDestination(destination)
        DestinationWrapper(destination) {
            when (destination) {
                is Destination.AccountManagement -> AccountManagementScreen(destination, baseModifier, contentModifier)
                is Destination.Inbox -> InboxScreen(destination, baseModifier, contentModifier)
                is Destination.Splash -> SplashScreen(baseModifier, contentModifier)
                is Destination.Conversation -> ConversationScreen(destination, baseModifier, contentModifier)
                is Destination.RoomMembers -> RoomMembersScreen(destination, baseModifier, contentModifier)
                is Destination.MessageReactions -> MessageReactionsScreen(destination, baseModifier, contentModifier)
                is Destination.SplitHorizontal -> SplitHorizontal(destination, baseModifier, contentModifier)
                is Destination.SplitVertical -> SplitVertical(destination, baseModifier, contentModifier)
                is Destination.About -> AboutScreen(baseModifier, contentModifier)
                is Destination.Settings -> SettingsScreen(destination, baseModifier, contentModifier)
            }
        }
    }
}

@Composable
private fun Modifier.forDestination(destination: Destination): AdaptiveSplitLayoutModifierPair {
    val pref = when (destination) {
        Destination.Inbox -> ScPrefs.MAX_WIDTH_INBOX
        is Destination.Conversation -> ScPrefs.MAX_WIDTH_CONVERSATION
        is Destination.MessageReactions,
        is Destination.RoomMembers -> ScPrefs.MAX_WIDTH_ROOM_DETAILS
        Destination.Settings,
        Destination.AccountManagement,
        Destination.About -> ScPrefs.MAX_WIDTH_SETTINGS
        is Destination.SplitHorizontal,
        is Destination.SplitVertical,
        Destination.Splash -> null
    }
    return if (pref == null) {
        AdaptiveSplitLayoutModifierPair(this, Modifier)
    } else {
        val pair = prefWidthModifiers(pref)
        pair.copy(outer = this.then(pair.outer))
    }
}

@Composable
private fun DestinationWrapper(
    destination: Destination,
    content: @Composable () -> Unit,
) {
    if (destination is Destination.WithSession) {
        ComposeSessionScope(destination.sessionId, content = content)
    } else {
        content()
    }
}
