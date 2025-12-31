package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.components.IconButtonWithConfirmation
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.AccountManagementData
import chat.schildi.revenge.model.AccountManagementViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_hide
import shire.composeapp.generated.resources.action_login
import shire.composeapp.generated.resources.action_logout
import shire.composeapp.generated.resources.action_show
import shire.composeapp.generated.resources.action_verify
import shire.composeapp.generated.resources.hint_homeserver
import shire.composeapp.generated.resources.hint_password
import shire.composeapp.generated.resources.hint_recovery_key
import shire.composeapp.generated.resources.hint_username
import shire.composeapp.generated.resources.manage_accounts
import shire.composeapp.generated.resources.title_login_account

@Composable
fun AccountManagementScreen(modifier: Modifier = Modifier) {
    val viewModel: AccountManagementViewModel = viewModel()
    val accounts = viewModel.data.collectAsState().value
    FocusContainer(
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            val destinationState = LocalDestinationState.current
            if (destinationState != null) {
                TopNavigation {
                    TopNavigationTitle(stringResource(Res.string.manage_accounts))
                    TopNavigationCloseOrNavigateToInboxIcon()
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyColumn(
                    Modifier.widthIn(max = ScPrefs.MAX_WIDTH_SETTINGS.value().dp)
                        .padding(vertical = Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangement,
                ) {
                    if (accounts.isNotEmpty()) {
                        item(key = "manage") {
                            SectionHeader(stringResource(Res.string.manage_accounts))
                        }
                        items(accounts, key = { it.session.userId }) { account ->
                            ExistingLogin(account, viewModel)
                        }
                    }
                    item(key = "new_header") {
                        SectionHeader(stringResource(Res.string.title_login_account))
                    }
                    item(key = "new") {
                        NewLogin(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = Dimens.windowPadding),
    )
}

@Composable
private fun ExistingLogin(account: AccountManagementData, viewModel: AccountManagementViewModel) {
    val scope = rememberCoroutineScope()
    FocusContainer(role = FocusRole.CONTAINER_ITEM) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.windowPadding)
        ) {
            Row(
                horizontalArrangement = Dimens.horizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Dimens.horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!account.session.isTokenValid) {
                        Text(
                            "❌",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text(
                        account.session.userId,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                IconButtonWithConfirmation(
                    icon = Icons.AutoMirrored.Default.Logout,
                    confirmText = stringResource(Res.string.action_logout),
                ) {
                    // TODO view progress, and move all the scope.launch into the viewModel with state tracked in there
                    scope.launch {
                        viewModel.logout(account.session)
                    }
                }
            }
            if (account.needsVerification) {
                var recoveryKey by remember { mutableStateOf(TextFieldValue()) }
                var isVerifying by remember(account) { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Dimens.horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = recoveryKey,
                        onValueChange = { recoveryKey = it },
                        label = { Text(stringResource(Res.string.hint_recovery_key)) },
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    // TODO move to VM
                    fun verify() {
                        if (isVerifying) return
                        scope.launch {
                            isVerifying = true
                            viewModel.verify(account.session, recoveryKey.text)
                            isVerifying = false
                        }
                    }
                    Button(
                        enabled = !isVerifying && recoveryKey.text.isNotBlank(),
                        onClick = ::verify,
                        modifier = Modifier
                            .keyFocusable(
                                role = FocusRole.NESTED_AUX_ITEM,
                                actionProvider = defaultActionProvider(
                                    primaryAction = InteractionAction.Invoke {
                                        verify()
                                        true
                                    },
                                ),
                                addClickListener = false,
                            ),
                    ) {
                        Text(stringResource(Res.string.action_verify))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewLogin(viewModel: AccountManagementViewModel) {
    val inProgress = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val error = remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf(TextFieldValue()) }
    var homeserver by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.windowPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        error.value?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            maxLines = 10,
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(Res.string.hint_username)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().keyFocusable(FocusRole.TEXT_FIELD_SINGLE_LINE),
        )
        OutlinedTextField(
            value = homeserver,
            onValueChange = { homeserver = it },
            label = { Text(stringResource(Res.string.hint_homeserver)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().keyFocusable(FocusRole.TEXT_FIELD_SINGLE_LINE),
        )
        val passwordVisible = remember { mutableStateOf(false) }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.hint_password)) },
            modifier = Modifier.fillMaxWidth().keyFocusable(FocusRole.TEXT_FIELD_SINGLE_LINE),
            visualTransformation = if (passwordVisible.value) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            maxLines = 1,
            trailingIcon = {
                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(
                        imageVector = if (passwordVisible.value)
                            Icons.Default.VisibilityOff
                        else
                            Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible.value)
                                Res.string.action_hide
                            else
                                Res.string.action_show
                        )
                    )
                }
            }
        )
        // TODO move to VM
        fun login() {
            if (inProgress.value) {
                return
            }
            scope.launch {
                inProgress.value = true
                try {
                    val server = homeserver.text.let {
                        if (it.contains("://")) {
                            it
                        } else {
                            "https://$it"
                        }
                    }
                    val serverResult = viewModel.setHomeserver(server)
                    if (serverResult.isFailure) {
                        error.value = serverResult.exceptionOrNull()?.message ?: "Setting server failed without exception"
                        return@launch
                    }
                    val result = viewModel.login(username.text, password.text)
                    if (result.isSuccess) {
                        password = TextFieldValue()
                        username = TextFieldValue()
                        homeserver = TextFieldValue()
                    } else {
                        error.value = result.exceptionOrNull()?.message ?: "Login failed without exception"
                    }
                } finally {
                    inProgress.value = false
                }
            }
        }
        Button(
            enabled = !inProgress.value &&
                    homeserver.text.isNotBlank() && username.text.isNotBlank() && password.text.isNotBlank(),
            onClick = ::login,
            modifier = Modifier
                .keyFocusable(
                    actionProvider = defaultActionProvider(
                        primaryAction = InteractionAction.Invoke {
                            login()
                            true
                        },
                    ),
                    addClickListener = false,
                ),
        ) {
            Text(stringResource(Res.string.action_login))
        }
    }
}
