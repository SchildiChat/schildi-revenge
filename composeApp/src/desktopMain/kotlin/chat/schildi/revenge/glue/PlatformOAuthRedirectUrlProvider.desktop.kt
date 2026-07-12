package chat.schildi.revenge.glue

import chat.schildi.revenge.ipc.SingleInstance
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider

actual val RevengeOAuthRedirectUrlProvider: OAuthRedirectUrlProvider = object : OAuthRedirectUrlProvider {
    override suspend fun provide(): String = "http://127.0.0.1:${SingleInstance.awaitIpcServerPort()}"
}
