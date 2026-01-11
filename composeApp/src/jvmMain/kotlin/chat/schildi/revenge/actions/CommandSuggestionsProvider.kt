package chat.schildi.revenge.actions

import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.findPreference
import chat.schildi.preferences.forEachPreference
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.HardcodedStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.ALLOWED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentContextBased
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.SUGGESTED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.findAll
import chat.schildi.revenge.config.keybindings.minArgsSize
import chat.schildi.revenge.flatMergeCombinedWith
import chat.schildi.revenge.model.RevengeRoomListDataSource
import chat.schildi.revenge.model.RoomListDataSource
import chat.schildi.revenge.model.account.AccountComparator
import co.touchlab.kermit.Logger
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
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.command_suggestion_title_and_hint

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

    private val scopedRoomSuggestions = roomListDataSource.allRooms.map {
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

    private val userIdInRoomSuggestions = userIdSuggestionsProvider?.userIdInRoomSuggestions
        ?.stateIn(scope, SharingStarted.Eagerly, null)

    private val roomStateEventSuggestions = roomContextSuggestionsProvider?.stateEventSuggestions

    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestionState = queryFlow.mapLatest { mode ->
        mode ?: return@mapLatest null
        suggestForCommandString(mode.query.text)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun suggestForCommandString(query: String): CommandSuggestionsState? {
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
                it.args.any {
                    it == ActionArgumentPrimitive.StateEventType || it == ActionArgumentPrimitive.NonEmptyStateKey
                }
            }) {
                roomContextSuggestionsProvider.prefetchState(scope)
            }
            val (currentValidity, argSuggestions) = when {
                possibleActions.isEmpty() -> Pair(CurrentCommandValidity.INVALID, emptyList())
                else -> {
                    val argumentsChecked = possibleActions.map { checkArguments(it.first, args) }
                    val validity = when {
                        argumentsChecked.any { it == null } -> CurrentCommandValidity.VALID
                        argumentsChecked.any { it is ActionResult.MissingParameters } ->
                            CurrentCommandValidity.INCOMPLETE
                        else -> {
                            // Check again with only arguments that are not being written right now, so if the command
                            // is only invalid because of missing arguments we don't mark it as invalid
                            val stableArgs = if (args.isEmpty()) args else args.subList(0, args.size - 1)
                            if (possibleActions.any {
                                checkArguments(it.first, stableArgs) is ActionResult.MissingParameters
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
                                val argContext = it.first.args.take(currentArgIndex).zip(args)
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
        return suggestPrimaryFor(arg, context, prefix).takeIf { it.isNotEmpty() }
            ?: suggestSecondaryFor(arg, context, prefix)
    }

    private fun suggestPrimaryFor(
        arg: ActionArgument,
        context: CommandArgContext,
        query: String,
    ): List<CommandSuggestion> = when (arg) {
        is ActionArgumentPrimitive -> {
            when (arg) {
                ActionArgumentPrimitive.Boolean -> BOOLEAN_SUGGESTIONS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.UserId,
                ActionArgumentPrimitive.UserIdInRoom -> userIdInRoomSuggestions?.value?.map {
                    it.toCommandSuggestion()
                } ?: emptyList()
                ActionArgumentPrimitive.UserIdNotInRoom -> emptyList() // TODO?
                ActionArgumentPrimitive.SessionId -> accounts.value
                ActionArgumentPrimitive.RoomId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        scopedRoomSuggestions.value.map { it.second }.distinct()
                    } else {
                        scopedRoomSuggestions.value.mapNotNull {
                            if (it.first.value in sessionIds) {
                                it.second
                            } else {
                                null
                            }
                        }
                    }
                }
                ActionArgumentPrimitive.SettingKey -> prefKeySuggestions
                ActionArgumentPrimitive.NavigatableDestinationName ->
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
                ActionArgumentPrimitive.Text,
                ActionArgumentPrimitive.Reason,
                ActionArgumentPrimitive.Integer,
                ActionArgumentPrimitive.Index,
                ActionArgumentPrimitive.SessionIndex,
                ActionArgumentPrimitive.EventId,
                ActionArgumentPrimitive.SpaceId,
                ActionArgumentPrimitive.SpaceSelectionId,
                ActionArgumentPrimitive.SpaceIndex,
                ActionArgumentPrimitive.UserName,
                ActionArgumentPrimitive.Empty -> emptyList()
                ActionArgumentPrimitive.EventType -> EVENT_TYPE_SUGGESTIONS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.StateEventType -> roomStateEventSuggestions?.value?.toStateEventTypeSuggestions().orEmpty()
                ActionArgumentPrimitive.NonEmptyStateKey -> {
                    val eventTypes = context.findAll(ActionArgumentPrimitive.StateEventType)
                    roomStateEventSuggestions?.value?.toStateEventKeySuggestions(eventTypes).orEmpty()
                }
            }.filterValidSuggestionsFor(query, arg).distinct()
        }
        is ActionArgumentAnyOf -> arg.arguments.flatMap { suggestPrimaryFor(it, context, query) }
        is ActionArgumentOptional -> suggestPrimaryFor(arg.argument, context, query)
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
                ActionArgumentPrimitive.NavigatableDestinationName ->
                    ALLOWED_DESTINATION_STRINGS.toSuggestionsWithoutHint()
                ActionArgumentPrimitive.RoomId -> {
                    val sessionIds = context.findAll(ActionArgumentPrimitive.SessionId)
                    if (sessionIds.isEmpty()) {
                        // Already suggested everything as primary suggestion
                        emptyList()
                    } else {
                        // Now we didn't find the room ID for this session, but we can still search for the others
                        scopedRoomSuggestions.value.map { it.second }
                    }
                }
                else -> emptyList()
            }.filterValidSuggestionsFor(query, arg).distinct()
        }
        is ActionArgumentAnyOf -> arg.arguments.flatMap { suggestSecondaryFor(it, context, query) }
        is ActionArgumentOptional -> suggestSecondaryFor(arg.argument, context, query)
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

fun List<StateEventCompletionSnapshot>.toStateEventTypeSuggestions() =
    map { it.eventType }.distinct().toSuggestionsWithoutHint()
fun List<StateEventCompletionSnapshot>.toStateEventKeySuggestions(eventTypes: List<String>) =
    filter { it.eventType in eventTypes && it.stateKey.isNotEmpty() }.map { it.stateKey }.distinct().toSuggestionsWithoutHint()
