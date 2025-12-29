package chat.schildi.revenge.config

import co.touchlab.kermit.Logger
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.time.Instant
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class JsonConfigWatcher<T : Any>(
    tag: String,
    private val scope: CoroutineScope,
    private val file: File,
    private val serializer: KSerializer<T>,
    debounce: Duration = 150.milliseconds,
    private val default: T,
    private val createIfMissing: Boolean = false,
) : ConfigWatcher<T>(
    tag = tag,
    scope = scope,
    file = file,
    debounce = debounce,
) {
    private val log = Logger.withTag(tag)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        launch()
    }

    override fun decodeFromString(text: String): T =
        json.decodeFromString(serializer, text)

    suspend fun persist(value: T) {
        _config.value = value
        withContext(Dispatchers.IO) {
            try {
                // Ensure directory exists
                file.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }

                // Serialize data
                val data = json.encodeToString(serializer, value)

                // Atomic replace
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(data)
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: IOException) {
                log.e("Failed to persist file", e)
            }
        }
    }

    override fun reloadNow(): Boolean {
        val success = super.reloadNow()
        if (!success && createIfMissing) {
            log.d { "Failed to load file, persisting default" }
            scope.launch(Dispatchers.IO) {
                persist(config.value ?: default)
            }
        }
        return success
    }
}

class TomlConfigWatcher<T : Any>(
    tag: String,
    private val scope: CoroutineScope,
    file: File,
    private val readDefaultFallback: (suspend () -> String?)?,
    private val serializer: KSerializer<T>,
    debounce: Duration = 150.milliseconds,
    onReloadSuccess: () -> Unit,
    private val onError: (Throwable?) -> Unit,
) : ConfigWatcher<T>(
    tag = tag,
    scope = scope,
    file = file,
    debounce = debounce,
    onReloadSuccess = onReloadSuccess,
    onError = onError,
) {
    private val log = Logger.withTag(tag)
    private val toml = Toml(
        inputConfig = TomlInputConfig(
            ignoreUnknownNames = true,
            allowEmptyValues = true,
            allowNullValues = true,
            allowEscapedQuotesInLiteralStrings = true,
            allowEmptyToml = true,
        )
    )

    override fun decodeFromString(text: String): T =
        toml.decodeFromString(serializer, text)

    init {
        launch()
    }

    override fun reloadNow(): Boolean {
        val success = super.reloadNow()
        if (!success && readDefaultFallback != null && config.value == null) {
            log.i { "Failed to load file, loading default" }
            scope.launch(Dispatchers.IO) {
                try {
                    val fallback = readDefaultFallback()
                    if (fallback != null) {
                        _config.value = decodeFromString(fallback)
                    }
                } catch (t: Throwable) {
                    log.e("Failed to parse default config", t)
                    onError(t)
                }
            }
        }
        return success
    }
}

abstract class ConfigWatcher<T : Any>(
    tag: String,
    scope: CoroutineScope,
    private val file: File,
    private val debounce: Duration = 150.milliseconds,
    private val onReloadSuccess: () -> Unit = {},
    private val onError: (Throwable?) -> Unit = {},
) {
    private val log = Logger.withTag(tag)

    protected val _config = MutableStateFlow<T?>(null)
    val config = _config.asStateFlow()
    private var isInitialLoad = true

    // Watcher job
    private val watchJob = scope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
        // Initial load if present. This is the open function variant so subclasses can persist the file if missing here.
        reloadNow()

        if (!File(file.parent).exists()) {
            // Parent does not exist; nothing to watch yet. We'll still try to reload on demand.
            return@launch
        }
        val watchService = FileSystems.getDefault().newWatchService()
        try {
            val dir = Paths.get(file.parent)
            dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                //StandardWatchEventKinds.ENTRY_DELETE
            )

            var lastEventAt: Instant? = null
            while (true) {
                val key = watchService.take() // blocks
                var relevant = false
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue
                    @Suppress("UNCHECKED_CAST")
                    val ev = event as WatchEvent<Path>
                    val changed = ev.context()
                    if (changed.name == file.name) {
                        relevant = true
                    }
                }
                val valid = key.reset()
                if (!valid) break

                if (relevant) {
                    // Debounce quick successive writes
                    val now = Instant.now()
                    val shouldReload = lastEventAt?.let { java.time.Duration.between(it, now).toMillis() >= debounce.inWholeMilliseconds } ?: true
                    lastEventAt = now
                    if (shouldReload) {
                        tryReload()
                    }
                }
            }
        } finally {
            try { watchService.close() } catch (_: Throwable) {}
        }
    }

    // Allow launching later so child class initialization can be ensured to be finished
    open fun launch() {
        watchJob.start()
    }

    /** Force a reload from disk. Returns true if a new value was emitted. */
    open fun reloadNow(): Boolean = tryReload()

    fun close() {
        watchJob.cancel()
    }

    abstract fun decodeFromString(text: String): T

    private fun tryReload(): Boolean {
        return try {
            if (file.exists()) {
                val text = Files.readString(Path.of(file.path))
                val parsed = decodeFromString(text)
                log.d("Config loaded from ${file.path}")
                _config.value = parsed
                if (!isInitialLoad) {
                    onReloadSuccess()
                }
                true
            } else {
                log.d("Config file ${file.path} does not exist")
                false
            }
        } catch (t: Throwable) {
            // Keep the last good value
            log.e("Failed to parse config", t)
            onError(t)
            false
        } finally {
            isInitialLoad = false
        }
    }
}
