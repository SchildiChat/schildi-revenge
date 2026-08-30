package io.element.android.features.enterprise.api

interface EnterpriseService {
    val isEnterpriseBuild: Boolean

    suspend fun tweakMasUrl(url: String, urlContentFetcher: Any): String
}
