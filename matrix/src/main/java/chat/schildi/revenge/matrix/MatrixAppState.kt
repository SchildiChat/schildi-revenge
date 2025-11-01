package chat.schildi.revenge.matrix

import chat.schildi.revenge.config.AccountsConfig
import chat.schildi.revenge.util.ScAppDirs
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import java.io.File

data object MatrixAppState {
    private val log = Logger.withTag("MatrixAppState")
    private val configLock = Mutex()
    private val clientLock = Mutex()
    private val _accountsConfig = MutableStateFlow<AccountsConfig?>(null)
    private val clients = mutableListOf<MatrixClient>()

    private val configDir = File(ScAppDirs.getUserConfigDir()).also { it.mkdirs() }
    private val accountsConfigFile = File(ScAppDirs.getUserConfigDir() + File.separator + "accounts.yaml")

    val accountsConfig = _accountsConfig.asStateFlow()

    private val _activeClients = MutableStateFlow<List<MatrixClient>>(emptyList())
    val activeClients = _activeClients.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        log.d { "Using accounts config at ${accountsConfigFile.path}" }
        val config = configLock.withLock {
            val config = _accountsConfig.value
            if (config == null) {
                val newConfig = if (accountsConfigFile.exists()) {
                    accountsConfigFile.inputStream().use {
                        Yaml.decodeFromString<AccountsConfig>(it.readAllBytes().decodeToString())
                    }
                } else {
                    log.d { "No accounts config found, starting empty" }
                    AccountsConfig()
                }
                _accountsConfig.emit(newConfig)
                newConfig
            } else {
                config
            }
        }
        log.d { "Loaded config with ${config.accounts.size} accounts" }
        clientLock.withLock {
            config.accounts.forEach { account ->
                clients.forAccount(account)
                    ?: MatrixClient(account.homeserver, account.username).also {
                        it.restoreSession()
                        clients.add(it)
                    }
            }
        }
        log.d { "Initialized ${clients.size} clients" }
    }

    fun getClientOrNull(account: AccountsConfig.Account) = clients.forAccount(account)

    suspend fun getOrCreateClient(username: String, homeserver: String, persist: Boolean): MatrixClient {
        val account = AccountsConfig.Account(username, homeserver)
        clients.forAccount(account)?.let {
            return it
        }
        if (persist) {
            persistAccountInConfig(account)
        }
        return clientLock.withLock {
            clients.forAccount(account)
                ?: MatrixClient(account.homeserver, account.username).also {
                    clients.add(it)
                }
        }
    }

    suspend fun persistClientInAccountConfig(client: MatrixClient) =
        persistAccountInConfig(client.toAccount())

    suspend fun persistAccountInConfig(account: AccountsConfig.Account) {
        configLock.withLock {
            var updated = false
            _accountsConfig.update { config ->
                config!!
                if (!config.accounts.contains(account)) {
                    updated = true
                    config.copy(
                        accounts = config.accounts + account,
                    )
                } else {
                    updated = false
                    config
                }
            }
            if (updated) {
                persistConfig()
            }
        }
    }

    /**
     * Drop temporary Matrix clients that have not been persisted to accounts config.
     */
    suspend fun cleanUpClients() {
        clientLock.withLock {
            val config = _accountsConfig.value!!
            val toRemove = mutableListOf<MatrixClient>()
            clients.forEach { client ->
                if (!config.accounts.contains(client.toAccount())) {
                    log.d { "Dropping client for ${client.username} on ${client.homeserver}" }
                    client.destroyClient()
                    toRemove.add(client)
                }
            }
            clients.removeAll(toRemove)
        }
    }

    suspend fun logoutClient(client: MatrixClient) {
        val clientAccount = client.toAccount()
        log.d { "Removing client for $clientAccount" }
        _accountsConfig.update {
            it?.copy(
                accounts = it.accounts.filter {
                    it != clientAccount
                }
            )
        }
        persistConfig()
        log.d { "Logging out client for $clientAccount" }
        client.logout()
        clientLock.withLock {
            clients.remove(client)
        }
        log.d { "Finished cleaning up client for $clientAccount" }
    }

    internal fun onClientActiveChanged(client: MatrixClient, isActive: Boolean) {
        _activeClients.update {
            if (isActive) {
                if (client in it) {
                    it
                } else {
                    it + client
                }
            } else {
                it.filter { it != client }
            }
        }
    }

    suspend fun persistConfig() = withContext(Dispatchers.IO) {
        val encodedConfig = Yaml.encodeToString(accountsConfig.value!!)
            .toByteArray(Charsets.UTF_8)
        accountsConfigFile.outputStream().use {
            it.write(encodedConfig)
        }
    }
}

private fun Collection<MatrixClient>.forAccount(account: AccountsConfig.Account) =
    find { it.homeserver == account.homeserver && it.username == account.username }
private fun MatrixClient.toAccount() = AccountsConfig.Account(
    homeserver = homeserver,
    username = username
)
