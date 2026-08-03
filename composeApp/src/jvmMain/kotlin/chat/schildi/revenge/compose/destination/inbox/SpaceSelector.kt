package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import chat.schildi.lib.util.formatUnreadCount
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.LocalKeyboardActionProvider
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.ContextMenuDecoration
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.components.ScrollableTabRow
import chat.schildi.revenge.compose.components.TabRowDefaults.tabIndicatorOffset
import chat.schildi.revenge.compose.components.WithContextMenu
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.compose.components.enterCommandModeContextMenuAction
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_NO_FILTER
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.revenge.model.spaces.SpaceOrder
import chat.schildi.revenge.model.spaces.toSpaceCatchAllMode
import chat.schildi.theme.scExposures
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_catch_dms_only
import shire.res.generated.resources.action_catch_groups_only
import shire.res.generated.resources.action_catch_space_orphans
import shire.res.generated.resources.action_leave
import shire.res.generated.resources.action_navigate_debug_timeline
import shire.res.generated.resources.pref_space_all_rooms_title
import kotlin.math.max
import kotlin.uuid.Uuid

@Composable
fun SpaceSelectorRow(
    lazyListState: LazyListState,
    spacesList: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>,
    totalUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    spaceSelectionHierarchy: ImmutableList<String>,
    onSpaceSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    getSpaceActionProvider: (SpaceListDataSource.SpaceHierarchyItem) -> KeyboardActionProvider<*>,
) {
    Column(modifier) {
        SpaceSelector(
            lazyListState = lazyListState,
            spacesList = spacesList,
            totalUnreadCounts = totalUnreadCounts,
            spaceSelection = spaceSelectionHierarchy,
            defaultSpace = null,
            parentSelection = persistentListOf(),
            selectSpace = { newSelection, parentSelection ->
                onSpaceSelected(
                    parentSelection + listOf(newSelection?.selectionId ?: PSEUDO_SPACE_ID_NO_FILTER)
                )
            },
            compactTabs = ScPrefs.COMPACT_ROOT_SPACES.value(),
            getSpaceActionProvider = getSpaceActionProvider,
        )
    }
}

@Composable
private fun ColumnScope.SpaceSelector(
    lazyListState: LazyListState,
    spacesList: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>,
    totalUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    spaceSelection: ImmutableList<String>,
    defaultSpace: SpaceListDataSource.AbstractSpaceHierarchyItem?,
    parentSelection: ImmutableList<String>,
    selectSpace: (SpaceListDataSource.AbstractSpaceHierarchyItem?, List<String>) -> Unit,
    compactTabs: Boolean,
    getSpaceActionProvider: (SpaceListDataSource.SpaceHierarchyItem) -> KeyboardActionProvider<*>,
) {
    val selectedSpaceIndex = if (spaceSelection.isEmpty()) {
        -1
    } else {
        spacesList.indexOfFirst { it.selectionId == spaceSelection.first() }
    }
    val childSelections = if (spaceSelection.isEmpty()) spaceSelection else spaceSelection.subList(1, spaceSelection.size)
    if (selectedSpaceIndex < 0 && childSelections.isNotEmpty()) {
        LaunchedEffect(spaceSelection) {
            Logger.withTag("SpaceSelector").w("Invalid space selection detected, clear")
            selectSpace(null, persistentListOf())
        }
        return
    }
    val selectedTab = selectedSpaceIndex + 1

    val expandSpaceChildren = childSelections.isNotEmpty()

    val allowAllRooms = defaultSpace != null || ScPrefs.PSEUDO_SPACE_ALL_ROOMS.value()

    // Actual space tabs
    val canExpandSelectedTab = !spacesList.getOrNull(selectedSpaceIndex)?.spaces.isNullOrEmpty()
    val renderExpandableIndicatorInTabs = !compactTabs
    val tabIndicatorColor = animateColorAsState(
        targetValue = if (expandSpaceChildren || (!canExpandSelectedTab && !renderExpandableIndicatorInTabs))
            MaterialTheme.colorScheme.secondary
        else
            MaterialTheme.colorScheme.primary,
        label = "tabIndicatorColor"
    ).value
    val selectedTabRendered = selectedTab.correctDownIfNot(allowAllRooms)
    ScrollableTabRow(
        selectedTabIndex = selectedTabRendered,
        edgePadding = 0.dp,
        minTabWidth = 0.dp,
        containerColor = MaterialTheme.scExposures.spaceBarBg ?: TabRowDefaults.primaryContainerColor,
        indicator = { tabPositions ->
            Box(
                Modifier
                    .tabIndicatorOffset(tabPositions.getOrNull(selectedTabRendered) ?: tabPositions[0])
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(3.dp)
                    .background(color = tabIndicatorColor, shape = RoundedCornerShape(1.5.dp))
            )
        },
    ) {
        if (allowAllRooms) {
            if (defaultSpace != null) {
                SpaceTab(defaultSpace, selectedTab == 0, expandSpaceChildren, false, compactTabs, getSpaceActionProvider) {
                    if (selectedTab != 0) {
                        selectSpace(null, parentSelection)
                    }
                }
            } else {
                ShowAllTab(totalUnreadCounts, selectedTab == 0, expandSpaceChildren, compactTabs) {
                    if (selectedTab != 0) {
                        selectSpace(null, parentSelection)
                    }
                }
            }
        }
        spacesList.forEachIndexed { index, space ->
            val selected = selectedSpaceIndex == index
            key(space.selectionId) {
                SpaceTab(
                    space,
                    selected,
                    expandSpaceChildren,
                    renderExpandableIndicatorInTabs && space.spaces.isNotEmpty(),
                    compactTabs,
                    getSpaceActionProvider,
                ) {
                    if (selectedSpaceIndex == index) {
                        if (expandSpaceChildren) {
                            // In case we selected a child, need to re-select this space
                            if (childSelections.isNotEmpty()) {
                                selectSpace(spacesList[index], parentSelection)
                            }
                        } else if (space.spaces.isNotEmpty()) {
                            // Null means we expand it
                            selectSpace(null, parentSelection + spacesList[index].selectionId)
                        }
                    } else {
                        selectSpace(spacesList[index], parentSelection)
                    }
                }
            }
        }
    }

    // Child spaces if expanded
    if (selectedSpaceIndex != -1 && expandSpaceChildren) {
        val safeSpace = spacesList[selectedSpaceIndex] as? SpaceListDataSource.SpaceHierarchyItem
        if (safeSpace != null) {
            SpaceSelector(
                lazyListState = lazyListState,
                spacesList = safeSpace.spaces,
                totalUnreadCounts = totalUnreadCounts,
                selectSpace = selectSpace,
                spaceSelection = childSelections,
                defaultSpace = spacesList[selectedSpaceIndex],
                parentSelection = (parentSelection + listOf(spacesList[selectedSpaceIndex].selectionId)).toImmutableList(),
                compactTabs = false,
                getSpaceActionProvider = getSpaceActionProvider,
            )
        }
    }
}

private fun Int.correctDownIfNot(condition: Boolean) = if (condition) this else dec()
private fun Int.correctUpIfNot(condition: Boolean) = if (condition) this else inc()

@Composable
private fun SpaceTabText(text: String, selected: Boolean, expandable: Boolean) {
    val color = animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        label = "tabSelectedColor",
    ).value
    Row {
        if (expandable) {
            // We want to keep the text centered despite having an expand-icon
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.widthIn(max = 192.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (expandable) {
            ExpandableIndicator(selected, Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
fun ExpandableIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    val color = animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        label = "tabSelectedColor",
    ).value
    Box(modifier.width(12.dp)) {
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color,
        )
    }
}

@Composable
private fun AbstractSpaceTab(
    text: String,
    selected: Boolean,
    collapsed: Boolean,
    expandable: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    contextMenu: ImmutableList<ContextMenuEntry> = persistentListOf(),
    icon: @Composable () -> Unit,
) {
    val focusId = rememberFocusId()
    WithContextMenu(
        focusId = focusId,
        entries = contextMenu,
    ) { openContextMenu ->
        val tabModifier = Modifier.keyFocusable(
            id = focusId,
            role = FocusRole.AUX_ITEM,
            actionProvider = actionProvider(
                primaryAction = InteractionAction.Invoke {
                    onClick()
                    true
                },
                secondaryAction = openContextMenu,
            ),
        ).semantics {
            contentDescription = text
            role = Role.Tab
            this.selected = selected
        }
        if (compact) {
            WithTooltip(text) {
                Box(
                    tabModifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    icon()
                    /*
                    if (expandable) {
                        ExpandableIndicator(
                            selected && !collapsed,
                            Modifier.align(Alignment.CenterEnd).offset(14.dp, 0.dp)
                        )
                    }
                     */
                }
            }
        } else {
            SpaceTabLayout(
                text = { SpaceTabText(text, selected, expandable) },
                icon = icon.takeIf { !collapsed },
                modifier = tabModifier,
            )
        }
    }
}

// Based on androidx / material3 Tab
@Composable
private fun SpaceTabLayout(
    text: @Composable () -> Unit,
    icon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(Modifier.layoutId("text").padding(horizontal = 16.dp)) { text() }
            if (icon != null) {
                Box(Modifier.layoutId("icon")) { icon() }
            }
        },
    ) { measurables, constraints ->
        val textPlaceable = measurables.fastFirst { it.layoutId == "text" }
            .measure(constraints.copy(minHeight = 0))
        val iconPlaceable = icon?.let {
            measurables.fastFirst { it.layoutId == "icon" }.measure(constraints)
        }
        val tabWidth = max(textPlaceable.width, iconPlaceable?.width ?: 0)
        val minimumHeight = if (iconPlaceable == null) SpaceTabHeight else SpaceTabHeightWithIcon
        val tabHeight = max(
            minimumHeight.roundToPx(),
            textPlaceable.height + (iconPlaceable?.height ?: 0) + SpaceTabIconBaselineDistance.roundToPx(),
        )

        layout(tabWidth, tabHeight) {
            if (iconPlaceable == null) {
                textPlaceable.placeRelative(0, (tabHeight - textPlaceable.height) / 2)
            } else {
                placeSpaceTabTextAndIcon(textPlaceable, iconPlaceable, tabWidth, tabHeight)
            }
        }
    }
}

private fun Placeable.PlacementScope.placeSpaceTabTextAndIcon(
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    tabWidth: Int,
    tabHeight: Int,
) {
    val textPlaceableY = tabHeight - textPlaceable[LastBaseline] - SpaceTabTextBaselineOffset.roundToPx()
    val iconOffset = iconPlaceable.height + SpaceTabIconBaselineDistance.roundToPx() - textPlaceable[FirstBaseline]
    textPlaceable.placeRelative((tabWidth - textPlaceable.width) / 2, textPlaceableY)
    iconPlaceable.placeRelative((tabWidth - iconPlaceable.width) / 2, textPlaceableY - iconOffset)
}

private val SpaceTabHeight = 48.dp
private val SpaceTabHeightWithIcon = 72.dp
private val SpaceTabIconBaselineDistance = 20.sp
private val SpaceTabTextBaselineOffset = 17.dp

@Composable
private fun SpaceTab(
    space: SpaceListDataSource.AbstractSpaceHierarchyItem,
    selected: Boolean,
    collapsed: Boolean,
    expandable: Boolean,
    compact: Boolean,
    getSpaceActionProvider: (SpaceListDataSource.SpaceHierarchyItem) -> KeyboardActionProvider<*>,
    onClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalKeyboardActionProvider provides when (space) {
            is SpaceListDataSource.SpaceHierarchyItem -> {
                getSpaceActionProvider(space).hierarchicalKeyboardActionProvider()
            }
            else -> LocalKeyboardActionProvider.current
        }
    ) {
        AbstractSpaceTab(
            text = space.name.render(),
            selected = selected,
            collapsed = collapsed,
            expandable = expandable,
            compact = compact,
            onClick = onClick,
            contextMenu = space.spaceContextMenu(),
        ) {
            SpaceUnreadCountBox(
                space.unreadCounts,
                spaceTabUnreadBadgeOffset(compact),
                (space as? SpaceListDataSource.SpaceHierarchyItem)?.order,
            ) {
                AbstractSpaceIcon(space = space, size = spaceTabIconSize(compact), shape = spaceTabIconShape(compact))
            }
        }
    }
}

@Composable
internal fun AbstractSpaceIcon(
    space: SpaceListDataSource.AbstractSpaceHierarchyItem?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.Inbox.spaceAvatar,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = Dimens.Inbox.spaceShape,
) {
    when (space) {
        is SpaceListDataSource.SpaceHierarchyItem -> AvatarImage(
            space.room.summary.info.avatarUrl?.let { MediaSource(it) },
            size = size,
            shape = shape,
            sessionId = space.room.sessionId,
            displayName = space.room.summary.info.name ?: "",
            modifier = modifier,
        )
        is SpaceListDataSource.PseudoSpaceItem -> when (val icon = space.icon) {
            is SpaceListDataSource.PseudoSpaceIconSource.Icon -> {
                PseudoSpaceIcon(
                    imageVector = icon.icon,
                    size = size,
                    color = color,
                    modifier = modifier,
                )
            }
            is SpaceListDataSource.PseudoSpaceIconSource.Avatar -> {
                AvatarImage(
                    MediaSource(icon.url),
                    size = size,
                    shape = shape,
                    sessionId = icon.sessionId,
                    displayName = space.name.render(),
                    modifier = modifier,
                )
            }
        }
        else -> PseudoSpaceIcon(
            Icons.Filled.Home,
            size = size,
            color = color,
            modifier = modifier,
        )
    }
}

@Composable
private fun PseudoSpaceIcon(
    imageVector: ImageVector,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(size),
        tint = color,
    )
}

@Composable
private fun ShowAllTab(
    unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    selected: Boolean,
    collapsed: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    AbstractSpaceTab(
        text = stringResource(Res.string.pref_space_all_rooms_title),
        selected = selected,
        collapsed = collapsed,
        expandable = false,
        compact = compact,
        onClick = onClick,
    ) {
        SpaceUnreadCountBox(unreadCounts, spaceTabUnreadBadgeOffset(compact)) {
            PseudoSpaceIcon(Icons.Filled.Home, spaceTabIconSize(compact))
        }
    }
}

@Composable
private fun SortOrderOverlay(order: SpaceOrder?, modifier: Modifier = Modifier) {
    val orderKey = order?.order ?: return
    Text(
        orderKey,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
    )
}

@Composable
fun SpaceUnreadCountBox(
    unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    offset: Dp,
    order: SpaceOrder? = null,
    content: @Composable () -> Unit
) {
    Box {
        val debugSortOrder = order != null && ScPrefs.RENDER_SPACE_ORDER_KEYS.value()

        val mode = ScPrefs.SPACE_UNREAD_COUNTS.value()
        if (unreadCounts == null || mode == ScPrefs.SpaceUnreadCountMode.HIDE) {
            content()
            if (debugSortOrder) {
                SortOrderOverlay(order, Modifier.align(Alignment.BottomStart))
            }
            return
        }

        val countChats = mode == ScPrefs.SpaceUnreadCountMode.CHATS
        val count: Long
        val badgeColor: Color
        var outlinedBadge = false
        when {
            unreadCounts.notifiedMessages > 0 -> {
                count = if (countChats) unreadCounts.notifiedChats else unreadCounts.notifiedMessages
                badgeColor = if (unreadCounts.mentionedMessages > 0) MaterialTheme.scExposures.mentionBadgeColor else MaterialTheme.scExposures.notificationBadgeColor
            }
            unreadCounts.mentionedMessages > 0 -> {
                count = if (countChats) unreadCounts.mentionedChats else unreadCounts.mentionedMessages
                badgeColor = MaterialTheme.scExposures.mentionBadgeColor
            }
            unreadCounts.markedUnreadChats > 0 -> {
                count = unreadCounts.markedUnreadChats
                badgeColor = MaterialTheme.scExposures.notificationBadgeColor
                outlinedBadge = true
            }
            unreadCounts.unreadMessages > 0 && ScPrefs.RENDER_SILENT_UNREAD.value() -> {
                count = if (countChats) unreadCounts.unreadChats else unreadCounts.unreadMessages
                badgeColor = MaterialTheme.scExposures.unreadBadgeColor
            }
            else -> {
                // No badge to show
                content()
                if (debugSortOrder) {
                    SortOrderOverlay(order, Modifier.align(Alignment.BottomStart))
                }
                return
            }
        }

        content()

        Box(
            modifier = Modifier
                .offset(offset, -offset)
                .let {
                    if (outlinedBadge)
                        it
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                    else
                        it.background(badgeColor.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                }
                .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                .align(Alignment.TopEnd)
        ) {
            Text(
                text = formatUnreadCount(count),
                color = if (outlinedBadge) badgeColor else MaterialTheme.scExposures.colorOnAccent,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 2.dp)
            )
        }
        // Keep icon centered
        Spacer(
            Modifier
                .width(offset)
                .offset(-offset, -offset)
                .align(Alignment.TopStart))

        if (debugSortOrder) {
            SortOrderOverlay(order, Modifier.align(Alignment.BottomStart))
        }
    }
}

private fun spaceTabIconSize(compact: Boolean) = Dimens.Inbox.spaceAvatar
private fun spaceTabIconShape(compact: Boolean) = Dimens.Inbox.spaceShape
private fun spaceTabUnreadBadgeOffset(compact: Boolean) = 6.dp

@Composable
fun SpaceListDataSource.AbstractSpaceHierarchyItem.spaceContextMenu(): ImmutableList<ContextMenuEntry> {
    val showDebugOptions = ScPrefs.SHOW_DEV_INFOS.value()
    return when (this) {
        is SpaceListDataSource.SpaceHierarchyItem -> {
            val allowCatchAll = room.summary.info.canUserManageSpaces && room.summary.info.isPublic == false
            val catchAllMode = room.summary.info.spaceCatchAll.toSpaceCatchAllMode()
            listOfNotNull(
                ContextMenuActionEntry(
                    Res.string.action_navigate_debug_timeline.toStringHolder(),
                    rememberVectorPainter(Icons.Default.Navigation),
                    Action.Navigation.NavigateInNewWindow,
                    actionArgs = persistentListOf(
                        DestinationEnum.Conversation.destName,
                        room.sessionId.value,
                        room.summary.roomId.value
                    ),
                    keyboardShortcut = Key.O,
                ).takeIf { showDebugOptions },
                ContextMenuActionEntry(
                    Res.string.action_catch_space_orphans.toStringHolder(),
                    rememberVectorPainter(Icons.Default.CatchingPokemon),
                    Action.Space.SetCatchAll,
                    actionArgs = persistentListOf(
                        // Pass boolean to keep other settings intact
                        (catchAllMode == SpaceCatchAllMode.None).toString()
                    ),
                    keyboardShortcut = Key.C,
                    decoration = ContextMenuDecoration.Toggle(catchAllMode != SpaceCatchAllMode.None),
                ).takeIf { allowCatchAll },
                ContextMenuActionEntry(
                    Res.string.action_catch_dms_only.toStringHolder(),
                    rememberVectorPainter(Icons.Default.Person),
                    Action.Space.SetCatchAll,
                    actionArgs = persistentListOf(
                        if (catchAllMode == SpaceCatchAllMode.Dms) {
                            SpaceCatchAllMode.All.name
                        } else {
                            SpaceCatchAllMode.Dms.name
                        }
                    ),
                    enabled = catchAllMode != SpaceCatchAllMode.None,
                    keyboardShortcut = Key.D,
                    decoration = ContextMenuDecoration.Toggle(catchAllMode == SpaceCatchAllMode.Dms),
                ).takeIf { allowCatchAll },
                ContextMenuActionEntry(
                    Res.string.action_catch_groups_only.toStringHolder(),
                    rememberVectorPainter(Icons.Default.Group),
                    Action.Space.SetCatchAll,
                    actionArgs = persistentListOf(
                        if (catchAllMode == SpaceCatchAllMode.Groups) {
                            SpaceCatchAllMode.All.name
                        } else {
                            SpaceCatchAllMode.Groups.name
                        }
                    ),
                    enabled = catchAllMode != SpaceCatchAllMode.None,
                    keyboardShortcut = Key.G,
                    decoration = ContextMenuDecoration.Toggle(catchAllMode == SpaceCatchAllMode.Groups),
                ).takeIf { allowCatchAll },
                ContextMenuActionEntry(
                    Res.string.action_leave.toStringHolder(),
                    rememberVectorPainter(Icons.Default.MeetingRoom),
                    Action.Room.Leave,
                    critical = true,
                    keyboardShortcut = Key.V,
                ),
                enterCommandModeContextMenuAction(),
            ).toImmutableList()
        }
        else -> persistentListOf()
    }
}
