/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api

import io.element.android.libraries.matrix.api.core.SessionId

/**
 * In-memory cache of the [MatrixClient] of every logged in session.
 *
 * Prefer injecting the [MatrixClient] directly: this provider is meant for the entry points that only know a [SessionId], such as push handling and workers.
 */
interface MatrixClientProvider {
    /**
     * Returns the cached [MatrixClient] for [sessionId], restoring the session from storage and starting its sync if it is not in memory yet.
     * Concurrent calls are serialised, so only one restore happens per session.
     *
     * @param sessionId the [SessionId] of the session to get or restore.
     * @return the client, or a failure if the session could not be restored.
     */
    suspend fun getOrRestore(sessionId: SessionId): Result<MatrixClient>

    /**
     * SC: For initial restoration of clients, we can initialize multiple in parallel, as long as we don't do
     * same sessionIds multiple time, so this one allows some smarter mutex usage for faster initialization.
     **/
    suspend fun <T>runBatchRestore(block: suspend BatchRestoreScope.() -> T): T

    /**
     * Returns the [MatrixClient] for [sessionId] only if it is already in memory, without attempting to restore it.
     *
     * @param sessionId the [SessionId] of the session to retrieve.
     * @return the cached client, or `null` if that session is not currently held in memory.
     */
    fun getOrNull(sessionId: SessionId): MatrixClient?
}

interface BatchRestoreScope {
    suspend fun getOrRestoreInBatch(sessionId: SessionId): Result<MatrixClient>
}
