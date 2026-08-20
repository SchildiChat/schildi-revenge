package chat.schildi.revenge

import androidx.compose.ui.ExperimentalComposeUiApi
import chat.schildi.revenge.actions.checkArguments
import chat.schildi.revenge.config.keybindings.Action
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import chat.schildi.revenge.ipc.SingleInstance
import chat.schildi.revenge.util.ExternalViewCache
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.Dispatchers
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    Logger.setLogWriters(
        platformLogWriter(RevengeLogFormatter),
        createAppFileLogWriter(),
    )
    Runtime.getRuntime().addShutdownHook(Thread(::flushAppFileLogs, "SchildiLogShutdown"))

    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        logUncaughtException(thread, error)
        exitProcess(1)
    }
    logProcessStarted()

    MainCommand().main(args)
}

class MainCommand : CliktCommand("schildi-revenge") {
    val startInTray by option(help = "Start the application minimized").flag()
    val command by argument("exec", help = "Send command to already running instance").multiple()
    init {
        context {
            readArgumentFile = null
        }
    }
    override fun run() {
        if (command.isEmpty()) {
            launchMainApp()
        } else {
            val deeplinkCommand = command.singleOrNull()?.let(::deeplinkCommandOrNull)
            val allowLaunchFromCommand = deeplinkCommand != null
            val joinedCommand = when {
                deeplinkCommand != null -> deeplinkCommand
                else -> command.joinToString(separator = " ")
            }
            val success = SingleInstance.notifyExistingInstance(joinedCommand)
            if (!success && allowLaunchFromCommand) {
                Logger.withTag("main").i("Failed to pass action to running instance, trying to launch new")
                // We usually allow launching from command if it opens a new window, so force start-in-tray here
                // to avoid getting two windows
                launchMainApp(startInTray = true, initialCommand = joinedCommand)
            } else {
                exitProcess(if (success) 0 else 1)
            }
        }
    }

    private fun launchMainApp(
        startInTray: Boolean = this.startInTray,
        initialCommand: String? = null,
    ) {
        SingleInstance.ensureSingleInstanceOrExit(
            openExistingInstance = initialCommand == null && !startInTray,
        )
        ExternalViewCache.clearAsync(ScCoroutines.scope(Dispatchers.IO, "ExternalViewCache"))
        ComposeApp.main(startInTray, initialCommand)
    }
}

internal fun deeplinkCommandOrNull(uri: String): String? {
    val validationError = checkArguments(
        Action.Global.ConsumeLink,
        args = listOf(uri),
        implicitArgs = emptyList(),
        validSessionIds = null,
    )
    return if (validationError == null) "${Action.Global.ConsumeLink.name} $uri" else null
}
