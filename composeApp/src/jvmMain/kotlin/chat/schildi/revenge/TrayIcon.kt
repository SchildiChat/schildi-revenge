package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.ApplicationScope
import chat.schildi.theme.ScColors
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
    val titleShow = stringResource(Res.string.tray_show)
    val titleMinimize = stringResource(Res.string.tray_minimize)
    val titleExit = stringResource(Res.string.tray_exit)
    // Hardcoding something that works both dark and light,
    // rather than using themed values, is probably better for tray icon badges
    val badgeColor = ScColors.colorAccentRed
    // TODO icon needs some work
    val icon = imageResource(Res.drawable.ic_launcher)
    Tray(
        icon = remember {
            IconWithBadgePainter(
                icon,
                // TODO actual unread count
                0,
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
    val unreadCount: Int,
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
            val radius = size.minDimension * 0.18f
            val center = Offset(size.width - radius, size.height - radius)
            drawCircle(badgeColor, radius, center)
            // TODO drawing text is not straightforward on multiplatform apparently, do I even need it?
        }
    }
}

