package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.focus.FocusContainer

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    FocusContainer(
        modifier = modifier.fillMaxSize(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        SplashScreenContent(contentModifier)
    }
}

@Composable
fun SplashScreenContent(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier)
}
