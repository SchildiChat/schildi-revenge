package chat.schildi.revenge.actions

import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.forEachPreference
import chat.schildi.revenge.UiState
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.minArgsSize
import chat.schildi.revenge.model.RevengeRoomListDataSource
import chat.schildi.revenge.model.RoomListDataSource
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

enum class CurrentCommandValidity {
    INCOMPLETE,
    INVALID,
    VALID,
}

data class CommandSuggestionsState(
    val query: String,
    val validity: CurrentCommandValidity,
    val currentSuggestions: ImmutableList<String>,
)

private val BOOLEAN_SUGGESTIONS = listOf("true", "false")

typealias CommandArgContext = List<Pair<ActionArgument, String>>

// TODO hook me into KeyboardActionHandler to get async suggestions or sth.
class CommandSuggestionsProvider(
    queryFlow: Flow<KeyboardActionMode.Command?>,
    val commandParser: CommandParser,
    private val scope: CoroutineScope,
    private val roomListDataSource: RoomListDataSource = RevengeRoomListDataSource,
) {
    private val log = Logger.withTag("CmdSuggestions")

    private val allCommands = commandParser.getAllPossibleCommandsSorted()
    private val allCommandSuggestions = allCommands.map { it.first }

    private val scopedRoomSuggestions = roomListDataSource.allRooms.map {
        it.map { Pair(it.sessionId, it.summary.roomId) }.distinct()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val prefKeySuggestions = buildList {
        ScPrefs.rootPrefs.forEachPreference {
            if (it.key != null) {
                add(it.sKey)
            }
        }
    }

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
                .map { it.first }
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
            val (currentValidity, argSuggestions) = when {
                possibleActions.isEmpty() -> Pair(CurrentCommandValidity.INVALID, emptyList())
                else -> {
                    val argumentsChecked = possibleActions.map { checkArguments(it.first, args) }
                    val validity = when {
                        argumentsChecked.any { it == null } -> CurrentCommandValidity.VALID
                        argumentsChecked.any { it is ActionResult.MissingParameters } -> CurrentCommandValidity.INCOMPLETE
                        else -> {
                            // Check again with only arguments that are not being written right now, so if the command
                            // is only invalid because of missing arguments we don't mark it as invalid
                            val stableArgs = if (args.isEmpty()) args else args.subList(0, args.size - 1)
                            if (possibleActions.any { checkArguments(it.first, stableArgs) is ActionResult.MissingParameters }) {
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
    ): List<String> {
        return suggestPrimaryFor(arg, context, prefix).takeIf { it.isNotEmpty() }
            ?: suggestSecondaryFor(arg, context, prefix)
    }

    private fun suggestPrimaryFor(
        arg: ActionArgument,
        context: CommandArgContext,
        prefix: String,
    ): List<String> = when(arg) {
        ActionArgumentPrimitive.Boolean -> BOOLEAN_SUGGESTIONS
        ActionArgumentPrimitive.Mxid -> emptyList() // TODO suggestion provider for rooms and global
        ActionArgumentPrimitive.SessionId -> UiState.currentValidSessionIds.value ?: emptyList()
        ActionArgumentPrimitive.RoomId -> {
            val sessionIds = context.findSessionIds()
            if (sessionIds.isEmpty()) {
                scopedRoomSuggestions.value.map { it.second.value }.distinct()
            } else {
                scopedRoomSuggestions.value.mapNotNull {
                    if (it.first.value in sessionIds) {
                        it.second.value
                    } else {
                        null
                    }
                }
            }
        }
        ActionArgumentPrimitive.SettingKey -> prefKeySuggestions
        ActionArgumentPrimitive.NavigatableDestinationName -> SUGGESTED_DESTINATION_STRINGS
        ActionArgumentPrimitive.Text,
        ActionArgumentPrimitive.Integer,
        ActionArgumentPrimitive.SessionIndex,
        ActionArgumentPrimitive.EventId,
        ActionArgumentPrimitive.SpaceId,
        ActionArgumentPrimitive.SpaceSelectionId,
        ActionArgumentPrimitive.SpaceIndex -> emptyList()
        is ActionArgumentAnyOf -> arg.arguments.flatMap { suggestFor(it, context, prefix) }
        is ActionArgumentOptional -> suggestFor(arg.argument, context, prefix)
    }.filterValidSuggestionsFor(prefix).distinct()

    // If we have less preferred but still valid suggestions
    private fun suggestSecondaryFor(
        arg: ActionArgument,
        context: CommandArgContext,
        prefix: String,
    ): List<String> = when(arg) {
        ActionArgumentPrimitive.NavigatableDestinationName -> ALLOWED_DESTINATION_STRINGS
        ActionArgumentPrimitive.RoomId -> {
            val sessionIds = context.findSessionIds()
            if (sessionIds.isEmpty()) {
                // Already suggested everything as primary suggestion
                emptyList()
            } else {
                // Now we didn't find the room ID for this session, but we can still search for the others
                scopedRoomSuggestions.value.map { it.second.value }
            }
        }
        else -> emptyList()
    }.filterValidSuggestionsFor(prefix).distinct()

    fun clear() {
        scope.cancel("Canceled on clear request")
    }

    fun <T>List<T>.filterValidSuggestionsFor(prefix: String, select: (T) -> String) = filter {
        select(it).startsWith(prefix)
    }
    fun List<String>.filterValidSuggestionsFor(prefix: String) = filterValidSuggestionsFor(prefix) { it }
}


fun CommandArgContext.findSessionIds() = mapNotNull { (ctxArgDef, ctxArgVal) ->
    ctxArgVal.takeIf { ctxArgDef.canHold(ActionArgumentPrimitive.SessionId) }
}
