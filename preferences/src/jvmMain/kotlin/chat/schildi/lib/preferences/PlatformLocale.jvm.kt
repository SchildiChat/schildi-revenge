package chat.schildi.lib.preferences

import java.util.Locale

internal actual fun languageDisplayName(languageTag: String): String = Locale.forLanguageTag(languageTag).displayName
