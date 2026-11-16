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
 * Load the Rust SDKs for JNA.
 */
object SdkLoader {
    private val log = Logger.withTag("SdkLoader")
    private val loaded = AtomicBoolean(false)

    private val isDebugBuild = BuildInfo.BUILD_TYPE == "debug"

    private fun libFileName(name: String): String = when (SystemInfo.getOs()) {
        OperatingSystem.Windows -> "$name.dll"
        OperatingSystem.Mac -> "lib$name.dylib"
        else -> "lib$name.so"
    }

    private val nativeLibraries = listOf(
        "matrix_sdk_ffi" to "../matrix-rust-sdk/target/${BuildInfo.RUST_PROFILE}",
        "matrix_rtc_ffi" to "../matrix-rust-rtc/target/${if (BuildInfo.RUST_PROFILE == "release") "release" else "debug"}",
    )

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return

            for ((library, devDir) in nativeLibraries) {
                loadLibrary(library, libFileName(library), devDir)
            }

            initPlatform(
                config = TracingConfiguration(
                    logLevel = LogLevel.DEBUG,
                    traceLogPacks = emptyList(),
                    extraTargets = emptyList(),
                    writeToStdoutOrSystem = false,
                    writeToFiles = createSdkTracingFileConfiguration(),
                ),
                useLightweightTokioRuntime = false
            )
            loaded.set(true)
        }
    }

    private fun loadLibrary(library: String, libName: String, devDir: String) {
        val candidateDirs = buildList<File> {
            if (isDebugBuild) {
                // For development, use local path
                add(File(devDir).absoluteFile)
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
                    return
                } catch (e: UnsatisfiedLinkError) {
                    linkingAttempts.add(file to e)
                    // try next
                }
            }
        }

        linkingAttempts.forEach { (file, error) ->
            log.e("Linking $library failed via ${file.absolutePath}", error)
        }
        throw IllegalStateException("Failed to find the $libName in following paths: [${candidateDirs.joinToString()}]", linkingAttempts.lastOrNull()?.second)
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
