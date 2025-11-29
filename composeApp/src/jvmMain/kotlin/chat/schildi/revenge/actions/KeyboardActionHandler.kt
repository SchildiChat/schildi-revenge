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
    val role: FocusRole,
    val coordinates: Rect,
    val focusRequester: FocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
    val searchable: Boolean,
)

enum class FocusRole {
    ITEM,
    SEARCH_BAR,
}

sealed interface KeyboardActionMode {
    data object Navigation : KeyboardActionMode
    data class Search(
        val query: String,
        val searchProvider: SearchProvider,
        val navigating: Boolean,
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

    private fun moveFocus(focusDirection: FocusDirection): Boolean {
        return focusManager?.moveFocus(focusDirection) == true ||
                focusableTargets.values.firstOrNull()?.focusRequester?.let {
                    log.i { "Could not move focus to $focusDirection, force focus anything" }
                    it.requestFocus()
                } ?: false
    }

    private fun focusClosestTo(position: Offset, onlySearchable: Boolean = false): Boolean {
        val filtered = if (onlySearchable) {
            focusableTargets.values.filter { it.searchable }
        } else {
            focusableTargets.values
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
            is KeyboardActionMode.Navigation -> handleNavigationEvent(event, mode)
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
                    handleNavigationEvent(event, mode)
                } else {
                    updateMode { mode.copy(navigating = true) }
                    windowCoordinates?.let {
                        focusClosestTo(it.topCenter, onlySearchable = true)
                    }
                    true
                }
            }
            Key.DirectionUp -> false // TODO cycle search history
            Key.DirectionDown -> false // TODO cycle search history
            else -> {
                if (mode.navigating) {
                    handleNavigationEvent(event, mode)
                } else {
                    false
                }
            }
        }
    }

    private fun handleNavigationEvent(event: KeyEvent, mode: KeyboardActionMode): Boolean {
        // TODO disallow focusing non-search results while in search mode or sth... how to do with focus direction?
        if (event.type == KeyDown) {
            // TODO make this configurable
            return if (event.isShiftPressed) {
                when (event.key) {
                    // Relative focus - TODO should maintain X offset rather than assuming center
                    Key.H -> windowCoordinates?.let { focusClosestTo(it.topCenter) } ?: false
                    Key.M -> windowCoordinates?.let { focusClosestTo(it.center) } ?: false
                    Key.L -> windowCoordinates?.let { focusClosestTo(it.bottomCenter) } ?: false
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
                    // Primary action (usually same as mouse click)
                    Key.Enter -> currentFocused()?.actions?.primaryAction?.let(::executeAction) ?: false
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
        coordinates: LayoutCoordinates,
        focusRequester: FocusRequester,
        destinationStateHolder: DestinationStateHolder?,
        actions: ActionProvider?,
        searchable: Boolean = false,
        role: FocusRole = FocusRole.ITEM,
    ) {
        focusableTargets[target] = FocusTarget(
            role,
            coordinates.boundsInWindow(),
            focusRequester,
            destinationStateHolder,
            actions,
            searchable,
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
        }
        windowCoordinates?.let {
            focusClosestTo(it.topCenter, onlySearchable = true)
        }
    }

    private fun handleSearchUpdate(
        query: String?,
        navigating: Boolean,
        handleSuccess: (KeyboardActionMode.Search) -> Unit,
    ): Boolean {
        var success: KeyboardActionMode.Search? = null
        updateMode { mode ->
            (mode as? KeyboardActionMode.Search)?.let {
                it.copy(query = query ?: it.query, navigating = navigating).also {
                    success = it
                }
            } ?: currentFocused()?.let { it.actions?.searchProvider }?.let {
                KeyboardActionMode.Search(
                    query = query ?: "",
                    searchProvider = it,
                    navigating = navigating,
                ).also {
                    success = it
                }
            } ?: focusableTargets.values.firstNotNullOfOrNull { it.actions?.searchProvider }?.let {
                KeyboardActionMode.Search(
                    query = query ?: "",
                    searchProvider = it,
                    navigating = navigating,
                ).also {
                    success = it
                }
            } ?: run {
                success = null
                log.w { "Updates search but no search provider available" }
                mode
            }
        }
        success?.let(handleSuccess)
        return success != null
    }
}
