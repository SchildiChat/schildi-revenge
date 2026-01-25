package io.element.android.services.analyticsproviders.api

object AnalyticsTransaction {
    fun setData(key: String, value: Any) {}
    fun putIndexableData(vararg idc: Any) {}
    fun traceId() = Unit
}

inline fun <T> AnalyticsTransaction.recordChildTransaction(
    operation: String,
    description: String?,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)
