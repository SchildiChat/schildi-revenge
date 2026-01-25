package io.element.android.libraries.matrix.impl.workmanager

import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.workmanager.api.ShimWorkManagerRequest

// SC minimal shim for workerless
data class PerformDatabaseVacuumWorkManagerRequest(
    val sessionId: SessionId,
) : ShimWorkManagerRequest
