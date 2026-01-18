package chat.schildi.revenge

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Load the Rust SDK for JNA.
 */
object SdkLoader {
    private val loaded = AtomicBoolean(false)

    private val isDebugBuild = BuildInfo.BUILD_TYPE == "debug"
    private val libName = when (val os = System.getProperty("os.name").lowercase()) {
        else -> {
            when {
                os.contains("win") -> "matrix_sdk_ffi.dll"
                os.contains("mac") || os.contains("darwin") -> "libmatrix_sdk_ffi.dylib"
                else -> "libmatrix_sdk_ffi.so"
            }
        }
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
                    } catch (_: UnsatisfiedLinkError) {
                        // try next
                    }
                }
            }

            if (!loaded.get()) {
                throw IllegalStateException("Failed to find the $libName in following paths: [${candidateDirs.joinToString()}]")
            }
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
