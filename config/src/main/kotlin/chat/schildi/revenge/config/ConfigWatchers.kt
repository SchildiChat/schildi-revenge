package chat.schildi.revenge.config

import chat.schildi.revenge.config.keybindings.KeybindingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import java.io.File

object ConfigWatchers {
    // TODO add special sanity check for conflicting key binding definitions
    fun keybindings(scope: CoroutineScope) = TomlConfigWatcher(
        tag = "KeybindingConfig",
        scope = scope,
        file = File(ScAppDirs.getUserConfigDir(), "keybindings.toml"),
        serializer = KeybindingConfig.serializer(),
    )

    fun <T : Any>jsonConfigWatcher(
        tag: String,
        fileName: String,
        scope: CoroutineScope,
        serializer: KSerializer<T>,
        default: T,
        createIfMissing: Boolean = false,
    ) = JsonConfigWatcher(
        tag = tag,
        scope = scope,
        file = File(ScAppDirs.getUserConfigDir(), fileName),
        serializer = serializer,
        default = default,
        createIfMissing = createIfMissing,
    )
}
