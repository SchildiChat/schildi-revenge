package chat.schildi.revenge.actions

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.Clipboard
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.Destination
import chat.schildi.revenge.compose.focus.AbstractFocusRequester
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.AllowedSingleLineTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.KeyMapped
import chat.schildi.revenge.config.keybindings.KeyTrigger
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

val LocalKeyboardActionHandler = compositionLocalOf<KeyboardActionHandler> {
    throw IllegalArgumentException("No keyboard action handler provided")
}

private data class FocusTarget(
    val id: UUID,
    val parent: FocusParent?,
    val role: FocusRole,
    val coordinates: Rect,
    val focusRequester: AbstractFocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
)

enum class FocusRole(val consumesKeyWhitelist: List<Key>? = null) {
    SEARCHABLE_ITEM,
    AUX_ITEM,
    CONTAINER,
    TEXT_FIELD_SINGLE_LINE(consumesKeyWhitelist = AllowedSingleLineTextFieldBindingKeys),
    TEXT_FIELD_MULTI_LINE(consumesKeyWhitelist = AllowedTextFieldBindingKeys),
    MESSAGE_COMPOSER(consumesKeyWhitelist = AllowedTextFieldBindingKeys),
    SEARCH_BAR, // Does not need to consume plain keys, key handler has a special mode for that
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
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("Nav/$windowId")

    // Set once available via LocalCompositionProvider
    var focusManager: FocusManager? = null
    var clipboard: Clipboard? = null

    var windowCoordinates: Rect? = null
    private var lastPointerPosition = Offset.Zero
    private val _currentFocus = MutableStateFlow<UUID?>(null)
    val currentFocus = _currentFocus.asStateFlow()

    private val _mode = MutableStateFlow<KeyboardActionMode>(KeyboardActionMode.Navigation)
    val mode = _mode.asStateFlow()

    private val _keyboardPrimary = MutableStateFlow(true)
    val keyboardPrimary = combine(
        _keyboardPrimary,
        RevengePrefs.settingFlow(ScPrefs.ALWAYS_SHOW_KEYBOARD_FOCUS),
        Boolean::or,
    ).stateIn(scope, SharingStarted.Eagerly, false)

    val currentKeyboardFocus = combine(
        currentFocus,
        keyboardPrimary
    ) { focus, enabled ->
        focus?.takeIf { enabled }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val needsKeyboardSearchBar = mode.map { m ->
        m is KeyboardActionMode.Search
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val searchQuery = mode.map {
        (it as? KeyboardActionMode.Search)?.query ?: ""
    }

    private val focusableTargets = ConcurrentHashMap<UUID, FocusTarget>()

    private val pendingKeyTriggersInAction = ConcurrentHashMap<KeyTrigger, Unit>()

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
        _keyboardPrimary.value = true
        if (parentId == null || currentFocus == null || currentFocus.coordinates.isEmpty) {
            // No clue what to do, but maybe compose internals have an idea
            log.i { "moveFocus: Fall back to FocusManager without current focus for $currentFocus" }
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
        }?.focusRequester?.requestFocus()
            // E.g. at the bottom of a scrolled list, the focus manager can still get us to the next item
            ?: (focusManager?.moveFocus(focusDirection) == true)
    }

    private fun focusClosestTo(
        position: Offset,
        parentId: UUID? = null,
        role: FocusRole? = null,
    ): Boolean {
        _keyboardPrimary.value = true
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

    fun focusByRole(role: FocusRole): Boolean {
        _keyboardPrimary.value = true
        return focusableTargets.values.find { it.role == role }?.focusRequester?.requestFocus() ?: false
    }

    private fun currentFocused(fallbackToRoot: Boolean = true): FocusTarget? {
        currentFocus.value?.let {
            val target = focusableTargets[it]
            if (target == null) {
                log.w { "Unable to find target $it" }
            }
            return target
        }
        if (fallbackToRoot) {
            return focusableTargets.values.find { it.parent == null }
        }
        return null
    }

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

    private fun navigateCurrentDestination(buildDestination: (Destination) -> Destination?): Boolean {
        val destinationStateHolder = currentFocused()?.destinationStateHolder
        return destinationStateHolder?.state?.value.let { destinationState ->
            destinationState?.destination?.let { destination ->
                buildDestination(destination)?.let {
                    navigateCurrentDestination(
                        destination = it,
                        destinationStateHolder = destinationStateHolder,
                    )
                }
            }
        } ?: false
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        val trigger = event.toTrigger() ?: return false
        val focused = currentFocused()
        // Disallow plain keybindings of keys handled by text fields
        if (!event.isCtrlPressed && focused?.role?.consumesKeyWhitelist?.let { event.key in it } == false) {
            return false
        }
        return when (event.type) {
            KeyDown -> {
                val consumed = when (val mode = mode.value) {
                    is KeyboardActionMode.Navigation -> handleNavigationEvent(trigger, focused)
                    is KeyboardActionMode.Search -> handleSearchEvent(trigger, focused, mode)
                }
                if (consumed) {
                    pendingKeyTriggersInAction[trigger] = Unit
                }
                consumed
            }
            KeyUp -> {
                pendingKeyTriggersInAction.remove(trigger) != null
            }
            else -> false
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

    private fun handleSearchEvent(
        key: KeyTrigger,
        focused: FocusTarget?,
        mode: KeyboardActionMode.Search,
    ): Boolean {
        return when (key.rawKey) {
            KeyMapped.Escape -> {
                updateMode { KeyboardActionMode.Navigation }
                true
            }
            KeyMapped.Enter -> {
                if (mode.navigating) {
                    handleNavigationEvent(key, focused)
                } else {
                    updateMode { mode.copy(navigating = true) }
                    windowCoordinates?.let {
                        focusClosestTo(it.topCenter, role = FocusRole.SEARCHABLE_ITEM)
                    }
                    true
                }
            }
            KeyMapped.DirectionUp -> false // TODO cycle search history; configurable binding?
            KeyMapped.DirectionDown -> false // TODO cycle search history; configurable binding?
            else -> {
                if (mode.navigating) {
                    handleNavigationEvent(key, focused)
                } else {
                    false
                }
            }
        }
    }

    private fun focusSearchResults(parentId: UUID?) {
        focusClosestTo(Offset.Zero, role = FocusRole.SEARCHABLE_ITEM, parentId = parentId)
    }

    private fun focusCurrentContainerRelative(
        focused: FocusTarget? = currentFocused(),
        select: (Rect) -> Offset,
    ): Boolean {
        return windowCoordinates?.let { coordinates ->
            val parent = focused?.parent
            focusClosestTo(select(coordinates), parentId = parent?.uuid)
        } ?: false
    }

    private fun focusParent(): Boolean {
        val parent = currentFocused()?.parent
        log.v { "Focus parent: $parent" }
        parent ?: return false
        _currentFocus.value = parent.uuid
        return true
    }

    private fun scrollListToTop(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToTop(scope) {
            focusCurrentContainerRelative(focused) { it.topCenter }
        } ?: false
    }

    private fun scrollListToBottom(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToBottom(scope) {
            focusCurrentContainerRelative(focused) { it.bottomCenter }
        } ?: false
    }

    private fun handleNavigationEvent(key: KeyTrigger, focused: FocusTarget?): Boolean {
        if (focused?.actions?.keyActions?.handleNavigationModeEvent(key) == true) {
            // Allow focused-item specific handling
            return true
        }
        val keyConfig = UiState.keybindingsConfig.value

        (focused?.actions?.primaryAction as? InteractionAction.NavigationAction)?.let { navigationActionable ->
            val navigationAction = keyConfig.navigationItem.find { it.trigger == key}
            if (navigationAction != null) {
                val destination = navigationActionable.buildDestination()
                return when (navigationAction.action) {
                    Action.NavigationItem.NavigateCurrent -> {
                        val destinationStateHolder = focused.destinationStateHolder ?: return false
                        navigateCurrentDestination(destination, destinationStateHolder)
                    }
                    Action.NavigationItem.NavigateInNewWindow -> {
                        UiState.openWindow(destination, navigationActionable.initialTitle)
                        true
                    }
                }
            }
        }

        val listAction = keyConfig.list.find { it.trigger == key }
        if (listAction != null) {
            return when (listAction.action) {
                Action.List.ScrollToTop -> scrollListToTop(focused)
                Action.List.ScrollToBottom -> scrollListToBottom(focused)
            }
        }
        val focusAction = keyConfig.focus.find { it.trigger == key }
        if (focusAction != null) {
            return when (focusAction.action) {
                Action.Focus.FocusUp -> moveFocus(FocusDirection.Up)
                Action.Focus.FocusDown -> moveFocus(FocusDirection.Down)
                Action.Focus.FocusLeft -> moveFocus(FocusDirection.Left)
                Action.Focus.FocusRight -> moveFocus(FocusDirection.Right)
                Action.Focus.FocusTop -> focusCurrentContainerRelative { it.topCenter } // TODO keep X offset rather than assuming center
                Action.Focus.FocusCenter -> focusCurrentContainerRelative { it.center } // TODO keep X offset rather than assuming center
                Action.Focus.FocusBottom -> focusCurrentContainerRelative { it.bottomCenter } // TODO keep X offset rather than assuming center
                Action.Focus.FocusParent -> focusParent()
            }
        }
        val navigationAction = keyConfig.navigation.find { it.trigger == key }
        if (navigationAction != null) {
            return when (navigationAction.action) {
                Action.Navigation.InboxInCurrent -> navigateCurrentDestination { Destination.Inbox }
                Action.Navigation.AccountManagementInCurrent -> navigateCurrentDestination {
                    Destination.AccountManagement
                }
                Action.Navigation.InboxInNewWindow -> {
                    UiState.openWindow(Destination.Inbox)
                    true
                }
                Action.Navigation.AccountManagementInNewWindow -> {
                    UiState.openWindow(Destination.AccountManagement)
                    true
                }
                Action.Navigation.SplitHorizontal -> navigateCurrentDestination {
                    Destination.SplitHorizontal(
                        DestinationStateHolder.forInitialDestination(it),
                        DestinationStateHolder.forInitialDestination(it),
                    )
                }
                Action.Navigation.SplitVertical -> navigateCurrentDestination {
                    Destination.SplitVertical(
                        DestinationStateHolder.forInitialDestination(it),
                        DestinationStateHolder.forInitialDestination(it),
                    )
                }
            }
        }
        val globalAction = keyConfig.global.find { it.trigger == key }
        if (globalAction != null) {
            return when (globalAction.action) {
                Action.Global.Search -> handleSearchUpdate("", navigating = false) {
                    // TODO search -> search-nav -> search inserts a slash into search field by accident
                    focusByRole(FocusRole.SEARCH_BAR)
                }
                Action.Global.ToggleTheme -> {
                    UiState.darkThemeOverride.update {
                        // TODO if override is null, read automatic theme
                        !(it ?: false)
                    }
                    true
                }
                Action.Global.AutomaticTheme -> {
                    UiState.darkThemeOverride.value = null
                    true
                }
                Action.Global.ToggleHiddenItems -> {
                    // TODO move to normal ScPref
                    UiState.setShowHiddenItems(!UiState.showHiddenItems.value)
                    true
                }
                Action.Global.SetSetting -> {
                    scope.launch {
                        RevengePrefs.handleSetAction(globalAction.args)
                    }
                    true
                }
                Action.Global.ToggleSetting -> {
                    scope.launch {
                        RevengePrefs.handleToggleAction(globalAction.args)
                    }
                    true
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
        //log.v { "Focus changed for $target to $state" }
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
        focusRequester: AbstractFocusRequester,
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
        if (lastPointerPosition != position) {
            lastPointerPosition = position
            _keyboardPrimary.value = false
        }
        val focusable = focusableTargets.values.firstNotNullOfOrNull { target ->
            target.takeIf {
                it.role != FocusRole.CONTAINER && it.coordinates.contains(position)
            }
        }
        // TODO flow + debounce + separate coroutine to avoid messing with composition
        focusable?.focusRequester?.requestFocus()
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

private fun KeyEvent.toTrigger(): KeyTrigger? {
    val rawKey = KeyMapped.entries.find { it.key.keyCode == key.keyCode } ?: return null
    return KeyTrigger(
        rawKey = rawKey,
        shift = isShiftPressed,
        alt = isAltPressed,
        ctrl = isCtrlPressed,
    )
}
