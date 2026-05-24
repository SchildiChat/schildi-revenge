package chat.schildi.revenge.actions

import kotlinx.serialization.Serializable

@Serializable
sealed interface ActionResult {
    val shouldExit: Boolean
    fun withChainSetting(chain: Boolean): ActionResult = this

    @Serializable
    sealed interface Actioned : ActionResult
    @Serializable
    sealed interface InvalidCommand : ActionResult {
        val message: String
    }

    @Serializable
    data class Success(
        // True after launching a coroutine that may still fail, causing the action to be considered "failed" eventually later
        val async: Boolean = false,
        val notifySuccess: Boolean = async,
        override val shouldExit: Boolean = true
    ) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    @Serializable
    data class Failure(val message: String, override val shouldExit: Boolean = true) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    @Serializable
    data class Malformed(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    @Serializable
    data class MissingParameters(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    @Serializable
    data class TooManyParameters(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    @Serializable
    data object Inapplicable : ActionResult {
        override val shouldExit = false
    }
    @Serializable
    data object NoOp : ActionResult {
        override val shouldExit = false
    }
    @Serializable
    data object NoMatch : ActionResult {
        override val shouldExit = false
    }
    @Serializable
    data object Ambiguous : ActionResult {
        override val shouldExit = false
    }
    companion object {
        fun chain(vararg actionHandlers: () -> ActionResult): ActionResult {
            var hasChainableSuccess = false
            actionHandlers.forEach { handler ->
                val actionResult = handler()
                if (actionResult.shouldExit) {
                    return actionResult
                }
                if (actionResult is Success) {
                    hasChainableSuccess = true
                }
            }
            return if (hasChainableSuccess) Success(shouldExit = false) else NoMatch
        }
    }
}

class ActionValidationException() : Exception("Internal action parsing validation error")

fun Boolean.orActionInapplicable() = if (this) ActionResult.Success() else ActionResult.Inapplicable
fun Boolean.orActionFailure(message: String) = if (this) ActionResult.Success() else ActionResult.Failure(message)
fun <T>T?.orActionValidationError() = this ?: throw ActionValidationException()
fun <T>Result<T>.toActionResult(async: Boolean = false, notifySuccess: Boolean = false) = if (isSuccess) {
    ActionResult.Success(async = async, notifySuccess = notifySuccess)
} else {
    ActionResult.Failure(exceptionOrNull()?.message ?: "Unknown failure")
}
fun Result<ActionResult>.unwrapActionResult(async: Boolean = false, notifySuccess: Boolean = false) = if (isSuccess) {
    getOrNull() ?: ActionResult.Success(async = async, notifySuccess = notifySuccess)
} else {
    ActionResult.Failure(exceptionOrNull()?.message ?: "Unknown failure")
}
fun <T>Result<T>.mapActionResult(
    async: Boolean = false,
    notifySuccess: Boolean = false,
    block: (T) -> ActionResult,
) = map(block).unwrapActionResult(async = async, notifySuccess = notifySuccess)
