package chat.schildi.revenge.dbus

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.io.File
import java.net.URI

object FreedesktopPortal {
    suspend fun requestFiles(title: String): List<File>? = withContext(Dispatchers.IO) {
        var connection: DBusConnection? = null
        var signalHandler: AutoCloseable? = null
        try {
            connection = DBusConnectionBuilder
                .forSessionBus()
                .withShared(false)
                .build()

            val fileChooser = connection.getRemoteObject(
                PORTAL_BUS_NAME,
                PORTAL_OBJECT_PATH,
                FileChooser::class.java,
                true,
            )
            val requestPath = fileChooser.OpenFile(
                "",
                title,
                mapOf(
                    "multiple" to Variant(false),
                    "modal" to Variant(true),
                ),
            )

            val response = CompletableDeferred<Request.Response>()
            val matchRule = DBusMatchRuleBuilder.create()
                .withType(Request.Response::class.java)
                .withPath(requestPath.path)
                .build()

            signalHandler = connection.addSigHandler(matchRule) { signal: Request.Response ->
                response.complete(signal)
            }

            response.await().toFileChooserResult()
        } finally {
            try {
                signalHandler?.close()
            } catch (_: Throwable) {
            }
            try {
                connection?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun Request.Response.toFileChooserResult(): List<File>? {
        return when (response.toInt()) {
            0 -> {
                val uris = results["uris"]?.value.asStringList()
                uris.mapNotNull { uri ->
                    runCatching { File(URI(uri)) }.getOrNull()
                }
            }
            1, 2 -> null
            else -> error("Desktop portal file chooser failed with response code ${response.toInt()}")
        }
    }

    private fun Any?.asStringList(): List<String> {
        return when (this) {
            is Array<*> -> filterIsInstance<String>()
            is Iterable<*> -> filterIsInstance<String>()
            is String -> listOf(this)
            else -> emptyList()
        }
    }

    @DBusInterfaceName("org.freedesktop.portal.FileChooser")
    interface FileChooser : DBusInterface {
        @Suppress("FunctionName")
        fun OpenFile(parentWindow: String, title: String, options: Map<String, Variant<*>>): DBusPath
    }

    @DBusInterfaceName("org.freedesktop.portal.Request")
    interface Request : DBusInterface {
        class Response(
            path: String,
            val response: UInt32,
            val results: Map<String, Variant<*>>,
        ) : DBusSignal(path, response, results)
    }

    private const val PORTAL_BUS_NAME = "org.freedesktop.portal.Desktop"
    private const val PORTAL_OBJECT_PATH = "/org/freedesktop/portal/desktop"
}
