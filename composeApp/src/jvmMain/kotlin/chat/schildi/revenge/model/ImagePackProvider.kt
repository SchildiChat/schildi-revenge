package chat.schildi.revenge.model

import chat.schildi.matrixsdk.ImagePackSource
import chat.schildi.matrixsdk.ImagePackStateEventContent
import chat.schildi.matrixsdk.ImagePackWithSource
import chat.schildi.revenge.UiState
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePackProvider(
    private val sessionId: SessionId,
    private val roomId: RoomId?,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("ImagePackProvider/$sessionId/$roomId")
    val client = UiState.selectClient(sessionId, scope)
    private val json = Json { ignoreUnknownKeys = true }

    // TODO include space parents
    val roomIds = flowOf(listOfNotNull(roomId))

    val imagePacks = combine(
        client,
        roomIds
    ) { a, b -> a to b }.flatMapLatest { (client, roomIds) ->
        client?.getImagePackFlow(roomIds) ?: flowOf(null)
    }.map { rawPacks ->
        rawPacks?.mapNotNull { rawPack ->
            try {
                val state = json.decodeFromString<ImagePackStateEventContent>(rawPack.raw)
                state.content?.let { pack ->
                    ImagePackWithSource(
                        pack = pack,
                        source = ImagePackSource(RoomId(rawPack.roomId), state.stateKey)
                    )
                }
            } catch (e: Exception) {
                log.e("Failed to deserialize image pack: $e; raw JSON: ${rawPack.raw}")
                null
            }
        }
    }
}
