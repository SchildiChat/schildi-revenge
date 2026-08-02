package chat.schildi.revenge

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import chat.schildi.revenge.glue.AndroidSyncOrchestrationAppStateProvider
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.revenge.util.ExternalViewCache
import chat.schildi.revenge.util.filepicker.ComposerAttachmentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform

class RevengeApplication : Application() {
    companion object {
        lateinit var instance: RevengeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        attachAppDirs()
        val initScope = ScCoroutines.scope(Dispatchers.IO, "AppInit")
        ComposerAttachmentCache.clearAsync(initScope)
        ExternalViewCache.clearAsync(initScope)
        AndroidSyncOrchestrationAppStateProvider.start(this)
        initPlatform(
            config = TracingConfiguration(
                logLevel = LogLevel.INFO,
                traceLogPacks = emptyList(),
                extraTargets = emptyList(),
                writeToStdoutOrSystem = BuildInfo.DEBUG,
                writeToFiles = null,
            ),
            useLightweightTokioRuntime = false,
        )

        initScope.launch {
            RevengePrefs.prefetch()
        }
    }
}
