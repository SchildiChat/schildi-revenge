/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.revenge.matrix

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.matrix.api.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

private const val SEND_QUEUES_RETRY_DELAY_MILLIS = 500L

class SendQueues(
    private val matrixClient: MatrixClient,
    private val syncService: SyncService = matrixClient.syncService,
) {
    /**
     * Launches the send queues retry mechanism in the given [coroutineScope].
     * Makes sure to re-enable all send queues when the network status is [NetworkStatus.Connected].
     */
    @OptIn(FlowPreview::class)
    fun launchIn(coroutineScope: CoroutineScope = matrixClient.sessionCoroutineScope) {
        combine(
            syncService.syncState,
            matrixClient.sendQueueDisabledFlow().onEach { Timber.tag("SendQueues").d("Send queue disabled for room: $it") },
        ) { syncState, _ -> syncState }
            .debounce(SEND_QUEUES_RETRY_DELAY_MILLIS.milliseconds)
            .onEach { syncState ->
                Timber.tag("SendQueues").d("Sync state changed: $syncState")
                if (syncState == SyncState.Running) {
                    Timber.tag("SendQueues").d("Enabling send queues again")
                    matrixClient.setAllSendQueuesEnabled(enabled = true)
                }
            }
            .launchIn(coroutineScope)
    }
}
