package chat.schildi.revenge.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import chat.schildi.revenge.RevengeApplication
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PushResolutionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val roomId = inputData.getString(KEY_ROOM_ID) ?: return Result.failure()
        log.d { "Resolving pushes for $sessionId/$roomId (attempt $runAttemptCount)" }
        return when (PushNotificationHandler.resolvePendingPushes(SessionId(sessionId), RoomId(roomId))) {
            PushResolutionOutcome.TransientFailure -> Result.retry()
            PushResolutionOutcome.PermanentFailure -> Result.failure()
            PushResolutionOutcome.Done -> Result.success()
        }
    }

    companion object {
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_ROOM_ID = "room_id"
        private const val UNIQUE_NAME_PREFIX = "push-resolution"
        private const val BACKOFF_DELAY_MILLIS = 30_000L

        private val log = Logger.withTag("PushResolutionWorker")

        fun uniqueWorkName(sessionId: String, roomId: String) = "$UNIQUE_NAME_PREFIX/$sessionId/$roomId"

        /**
         * Enqueues a [PushResolutionWorker] for the given (session, room) scope, unless a worker for
         * that scope is already scheduled to run and will pick up newly persisted pushes on its own.
         *
         * Workers re-read pending pushes from the database on start, so:
         * - if a never-ran worker is already waiting, skip (it will see the new pushes when it runs);
         * - if a worker is currently running, append a follow-up worker behind it;
         * - if only a backed-off retry is waiting, replace it so the new pushes are resolved immediately.
         */
        suspend fun schedule(context: Context, sessionId: String, roomId: String) {
            val name = uniqueWorkName(sessionId, roomId)
            val workManager = WorkManager.getInstance(context)
            val infos = workManager.getWorkInfosForUniqueWorkFlow(name).first()
            val running = infos.any { it.state == WorkInfo.State.RUNNING }
            val waiting = infos.filter {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
            }
            val policy = when {
                running && waiting.isNotEmpty() -> {
                    log.d { "Skipping enqueue for $name, waiting work will pick up new pushes" }
                    return
                }
                running -> ExistingWorkPolicy.APPEND_OR_REPLACE
                waiting.any { it.runAttemptCount > 0 } -> ExistingWorkPolicy.REPLACE
                waiting.isNotEmpty() -> {
                    log.d { "Skipping enqueue for $name, waiting work will pick up new pushes" }
                    return
                }
                else -> ExistingWorkPolicy.APPEND_OR_REPLACE
            }
            log.d { "Enqueuing $name with policy $policy" }
            val request = OneTimeWorkRequestBuilder<PushResolutionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_SESSION_ID, sessionId)
                        .putString(KEY_ROOM_ID, roomId)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniqueWork(name, policy, request)
        }
    }
}

actual suspend fun schedulePushResolutionWork(sessionId: String, roomId: String) {
    PushResolutionWorker.schedule(RevengeApplication.instance, sessionId, roomId)
}
