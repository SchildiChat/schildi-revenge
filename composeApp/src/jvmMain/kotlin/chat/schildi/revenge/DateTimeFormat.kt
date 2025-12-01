package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.datetime_today
import shire.composeapp.generated.resources.datetime_yesterday
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeFormat {

    fun timestampToDateTime(timestamp: Long) =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

    fun formatDate(
        dateTime: LocalDateTime,
        strings: DateTimeStrings,
        now: LocalDateTime = LocalDateTime.now(),
    ): String {
        return if (now.year == dateTime.year) {
            if (now.dayOfYear == dateTime.dayOfYear) {
                strings.today
            } else if (now.dayOfYear == dateTime.dayOfYear + 1) {
                strings.yesterday
            } else {
                DateTimeFormatter.ofPattern("EEE, MMM d").format(dateTime)
            }
        } else {
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy").format(dateTime)
        }
    }

    fun formatTime(dateTime: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
    }

    fun formatDateOrTime(
        dateTime: LocalDateTime,
        strings: DateTimeStrings,
        now: LocalDateTime = LocalDateTime.now(),
    ): String {
        return if (now.year == dateTime.year && now.dayOfYear == dateTime.dayOfYear) {
            formatTime(dateTime)
        } else {
            formatDate(dateTime, strings, now)
        }
    }

    @Composable
    fun dateTimeStrings() = DateTimeStrings(
        today = stringResource(Res.string.datetime_today),
        yesterday = stringResource(Res.string.datetime_yesterday),
    )

    @Composable
    fun formatTimestampAsDate(timestamp: Long): String {
        val strings = dateTimeStrings()
        return remember(timestamp) {
            formatDate(timestampToDateTime(timestamp), strings)
        }
    }

    @Composable
    fun formatTimestampAsDateOrTime(timestamp: Long): String {
        val strings = dateTimeStrings()
        return remember(timestamp) {
            formatDateOrTime(timestampToDateTime(timestamp), strings)
        }
    }
}

data class DateTimeStrings(
    val today: String,
    val yesterday: String,
)
