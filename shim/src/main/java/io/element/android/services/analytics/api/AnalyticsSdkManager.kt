package io.element.android.services.analytics.api

// shim package, just do whatever needed to make it compile
interface AnalyticsSdkManager {
    fun enableSdkAnalytics(enabled: Boolean)
    fun startSpan(name: String, parentTraceId: String?): AnalyticsSdkSpan
    fun bridge(parentTraceId: String?): AnalyticsSdkSpan
}
