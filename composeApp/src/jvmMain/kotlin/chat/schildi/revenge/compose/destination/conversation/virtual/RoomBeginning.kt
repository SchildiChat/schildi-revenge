package chat.schildi.revenge.compose.destination.conversation.virtual

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.theme.LocalMessageStyle
import chat.schildi.theme.scExposures
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.event_placeholder_room_beginning

@Composable
fun RoomBeginning(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.event_placeholder_room_beginning),
            color = MaterialTheme.scExposures.accentColor,
            style = LocalMessageStyle.current.textStyle,
        )
    }
}
