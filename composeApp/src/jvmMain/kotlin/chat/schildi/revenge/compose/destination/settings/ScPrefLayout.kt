package chat.schildi.revenge.compose.destination.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import chat.schildi.preferences.LocalScPreferencesStore
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.enabledState
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.defaultActionProvider
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import org.jetbrains.compose.resources.stringResource
import java.util.UUID

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> ScPref<T>.ScPrefLayout(
    modifier: Modifier = Modifier,
    clickAction: ((T) -> InteractionAction?) = { null },
    focusId: UUID = rememberFocusId(),
    selectionAsSummary: Boolean = false,
    valueToString: @Composable (T) -> String = { it.toString() },
    trailing: @Composable ((value: T, enabled: Boolean) -> Unit)? = null,
) {
    val currentValue = value()
    val enabled = LocalScPreferencesStore.current.enabledState(this).value
    val secondaryText = summaryRes?.let { stringResource(it, currentValue.toString()) }
        ?: currentValue.takeIf { selectionAsSummary }?.let { valueToString(it) }
    ListItem(
        modifier = modifier.keyFocusable(
            role = FocusRole.LIST_ITEM,
            id = focusId,
            actionProvider = defaultActionProvider(
                primaryAction = if (enabled) clickAction(currentValue) else null,
            ),
            enableClicks = enabled,
        ),
        text = {
            val color = animateColorAsState(
                if (enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary
            ).value
            Text(stringResource(titleRes), color = color)
        },
        secondaryText = secondaryText?.let {{
            val color = animateColorAsState(
                if (enabled)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.tertiary
            ).value
            Text(it, color = color)
        }},
        trailing = trailing?.let {{
            it.invoke(currentValue, enabled)
        }},
    )
}

@Composable
fun ScPrefCategoryHeader(
    title: String,
    isFirst: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Column(Modifier.fillMaxWidth()) {
        if (isFirst) {
            Spacer(Modifier.height(Dimens.windowPadding))
        } else {
            HorizontalDivider(modifier = Modifier.padding(top = Dimens.listPaddingBig))
        }
        Text(
            text = title,
            style = style,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.windowPadding, vertical = Dimens.listPaddingBig),
        )
    }
}
