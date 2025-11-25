package chat.schildi.revenge.util

import kotlinx.serialization.json.Json

val ScJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}
