package chat.schildi.revenge.model.spaces

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import chat.schildi.matrixsdk.ROOM_ACCOUNT_DATA_SPACE_ORDER
import chat.schildi.matrixsdk.SpaceOrderSerializer
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.CombinedSessions
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.HardcodedStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.flatMerge
import chat.schildi.revenge.model.ScopedRoomKey
import chat.schildi.revenge.model.ScopedRoomSummary
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.MatrixSpaceChildInfo
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.nameless_space_fallback_title
import shire.composeapp.generated.resources.sc_pseudo_space_dms
import shire.composeapp.generated.resources.sc_pseudo_space_favorites
import shire.composeapp.generated.resources.sc_pseudo_space_groups
import shire.composeapp.generated.resources.sc_pseudo_space_invites
import shire.composeapp.generated.resources.sc_pseudo_space_notifications_short
import shire.composeapp.generated.resources.sc_pseudo_space_spaceless_groups_short
import shire.composeapp.generated.resources.sc_pseudo_space_spaceless_short
import shire.composeapp.generated.resources.sc_pseudo_space_unread
import shire.composeapp.generated.resources.sc_space_all_rooms_title
import timber.log.Timber

const val REAL_SPACE_ID_PREFIX = "s:"
const val PSEUDO_SPACE_ID_PREFIX = "p:"

private typealias ScopedSpaceId = ScopedRoomKey

private data class SpaceBuilderRoom(
    val client: MatrixClient,
    val summary: RoomSummary,
) {
    val id: ScopedSpaceId
        get() = ScopedSpaceId(client.sessionId, summary.roomId)
    fun toScopedRoomSummary() = ScopedRoomSummary(
        client.sessionId,
        summary,
    )
}

val RevengeSpaceListDataSource = SpaceListDataSource()

@Inject
class SpaceListDataSource(
    private val combinedSessions: CombinedSessions = UiState.combinedSessions,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
    private val sessionIdComparatorFlow: Flow<Comparator<SessionId>> = UiState.sessionIdComparator,
) {
    private val log = Logger.withTag("SpaceListDataSource")

    private val spaceComparatorFlow = sessionIdComparatorFlow.map { SpaceComparator(it) }

    private val _forceRebuildFlow = MutableStateFlow(System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allSpacesFlat = combinedSessions.flatMerge(
        map = { input ->
            input.client.roomListService.allSpaces.summaries.map {
                it.map {
                    SpaceBuilderRoom(input.client, it)
                }
            }
        },
        onUpdatedInput = { it ->
            it.forEach {
                log.v("Init for ${it.client.sessionId}")
                it.client.roomListService.allRooms.updateFilter(RoomListFilter.All(emptyList()))
                it.client.roomListService.allRooms.loadMore()
            }
        },
        merge = { it ->
            log.v("Merging room lists [${it.joinToString { it.size.toString() }}]")
            // No need to do sophisticated sorted merging at this time, before we built any hierarchy
            it.asIterable().flatten()
        },
    ).flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val accounts = combinedSessions.flatMerge(
        map = {
            it.client.userProfile
        },
        merge = {
            it.toList()
        }
    )

    val allSpacesHierarchical = combine(
        accounts,
        allSpacesFlat,
        scPreferencesStore.pseudoSpaceSettingsFlow(),
        spaceComparatorFlow,
        _forceRebuildFlow,
    ) { accounts, flatSpaces, settings, spaceComparator, _ ->
        Timber.d("Rebuilding space hierarchy for ${flatSpaces.size} spaces")
        buildSpaceHierarchy(accounts, flatSpaces, settings, spaceComparator)
    }.flowOn(Dispatchers.IO)

    private suspend fun buildSpaceHierarchy(
        accounts: List<MatrixUser>,
        spaceSummaries: List<SpaceBuilderRoom>,
        pseudoSpaceSettings: PseudoSpaceSettings,
        spaceComparator: SpaceComparator,
    ): List<AbstractSpaceHierarchyItem> {
        val pseudoSpaces = mutableListOf<PseudoSpaceItem>()
        pseudoSpaces.add(
            FavoritesPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_favorites),
                pseudoSpaceSettings.favorites
            )
        )
        pseudoSpaces.add(
            DmsPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_dms),
                pseudoSpaceSettings.dms
            )
        )
        pseudoSpaces.add(
            GroupsPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_groups),
                pseudoSpaceSettings.groups,
            )
        )
        val spacelessRooms = spaceSummaries.flatMap { it.summary.info.spaceChildren.map { it.roomId } }.toImmutableList()
        pseudoSpaces.add(
            SpacelessGroupsPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_spaceless_groups_short),
                pseudoSpaceSettings.spacelessGroups,
                spacelessRooms,
            )
        )
        pseudoSpaces.add(
            SpacelessPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_spaceless_short),
                pseudoSpaceSettings.spaceless,
                spacelessRooms,
                pseudoSpaceSettings.spacelessGroups
            )
        )
        pseudoSpaces.add(
            NotificationsPseudoSpaceItem(
                StringResourceHolder(
                    if (pseudoSpaceSettings.unread)
                        Res.string.sc_pseudo_space_notifications_short
                    else
                        Res.string.sc_pseudo_space_unread
                ),
                pseudoSpaceSettings.notifications,
                pseudoSpaceSettings.clientUnreadCounts
            )
        )
        pseudoSpaces.add(
            UnreadPseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_unread),
                pseudoSpaceSettings.unread,
                pseudoSpaceSettings.clientUnreadCounts
            )
        )
        pseudoSpaces.add(
            InvitePseudoSpaceItem(
                StringResourceHolder(Res.string.sc_pseudo_space_invites),
                pseudoSpaceSettings.invites,
            )
        )
        pseudoSpaces.addAll(
            accounts.map {
                SessionIdPseudoSpaceItem(
                    sessionId = it.userId,
                    avatarUrl = it.avatarUrl,
                    enabled = pseudoSpaceSettings.accounts,
                    name = HardcodedStringHolder(it.displayName ?: it.userId.value),
                )
            }
        )
        return pseudoSpaces + buildSpaceHierarchy(spaceSummaries, spaceComparator)
    }

    // Force rebuilding a space filter. Only a workaround until we can do proper listener to m.space.child state events...
    suspend fun forceRebuildSpaceFilter() {
        _forceRebuildFlow.emit(System.currentTimeMillis())
    }

    /**
     * Build the space hierarchy and avoid loops
     */
    private suspend fun buildSpaceHierarchy(
        spaceSummaries: List<SpaceBuilderRoom>,
        spaceComparator: SpaceComparator,
    ): List<AbstractSpaceHierarchyItem> {
        // Map spaceId -> list of child spaces
        val spaceHierarchyMap = HashMap<ScopedSpaceId, MutableList<Pair<MatrixSpaceChildInfo, SpaceBuilderRoom>>>()
        // Map spaceId -> list of regular child rooms
        val regularChildren = HashMap<ScopedSpaceId, MutableList<MatrixSpaceChildInfo>>()
        val rootSpaces = HashSet<SpaceBuilderRoom>(spaceSummaries)
        spaceSummaries.forEach { parentSpace ->
            parentSpace.summary.info.spaceChildren.forEach childLoop@{ spaceChild ->
                val childId = ScopedSpaceId(parentSpace.client.sessionId, RoomId(spaceChild.roomId))
                val child = spaceSummaries.find { it.id == childId }
                if (child == null) {
                    // Treat as regular child, since it doesn't appear to be a space (at least none known to us at this point)
                    regularChildren[parentSpace.id] =
                        regularChildren[parentSpace.id]?.apply { add(spaceChild) } ?: mutableListOf(spaceChild)
                    return@childLoop
                }
                rootSpaces.removeAll { it.summary.roomId.value == spaceChild.roomId }
                spaceHierarchyMap[parentSpace.id] = spaceHierarchyMap[parentSpace.id]?.apply {
                    add(Pair(spaceChild, child))
                } ?: mutableListOf(Pair(spaceChild, child))
            }
        }

        // Build the actual immutable recursive data structures that replicate the hierarchy
        return rootSpaces.map {
            val order = it.client.getRoomAccountData(it.summary.roomId, ROOM_ACCOUNT_DATA_SPACE_ORDER)
                ?.let { SpaceOrderSerializer.deserializeContent(it) }?.getOrNull()?.order
            createSpaceHierarchyItem(
                it,
                order,
                spaceComparator,
                spaceHierarchyMap,
                regularChildren
            )
        }.sortedWith(spaceComparator)
    }

    private fun createSpaceHierarchyItem(
        spaceSummary: SpaceBuilderRoom,
        order: String?,
        spaceComparator: SpaceComparator,
        hierarchy: HashMap<ScopedSpaceId, MutableList<Pair<MatrixSpaceChildInfo, SpaceBuilderRoom>>>,
        regularChildren: HashMap<ScopedSpaceId, MutableList<MatrixSpaceChildInfo>>,
        forbiddenChildren: List<ScopedSpaceId> = emptyList(),
    ): SpaceHierarchyItem {
        // Space children
        val children = hierarchy[spaceSummary.id]?.mapNotNull { (spaceChildInfo, child) ->
            if (child.id in forbiddenChildren) {
                Timber.w("Detected space loop: ${spaceSummary.summary.roomId} -> ${child.summary.roomId.value}")
                null
            } else {
                createSpaceHierarchyItem(
                    child,
                    spaceChildInfo.order,
                    spaceComparator,
                    hierarchy,
                    regularChildren,
                    forbiddenChildren + listOf(spaceSummary.id),
                )
            }
        }?.sortedWith(spaceComparator)?.toImmutableList() ?: persistentListOf()

        // Room children
        val directChildrenRooms = regularChildren[spaceSummary.id].orEmpty().map {
            ScopedRoomKey(spaceSummary.client.sessionId, RoomId(it.roomId))
        }

        return SpaceHierarchyItem(
            room = spaceSummary.toScopedRoomSummary(),
            order = order,
            spaces = children,
            directChildren = directChildrenRooms.toImmutableSet(),
            flattenedRooms = (
                // All direct + indirect children rooms
                directChildrenRooms + children.flatMap { it.flattenedRooms }
                ).toImmutableSet(),
        )
    }

    @Immutable
    sealed interface AbstractSpaceHierarchyItem {
        val sessionIds: ImmutableList<SessionId>?
        val name: ComposableStringHolder
        val selectionId: String
        val spaces: ImmutableList<SpaceHierarchyItem>
        val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts?
        val enabled: Boolean
        fun applyFilter(rooms: List<ScopedRoomSummary>): ImmutableList<ScopedRoomSummary>
        fun canHide(spaceUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts): Boolean = false
        // To add additional space information independent of the actual space hierarchy, use separate flows to enrich
        fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?): AbstractSpaceHierarchyItem
    }

    @Immutable
    data class SpaceHierarchyItem(
        val room: ScopedRoomSummary,
        val order: String?,
        override val spaces: ImmutableList<SpaceHierarchyItem>,
        val directChildren: ImmutableSet<ScopedRoomKey>,
        val flattenedRooms: ImmutableSet<ScopedRoomKey>,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
        val mergedRooms: ImmutableList<ScopedRoomSummary> = persistentListOf(),
    ) : AbstractSpaceHierarchyItem {
        override val name = room.summary.info.name?.toStringHolder() ?: StringResourceHolder(Res.string.nameless_space_fallback_title)
        override val selectionId = "$REAL_SPACE_ID_PREFIX{${sessionIds.sortedBy(SessionId::value).joinToString(separator = ";")}}:${room.summary.roomId.value}"
        override val enabled = true

        override val sessionIds: ImmutableList<SessionId>
            get() = persistentListOf(room.sessionId, *mergedRooms.map { it.sessionId }.toTypedArray())

        override fun enrich(
            getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?
        ): AbstractSpaceHierarchyItem = copy(
            unreadCounts = getUnreadCounts(this),
            spaces = spaces.map { it.enrich(getUnreadCounts) as SpaceHierarchyItem }.toImmutableList(),
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { flattenedRooms.contains(it.key) }.toImmutableList()
    }

    sealed interface PseudoSpaceIconSource {
        data class Icon(val icon: ImageVector):  PseudoSpaceIconSource
        data class Avatar(val url: String, val sessionId: SessionId): PseudoSpaceIconSource
    }

    @Immutable
    abstract class PseudoSpaceItem(
        val id: String,
        open val icon: PseudoSpaceIconSource,
    ) : AbstractSpaceHierarchyItem {
        override val sessionIds: ImmutableList<SessionId>? = null
        override val selectionId = "$PSEUDO_SPACE_ID_PREFIX$id"
        override val spaces = persistentListOf<SpaceHierarchyItem>()
    }

    @Immutable
    data class FavoritesPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "fav",
        Icons.Default.Star.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { it.summary.info.isFavorite }.toImmutableList()
    }

    @Immutable
    data class DmsPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "dm",
        Icons.Default.Person.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { it.summary.info.isDirect }.toImmutableList()
    }

    @Immutable
    data class GroupsPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "group",
        Icons.Default.Groups.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { !it.summary.info.isDirect }.toImmutableList()
    }

    @Immutable
    data class SpacelessGroupsPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        val excludedRooms: ImmutableList<String>,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "spaceless/group",
        Icons.Default.Tag.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { !it.summary.info.isDirect && !excludedRooms.contains(it.summary.roomId.value) }.toImmutableList()
    }

    @Immutable
    data class SpacelessPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        val excludedRooms: ImmutableList<String>,
        val conflictsWithSpacelessGroups: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "spaceless",
        if (conflictsWithSpacelessGroups) Icons.Default.Rocket.spaceIcon() else Icons.Default.Tag.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>) =
            rooms.filter { !excludedRooms.contains(it.summary.roomId.value) }.toImmutableList()
    }

    @Immutable
    data class NotificationsPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        val clientUnreadCounts: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "notif",
        Icons.Default.Notifications.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>): ImmutableList<ScopedRoomSummary> {
            return if (clientUnreadCounts) {
                rooms.filter {
                    it.summary.info.numUnreadNotifications > 0 || it.summary.info.numUnreadMentions > 0
                        || it.summary.info.isMarkedUnread || it.summary.info.currentUserMembership == CurrentUserMembership.INVITED
                }
            } else {
                rooms.filter {
                    it.summary.info.notificationCount > 0 || it.summary.info.highlightCount > 0 || it.summary.info.numUnreadMentions > 0
                        || it.summary.info.isMarkedUnread || it.summary.info.currentUserMembership == CurrentUserMembership.INVITED
                }
            }.toImmutableList()
        }
        override fun canHide(spaceUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts): Boolean =
            spaceUnreadCounts.markedUnreadChats == 0L && spaceUnreadCounts.notifiedChats == 0L
    }

    @Immutable
    data class UnreadPseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        val clientUnreadCounts: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "unread",
        Icons.Default.RemoveRedEye.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>): ImmutableList<ScopedRoomSummary> {
            return if (clientUnreadCounts) {
                rooms.filter { it.summary.info.numUnreadMessages > 0 || it.summary.info.isMarkedUnread || it.summary.info.currentUserMembership == CurrentUserMembership.INVITED }
            } else {
                rooms.filter { it.summary.info.unreadCount > 0 || it.summary.info.isMarkedUnread || it.summary.info.currentUserMembership == CurrentUserMembership.INVITED }
            }.toImmutableList()
        }
        override fun canHide(spaceUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts): Boolean =
            spaceUnreadCounts.markedUnreadChats == 0L && spaceUnreadCounts.notifiedChats == 0L && spaceUnreadCounts.unreadChats == 0L
    }

    @Immutable
    data class InvitePseudoSpaceItem(
        override val name: StringResourceHolder,
        override val enabled: Boolean,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "invites",
        Icons.Default.MeetingRoom.spaceIcon(),
    ) {
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun applyFilter(rooms: List<ScopedRoomSummary>): ImmutableList<ScopedRoomSummary> =
            rooms.filter { it.summary.info.currentUserMembership == CurrentUserMembership.INVITED }.toImmutableList()
        override fun canHide(spaceUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts): Boolean =
            spaceUnreadCounts.inviteCount == 0L
    }

    @Immutable
    data class SessionIdPseudoSpaceItem(
        val sessionId: SessionId,
        val avatarUrl: String?,
        override val enabled: Boolean,
        override val name: ComposableStringHolder,
        override val unreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts? = null,
    ) : PseudoSpaceItem(
        "account/$sessionId",
        avatarUrl?.let { PseudoSpaceIconSource.Avatar(it, sessionId) } ?: Icons.Default.AccountCircle.spaceIcon(),
    ) {
        override val sessionIds = persistentListOf(sessionId)
        override fun enrich(getUnreadCounts: (AbstractSpaceHierarchyItem) -> SpaceAggregationDataSource.SpaceUnreadCounts?) = copy(
            unreadCounts = getUnreadCounts(this)
        )
        override fun canHide(spaceUnreadCounts: SpaceAggregationDataSource.SpaceUnreadCounts) = spaceUnreadCounts.isEmptySpace
        override fun applyFilter(rooms: List<ScopedRoomSummary>): ImmutableList<ScopedRoomSummary> =
            rooms.filter { it.sessionId == sessionId }.toImmutableList()
    }

    data class PseudoSpaceSettings(
        val favorites: Boolean,
        val dms: Boolean,
        val groups: Boolean,
        val spacelessGroups: Boolean,
        val spaceless: Boolean,
        val notifications: Boolean,
        val unread: Boolean,
        val invites: Boolean,
        val accounts: Boolean,
        val clientUnreadCounts: Boolean,
    ) {
        fun hasSpaceIndependentPseudoSpace() = favorites || dms || groups || notifications || unread || invites
    }
}

private fun ImageVector.spaceIcon() = SpaceListDataSource.PseudoSpaceIconSource.Icon(this)

fun ScPreferencesStore.pseudoSpaceSettingsFlow(): Flow<SpaceListDataSource.PseudoSpaceSettings> {
    return combinedSettingFlow { lookup ->
        SpaceListDataSource.PseudoSpaceSettings(
            favorites = ScPrefs.PSEUDO_SPACE_FAVORITES.safeLookup(lookup),
            dms = ScPrefs.PSEUDO_SPACE_DMS.safeLookup(lookup),
            groups = ScPrefs.PSEUDO_SPACE_GROUPS.safeLookup(lookup),
            spacelessGroups = ScPrefs.PSEUDO_SPACE_SPACELESS_GROUPS.safeLookup(lookup),
            spaceless = ScPrefs.PSEUDO_SPACE_SPACELESS.safeLookup(lookup),
            notifications = ScPrefs.PSEUDO_SPACE_NOTIFICATIONS.safeLookup(lookup),
            unread = ScPrefs.PSEUDO_SPACE_UNREAD.safeLookup(lookup),
            invites = ScPrefs.PSEUDO_SPACE_INVITES.safeLookup(lookup),
            accounts = ScPrefs.PSEUDO_SPACE_ACCOUNTS.safeLookup(lookup),
            clientUnreadCounts = ScPrefs.CLIENT_GENERATED_UNREAD_COUNTS.safeLookup(lookup),
        )
    }
}

fun List<SpaceListDataSource.AbstractSpaceHierarchyItem>.resolveSelection(selection: List<String>): SpaceListDataSource.AbstractSpaceHierarchyItem? {
    var space: SpaceListDataSource.AbstractSpaceHierarchyItem? = null
    var spaceList = this
    selection.forEach { spaceId ->
        space = spaceList.find { it.selectionId == spaceId }
        if (space == null) {
            return null
        }
        spaceList = (space as? SpaceListDataSource.SpaceHierarchyItem)?.spaces.orEmpty()
    }
    return space
}

fun List<SpaceListDataSource.AbstractSpaceHierarchyItem>.findInHierarchy(condition: (SpaceListDataSource.AbstractSpaceHierarchyItem) -> Boolean): List<String>? {
    forEach { space ->
        if (condition(space)) {
            return listOf(space.selectionId)
        }
        if (space is SpaceListDataSource.SpaceHierarchyItem) {
            space.spaces.findInHierarchy(condition)?.let {
                return listOf(space.selectionId) + it
            }
        }
    }
    return null
}

fun isSpaceFilterActive(selection: List<String>): Boolean {
    // No need to resolveSelection() the whole hierarchy, checking the first selection is enough
    return selection.firstOrNull()?.startsWith(REAL_SPACE_ID_PREFIX) == true
}

@Composable
fun List<SpaceListDataSource.AbstractSpaceHierarchyItem>.resolveSpaceName(selection: List<String>): ComposableStringHolder? {
    // if this.isEmpty(), spaces are disabled, in which case we want to return null
    if (isEmpty()) {
        return null
    }
    return resolveSelection(selection)?.name ?: StringResourceHolder(Res.string.sc_space_all_rooms_title)
}

fun ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>.filterByVisible(
    selection: ImmutableList<String>?,
    filterByUnread: Boolean,
): ImmutableList<SpaceListDataSource.AbstractSpaceHierarchyItem> {
    val currentSelection = selection?.firstOrNull()
    return if (filterByUnread) {
        filter { space ->
            space.selectionId == currentSelection ||
                    space.enabled && space.unreadCounts?.let { space.canHide(it) } != true
        }
    } else {
        filter { space ->
            space.selectionId == currentSelection || space.enabled
        }
    }.toImmutableList()
}

fun List<SpaceListDataSource.AbstractSpaceHierarchyItem>.flattenWithParents(
    result: MutableList<Pair<SpaceListDataSource.AbstractSpaceHierarchyItem, MutableList<SpaceListDataSource.AbstractSpaceHierarchyItem>>> = mutableListOf(),
    currentParent: SpaceListDataSource.AbstractSpaceHierarchyItem? = null,
): List<Pair<SpaceListDataSource.AbstractSpaceHierarchyItem, List<SpaceListDataSource.AbstractSpaceHierarchyItem>>> {
    forEach { space ->
        val previouslyAdded = result.find { it.first.selectionId == space.selectionId }
        if (previouslyAdded != null) {
            if (currentParent != null &&
                previouslyAdded.second.none { it.selectionId == currentParent.selectionId }
            ) {
                previouslyAdded.second.add(currentParent)
            }
            return@forEach
        }
        result.add(Pair(space, currentParent?.let { mutableListOf(it) } ?: mutableListOf()))
        space.spaces.flattenWithParents(
            result = result,
            currentParent = space,
        )
    }
    return result
}

fun <T: SpaceListDataSource.AbstractSpaceHierarchyItem>List<T>.filterHierarchical(
    condition: (SpaceListDataSource.AbstractSpaceHierarchyItem) -> Boolean
): List<T> {
    return this.mapNotNull {
        if (!condition(it)) {
            null
        } else if (it is SpaceListDataSource.SpaceHierarchyItem) {
            it.copy(
                spaces = it.spaces.filterHierarchical(condition).toImmutableList(),
            ) as? T
        } else {
            it
        }
    }
}
