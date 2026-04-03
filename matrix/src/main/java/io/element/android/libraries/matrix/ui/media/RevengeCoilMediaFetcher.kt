/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import chat.schildi.revenge.util.escapeForFilename
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer

/**
 * Revenge's alternative to upstream CoilMediaFetcher:
 * - Replaces MediaRequestData.Kind.Content with a file-based fetcher and bypasses SDK to read existing downloads.
 * - Disables Rust-SDK-based file cache for these requests
 */
internal class RevengeCoilMediaFetcher(
    private val mediaLoader: MatrixMediaLoader,
    private val mediaData: MediaRequestData,
) : Fetcher {

    private val cacheDir = File(mediaLoader.baseCacheDirectory, "mediaContent")

    override suspend fun fetch(): FetchResult? {
        val mediaSource = mediaData.source
        if (mediaSource == null) {
            Timber.e("MediaData source is null")
            return null
        }
        return when (val kind = mediaData.kind) {
            is MediaRequestData.Kind.Content -> fetchFile(mediaSource, buildFileForContentCache(mediaSource.safeUrl))
            is MediaRequestData.Kind.Thumbnail -> fetchThumbnail(mediaSource, kind)
            is MediaRequestData.Kind.File -> fetchFile(mediaSource, kind)
        }
    }

    private suspend fun buildFileForContentCache(url: String) = withContext(Dispatchers.IO) {
        MediaRequestData.Kind.File(
            fileName = url.removePrefix("mxc://").escapeForFilename(),
            mimeType = "application/octet-stream",
        )
    }

    private suspend fun fetchFile(mediaSource: MediaSource, kind: MediaRequestData.Kind.File): FetchResult? {
        val expectFile = File(mediaLoader.baseCacheDirectory, kind.fileName)
        if (expectFile.exists() && expectFile.isFile && expectFile.length() > 0) {
            return SourceFetchResult(
                source = ImageSource(
                    file = expectFile.toOkioPath(),
                    fileSystem = FileSystem.SYSTEM,
                    closeable = null,
                ),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        return mediaLoader.downloadMediaFile(mediaSource, kind.mimeType, kind.fileName, useCache = false)
            .map { mediaFile ->
                expectFile.parentFile.mkdirs()
                mediaFile.use {
                    it.persist(expectFile.path)
                }
                SourceFetchResult(
                    source = ImageSource(
                        file = expectFile.toOkioPath(),
                        fileSystem = FileSystem.SYSTEM,
                        closeable = null,
                    ),
                    mimeType = null,
                    dataSource = DataSource.NETWORK,
                )
            }
            .onFailure {
                Timber.w("Failed to fetch file: $it")
            }
            .getOrNull()
    }

    private suspend fun fetchThumbnail(mediaSource: MediaSource, kind: MediaRequestData.Kind.Thumbnail): FetchResult? {
        return mediaLoader.loadMediaThumbnail(
            source = mediaSource,
            width = kind.width,
            height = kind.height,
        ).map { byteArray ->
            byteArray.asSourceResult()
        }.onFailure {
            Timber.w("Failed to fetch thumbnail: $it")
        }.getOrNull()
    }

    private fun ByteArray.asSourceResult(): SourceFetchResult {
        val byteBuffer = ByteBuffer.wrap(this)
        val bufferedSource = try {
            Buffer().apply { write(byteBuffer) }
        } finally {
            byteBuffer.position(0)
        }
        return SourceFetchResult(
            source = ImageSource(
                source = bufferedSource,
                fileSystem = FileSystem.SYSTEM,
            ),
            mimeType = null,
            dataSource = DataSource.MEMORY
        )
    }
}
