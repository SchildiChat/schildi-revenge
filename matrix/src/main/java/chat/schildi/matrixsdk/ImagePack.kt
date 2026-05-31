package chat.schildi.matrixsdk

import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ImagePack(
    val images: Map<String, ImagePackImageWithRawInfo>,
    val pack: PackInfo?,
) {
    val supportsCustomEmoji = pack == null || pack.supportsCustomEmoji
    val supportsSticker = pack == null || pack.supportsSticker
}

@Serializable
data class PackInfo(
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val usage: List<String>? = null,
) {
    val supportsCustomEmoji = usage.isNullOrEmpty() || usage.contains("emoticon")
    val supportsSticker = usage.isNullOrEmpty() || usage.contains("sticker")
}

@Serializable
data class ImagePackImageWithRawInfo(
    val url: String,
    val body: String? = null,
    val info: JsonElement? = null,
)

@Serializable
data class ImagePackStateEventContent(
    @SerialName("state_key")
    val stateKey: String,
    val content: ImagePack?,
)

data class ImagePackSource(
    val roomId: RoomId,
    val stateKey: String,
)

data class ImagePackImageSource(
    val packSource: ImagePackSource,
    val info: PackInfo?,
) {
    val supportsCustomEmoji = info == null || info.supportsCustomEmoji
    val supportsSticker = info == null || info.supportsSticker
}

data class ImagePackWithSource(
    val pack: ImagePack,
    val source: ImagePackSource,
)
