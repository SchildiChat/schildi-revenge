package chat.schildi.revenge.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.AdaptiveSplitLayoutModifierPair
import chat.schildi.revenge.compose.components.ComposeSessionScope
import chat.schildi.revenge.compose.components.WEIGHT_DEFAULT
import chat.schildi.revenge.compose.components.prefWidthModifiers
import chat.schildi.revenge.compose.destination.AboutScreen
import chat.schildi.revenge.compose.destination.devtools.AccountDevToolsScreen
import chat.schildi.revenge.compose.destination.AccountManagementScreen
import chat.schildi.revenge.compose.destination.DiagnosticsScreen
import chat.schildi.revenge.compose.destination.SessionSelectorScreen
import chat.schildi.revenge.compose.destination.SplashScreen
import chat.schildi.revenge.compose.destination.conversation.ConversationScreen
import chat.schildi.revenge.compose.destination.devtools.RoomDevToolsScreen
import chat.schildi.revenge.compose.destination.conversation.RoomDetailsScreen
import chat.schildi.revenge.compose.destination.conversation.UserDetailsScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.MessageReactionsScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.MessageReadReceiptsScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.RoomMembersScreen
import chat.schildi.revenge.compose.destination.inbox.InboxScreen
import chat.schildi.revenge.compose.destination.settings.SettingsScreen
import chat.schildi.revenge.compose.destination.split.ConversationDetailsMultiPaneScreen
import chat.schildi.revenge.compose.destination.split.EmptyPaneScreen
import chat.schildi.revenge.compose.destination.split.InboxConversationMultiPaneScreen
import chat.schildi.revenge.compose.destination.split.SplitHorizontal
import chat.schildi.revenge.compose.destination.split.SplitVertical
import chat.schildi.revenge.compose.destination.verification.VerificationRequestScreen
import chat.schildi.revenge.config.keybindings.DestinationEnum

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
                is Destination.RoomDetails -> RoomDetailsScreen(destination, baseModifier, contentModifier)
                is Destination.RoomMembers -> RoomMembersScreen(destination, baseModifier, contentModifier)
                is Destination.MessageReactions -> MessageReactionsScreen(destination, baseModifier, contentModifier)
                is Destination.MessageReadReceipts -> MessageReadReceiptsScreen(destination, baseModifier, contentModifier)
                is Destination.UserDetails -> UserDetailsScreen(destination, baseModifier, contentModifier)
                is Destination.AccountDevTools -> AccountDevToolsScreen(destination, baseModifier, contentModifier)
                is Destination.RoomDevTools -> RoomDevToolsScreen(destination, baseModifier, contentModifier)
                is Destination.SplitHorizontal -> SplitHorizontal(destination, baseModifier, contentModifier)
                is Destination.SplitVertical -> SplitVertical(destination, baseModifier, contentModifier)
                is Destination.About -> AboutScreen(destination, baseModifier, contentModifier)
                is Destination.Diagnostics -> DiagnosticsScreen(destination, baseModifier, contentModifier)
                is Destination.VerificationRequest -> VerificationRequestScreen(destination, baseModifier, contentModifier)
                is Destination.SessionSelector -> SessionSelectorScreen(destination, baseModifier, contentModifier)
                is Destination.Settings -> SettingsScreen(destination, baseModifier, contentModifier)
                is Destination.SettingsPane -> SettingsScreen(destination, baseModifier, contentModifier)
                is Destination.InboxConversationMultiPane -> InboxConversationMultiPaneScreen(destination, baseModifier, contentModifier)
                is Destination.ConversationDetailsMultiPane -> ConversationDetailsMultiPaneScreen(destination, baseModifier, contentModifier)
                is Destination.MultiPaneConversationPlaceholder,
                is Destination.MultiPaneRoomInfoPlaceholder,
                is Destination.MultiPaneSettingsPlaceholder -> EmptyPaneScreen(baseModifier, contentModifier)
            }
        }
    }
}

private data class DestinationMeasure(
    val maxWidth: Int?,
    val weight: Int,
) {
    operator fun plus(other: DestinationMeasure) = DestinationMeasure(
        maxWidth = if (maxWidth == null || other.maxWidth == null) null else maxWidth + other.maxWidth,
        weight = weight + other.weight,
    )

    companion object {
        @Composable
        fun from(maxWidth: ScPref<Int>?, weight: ScPref<Int>) = DestinationMeasure(
            maxWidth = maxWidth?.value(),
            weight = weight.value(),
        )
        @Composable
        fun from(maxWidth: ScPref<Int>?, weight: Int) = DestinationMeasure(
            maxWidth = maxWidth?.value(),
            weight = weight,
        )
    }
}

@Composable
private fun DestinationEnum.measureInfo(): DestinationMeasure = when (this) {
    DestinationEnum.Inbox -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_INBOX, ScPrefs.LAYOUT_WEIGHT_INBOX)
    DestinationEnum.Conversation -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_CONVERSATION, ScPrefs.LAYOUT_WEIGHT_CONVERSATION)
    DestinationEnum.SplitRoomDetailsPlaceholder,
    DestinationEnum.RoomDetails,
    DestinationEnum.MessageReactions,
    DestinationEnum.MessageReadReceipts,
    DestinationEnum.RoomMembers,
    DestinationEnum.UserDetails -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_ROOM_DETAILS, ScPrefs.LAYOUT_WEIGHT_ROOM_DETAILS)
    DestinationEnum.ConversationPins -> DestinationMeasure.from(
        ScPrefs.MAX_WIDTH_CONVERSATION,
        ScPrefs.LAYOUT_WEIGHT_ROOM_DETAILS,
    )
    DestinationEnum.ConversationThread -> DestinationMeasure.from(
        ScPrefs.MAX_WIDTH_CONVERSATION,
        if (ScPrefs.ALLOW_THREADS_IN_DETAILS_PANE.value()) ScPrefs.LAYOUT_WEIGHT_ROOM_DETAILS else ScPrefs.LAYOUT_WEIGHT_CONVERSATION
    )
    DestinationEnum.AccountManagement,
    DestinationEnum.Diagnostics,
    DestinationEnum.VerificationRequest,
    DestinationEnum.SessionSelector,
    DestinationEnum.About -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_SETTINGS, ScPrefs.LAYOUT_WEIGHT_SETTINGS)
    DestinationEnum.Settings -> DestinationEnum.SettingsRoot.measureInfo() + DestinationEnum.SplitSettingsDetailsPlaceholder.measureInfo()
    DestinationEnum.SettingsRoot -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_SETTINGS, ScPrefs.LAYOUT_WEIGHT_SETTINGS_ROOT)
    DestinationEnum.SplitSettingsDetailsPlaceholder,
    DestinationEnum.SettingsDetails -> DestinationMeasure.from(ScPrefs.MAX_WIDTH_SETTINGS, ScPrefs.LAYOUT_WEIGHT_SETTINGS)
    DestinationEnum.SplitConversationPlaceholder -> if (ScPrefs.PREFER_CONVERSATION_DETAILS_SPLIT.value()) {
        DestinationEnum.ConversationDetailsSplit.measureInfo()
    } else {
        DestinationEnum.Conversation.measureInfo()
    }
    DestinationEnum.InboxConversationSplit -> DestinationEnum.Inbox.measureInfo() + DestinationEnum.SplitConversationPlaceholder.measureInfo()
    DestinationEnum.ConversationDetailsSplit -> DestinationEnum.Conversation.measureInfo() + DestinationEnum.SplitRoomDetailsPlaceholder.measureInfo()
    DestinationEnum.SplitHorizontal,
    DestinationEnum.SplitVertical,
    DestinationEnum.AccountDevTools,
    DestinationEnum.RoomDevTools,
    DestinationEnum.Splash -> DestinationMeasure.from(null, WEIGHT_DEFAULT)
}

@Composable
private fun Modifier.forDestination(destination: Destination): AdaptiveSplitLayoutModifierPair {
    val (maxWidth, weight) = destination.type.measureInfo()
    return if (maxWidth == null) {
        AdaptiveSplitLayoutModifierPair(this, Modifier, weight)
    } else {
        val pair = prefWidthModifiers(maxWidth, weight = weight)
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
