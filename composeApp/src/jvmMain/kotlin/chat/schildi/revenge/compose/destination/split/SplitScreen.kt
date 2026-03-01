package chat.schildi.revenge.compose.destination.split

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.DestinationContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.compose.components.AdaptiveColumn
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.FlatFocusContainer
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.DestinationEnum
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

enum class SplitRole {
    Start,
    End,
    Top,
    Bottom,
}

@Composable
fun SplitHorizontal(
    destination: Destination.SplitHorizontal,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    AdaptiveRow(
        modifier.then(contentModifier).fillMaxSize(),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        SplitScreenDestination(
            focusId = primaryFocusId,
            splitRole = SplitRole.Start,
            splitType = destination.type,
            destinationHolder = destination.primary,
        )
        SplitScreenDestination(
            focusId = secondaryFocusId,
            splitRole = SplitRole.End,
            splitType = destination.type,
            destinationHolder = destination.secondary,
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
        modifier.then(contentModifier).fillMaxSize(),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        SplitScreenDestination(
            focusId = primaryFocusId,
            splitRole = SplitRole.Top,
            splitType = destination.type,
            destinationHolder = destination.primary,
        )
        SplitScreenDestination(
            focusId = secondaryFocusId,
            splitRole = SplitRole.Bottom,
            splitType = destination.type,
            destinationHolder = destination.secondary,
        )
    }
}

@Composable
fun SplitScreenDestination(
    focusId: UUID,
    splitRole: SplitRole,
    splitType: DestinationEnum,
    destinationHolder: DestinationStateHolder,
    modifier: Modifier = Modifier,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusedContainers = keyHandler.currentFocusedNestingDestinations.collectAsState(persistentListOf())
    val isActive = focusedContainers.value.firstOrNull() == focusId
    val hasActive = focusedContainers.value.contains(focusId)
    val parentDestinationStateHolder = LocalDestinationState.current

    // Publish current focused title for window title
    if (parentDestinationStateHolder != null) {
        val childTitle = destinationHolder.state.collectAsState().value.titleOverride
        LaunchedEffect(hasActive, childTitle) {
            if (hasActive) {
                parentDestinationStateHolder.publishTitle(
                    childTitle
                ) {
                    it.type == splitType
                }
            }
        }
    }

    // Actual content
    FlatFocusContainer(
        LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
        role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
        focusId = focusId,
        modifier = modifier.splitContainerDecoration(
            isActive,
            splitRole,
        ),
    ) { modifier ->
        DestinationContent(destinationHolder, modifier)
    }
}

@Composable
private fun Modifier.splitContainerDecoration(
    isActive: Boolean,
    splitRole: SplitRole,
): Modifier {
    val padding = Dimens.Split.outlineWidth
    val halfPadding = padding / 2
    val paddingValues = when (splitRole) {
        SplitRole.Start -> PaddingValues(start = padding, top = padding, end = halfPadding, bottom = padding)
        SplitRole.End -> PaddingValues(start = halfPadding, top = padding, end = padding, bottom = padding)
        SplitRole.Top -> PaddingValues(start = padding, top = padding, end = padding, bottom = halfPadding)
        SplitRole.Bottom -> PaddingValues(start = padding, top = halfPadding, end = padding, bottom = padding)
    }
    val outline = animateColorAsState(
        if (isActive) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    ).value
    return border(color = outline, width = Dimens.Split.outlineWidth, shape = Dimens.Split.highlightShape)
        .padding(paddingValues)
}
