package chat.schildi.revenge

import kotlin.time.Duration

object DurationFormat {
    fun formatMediaDuration(duration: Duration) = buildString {
        val hours = duration.inWholeHours
        if (hours > 0) {
            append(hours)
            append(":")
            append(duration.inWholeMinutes.toString().padStart(2, '0'))
        } else {
            append(duration.inWholeMinutes)
        }
        append(":")
        append((duration.inWholeSeconds % 60).toString().padStart(2, '0'))
    }
}
