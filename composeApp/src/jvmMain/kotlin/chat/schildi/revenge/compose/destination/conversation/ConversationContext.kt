package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.runtime.compositionLocalOf
import io.element.android.libraries.matrix.api.core.ThreadId

val LocalThreadId = compositionLocalOf<ThreadId?> { null }
