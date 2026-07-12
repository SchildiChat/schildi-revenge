package chat.schildi.revenge.compose.focus

import androidx.compose.runtime.compositionLocalOf
import kotlin.uuid.Uuid

data class FocusParent(
    val uuid: Uuid,
    val parent: FocusParent?,
)

val LocalFocusParent = compositionLocalOf<FocusParent?> { null }
