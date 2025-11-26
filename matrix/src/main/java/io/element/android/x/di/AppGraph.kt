/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import io.element.android.appnav.di.MatrixSessionCache
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.ui.media.ImageLoaderFactory
import io.element.android.libraries.sessionstorage.api.SessionStore

@DependencyGraph(AppScope::class)
interface AppGraph {
    val sessionGraphFactory: SessionGraph.Factory
    val authenticationService: MatrixAuthenticationService
    val sessionStore: SessionStore
    val sessionCache: MatrixSessionCache
    val imageLoaderFactory: ImageLoaderFactory

    @DependencyGraph.Factory
    interface Factory {
        fun create(): AppGraph
    }
}
