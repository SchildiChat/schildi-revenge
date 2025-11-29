package chat.schildi.revenge.compose.focus

import androidx.compose.runtime.compositionLocalOf
import java.util.UUID

data class FocusParent(
    val uuid: UUID,
    val parent: FocusParent?,
)

val LocalFocusParent = compositionLocalOf<FocusParent?> { null }
