package chat.schildi.revenge

import chat.schildi.lib.util.OperatingSystem
import chat.schildi.lib.util.SystemInfo
import co.touchlab.kermit.Logger
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Load the Rust SDK for JNA.
 */
object SdkLoader {
    private val log = Logger.withTag("SdkLoader")
    private val loaded = AtomicBoolean(false)

    private val isDebugBuild = BuildInfo.BUILD_TYPE == "debug"
    private val libName = when (SystemInfo.getOs()) {
        OperatingSystem.Windows -> "matrix_sdk_ffi.dll"
        OperatingSystem.Mac -> "libmatrix_sdk_ffi.dylib"
        else -> "libmatrix_sdk_ffi.so"
    }

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return

            val candidateDirs = buildList<File> {
                if (isDebugBuild) {
                    // For development, use local path
                    add(File("../matrix-rust-sdk/target/${BuildInfo.RUST_PROFILE}").absoluteFile)
                } else {
                    // When installed natively
                    val resourcesDir = System.getProperty("compose.application.resources.dir")
                    add(File(resourcesDir))
                }
            }

            val linkingAttempts = mutableListOf<Pair<File, Throwable>>()
            for (dir in candidateDirs) {
                val file = File(dir, libName)
                if (file.isFile) {
                    // Help JNA find the library by adding directory to jna.library.path
                    tryAddJnaPath(dir)
                    // Eagerly load the exact file path to ensure symbols are present
                    try {
                        System.load(file.absolutePath)
                        loaded.set(true)
                        break
                    } catch (e: UnsatisfiedLinkError) {
                        linkingAttempts.add(file to e)
                        // try next
                    }
                }
            }

            if (!loaded.get()) {
                linkingAttempts.forEach { (file, error) ->
                    log.e("Linking failed via ${file.absolutePath}", error)
                }
                throw IllegalStateException("Failed to find the $libName in following paths: [${candidateDirs.joinToString()}]", linkingAttempts.lastOrNull()?.second)
            }

            initPlatform(
                config = TracingConfiguration(
                    logLevel = LogLevel.INFO,
                    traceLogPacks = emptyList(),
                    extraTargets = emptyList(),
                    writeToStdoutOrSystem = false,
                    writeToFiles = createSdkTracingFileConfiguration(),
                ),
                useLightweightTokioRuntime = false
            )
        }
    }

    private fun tryAddJnaPath(dir: File) {
        if (!dir.isDirectory) return
        val prop = "jna.library.path"
        val current = System.getProperty(prop)
        val pathSep = File.pathSeparator
        if (current == null || current.isEmpty()) {
            System.setProperty(prop, dir.absolutePath)
        } else if (!current.split(pathSep).any { it == dir.absolutePath }) {
            System.setProperty(prop, current + pathSep + dir.absolutePath)
        }
    }
}
