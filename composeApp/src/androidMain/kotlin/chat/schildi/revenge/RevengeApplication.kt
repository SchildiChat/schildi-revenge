package chat.schildi.revenge

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform

class RevengeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        attachAppDirs()
        initPlatform(
            config = TracingConfiguration(
                logLevel = LogLevel.INFO,
                traceLogPacks = emptyList(),
                extraTargets = emptyList(),
                writeToStdoutOrSystem = true,
                writeToFiles = null,
            ),
            useLightweightTokioRuntime = false,
        )
    }
}
