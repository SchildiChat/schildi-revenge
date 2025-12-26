package chat.schildi.revenge.actions

import chat.schildi.revenge.config.keybindings.handlesCommand

class CommandParser(
    private val actionHandlers: List<KeyboardActionProvider<*>>,
) {
    fun getAllPossibleCommandsSorted() = actionHandlers
        .flatMap { it.getPossibleActions() }
        .distinct()
        .flatMap { action ->
            (action.aliases + listOf(action.name, action.name.lowercase())).map {
                it to action
            }
        }
        .sortedBy { it.first.lowercase() }

    fun parseCommandString(command: String): Pair<String, List<String>>? {
        if (command.isBlank()) {
            return null
        }
        val commandParsed = command.trim().split(Regex("\\s+"))
        val mainCommand = commandParsed.firstOrNull() ?: return null
        val args = commandParsed.subList(1, commandParsed.size)
        return Pair(mainCommand, args)
    }

    fun getPossibleActions(mainCommand: String) = actionHandlers.flatMap { handler ->
        handler.getPossibleActions().mapNotNull {
            if (it.handlesCommand(mainCommand)) {
                it to handler
            } else {
                null
            }
        }
    }
}
