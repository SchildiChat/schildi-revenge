package chat.schildi.revenge.util.matrix

import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.toActionResult
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

suspend fun updateAccountData(
    client: MatrixClient,
    eventType: String,
    update: (JsonObject?) -> JsonObject?,
): ActionResult {
    val data = client.getAccountData(eventType)?.let {
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
        client.setAccountData(eventType, Json.encodeToString(updatedData)).toActionResult()
    }
}
