package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.platformOpenFile
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.media.MediaDownloadRepo
import chat.schildi.revenge.util.MimeUtil
import coil3.compose.LocalPlatformContext
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.coroutines.Dispatchers

@Composable
fun clickToOpenImageAction(
    sessionId: SessionId?,
    mediaSource: MediaSource?,
    mimeType: String? = null,
    filename: String? = null,
    roomId: RoomId? = null,
    timestamp: Long = 0,
): InteractionAction? {
    sessionId ?: return null
    mediaSource ?: return null
    val actionContext = currentActionContext()
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    return InteractionAction.Invoke {
        actionContext.launchActionAsync(
            "viewImage/${mediaSource.safeUrl}",
            scope,
            Dispatchers.IO,
            "viewImage/${mediaSource.safeUrl}"
        ) {
            MediaDownloadRepo.requestAttachmentDownload(
                sessionId = sessionId,
                roomId = roomId,
                mediaSource = mediaSource,
                sourceTimestamp = timestamp,
                mimeType = mimeType,
                filename = filename,
            ).onSuccess {
                val effectiveMimetype = mimeType ?: MimeUtil.detectMimeType(it)
                platformOpenFile(platformContext, it, effectiveMimetype)
            }.toActionResult()
        } is ActionResult.Success
    }
}
