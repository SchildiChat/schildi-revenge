package chat.schildi.revenge.ipc

import chat.schildi.revenge.UiState
import co.touchlab.kermit.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Simple cross-platform single-instance guard with localhost TCP IPC.
 * Primary instance holds a file lock and runs a small server accepting commands.
 * Secondary instance connects and sends a SHOW command to unminimize the UI and exits.
 */
object SingleInstance {
    private val log = Logger.withTag("SingleInstance")

    private val appDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".schildi-revenge")
        dir.mkdirs()
        dir
    }
    private val lockFile = File(appDir, "app.lock")
    private val portFile = File(appDir, "app.port")

    @Volatile
    private var lockChannel: FileChannel? = null

    @Volatile
    private var serverThread: Thread? = null

    fun ensureSingleInstanceOrExit() {
        if (tryAcquireLock()) {
            startServer()
            return
        }
        // Another instance seems to be running; try to notify it and exit.
        log.i { "Another instance is already running" }
        notifyExistingInstance()
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

    private fun startServer() {
        serverThread = thread(name = "single-instance-ipc", isDaemon = true) {
            try {
                ServerSocket(0, 0, InetAddress.getByName(null)).use { server ->
                    val port = server.localPort
                    portFile.writeText(port.toString())
                    log.i("IPC server started on 127.0.0.1:$port")
                    while (!Thread.currentThread().isInterrupted) {
                        val socket = server.accept()
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                log.e("IPC server error", e)
            } finally {
                // Best effort cleanup
                runCatching { portFile.delete() }
            }
        }
        // Also add a shutdown hook to clean files
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            runCatching { portFile.delete() }
            runCatching { lockChannel?.close() }
        })
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(s.getOutputStream(), true)
            val line = reader.readLine()?.trim() ?: return
            when (line.uppercase()) {
                "SHOW" -> {
                    UiState.setMinimized(false)
                    writer.println("OK")
                }
                else -> writer.println("ERR Unknown command")
            }
        }
    }

    private fun notifyExistingInstance() {
        val port = runCatching { portFile.readText().trim().toInt() }.getOrNull()
        if (port == null) {
            log.w("Port file missing or invalid; cannot notify existing instance.")
            return
        }
        runCatching {
            Socket(InetAddress.getByName(null), port).use { socket ->
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("SHOW")
                reader.readLine() // read response
            }
        }.onFailure {
            log.w("Failed to notify existing instance on port $port", it)
        }
    }
}
