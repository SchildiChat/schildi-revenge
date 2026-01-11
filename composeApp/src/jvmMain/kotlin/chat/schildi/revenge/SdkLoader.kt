package chat.schildi.revenge

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ensures the Rust matrix-sdk-ffi native library is available for JNA at launch-time.
 *
 * This tries to:
 * - Resolve the OS-specific library filename (libmatrix_sdk_ffi.so/.dylib or matrix_sdk_ffi.dll)
 * - For packaged releases: look in the app's installation directory
 * - For development: find it under ../matrix-rust-sdk/target/{debug|release} relative to the project
 * - Add that directory to jna.library.path so JNA can locate it
 * - Eagerly System.load(<absolute path>) if the file exists to avoid lazy-loading errors
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

            val rustFlavor = BuildInfo.RUST_PROFILE

            // Try to resolve library path, checking packaged locations first, then development paths
            val candidateDirs = buildList<File> {
                val codeSource = SdkLoader::class.java.protectionDomain?.codeSource?.location
                val fromClasses = codeSource?.toURI()?.let { File(it).absoluteFile }
                
                if (fromClasses != null) {
                    // 1) Check for packaged app structure (jpackage/AppImage)
                    // The library should be in the app directory next to app/
                    // Structure: <app-dir>/lib/ or <app-dir>/app/ (jar) + <app-dir>/<libname>
                    var appDir: File? = fromClasses
                    // Navigate up from the jar/class location to find app root
                    repeat(10) {
                        appDir = appDir?.parentFile
                        if (appDir != null) {
                            // Try app root directory itself
                            add(appDir)
                            // Try lib subdirectory
                            add(File(appDir, "lib"))
                            // Try app subdirectory (common jpackage structure)
                            add(File(appDir, "app"))
                        }
                    }
                    
                    // 2) Relative to this class location: .../build/classes/... -> go up to project root heuristically
                    var f: File? = fromClasses
                    repeat(5) { // walk up a few levels to reach repo root in typical layouts
                        f = f?.parentFile
                        val dir = f?.resolve("matrix-rust-sdk/target/$rustFlavor")
                        if (dir != null) add(dir)
                    }
                }
                
                // 3) Working directory (useful when running from repo root during development)
                add(File("matrix-rust-sdk/target/$rustFlavor"))
                add(File("./matrix-rust-sdk/target/$rustFlavor").absoluteFile)
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
