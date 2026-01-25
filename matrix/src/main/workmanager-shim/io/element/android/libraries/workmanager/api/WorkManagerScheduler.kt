package io.element.android.libraries.workmanager.api

import io.element.android.libraries.matrix.api.core.SessionId

// Just a shim for now, make sure to implement any necessary clean up tasks manually!

interface ShimWorkManagerRequest

object WorkManagerScheduler {
    fun hasPendingWork(sessionId: SessionId, type: WorkManagerRequestType) = true
    fun submit(vararg idc: Any?) {}
}
