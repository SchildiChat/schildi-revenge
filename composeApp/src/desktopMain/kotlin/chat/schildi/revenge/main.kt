package chat.schildi.revenge

import androidx.compose.ui.ExperimentalComposeUiApi
import chat.schildi.revenge.actions.checkArguments
import chat.schildi.revenge.config.keybindings.Action
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import co.touchlab.kermit.platformLogWriter
import chat.schildi.revenge.ipc.SingleInstance
import chat.schildi.revenge.util.ExternalViewCache
import chat.schildi.revenge.util.SystemInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    Logger.setLogWriters(platformLogWriter(RevengeLogFormatter))

    // Avoid ugly JVM crash dialog. May want to replace with our own branded crash screen later.
    // On Windows keep it, since I don't know how to get crash logs otherwise, and it's less ugly there anyway.
    if (!SystemInfo.isWindows()) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Logger.e("Schildi encountered a fatal error in ${t.name}", e)
            exitProcess(1)
        }
    }

    MainCommand().main(args)
}

private object RevengeLogFormatter : MessageStringFormatter {
    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String {
        return "${Instant.now().truncatedTo(ChronoUnit.MILLIS)} ${super.formatMessage(severity, tag, message)}"
    }
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
            var allowLaunchFromCommand = false
            val joinedCommand = when {
                isSupportedDeeplink(command) -> {
                    allowLaunchFromCommand = true
                    "${Action.Global.ConsumeLink.name} ${command.first()}"
                }
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

    private fun isSupportedDeeplink(args: List<String>) = checkArguments(
        Action.Global.ConsumeLink,
        args = args,
        implicitArgs = emptyList(),
        validSessionIds = null,
    ) == null

    private fun launchMainApp(
        startInTray: Boolean = this.startInTray,
        initialCommand: String? = null,
    ) {
        SingleInstance.ensureSingleInstanceOrExit(
            openExistingInstance = initialCommand == null && !startInTray,
        )
        ExternalViewCache.clear()
        ComposeApp.main(startInTray, initialCommand)
    }
}
