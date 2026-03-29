package chat.schildi.revenge

import androidx.compose.ui.ExperimentalComposeUiApi
import co.touchlab.kermit.Logger
import kotlin.system.exitProcess
import chat.schildi.revenge.ipc.SingleInstance
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    // Avoid ugly JVM crash dialog. May want to replace with our own branded crash screen later.
    // On Windows keep it, since I don't know how to get crash logs otherwise, and it's less ugly there anyway.
    if (!System.getProperty("os.name").lowercase().contains("win")) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Logger.e("Schildi encountered a fatal error in ${t.name}", e)
            exitProcess(1)
        }
    }

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
            SingleInstance.ensureSingleInstanceOrExit(startInTray)
            ComposeApp.main(startInTray)
        } else {
            SingleInstance.notifyExistingInstance(command.joinToString(separator = " "))
        }
    }
}
