package chat.schildi.revenge.push

/**
 * Schedules a push resolution worker for the given (session, room) scope, if needed.
 *
 * Push resolution involves calling [PushNotificationHandler.resolvePendingPushes] in a manner that makes sense for
 * the underlying operating system (including network-based scheduling and retries if possible).
 */
expect suspend fun schedulePushResolutionWork(sessionId: String, roomId: String)
