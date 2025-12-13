package chat.schildi.revenge.compose.destination

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.focus.FocusContainer

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    FocusContainer(modifier = modifier) {
        CircularProgressIndicator()
    }
}
