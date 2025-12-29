package chat.schildi.revenge.store

import chat.schildi.revenge.config.ConfigWatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer

abstract class FileBackedStore<T : Any>(
    private val tag: String,
    private val scope: CoroutineScope,
    fileName: String,
    serializer: KSerializer<T>,
    private val default: T,
) {
    val configWatcher = ConfigWatchers.jsonConfigWatcher(
        tag = "$tag/Watcher",
        fileName = fileName,
        scope = scope,
        serializer = serializer,
        default = default,
        createIfMissing = true,
    )

    private val _config = MutableStateFlow<T?>(null)
    val config = _config.asStateFlow()

    init {
        configWatcher.config.onEach {
            _config.emit(it)
        }.launchIn(scope)
    }

    protected fun update(block: (T) -> T) {
        var updated: T? = null
        _config.update { previous ->
            block(previous ?: default).also {
                updated = it.takeIf { it != previous }
            }
        }
        if (updated != null) {
            scope.launch {
                configWatcher.persist(updated)
            }
        }
    }
}
