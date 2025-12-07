package chat.schildi.revenge.config

import co.touchlab.kermit.Logger
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.time.Instant
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ConfigWatcher<T : Any>(
    tag: String,
    scope: CoroutineScope,
    private val file: File,
    private val serializer: KSerializer<T>,
    private val debounce: Duration = 150.milliseconds,
    default: T,
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

    private val _config = MutableStateFlow(default)
    val config = _config.asStateFlow()

    // Watcher job
    private val watchJob = scope.launch(Dispatchers.IO) {
        // Initial load if present
        tryReload()

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

    /** Force a reload from disk. Returns true if a new value was emitted. */
    fun reloadNow(): Boolean = tryReload()

    fun close() {
        watchJob.cancel()
    }

    private fun tryReload(): Boolean {
        return try {
            if (file.exists()) {
                val text = Files.readString(Path.of(file.path))
                val parsed = toml.decodeFromString(serializer, text)
                log.d("Config loaded from ${file.path}")
                _config.value = parsed
                true
            } else {
                log.d("Config file ${file.path} does not exist")
                false
            }
        } catch (t: Throwable) {
            // TODO surface errors to UI somehow, needs error rendering framework
            // Keep the last good value
            log.e("Failed to parse config", t)
            false
        }
    }
}
