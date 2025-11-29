package chat.schildi.revenge.actions

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.navigation.Destination
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

val LocalKeyboardActionHandler = compositionLocalOf { KeyboardActionHandler(-1) }

private data class FocusTarget(
    val id: UUID,
    val parent: FocusParent?,
    val role: FocusRole,
    val coordinates: Rect,
    val focusRequester: FocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
)

enum class FocusRole {
    SEARCHABLE_ITEM,
    AUX_ITEM,
    CONTAINER,
    SEARCH_BAR,
}

sealed interface KeyboardActionMode {
    data object Navigation : KeyboardActionMode
    data class Search(
        val query: String,
        val searchProvider: SearchProvider,
        val navigating: Boolean,
        val searchFocusContainer: UUID?,
    ) : KeyboardActionMode
}

class KeyboardActionHandler(
    windowId: Int,
) {
    private val log = Logger.withTag("Nav/$windowId")

    var focusManager: FocusManager? = null
    var windowCoordinates: Rect? = null
    private var lastPointerPosition = Offset.Zero
    private val _currentFocus = MutableStateFlow<UUID?>(null)
    val currentFocus = _currentFocus.asStateFlow()

    private val _mode = MutableStateFlow<KeyboardActionMode>(KeyboardActionMode.Navigation)
    val mode = _mode.asStateFlow()

    val searchQuery = mode.map {
        (it as? KeyboardActionMode.Search)?.query ?: ""
    }

    private val focusableTargets = ConcurrentHashMap<UUID, FocusTarget>()

    private fun distanceToRect(rect: Rect, p: Offset): Float {
        val nearestX = p.x.coerceIn(rect.left, rect.right)
        val nearestY = p.y.coerceIn(rect.top, rect.bottom)

        val dx = p.x - nearestX
        val dy = p.y - nearestY

        return sqrt(dx * dx + dy * dy)
    }

    private fun moveFocus(
        focusDirection: FocusDirection,
        currentFocus: FocusTarget? = currentFocused(),
        parentId: UUID? = currentFocus?.parent?.uuid,
    ): Boolean {
        if (parentId == null || currentFocus == null) {
            // No clue what to do, but maybe compose internals have an idea
            log.i { "moveFocus: Fall back to FocusManager without current focus" }
            return focusManager?.moveFocus(focusDirection) == true
        }
        val focusDirectionCheck: (FocusTarget) -> Boolean = when (focusDirection) {
            FocusDirection.Left -> {{ it.coordinates.right <= currentFocus.coordinates.left }}
            FocusDirection.Right -> {{ it.coordinates.left >= currentFocus.coordinates.right }}
            FocusDirection.Up -> {{ it.coordinates.bottom <= currentFocus.coordinates.top }}
            FocusDirection.Down -> {{ it.coordinates.top >= currentFocus.coordinates.bottom }}
            // Unsupported directions, unclear what to do; fallback to focus manager
            else -> {
                return focusManager?.moveFocus(focusDirection) == true
            }
        }
        val filteredTargets = focusableTargets.values.filter {
            if (it.parent?.uuid != parentId || it.id == currentFocus.id) {
                return@filter false
            }
            focusDirectionCheck(it)
        }
        return filteredTargets.minByOrNull {
            distanceToRect(it.coordinates, currentFocus.coordinates.center)
        }?.focusRequester?.requestFocus() ?: false
    }

    private fun focusClosestTo(
        position: Offset,
        parentId: UUID? = null,
        role: FocusRole? = null,
    ): Boolean {
        val filtered = if (parentId == null && role == null) {
            focusableTargets.values
        } else {
            focusableTargets.values.filter {
                (role == null || it.role == role) &&
                        (parentId == null || it.parent?.uuid == parentId)
            }
        }
        return filtered.minByOrNull {
            distanceToRect(it.coordinates, position)
        }?.focusRequester?.requestFocus() ?: false
    }

    private fun focusByRole(role: FocusRole): Boolean {
        return focusableTargets.values.find { it.role == role }?.focusRequester?.requestFocus() ?: false
    }

    private fun currentFocused() = currentFocus.value?.let { focusableTargets[it] }

    fun executeAction(
        action: InteractionAction,
        destinationStateHolder: DestinationStateHolder? = null
    ): Boolean {
        updateMode {
            if (it !is KeyboardActionMode.Search) {
                it
            } else {
                KeyboardActionMode.Navigation
            }
        }
        return when (action) {
            is InteractionAction.NavigationAction -> {
                val destination = action.buildDestination()
                when (action) {
                    is InteractionAction.OpenWindow -> {
                        UiState.openWindow(destination, action.initialTitle)
                        true
                    }
                    is InteractionAction.NavigateCurrent -> {
                        navigateCurrentDestination(destination, destinationStateHolder)
                    }
                }
            }
        }
    }

    private fun navigateCurrentDestination(
        destination: Destination,
        destinationStateHolder: DestinationStateHolder? = null,
    ): Boolean {
        return (
                destinationStateHolder
                    ?: currentFocused()?.destinationStateHolder ?:
                    focusableTargets.values.firstNotNullOfOrNull { it.destinationStateHolder }
        )?.navigate(destination) != null
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (val mode = mode.value) {
            is KeyboardActionMode.Navigation -> handleNavigationEvent(event)
            is KeyboardActionMode.Search -> handleSearchEvent(event, mode)
        }
    }

    private fun updateMode(update: (KeyboardActionMode) -> KeyboardActionMode) {
        _mode.update { old ->
            val new = update(old)
            val oldSearchProvider = (old as? KeyboardActionMode.Search)?.searchProvider
            if (oldSearchProvider != null && oldSearchProvider != (new as? KeyboardActionMode.Search)?.searchProvider) {
                oldSearchProvider.onSearchCleared()
            }
            new
        }
    }

    private fun handleSearchEvent(event: KeyEvent, mode: KeyboardActionMode.Search): Boolean {
        return when (event.key) {
            Key.Escape -> {
                updateMode { KeyboardActionMode.Navigation }
                true
            }
            Key.Enter -> {
                if (mode.navigating) {
                    handleNavigationEvent(event)
                } else {
                    updateMode { mode.copy(navigating = true) }
                    windowCoordinates?.let {
                        focusClosestTo(it.topCenter, role = FocusRole.SEARCHABLE_ITEM)
                    }
                    true
                }
            }
            Key.DirectionUp -> false // TODO cycle search history
            Key.DirectionDown -> false // TODO cycle search history
            else -> {
                if (mode.navigating) {
                    handleNavigationEvent(event)
                } else {
                    false
                }
            }
        }
    }

    private fun focusSearchResults(parentId: UUID?) {
        focusClosestTo(Offset.Zero, role = FocusRole.SEARCHABLE_ITEM, parentId = parentId)
    }

    private fun focusCurrentContainerRelative(select: (Rect) -> Offset): Boolean {
        return windowCoordinates?.let { coordinates ->
            val parent = currentFocused()?.parent
            focusClosestTo(select(coordinates), parentId = parent?.uuid)
        } ?: false
    }

    private fun focusParent(): Boolean {
        val parent = currentFocused()?.parent ?: return false
        _currentFocus.value = parent.uuid
        return true
    }

    private fun handleNavigationEvent(event: KeyEvent): Boolean {
        if (event.type == KeyDown) {
            // TODO make this configurable
            return if (event.isShiftPressed) {
                when (event.key) {
                    // Relative focus - TODO should maintain X offset rather than assuming center?
                    Key.H -> focusCurrentContainerRelative { it.topCenter }
                    Key.M -> focusCurrentContainerRelative { it.center }
                    Key.L -> focusCurrentContainerRelative { it.bottomCenter }
                    // Some navigation
                    Key.I -> navigateCurrentDestination(Destination.Inbox)
                    Key.A -> navigateCurrentDestination(Destination.AccountManagement)
                    // Tertiary action (usually same as mouse middle click)
                    Key.Enter -> currentFocused()?.actions?.tertiaryAction?.let(::executeAction) ?: false
                    else -> false
                }
            } else if (event.isCtrlPressed) {
                when (event.key) {
                    // Secondary action (usually same as mouse right click)
                    Key.Enter -> currentFocused()?.actions?.secondaryAction?.let(::executeAction) ?: false
                    else -> false
                }
            } else {
                when (event.key) {
                    // Arrow keys
                    Key.DirectionLeft -> moveFocus(FocusDirection.Left)
                    Key.DirectionRight -> moveFocus(FocusDirection.Right)
                    Key.DirectionUp -> moveFocus(FocusDirection.Up)
                    Key.DirectionDown -> moveFocus(FocusDirection.Down)
                    // QWERTY VIM-like navigation
                    Key.H -> moveFocus(FocusDirection.Left)
                    Key.J -> moveFocus(FocusDirection.Down)
                    Key.K -> moveFocus(FocusDirection.Up)
                    Key.L -> moveFocus(FocusDirection.Right)
                    // Colemak VIM-like navigation
                    Key.E -> moveFocus(FocusDirection.Up)
                    Key.N -> moveFocus(FocusDirection.Down)
                    Key.I -> moveFocus(FocusDirection.Right)
                    // Primary action (usually same as mouse click) + container navigation
                    Key.Enter -> {
                        val current = currentFocused()
                        if (current?.role == FocusRole.CONTAINER) {
                            focusClosestTo(Offset.Zero, parentId = current.id)
                        } else {
                            current?.actions?.primaryAction?.let(::executeAction) ?: false
                        }
                    }
                    Key.Escape -> focusParent()
                    // Mode switching
                    Key.Slash -> handleSearchUpdate("", navigating = false) {
                        // TODO search -> search-nav -> search inserts a slash into search field by accident
                        focusByRole(FocusRole.SEARCH_BAR)
                    }
                    else -> false
                }
            }
        }
        return false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        // TODO?
        return false
    }

    fun onFocusChanged(target: UUID, state: FocusState) {
        log.v { "Focus changed for $target to $state" }
        var lostFocusTarget: UUID? = null
        if (state.isFocused) {
            _currentFocus.update {
                lostFocusTarget = it
                target
            }
        } else if (!state.hasFocus) {
            _currentFocus.update {
                if (it == target) {
                    lostFocusTarget = it
                    null
                } else {
                    it
                }
            }
        }
        lostFocusTarget?.let { focusableTargets[it] }?.let(::handleLostFocus)
    }

    private fun handleLostFocus(target: FocusTarget) {
        if (target.role == FocusRole.SEARCH_BAR) {
            updateMode { mode ->
                (mode as? KeyboardActionMode.Search)?.copy(navigating = true) ?: mode
            }
        }
    }

    fun registerFocusTarget(
        target: UUID,
        parent: FocusParent?,
        coordinates: LayoutCoordinates,
        focusRequester: FocusRequester,
        destinationStateHolder: DestinationStateHolder?,
        actions: ActionProvider?,
        role: FocusRole = FocusRole.SEARCHABLE_ITEM,
    ) {
        focusableTargets[target] = FocusTarget(
            target,
            parent,
            role,
            coordinates.boundsInWindow(),
            focusRequester,
            destinationStateHolder,
            actions,
        )
    }

    fun unregisterFocusTarget(target: UUID) {
        focusableTargets.remove(target)
    }

    fun handlePointer(position: Offset) {
        lastPointerPosition = position
        val focusable = focusableTargets.firstNotNullOfOrNull { target ->
            target.takeIf {
                it.value.coordinates.contains(position)
            }
        }
        focusable?.value?.focusRequester?.requestFocus()
    }

    fun onSearchType(query: String) = handleSearchUpdate(query, navigating = false) {
        it.searchProvider.onSearchEnter(it.query)
    }

    fun onSearchEnter(query: String? = null) {
        handleSearchUpdate(query, navigating = true) {
            it.searchProvider.onSearchEnter(it.query)
            focusSearchResults(it.searchFocusContainer)
        }
    }

    private fun handleSearchUpdate(
        query: String?,
        navigating: Boolean,
        handleSuccess: (KeyboardActionMode.Search) -> Unit,
    ): Boolean {
        var success: KeyboardActionMode.Search? = null
        updateMode { mode ->
            if (mode is KeyboardActionMode.Search) {
                mode.copy(query = query ?: mode.query, navigating = navigating).also {
                    success = it
                }
            } else {
                val current = currentFocused() ?: focusableTargets.values.firstNotNullOfOrNull {
                    it.takeIf { it.actions?.searchProvider != null }
                }
                if (current?.actions?.searchProvider != null) {
                    KeyboardActionMode.Search(
                        query = query ?: "",
                        searchProvider = current.actions.searchProvider,
                        navigating = navigating,
                        searchFocusContainer = current.parent?.uuid,
                    ).also {
                        success = it
                    }
                } else {
                    success = null
                    log.w { "Updates search but no search provider available" }
                    mode
                }
            }
        }
        success?.let(handleSuccess)
        return success != null
    }
}
