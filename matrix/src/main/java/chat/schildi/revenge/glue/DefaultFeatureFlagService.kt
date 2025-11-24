package chat.schildi.revenge.glue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.featureflag.api.Feature
import io.element.android.libraries.featureflag.api.FeatureFlagService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ContributesBinding(AppScope::class)
object DefaultFeatureFlagService : FeatureFlagService {
    override fun isFeatureEnabledFlow(feature: Feature): Flow<Boolean> {
        // TODO
        return flowOf(feature.defaultValue)
    }

    override suspend fun setFeatureEnabled(
        feature: Feature,
        enabled: Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAvailableFeatures(
        includeFinishedFeatures: Boolean,
        isInLabs: Boolean
    ): List<Feature> {
        TODO("Not yet implemented")
    }

}
