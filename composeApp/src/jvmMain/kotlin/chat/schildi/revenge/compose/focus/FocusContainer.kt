package chat.schildi.revenge.compose.focus

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.schildi.revenge.actions.FocusRole
import java.util.UUID

/**
 * A container for items in the same hierarchy depth, similar to [focusGroup]
 * but with a more explicit depth for keyboard navigation.
 */
@Composable
fun FocusContainer(
    vararg providedValues: ProvidedValue<*>,
    modifier: Modifier = Modifier,
    role: FocusRole = FocusRole.CONTAINER,
    contentAlignment: Alignment = Alignment.Center,
    focusId: UUID = rememberFocusId(),
    content: @Composable BoxScope.() -> Unit,
) {
    FlatFocusContainer(
        providedValues = providedValues,
        modifier = modifier,
        role = role,
        focusId = focusId,
    ) { modifier ->
        Box(
            modifier = modifier,
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * A container for items in the same hierarchy depth, similar to [focusGroup]
 * but with a more explicit depth for keyboard navigation.
 * This alternative variant of [FocusContainer] assumes you will use the provided
 * [Modifier] yourself rather than relying on an implicit [Box].
 */
@Composable
fun FlatFocusContainer(
    vararg providedValues: ProvidedValue<*>,
    modifier: Modifier = Modifier,
    role: FocusRole = FocusRole.CONTAINER,
    focusId: UUID = rememberFocusId(),
    content: @Composable (Modifier) -> Unit,
) {
    val parent = LocalFocusParent.current
    val me = remember(focusId, parent) { FocusParent(focusId, parent) }
    CompositionLocalProvider(
        LocalFocusParent provides me,
        *providedValues
    ) {
        content(modifier.focusGroup().keyFocusableContainer(me.uuid, parent, role))
    }
}
