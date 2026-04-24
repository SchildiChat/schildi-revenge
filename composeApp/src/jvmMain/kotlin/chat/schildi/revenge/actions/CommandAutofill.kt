package chat.schildi.revenge.actions

import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentContextBased
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.getParameter

data class CommandSuggestionTarget(
    val currentArgIndex: Int,
    val prefix: String,
    val resolvedStableArgs: List<String>,
    val stableExplicitArgs: List<String>,
)

fun Action.resolveSuggestionTarget(
    rawArgs: List<String>,
    queryEndsWithSpace: Boolean,
    impliedContext: CommandArgContext,
): CommandSuggestionTarget {
    val stableExplicitArgs = if (queryEndsWithSpace || rawArgs.isEmpty()) {
        rawArgs
    } else {
        rawArgs.dropLast(1)
    }
    val prefix = if (queryEndsWithSpace || rawArgs.isEmpty()) "" else rawArgs.last()
    val resolvedStableArgs = autoFillImpliedArgs(stableExplicitArgs, impliedContext)
    return CommandSuggestionTarget(
        currentArgIndex = resolvedStableArgs.size,
        prefix = prefix,
        resolvedStableArgs = resolvedStableArgs,
        stableExplicitArgs = stableExplicitArgs,
    )
}

fun Action.autoFillImpliedArgs(
    explicitArgs: List<String>,
    impliedContext: CommandArgContext,
): List<String> {
    if (explicitArgs.size >= args.size) {
        return explicitArgs
    }
    val resolvedArgs = explicitArgs.toMutableList()
    while (resolvedArgs.size < args.size) {
        val argIndex = resolvedArgs.size
        val argDef = args[argIndex]
        val context = args.take(argIndex).zip(resolvedArgs) + impliedContext
        val impliedValue = argDef.resolveUniqueImpliedValue(context) ?: break
        resolvedArgs += impliedValue
    }
    return resolvedArgs
}

private fun ActionArgument.resolveUniqueImpliedValue(context: CommandArgContext): String? = when (this) {
    is ActionArgumentPrimitive -> context.getParameter(this)
    is ActionArgumentAnyOf -> arguments.mapNotNull { context.getParameter(it) }.distinct().singleOrNull()
    is ActionArgumentOptional -> argument.resolveUniqueImpliedValue(context)
    is ActionArgumentContextBased -> getFor(context).resolveUniqueImpliedValue(context)
}
