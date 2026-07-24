package chat.schildi.revenge.compose.destination.devtools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.components.EditTextValue
import chat.schildi.revenge.compose.components.EditableText
import chat.schildi.revenge.model.devtools.DevToolsSection
import chat.schildi.revenge.model.devtools.StateLikeType
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DevToolsEventList(
    sections: ImmutableList<DevToolsSection>,
    listState: LazyListState,
    persist: suspend (StateLikeType, String) -> Result<Unit>,
    maxEditItemHeight: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues(),
    ) {
        sections.forEachIndexed { index, section ->
            devToolsListSection(section, isFirst = index == 0, isLast = index == sections.size -1, persist, maxEditItemHeight)
        }
    }
}

private fun LazyListScope.devToolsListSection(
    section: DevToolsSection,
    isFirst: Boolean,
    isLast: Boolean,
    persist: suspend (StateLikeType, String) -> Result<Unit>,
    maxEditItemHeight: Dp,
) {
    when (section) {
        is DevToolsSection.EventList<*> -> {
            if (!isFirst || !isLast) {
                item {
                    if (isFirst) {
                        Spacer(Modifier.height(Dimens.listPadding))
                    } else {
                        HorizontalDivider(modifier = Modifier.padding(top = Dimens.listPadding))
                    }
                    Text(
                        text = section.title.render(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.listPadding),
                    )
                }
            }
            items(section.entries, key = { it.type }) { entry ->
                EditableText(
                    editId = entry.type,
                    currentValue = EditTextValue.Json(entry.content),
                    role = FocusRole.LIST_ITEM_EDITABLE_MULTI_LINE,
                    modifier = Modifier.fillMaxWidth(),
                    renderColor = MaterialTheme.colorScheme.onSurface,
                    persist = {
                        persist(entry.type, it)
                    },
                    canEdit = entry.canEdit,
                    style = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    header = {
                        DevToolsEventHeader(
                            entry.type,
                            modifier = Modifier.padding(bottom = Dimens.listPaddingSmall),
                        )
                    },
                    previewMaxLines = if (section.searchHighlight.isNullOrEmpty()) 20 else Int.MAX_VALUE,
                    editMaxHeight = maxEditItemHeight,
                )
            }
        }
    }
}

@Composable
private fun DevToolsEventHeader(
    type: StateLikeType,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val text = when (type) {
        is StateLikeType.AccountData -> type.eventType
        is StateLikeType.RoomAccountData -> type.eventType
        is StateLikeType.RoomState -> if (type.stateKey.isEmpty()) type.eventType else "${type.eventType} / ${type.stateKey}"
    }
    Text(
        text,
        color = color,
        style = style,
        modifier = modifier,
    )
}
