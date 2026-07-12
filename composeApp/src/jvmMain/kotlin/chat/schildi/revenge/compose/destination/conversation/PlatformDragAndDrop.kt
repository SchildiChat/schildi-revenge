package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.ui.draganddrop.DragAndDropEvent

expect fun DragAndDropEvent.hasDroppedFiles(): Boolean
expect fun DragAndDropEvent.firstDroppedFile(): String?
