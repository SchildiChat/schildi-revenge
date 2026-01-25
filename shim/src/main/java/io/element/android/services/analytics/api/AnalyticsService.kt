package io.element.android.services.analytics.api

import co.touchlab.kermit.Logger
import io.element.android.services.analyticsproviders.api.AnalyticsTransaction

object AnalyticsService {
    fun getLongRunningTransaction(tx: Any) {}
    fun addIndexableData(vararg idc: Any) {}
    fun trackError(t: Throwable) {
        Logger.withTag("TrackError").w("Tracked error", t)
    }
}

inline fun <T> AnalyticsService.recordTransaction(
    name: String,
    operation: String,
    parentTransaction: Unit = Unit,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)


inline fun <T> AnalyticsService.inBridgeSdkSpan(
    parentTraceId: Unit,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)

object AnalyticsLongRunningTransaction {
    val OpenRoom = Unit
}
