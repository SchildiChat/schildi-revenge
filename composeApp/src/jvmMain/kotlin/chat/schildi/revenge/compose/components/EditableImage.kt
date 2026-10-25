package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.CopyActions
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.ImageEditActions
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.compose.destination.conversation.firstDroppedFile
import chat.schildi.revenge.compose.destination.conversation.hasDroppedFiles
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.compose.focus.rememberFocusId
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.preferences.value
import chat.schildi.theme.scExposures
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.MediaSource
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_clear
import shire.res.generated.resources.action_discard_changes
import shire.res.generated.resources.action_remove_avatar
import shire.res.generated.resources.action_save_changes
import shire.res.generated.resources.action_select_picture
import java.io.File
import java.net.URI
import kotlin.uuid.Uuid

sealed interface EditImageValue {
    data class Local(val path: String, val mimetype: String, val isAppOwned: Boolean) : EditImageValue
    data class Remote(val mediaSource: MediaSource) : EditImageValue
    data object Cleared : EditImageValue
}

@Composable
fun EditableAvatar(
    editId: Any,
    currentValue: MediaSource?,
    size: Dp,
    displayName: String,
    role: FocusRole,
    persist: (suspend (EditImageValue) -> Result<Unit>)?,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true,
    sessionId: SessionId? = LocalSessionId.current,
    shape: Shape = Dimens.avatarShape,
    contentDescription: String? = null,
    allowAnimated: Boolean = ScPrefs.ANIMATE_AVATARS.value(),
    editState: MutableState<EditImageValue?> = rememberEditState(currentValue, canEdit),
    clickAction: InteractionAction? = if (canEdit && persist == null) null else clickToOpenImageAction(
        sessionId = sessionId,
        mediaSource = ((editState.value ?: currentValue?.let(EditImageValue::Remote)) as? EditImageValue.Remote)?.mediaSource,
    ),
    focusId: Uuid = rememberFocusId(),
) {
    EditableImage(
        editId = editId,
        currentValue = currentValue?.let(EditImageValue::Remote) ?: EditImageValue.Cleared,
        role = role,
        persist = persist,
        modifier = modifier,
        canEdit = canEdit,
        editState = editState,
        clearTitle = Res.string.action_remove_avatar.toStringHolder(),
        clickAction = clickAction,
        focusId = focusId,
    ) { currentValue ->
        AvatarImage(
            source = currentValue,
            size = size,
            displayName = displayName,
            sessionId = sessionId,
            shape = shape,
            contentDescription = contentDescription,
            allowAnimated = allowAnimated,
        )
    }
}

@Composable
fun rememberEditState(vararg keys: Any?): MutableState<EditImageValue?> = remember(*keys) {
    mutableStateOf(null)
}

@Composable
fun EditableImage(
    editId: Any,
    currentValue: EditImageValue,
    role: FocusRole,
    persist: (suspend (EditImageValue) -> Result<Unit>)?,
    modifier: Modifier = Modifier,
    clickAction: InteractionAction? = null,
    canEdit: Boolean = true,
    clearTitle: ComposableStringHolder = Res.string.action_clear.toStringHolder(),
    editState: MutableState<EditImageValue?> = rememberEditState(currentValue, canEdit),
    focusId: Uuid = rememberFocusId(),
    content: @Composable BoxScope.(EditImageValue) -> Unit,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val isPersisting = editId in keyHandler.editPersistInProgress.collectAsState().value

    val renderedValue = editState.value ?: currentValue
    val canSave = canEdit && persist != null && editState.value != null && !isPersisting
    val canClear = canEdit && renderedValue != EditImageValue.Cleared && !isPersisting

    WithContextMenu(
        focusId = focusId,
        entries = listOfNotNull<ContextMenuEntry>(
            ContextMenuActionEntry(
                title = Res.string.action_save_changes.toStringHolder(),
                action = Action.ImageEditAble.SaveEdit,
                keyboardShortcut = Key.S,
            ).takeIf { canSave },
            ContextMenuActionEntry(
                title = Res.string.action_discard_changes.toStringHolder(),
                action = Action.ImageEditAble.DiscardEdit,
                keyboardShortcut = Key.D,
            ).takeIf { canSave },
            ContextMenuActionEntry(
                title = Res.string.action_select_picture.toStringHolder(),
                action = Action.ImageEditAble.OpenPickerAndSave,
                keyboardShortcut = Key.P,
            ).takeIf { canEdit && !isPersisting },
            ContextMenuActionEntry(
                title = clearTitle,
                action = Action.ImageEditAble.ClearAndSave,
                keyboardShortcut = Key.R,
            ).takeIf { canClear },
        ).toImmutableList(),
    ) { openContextMenu ->

        val actionProvider = editableImageActionProvider(
            editId = editId,
            focusId = focusId,
            editState = editState,
            persist = persist,
            canEdit = canEdit,
            accessCurrentValue = { currentValue },
            primaryAction = clickAction ?: openContextMenu,
            secondaryAction = openContextMenu,
        )

        var isDragging by remember { mutableStateOf(false) }
        val dragAlpha = animateFloatAsState(if (isDragging) Dimens.Editable.IN_PROGRESS_ALPHA else 1f)
        val fileDragTarget = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    Logger.withTag("DnD").d { "Entered $event" }
                    isDragging = true
                }

                override fun onExited(event: DragAndDropEvent) {
                    Logger.withTag("DnD").d { "Exited $event" }
                    isDragging = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    isDragging = false
                    val rawUri = event.firstDroppedFile() ?: return false
                    val file = try {
                        File(URI(rawUri))
                    } catch (e: Exception) {
                        Logger.withTag("DnD").e("Failed to parse file URI $rawUri", e)
                        return false
                    }
                    Logger.withTag("DnD").d { "Received drop $event for editable image $editId" }
                    return keyHandler.handleAction(
                        focusItem = focusId,
                        action = Action.ImageEditAble.Set,
                        args = listOf(file.absolutePath)
                    ) is ActionResult.Success
                }
            }
        }

        Box(
            modifier.keyFocusable(
                role = role,
                id = focusId,
                actionProvider = actionProvider,
            )
                .thenIf(canEdit) {
                    alpha(dragAlpha.value)
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                !isPersisting && event.hasDroppedFiles()
                            },
                            target = fileDragTarget,
                        )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.alpha(animateFloatAsState(if (isPersisting) Dimens.Editable.IN_PROGRESS_ALPHA else 1f).value)
            ) {
                content(renderedValue)
            }
            if (isPersisting) {
                CircularProgressIndicator(
                    color = MaterialTheme.scExposures.accentColor,
                )
            }
            EditableImageOverlay(
                parentFocusId = focusId,
                canSave = canSave,
            )
        }
    }
}

@Composable
private fun BoxScope.EditableImageOverlay(
    parentFocusId: Uuid,
    canSave: Boolean,
) {
    AnimatedVisibility(
        canSave,
        enter = fadeIn(Dimens.tween()),
        exit = fadeOut(Dimens.tween()),
        modifier = Modifier.align(Alignment.TopEnd),
    ) {
        WithTooltip(
            stringResource(Res.string.action_discard_changes)
        ) {
            OverlayIconButton(
                Icons.AutoMirrored.Default.Undo,
                stringResource(Res.string.action_discard_changes),
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.HandleAction(
                        focusId = parentFocusId,
                        action = Action.ImageEditAble.DiscardEdit,
                    )
                ),
                iconSize = Dimens.Editable.overlayButtonIconSize,
                buttonSize = Dimens.Editable.overlayButtonSize,
            )
        }
    }
    AnimatedVisibility(
        canSave,
        enter = fadeIn(Dimens.tween()),
        exit = fadeOut(Dimens.tween()),
        modifier = Modifier.align(Alignment.BottomEnd),
    ) {
        WithTooltip(
            stringResource(Res.string.action_save_changes),
        ) {
            OverlayIconButton(
                Icons.Default.Save,
                stringResource(Res.string.action_save_changes),
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.HandleAction(
                        focusId = parentFocusId,
                        action = Action.ImageEditAble.SaveEdit,
                    )
                ),
                iconSize = Dimens.Editable.overlayButtonIconSize,
                buttonSize = Dimens.Editable.overlayButtonSize,
            )
        }
    }
}

@Composable
private fun editableImageActionProvider(
    editId: Any,
    focusId: Uuid,
    canEdit: Boolean,
    editState: MutableState<EditImageValue?>,
    persist: (suspend (EditImageValue) -> Result<Unit>)?,
    accessCurrentValue: () -> EditImageValue,
    primaryAction: InteractionAction?,
    secondaryAction: InteractionAction? = null,
) = actionProvider(
    primaryAction = primaryAction,
    secondaryAction = secondaryAction,
    editActions = imageEditActions(
        editId = editId,
        focusId = focusId,
        canEdit = canEdit,
        editState = editState,
        persist = persist,
        accessCurrentValue = accessCurrentValue,
    ),
    copyActions = CopyActions(
        accessMxcUrl = {
            when (val current = accessCurrentValue()) {
                is EditImageValue.Remote -> current.mediaSource.safeUrl
                is EditImageValue.Local -> current.path
                is EditImageValue.Cleared -> null
            }
        }
    )
)

@Composable
private fun imageEditActions(
    editId: Any,
    focusId: Uuid?,
    canEdit: Boolean,
    editState: MutableState<EditImageValue?>,
    persist: (suspend (EditImageValue) -> Result<Unit>)?,
    accessCurrentValue: () -> EditImageValue,
) = if (canEdit) remember(
    editId,
    focusId,
    editState,
    accessCurrentValue,
    persist,
) {
    ImageEditActions(
        editId = editId,
        stableFocusId = focusId,
        editEcho = editState,
        accessPersistedValue = accessCurrentValue,
        persistValue = persist,
    )
} else null
