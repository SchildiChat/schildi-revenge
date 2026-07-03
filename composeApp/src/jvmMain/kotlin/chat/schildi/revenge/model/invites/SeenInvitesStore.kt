/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.revenge.model.invites

import chat.schildi.revenge.model.ScopedRoomKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SeenInvitesStore {
    fun seenInvites(): Flow<Set<ScopedRoomKey>>
    fun isInviteSeen(key: ScopedRoomKey): Boolean
    suspend fun markInviteAsSeen(key: ScopedRoomKey)
    suspend fun markInviteAsUnSeen(key: ScopedRoomKey)

    fun isInviteSeenFlow(key: ScopedRoomKey) =seenInvites().map { key in it }
}
