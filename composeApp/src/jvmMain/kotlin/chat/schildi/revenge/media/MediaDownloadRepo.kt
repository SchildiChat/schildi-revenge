package chat.schildi.revenge.media

import chat.schildi.revenge.UiState
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.media.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

interface MediaDownloadResultListener {
    suspend fun onDownloadResult(result: Result<File>)
}

abstract class KeyedMediaDownloadResultListener(
    val listenerId: Any,
) : MediaDownloadResultListener {
    override fun equals(other: Any?): Boolean = (other as? KeyedMediaDownloadResultListener)?.listenerId == listenerId
    override fun hashCode() = listenerId.hashCode()
}

data class ScopedMediaKey(
    val sessionId: SessionId,
    val url: String,
)

private data class JobInfo(
    val job: Job,
    val listeners: List<MediaDownloadResultListener>,
)

object MediaDownloadRepo {
    private val log = Logger.withTag("MediaDownloadRepo")

    private val jobQueueLock = Mutex()

    private val jobs = ConcurrentHashMap<ScopedMediaKey, JobInfo>()

    /**
     * If another listener with [actionKey] is already registered, will exit early and not be added again.
     * The callback function may still be called early if the file already exists or download will not be successful,
     * so if you add the same listener multiple times, you may still get multiple callback invocations.
     */
    suspend fun requestAttachmentDownload(
        sessionId: SessionId,
        roomId: RoomId,
        sourceTimestamp: Long,
        mediaSource: MediaSource,
        mimeType: String?,
        filename: String?,
        actionKey: Any,
        onResult: suspend (Result<File>) -> Unit,
    ) = requestAttachmentDownload(
        sessionId = sessionId,
        roomId = roomId,
        sourceTimestamp = sourceTimestamp,
        mediaSource = mediaSource,
        mimeType = mimeType,
        filename = filename,
        listener = object : KeyedMediaDownloadResultListener(actionKey) {
            override suspend fun onDownloadResult(result: Result<File>) = onResult(result)

        }
    )

    suspend fun requestAttachmentDownload(
        sessionId: SessionId,
        roomId: RoomId,
        sourceTimestamp: Long,
        mediaSource: MediaSource,
        mimeType: String?,
        filename: String?,
    ): Result<File> {
        val resultState = MutableStateFlow<Result<File>?>(null)
        requestAttachmentDownload(
            sessionId = sessionId,
            roomId = roomId,
            sourceTimestamp = sourceTimestamp,
            mediaSource = mediaSource,
            mimeType = mimeType,
            filename = filename,
            listener = object : MediaDownloadResultListener {
                override suspend fun onDownloadResult(result: Result<File>) = resultState.emit(result)

            }
        )
        return resultState.filterNotNull().first()
    }

    suspend fun requestAttachmentDownload(
        sessionId: SessionId,
        roomId: RoomId,
        sourceTimestamp: Long,
        mediaSource: MediaSource,
        mimeType: String?,
        filename: String?,
        listener: MediaDownloadResultListener,
    ) = withContext(Dispatchers.IO) {
        val file = MediaPersistencePaths.getPersistentAttachmentFile(
            sessionId,
            roomId,
            sourceTimestamp,
            mediaSource.safeUrl,
            filename,
        )
        if (file.exists()) {
            listener.onDownloadResult(Result.success(file))
            return@withContext
        }
        val client = UiState.currentClientFor(sessionId)
        if (client == null) {
            listener.onDownloadResult(Result.failure(IOException("Client not ready")))
            return@withContext
        }
        val key = ScopedMediaKey(sessionId, mediaSource.safeUrl)
        jobQueueLock.withLock {
            if (file.exists()) {
                listener.onDownloadResult(Result.success(file))
                return@withContext
            }
            val jobInfo = jobs[key]
            val mediaLoader = client.matrixMediaLoader
            if (jobInfo == null || jobInfo.job.isCancelled) {
                jobs[key] = JobInfo(
                    job = kickDownload(key, mediaLoader, mediaSource, mimeType, filename, file),
                    listeners = listOf(listener),
                )
            } else {
                jobs[key] = jobInfo.copy(
                    listeners = (jobInfo.listeners + listener).distinct(),
                )
            }
        }
    }

    private fun CoroutineScope.kickDownload(
        key: ScopedMediaKey,
        mediaLoader: MatrixMediaLoader,
        mediaSource: MediaSource,
        mimeType: String?,
        filename: String?,
        outFile: File,
    ): Job {
        return launch(Dispatchers.IO) {
            var result: Result<File>
            var listeners: List<MediaDownloadResultListener>
            try {
                val downloadResult = mediaLoader.downloadMediaFile(
                    source = mediaSource,
                    mimeType = mimeType,
                    filename = filename,
                    useCache = false,
                )
                result = if (downloadResult.isFailure) {
                    Result.failure(downloadResult.exceptionOrNull()!!)
                } else {
                    var exception = IOException("Failed to persist download")
                    try {
                        val download = downloadResult.getOrNull()!!
                        val persistSuccess = download.use {
                            // Try persisting via Rust SDK, which just renames the file - won't work across filesystems
                            if (it.persist(outFile.absolutePath)) {
                                true
                            } else {
                                log.e("Failed to persist file in-place, resorting to copy")
                                try {
                                    Files.copy(it.toFile().toPath(), outFile.toPath())
                                    true
                                } catch (e: IOException) {
                                    exception = e
                                    false
                                }
                            }
                        }
                        if (persistSuccess) {
                            Result.success(outFile)
                        } else {
                            Result.failure(exception)
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            } finally {
                listeners = jobQueueLock.withLock {
                    jobs.remove(key)?.listeners.orEmpty()
                }
            }
            listeners.forEach {
                it.onDownloadResult(result)
            }
        }
    }
}
