package chat.schildi.revenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.ApplicationScope
import chat.schildi.revenge.model.GlobalUnreadCountsSource
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.theme.scExposures
import com.kdroid.composetray.tray.api.Tray
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.ic_launcher
import shire.composeapp.generated.resources.tray_exit
import shire.composeapp.generated.resources.tray_minimize
import shire.composeapp.generated.resources.tray_show

@Composable
fun ApplicationScope.TrayIcon(
    isMinimized: Boolean,
    setMinimized: (Boolean) -> Unit,
) {
    val unreadCounts = GlobalUnreadCountsSource.globalUnreadCounts
        .collectAsState(SpaceAggregationDataSource.SpaceUnreadCounts()).value
    TrayIcon(
        isMinimized = isMinimized,
        setMinimized = setMinimized,
        unreadCounts = unreadCounts,
    )
}

@Composable
fun ApplicationScope.TrayIcon(
    isMinimized: Boolean,
    setMinimized: (Boolean) -> Unit,
    unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts,
) {
    val titleShow = stringResource(Res.string.tray_show)
    val titleMinimize = stringResource(Res.string.tray_minimize)
    val titleExit = stringResource(Res.string.tray_exit)
    // Hardcoding something that works both dark and light,
    // rather than using themed values, is probably better for tray icon badges
    val badgeColor = MaterialTheme.scExposures.mentionBadgeColor
    // TODO icon needs some work
    val icon = imageResource(Res.drawable.ic_launcher)
    Tray(
        icon = remember(unreadCounts.notifiedChats, badgeColor) {
            IconWithBadgePainter(
                icon,
                unreadCounts.notifiedChats,
                badgeColor,
            )
        },
        tooltip = stringResource(Res.string.app_title)
    ) {
        if (isMinimized) {
            Item(label = titleShow) {
                setMinimized(false)
            }
        } else {
            Item(label = titleMinimize) {
                setMinimized(true)
            }
        }

        Divider()

        Item(label = titleExit) {
            exitApplication()
        }
    }
}

class IconWithBadgePainter(
    val icon: ImageBitmap,
    val unreadCount: Long,
    val badgeColor: Color,
) : Painter() {
    override val intrinsicSize = Size.Unspecified //IntSize(icon.width, icon.height).toSize()

    override fun DrawScope.onDraw() {
        drawImage(
            image = icon,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(icon.width, icon.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
        if (unreadCount > 0) {
            // Draw badge circle
            val radius = size.minDimension * 0.3f
            val badgeCenter = Offset(size.width - radius, size.height - radius)
            drawCircle(badgeColor, radius, badgeCenter)

            // Draw unread count text inside the badge (JVM/Skia)
            // Keep it compact and readable; cap at 999+
            val text = when {
                unreadCount > 999 -> "999+"
                else -> unreadCount.toString()
            }

            drawIntoCanvas { canvas ->
                // Use Skia directly for text drawing
                val skiaCanvas = canvas.nativeCanvas
                val paint = org.jetbrains.skia.Paint().apply {
                    isAntiAlias = true
                    color = Color.White.toArgb()
                }

                // Font size proportional to badge radius
                val fontSize = radius * 1.75f
                // Use default system typeface
                val font = org.jetbrains.skia.Font(null, fontSize)

                // Measure text to center horizontally using TextLine for reliable width
                val line = org.jetbrains.skia.TextLine.make(text, font)
                val textWidth: Float = line.width
                // Center vertically around badge center using ascent/descent
                val baselineY = badgeCenter.y + (fontSize / 3f) // TODO it should be 2f but that looks off, how portable is it right now?
                val startX = badgeCenter.x - (textWidth / 2f)

                skiaCanvas.drawTextLine(line, startX, baselineY, paint)
            }
        }
    }
}

