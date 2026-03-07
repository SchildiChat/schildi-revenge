package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.AdaptiveSplitLayoutModifierPair
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.components.WEIGHT_DEFAULT
import chat.schildi.revenge.compose.components.prefWidthModifiers
import chat.schildi.revenge.compose.destination.AboutScreen
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.destination.conversation.ConversationScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.MessageReactionsScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.RoomMembersScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.settings.SettingsScreen
import chat.schildi.revenge.compose.destination.split.EmptyPaneScreen
import chat.schildi.revenge.compose.destination.split.InboxConversationMultiPaneScreen
import chat.schildi.revenge.compose.destination.split.SplitHorizontal
import chat.schildi.revenge.compose.destination.split.SplitVertical

val LocalDestinationDepth = compositionLocalOf { 0 }

@Composable
fun DestinationContent(
    destinationHolder: DestinationStateHolder,
    modifier: Modifier = Modifier,
) {
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
                is Destination.SettingsPane -> SettingsScreen(destination, baseModifier, contentModifier)
                is Destination.MultiPanePlaceholder -> EmptyPaneScreen(baseModifier, contentModifier)
                is Destination.InboxConversationMultiPane -> InboxConversationMultiPaneScreen(destination, baseModifier, contentModifier)
            }
        }
    }
}

@Composable
private fun Modifier.forDestination(destination: Destination): AdaptiveSplitLayoutModifierPair {
    val (pref, weight) = when (destination) {
        Destination.Inbox -> Pair(ScPrefs.MAX_WIDTH_INBOX, ScPrefs.LAYOUT_WEIGHT_INBOX.value())
        is Destination.Conversation -> Pair(ScPrefs.MAX_WIDTH_CONVERSATION, ScPrefs.LAYOUT_WEIGHT_CONVERSATION.value())
        is Destination.MessageReactions,
        is Destination.RoomMembers -> Pair(ScPrefs.MAX_WIDTH_ROOM_DETAILS, ScPrefs.LAYOUT_WEIGHT_ROOM_DETAILS.value())
        Destination.AccountManagement,
        Destination.About -> Pair(ScPrefs.MAX_WIDTH_SETTINGS, ScPrefs.LAYOUT_WEIGHT_SETTINGS.value())
        is Destination.SettingsPane -> Pair(
            ScPrefs.MAX_WIDTH_SETTINGS,
            if (destination.rootPreferenceCategory == null) {
                ScPrefs.LAYOUT_WEIGHT_SETTINGS_ROOT.value()
            } else {
                ScPrefs.LAYOUT_WEIGHT_SETTINGS.value()
            }
        )
        is Destination.SplitHorizontal,
        is Destination.SplitVertical,
        is Destination.MultiPane,
        Destination.Splash -> Pair(null, WEIGHT_DEFAULT)
        is Destination.MultiPanePlaceholder -> when (destination.category) {
            DestinationCategory.INBOX -> Pair(ScPrefs.MAX_WIDTH_INBOX, ScPrefs.LAYOUT_WEIGHT_INBOX.value())
            DestinationCategory.CONVERSATION -> Pair(ScPrefs.MAX_WIDTH_CONVERSATION, ScPrefs.LAYOUT_WEIGHT_CONVERSATION.value())
            DestinationCategory.CONVERSATION_DETAILS -> Pair(ScPrefs.MAX_WIDTH_ROOM_DETAILS, ScPrefs.LAYOUT_WEIGHT_ROOM_DETAILS.value())
            DestinationCategory.ABOUT,
            DestinationCategory.SETTINGS -> Pair(ScPrefs.MAX_WIDTH_SETTINGS, ScPrefs.LAYOUT_WEIGHT_SETTINGS.value())
            DestinationCategory.WILDCARD -> Pair(null, WEIGHT_DEFAULT)
        }
    }
    return if (pref == null) {
        AdaptiveSplitLayoutModifierPair(this, Modifier, weight)
    } else {
        val pair = prefWidthModifiers(pref, weight = weight)
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
