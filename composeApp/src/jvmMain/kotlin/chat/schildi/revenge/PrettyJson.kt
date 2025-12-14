package chat.schildi.revenge

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun String.toPrettyJson(): String {
    return try {
        PrettyJson.encodeToString(
            PrettyJson.parseToJsonElement(this)
        )
    } catch (_: SerializationException) {
        this
    }
}
