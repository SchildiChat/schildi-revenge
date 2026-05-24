package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationSearchOrTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.search.LocalSearchProvider
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.SessionSelectorAccount
import chat.schildi.revenge.model.SessionSelectorViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.media.MediaSource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.empty_screen_placeholder_unexpected
import shire.composeapp.generated.resources.select_account

@Composable
fun SessionSelectorScreen(
    destination: Destination.SessionSelector,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: SessionSelectorViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { SessionSelectorViewModel() } },
    )
    publishTitle(viewModel)

    val destinationState = LocalDestinationState.current
    val accounts = viewModel.accounts.collectAsState().value
    val filteredAccounts = viewModel.filteredAccounts.collectAsState().value
    LaunchedEffect(accounts, destination, destinationState) {
        val loadedAccounts = accounts ?: return@LaunchedEffect
        when (loadedAccounts.size) {
            0 -> destinationState?.navigate(Destination.AccountManagement, NavigationPreference.REPLACE)
            1 -> destinationState?.navigate(destination.destinationBuilder(loadedAccounts.single().sessionId), NavigationPreference.REPLACE)
        }
    }

    val listState = rememberLazyListState()
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides remember(listState) { ListActions(listState) },
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(stringResource(Res.string.select_account))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val renderedAccounts = filteredAccounts?.accounts
                if (renderedAccounts.isNullOrEmpty()) {
                    EmptyListScreen(
                        title = Res.string.empty_screen_placeholder_unexpected.toStringHolder(),
                        icon = rememberVectorPainter(Icons.Default.AccountCircle),
                        renderedSearchTerm = filteredAccounts?.searchTerm,
                        isLoading = accounts == null || filteredAccounts == null,
                        modifier = contentModifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = contentModifier,
                        state = listState,
                    ) {
                        items(renderedAccounts, key = { it.sessionId }) { account ->
                            SessionSelectorRow(
                                account = account,
                                onClick = {
                                    destinationState?.navigate(
                                        destination.destinationBuilder(account.sessionId),
                                        NavigationPreference.REPLACE,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSelectorRow(
    account: SessionSelectorAccount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = account.user.displayName ?: account.sessionId.value
    Row(
        modifier
            .fillMaxWidth()
            .keyFocusable(
                role = FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Invoke {
                        onClick()
                        true
                    },
                    copyActions = plainTextCopyAction { account.sessionId.value },
                ),
            )
            .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPadding),
        horizontalArrangement = Dimens.horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            source = account.user.avatarUrl?.let { MediaSource(it) },
            size = Dimens.Conversation.avatar,
            sessionId = account.sessionId,
            shape = Dimens.ownAccountAvatarShape,
            displayName = displayName,
            contentDescription = account.sessionId.value,
        )
        SelectionContainer(Modifier.weight(1f)) {
            Column {
                Text(
                    displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    account.sessionId.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
