package io.element.android.services.analyticsproviders.api

object AnalyticsTransaction {
    fun setData(key: String, value: Any) {}
}

inline fun <T> AnalyticsTransaction.recordChildTransaction(
    operation: String,
    description: String?,
    block: (AnalyticsTransaction) -> T
): T = block(AnalyticsTransaction)
