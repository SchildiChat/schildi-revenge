package chat.schildi.revenge.model

import chat.schildi.revenge.util.MediaInfoUtil
import chat.schildi.revenge.util.MimeUtil
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.media.ThumbnailInfo
import io.element.android.libraries.matrix.api.media.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Build a composer [Attachment] for a local file, probing it for mime type,
 * dimensions, duration and thumbnails. Returns null if the file does not exist.
 */
suspend fun buildAttachmentForFile(
    file: File,
    mimeType: String? = null,
    isFileAppOwned: Boolean = false,
): Attachment? = withContext(Dispatchers.IO) {
    if (!file.exists()) {
        return@withContext null
    }
    val resolvedMimeType = mimeType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        ?.takeUnless { it.isEmpty() || it == "application/octet-stream" }
        ?: MimeUtil.detectMimeType(file)
    val attachmentType = MimeUtil.classifyFromMime(resolvedMimeType)
    val fileSize = file.length()
    when (attachmentType) {
        MimeUtil.AttachmentKind.IMAGE -> {
            val measures = MediaInfoUtil.probeImage(file)
            val blurhash = MediaInfoUtil.generateImageBlurHash(file)
            Attachment.Image(
                file = file,
                thumbnail = null, // TODO?
                imageInfo = ImageInfo(
                    height = measures.height?.toLong(),
                    width = measures.width?.toLong(),
                    mimetype = resolvedMimeType,
                    size = fileSize,
                    thumbnailInfo = null,
                    thumbnailSource = null,
                    blurhash = blurhash,
                ),
                isFileAppOwned = isFileAppOwned,
            )
        }
        MimeUtil.AttachmentKind.VIDEO -> {
            val thumbnail = MediaInfoUtil.generateVideoThumbnail(file)
            val blurhash = thumbnail?.let { MediaInfoUtil.generateImageBlurHash(it.thumbnail.data) }
            val thumbnailSize = thumbnail?.thumbnail?.data?.size?.toLong()
            Attachment.Video(
                file = file,
                thumbnail = thumbnail?.thumbnail,
                videoInfo = VideoInfo(
                    duration = thumbnail?.videoMeasures?.durationMs?.milliseconds,
                    height = thumbnail?.videoMeasures?.height?.toLong(),
                    width = thumbnail?.videoMeasures?.width?.toLong(),
                    mimetype = resolvedMimeType,
                    size = fileSize,
                    thumbnailInfo = thumbnail?.thumbnailMeasures?.let {
                        ThumbnailInfo(
                            height = it.height?.toLong(),
                            width = it.width?.toLong(),
                            mimetype = "image/jpeg",
                            size = thumbnailSize,
                        )
                    },
                    thumbnailSource = null,
                    blurhash = blurhash,
                ),
                isFileAppOwned = isFileAppOwned,
            )
        }
        MimeUtil.AttachmentKind.AUDIO -> {
            val measures = MediaInfoUtil.probeAudio(file)
            Attachment.Audio(
                file,
                AudioInfo(
                    duration = measures.durationMs?.milliseconds,
                    size = fileSize,
                    mimetype = resolvedMimeType,
                ),
                isFileAppOwned = isFileAppOwned,
            )
        }
        MimeUtil.AttachmentKind.OTHER -> {
            Attachment.Generic(
                file,
                FileInfo(
                    mimetype = resolvedMimeType,
                    size = fileSize,
                    thumbnailInfo = null,
                    thumbnailSource = null,
                ),
                isFileAppOwned = isFileAppOwned,
            )
        }
    }
}
