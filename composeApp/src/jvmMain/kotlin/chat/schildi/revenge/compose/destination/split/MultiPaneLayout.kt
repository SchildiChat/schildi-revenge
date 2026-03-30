package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationState
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.config.keybindings.DestinationEnum
import java.util.UUID

private data class MultiPaneLayoutTarget(
    val destinationHolder: MultiPaneLayoutDestinationStateHolderWrapper,
    val focusId: UUID,
    val role: SplitRole,
)

@Composable
fun MultiPaneLayout(
    outerDestination: DestinationEnum,
    innerDestinations: List<MultiPaneLayoutDestinationStateHolderWrapper>,
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
                actionProvider = multiPaneKeyboardActionProvider(target.destinationHolder)
            )
        }
    }
}

class MultiPaneLayoutDestinationStateHolderWrapper(
    val parent: DestinationStateHolder?,
    val inner: DestinationStateHolder,
    val closeSplit: () -> ActionResult,
    val closeDestination: (DestinationEnum) -> ActionResult,
    val close: ((KeyboardActionHandler) -> Unit)? = null,
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
        // Fallback to parent navigation behavior if possible, so we'd rather replace the whole split
        // then having incompatible stuff in it
        (parent ?: inner).navigate(
            destination,
            preference,
            invalidateHolderId,
            initialTitle,
        )
    }

    override fun closeScreen(keyHandler: KeyboardActionHandler) =
        close?.invoke(keyHandler) ?: inner.closeScreen(keyHandler)

    // Need to make sure to allow navigating from inbox -> inbox to close destination pane or sth.
    override fun isNavigationDestinationApplicable(destination: Destination) =
        parent?.isNavigationDestinationApplicable(destination) != false
}

fun buildMultiPaneDestinationStateHolderWrapper(
    inner: DestinationStateHolder,
    parent: DestinationStateHolder?,
    isDetails: Boolean,
    accessDetails: () -> DestinationStateHolder,
    createPlaceholder: () -> Destination.MultiPanePlaceholder,
    mainDestination: DestinationEnum,
    allowedDetailsDestinations: List<DestinationEnum>,
    allowedDetailsCategories: List<DestinationCategory>,
) = MultiPaneLayoutDestinationStateHolderWrapper(
    parent = parent,
    inner = inner,
    closeSplit = {
        val details = accessDetails()
        if (accessDetails().state.value.destination is Destination.MultiPanePlaceholder) {
            ActionResult.Inapplicable
        } else {
            details.navigate(
                createPlaceholder(),
                NavigationPreference.REPLACE
            )
            ActionResult.Success()
        }
    },
    closeDestination = { toClose ->
        val details = accessDetails()
        val placeholder = createPlaceholder()
        if (toClose !in allowedDetailsDestinations || toClose == placeholder.type) {
            ActionResult.Inapplicable
        } else if (details.state.value.destination.type != toClose) {
            ActionResult.Inapplicable
        } else {
            details.navigate(
                placeholder,
                NavigationPreference.REPLACE
            )
            ActionResult.Success()
        }
    },
    close = if (isDetails) {
        {
            accessDetails().navigate(
                createPlaceholder(),
                NavigationPreference.REPLACE
            )
        }
    } else if (parent != null) {
        parent::closeScreen
    } else {
        null
    }
) { navDestination ->
    when {
        navDestination.type == mainDestination -> {
            accessDetails().navigate(
                createPlaceholder(),
                NavigationPreference.REPLACE
            )
            true
        }
        navDestination.type in allowedDetailsDestinations -> {
            accessDetails().navigate(navDestination)
            true
        }
        navDestination.category in allowedDetailsCategories -> {
            accessDetails().navigate(navDestination)
            true
        }
        else -> {
            false
        }
    }
}
