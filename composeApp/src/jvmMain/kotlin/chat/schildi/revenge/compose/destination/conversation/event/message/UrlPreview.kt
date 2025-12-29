package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import chat.schildi.matrixsdk.urlpreview.UrlPreview
import chat.schildi.matrixsdk.urlpreview.UrlPreviewInfo
import chat.schildi.matrixsdk.urlpreview.UrlPreviewStateHolder
import chat.schildi.matrixsdk.urlpreview.UrlPreviewStateProvider
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.compose.media.imageLoader
import chat.schildi.revenge.compose.util.UrlUtil
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.core.data.tryOrNull
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.ui.media.MediaRequestData

import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

/**
 * Provides a [UrlPreviewStateProvider] to the composition.
 */
val LocalUrlPreviewStateProvider = staticCompositionLocalOf<UrlPreviewStateProvider?> { null }

@Composable
fun <T>T.takeIfUrlPreviewsEnabledForRoom(room: BaseRoom): T? {
    val allowed = if (room.info().isEncrypted != false) {
        ScPrefs.URL_PREVIEWS_IN_E2EE_ROOMS.value()
    } else {
        ScPrefs.URL_PREVIEWS.value()
    }
    return if (allowed) this else null
}

private fun String.isIpAddress(): Boolean {
    val host = URI(this).host ?: return false
    return try {
        val inet = InetAddress.getByName(host)
        // true if the input was already an IP literal (IPv4 or IPv6)
        inet.hostAddress == host
    } catch (_: UnknownHostException) {
        false
    }
}

private fun String.toPreviewableUrl(requireExplicitHttps: Boolean): String? {
    val url = when {
        "://" in this -> this
        requireExplicitHttps -> return null
        // There's some funny "tel:" linkifications of numbers that would match otherwise
        ":" in this -> return null
        else -> "https://$this"
    }
    val uri = tryOrNull { URI(url) } ?: return null
    val host = uri.host ?: return null
    // Message and room links shouldn't render an url preview
    if (host == "matrix.to") {
        return null
    }
    // Don't bother for non-http(s) schemes
    if (uri.scheme != "https" && (requireExplicitHttps || uri.scheme != "http")) {
        return null
    }
    // Don't bother for IP links
    if (host.isIpAddress()) {
        return null
    }
    return uri.toString()
}

private fun String.isAllowedUrlPrefix(): Boolean {
    // If we have a ":" right before the URL, it's probably just part of an mxid...
    // I also have some messages with slashes that I don't want here
    if (endsWith(":") || endsWith("/")) {
        return false
    }
    return true
}

@Composable
fun resolveUrlPreview(body: String): UrlPreviewInfo? {
    if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
        return null
    }
    // This will be null when url previews are disabled for this room
    val urlPreviewStateProvider = LocalUrlPreviewStateProvider.current ?: return null
    var previewStateHolder by remember { mutableStateOf<UrlPreviewStateHolder?>(null) }
    // Whether to only linkify links that explicitly contain "https://" at the beginning.
    val requireExplicitHttps = ScPrefs.URL_PREVIEWS_REQUIRE_EXPLICIT_LINKS.value()
    LaunchedEffect(body, requireExplicitHttps) {
        /* TODO from formatted body links
        val formattedBody = content.formattedBody as? Spanned
        if (formattedBody == null) {
            previewStateHolder = null
            return@LaunchedEffect
        }
        val urlSpans = formattedBody.getSpans<URLSpan>()
        if (urlSpans.isEmpty()) {
            previewStateHolder = null
            return@LaunchedEffect
        }
        val urls = formattedBody.getSpans<URLSpan>().mapNotNull { urlSpan ->
            val urlSpanStart = formattedBody.getSpanStart(urlSpan).takeIf { it != -1 } ?: return@mapNotNull null
            val urlSpanEnd = formattedBody.getSpanEnd(urlSpan).takeIf { it != -1 } ?: return@mapNotNull null
            if (formattedBody.getSpans<CustomMentionSpan>(urlSpanStart, urlSpanEnd).isNotEmpty() ||
                formattedBody.getSpans<MentionSpan>(urlSpanStart, urlSpanEnd).isNotEmpty()) {
                // Don't mind links in mentions
                return@mapNotNull null
            }
            if (!formattedBody.substring(0, urlSpanStart).isAllowedUrlPrefix()) {
                return@mapNotNull null
            }
            Pair(urlSpan, urlSpanStart)
        }.sortedBy {
            // Sort by link start position
            it.second
        }.mapNotNull { (urlSpan, _) ->
            urlSpan.url.toPreviewableUrl(requireExplicitHttps)
        }
         */
        val urls = UrlUtil.extractUrlsFromText(body)
        urls.firstOrNull()?.let { url ->
            previewStateHolder = urlPreviewStateProvider.getStateHolder(url)
        } ?: run {
            previewStateHolder = null
        }
    }
    LaunchedEffect(previewStateHolder) {
        previewStateHolder?.onRender()
    }
    return previewStateHolder?.let { stateHolder ->
        stateHolder.state.collectAsState().value?.let {
            UrlPreviewInfo(stateHolder.url, it)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UrlPreviewView(
    urlPreview: UrlPreview,
    paddingValues: PaddingValues = PaddingValues(bottom = Dimens.Conversation.replyItemPadding),
    modifier: Modifier = Modifier,
    onLongCLick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
        return
    }
    Column(
        modifier
            // Similar background design to InReplyToView
            .padding(paddingValues)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp)) // Add border so link previews look different from replies
            .combinedClickable(onClick = onClick, onLongClick = onLongCLick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val titleColumnHeight = remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        Row {
            urlPreview.imageUrl?.let { imageUrl ->
                SubcomposeAsyncImage(
                    modifier = Modifier.align(Alignment.Top),
                    imageLoader = imageLoader(),
                    model = MediaRequestData(MediaSource(imageUrl), MediaRequestData.Kind.Content),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    contentDescription = null,
                ) {
                    when (painter.state.collectAsState().value) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .sizeIn(
                                    maxWidth = 140.dp,
                                    maxHeight = max(80.dp, density.run { titleColumnHeight.intValue.toDp() }),
                                    minWidth = 16.dp,
                                    minHeight = 16.dp,
                                )
                                .padding(end = 4.dp, top = 4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        else -> {}
                    }
                }
            }
            Column(
                Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.CenterVertically)
                    .onGloballyPositioned { titleColumnHeight.intValue = it.size.height },
            ) {
                urlPreview.title?.let { title ->
                    Text(
                        text = title.trim(),
                        style = Dimens.Conversation.textMessageStyle,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                urlPreview.siteName?.let { site ->
                    Text(
                        text = site.trim(),
                        style = Dimens.Conversation.textMessageStyle,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        urlPreview.description?.let { description ->
            val sanitized = description.replace("\n\n", "\n").replace("\n", " ").trim()
            var expanded by remember { mutableStateOf(false) }
            Text(
                text = sanitized,
                style = Dimens.Conversation.textMessageStyle,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = if (expanded) 50 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .thenIf(urlPreview.imageUrl != null || urlPreview.title != null || urlPreview.siteName != null) { padding(top = 4.dp) }
                    .clip(RoundedCornerShape(4.dp))
                    .combinedClickable(onLongClick = onLongCLick) { expanded = !expanded }
                    .padding(horizontal = 4.dp),
            )
        }
    }
}
