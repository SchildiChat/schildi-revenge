package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationState
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.config.keybindings.DestinationEnum
import java.util.UUID

private data class MultiPaneLayoutTarget(
    val destinationHolder: DestinationStateHolder,
    val focusId: UUID,
    val role: SplitRole,
)

@Composable
fun MultiPaneLayout(
    outerDestination: DestinationEnum,
    innerDestinations: List<DestinationStateHolder>,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val targets = innerDestinations.mapIndexed { index, holder ->
        key(holder.state.value.holderId) {
            MultiPaneLayoutTarget(
                destinationHolder = holder,
                focusId = rememberFocusId(),
                role = when {
                    innerDestinations.size == 1 -> SplitRole.Singular
                    index == 0 -> SplitRole.Start
                    index == innerDestinations.size - 1 -> SplitRole.End
                    else -> SplitRole.Horizontal
                }
            )
        }
    }
    val (outerModifier, innerContentModifier) = if (targets.size == 1) {
        Pair(modifier, contentModifier)
    } else {
        Pair(modifier.then(contentModifier), Modifier)
    }
    AdaptiveRow(
        outerModifier.fillMaxSize(),
    ) {
        targets.forEach { target ->
            SplitScreenDestination(
                focusId = target.focusId,
                splitRole = target.role,
                splitType = outerDestination,
                destinationHolder = target.destinationHolder,
                contentModifier = innerContentModifier,
            )
        }
    }
}

class MultiPaneLayoutDestinationStateHolderWrapper(
    val inner: DestinationStateHolder,
    val close: (() -> Unit)? = null,
    val interceptNavigation: (Destination) -> Boolean,
) : DestinationStateHolder {

    override val state = inner.state

    override fun replaceWith(destinationState: DestinationState) = inner.replaceWith(destinationState)
    override fun publishTitle(
        titleOverride: ComposableStringHolder?,
        verifyDestination: (Destination) -> Boolean
    ) = inner.publishTitle(
        titleOverride,
        verifyDestination,
    )

    override fun navigate(
        destination: Destination,
        preference: NavigationPreference,
        invalidateHolderId: Boolean,
        initialTitle: ComposableStringHolder?
    ) {
        // Intercept supported panes
        if (preference == NavigationPreference.AUTO || preference == NavigationPreference.REPLACE) {
            if (interceptNavigation(destination)) {
                return
            }
        }
        // Fallback to normal navigation behavior
        inner.navigate(
            destination,
            preference,
            invalidateHolderId,
            initialTitle,
        )
    }

    override fun closeScreen(keyHandler: KeyboardActionHandler) =
        close?.invoke() ?: inner.closeScreen(keyHandler)
}
