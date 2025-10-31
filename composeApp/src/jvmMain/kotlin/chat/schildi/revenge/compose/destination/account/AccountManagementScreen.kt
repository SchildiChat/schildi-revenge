package chat.schildi.revenge.compose.destination.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SecureTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.compose.util.rememberInvalidating
import chat.schildi.revenge.config.AccountsConfig
import chat.schildi.revenge.matrix.MatrixAppState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_hide
import shire.composeapp.generated.resources.action_login
import shire.composeapp.generated.resources.action_show
import shire.composeapp.generated.resources.hint_homeserver
import shire.composeapp.generated.resources.hint_password
import shire.composeapp.generated.resources.hint_username
import shire.composeapp.generated.resources.manage_accounts
import shire.composeapp.generated.resources.title_login_account

@Composable
fun AccountManagementScreen() {
    val accounts = MatrixAppState.accountsConfig.collectAsState().value?.accounts!!
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (accounts.isNotEmpty()) {
            item {
                SectionHeader(stringResource(Res.string.manage_accounts))
            }
            items(accounts) { account ->
                ExistingLogin(account)
            }
        }
        item {
            SectionHeader(stringResource(Res.string.title_login_account))
        }
        item {
            NewLogin()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ExistingLogin(account: AccountsConfig.Account) {
    val client = remember(account) {
        MatrixAppState.getClientOrNull(account)
    }
    val userId = rememberInvalidating(2000, client) {
        client?.userId()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (userId != null) {
            Text(
                userId,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                "❌",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                account.username,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                account.homeserver,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NewLogin() {
    val inProgress = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val error = remember { mutableStateOf<String?>(null) }
    val username = rememberTextFieldState()
    val homeserver = rememberTextFieldState()
    val password = rememberTextFieldState()
    Column(
        modifier = Modifier.width(256.dp),
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
            state = username,
            label = { Text(stringResource(Res.string.hint_username)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            state = homeserver,
            label = { Text(stringResource(Res.string.hint_homeserver)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        val passwordVisible = remember { mutableStateOf(false) }
        SecureTextField(
            state = password,
            label = { Text(stringResource(Res.string.hint_password)) },
            modifier = Modifier.fillMaxWidth(),
            textObfuscationMode = if (passwordVisible.value)
                TextObfuscationMode.Visible
            else
                TextObfuscationMode.Hidden,
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
        Button(
            enabled = !inProgress.value &&
                    homeserver.text.isNotBlank() && username.text.isNotBlank() && password.text.isNotBlank(),
            onClick = {
                scope.launch {
                    inProgress.value = true
                    try {
                        val server = homeserver.text.let {
                            if (it.contains("://")) {
                                it.toString()
                            } else {
                                "https://$it"
                            }
                        }
                        val client = MatrixAppState.getOrCreateClient(
                            username = username.text.toString(),
                            homeserver = server,
                            persist = false
                        )
                        val result = client.login(password.text.toString())
                        if (result.isSuccess) {
                            password.setTextAndPlaceCursorAtEnd("")
                            username.setTextAndPlaceCursorAtEnd("")
                            homeserver.setTextAndPlaceCursorAtEnd("")
                            MatrixAppState.persistClientInAccountConfig(client)
                        } else {
                            error.value = result.exceptionOrNull()?.message ?: "Login failed without exception"
                            MatrixAppState.cleanUpClients()
                        }
                    } finally {
                        inProgress.value = false
                    }
                }
            },
        ) {
            Text(stringResource(Res.string.action_login))
        }
    }
}
