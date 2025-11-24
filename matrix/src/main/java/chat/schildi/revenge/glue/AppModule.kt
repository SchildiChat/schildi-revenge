package chat.schildi.revenge.glue

import chat.schildi.revenge.util.ScAppDirs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.di.BaseDirectory
import io.element.android.libraries.di.CacheDirectory
import io.element.android.libraries.di.annotations.AppCoroutineScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.plus
import java.io.File

@BindingContainer
@ContributesTo(AppScope::class)
object AppModule {
    @Provides
    @BaseDirectory
    fun providesBaseDirectory(): File {
        return File(ScAppDirs.getUserDataDir(), "sessions")
    }

    @Provides
    @CacheDirectory
    fun providesCacheDirectory(): File {
        return File(ScAppDirs.getUserCacheDir(), "sdkCache")
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesBuildMeta(): BuildMeta {
        // TODO
        return BuildMeta(
            buildType = BuildType.DEBUG_SC,
            isDebuggable = true,
            applicationName = "Schildi Revenge",
            productionApplicationName = "SchildiRevenge",
            applicationId = "chat.schildi.revenge",
            lowPrivacyLoggingEnabled = false,
            versionName = "WIP",
            versionCode = 1,
            gitRevision = "",
            gitBranchName = "",
            flavorDescription = "",
            flavorShortDescription = ""
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesCoroutineDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers.Default
    }

    @Provides
    @AppCoroutineScope
    @SingleIn(AppScope::class)
    fun providesAppCoroutineScope(): CoroutineScope {
        return MainScope() + CoroutineName("SchildiRevenge Scope")
    }
}
