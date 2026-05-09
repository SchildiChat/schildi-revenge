package chat.schildi.revenge.glue

import chat.schildi.revenge.sessionstorage.migration.SessionDataV1
import chat.schildi.revenge.util.ScJson
import co.touchlab.kermit.Logger
import io.element.android.libraries.sessionstorage.api.SessionData
import kotlinx.serialization.Serializable

private val log = Logger.withTag("RevengeSessionStore")

@Serializable
internal data class RevengeSessionStoreData(
    val sessions: List<SessionData> = emptyList(),
) {
    companion object {
        fun fromSerializedWithMigration(serialized: String): RevengeSessionStoreData {
            return try {
                ScJson.decodeFromString<RevengeSessionStoreData>(serialized)
            } catch (e: Exception) {
                log.i("Failed to decode session data ($e), trying migration V1")
                RevengeSessionStoreDataV1.fromSerializedWithMigration(serialized).upgrade()
            }
        }
    }
}

/**
 * Migration from pre-26.05.0 merge
 */
@Serializable
internal data class RevengeSessionStoreDataV1(
    val sessions: List<SessionDataV1> = emptyList(),
) {
    fun upgrade() = RevengeSessionStoreData(
        sessions = sessions.map {
            SessionData(
                userId = it.userId,
                deviceId = it.deviceId,
                accessToken = it.accessToken,
                refreshToken = it.refreshToken,
                homeserverUrl = it.homeserverUrl,
                oAuthData = it.oidcData,
                loginTimestamp = it.loginTimestamp,
                isTokenValid = it.isTokenValid,
                loginType = it.loginType,
                passphrase = it.passphrase,
                sessionPath = it.sessionPath,
                cachePath = it.cachePath,
                position = it.position,
                lastUsageIndex = it.lastUsageIndex,
                userDisplayName = it.userDisplayName,
                userAvatarUrl = it.userAvatarUrl,
            )
        }
    )

    companion object {
        fun fromSerializedWithMigration(serialized: String): RevengeSessionStoreDataV1 {
            // Oldest variant, no fallbacks exist. If this fails, it fails.
            return ScJson.decodeFromString<RevengeSessionStoreDataV1>(serialized)
        }
    }
}
