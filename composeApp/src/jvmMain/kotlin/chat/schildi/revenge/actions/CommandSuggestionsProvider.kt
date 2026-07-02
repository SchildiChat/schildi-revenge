package chat.schildi.revenge.actions

import chat.schildi.lib.preferences.ScPrefContainer
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.findPreference
import chat.schildi.lib.preferences.forEachPreference
import chat.schildi.lib.preferences.forEachPreferenceOrContainer
import chat.schildi.revenge.UiState
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.HardcodedStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.config.keybindings.ALLOWED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentContextBased
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.ActionArgumentRepeatable
import chat.schildi.revenge.config.keybindings.ActionRoomNotificationSetting
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.SUGGESTED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import chat.schildi.revenge.config.keybindings.findAll
import chat.schildi.revenge.config.keybindings.maxArgsSize
import chat.schildi.revenge.config.keybindings.minArgsSize
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.RevengeRoomListDataSource
import chat.schildi.revenge.model.RoomListDataSource
import chat.schildi.revenge.model.account.AccountComparator
import chat.schildi.revenge.model.spaces.RevengeSpaceListDataSource
import chat.schildi.revenge.model.spaces.SpaceBuilderRoom
import chat.schildi.revenge.model.spaces.SpaceListDataSource
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import shire.res.generated.resources.Res
import shire.res.generated.resources.command_suggestion_title_and_hint

enum class CurrentCommandValidity {
    INCOMPLETE,
    INVALID,
    VALID,
}

data class CommandSuggestion(
    val value: String,
    val hint: ComposableStringHolder? = null,
)

data class CommandSuggestionsState(
    val query: String,
    val validity: CurrentCommandValidity,
    val currentSuggestions: ImmutableList<CommandSuggestion>,
)

private val BOOLEAN_SUGGESTIONS = listOf("true", "false")
private val EVENT_TYPE_SUGGESTIONS = listOf(
    "m.room.message",
    "m.sticker",
)

class CommandSuggestionsProvider(
    queryFlow: Flow<KeyboardActionMode.Command?>,
    val commandParser: CommandParser,
    private val scope: CoroutineScope,
    private val userIdSuggestionsProvider: UserIdSuggestionsProvider?,
    private val roomContextSuggestionsProvider: RoomContextSuggestionsProvider?,
    private val roomListDataSource: RoomListDataSource = RevengeRoomListDataSource,
    private val spaceListDataSource: SpaceListDataSource = RevengeSpaceListDataSource,
) {
    private val log = Logger.withTag("CmdSuggestions")

    private val allCommands = commandParser.getAllPossibleCommandsSorted()
    private val allCommandSuggestions = allCommands.map { it.first }

    val accounts = UiState.combinedSessions.flatMergeCombinedWith(
        map = { it, _ ->
            it.client.userProfile
        },
        merge = { it, comparator ->
            it.sortedWith(AccountComparator(comparator) { it.userId })
                .map { CommandSuggestion(it.userId.value, it.displayName?.toStringHolder()) }
        },
        onEmpty = { emptyList() },
        other = UiState.sessionIdComparator,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val scopedRoomIdSuggestions = roomListDataSource.allRooms.map {
        it.map {
            Pair(
                it.sessionId,
                CommandSuggestion(
                    it.summary.roomId.value,
                    it.summary.info.name?.toStringHolder()
                )
            )
        }.distinct()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val scopedRoomAliasSuggestions = roomListDataSource.allRooms.map {
        it.flatMap { room ->
            room.summary.info.aliases.map { alias ->
                Pair(
                    room.sessionId,
                    CommandSuggestion(
                        alias.value,
                        room.summary.info.name?.toStringHolder(),
                    )
                )
            }
        }.distinct()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val scopedUserIdDmsSuggestions = roomListDataSource.allRooms.map {
        it.mapNotNull { room ->
            if (room.summary.isDm) {
                room.summary.info.heroes
                    .filter { it.userId != room.sessionId }
                    .takeIf { it.size == 1 }
                    ?.firstOrNull()?.let { user ->
                        Pair(
                            room.sessionId,
                            CommandSuggestion(
                                user.userId.value,
                                user.displayName?.toStringHolder(),
                            )
                        )
                    }
            } else {
                null
            }
        }.distinct()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val globalUserIdSuggestions = scopedUserIdDmsSuggestions.map {
        it.map {
            it.second
        }.distinctBy { it.value }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val spaceSuggestionSource = spaceListDataSource.allSpacesFlat
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val pseudoSpaceSuggestions = spaceListDataSource.pseudoSpaceIdSuggestions
        .map { it.toSuggestionsWithoutHint() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val prefKeySuggestions = buildList {
        ScPrefs.rootPrefs.forEachPreference {
            if (it.key != null) {
                val summaryRes = it.summaryRes
                val hint = if (summaryRes != null) {
                    StringResourceHolder(
                        Res.string.command_suggestion_title_and_hint,
                        it.titleRes.toStringHolder(),
                        summaryRes.toStringHolder(),
                    )
                } else {
                    it.titleRes.toStringHolder()
                }
                add(CommandSuggestion(it.sKey, hint))
            }
        }
    }

    private val prefCategorySuggestions = buildList {
        ScPrefs.rootPrefs.forEachPreferenceOrContainer {
            val containerKey = (it as? ScPrefContainer)?.sKey
            if (containerKey != null) {
                val summaryRes = it.summaryRes
                val hint = if (summaryRes != null) {
                    StringResourceHolder(
                        Res.string.command_suggestion_title_and_hint,
                        it.titleRes.toStringHolder(),
                        summaryRes.toStringHolder(),
                    )
                } else {
                    it.titleRes.toStringHolder()
                }
                add(CommandSuggestion(containerKey, hint))
            }
        }
    }

    private val userIdInRoomSuggestions = userIdSuggestionsProvider?.userIdInRoomSuggestions
        ?.stateIn(scope, SharingStarted.Eagerly, null)

    private val roomStateEventSuggestions = roomContextSuggestionsProvider?.stateEventSuggestions

    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestionState = queryFlow.mapLatest { mode ->
        mode ?: return@mapLatest null
        suggestForCommandString(mode.query.text, mode.impliedArguments)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun suggestForCommandString(
        query: String,
        impliedContext: List<Pair<ActionArgument, String>>,
    ): CommandSuggestionsState? {
        val (cmd, args) = commandParser.parseCommandString(query) ?: return null
        return if (args.isEmpty() && !query.endsWith(" ")) {
            val isValidCommand = cmd in allCommandSuggestions && commandParser
                .getPossibleActions(cmd)
                .any { it.first.minArgsSize() == 0 }
            val currentSuggestions = allCommands.filterValidSuggestionsFor(cmd) { it.first }
                // Don't suggest multiple aliases for a given action
                .distinctBy { it.second }
                .map { CommandSuggestion(it.first, it.second.description()) }
                // Deduplicate multiple actions with same command alias in different contexts
                .distinct()
            CommandSuggestionsState(
                query = query,
                validity = if (isValidCommand)
                    CurrentCommandValidity.VALID
                else if (currentSuggestions.isEmpty())
                    CurrentCommandValidity.INVALID
                else
                    CurrentCommandValidity.INCOMPLETE,
                currentSuggestions = currentSuggestions.toPersistentList(),
            )
        } else {
            val possibleActions = commandParser.getPossibleActions(cmd)
            // Fetching state events is only done on demand, since it involves network (was easier to hook up in
            // the Rust SDK then reading from storage, but TODO maybe we can access the Rust SDK state store without
            // network in the future
            if (roomContextSuggestionsProvider != null && possibleActions.any { (it, _) ->
                it.args.any { arg ->
                    listOf(
                        ActionArgumentPrimitive.StateEventType,
                        ActionArgumentPrimitive.NonEmptyStateKey,
                        ActionArgumentPrimitive.RoomName,
                        ActionArgumentPrimitive.RoomTopic
                    ).any { arg.canHold(it) }
                }
            }) {
                roomContextSuggestionsProvider.prefetchState(scope)
            }
            val (currentValidity, argSuggestions) = when {
                possibleActions.isEmpty() -> Pair(CurrentCommandValidity.INVALID, emptyList())
                else -> {
                    val argumentsChecked = possibleActions.map { checkArguments(it.first, args, impliedContext) }
                    val validity = when {
                        argumentsChecked.any { it == null } -> CurrentCommandValidity.VALID
                        argumentsChecked.any { it is ActionResult.MissingParameters } ->
                            CurrentCommandValidity.INCOMPLETE
                        else -> {
                            // Check again with only arguments that are not being written right now, so if the command
                            // is only invalid because of missing arguments we don't mark it as invalid
                            val stableArgs = if (args.isEmpty()) args else args.subList(0, args.size - 1)
                            if (possibleActions.any {
                                val check = checkArguments(it.first, stableArgs, impliedContext)
                                check is ActionResult.MissingParameters || (check == null && it.first.maxArgsSize() > stableArgs.size)
                            }) {
                                CurrentCommandValidity.INCOMPLETE
                            } else {
                                CurrentCommandValidity.INVALID
                            }
                        }
                    }
                    if (validity == CurrentCommandValidity.INVALID) {
                        Pair(validity, emptyList())
                    } else {
                        val currentArgIndex = if (query.endsWith(" ")) args.size else args.size - 1
                        val argSuggestions = possibleActions.flatMap {
                            val argDef = it.first.args.getOrNull(currentArgIndex)
                            if (argDef == null) {
                                emptyList()
                            } else {
                                val argContext = it.first.args.take(currentArgIndex).zip(args) + impliedContext
                                suggestFor(argDef, argContext, args.getOrNull(currentArgIndex) ?: "")
                            }
                        }.distinct()
                        Pair(validity, argSuggestions)
                    }
                }
            }
            CommandSuggestionsState(
                query = query,
                validity = currentValidity,
                currentSuggestions = argSuggestions.toPersistentList(),
            )
        }
    }

    private fun suggestFor(
        arg: ActionArgument,
        context: CommandArgContext,
        prefix: String,
    ): List<CommandSuggestion> {
        val impliedValues = arg.possiblePrimitives(context).flatMap {
            context.findAll(it)
        }.filterValidSuggestionsFor(prefix) { it }.distinct()
        val suggestionList = suggestPrimaryFor(arg, context, prefix).takeIf { it.isNotEmpty() }
            ?: suggestSecondaryFor(arg, context, prefix)
        val impliedSuggestions = impliedValues.map { implied ->
            suggestionList.find { it.value == implied } ?: CommandSuggestion(implied)
        }
        return impliedSuggestions + suggestionList.filter { it.value !in impliedValues }
    }

    private fun suggestPrimaryFor(
        arg: ActionArgument,
        context: CommandArgContext,
        query: String,
    ): List<CommandSuggestion> = when (arg) {
        is ActionArgumentPrimitive -> {
            when (arg) {
                ActionArgumentPrimitive.Boolean -> BOOLEAN_SUGGESTIONS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.UserId -> {
                    val inRoomSuggestions = userIdInRoomSuggestions?.value.orEmpty().map { it.toCommandSuggestion() }
                    if (inRoomSuggestions.isNotEmpty()) {
                        (inRoomSuggestions + globalUserIdSuggestions.value).distinctBy { it.value }
                    } else {
                        globalUserIdSuggestions.value
                    }
                }
                ActionArgumentPrimitive.UserIdInRoom -> userIdInRoomSuggestions?.value?.mapNotNull { suggestion ->
                    // Depending on the command, "invite" or "knock" states may also be considered as "in the room";
                    // though leave and ban usually aren't.
                    if (suggestion.membership in listOf(RoomMembershipState.LEAVE, RoomMembershipState.BAN)) {
                        null
                    } else {
                        suggestion.toCommandSuggestion()
                    }
                } ?: emptyList()
                ActionArgumentPrimitive.UserIdNotInRoom -> {
                    val inRoom = userIdInRoomSuggestions?.value?.map { it.userId.value }.orEmpty().toSet()
                    // Global suggestions
                    globalUserIdSuggestions.value.filter { it.value !in inRoom } +
                            // Users that were historically in the room but left are also worth suggesting
                            userIdInRoomSuggestions?.value?.mapNotNull { suggestion ->
                                if (suggestion.membership != RoomMembershipState.JOIN) {
                                    suggestion.toCommandSuggestion()
                                } else {
                                    null
                                }
                            }.orEmpty()
                }
                ActionArgumentPrimitive.ExistingDmUserId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        emptyList()
                    } else {
                        scopedUserIdDmsSuggestions.value.mapNotNull {
                            if (it.first.value in sessionIds) {
                                it.second
                            } else {
                                null
                            }
                        }
                    }
                }
                ActionArgumentPrimitive.SessionId -> accounts.value
                ActionArgumentPrimitive.RoomId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        scopedRoomIdSuggestions.value.map { it.second }.distinct()
                    } else {
                        scopedRoomIdSuggestions.value.mapNotNull {
                            if (it.first.value in sessionIds) {
                                it.second
                            } else {
                                null
                            }
                        }
                    }
                }
                ActionArgumentPrimitive.RoomAlias -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        scopedRoomAliasSuggestions.value.map { it.second }.distinct()
                    } else {
                        scopedRoomAliasSuggestions.value.mapNotNull {
                            if (it.first.value in sessionIds) {
                                it.second
                            } else {
                                null
                            }
                        }
                    }
                }
                ActionArgumentPrimitive.SettingKey -> prefKeySuggestions
                ActionArgumentPrimitive.SettingCategory -> prefCategorySuggestions
                ActionArgumentPrimitive.DestinationName,
                ActionArgumentPrimitive.NavigatableDestination ->
                    SUGGESTED_DESTINATION_STRINGS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.SettingValue -> {
                    val settingKeys = context.findAll(ActionArgumentPrimitive.SettingKey)
                    if (settingKeys.isEmpty()) {
                        emptyList()
                    } else {
                        settingKeys.flatMap { sKey ->
                            val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
                            pref?.autoSuggestionValues()?.toSuggestionsWithoutHint().orEmpty()
                        }
                    }
                }
                ActionArgumentPrimitive.RoomNotificationSetting -> {
                    ActionRoomNotificationSetting.entries.map { it.name }.toSuggestionsWithoutHint()
                }
                ActionArgumentPrimitive.SpaceId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        spaceSuggestionSource.value.toSuggestions()
                    } else {
                        spaceSuggestionSource.value.filter { it.id.sessionId.value in sessionIds }.toSuggestions()
                    }
                }
                ActionArgumentPrimitive.ParentSpaceId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    val roomIds = context.findAll(ActionArgumentPrimitive.RoomId)
                    spaceSuggestionSource.value.filter {
                        (sessionIds.isEmpty() || it.id.sessionId.value in sessionIds) &&
                                (roomIds.isEmpty() || it.summary.info.spaceChildren.any { it.roomId in roomIds })
                    }.toSuggestions()
                }
                ActionArgumentPrimitive.NonParentSpaceId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    val roomIds = context.findAll(ActionArgumentPrimitive.RoomId)
                    spaceSuggestionSource.value.filter {
                        (sessionIds.isEmpty() || it.id.sessionId.value in sessionIds) &&
                                (roomIds.isEmpty() || it.summary.info.spaceChildren.all { it.roomId !in roomIds })
                    }.toSuggestions()
                }
                ActionArgumentPrimitive.PseudoSpaceId -> pseudoSpaceSuggestions.value
                ActionArgumentPrimitive.Text,
                ActionArgumentPrimitive.Reason,
                ActionArgumentPrimitive.Integer,
                ActionArgumentPrimitive.PositiveOffset,
                ActionArgumentPrimitive.PowerLevel,
                ActionArgumentPrimitive.Index,
                ActionArgumentPrimitive.SessionIndex,
                ActionArgumentPrimitive.EventId,
                ActionArgumentPrimitive.ThreadId,
                ActionArgumentPrimitive.SpaceSelectionId,
                ActionArgumentPrimitive.SpaceIndex,
                ActionArgumentPrimitive.UserName,
                ActionArgumentPrimitive.Mxc,
                ActionArgumentPrimitive.RoomIdNotJoined,
                ActionArgumentPrimitive.RoomAliasNotJoined,
                ActionArgumentPrimitive.ServerName,
                ActionArgumentPrimitive.SpaceOrder,
                ActionArgumentPrimitive.MatrixLink,
                ActionArgumentPrimitive.MatrixToLink,
                ActionArgumentPrimitive.SchildiChatLegacyLink,
                ActionArgumentPrimitive.Json,
                ActionArgumentPrimitive.AccountDataType,
                ActionArgumentPrimitive.RoomAccountDataType,
                ActionArgumentPrimitive.Empty -> emptyList()
                ActionArgumentPrimitive.RoomName -> roomContextSuggestionsProvider?.roomInfo?.value?.name.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.RoomTopic -> roomContextSuggestionsProvider?.roomInfo?.value?.topic.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.EventType -> EVENT_TYPE_SUGGESTIONS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.StateEventType -> roomStateEventSuggestions?.value?.toStateEventTypeSuggestions().orEmpty()
                ActionArgumentPrimitive.NonEmptyStateKey -> {
                    val eventTypes = context.findAll(ActionArgumentPrimitive.StateEventType)
                    roomStateEventSuggestions?.value?.toStateEventKeySuggestions(eventTypes).orEmpty()
                }
                ActionArgumentPrimitive.SpaceCatchAllMode -> SpaceCatchAllMode.entries.map { it.name }.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.FocusRole -> FocusRole.entries.map { it.name }.toSuggestionsWithoutHint()
            }.filterValidSuggestionsFor(query, arg).distinct()
        }
        is ActionArgumentAnyOf -> arg.arguments.flatMap { suggestPrimaryFor(it, context, query) }
        is ActionArgumentOptional -> suggestPrimaryFor(arg.argument, context, query)
        is ActionArgumentRepeatable -> suggestPrimaryFor(arg.argument, context, query)
        is ActionArgumentContextBased -> suggestPrimaryFor(arg.getFor(context), context, query)
    }

    // If we have less preferred but still valid suggestions
    private fun suggestSecondaryFor(
        arg: ActionArgument,
        context: CommandArgContext,
        query: String,
    ): List<CommandSuggestion> = when (arg) {
        is ActionArgumentPrimitive -> {
            when (arg) {
                ActionArgumentPrimitive.NavigatableDestination ->
                    ALLOWED_DESTINATION_STRINGS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.RoomId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        // Already suggested everything as primary suggestion
                        emptyList()
                    } else {
                        // Now we didn't find the room ID for this session, but we can still search for the others
                        scopedRoomIdSuggestions.value.map { it.second }
                    }
                }
                ActionArgumentPrimitive.RoomNotificationSetting -> {
                    ActionRoomNotificationSetting.entries.flatMap { it.aliases }.toSuggestionsWithoutHint()
                }
                else -> emptyList()
            }.filterValidSuggestionsFor(query, arg).distinct()
        }
        is ActionArgumentAnyOf -> arg.arguments.flatMap { suggestSecondaryFor(it, context, query) }
        is ActionArgumentOptional -> suggestSecondaryFor(arg.argument, context, query)
        is ActionArgumentRepeatable -> suggestSecondaryFor(arg.argument, context, query)
        is ActionArgumentContextBased -> suggestSecondaryFor(arg.getFor(context), context, query)
    }

    fun clear() {
        scope.cancel("Canceled on clear request")
    }

    fun <T>List<T>.filterValidSuggestionsFor(
        query: String,
        arg: ActionArgumentPrimitive? = null,
        selectHint: (T) -> String? = { null },
        select: (T) -> String,
    ): List<T> {
        val queryLower = query.lowercase()
        return filter {
            // Sometimes startsWith(), sometimes contains() makes more sense
            when (arg) {
                ActionArgumentPrimitive.SettingKey -> select(it).lowercase().contains(queryLower)
                ActionArgumentPrimitive.SpaceId,
                ActionArgumentPrimitive.ParentSpaceId,
                ActionArgumentPrimitive.NonParentSpaceId,
                ActionArgumentPrimitive.RoomId -> select(it).lowercase().startsWith(queryLower) ||
                        selectHint(it)?.lowercase()?.contains(queryLower) == true
                else -> select(it).lowercase().contains(queryLower)
            }
        }
    }
    fun List<CommandSuggestion>.filterValidSuggestionsFor(query: String, arg: ActionArgumentPrimitive?) =
        filterValidSuggestionsFor(query, arg, select = { it.value }, selectHint = { (it.hint as? HardcodedStringHolder)?.value })
}

fun List<String>.toSuggestionsWithoutHint() = map { CommandSuggestion(it, null) }
fun String?.toSuggestionsWithoutHint() = if (this == null) emptyList() else listOf(CommandSuggestion(this, null))

fun List<StateEventCompletionSnapshot>.toStateEventTypeSuggestions() =
    map { it.eventType }.distinct().toSuggestionsWithoutHint()
fun List<StateEventCompletionSnapshot>.toStateEventKeySuggestions(eventTypes: List<String>) =
    filter { it.eventType in eventTypes && it.stateKey.isNotEmpty() }.map { it.stateKey }.distinct().toSuggestionsWithoutHint()

fun List<SpaceBuilderRoom>.toSuggestions() =
    distinctBy { it.id.roomId }.map {
        CommandSuggestion(
            it.summary.roomId.value,
            it.summary.info.name?.toStringHolder()
        )
    }
