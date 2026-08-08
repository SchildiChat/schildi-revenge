package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.focus.FocusContainer
import org.jetbrains.compose.resources.painterResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.ic_launcher

@Composable
fun EmptyPaneScreen(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    FocusContainer(
        modifier = modifier.safeDrawingPadding(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Dimens.verticalArrangement,
            ) {
                Image(
                    painterResource(Res.drawable.ic_launcher),
                    null,
                    Modifier.size(48.dp),
                )
            }
        }
    }
}
