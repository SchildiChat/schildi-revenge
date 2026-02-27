package chat.schildi.revenge.compose.destination.split

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.DestinationContent
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.compose.focus.FocusContainer
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
fun SplitHorizontal(destination: Destination.SplitHorizontal, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(-Dimens.Split.outlineWidth),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        FocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = primaryFocusId,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(destination.fraction)
                    .splitContainerDecoration(primaryFocusId, SplitRole.Start),
                Alignment.Center
            ) {
                DestinationContent(destination.primary, Modifier.fillMaxSize())
            }
        }
        FocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = secondaryFocusId,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .splitContainerDecoration(secondaryFocusId, SplitRole.End),
                Alignment.Center
            ) {
                DestinationContent(destination.secondary, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun SplitVertical(destination: Destination.SplitVertical, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(-Dimens.Split.outlineWidth),
    ) {
        val primaryFocusId = rememberFocusId()
        val secondaryFocusId = rememberFocusId()
        FocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(true),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = primaryFocusId,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(destination.fraction)
                    .splitContainerDecoration(primaryFocusId, SplitRole.Top),
                Alignment.Center
            ) {
                DestinationContent(destination.primary, Modifier.fillMaxSize())
            }
        }
        FocusContainer(
            LocalKeyboardActionProvider provides splitKeyboardActionProvider(false),
            role = FocusRole.NESTING_DESTINATION_ROOT_CONTAINER,
            focusId = secondaryFocusId,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .splitContainerDecoration(secondaryFocusId, SplitRole.Bottom),
                Alignment.Center
            ) {
                DestinationContent(destination.secondary, Modifier.fillMaxSize())
            }
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
