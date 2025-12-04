package io.element.android.services.analytics.api

import io.element.android.services.analyticsproviders.api.AnalyticsTransaction

object AnalyticsService {
    fun getLongRunningTransaction(tx: Any) {}
}

inline fun <T> AnalyticsService.recordTransaction(
    name: String,
    operation: String,
    parentTransaction: Unit = Unit,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)

object AnalyticsLongRunningTransaction {
    val OpenRoom = Unit
}
