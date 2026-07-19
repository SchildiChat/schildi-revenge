package chat.schildi.revenge

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import chat.schildi.revenge.glue.AndroidSyncOrchestrationAppStateProvider
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
    }
}
