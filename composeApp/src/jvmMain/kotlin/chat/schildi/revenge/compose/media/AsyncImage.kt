package chat.schildi.revenge.compose.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.components.LocalSessionId
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.ui.media.MediaRequestData

@Composable
fun AsyncImage(
    model: MediaRequestData,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    sessionId: SessionId? = LocalSessionId.current,
) {
    coil3.compose.AsyncImage(
        model,
        contentDescription = contentDescription,
        onError = {
            Logger.withTag("AsyncImage").e("$it")
        },
        imageLoader = imageLoader(sessionId = sessionId),
        modifier = modifier,
    )
}
