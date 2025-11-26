package chat.schildi.revenge.glue

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.sessionstorage.api.observer.SessionListener
import io.element.android.libraries.sessionstorage.api.observer.SessionObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
object RevengeSessionObserver : SessionObserver {
    private val listenerLock = Mutex()
    private val listeners = mutableListOf<SessionListener>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        RevengeSessionStore.sessionUpdatesFlow
            .onEach { update ->
                when (update) {
                    is SessionUpdate.OnSessionCreated -> listeners.forEach { listener ->
                        listener.onSessionCreated(update.userId)
                    }
                    is SessionUpdate.OnSessionDeleted -> listeners.forEach { listener ->
                        listener.onSessionDeleted(update.userId, update.wasLastSession)
                    }
                }
            }.launchIn(scope)
    }

    override fun addListener(listener: SessionListener) {
        runBlocking {
            listenerLock.withLock {
                if (listener !in listeners) {
                    listeners += listener
                }
            }
        }
    }

    override fun removeListener(listener: SessionListener) {
        runBlocking {
            listenerLock.withLock {
                listeners.remove(listener)
            }
        }
    }
}
