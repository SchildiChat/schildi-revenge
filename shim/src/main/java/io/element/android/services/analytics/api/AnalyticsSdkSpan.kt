package io.element.android.services.analytics.api

// shim, just add whatever needed to make it compile
interface AnalyticsSdkSpan {
    fun enter()
    fun exit()
}
