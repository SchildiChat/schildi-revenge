package chat.schildi.revenge.compose.destination.split

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.compose.DestinationContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.compose.components.AdaptiveColumn
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.FlatFocusContainer
import chat.schildi.revenge.compose.focus.rememberFocusId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.uuid.Uuid

enum class SplitRole {
    Start,
    End,
    Top,
    Bottom,
    Singular,
    Horizontal,
    Vertical,
}

@Composable
fun SplitHorizontal(
    destination: Destination.SplitHorizontal,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    AdaptiveRow(
        modifier.safeDrawingPadding().then(contentModifier).fillMaxSize(),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        PublishSplitDestinationTitle(
            destination,
            persistentListOf(primaryFocusId to destination.primary, secondaryFocusId to destination.secondary),
        )
        SplitScreenDestination(
            focusId = primaryFocusId,
            splitRole = SplitRole.Start,
            destinationHolder = destination.primary,
            actionProvider = splitKeyboardActionProvider(true),
            meta = MultiPaneMeta(
                childIndex = 0,
                parent = destination.destinationId,
                parentMeta = LocalMultiPaneMeta.current,
            ),
        )
        SplitScreenDestination(
            focusId = secondaryFocusId,
            splitRole = SplitRole.End,
            destinationHolder = destination.secondary,
            actionProvider = splitKeyboardActionProvider(false),
            meta = MultiPaneMeta(
                childIndex = 1,
                parent = destination.destinationId,
                parentMeta = LocalMultiPaneMeta.current,
            ),
        )
    }
}

@Composable
fun SplitVertical(
    destination: Destination.SplitVertical,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    AdaptiveColumn(
        modifier.safeDrawingPadding().then(contentModifier).fillMaxSize(),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        PublishSplitDestinationTitle(
            destination,
            persistentListOf(primaryFocusId to destination.primary, secondaryFocusId to destination.secondary),
        )
        SplitScreenDestination(
            focusId = primaryFocusId,
            splitRole = SplitRole.Top,
            destinationHolder = destination.primary,
            actionProvider = splitKeyboardActionProvider(true),
            meta = MultiPaneMeta(
                childIndex = 0,
                parent = destination.destinationId,
                parentMeta = LocalMultiPaneMeta.current,
            ),
        )
        SplitScreenDestination(
            focusId = secondaryFocusId,
            splitRole = SplitRole.Bottom,
            destinationHolder = destination.secondary,
            actionProvider = splitKeyboardActionProvider(false),
            meta = MultiPaneMeta(
                childIndex = 1,
                parent = destination.destinationId,
                parentMeta = LocalMultiPaneMeta.current,
            ),
        )
    }
}

@Composable
fun SplitScreenDestination(
    focusId: Uuid,
    splitRole: SplitRole,
    destinationHolder: DestinationStateHolder,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    actionProvider: HierarchicalKeyboardActionProvider,
    meta: MultiPaneMeta,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusedContainers = keyHandler.currentFocusedNestingDestinations.collectAsState(persistentListOf())
    val isActive = focusedContainers.value.firstOrNull() == focusId
    // Actual content
    FlatFocusContainer(
        LocalKeyboardActionProvider provides actionProvider,
        LocalMultiPaneMeta provides meta,
        role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
        focusId = focusId,
        modifier = modifier.splitContainerDecoration(
            isActive,
            splitRole,
        ),
    ) { modifier ->
        DestinationContent(destinationHolder, modifier.then(contentModifier))
    }
}

@Composable
fun PublishSplitDestinationTitle(
    destination: Destination,
    panes: ImmutableList<Pair<Uuid, DestinationStateHolder>>,
    preferredTitleDestinationIndex: Int = 0,
) {
    val parent = LocalDestinationState.current
    val hasPublishedTitle = remember(parent, destination) { mutableStateOf(false) }
    val focused = LocalKeyboardActionHandler.current.currentFocusedNestingDestinations
        .collectAsState(persistentListOf()).value
    val titles = panes.map { (focusId, holder) ->
        val state = holder.state.collectAsState().value
        focusId to (state.titleOverride ?: state.destination.title)
    }
    val focusedTitle = titles.firstOrNull { (focusId, _) -> focusId in focused }?.second
    val title = focusedTitle
        ?: titles.getOrNull(preferredTitleDestinationIndex)?.second
        ?: titles.firstNotNullOfOrNull { it.second }
    LaunchedEffect(parent, destination, title) {
        if (title != null) hasPublishedTitle.value = true
        if (hasPublishedTitle.value) {
            parent?.publishTitle(title) { it == destination }
        }
    }
}

@Composable
private fun Modifier.splitContainerDecoration(
    isActive: Boolean,
    splitRole: SplitRole,
): Modifier {
    if (splitRole == SplitRole.Singular) {
        return this
    }
    val padding = Dimens.Split.outlineWidth
    val halfPadding = padding / 2
    val paddingValues = when (splitRole) {
        SplitRole.Start -> PaddingValues(start = padding, top = padding, end = halfPadding, bottom = padding)
        SplitRole.End -> PaddingValues(start = halfPadding, top = padding, end = padding, bottom = padding)
        SplitRole.Top -> PaddingValues(start = padding, top = padding, end = padding, bottom = halfPadding)
        SplitRole.Bottom -> PaddingValues(start = padding, top = halfPadding, end = padding, bottom = padding)
        SplitRole.Horizontal -> PaddingValues(horizontal = halfPadding, vertical = padding)
        SplitRole.Vertical -> PaddingValues(horizontal = padding, vertical = halfPadding)
    }
    val keyHandler = LocalKeyboardActionHandler.current
    val renderActive = (keyHandler.keyboardPrimary.collectAsState().value || ScPrefs.FOCUS_FOLLOWS_MOUSE.value()) &&
            keyHandler.isWindowsFocused.collectAsState().value
    val outline = animateColorAsState(
        if (isActive && renderActive) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    ).value
    return border(color = outline, width = Dimens.Split.outlineWidth, shape = Dimens.Split.highlightShape)
        .padding(paddingValues)
}
