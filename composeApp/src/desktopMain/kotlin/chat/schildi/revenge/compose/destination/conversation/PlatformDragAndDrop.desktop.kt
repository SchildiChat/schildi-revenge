@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package chat.schildi.revenge.compose.destination.conversation

import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData

actual fun DragAndDropEvent.hasDroppedFiles(): Boolean = dragData() is DragData.FilesList
actual fun DragAndDropEvent.firstDroppedFile(): String? =
    (dragData() as? DragData.FilesList)?.readFiles()?.firstOrNull()
