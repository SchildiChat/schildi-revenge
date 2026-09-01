package chat.schildi.revenge.util.matrix

import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.toActionResult
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

suspend fun MatrixClient.updateAccountData(
    eventType: String,
    update: (JsonObject?) -> JsonObject?,
): ActionResult {
    val data = getAccountData(eventType).getOrNull()?.let {
        try {
            Json.parseToJsonElement(it).jsonObject
        } catch (e: Exception) {
            return ActionResult.Failure("Failed to parse account data $eventType: $e")
        }
    }
    val updatedData = try {
       update(data)
    } catch (e: Exception) {
        return ActionResult.Failure("Failed to update account data $eventType: $e")
    }
    return if (updatedData == null || updatedData == data) {
        ActionResult.Inapplicable
    } else {
        setAccountData(eventType, Json.encodeToString(updatedData)).toActionResult()
    }
}

suspend fun MatrixClient.updateRoomAccountData(
    roomId: RoomId,
    eventType: String,
    update: (JsonObject?) -> JsonObject?,
): ActionResult {
    val data = getRoomAccountData(roomId, eventType).getOrNull()?.let {
        try {
            Json.parseToJsonElement(it).jsonObject
        } catch (e: Exception) {
            return ActionResult.Failure("Failed to parse room account data $eventType: $e")
        }
    }
    val updatedData = try {
        update(data)
    } catch (e: Exception) {
        return ActionResult.Failure("Failed to update room account data $eventType: $e")
    }
    return if (updatedData == null || updatedData == data) {
        ActionResult.Inapplicable
    } else {
        setRoomAccountData(roomId, eventType, Json.encodeToString(updatedData)).toActionResult()
    }
}

suspend fun JoinedRoom.updateRoomState(
    eventType: String,
    stateKey: String,
    update: (JsonObject?) -> JsonObject?,
): ActionResult {
    val stateResult = getRawState(eventType, stateKey)
    if (stateResult.isFailure) {
        return stateResult.toActionResult()
    }
    val data = stateResult.getOrNull()?.let {
        try {
            Json.parseToJsonElement(it).jsonObject["content"]!!.jsonObject
        } catch (e: Exception) {
            return ActionResult.Failure("Failed to parse room state data $eventType: $e")
        }
    }
    val updatedData = try {
        update(data)
    } catch (e: Exception) {
        return ActionResult.Failure("Failed to update room state data $eventType: $e")
    }
    return if (updatedData == null || updatedData == data) {
        ActionResult.Inapplicable
    } else {
        sendRawState(eventType, stateKey, Json.encodeToString(updatedData)).toActionResult()
    }
}
