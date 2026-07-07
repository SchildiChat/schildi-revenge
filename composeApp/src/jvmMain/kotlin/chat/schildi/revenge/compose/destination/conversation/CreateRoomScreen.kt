package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalMatrixBodyDrawStyle
import chat.schildi.revenge.LocalMatrixBodyFormatter
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.ListActions
import chat.schildi.revenge.actions.LocalListActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.currentActionContext
import chat.schildi.revenge.compose.components.EditTextValue
import chat.schildi.revenge.compose.components.EditableDropdown
import chat.schildi.revenge.compose.components.EditableDropdownEntry
import chat.schildi.revenge.compose.components.EditableText
import chat.schildi.revenge.compose.components.ExpandButton
import chat.schildi.revenge.compose.components.KeyboardShortcutAssigner
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.components.keyboardShortcutFromIndexZero
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.matrixBodyDrawStyle
import chat.schildi.revenge.matrixBodyFormatter
import chat.schildi.revenge.model.conversation.COMMON_ROOM_PRESETS
import chat.schildi.revenge.model.conversation.CreateRoomState
import chat.schildi.revenge.model.conversation.CreateRoomViewModel
import chat.schildi.revenge.model.conversation.HISTORY_VISIBILITY_ENTRIES
import chat.schildi.revenge.model.conversation.JOIN_RULE_ENTRIES
import chat.schildi.revenge.model.conversation.ROOM_PRESETS
import chat.schildi.revenge.model.conversation.ROOM_VISIBILITY_ENTRIES
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_create
import shire.res.generated.resources.create_room
import shire.res.generated.resources.hint_e2ee
import shire.res.generated.resources.hint_history_visibility
import shire.res.generated.resources.hint_join_rule
import shire.res.generated.resources.hint_nothing_selected
import shire.res.generated.resources.hint_preset_default
import shire.res.generated.resources.hint_room_directory_visibility
import shire.res.generated.resources.hint_room_name
import shire.res.generated.resources.hint_room_version
import shire.res.generated.resources.hint_topic
import shire.res.generated.resources.history_visibility_custom
import shire.res.generated.resources.power_level_user_role_creator
import shire.res.generated.resources.pref_category_advanced_room_settings
import shire.res.generated.resources.room_preset
import shire.res.generated.resources.hint_disabled
import shire.res.generated.resources.hint_enabled
import kotlin.collections.orEmpty
import kotlin.comparisons.compareBy

@Composable
fun CreateRoomScreen(
    destination: Destination.CreateRoom,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModelKey = viewModelKey(destination)
    val viewModel: CreateRoomViewModel = viewModel(
        key = viewModelKey,
        factory = CreateRoomViewModel.factory(destination.initialSessionId),
    )
    publishTitle(viewModel)

    val listState = rememberLazyListState()
    val listAction = remember(listState) { ListActions(listState) }
    FocusContainer(
        LocalListActionProvider provides listAction,
        LocalMatrixBodyFormatter provides matrixBodyFormatter(),
        LocalMatrixBodyDrawStyle provides matrixBodyDrawStyle(),
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .fillMaxSize(),
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column(contentModifier.fillMaxSize()) {
            TopNavigation {
                TopNavigationTitle(stringResource(Res.string.create_room))
                TopNavigationCloseOrNavigateToInboxIcon()
            }

            val state = viewModel.state.collectAsState().value
            val settings = viewModel.settings.collectAsState().value
            val params = settings.params
            val renderedTopic = viewModel.renderedTopic.collectAsState().value
            val showAdvancedSettings = ScPrefs.SHOW_ADVANCED_ROOM_CREATION_PARAMETERS.value()
            val actionContext = currentActionContext()

            val roomVersions = viewModel.availableRoomVersions.collectAsState(null).value

            val canEditSettings = state is CreateRoomState.Idle || state is CreateRoomState.Failure
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Dimens.verticalArrangement,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                ) {
                    // Room creator
                    item {
                        val sessionIds = viewModel.availableSessionIds.collectAsState(null).value.orEmpty().map {
                            EditableDropdownEntry(it, it.value.toStringHolder())
                        }
                        CreateRoomDropDownSetting(
                            stringResource(Res.string.power_level_user_role_creator),
                            settings.sessionId,
                            sessionIds,
                            persist = { viewModel.setSessionId(it) },
                            enabled = canEditSettings && sessionIds.any { it.value != settings.sessionId },
                        )
                    }

                    // Room name
                    item {
                        EditableText(
                            "$viewModelKey/roomName",
                            currentValue = params.name?.let(EditTextValue::Plain),
                            role = FocusRole.LIST_ITEM_EDITABLE_SINGLE_LINE,
                            modifier = Modifier.fillMaxWidth(),
                            renderColor = MaterialTheme.colorScheme.onSurface,
                            persist = viewModel::setRoomName,
                            textAlign = TextAlign.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            canEdit = canEditSettings,
                            singleLine = true,
                            header = {
                                CreateRoomSectionHeader(
                                    stringResource(Res.string.hint_room_name),
                                    modifier = Modifier.padding(bottom = Dimens.listPaddingSmall),
                                )
                            }
                        )
                    }

                    // Room topic
                    item {
                        EditableText(
                            "$viewModelKey/roomTopic",
                            currentValue = renderedTopic?.let {
                                EditTextValue.AutoFormatted(
                                    params.topic ?: renderedTopic.text.text,
                                    renderedTopic
                                )
                            } ?: params.topic?.let(EditTextValue::Plain),
                            role = FocusRole.LIST_ITEM_EDITABLE_MULTI_LINE,
                            modifier = Modifier.fillMaxWidth(),
                            renderColor = MaterialTheme.colorScheme.onSurface,
                            persist = viewModel::setRoomTopic,
                            textAlign = TextAlign.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            canEdit = canEditSettings,
                            header = {
                                CreateRoomSectionHeader(
                                    stringResource(Res.string.hint_topic),
                                    modifier = Modifier.padding(bottom = Dimens.listPaddingSmall),
                                )
                            }
                        )
                    }

                    // Room preset
                    item {
                        CreateRoomDropDownSetting(
                            stringResource(Res.string.room_preset),
                            params.preset,
                            if (showAdvancedSettings) ROOM_PRESETS else COMMON_ROOM_PRESETS,
                            persist = { viewModel.setPreset(it) },
                            enabled = canEditSettings,
                        )
                    }

                    // Encryption
                    item {
                        CreateRoomToggle(
                            stringResource(Res.string.hint_e2ee),
                            currentValue = params.isEncrypted,
                            persist = { viewModel.setEncrypted(it) ; true },
                            enabled = canEditSettings,
                        )
                    }

                    item {
                        val scope = rememberCoroutineScope()
                        ExpandButton(
                            text = stringResource(Res.string.pref_category_advanced_room_settings),
                            expanded = showAdvancedSettings,
                            enabled = canEditSettings,
                        ) {
                            scope.launch {
                                RevengePrefs.setSetting(
                                    ScPrefs.SHOW_ADVANCED_ROOM_CREATION_PARAMETERS,
                                    !showAdvancedSettings
                                )
                            }
                        }
                    }

                    if (showAdvancedSettings) {
                        item {
                            CreateRoomDropDownSetting(
                                stringResource(Res.string.hint_room_directory_visibility),
                                params.visibility,
                                ROOM_VISIBILITY_ENTRIES,
                                persist = { viewModel.setVisibility(it) },
                                enabled = canEditSettings,
                            )
                        }

                        item {
                            CreateRoomDropDownSetting(
                                stringResource(Res.string.hint_join_rule),
                                params.joinRuleOverride,
                                JOIN_RULE_ENTRIES,
                                persist = { viewModel.setRoomJoinRule(it) },
                                persistNull = { viewModel.setRoomJoinRule(null) },
                                enabled = canEditSettings,
                            )
                        }

                        item {
                            CreateRoomDropDownSetting(
                                stringResource(Res.string.hint_history_visibility),
                                params.historyVisibilityOverride,
                                HISTORY_VISIBILITY_ENTRIES,
                                persist = { viewModel.setRoomHistoryVisibility(it) },
                                persistNull = { viewModel.setRoomHistoryVisibility(null) },
                                renderValue = { value, entry ->
                                    entry?.title?.render()
                                        ?: (value as? RoomHistoryVisibility.Custom)?.let {
                                            stringResource(Res.string.history_visibility_custom, it.value)
                                        } ?: value.toString()
                                },
                                enabled = canEditSettings,
                            )
                        }

                        if (roomVersions != null && roomVersions.available.isNotEmpty()) {
                            item {
                                CreateRoomDropDownSetting(
                                    stringResource(Res.string.hint_room_version),
                                    params.roomVersion,
                                    roomVersions.available.toList().sortedWith(
                                        compareBy<Pair<String, String>> { (_, capability) ->
                                            when (capability) {
                                                "stable" -> 1
                                                "unstable" -> 2
                                                else -> 3
                                            }
                                        }.thenBy { (version, _) ->
                                            version.toLongOrNull() ?: Long.MAX_VALUE
                                        }.thenBy { (version, _) ->
                                            version
                                        }
                                    ).map { (version, _) ->
                                        EditableDropdownEntry(version, version.toStringHolder())
                                    },
                                    persist = { viewModel.setRoomVersion(it) },
                                    persistNull = { viewModel.setRoomVersion(null) },
                                    nullText = buildString {
                                        append(stringResource(Res.string.hint_preset_default))
                                        append(" (")
                                        append(roomVersions.default)
                                        append(")")
                                    },
                                    enabled = canEditSettings && roomVersions.available.size > 1,
                                    keyboardShortcutAssigner = object : KeyboardShortcutAssigner<String> {
                                        override fun invoke(index: Int, item: String?): Key? =
                                            (item ?: "0").toIntOrNull()?.keyboardShortcutFromIndexZero()
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            enabled = canEditSettings,
                            onClick = { viewModel.createRoom(actionContext) },
                            modifier = Modifier
                                // Extra padding to separate from edit fields
                                .padding(top = Dimens.listPadding)
                                .keyFocusable(
                                    role = FocusRole.LIST_ITEM,
                                    actionProvider = actionProvider(
                                        primaryAction = InteractionAction.Invoke {
                                            viewModel.createRoom(actionContext)
                                            true
                                        },
                                    ),
                                    addClickListener = false,
                                ),
                        ) {
                            Text(stringResource(Res.string.action_create))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomToggle(
    text: String,
    currentValue: Boolean,
    persist: (Boolean) -> Boolean,
    enabled: Boolean = true,
) {
    CreateRoomSection(
        headerText = text,
        modifier = Modifier.keyFocusable(
            role = FocusRole.LIST_ITEM,
            actionProvider = actionProvider(
                primaryAction = if (enabled) {
                    InteractionAction.Invoke {
                        persist(!currentValue)
                    }
                } else {
                    null
                },
            ),
            enableClicks = enabled,
        ),
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.tertiary
        },
    ) {
        Row(
            horizontalArrangement = Dimens.horizontalArrangementBig,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionText(
                text = stringResource(
                    if (currentValue) Res.string.hint_enabled else Res.string.hint_disabled
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Switch(
                checked = currentValue,
                onCheckedChange = {
                    persist(it)
                },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun <T>CreateRoomDropDownSetting(
    headerText: String,
    currentValue: T?,
    items: List<EditableDropdownEntry<out T>>,
    persist: suspend ActionContext.(T) -> ActionResult,
    persistNull: (suspend () -> ActionResult)? = null,
    nullText: String = stringResource(
        if (persistNull == null)
            Res.string.hint_nothing_selected
        else
            Res.string.hint_preset_default
    ),
    renderValue: @Composable (T, EditableDropdownEntry<out T>?) -> String = { value, entry ->
        entry?.title?.render()
            ?: value.toString()
    },
    enabled: Boolean = true,
    keyboardShortcutAssigner: KeyboardShortcutAssigner<T> = if (persistNull == null)
        KeyboardShortcutAssigner.Indexed()
    else
        KeyboardShortcutAssigner.ZeroIndexed(),
) {
    EditableDropdown(
        currentValue,
        items,
        FocusRole.LIST_ITEM,
        persist = persist,
        enabled = enabled,
        nullText = nullText,
        persistNull = persistNull ?: { ActionResult.Inapplicable },
        nullItem = persistNull?.let {
            EditableDropdownEntry(Unit, Res.string.hint_preset_default.toStringHolder())
        },
        keyboardShortcutAssigner = keyboardShortcutAssigner,
    ) { modifier, value, entry ->
        CreateRoomSection(
            headerText,
            modifier,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        ) {
            val text = if (value == null) {
                nullText
            } else {
                renderValue(value, entry)
            }
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

@Composable
private fun CreateRoomSection(
    headerText: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Dimens.verticalArrangementSmall,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (headerText != null) {
            CreateRoomSectionHeader(headerText, Modifier, color, style)
        }
        content()
    }
}

@Composable
private fun CreateRoomSectionHeader(
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
        textAlign = TextAlign.Center,
    )
}


@Composable
private fun SectionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = TextAlign.Center,
    )
}
