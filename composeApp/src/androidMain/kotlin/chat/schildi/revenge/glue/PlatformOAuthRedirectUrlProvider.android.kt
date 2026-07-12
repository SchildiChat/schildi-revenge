package chat.schildi.revenge.glue

import chat.schildi.revenge.RevengeApplication
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider

actual val RevengeOAuthRedirectUrlProvider: OAuthRedirectUrlProvider = object : OAuthRedirectUrlProvider {
    override suspend fun provide(): String = "${RevengeApplication.instance.packageName}:/"
}
