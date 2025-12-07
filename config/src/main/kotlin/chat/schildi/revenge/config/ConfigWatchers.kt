package chat.schildi.revenge.config

import chat.schildi.revenge.config.keybindings.KeybindingConfig
import kotlinx.coroutines.CoroutineScope
import java.io.File

object ConfigWatchers {
    // TODO add special sanity check for conflicting key binding definitions
    fun keybindings(scope: CoroutineScope) = ConfigWatcher(
        tag = "KeybindingConfig",
        scope = scope,
        file = File(ScAppDirs.getUserConfigDir(), "keybindings.toml"),
        serializer = KeybindingConfig.serializer(),
        default = KeybindingConfig(),
    )
}
