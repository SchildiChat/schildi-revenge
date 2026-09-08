package chat.schildi.matrixsdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val FULLY_READ_ACCOUNT_DATA_TYPE = "m.fully_read"

@Serializable
data class FullyReadContent(
    @SerialName("event_id")
    val eventId: String? = null,
)
