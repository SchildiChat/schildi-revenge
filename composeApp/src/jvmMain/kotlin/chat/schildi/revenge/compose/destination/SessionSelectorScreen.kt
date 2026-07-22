package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
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
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.SessionSelectorAccount
import chat.schildi.revenge.model.SessionSelectorViewModel
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_processing
import shire.res.generated.resources.failed_to_resolve_room
import shire.res.generated.resources.select_account

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

    val didNavigate = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    suspend fun navigateToSessionDestination(sessionId: SessionId) {
        didNavigate.value = true
        val result = destination.destinationBuilder(sessionId)
        val destination = result.getOrNull()
        if (result.isFailure || destination == null) {
            error.value = result.exceptionOrNull()?.message ?: getString(Res.string.failed_to_resolve_room)
        } else {
            destinationState?.navigate(destination, NavigationPreference.REPLACE)
        }
    }

    LaunchedEffect(accounts, destination, destinationState) {
        val loadedAccounts = accounts ?: return@LaunchedEffect
        when (loadedAccounts.size) {
            0 -> destinationState?.navigate(
                Destination.AccountManagement(isInitialSetup = true),
                NavigationPreference.REPLACE,
            )
            1 -> navigateToSessionDestination(loadedAccounts.single().sessionId)
        }
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    FocusContainer(
        LocalSearchProvider provides viewModel,
        LocalListActionProvider provides remember(listState) { ListActions(listState) },
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationSearchOrTitle(stringResource(Res.string.select_account))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            destination.description?.let {
                Text(
                    it.render(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(Dimens.windowPadding),
                    textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val renderedAccounts = filteredAccounts?.accounts
                if (renderedAccounts.isNullOrEmpty()) {
                    EmptyListScreen(
                        title = Res.string.action_processing.toStringHolder(),
                        icon = rememberVectorPainter(Icons.Default.AccountCircle),
                        renderedSearchTerm = filteredAccounts?.searchTerm,
                        isLoading = accounts == null || filteredAccounts == null || didNavigate.value,
                        modifier = contentModifier.fillMaxSize(),
                    )
                } else if (error.value != null) {
                    Text(
                        error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        modifier = contentModifier,
                        state = listState,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        items(renderedAccounts, key = { it.sessionId }) { account ->
                            SessionSelectorRow(
                                account = account,
                                onClick = {
                                    scope.launch {
                                        navigateToSessionDestination(account.sessionId)
                                    }
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
