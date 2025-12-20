package chat.schildi.revenge.actions

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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.window.ApplicationScope
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.collectScPrefs
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.Destination
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.compose.focus.AbstractFocusRequester
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.AllowedComposerTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedSingleLineTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.Binding
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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt

val LocalKeyboardActionHandler = compositionLocalOf<KeyboardActionHandler> {
    throw IllegalArgumentException("No keyboard action handler provided")
}

private data class FocusTarget(
    val id: UUID,
    val parent: FocusParent?,
    val role: FocusRole,
    val coordinates: Rect,
    val isFullyVisible: Boolean,
    val focusRequester: AbstractFocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
)

enum class FocusRole(val consumesKeyWhitelist: List<Key>? = null) {
    LIST_ITEM,
    AUX_ITEM,
    NESTED_AUX_ITEM,
    DESTINATION_ROOT_CONTAINER,
    CONTAINER,
    CONTAINER_ITEM, // Can both like AUX_ITEM and CONTAINER
    TEXT_FIELD_SINGLE_LINE(consumesKeyWhitelist = AllowedSingleLineTextFieldBindingKeys),
    TEXT_FIELD_MULTI_LINE(consumesKeyWhitelist = AllowedTextFieldBindingKeys),
    MESSAGE_COMPOSER(consumesKeyWhitelist = AllowedComposerTextFieldBindingKeys),
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
    private val scope: CoroutineScope,
    private val windowId: Int,
    private val applicationScope: ApplicationScope,
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
        allowPartial: Boolean,
        parentId: UUID? = null,
        role: FocusRole? = null,
    ): Boolean {
        _keyboardPrimary.value = true
        val filtered = if (parentId == null && role == null) {
            focusableTargets.values
        } else {
            focusableTargets.values.filter {
                (role == null || it.role == role) &&
                        (allowPartial || it.isFullyVisible) &&
                        (parentId == null || it.parent?.uuid == parentId)
            }
        }
        return filtered.minByOrNull {
            distanceToRect(it.coordinates, position)
        }?.focusRequester?.requestFocus() ?: false
    }

    fun focusByRole(role: FocusRole): Boolean {
        _keyboardPrimary.value = true
        val focusRequester = focusableTargets.values.find { it.role == role }?.focusRequester
        return if (focusRequester != null) {
            // Don't immediately request focus, it causes issues where text fields
            // may still consume the key in addition to us focusing it
            scope.launch {
                focusRequester.requestFocus()
            }
            true
        } else {
            false
        }
    }

    private fun currentFocused(fallbackToRoot: Boolean = true): FocusTarget? {
        currentFocus.value?.let {
            val target = focusableTargets[it]
            if (target == null) {
                log.w { "Unable to find target $it" }
            } else {
                return target
            }
        }
        if (fallbackToRoot) {
            val root = focusableTargets.values.find { it.parent == null }
            log.i { "No active focus, use root ${root?.id}" }
            return root
        }
        return null
    }

    fun executeAction(
        action: InteractionAction,
        destinationStateHolder: DestinationStateHolder? = null
    ): Boolean {
        return when (action) {
            is InteractionAction.NavigationAction -> {
                val destination = action.buildDestination()
                when (action) {
                    is InteractionAction.OpenWindow -> {
                        UiState.openWindow(destination, action.initialTitle)
                        true
                    }
                    is InteractionAction.NavigateCurrent -> {
                        updateMode { KeyboardActionMode.Navigation }
                        navigateCurrentDestination(destination, destinationStateHolder)
                    }
                }
            }
            is InteractionAction.Invoke -> action.invoke()
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
                    is KeyboardActionMode.Navigation -> handleNavigationEvent(trigger, focused) is ActionResult.Actioned
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
                    handleNavigationEvent(key, focused) is ActionResult.Actioned
                } else {
                    updateMode { mode.copy(navigating = true) }
                    windowCoordinates?.let {
                        focusClosestTo(it.topCenter, allowPartial = true, role = FocusRole.LIST_ITEM)
                    }
                    true
                }
            }
            KeyMapped.DirectionUp -> false // TODO cycle search history; configurable binding?
            KeyMapped.DirectionDown -> false // TODO cycle search history; configurable binding?
            else -> {
                if (mode.navigating) {
                    handleNavigationEvent(key, focused) is ActionResult.Actioned
                } else {
                    false
                }
            }
        }
    }

    private fun focusSearchResults(parentId: UUID?) {
        focusClosestTo(Offset.Zero, allowPartial = true, role = FocusRole.LIST_ITEM, parentId = parentId)
    }

    private fun focusCurrentContainerRelative(
        currentFocus: FocusTarget?,
        role: FocusRole? = null,
        select: (Rect) -> Offset,
    ) = focusCurrentContainerRelative(parentId =  currentFocus?.parent?.uuid, select = select, role = role)

    private fun focusCurrentContainerRelative(
        parentId: UUID? = currentFocused()?.parent?.uuid,
        role: FocusRole? = null,
        select: (Rect) -> Offset,
    ): Boolean {
        return windowCoordinates?.let { coordinates ->
            focusClosestTo(select(coordinates), allowPartial = false, parentId = parentId, role = role)
        } ?: false
    }

    private fun focusParent(focused: FocusTarget? = currentFocused()): Boolean {
        val parent = focused?.parent
        log.v { "Focus parent: $parent" }
        parent ?: return false
        _currentFocus.value = parent.uuid
        return true
    }

    private fun focusEnterContainer(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        focused ?: return false
        if (focused.role != FocusRole.CONTAINER && focused.role != FocusRole.CONTAINER_ITEM) {
            return false
        }
        return focusCurrentContainerRelative(focused.id) { it.topCenter }
    }

    private fun scrollListToTop(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToTop(scope) {
            focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.topCenter }
        } ?: false
    }

    private fun scrollListToBottom(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToBottom(scope) {
            focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.bottomCenter }
        } ?: false
    }

    private fun scrollListToStart(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToStart(scope) {
            if (focused.actions.listActions.isReverseList) {
                focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.bottomCenter }
            } else {
                focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.topCenter }
            }
        } ?: false
    }

    private fun scrollListToEnd(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToEnd(scope) {
            if (focused.actions.listActions.isReverseList) {
                focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.topCenter }
            } else {
                focusCurrentContainerRelative(focused, FocusRole.LIST_ITEM) { it.bottomCenter }
            }
        } ?: false
    }

    private fun handleNavigationEvent(key: KeyTrigger, focused: FocusTarget?): ActionResult {
        val currentDestination = focused?.destinationStateHolder?.state?.value?.destination
        val currentDestinationName = currentDestination?.name
        val keyConfig = UiState.keybindingsConfig.value

        return ActionResult.chain(
            {
                focused?.actions?.keyActions?.handleNavigationModeEvent(key, currentDestinationName) ?: ActionResult.NoMatch
            },
            {
                (focused?.actions?.primaryAction as? InteractionAction.NavigationAction)?.let { navigationActionable ->
                    keyConfig.navigationItem.execute(key, currentDestinationName) { navigationAction ->
                        val destination = navigationActionable.buildDestination()
                        when (navigationAction.action) {
                            Action.NavigationItem.NavigateCurrent -> {
                                val destinationStateHolder = focused.destinationStateHolder ?: return@execute ActionResult.Inapplicable
                                updateMode { KeyboardActionMode.Navigation }
                                navigateCurrentDestination(destination, destinationStateHolder).orActionInapplicable()
                            }
                            Action.NavigationItem.NavigateInNewWindow -> {
                                UiState.openWindow(destination, navigationActionable.initialTitle)
                                ActionResult.Success()
                            }
                        }
                    }
                } ?: ActionResult.NoMatch
            },
            {
                keyConfig.list.execute(key, currentDestinationName) { listAction ->
                    when (listAction.action) {
                        Action.List.ScrollToTop -> scrollListToTop(focused).orActionInapplicable()
                        Action.List.ScrollToBottom -> scrollListToBottom(focused).orActionInapplicable()
                        Action.List.ScrollToStart -> scrollListToStart(focused).orActionInapplicable()
                        Action.List.ScrollToEnd -> scrollListToEnd(focused).orActionInapplicable()
                    }
                }
            },
            {
                keyConfig.focus.execute(key, currentDestinationName) { focusAction ->
                    when (focusAction.action) {
                        Action.Focus.FocusUp -> moveFocus(FocusDirection.Up, focused)
                        Action.Focus.FocusDown -> moveFocus(FocusDirection.Down, focused)
                        Action.Focus.FocusLeft -> moveFocus(FocusDirection.Left, focused)
                        Action.Focus.FocusRight -> moveFocus(FocusDirection.Right, focused)
                        Action.Focus.FocusTop -> focusCurrentContainerRelative(focused) { it.topCenter } // TODO keep X offset rather than assuming center
                        Action.Focus.FocusCenter -> focusCurrentContainerRelative(focused) { it.center } // TODO keep X offset rather than assuming center
                        Action.Focus.FocusBottom -> focusCurrentContainerRelative(focused) { it.bottomCenter } // TODO keep X offset rather than assuming center
                        Action.Focus.FocusParent -> focusParent(focused)
                        Action.Focus.FocusEnterContainer -> focusEnterContainer(focused)
                    }.orActionInapplicable()
                }
            },
            {
                keyConfig.navigation.execute(key, currentDestinationName) { navigationAction ->
                    when (navigationAction.action) {
                        Action.Navigation.NavigateCurrent -> {
                            val destination = navigationAction.args[0].toDestinationOrNull().orActionValidationError()
                            navigateCurrentDestination { destination }.orActionInapplicable()
                        }
                        Action.Navigation.NavigateInNewWindow -> {
                            // We should have checked this already
                            val destination = navigationAction.args[0].toDestinationOrNull().orActionValidationError()
                            UiState.openWindow(destination)
                            ActionResult.Success()
                        }
                        Action.Navigation.SplitHorizontal -> navigateCurrentDestination {
                            Destination.SplitHorizontal(
                                DestinationStateHolder.forInitialDestination(it),
                                DestinationStateHolder.forInitialDestination(it),
                            )
                        }.orActionInapplicable()
                        Action.Navigation.SplitVertical -> navigateCurrentDestination {
                            Destination.SplitVertical(
                                DestinationStateHolder.forInitialDestination(it),
                                DestinationStateHolder.forInitialDestination(it),
                            )
                        }.orActionInapplicable()
                        Action.Navigation.CloseWindow -> {
                            UiState.closeWindow(windowId, applicationScope)
                            ActionResult.Success()
                        }
                    }
                }
            },
            {
                keyConfig.global.execute(key, currentDestinationName) { globalAction ->
                    when (globalAction.action) {
                        Action.Global.Search -> {
                            if (mode.value is KeyboardActionMode.Search) {
                                // Search already active, just focus again
                                focusByRole(FocusRole.SEARCH_BAR).orActionInapplicable()
                            } else {
                                handleSearchUpdate("", navigating = false) {
                                    focusByRole(FocusRole.SEARCH_BAR)
                                }.orActionInapplicable()
                            }
                        }
                        Action.Global.ToggleTheme -> {
                            UiState.darkThemeOverride.update {
                                // TODO if override is null, read automatic theme
                                !(it ?: false)
                            }
                            ActionResult.Success()
                        }
                        Action.Global.AutomaticTheme -> {
                            UiState.darkThemeOverride.value = null
                            ActionResult.Success()
                        }
                        Action.Global.ToggleHiddenItems -> {
                            // TODO move to normal ScPref
                            UiState.setShowHiddenItems(!UiState.showHiddenItems.value)
                            ActionResult.Success()
                        }
                        Action.Global.SetSetting -> {
                            scope.launch {
                                RevengePrefs.handleSetAction(globalAction.args)
                            }
                            ActionResult.Success()
                        }
                        Action.Global.ToggleSetting -> {
                            scope.launch {
                                RevengePrefs.handleToggleAction(globalAction.args)
                            }
                            ActionResult.Success()
                        }
                    }
                }
            }
        )

        return ActionResult.NoMatch
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
        role: FocusRole = FocusRole.LIST_ITEM,
    ) {
        val bounds = coordinates.boundsInWindow()
        focusableTargets[target] = FocusTarget(
            target,
            parent,
            role,
            bounds,
            bounds.size.toIntSize() == coordinates.size,
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
                it.isFullyVisible &&
                        it.role != FocusRole.CONTAINER &&
                        it.role != FocusRole.DESTINATION_ROOT_CONTAINER &&
                        it.role != FocusRole.NESTED_AUX_ITEM &&
                        it.coordinates.contains(position)
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

    fun copyToClipboard(content: String): ActionResult {
        val localClipboard = clipboard ?: return ActionResult.Failure("No clipboard found")
        launchActionAsync("copyToClipboard", scope) {
            localClipboard.setClipEntry(
                ClipEntry(java.awt.datatransfer.StringSelection(content))
            )
            // TODO toast or something
            ActionResult.Success()
        }
        return ActionResult.Success()
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

sealed interface ActionResult {
    val shouldExit: Boolean
    fun withChainSetting(chain: Boolean): ActionResult = this

    sealed interface Actioned : ActionResult

    data class Success(
        // True after launching a coroutine that may still fail, causing the action to be considered "failed" eventually later
        val async: Boolean = false,
        override val shouldExit: Boolean = true
    ) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class Failure(val message: String, override val shouldExit: Boolean = true) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class Malformed(val message: String, override val shouldExit: Boolean = true) : ActionResult {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data object Inapplicable : ActionResult {
        override val shouldExit = false
    }
    data object NoMatch : ActionResult {
        override val shouldExit = false
    }
    companion object {
        fun chain(vararg actionHandlers: () -> ActionResult): ActionResult {
            var hasChainableSuccess = false
            actionHandlers.forEach { handler ->
                val actionResult = handler()
                if (actionResult.shouldExit) {
                    return actionResult
                }
                if (actionResult is Success) {
                    hasChainableSuccess = true
                }
            }
            return if (hasChainableSuccess) Success(shouldExit = false) else NoMatch
        }
    }
}

class ActionValidationException() : Exception("Internal action parsing validation error")

fun Boolean.orActionInapplicable() = if (this) ActionResult.Success() else ActionResult.Inapplicable
fun Boolean.orActionFailure(message: String) = if (this) ActionResult.Success() else ActionResult.Failure(message)
fun <T>T?.orActionValidationError() = this ?: throw ActionValidationException()

/**
 * Try all possible bindings for a given key until the first one returns true.
 */
fun <A: Action>List<Binding<A>>.execute(
    key: KeyTrigger,
    currentDestinationName: String?,
    block: (Binding<A>) -> ActionResult
): ActionResult {
    val actions = filter {
        it.trigger == key && (it.destinations.isEmpty() || it.destinations.contains(currentDestinationName))
    }
    var hasChainableSuccess = false
    actions.forEach { action ->
        val actionResult = try {
            action.checkArguments() ?: block(action).withChainSetting(action.chain)
        } catch (e: IndexOutOfBoundsException) {
            Logger.e("Error executing action", e)
            ActionResult.Failure(e.message ?: "Exception occurred trying to execute action")
        } catch (e: ActionValidationException) {
            Logger.e("Error executing action", e)
            ActionResult.Failure(e.message ?: "Exception occurred trying to execute action")
        }
        if (actionResult.shouldExit) {
            return actionResult
        }
        if (actionResult is ActionResult.Success) {
            hasChainableSuccess = true
        }
    }
    return if (hasChainableSuccess) ActionResult.Success(shouldExit = false) else ActionResult.NoMatch
}

fun Binding<*>.checkArguments(
    validSessionIds: List<String>? = UiState.currentValidSessionIds.value,
    validSettingKeys: List<String> = ScPrefs.validSettingKeys,
): ActionResult.Malformed? {
    val actionName = javaClass.simpleName
    if (action.args.size != args.size) {
        return ActionResult.Malformed(
            "Invalid parameter size for $actionName, expected ${action.args.size} got ${args.size}"
        )
    }
    action.args.zip(args).forEach { (argDef, argVal) ->
        when (argDef) {
            ActionArgument.Text -> {}
            ActionArgument.Boolean -> {
                if (argVal.toBooleanStrictOrNull() == null) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected boolean got $argVal"
                    )
                }
            }
            ActionArgument.Integer -> {
                if (argVal.toIntOrNull() == null) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected int got $argVal"
                    )
                }
            }
            ActionArgument.SessionId,
            ActionArgument.Mxid -> {
                if (argDef == ActionArgument.SessionId && validSessionIds != null) {
                    if (!validSessionIds.contains(argVal)) {
                        return ActionResult.Malformed(
                            "Invalid parameter for $actionName, not an existing user login: $argVal"
                        )
                    }
                } else {
                    // TODO full mxid regex checking
                    if (!argVal.startsWith("@") || !argVal.contains(":")) {
                        return ActionResult.Malformed(
                            "Invalid parameter for $actionName, expected MXID got $argVal"
                        )
                    }
                }
            }
            ActionArgument.RoomId -> {
                if (!argVal.startsWith("!")) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected room ID got $argVal"
                    )
                }
            }
            ActionArgument.EventId -> {
                if (!argVal.startsWith("$")) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected room ID got $argVal"
                    )
                }
            }
            ActionArgument.SettingKey -> {
                if (argVal !in validSettingKeys) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, not a valid settings key: $argVal"
                    )
                }
            }
            ActionArgument.NavigatableDestinationName -> {
                if (argVal.toDestinationOrNull() == null) {
                    return ActionResult.Malformed(
                        "Invalid parameter for $actionName, not a valid destination: $argVal"
                    )
                }
            }
        }
    }
    return null
}

fun launchActionAsync(
    actionName: String,
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> ActionResult,
): ActionResult {
    // TODO track error for UI and stuff
    scope.launch(context) {
        val result = block()
        if (result is ActionResult.Failure) {
            Logger.withTag("AsyncAction").e("Failed to execute $actionName: ${result.message}")
        }
    }
    return ActionResult.Success(async = true)
}

private fun String.toDestinationOrNull() = when (lowercase()) {
    "inbox" -> Destination.Inbox
    "accountmanagement",
    "accounts" -> Destination.AccountManagement
    else -> null
}
