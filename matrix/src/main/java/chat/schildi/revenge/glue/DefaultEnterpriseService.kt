package chat.schildi.revenge.glue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.enterprise.api.EnterpriseService

@ContributesBinding(AppScope::class)
object DefaultEnterpriseService : EnterpriseService {
    override val isEnterpriseBuild: Boolean = false

    override suspend fun tweakMasUrl(url: String, urlContentFetcher: Any): String = url
}
