package chat.schildi.revenge.config

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.createSingleProcessCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardCopyOption
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.atomic.AtomicInteger

/**
 * An InterProcessCoordinator that emits update notifications whenever the backing file
 * changes on disk. It uses a WatchService on the parent directory and reacts to
 * create/modify/delete events for the specific target filename.
 */
internal class FileWatchingCoordinator(private val target: Path) : InterProcessCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _updates = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val version = AtomicInteger(0)
    private val mutex = Mutex()

    override val updateNotifications: Flow<Unit> = _updates

    init {
        scope.launch {
            val dir = target.parent
            val ws = FileSystems.getDefault().newWatchService()
            try {
                // Register interest in basic events; ATOMIC_MOVE will surface as CREATE/DELETE pair
                dir.register(ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            } catch (_: Throwable) {
                // If registration fails, bail out silently
                return@launch
            }

            fun emitUpdate() {
                version.incrementAndGet()
                _updates.tryEmit(Unit)
            }

            while (true) {
                val key: WatchKey = try {
                    ws.take() // blocking wait
                } catch (_: InterruptedException) {
                    break
                }
                var relevant = false
                for (event in key.pollEvents()) {
                    val kind: WatchEvent.Kind<*> = event.kind()
                    // Overflow means events may have been lost; treat as relevant
                    if (kind === java.nio.file.StandardWatchEventKinds.OVERFLOW) {
                        relevant = true
                        continue
                    }
                    val changed = event.context() as? Path
                    if (changed != null && changed.fileName.toString() == target.fileName.toString()) {
                        relevant = true
                    }
                }
                // Reset key; if invalid, watcher is closed
                val valid = key.reset()
                if (relevant) emitUpdate()
                if (!valid) break
            }
            try {
                ws.close()
            } catch (_: Throwable) {}
        }
    }

    // InterProcessCoordinator API
    override suspend fun getVersion(): Int = version.get()

    override suspend fun incrementAndGetVersion(): Int {
        val v = version.incrementAndGet()
        // Signal listeners that version changed
        _updates.emit(Unit)
        return v
    }

    override suspend fun <T> lock(block: suspend () -> T): T = mutex.withLock { block() }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        val acquired = mutex.tryLock()
        return if (acquired) {
            try {
                block(true)
            } finally {
                mutex.unlock()
            }
        } else {
            block(false)
        }
    }

    fun shutdown() { scope.cancel() }
}

class RevengeDatastoreStorage(private val filePath: String) : Storage<Preferences> {
    override fun createConnection(): StorageConnection<Preferences> {
        return RevengeStorageConnection(filePath)
    }
}

internal class RevengeStorageConnection(private val path: String) : StorageConnection<Preferences> {
    // Replace single-process coordinator with file watching coordinator to observe external changes
    private val watcher: FileWatchingCoordinator? = try {
        FileWatchingCoordinator(File(path).toPath())
    } catch (_: Throwable) {
        null
    }
    override val coordinator: InterProcessCoordinator = watcher ?: createSingleProcessCoordinator(path)

    override suspend fun <R> readScope(block: suspend ReadScope<Preferences>.(locked: Boolean) -> R): R {
        val file = File(path)
        val data = readFromToml(file)
        val scope = object : ReadScope<Preferences> {
            override suspend fun readData(): Preferences = data
            override fun close() { /* no-op */ }
        }
        return block(scope, /* locked = */ false)
    }

    override suspend fun writeScope(block: suspend WriteScope<Preferences>.() -> Unit) {
        val file = File(path)
        val holder = object {
            var current: Preferences = readFromToml(file)
            fun set(newData: Preferences) { current = newData }
        }
        val scope = object : WriteScope<Preferences> {
            override suspend fun writeData(value: Preferences) {
                holder.set(value)
            }
            override suspend fun readData(): Preferences = holder.current
            override fun close() { /* no-op */ }
        }
        block(scope)
        writeToToml(file, holder.current)
    }

    override fun close() {
        // Close the watcher to release resources
        try { watcher?.shutdown() } catch (_: Throwable) {}
    }

    private fun readFromToml(file: File): Preferences {
        if (!file.exists()) return emptyPreferences()
        return try {
            val lines = file.readLines()
            val prefs: MutablePreferences = mutablePreferencesOf()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val idx = trimmed.indexOf('=')
                if (idx <= 0) continue
                val rawKey = trimmed.take(idx).trim()
                val rawValue = trimmed.substring(idx + 1).trim()
                // Detect value type: boolean, int, or string (quoted)
                val value = when {
                    rawValue.equals("true", ignoreCase = true) -> true
                    rawValue.equals("false", ignoreCase = true) -> false
                    rawValue.startsWith("\"") && rawValue.endsWith("\"") -> rawValue.substring(1, rawValue.length - 1)
                    rawValue.startsWith("'''") && rawValue.endsWith("'''") -> rawValue.substring(3, rawValue.length - 3)
                    rawValue.toIntOrNull() != null -> rawValue.toInt()
                    else -> rawValue.trim('"') // fallback to string
                }
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(rawKey)] = value
                    is Int -> prefs[intPreferencesKey(rawKey)] = value
                    is String -> prefs[stringPreferencesKey(rawKey)] = value
                    else -> {}
                }
            }
            prefs
        } catch (_: Throwable) {
            emptyPreferences()
        }
    }

    private fun writeToToml(file: File, data: Preferences) {
        // Ensure directory exists
        file.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }

        val sb = StringBuilder()
        //sb.appendLine("# Generated Schildi-Revenge preferences")
        // TODO categorize
        data.asMap().entries.sortedBy { it.key.name }.forEach { (k, v) ->
            val key = k.name
            val valueStr = when (v) {
                is Boolean -> if (v) "true" else "false"
                is Int -> v.toString()
                is String -> formatTomlString(v)
                is Long -> v.toString() // not expected but harmless
                is Float -> v.toString()
                is Double -> v.toString()
                is Set<*> -> formatTomlString(v.joinToString(","))
                else -> formatTomlString(v.toString())
            }
            sb.append(key).append(" = ").append(valueStr).append('\n')
        }

        // Atomic replace
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(sb.toString())
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun formatTomlString(s: String): String {
        val escaped = s.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
