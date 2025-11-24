package chat.schildi.revenge.glue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.matrix.api.auth.OidcRedirectUrlProvider

@ContributesBinding(AppScope::class)
object DefaultOidcRedirectUrlProvider : OidcRedirectUrlProvider {
    override fun provide() = "chat.schildi.revenge:/"
}
