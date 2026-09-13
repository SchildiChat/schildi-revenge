package chat.schildi.revenge.preferences

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.config.RevengeDatastoreStorage
import chat.schildi.revenge.config.ScAppDirs
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Local, per-room timeline display overrides. These are not synced anywhere and only affect this
 * client - useful e.g. for bridged rooms that don't advertise themselves as such via `m.bridge`
 * state events (many classic IRC bridges don't), so the general "hide in bridged rooms" setting
 * can't auto-detect them.
 */
interface RoomTimelinePreferencesStore {
    fun hideMembershipEventsFlow(roomId: RoomId): Flow<Boolean>
    suspend fun setHideMembershipEvents(roomId: RoomId, hide: Boolean)
}

class DefaultRoomTimelinePreferencesStore : RoomTimelinePreferencesStore {
    private val dataDir = File(ScAppDirs.getUserConfigDir()).also {
        it.mkdirs()
    }
    private val storeFile = File(dataDir, "room_timeline_preferences.toml")
    private val store = DataStoreFactory.create(
        RevengeDatastoreStorage(
            storeFile.path,
            ScCoroutines.scope(Dispatchers.IO, "RoomTimelinePrefStore"),
        )
    )

    private fun hideMembershipEventsKey(roomId: RoomId) = booleanPreferencesKey("hide_membership_events_${roomId.value}")

    override fun hideMembershipEventsFlow(roomId: RoomId): Flow<Boolean> {
        val key = hideMembershipEventsKey(roomId)
        return store.data.map { prefs: Preferences -> prefs[key] ?: false }
    }

    override suspend fun setHideMembershipEvents(roomId: RoomId, hide: Boolean) {
        val key = hideMembershipEventsKey(roomId)
        store.edit { prefs ->
            if (hide) {
                prefs[key] = true
            } else {
                prefs.remove(key)
            }
        }
    }
}

val RevengeRoomTimelinePrefs: RoomTimelinePreferencesStore = DefaultRoomTimelinePreferencesStore()
