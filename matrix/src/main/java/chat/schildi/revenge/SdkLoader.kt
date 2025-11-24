package chat.schildi.revenge

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ensures the Rust matrix-sdk-ffi native library is available for JNA at launch-time.
 *
 * This tries to:
 * - Resolve the OS-specific library filename (libmatrix_sdk_ffi.so/.dylib or matrix_sdk_ffi.dll)
 * - Find it under ../matrix-rust-sdk/target/debug relative to the project (development flow)
 * - Add that directory to jna.library.path so JNA can locate it
 * - Eagerly System.load(<absolute path>) if the file exists to avoid lazy-loading errors
 * TODO revise for release builds and possible packaging; and clean up AI stuff
 */
object SdkLoader {
    private val loaded = AtomicBoolean(false)

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return

            val libName = when (val os = System.getProperty("os.name").lowercase()) {
                else -> {
                    when {
                        os.contains("win") -> "matrix_sdk_ffi.dll"
                        os.contains("mac") || os.contains("darwin") -> "libmatrix_sdk_ffi.dylib"
                        else -> "libmatrix_sdk_ffi.so"
                    }
                }
            }

            // Try to resolve path relative to the repo root when running from sources.
            // matrix module dir is .../schildi-revenge/matrix; we want ../matrix-rust-sdk/target/debug/<lib>
            val candidateDirs = buildList<File> {
                // 1) Working directory (useful when running from repo root)
                add(File("matrix-rust-sdk/target/debug"))
                add(File("./matrix-rust-sdk/target/debug").absoluteFile)
                // 2) Relative to this class location: .../build/classes/... -> go up to project root heuristically
                val codeSource = SdkLoader::class.java.protectionDomain?.codeSource?.location
                val fromClasses = codeSource?.toURI()?.let { File(it).absoluteFile }
                if (fromClasses != null) {
                    var f: File? = fromClasses
                    repeat(5) { // walk up a few levels to reach repo root in typical layouts
                        f = f?.parentFile
                        val dir = f?.resolve("matrix-rust-sdk/target/debug")
                        if (dir != null) add(dir)
                    }
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
                        break
                    } catch (_: UnsatisfiedLinkError) {
                        // try next
                    }
                }
            }

            loaded.set(true)
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
