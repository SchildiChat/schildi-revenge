package chat.schildi.revenge.actions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.input.TextFieldValue
import chat.schildi.revenge.compose.components.EditImageValue
import java.io.File
import kotlin.uuid.Uuid

sealed interface EditActions {
    val editId: Any
    val stableFocusId: Uuid?
    fun discardEdit()
    suspend fun persistEdit(): Result<Unit>?
}

data class PlaintextEditActions(
    override val editId: Any,
    override val stableFocusId: Uuid?,
    val editEcho: MutableState<TextFieldValue?>,
    val accessPersistedValue: () -> String?,
    val persistValue: suspend (String) -> Result<Unit>,
) : EditActions {
    override fun discardEdit() {
        editEcho.value = null
    }

    override suspend fun persistEdit(): Result<Unit>? {
        val newValue = editEcho.value?.text?.takeIf { it != accessPersistedValue() } ?: return null
        return persistValue(newValue)
    }
}

data class ImageEditActions(
    override val editId: Any,
    override val stableFocusId: Uuid?,
    val editEcho: MutableState<EditImageValue?>,
    val accessPersistedValue: () -> EditImageValue,
    // Null if persistence should be handled in batch with other actions, e.g. on room creation.
    val persistValue: (suspend (EditImageValue) -> Result<Unit>)?,
) : EditActions {
    override fun discardEdit() = updateEcho { null }

    val needsExplicitSave = persistValue != null

    fun updateEcho(transform: (EditImageValue?) -> EditImageValue?) {
        val old = editEcho.value
        val new = transform(old)
        if (old != new) {
            editEcho.value = new
            (old as? EditImageValue.Local)?.takeIf { it.isAppOwned }?.let {
                runCatching {
                    File(old.path).parentFile?.deleteRecursively()
                }
            }
        }
    }

    override suspend fun persistEdit(): Result<Unit>? {
        val newValue = editEcho.value?.takeIf { it != accessPersistedValue() } ?: return null
        return persistValue?.invoke(newValue)
            ?.onSuccess {
                updateEcho { null }
            }
    }
}
