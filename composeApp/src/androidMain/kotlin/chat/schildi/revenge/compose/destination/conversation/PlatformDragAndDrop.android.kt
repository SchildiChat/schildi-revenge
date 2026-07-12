package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.ui.draganddrop.DragAndDropEvent

actual fun DragAndDropEvent.hasDroppedFiles(): Boolean = false
actual fun DragAndDropEvent.firstDroppedFile(): String? = null
