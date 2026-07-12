package chat.schildi.revenge.actions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.input.TextFieldValue
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
