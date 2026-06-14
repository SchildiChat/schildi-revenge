package chat.schildi.revenge.ipc

import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.DeferredActionResultCallback
import chat.schildi.revenge.config.ScAppDirs
import chat.schildi.revenge.config.keybindings.Action
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import shire.res.generated.resources.Res
import shire.res.generated.resources.oauth_login_failure_details_title
import shire.res.generated.resources.oauth_login_failure_message
import shire.res.generated.resources.oauth_login_failure_title
import shire.res.generated.resources.oauth_login_success_message
import shire.res.generated.resources.oauth_login_success_title
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

/**
 * Simple cross-platform single-instance guard with localhost TCP IPC.
 * Primary instance holds a file lock and runs a small server accepting commands.
 * Secondary instance connects and sends a SHOW command to unminimize the UI and exits.
 */
object SingleInstance {
    private val log = Logger.withTag("SingleInstance")
    private const val CLIENT_TIMEOUT_MILLIS = 30_000

    private val dataDir = File(ScAppDirs.getUserDataDir()).also {
        it.mkdirs()
    }
    private val lockFile = File(dataDir, "app.lock")
    private val portFile = File(dataDir, "app.port")

    @Volatile
    private var lockChannel: FileChannel? = null

    @Volatile
    private var serverThread: Thread? = null

    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val serverPort = CompletableDeferred<Int>()

    fun ensureSingleInstanceOrExit(openExistingInstance: Boolean) {
        if (tryAcquireLock()) {
            startServer()
            return
        }
        // Another instance seems to be running; try to notify it and exit.
        log.i { "Another instance is already running" }
        if (openExistingInstance) {
            notifyExistingInstance("${Action.Global.SetMinimized.name} false")
        }
        exitProcess(0)
    }

    private fun tryAcquireLock(): Boolean {
        return try {
            lockChannel = FileChannel.open(
                lockFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
            val lock = lockChannel!!.tryLock()
            if (lock == null) {
                false
            } else {
                // Keep the lock and channel open for the lifetime of the process
                true
            }
        } catch (e: OverlappingFileLockException) {
            false
        } catch (e: Exception) {
            log.w("Failed to acquire lock, assuming another instance is running", e)
            false
        }
    }

    suspend fun awaitIpcServerPort() = serverPort.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startServer() {
        serverThread = thread(name = "single-instance-ipc", isDaemon = true) {
            try {
                ServerSocket(0, 0, InetAddress.getByName(null)).use { server ->
                    val port = server.localPort
                    if (!serverPort.complete(port)) {
                        throw RuntimeException("Started IPC server twice on ports ${serverPort.getCompleted()} and $port")
                    }
                    portFile.writeText(port.toString())
                    log.i("IPC server started on 127.0.0.1:$port")
                    while (!Thread.currentThread().isInterrupted) {
                        val socket = server.accept()
                        socket.soTimeout = CLIENT_TIMEOUT_MILLIS
                        serverScope.launch {
                            try {
                                handleClient(socket)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                log.w("IPC client error", e)
                                runCatching { socket.close() }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.e("IPC server error", e)
                serverPort.completeExceptionally(e)
            } finally {
                // Best effort cleanup
                serverScope.cancel()
                runCatching { portFile.delete() }
            }
        }
        // Also add a shutdown hook to clean files
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            serverScope.cancel()
            runCatching { portFile.delete() }
            runCatching { lockChannel?.close() }
            runCatching { lockFile.delete() }
        })
    }

    private suspend fun handleClient(socket: Socket) {
        var speakHttp = false
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8), false)
            val line = reader.readLine()?.trim() ?: return
            if (line.actionLooksLikeHttp()) {
                speakHttp = true
            }
            val deferredResult = DeferredActionResultCallback()
            val immediateResult = UiState.headlessKeyboardActionHandler?.executeCommandFromIpc(line, deferredResult)
            val result = if (immediateResult == null) {
                ActionResult.Failure("Not initialized")
            } else if ((immediateResult as? ActionResult.Success)?.async == true) {
                if (!speakHttp) {
                    writer.println("Processing...")
                    writer.flush()
                }
                withTimeoutOrNull(CLIENT_TIMEOUT_MILLIS.toLong().milliseconds) {
                    deferredResult.actionResult.await()
                } ?: ActionResult.Failure("Timed out waiting for action result")
            } else {
                immediateResult
            }
            if (speakHttp) {
                val respCode = when (result) {
                    is ActionResult.Success -> "200 OK"
                    else -> "400 Bad Request"
                }
                val body = renderHttpResult(result)
                writer.print("HTTP/1.1 $respCode\r\n")
                writer.print("Content-Type: text/html; charset=utf-8\r\n")
                writer.print("Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n")
                writer.print("Connection: close\r\n")
                writer.print("\r\n")
                writer.print(body)
            } else {
                val resultJson = Json.encodeToString(result)
                writer.println(resultJson)
            }
            writer.flush()
        }
    }

    private suspend fun renderHttpResult(result: ActionResult): String {
        val success = result is ActionResult.Success
        val template = Res.readBytes(
            if (success) "files/oauth-success.html" else "files/oauth-failure.html"
        ).decodeToString()
        return renderTemplate(
            template,
            mapOf(
                "iconSrc" to getOAuthIconData(success),
                "title" to getString(if (success) Res.string.oauth_login_success_title else Res.string.oauth_login_failure_title),
                "message" to getString(if (success) Res.string.oauth_login_success_message else Res.string.oauth_login_failure_message),
                "details_header" to getString(Res.string.oauth_login_failure_details_title),
                "details_content" to ((result as? ActionResult.Failure)?.message ?: result.toString()),
            )
        )
    }

    private suspend fun getOAuthIconData(success: Boolean): String {
        val iconBytes = Res.readBytes(
            if (success) "drawable-xxxhdpi/ic_launcher.png" else "files/turtle_error.png"
        )
        return "data:image/png;base64,${Base64.getEncoder().encodeToString(iconBytes)}"
    }

    private fun renderTemplate(template: String, values: Map<String, String>): String {
        return values.entries.fold(template) { rendered, (key, value) ->
            rendered.replace("{{$key}}", value.escapeHtml())
        }
    }

    private fun String.escapeHtml(): String = buildString(length) {
        this@escapeHtml.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> char
                }
            )
        }
    }

    private fun String.actionLooksLikeHttp() = startsWith("GET ")

    fun notifyExistingInstance(command: String): Boolean {
        val port = runCatching { portFile.readText().trim().toInt() }.getOrNull()
        if (port == null) {
            log.w("Port file missing or invalid; cannot notify existing instance.")
            return false
        }
        runCatching {
            Socket(InetAddress.getByName(null), port).use { socket ->
                socket.soTimeout = CLIENT_TIMEOUT_MILLIS
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(command)
                val response = reader.readLine() // read response
                val parsedResponse = try {
                    Json.decodeFromString<ActionResult>(response)
                } catch (_: Exception) {
                    null
                }
                log.i("Result: $response")
                return parsedResponse is ActionResult.Success
            }
        }.onFailure {
            log.w("Failed to notify existing instance on port $port", it)
        }
        return false
    }
}
