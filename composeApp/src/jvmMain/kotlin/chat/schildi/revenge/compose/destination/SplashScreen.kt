package chat.schildi.revenge.compose.destination

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.components.ScreenLoadProgressDetails
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.model.LoadState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    FocusContainer(
        modifier = modifier.fillMaxSize(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        SplashScreenContent(contentModifier, UiState.globalLoadState.state)
    }
}

@Composable
fun SplashScreenContent(
    modifier: Modifier = Modifier,
    loadState: StateFlow<LoadState>? = null,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Dimens.verticalArrangementBig,
    ) {
        CircularProgressIndicator()
        if (loadState != null) {
            ScreenLoadProgressDetails(loadState.collectAsState().value)
        }
    }
}
