package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.media.onAsyncImageError
import coil3.compose.AsyncImage
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.matrix.ui.media.MediaRequestData.Kind.Thumbnail

@Composable
fun ImageMessage(image: ImageMessageType, modifier: Modifier = Modifier) {
    // TODO caption, placeholder, ...
    AsyncImage(
        MediaRequestData(image.source, Thumbnail(1000)),
        null,
        imageLoader = imageLoader(),
        onError = ::onAsyncImageError,
        modifier = modifier
            .sizeIn(
                minWidth = Dimens.Conversation.imageMinWidth,
                minHeight = Dimens.Conversation.imageMinHeight,
                maxWidth = Dimens.Conversation.imageMaxWidth,
                maxHeight = Dimens.Conversation.imageMaxHeight,
            )
            .clip(Dimens.Conversation.messageBubbleShape),
    )
}
