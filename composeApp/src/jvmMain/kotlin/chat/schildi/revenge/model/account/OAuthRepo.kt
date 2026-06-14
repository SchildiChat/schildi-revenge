package chat.schildi.revenge.model.account

import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.glue.RevengeOAuthRedirectUrlProvider
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.auth.OAuthDetails
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val RevengeOAUthRepo = OAuthRepo()

sealed interface OAuthLoginState {
    data object Idle : OAuthLoginState
    data object Cancelled : OAuthLoginState
    data class Waiting(val details: OAuthDetails) : OAuthLoginState
    data class Processing(val url: String) : OAuthLoginState
    data class AuthenticationResult(val result: Result<SessionId>) : OAuthLoginState
}

class OAuthRepo(
    private val authenticationService: MatrixAuthenticationService = UiState.appGraph.authenticationService,
    private val oAuthRedirectUrlProvider: OAuthRedirectUrlProvider = RevengeOAuthRedirectUrlProvider,
) {
    private val _state = MutableStateFlow<OAuthLoginState>(OAuthLoginState.Idle)
    val state = _state.asStateFlow()

    fun onOAuthRequestLaunched(details: OAuthDetails) {
        _state.value = OAuthLoginState.Waiting(details)
    }

    fun onOAuthRequestCancelled() {
        _state.value = OAuthLoginState.Cancelled
    }

    suspend fun handleOAuthLoginCallback(callbackUrl: String): ActionResult {
        _state.value = OAuthLoginState.Processing(callbackUrl)
        return authenticationService
            .loginWithOAuth(prepareCallbackUrl(callbackUrl))
            .also { _state.value = OAuthLoginState.AuthenticationResult(it) }
            .toActionResult()
    }

    private suspend fun prepareCallbackUrl(callbackUrl: String): String {
        return if (callbackUrl.startsWith("/")) {
            "${oAuthRedirectUrlProvider.provide()}$callbackUrl"
        } else {
            callbackUrl
        }
    }
}
