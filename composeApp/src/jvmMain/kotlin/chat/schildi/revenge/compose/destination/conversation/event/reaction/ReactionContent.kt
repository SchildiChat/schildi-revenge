package chat.schildi.revenge.compose.destination.conversation.event.reaction

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.containsOnlyEmojis
import chat.schildi.theme.rememberEmojiFontFamily
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun ReactionContent(
    reaction: String,
    reactionShortcode: String? = null,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    val isRealEmojiReaction = remember(reaction) {
        reaction.containsOnlyEmojis()
    }
    val reactionTextStyle = if (isRealEmojiReaction) {
        baseStyle.merge(fontFamily = rememberEmojiFontFamily())
    } else {
        baseStyle
    }
    if (reaction.startsWith("mxc://")) {
        SubcomposeAsyncImage(
            model = MediaRequestData(MediaSource(reaction), MediaRequestData.Kind.Content),
            modifier = modifier.height(
                LocalDensity.current.run {
                    MaterialTheme.typography.bodyMedium.lineHeight.toDp()
                }
            ),
            imageLoader = imageLoader(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            contentDescription = reactionShortcode ?: reaction,
        ) {
            when (painter.state.collectAsState().value) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                else -> {
                    Text(
                        (reactionShortcode ?: reaction).take(Dimens.Conversation.reactionMaxLength),
                        color = MaterialTheme.colorScheme.primary,
                        style = reactionTextStyle,
                    )
                }
            }
        }
    } else {
        Text(
            reaction.take(Dimens.Conversation.reactionMaxLength),
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            style = reactionTextStyle,
        )
    }
}
