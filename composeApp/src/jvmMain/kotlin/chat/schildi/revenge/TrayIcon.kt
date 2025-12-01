package chat.schildi.revenge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.ApplicationScope
import com.kdroid.composetray.tray.api.Tray
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
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
    Tray(
        //icon = painterResource(Res.drawable.), // TODO
        icon = rememberVectorPainter(Icons.Default.Satellite),
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
