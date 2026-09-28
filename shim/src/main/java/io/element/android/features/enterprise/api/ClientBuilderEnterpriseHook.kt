package io.element.android.features.enterprise.api

object ClientBuilderEnterpriseHook {
    operator fun <T>invoke(v: T, s: Any) = v
    fun <T>beforeClientCreationWithSession(clientBuilder: T, sessionId: Any): T = clientBuilder
    fun <T>beforeClientCreation(clientBuilder: T): T = clientBuilder
}
