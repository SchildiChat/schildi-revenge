package chat.schildi.revenge.compose.destination

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.model.account.AccountManagementData
import chat.schildi.revenge.model.account.AccountManagementViewModel
import chat.schildi.revenge.model.account.LoginVariant
import chat.schildi.revenge.model.account.OAuthLoginState
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.viewModelKey
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.auth.MatrixHomeServerDetails
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.account_status_invalid_token
import shire.res.generated.resources.action_change
import shire.res.generated.resources.action_check
import shire.res.generated.resources.action_hide
import shire.res.generated.resources.action_login
import shire.res.generated.resources.action_login_with_browser
import shire.res.generated.resources.action_login_with_password
import shire.res.generated.resources.action_logout
import shire.res.generated.resources.action_retry
import shire.res.generated.resources.action_show
import shire.res.generated.resources.action_verify
import shire.res.generated.resources.action_verify_with_another_device
import shire.res.generated.resources.hint_homeserver
import shire.res.generated.resources.hint_password
import shire.res.generated.resources.hint_recovery_key
import shire.res.generated.resources.hint_username
import shire.res.generated.resources.login_continue_in_browser_message
import shire.res.generated.resources.login_not_supported
import shire.res.generated.resources.manage_accounts
import shire.res.generated.resources.title_login_account
import shire.res.generated.resources.verification_cancelled
import shire.res.generated.resources.verification_status_not_verified
import shire.res.generated.resources.verification_status_verified
import shire.res.generated.resources.verified_off_24px
import kotlin.uuid.Uuid

// TODO redo me with more view model responsibilities, only one account login active at a time etc.
//  when I have a better idea for how the UI should look
@Composable
fun AccountManagementScreen(
    destination: Destination.AccountManagement,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: AccountManagementViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { AccountManagementViewModel() } }
    )
    val accounts = viewModel.data.collectAsState().value
    FocusContainer(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ),
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
                    contentModifier.padding(vertical = Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangement,
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
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
                        SectionHeader(
                            stringResource(Res.string.title_login_account),
                            modifier = Modifier.padding(top = Dimens.horizontalItemPadding),
                        )
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
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(horizontal = Dimens.windowPadding),
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
                    AccountStatusRow(account)
                    Text(
                        account.session.userId,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                if (account.sessionVerifiedStatus?.isVerified() == false) {
                    val destinationStateHolder = LocalDestinationState.current
                    AccountManagementButton(
                        stringResource(Res.string.action_verify_with_another_device),
                        role = FocusRole.NESTED_AUX_ITEM,
                        onClick = {
                            viewModel.launchDeviceVerification(account.sessionId, destinationStateHolder!!).isSuccess
                        },
                    )
                }
                val keyboardActionHandler = LocalKeyboardActionHandler.current
                val logoutFocusId = rememberFocusId()
                AccountManagementIconButton(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Default.Logout),
                    contentDescription = stringResource(Res.string.action_logout),
                    tint = MaterialTheme.colorScheme.error,
                    focusId = logoutFocusId,
                ) {
                    keyboardActionHandler.handleAction(
                        focusItem = logoutFocusId,
                        action = if (account.session.isTokenValid)
                            Action.Global.Logout
                        else
                            Action.Global.LogoutOrDelete,
                        args = listOf(account.session.userId),
                    ) is ActionResult.Actioned
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
                        singleLine = true,
                        modifier = Modifier.weight(1f).keyFocusable(FocusRole.TEXT_FIELD_SINGLE_LINE),
                    )
                    // TODO move to VM
                    fun verify(): Boolean {
                        if (isVerifying) return false
                        scope.launch {
                            isVerifying = true
                            viewModel.verify(account.session, recoveryKey.text)
                            isVerifying = false
                        }
                        return true
                    }
                    AccountManagementButton(
                        text = stringResource(Res.string.action_verify),
                        role = FocusRole.NESTED_AUX_ITEM,
                        enabled = !isVerifying && recoveryKey.text.isNotBlank(),
                        onClick = ::verify,
                    )
                }
            }
            if (ScPrefs.SHOW_DEV_INFOS.value()) {
                Text("Verification state: ${account.sessionVerifiedStatus}")
                Text("Backup state: ${account.backupState}")
                Text("Recovery state: ${account.recoveryState}")
            }
        }
    }
}

@Composable
private fun AccountStatusRow(
    account: AccountManagementData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Dimens.horizontalArrangementSmall,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!account.session.isTokenValid) {
            AccountStatusIcon(
                rememberVectorPainter(Icons.Default.Warning),
                stringResource(Res.string.account_status_invalid_token),
                critical = true,
            )
            return
        }
        when (account.sessionVerifiedStatus) {
            SessionVerifiedStatus.NotVerified -> AccountStatusIcon(
                painterResource(Res.drawable.verified_off_24px),
                stringResource(Res.string.verification_status_not_verified),
                critical = true,
            )
            SessionVerifiedStatus.Verified -> AccountStatusIcon(
                rememberVectorPainter(Icons.Default.Verified),
                stringResource(Res.string.verification_status_verified),
            )
            SessionVerifiedStatus.Unknown,
            null -> {}
        }
        /*
        when (account.recoveryState) {
            RecoveryState.WAITING_FOR_SYNC -> TODO()
            RecoveryState.UNKNOWN -> TODO()
            RecoveryState.ENABLED -> TODO()
            RecoveryState.DISABLED -> TODO()
            RecoveryState.INCOMPLETE -> TODO()
            null -> TODO()
        }
        when (account.backupState) {
            BackupState.WAITING_FOR_SYNC -> TODO()
            BackupState.UNKNOWN -> TODO()
            BackupState.CREATING -> TODO()
            BackupState.ENABLING -> TODO()
            BackupState.RESUMING -> TODO()
            BackupState.ENABLED -> TODO()
            BackupState.DOWNLOADING -> TODO()
            BackupState.DISABLING -> TODO()
            null -> TODO()
        }
         */
    }
}

@Composable
private fun AccountStatusIcon(
    icon: Painter,
    hint: String,
    modifier: Modifier = Modifier,
    critical: Boolean = false,
) {
    WithTooltip(hint, modifier) {
        Icon(
            icon,
            hint,
            Modifier.size(16.dp),
            tint = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewLogin(viewModel: AccountManagementViewModel) {
    val setHomeserverInProgress = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val actionContext = currentActionContext()
    var homeserver by remember { mutableStateOf(TextFieldValue()) }
    var hsDetails by remember(homeserver.text) { mutableStateOf<Result<MatrixHomeServerDetails>?>(null) }
    var loginVariant by remember(homeserver.text) { mutableStateOf<LoginVariant?>(null) }
    val loginError = remember(homeserver.text) { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.oauthState.collect {
            when (it) {
                is OAuthLoginState.AuthenticationResult -> {
                    if (it.result.isSuccess) {
                        homeserver = TextFieldValue()
                    } else {
                        loginError.value = it.result.exceptionOrNull()?.let {
                            it.message ?: it.toString()
                        } ?: "Unexpected oauth login error"
                    }
                }
                OAuthLoginState.Cancelled -> {
                    loginError.value = getString(Res.string.verification_cancelled)
                }
                OAuthLoginState.Idle,
                is OAuthLoginState.Processing,
                is OAuthLoginState.Waiting -> {}
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.windowPadding)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val error = loginError.value ?: hsDetails?.exceptionOrNull()?.let {
            it.message ?: it.toString()
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 10,
            )
        }
        Row(
            horizontalArrangement = Dimens.horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = homeserver,
                onValueChange = { homeserver = it },
                label = { Text(stringResource(Res.string.hint_homeserver)) },
                singleLine = true,
                modifier = Modifier.weight(1f).keyFocusable(FocusRole.TEXT_FIELD_SINGLE_LINE),
                enabled = loginVariant == null && !setHomeserverInProgress.value,
            )
            AnimatedContent(Pair(loginVariant, hsDetails?.getOrNull())) { (selectedLogin, currentHsDetails) ->
                when {
                    selectedLogin != null -> {
                        AccountManagementButton(
                            stringResource(Res.string.action_change),
                        ) {
                            loginVariant = null
                            hsDetails = null
                            true
                        }
                    }
                    currentHsDetails == null -> {
                        AccountManagementButton(
                            stringResource(Res.string.action_check),
                            enabled = !setHomeserverInProgress.value && homeserver.text.isNotBlank(),
                        ) {
                            if (setHomeserverInProgress.value) {
                                return@AccountManagementButton false
                            }
                            setHomeserverInProgress.value = true
                            scope.launch {
                                try {
                                    viewModel.setHomeserver(homeserver.text)
                                        .also {
                                            hsDetails = it
                                        }
                                        .onFailure {
                                            loginError.value = it.message ?: it.toString()
                                        }
                                        .onSuccess {
                                            when {
                                                it.supportsOAuthLogin && it.supportsPasswordLogin -> {
                                                    loginVariant = null
                                                }

                                                it.supportsOAuthLogin -> {
                                                    loginVariant = LoginVariant.OAUTH
                                                    viewModel.loginWithBrowser(actionContext)
                                                }

                                                it.supportsPasswordLogin -> {
                                                    loginVariant = LoginVariant.PASSWORD
                                                }

                                                else -> {
                                                    loginError.value = getString(Res.string.login_not_supported)
                                                }
                                            }
                                        }
                                } finally {
                                    setHomeserverInProgress.value = false
                                }
                            }
                            true
                        }
                    }
                }
            }
        }
        when (loginVariant) {
            null -> {
                hsDetails?.getOrNull()?.let { currentHsDetails ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Dimens.horizontalArrangement,
                    ) {
                        if (currentHsDetails.supportsOAuthLogin) {
                            AccountManagementButton(
                                stringResource(Res.string.action_login_with_browser)
                            ) {
                                loginVariant = LoginVariant.OAUTH
                                scope.launch {
                                    viewModel.loginWithBrowser(actionContext)
                                }
                                true
                            }
                        }
                        if (currentHsDetails.supportsPasswordLogin) {
                            AccountManagementButton(
                                stringResource(Res.string.action_login_with_password)
                            ) {
                                loginVariant = LoginVariant.PASSWORD
                                true
                            }
                        }
                    }
                }
            }

            LoginVariant.PASSWORD -> {
                PasswordLogin(viewModel, loginError) {
                    homeserver = TextFieldValue()
                }
            }

            LoginVariant.OAUTH -> {
                Text(stringResource(Res.string.login_continue_in_browser_message))
                AccountManagementButton(
                    stringResource(Res.string.action_retry)
                ) {
                    scope.launch {
                        viewModel.loginWithBrowser(actionContext)
                    }
                    true
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PasswordLogin(
    viewModel: AccountManagementViewModel,
    loginError: MutableState<String?>,
    onSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    val loginInProgress = remember { mutableStateOf(false) }
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text(stringResource(Res.string.hint_username)) },
        singleLine = true,
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
        singleLine = true,
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
    // TODO move state to VM
    fun loginWithPassword(): Boolean {
        val log = Logger.withTag("AccountLogin")
        if (loginInProgress.value) {
            log.w("Ignoring login request, already logging in")
            return false
        }
        scope.launch {
            loginInProgress.value = true
            loginError.value = null
            try {
                val result = viewModel.loginWithPassword(username.text, password.text)
                if (result.isSuccess) {
                    password = TextFieldValue()
                    username = TextFieldValue()
                    onSuccess()
                } else {
                    loginError.value = result.exceptionOrNull()?.let { it.message ?: it.toString() }
                        ?: "Login failed without exception"
                }
            } finally {
                loginInProgress.value = false
            }
        }
        return true
    }
    AccountManagementButton(
        stringResource(Res.string.action_login),
        enabled = !loginInProgress.value && username.text.isNotBlank() && password.text.isNotBlank(),
        onClick = ::loginWithPassword,
        modifier = Modifier.align(Alignment.End)
    )
}

@Composable
private fun AccountManagementButton(
    text: String,
    modifier: Modifier = Modifier,
    role: FocusRole = FocusRole.NESTED_AUX_ITEM,
    enabled: Boolean = true,
    onClick: () -> Boolean,
) {
    Button(
        enabled = enabled,
        onClick = { onClick() },
        modifier = modifier
            .keyFocusable(
                role = role,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Invoke(onClick),
                ),
                addClickListener = false,
            ),
    ) {
        Text(text)
    }
}

@Composable
private fun AccountManagementIconButton(
    icon: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    role: FocusRole = FocusRole.NESTED_AUX_ITEM,
    enabled: Boolean = true,
    focusId: Uuid = rememberFocusId(),
    onClick: () -> Boolean,
) {
    IconButton(
        modifier = modifier
            .keyFocusable(
                role = role,
                id = focusId,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Invoke(onClick),
                ),
                addClickListener = false,
                enableClicks = enabled,
            ),
        onClick = { onClick() },
        enabled = enabled,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
