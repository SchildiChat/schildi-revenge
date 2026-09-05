package chat.schildi.revenge.compose.destination.split

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chat.schildi.lib.preferences.ScBoolPref
import chat.schildi.revenge.DefaultDestinationStateHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationCategory
import chat.schildi.revenge.DestinationState
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.preferences.value
import kotlin.uuid.Uuid

private data class MultiPaneLayoutTarget(
    val destinationHolder: MultiPaneLayoutDestinationStateHolderWrapper,
    val focusId: Uuid,
    val role: SplitRole,
)

data class MultiPaneMeta(
    val childIndex: Int,
    val parent: DestinationEnum,
    val parentMeta: MultiPaneMeta?,
)
val LocalMultiPaneMeta = compositionLocalOf<MultiPaneMeta?> { null }

@Composable
fun requireSinglePaneLayout(
    multiPaneType: DestinationEnum,
    pref: ScBoolPref,
    toMultiPaneDestination: (DestinationStateHolder) -> Destination,
): Boolean {
    if (LocalMultiPaneMeta.current?.parent != multiPaneType) {
        val preferMultiPane = pref.value()
        if (preferMultiPane) {
            val destinationState = LocalDestinationState.current
            SideEffect(destinationState) {
                destinationState ?: return@SideEffect
                val childState = destinationState.state.value
                destinationState.replaceWith(
                    DestinationState(
                        holderId = Uuid.random(),
                        destination = toMultiPaneDestination(DefaultDestinationStateHolder(childState)),
                    )
                )
            }
            return true
        }
    }
    return false
}

@Composable
fun requireMultiPaneLayout(
    pref: ScBoolPref,
    toSinglePaneDestination: () -> DestinationState,
): Boolean {
    val preferMultiPane = pref.value()
    if (!preferMultiPane) {
        val destinationState = LocalDestinationState.current
        SideEffect(destinationState) {
            destinationState?.replaceWith(toSinglePaneDestination())
        }
        return true
    }
    return false
}

@Composable
fun MultiPaneLayout(
    outerDestination: DestinationEnum,
    innerDestinations: List<MultiPaneLayoutDestinationStateHolderWrapper>,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val (outerModifier, innerContentModifier) = if (innerDestinations.size == 1) {
        Pair(modifier, contentModifier)
    } else {
        Pair(modifier.then(contentModifier), Modifier)
    }
    AdaptiveRow(
        outerModifier.thenIf(innerDestinations.size > 1) { safeDrawingPadding() }.fillMaxSize(),
    ) {
        innerDestinations.forEachIndexed { index, destinationHolder ->
            key(destinationHolder.state.collectAsState().value.holderId) {
                val focusId = rememberFocusId()
                val target = remember(destinationHolder, focusId, index, innerDestinations.size) {
                    MultiPaneLayoutTarget(
                        destinationHolder = destinationHolder,
                        focusId = focusId,
                        role = when {
                            innerDestinations.size == 1 -> SplitRole.Singular
                            index == 0 -> SplitRole.Start
                            index == innerDestinations.size - 1 -> SplitRole.End
                            else -> SplitRole.Horizontal
                        }
                    )
                }
                SplitScreenDestination(
                    focusId = target.focusId,
                    splitRole = target.role,
                    splitType = outerDestination,
                    destinationHolder = target.destinationHolder,
                    contentModifier = innerContentModifier,
                    actionProvider = multiPaneKeyboardActionProvider(target.destinationHolder),
                    meta = MultiPaneMeta(
                        childIndex = index,
                        parent = outerDestination,
                        parentMeta = LocalMultiPaneMeta.current,
                    ),
                )
            }
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
    accessMain: () -> DestinationStateHolder,
    accessDetails: () -> DestinationStateHolder,
    createPlaceholder: () -> Destination.MultiPanePlaceholder,
    mainDestination: DestinationEnum,
    allowedDetailsDestinations: List<DestinationEnum>,
    allowedDetailsCategories: List<DestinationCategory>,
    isCompatibleDetails: (Destination) -> Boolean = { true },
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
        if (toClose !in allowedDetailsDestinations || toClose == placeholder.destinationId) {
            ActionResult.Inapplicable
        } else if (details.state.value.destination.destinationId != toClose) {
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
        navDestination.destinationId == mainDestination -> {
            val main = accessMain()
            if (main.state.value.destination != navDestination) {
                main.navigate(navDestination, NavigationPreference.REPLACE)
            }
            accessDetails().navigate(
                createPlaceholder(),
                NavigationPreference.REPLACE
            )
            true
        }
        navDestination !is Destination.MultiPanePlaceholder && !isCompatibleDetails(navDestination) -> {
            false
        }
        navDestination.destinationId in allowedDetailsDestinations -> {
            accessDetails().navigate(navDestination, NavigationPreference.REPLACE)
            true
        }
        navDestination.category in allowedDetailsCategories -> {
            accessDetails().navigate(navDestination, NavigationPreference.REPLACE)
            true
        }
        else -> {
            false
        }
    }
}
