package chat.schildi.revenge.model.devtools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.Destination
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.model.LoadCheckPoint
import chat.schildi.revenge.model.LoadStateHolder
import chat.schildi.revenge.toPrettyJson
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import shire.res.generated.resources.Res
import shire.res.generated.resources.account_dev_tools_title
import shire.res.generated.resources.dual_title_format
import shire.res.generated.resources.hint_account_data

class AccountDevToolsViewModel(
    private val sessionId: SessionId,
) : ViewModel(), TitleProvider, SearchProvider {
    private val log = Logger.withTag("AccountDevTools")

    private val loadStateHolder = LoadStateHolder(
        LoadCheckPoint.Client(sessionId),
        LoadCheckPoint.AccountData,
    )
    val loadState = loadStateHolder.state

    private val searchTerm = MutableStateFlow<String?>(null)
    override fun onSearchType(query: String) {
        searchTerm.value = query
    }
    override fun onSearchEnter(query: String) = onSearchType(query)
    override fun onSearchCleared() {
        searchTerm.value = null
    }

    private val accountDataList = MutableStateFlow<ImmutableList<DevToolsStateLikeEventContent<StateLikeType.AccountData>>?>(null)

    val sectionedList = combine(
        accountDataList,
        searchTerm,
    ) { list, search ->
        list?.let {
            val searchLower = search?.lowercase()
            persistentListOf(
                DevToolsSection.EventList(
                    Res.string.hint_account_data.toStringHolder(),
                    list.filterForSearch(searchLower),
                    searchLower,
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope, loadStateHolder)

    override val windowTitle = flowOf(windowTitle(sessionId))

    init {
        clientFlow.onEach { client ->
            client?.let(::refresh)
        }.launchIn(viewModelScope)
    }

    fun refresh() = clientFlow.value?.let(::refresh)

    private fun refresh(client: MatrixClient) {
        viewModelScope.launch {
            client.getGlobalAccountData().also {
                log.d { "Global account data refresh successful" }
                loadStateHolder.handleResult(LoadCheckPoint.AccountData, it)
            }.onFailure {
                log.e("Failed to fetch account data", it)
            }.getOrNull()?.let { accountDataRawEvents ->
                accountDataList.value = accountDataRawEvents.map { event ->
                    DevToolsStateLikeEventContent(
                        type = StateLikeType.AccountData(event.eventType),
                        content = event.content.toPrettyJson {
                            log.e("Failed to prettify json for event ${event.eventType}", it)
                        },
                    )
                }.sortedBy { it.type.eventType }.toPersistentList()
            }
        }
    }

    suspend fun persist(type: StateLikeType, content: String): Result<Unit> {
        if (type !is StateLikeType.AccountData) {
            return Result.failure(IllegalArgumentException("Tried to edit incompatible type $type"))
        }
        val client = clientFlow.value ?: return Result.failure(IllegalStateException("Client not ready"))
        return client.setAccountData(type.eventType, content).also {
            if (it.isSuccess) {
                // Why do I need that? :(
                delay(500)
                refresh(client)
            } else {
                log.e("Failed to set account data ${type.eventType}", it.exceptionOrNull())
            }
        }
    }

    companion object {
        fun factory(sessionId: SessionId) = viewModelFactory {
            initializer {
                AccountDevToolsViewModel(sessionId)
            }
        }

        fun windowTitle(sessionId: SessionId) = StringResourceHolder(
            Res.string.dual_title_format,
            Res.string.account_dev_tools_title.toStringHolder(),
            sessionId.value.toStringHolder(),
        )
    }

    override fun verifyDestination(destination: Destination) =
        destination is Destination.AccountDevTools && destination.sessionId == sessionId
}
