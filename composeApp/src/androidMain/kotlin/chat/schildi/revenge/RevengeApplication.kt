package chat.schildi.revenge

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import chat.schildi.revenge.glue.AndroidSyncOrchestrationAppStateProvider
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.revenge.util.ExternalViewCache
import chat.schildi.revenge.util.filepicker.ComposerAttachmentCache
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform
import kotlin.system.exitProcess

class RevengeApplication : Application() {
    companion object {
        lateinit var instance: RevengeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        attachAppDirs()
        Logger.setLogWriters(
            platformLogWriter(),
            createAppFileLogWriter(),
        )
        val systemExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                logUncaughtException(thread, error)
            } finally {
                systemExceptionHandler?.uncaughtException(thread, error) ?: exitProcess(1)
            }
        }
        logProcessStarted()
        val initScope = ScCoroutines.scope(Dispatchers.IO, "AppInit")
        ComposerAttachmentCache.clearAsync(initScope)
        ExternalViewCache.clearAsync(initScope)
        AndroidSyncOrchestrationAppStateProvider.start(this)
        initPlatform(
            config = TracingConfiguration(
                logLevel = LogLevel.INFO,
                traceLogPacks = emptyList(),
                extraTargets = emptyList(),
                writeToStdoutOrSystem = false,
                writeToFiles = createSdkTracingFileConfiguration(),
            ),
            useLightweightTokioRuntime = false,
        )

        initScope.launch {
            RevengePrefs.prefetch()
        }
    }
}
