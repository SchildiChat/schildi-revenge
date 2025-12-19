package chat.schildi.revenge.compose.destination.inbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.lib.util.formatUnreadCount
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.ScrollableTabRow
import chat.schildi.revenge.compose.components.TabRowDefaults.tabIndicatorOffset
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceAggregationDataSource
import chat.schildi.theme.scExposures
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.sc_space_all_rooms_title

@Composable
fun SpaceSelectorRow(
    lazyListState: LazyListState,
    spacesList: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>,
    totalUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?,
    spaceSelectionHierarchy: ImmutableList<String>,
    onSpaceSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
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
                if (newSelection == null) {
                    onSpaceSelected(parentSelection)
                } else {
                    onSpaceSelected(parentSelection + listOf(newSelection.selectionId))
                }
            },
            compactTabs = ScPrefs.COMPACT_ROOT_SPACES.value(),
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
    selectSpace: (SpaceListDataSource.AbstractSpaceHierarchyItem?, ImmutableList<String>) -> Unit,
    compactTabs: Boolean,
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

    // Child spaces if expanded
    var expandSpaceChildren by remember { mutableStateOf(childSelections.isNotEmpty()) }

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
                SpaceTab(defaultSpace, selectedTab == 0, expandSpaceChildren, false, compactTabs) {
                    expandSpaceChildren = false
                    if (selectedTab != 0) {
                        selectSpace(null, parentSelection)
                    }
                }
            } else {
                ShowAllTab(totalUnreadCounts, selectedTab == 0, expandSpaceChildren, compactTabs) {
                    expandSpaceChildren = false
                    if (selectedTab != 0) {
                        selectSpace(null, parentSelection)
                    }
                }
            }
        }
        spacesList.forEachIndexed { index, space ->
            val selected = selectedSpaceIndex == index
            SpaceTab(
                space,
                selected,
                expandSpaceChildren,
                renderExpandableIndicatorInTabs && space.spaces.isNotEmpty(),
                compactTabs,
            ) {
                if (selectedSpaceIndex == index) {
                    if (expandSpaceChildren) {
                        expandSpaceChildren = false
                        // In case we selected a child, need to re-select this space
                        if (childSelections.isNotEmpty()) {
                            selectSpace(spacesList[index], parentSelection)
                        }
                    } else if (space.spaces.isNotEmpty()) {
                        expandSpaceChildren = true
                    }
                } else {
                    expandSpaceChildren = false
                    selectSpace(spacesList[index], parentSelection)
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
            )
        }
    }
}

private fun Int.correctDownIfNot(condition: Boolean) = if (condition) this else dec()
private fun Int.correctUpIfNot(condition: Boolean) = if (condition) this else inc()

private fun selectSpaceIndex(
    index: Int,
    spacesList: ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>,
    selectSpace: (SpaceListDataSource.AbstractSpaceHierarchyItem?, ImmutableList<String>) -> Unit,
    parentSelection: ImmutableList<String>
) {
    if (index == 0) {
        selectSpace(null, parentSelection)
    } else {
        selectSpace(spacesList[index-1], parentSelection)
    }
}

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
    icon: @Composable () -> Unit,
) {
    if (compact) {
        Box(
            Modifier
                .clickable(onClick = onClick)
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
    } else {
        Tab(
            text = { SpaceTabText(text, selected, expandable) },
            icon = icon.takeIf { !collapsed },
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun SpaceTab(
    space: SpaceListDataSource.AbstractSpaceHierarchyItem,
    selected: Boolean,
    collapsed: Boolean,
    expandable: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    AbstractSpaceTab(
        text = space.name.render(),
        selected = selected,
        collapsed = collapsed,
        expandable = expandable,
        compact = compact,
        onClick = onClick,
    ) {
        SpaceUnreadCountBox(space.unreadCounts, spaceTabUnreadBadgeOffset(compact)) {
            AbstractSpaceIcon(space = space, size = spaceTabIconSize(compact), shape = spaceTabIconShape(compact))
        }
    }
}

@Composable
private fun AbstractSpaceIcon(
    space: SpaceListDataSource.AbstractSpaceHierarchyItem?,
    size: Dp = Dimens.Inbox.spaceAvatar,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = Dimens.Inbox.spaceShape,
) {
    when(space) {
        is SpaceListDataSource.SpaceHierarchyItem -> AvatarImage(
            space.room.summary.info.avatarUrl?.let { MediaSource(it) },
            size = size,
            shape = shape,
            sessionId = space.room.sessionId,
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
        text = stringResource(Res.string.sc_space_all_rooms_title),
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
fun SpaceUnreadCountBox(unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?, offset: Dp, content: @Composable () -> Unit) {
    val mode = ScPrefs.SPACE_UNREAD_COUNTS.value()
    if (unreadCounts == null || mode == ScPrefs.SpaceUnreadCountMode.HIDE) {
        content()
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
            return
        }
    }
    Box {
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
    }
}

private fun spaceTabIconSize(compact: Boolean) = Dimens.Inbox.spaceAvatar
private fun spaceTabIconShape(compact: Boolean) = Dimens.Inbox.spaceShape
private fun spaceTabUnreadBadgeOffset(compact: Boolean) = 6.dp
