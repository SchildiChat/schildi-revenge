package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ListAction
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalUserIdSuggestionsProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.ConversationDetailsTopNavigation
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.UserDetailsViewModel
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.media.MediaSource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_unexpected

@Composable
fun UserDetailsScreen(
    destination: Destination.UserDetails,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: UserDetailsViewModel =
        viewModel(
            key = viewModelKey(destination),
            factory =
                UserDetailsViewModel.factory(
                    destination.sessionId,
                    destination.userId,
                ),
        )

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListAction(listState) }
    FocusContainer(
        LocalUserIdSuggestionsProvider provides viewModel,
        LocalListActionProvider provides listAction,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.fillMaxSize(),
    ) {
        val info = viewModel.info.collectAsState().value
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(info?.displayName ?: viewModel.userId.value)
            if (info == null) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_unexpected.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Person),
                    isSearching = false,
                    isLoading = true,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Dimens.verticalArrangement,
                    ) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().keyFocusable(),
                                contentAlignment = Alignment.Center,
                            ) {
                                AvatarImage(
                                    source = info.avatarUrl?.let { MediaSource(it) },
                                    size = 128.dp,
                                    displayName = info.displayName ?: info.userId.value,
                                )
                            }
                        }
                        if (info.displayName != null) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().keyFocusable(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SelectionContainer {
                                        Text(
                                            info.displayName ?: "",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            SelectionContainer {
                                Box(
                                    Modifier.fillMaxWidth().keyFocusable(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        viewModel.userId.value,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
