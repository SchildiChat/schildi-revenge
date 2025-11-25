package io.element.android.services.analytics.api

import io.element.android.services.analyticsproviders.api.AnalyticsTransaction

object AnalyticsService

inline fun <T> AnalyticsService.recordTransaction(
    name: String,
    operation: String,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)
