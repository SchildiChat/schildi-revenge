package chat.schildi.revenge.util.matrix

import com.beeper.android.messageformat.MatrixPatterns
import com.beeper.android.messageformat.MatrixToLink
import io.ktor.http.decodeURLPart

object MatrixLinkPatterns {
    const val SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE = "schildichat://"
    const val SC_LEGACY_ROOM_LINK_PREFIX = "${SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE}room/"
    const val SC_LEGACY_USER_LINK_PREFIX = "${SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE}user/"
    const val SC_WEB_ROOM_LINK_PREFIX = "${SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE}vector/webapp/#/room/"
    const val SC_WEB_USER_LINK_PREFIX = "${SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE}vector/webapp/#/user/"

    fun parseMatrixLink(url: String): MatrixToLink? {
        return if (url.startsWith(SC_LEGACY_MATRIX_TO_CUSTOM_SCHEME_URL_BASE, ignoreCase = true)) {
            parseSchildiChatLegacyLink(url)
        } else {
            MatrixPatterns.parseMatrixLink(url, isAutoLink = true)
        }
    }

    fun parseSchildiChatLegacyLink(url: String): MatrixToLink? {
        return when {
            url.startsWith(SC_LEGACY_ROOM_LINK_PREFIX, ignoreCase = true) -> handleScLegacyRoomLink(SC_LEGACY_ROOM_LINK_PREFIX, url)
            url.startsWith(SC_LEGACY_USER_LINK_PREFIX, ignoreCase = true) -> handleScLegacyUserLink(SC_LEGACY_USER_LINK_PREFIX, url)
            url.startsWith(SC_WEB_ROOM_LINK_PREFIX, ignoreCase = true) -> handleScLegacyRoomLink(SC_WEB_ROOM_LINK_PREFIX, url)
            url.startsWith(SC_WEB_USER_LINK_PREFIX, ignoreCase = true) -> handleScLegacyUserLink(SC_WEB_USER_LINK_PREFIX, url)
            else -> null
        }
    }

    private fun handleScLegacyRoomLink(prefix: String, url: String): MatrixToLink.RoomLink? {
        val roomId = url.substring(prefix.length).substringBefore("?").decodeURLPart()
        return if (io.element.android.libraries.matrix.api.core.MatrixPatterns.isRoomId(roomId) ||
            io.element.android.libraries.matrix.api.core.MatrixPatterns.isRoomAlias(roomId)
        ) {
            val via = url.substringAfter("?").parameters("via")
            MatrixToLink.RoomLink(roomId, via, url)
        } else {
            null
        }
    }

    private fun handleScLegacyUserLink(prefix: String, url: String): MatrixToLink.UserMention? {
        val userId = url.substring(prefix.length).substringBefore("?").decodeURLPart()
        return if (io.element.android.libraries.matrix.api.core.MatrixPatterns.isUserId(userId)) {
            MatrixToLink.UserMention(userId, url, isAutoLink = true)
        } else {
            null
        }
    }

    private fun String.parameters(name: String): List<String>? {
        val values = split('&').mapNotNull { item ->
            if (item.isEmpty()) {
                return@mapNotNull null
            }
            val key = item.substringBefore('=')
            if (!key.equals(name, ignoreCase = true)) {
                return@mapNotNull null
            }
            item.substringAfter('=', missingDelimiterValue = "").decodeURLPart()
        }
        return values.ifEmpty { null }
    }
}
