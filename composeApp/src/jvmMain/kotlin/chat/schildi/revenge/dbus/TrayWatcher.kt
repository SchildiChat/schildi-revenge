package chat.schildi.revenge.dbus

import chat.schildi.revenge.UiState
import chat.schildi.revenge.util.OsDetection
import co.touchlab.kermit.Logger
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

object TrayWatcher {
    private val log = Logger.withTag("TrayWatcher")
    private const val statusNotifierWatcherName = "org.kde.StatusNotifierWatcher"
    private const val reconnectDelayMs = 2_000L
    private const val recreationDebounceMs = 750L

    private val started = AtomicBoolean(false)
    private val lastRecreationAtMs = AtomicLong(0L)

    fun start() {
        if (!isSupportedEnvironment()) {
            return
        }
        if (!started.compareAndSet(false, true)) {
            return
        }

        thread(name = "TrayWatcher", isDaemon = true) {
            runWatcherLoop()
        }
    }

    private fun isSupportedEnvironment(): Boolean {
        if (!OsDetection.isLinux()) {
            return false
        }

        val sessionType = System.getenv("XDG_SESSION_TYPE")?.lowercase().orEmpty()
        return sessionType == "wayland"
    }

    private fun runWatcherLoop() {
        while (true) {
            var connection: DBusConnection? = null
            var handlerCloseable: AutoCloseable? = null
            try {
                connection =
                    DBusConnectionBuilder
                        .forSessionBus()
                        .withShared(false)
                        .build()

                handlerCloseable =
                    connection.addSigHandler(DBus.NameOwnerChanged::class.java) { signal ->
                        if (signal.name != statusNotifierWatcherName) {
                            return@addSigHandler
                        }

                        val now = System.currentTimeMillis()
                        val last = lastRecreationAtMs.get()
                        if (now - last < recreationDebounceMs) {
                            return@addSigHandler
                        }

                        if (!lastRecreationAtMs.compareAndSet(last, now)) {
                            return@addSigHandler
                        }

                        log.i {
                            "StatusNotifierWatcher owner changed from '${signal.oldOwner}' to '${signal.newOwner}', recreating UI"
                        }
                        UiState.recreateTayIcon()
                    }

                while (connection.isConnected) {
                    Thread.sleep(5_000L)
                }
            } catch (t: Throwable) {
                log.w(t) { "Tray watcher disconnected, will retry" }
            } finally {
                try {
                    handlerCloseable?.close()
                } catch (_: Throwable) {
                }
                try {
                    connection?.close()
                } catch (_: Throwable) {
                }
            }

            Thread.sleep(reconnectDelayMs)
        }
    }
}
