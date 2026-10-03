package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.KeyboardActionMode
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.plainTextCopyActionWithUserId
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.EditableDropdownEntry
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.LocalFocusParent
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.model.conversation.StartChatCandidate
import chat.schildi.revenge.model.conversation.StartChatSearchState
import chat.schildi.revenge.model.conversation.StartChatState
import chat.schildi.revenge.model.conversation.StartChatViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.empty_screen_placeholder_search_in_progress
import shire.res.generated.resources.hint_start_chat_account
import shire.res.generated.resources.start_chat
import shire.res.generated.resources.start_chat_hint_idle
import shire.res.generated.resources.start_chat_no_results
import shire.res.generated.resources.start_chat_results_limited
import shire.res.generated.resources.start_chat_search_error

@Composable
fun StartChatScreen(
    destination: Destination.StartChat,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: StartChatViewModel = viewModel(
        key = viewModelKey(destination),
        factory = StartChatViewModel.factory(destination.initialSessionId),
    )
    publishTitle(viewModel)

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides listAction,
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .fillMaxSize(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        // Scope search result focus to this pane, e.g. when shown next to the inbox
        val searchFocusContainer = LocalFocusParent.current?.uuid
        // Searching is all this screen is about, so open the search bar right away,
        // unless the user is busy with a search or command elsewhere
        val keyHandler = LocalKeyboardActionHandler.current
        LaunchedEffect(keyHandler, viewModel) {
            if (keyHandler.mode.value is KeyboardActionMode.Navigation) {
                keyHandler.onSearchType("", viewModel, searchFocusContainer)
            }
        }

        Column(contentModifier.fillMaxSize()) {
            TopNavigation {
                TopNavigationSearchOrTitle(
                    stringResource(Res.string.start_chat),
                    searchFocusContainer = searchFocusContainer,
                )
                TopNavigationCloseOrNavigateToInboxIcon()
            }

            val state = viewModel.state.collectAsState().value
            val searchState = viewModel.searchState.collectAsState().value
            val sessionId = viewModel.sessionId.collectAsState().value
            val sessionIds = viewModel.availableSessionIds.collectAsState(null).value.orEmpty()
            val actionContext = currentActionContext()
            val canStartChat = state is StartChatState.Idle

            LazyColumn(
                state = listState,
                verticalArrangement = Dimens.verticalArrangement,
                contentPadding = WindowInsets.navigationBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            ) {
                // Aux item, so that confirming the search focuses the first user rather than this
                item {
                    CreateRoomDropDownSetting(
                        stringResource(Res.string.hint_start_chat_account),
                        sessionId,
                        sessionIds.map { EditableDropdownEntry(it, it.value.toStringHolder()) },
                        persist = { viewModel.setSessionId(it) },
                        enabled = canStartChat && sessionIds.any { it != sessionId },
                        focusRole = FocusRole.AUX_ITEM,
                    )
                }

                when (searchState) {
                    StartChatSearchState.Idle -> item {
                        StartChatStatusText(stringResource(Res.string.start_chat_hint_idle))
                    }
                    StartChatSearchState.Loading -> item {
                        StartChatStatusText(stringResource(Res.string.empty_screen_placeholder_search_in_progress))
                    }
                    is StartChatSearchState.Error -> item {
                        StartChatStatusText(
                            searchState.message ?: stringResource(Res.string.start_chat_search_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is StartChatSearchState.Results -> {
                        if (searchState.candidates.isEmpty()) {
                            item {
                                StartChatStatusText(
                                    stringResource(
                                        if (searchState.loading) {
                                            Res.string.empty_screen_placeholder_search_in_progress
                                        } else {
                                            Res.string.start_chat_no_results
                                        }
                                    )
                                )
                            }
                        }
                        items(searchState.candidates, key = { it.user.userId.value }) { candidate ->
                            StartChatCandidateRow(
                                candidate = candidate,
                                sessionId = sessionId,
                                enabled = canStartChat,
                                onClick = {
                                    viewModel.startChat(candidate.user.userId, actionContext) {
                                        // Leave the search of this now replaced screen, like other lists do
                                        val mode = keyHandler.mode.value
                                        if (mode is KeyboardActionMode.Search && mode.searchProvider == viewModel) {
                                            keyHandler.clearSearch()
                                        }
                                    }
                                },
                            )
                        }
                        if (searchState.limited) {
                            item {
                                StartChatStatusText(stringResource(Res.string.start_chat_results_limited))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartChatStatusText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
    )
}

@Composable
private fun StartChatCandidateRow(
    candidate: StartChatCandidate,
    sessionId: SessionId?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = candidate.user
    val displayName = user.displayName?.takeIf(String::isNotBlank)
    Row(
        modifier
            .fillMaxWidth()
            .keyFocusable(
                role = FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = if (enabled) {
                        InteractionAction.Invoke {
                            onClick()
                            true
                        }
                    } else {
                        null
                    },
                    copyActions = plainTextCopyActionWithUserId(user.userId) { displayName ?: user.userId.value },
                ),
                enableClicks = enabled,
            )
            .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            source = user.avatarUrl?.let { MediaSource(it) },
            size = Dimens.Inbox.avatar,
            displayName = displayName ?: user.userId.value,
            sessionId = sessionId,
        )
        Column(Modifier.weight(1f)) {
            Text(
                displayName ?: user.userId.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (displayName != null) {
                Text(
                    user.userId.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
