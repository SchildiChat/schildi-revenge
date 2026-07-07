package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.CopyActions
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.actions.plainTextCopyActionWithMxcUrl
import chat.schildi.revenge.actions.toCopyAction
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.EditTextValue
import chat.schildi.revenge.compose.components.EditableDropdown
import chat.schildi.revenge.compose.components.EditableDropdownEntry
import chat.schildi.revenge.compose.components.EditableText
import chat.schildi.revenge.compose.components.EmptyListScreen
import chat.schildi.revenge.compose.destination.conversation.userlist.ConversationDetailsTopNavigation
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.components.TopNavigationIcon
import chat.schildi.revenge.matrixBodyDrawStyle
import chat.schildi.revenge.matrixBodyFormatter
import chat.schildi.revenge.model.RoomDetailsViewModel
import chat.schildi.revenge.model.RoomSettingsPermissions
import chat.schildi.revenge.model.conversation.HISTORY_VISIBILITY_ENTRIES
import chat.schildi.revenge.model.conversation.JOIN_RULE_ENTRIES
import chat.schildi.revenge.plaintext.EventTextFormat
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_show_room_members
import shire.res.generated.resources.empty_screen_placeholder_unexpected
import shire.res.generated.resources.hint_canonical_room_alias
import shire.res.generated.resources.hint_connected_bridges
import shire.res.generated.resources.hint_direct_chat
import shire.res.generated.resources.hint_encrypted
import shire.res.generated.resources.hint_history_visibility
import shire.res.generated.resources.hint_join_rule
import shire.res.generated.resources.hint_no_room_name
import shire.res.generated.resources.hint_not_encrypted
import shire.res.generated.resources.hint_other_room_aliases
import shire.res.generated.resources.hint_private_room
import shire.res.generated.resources.hint_public_room
import shire.res.generated.resources.hint_room_id
import shire.res.generated.resources.hint_room_name_private
import shire.res.generated.resources.hint_room_predecessor
import shire.res.generated.resources.hint_room_type
import shire.res.generated.resources.hint_room_version
import shire.res.generated.resources.hint_topic
import shire.res.generated.resources.history_visibility_custom
import shire.res.generated.resources.room_details_title
import kotlin.toString

@Composable
fun RoomDetailsScreen(
    destination: Destination.RoomDetails,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModelKey = viewModelKey(destination)
    val viewModel = viewModel<RoomDetailsViewModel>(
        key = viewModelKey,
        factory = RoomDetailsViewModel.factory(destination.sessionId, destination.roomId),
    )
    publishTitle(viewModel)

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalKeyboardActionProvider provides
                viewModel.actionProvider.hierarchicalKeyboardActionProvider(),
        LocalRoomContextSuggestionsProvider provides viewModel.roomContextSuggestionsProvider,
        LocalListActionProvider provides listAction,
        LocalMatrixBodyFormatter provides matrixBodyFormatter(),
        LocalMatrixBodyDrawStyle provides matrixBodyDrawStyle(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ).fillMaxSize(),
    ) {
        val info = viewModel.roomInfo.collectAsState().value
        val topic = viewModel.topic.collectAsState().value
        val permissions = viewModel.roomSettingsPermissions.collectAsState().value ?: RoomSettingsPermissions()
        val predecessorRoom = viewModel.predecessorRoom.collectAsState().value
        val roomType = viewModel.roomType.collectAsState().value
        val destinationState = LocalDestinationState.current
        Column(contentModifier.fillMaxSize()) {
            ConversationDetailsTopNavigation(stringResource(Res.string.room_details_title)) {
                TopNavigationIcon(
                    Icons.Default.Group,
                    stringResource(Res.string.action_show_room_members),
                ) {
                    destinationState?.navigate(Destination.RoomMembers(viewModel.sessionId, viewModel.roomId))
                }
            }
            if (info == null) {
                EmptyListScreen(
                    title = Res.string.empty_screen_placeholder_unexpected.toStringHolder(),
                    icon = rememberVectorPainter(Icons.Default.Person),
                    isSearching = false,
                    isLoading = true,
                    loadState = viewModel.loadState,
                    modifier = Modifier.fillMaxSize().padding(Dimens.windowPadding),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().padding(Dimens.windowPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Dimens.verticalArrangement,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().keyFocusable(
                                    FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        copyActions = plainTextCopyActionWithMxcUrl(info.avatarUrl),
                                    ),
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                AvatarImage(
                                    source = info.avatarUrl?.let { MediaSource(it) },
                                    size = 128.dp,
                                    displayName = info.name ?: viewModel.roomId.value,
                                    allowAnimated = true,
                                )
                            }
                        }
                        if (info.rawName != null || !info.isDirect) {
                            // Generated name, if differs from raw name
                            if (info.rawName != info.name) {
                                info.name?.let { displayName ->
                                    item {
                                        RoomDetailsSection(
                                            copyActions = plainTextCopyAction { displayName },
                                        ) {
                                            SectionText(displayName)
                                        }
                                    }
                                }
                            }
                            // Editable room name
                            item {
                                EditableText(
                                    "$viewModelKey/roomName",
                                    currentValue = info.rawName?.let(EditTextValue::Plain),
                                    role = FocusRole.LIST_ITEM_EDITABLE_SINGLE_LINE,
                                    modifier = Modifier.fillMaxWidth(),
                                    renderColor = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    persist = viewModel::setRoomName,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    textAlign = TextAlign.Center,
                                    emptyFallbackText = stringResource(Res.string.hint_no_room_name),
                                    canEdit = permissions.canEditName,
                                    singleLine = true,
                                )
                            }
                        }
                        item {
                            RoomInfoBadgeRow(
                                info,
                                Modifier.fillMaxWidth().keyFocusable(FocusRole.LIST_ITEM),
                            )
                        }
                        if (topic != null || permissions.canEditTopic) {
                            item {
                                EditableText(
                                    "$viewModelKey/roomTopic",
                                    currentValue = topic?.let {
                                        EditTextValue.AutoFormatted(info.topic ?: topic.text.text, topic)
                                    } ?: info.topic?.let(EditTextValue::Plain),
                                    role = FocusRole.LIST_ITEM_EDITABLE_MULTI_LINE,
                                    modifier = Modifier.fillMaxWidth(),
                                    renderColor = MaterialTheme.colorScheme.onSurface,
                                    persist = viewModel::setRoomTopic,
                                    canEdit = permissions.canEditTopic,
                                    header = {
                                        RoomDetailsSectionHeader(
                                            stringResource(Res.string.hint_topic),
                                            modifier = Modifier.padding(bottom = Dimens.listPaddingSmall),
                                        )
                                    }
                                )
                            }
                        }
                        // Editable private room name override
                        item {
                            EditableText(
                                "$viewModelKey/roomNamePrivate",
                                currentValue = info.privateRoomName?.let(EditTextValue::Plain),
                                role = FocusRole.LIST_ITEM_EDITABLE_SINGLE_LINE,
                                modifier = Modifier.fillMaxWidth(),
                                renderColor = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                persist = viewModel::setPrivateRoomName,
                                header = {
                                    RoomDetailsSectionHeader(
                                        stringResource(Res.string.hint_room_name_private),
                                        modifier = Modifier.padding(bottom = Dimens.listPaddingSmall),
                                    )
                                }
                            )
                        }
                        info.canonicalAlias?.let { alias ->
                            item {
                                RoomDetailsSection(
                                    stringResource(Res.string.hint_canonical_room_alias),
                                    plainTextCopyAction { alias.value },
                                ) {
                                    SectionText(alias.value)
                                }
                            }
                        }
                        val otherAliases = info.alternativeAliases.filter { it != info.canonicalAlias }.distinct()
                        if (otherAliases.isNotEmpty()) {
                            item {
                                RoomDetailsSection(
                                    stringResource(Res.string.hint_other_room_aliases),
                                    plainTextCopyAction { otherAliases.joinToString() },
                                ) {
                                    otherAliases.forEach { alias ->
                                        SectionText(alias.value)
                                    }
                                }
                            }
                        }
                        info.joinRule?.let { joinRule ->
                            item {
                                val roomNames = viewModel.joinRuleRoomNames.collectAsState().value
                                RoomDetailsDropDownSetting(
                                    stringResource(Res.string.hint_join_rule),
                                    joinRule,
                                    JOIN_RULE_ENTRIES,
                                    persist = { viewModel.setJoinRule(it) },
                                    renderValue = { value, entry ->
                                        entry?.title?.render() ?: value?.let {
                                            EventTextFormat.joinRuleToText(value, roomNames.orEmpty())
                                        } ?: value.toString()
                                    },
                                    enabled = permissions.canSetJoinRule,
                                )
                            }
                        }
                        item {
                            RoomDetailsDropDownSetting(
                                stringResource(Res.string.hint_history_visibility),
                                info.historyVisibility,
                                HISTORY_VISIBILITY_ENTRIES,
                                persist = { viewModel.setRoomHistoryVisibility(it) },
                                renderValue = { value, entry ->
                                    entry?.title?.render()
                                        ?: (value as? RoomHistoryVisibility.Custom)?.let {
                                            stringResource(Res.string.history_visibility_custom, it.value)
                                        } ?: value.toString()
                                },
                                enabled = permissions.canSetRoomHistoryVisibility,
                            )
                        }
                        item {
                            RoomInfoAdvancedInfoField(
                                stringResource(Res.string.hint_room_id),
                                viewModel.roomId.value,
                                monospace = true,
                            )
                        }
                        roomType?.let { roomType ->
                            item {
                                RoomInfoAdvancedInfoField(
                                    stringResource(Res.string.hint_room_type),
                                    roomType,
                                )
                            }
                        }
                        info.roomVersion?.let { roomVersion ->
                            item {
                                RoomInfoAdvancedInfoField(
                                    stringResource(Res.string.hint_room_version),
                                    roomVersion,
                                )
                            }
                        }
                        predecessorRoom?.let {
                            item {
                                RoomInfoAdvancedInfoField(
                                    stringResource(Res.string.hint_room_predecessor),
                                    predecessorRoom.roomId.value,
                                    primaryAction = InteractionAction.Navigate {
                                        Destination.Conversation(
                                            sessionId = viewModel.sessionId,
                                            roomId = predecessorRoom.roomId,
                                        )
                                    }
                                )
                            }
                        }
                        val bridges = info.bridgeState.mapNotNull { bridge ->
                            if (bridge.protocol?.displayName != null) {
                                if (bridge.protocol?.id != null && bridge.protocol?.displayName?.lowercase() != bridge.protocol?.id) {
                                    buildString {
                                        append(bridge.protocol?.displayName)
                                        append(" (")
                                        append(bridge.protocol?.id)
                                        append(")")
                                    }
                                } else {
                                    bridge.protocol?.displayName
                                }
                            } else {
                                bridge.protocol?.id
                            }
                        }.distinct()
                        if (bridges.isNotEmpty()) {
                            item {
                                RoomDetailsSection(
                                    stringResource(Res.string.hint_connected_bridges),
                                    plainTextCopyAction { bridges.joinToString() },
                                ) {
                                    bridges.forEach { bridge ->
                                        SectionText(bridge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomDetailsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    Text(
        text,
        color = color,
        style = style,
        modifier = modifier,
    )
}

@Composable
private fun RoomDetailsSection(
    headerText: String? = null,
    copyActions: CopyActions?,
    modifier: Modifier = Modifier,
    primaryAction: InteractionAction? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    content: @Composable ColumnScope.() -> Unit,
) {
    SelectionContainer(modifier) {
        Column(
            Modifier.fillMaxWidth().keyFocusable(
                role = FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = primaryAction,
                    copyActions = copyActions,
                ),
            ),
            verticalArrangement = Dimens.verticalArrangementSmall,
        ) {
            if (headerText != null) {
                RoomDetailsSectionHeader(headerText, Modifier, color, style)
            }
            content()
        }
    }
}

@Composable
private fun RoomDetailsSection(
    headerText: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Dimens.verticalArrangementSmall,
    ) {
        if (headerText != null) {
            RoomDetailsSectionHeader(headerText, Modifier, color, style)
        }
        content()
    }
}

@Composable
private fun SectionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        style = style,
    )
}

@Composable
private fun RoomInfoBadgeRow(
    info: RoomInfo,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        FlowRow(
            Modifier,
            verticalArrangement = Dimens.verticalArrangementSmall,
            horizontalArrangement = Dimens.horizontalArrangement,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            when (info.isPublic) {
                true -> RoomInfoBadge(stringResource(Res.string.hint_public_room), Icons.Default.Public)
                false -> {
                    if (!info.isDirect) {
                        RoomInfoBadge(stringResource(Res.string.hint_private_room), Icons.Default.PublicOff)
                    }
                }

                else -> {}
            }
            if (info.isDirect) {
                RoomInfoBadge(stringResource(Res.string.hint_direct_chat), Icons.Default.Person)
            }
            when (info.isEncrypted) {
                true -> RoomInfoBadge(stringResource(Res.string.hint_encrypted), Icons.Default.Lock)
                false -> RoomInfoBadge(stringResource(Res.string.hint_not_encrypted), Icons.Default.NoEncryption)
                else -> {}
            }
        }
    }
}

@Composable
private fun RoomInfoBadge(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(MaterialTheme.scExposures.bubbleBgIncoming, Dimens.Conversation.messageBubbleShape)
            .padding(Dimens.Conversation.messageBubbleInnerPadding),
        horizontalArrangement = Dimens.horizontalArrangementSmall,
    ) {
        Icon(
            icon,
            text,
            Modifier.size(16.dp),
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RoomInfoAdvancedInfoField(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    primaryAction: InteractionAction? = null,
    monospace: Boolean = false,
) {
    RoomDetailsSection(
        title,
        copyActions = content.toCopyAction(),
        primaryAction = primaryAction,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier,
    ) {
        SectionText(
            content,
            style = MaterialTheme.typography.bodySmall.let {
                if (monospace) {
                    it.copy(fontFamily = FontFamily.Monospace)
                } else {
                    it
                }
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T>RoomDetailsDropDownSetting(
    headerText: String,
    currentValue: T,
    items: List<EditableDropdownEntry<out T>>,
    persist: suspend ActionContext.(T) -> ActionResult,
    renderValue: @Composable (T?, EditableDropdownEntry<out T>?) -> String = { value, entry ->
        entry?.title?.render() ?: value.toString()
    },
    enabled: Boolean = true,
) {
    EditableDropdown(
        currentValue,
        items,
        FocusRole.LIST_ITEM,
        persist = persist,
        enabled = enabled,
    ) { modifier, value, entry ->
        RoomDetailsSection(
            headerText,
            modifier,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        ) {
            val text = renderValue(value, entry)
            SectionText(
                text,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
