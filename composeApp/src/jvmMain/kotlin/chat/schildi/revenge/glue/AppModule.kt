package chat.schildi.revenge.glue

import android.content.Context
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.config.ScAppDirs
import chat.schildi.revenge.preferences.RevengePrefs
import coil3.PlatformContext
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File

@BindingContainer
@ContributesTo(AppScope::class)
object AppModule {
    val okHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(
            Interceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        // TODO version number?
                        .header("User-Agent", "Schildi Revenge")
                        .build()
                )
            }
        )
    }.build()

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
    @SingleIn(AppScope::class)
    fun providesScPreferencesStore(): ScPreferencesStore {
        return RevengePrefs
    }

    @Provides
    @AppCoroutineScope
    @SingleIn(AppScope::class)
    fun providesAppCoroutineScope(): CoroutineScope {
        return ScCoroutines.scope(Dispatchers.Main, "DI")
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesOkHttpClient(): OkHttpClient {
        return okHttpClient
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesPlatformContext(): Context {
        return PlatformContext.INSTANCE
    }
}
