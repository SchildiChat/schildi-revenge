package chat.schildi.revenge.compose.destination.split

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.DestinationContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.compose.components.AdaptiveColumn
import chat.schildi.revenge.compose.components.AdaptiveRow
import chat.schildi.revenge.compose.focus.FlatFocusContainer
import chat.schildi.revenge.compose.focus.rememberFocusId
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
        FlatFocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = primaryFocusId,
            modifier = Modifier.splitContainerDecoration(primaryFocusId, SplitRole.Start),
        ) { modifier ->
            DestinationContent(destination.primary, modifier)
        }
        FlatFocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = secondaryFocusId,
            modifier = Modifier.splitContainerDecoration(secondaryFocusId, SplitRole.End),
        ) { modifier ->
            DestinationContent(destination.secondary, modifier)
        }
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
        FlatFocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = primaryFocusId,
            modifier = Modifier.splitContainerDecoration(primaryFocusId, SplitRole.Top),
        ) { modifier ->
            DestinationContent(destination.primary, modifier)
        }
        FlatFocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = secondaryFocusId,
            modifier = Modifier.splitContainerDecoration(secondaryFocusId, SplitRole.Bottom),
        ) { modifier ->
            DestinationContent(destination.secondary, modifier)
        }
    }
}


@Composable
private fun Modifier.splitContainerDecoration(
    id: UUID,
    splitRole: SplitRole,
): Modifier {
    val keyHandler = LocalKeyboardActionHandler.current
    val focusedContainers = keyHandler.currentFocusedNestingDestinations.collectAsState(persistentListOf())
    val isActive = focusedContainers.value.firstOrNull() == id
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
