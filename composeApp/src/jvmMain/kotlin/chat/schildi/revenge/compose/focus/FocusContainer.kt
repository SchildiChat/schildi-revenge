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
import java.util.UUID

/**
 * A container for items in the same hierarchy depth, similar to [focusGroup]
 * but with a more explicit depth for keyboard navigation.
 */
@Composable
fun FocusContainer(
    vararg providedValues: ProvidedValue<*>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val parent = LocalFocusParent.current
    val me = remember { FocusParent(UUID.randomUUID(), parent) }
    CompositionLocalProvider(
        LocalFocusParent provides me,
        *providedValues
    ) {
        Box(
            modifier = modifier.focusGroup().keyFocusableContainer(me.uuid, parent),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}
