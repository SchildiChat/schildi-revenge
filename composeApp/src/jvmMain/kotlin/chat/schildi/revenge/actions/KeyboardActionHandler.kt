package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.window.ApplicationScope
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.findPreference
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.Destination
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.compose.focus.AbstractFocusRequester
import chat.schildi.revenge.compose.focus.FakeFocusRequester
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.ALLOWED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentContextBased
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.AllowedComposerTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedSingleLineTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.Binding
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.KeyMapped
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.config.keybindings.KeybindingConfig
import chat.schildi.revenge.config.keybindings.findAll
import chat.schildi.revenge.config.keybindings.minArgsSize
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_PREFIX
import chat.schildi.revenge.model.spaces.REAL_SPACE_ID_PREFIX
import co.touchlab.kermit.Logger
import io.element.android.libraries.core.coroutine.childScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_cancel
import shire.composeapp.generated.resources.action_processing
import shire.composeapp.generated.resources.action_processing_done
import shire.composeapp.generated.resources.command_ambiguous
import shire.composeapp.generated.resources.command_ambiguous_none_valid
import shire.composeapp.generated.resources.command_copied_to_clipboard
import shire.composeapp.generated.resources.command_not_applicable
import shire.composeapp.generated.resources.command_not_found
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map
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

enum class FocusRole(val consumesKeyWhitelist: List<Key>? = null, val autoRequestFocus: Boolean = false) {
    LIST_ITEM,
    AUX_ITEM,
    NESTED_AUX_ITEM,
    DESTINATION_ROOT_CONTAINER,
    CONTAINER,
    CONTAINER_ITEM, // Can both like AUX_ITEM and CONTAINER
    TEXT_FIELD_SINGLE_LINE(consumesKeyWhitelist = AllowedSingleLineTextFieldBindingKeys),
    TEXT_FIELD_MULTI_LINE(consumesKeyWhitelist = AllowedTextFieldBindingKeys),
    MESSAGE_COMPOSER(consumesKeyWhitelist = AllowedComposerTextFieldBindingKeys),
    SEARCH_BAR(autoRequestFocus = true), // Does not need to consume plain keys, key handler has a special mode for that
    COMMAND_BAR(autoRequestFocus = true), // Does not need to consume plain keys, key handler has a special mode for that
}

sealed interface KeyboardActionMode {
    data object Navigation : KeyboardActionMode
    data class Search(
        val query: String,
        val searchProvider: SearchProvider,
        val navigating: Boolean,
        val searchFocusContainer: UUID?,
    ) : KeyboardActionMode
    data class Command(
        val query: TextFieldValue,
        // Fix the item we want to action on
        val focused: UUID?,
        val suggestionsProvider: CommandSuggestionsProvider,
        val selectedSuggestion: String?,
    ) : KeyboardActionMode
}

sealed interface AbstractAppMessage {
    val message: ComposableStringHolder
    val timestamp: Long
    val uniqueId: String?
    val canAutoDismiss: Boolean
    val dismissedTimestamp: Long?
    fun copyDismissed(dismissedTimestamp: Long): AbstractAppMessage
}

data class AppMessage(
    override val message: ComposableStringHolder,
    val isError: Boolean = false,
    override val timestamp: Long = System.currentTimeMillis(),
    override val uniqueId: String? = null,
    override val canAutoDismiss: Boolean = true,
    override val dismissedTimestamp: Long? = null,
) : AbstractAppMessage {
    override fun copyDismissed(dismissedTimestamp: Long) = copy(dismissedTimestamp = dismissedTimestamp)
}

data class ConfirmActionAppMessage(
    override val message: ComposableStringHolder,
    override val timestamp: Long = System.currentTimeMillis(),
    override val dismissedTimestamp: Long? = null,
    val confirmText: ComposableStringHolder,
    val cancelText: ComposableStringHolder = StringResourceHolder(Res.string.action_cancel),
    val onDismiss: () -> Unit = {},
    val action: () -> Unit,
) : AbstractAppMessage {
    override val uniqueId = MESSAGE_ID
    override val canAutoDismiss = false
    override fun copyDismissed(dismissedTimestamp: Long) = copy(dismissedTimestamp = dismissedTimestamp).also {
        onDismiss()
    }
    companion object {
        const val MESSAGE_ID = "confirmAction"
    }
}

// TODO config or something
const val MESSAGE_EXPIRY_DURATION = 5000L
private const val COMMAND_MESSAGE_ID = "cmd"

data class FocusState(
    val keyboardFocus: UUID? = null,
    val commandFocus: UUID? = null,
)

private data class KeyboardActionHandlerSettings(
    val alwaysShowKeyboardFocus: Boolean,
    val focusFollowsMouse: Boolean,
) {
    companion object {
        fun from(lookup: (ScPref<*>) -> Any?) = KeyboardActionHandlerSettings(
            alwaysShowKeyboardFocus = ScPrefs.ALWAYS_SHOW_KEYBOARD_FOCUS.safeLookup(lookup),
            focusFollowsMouse = ScPrefs.FOCUS_FOLLOWS_MOUSE.safeLookup(lookup),
        )
    }
}

class KeyboardActionHandler(
    private val scope: CoroutineScope,
    private val windowId: Int,
    private val applicationScope: ApplicationScope,
) {
    private val log = Logger.withTag("Nav/$windowId")

    private val _messageBoard = MutableStateFlow<ImmutableList<AbstractAppMessage>>(persistentListOf())
    val messageBoard = _messageBoard.asStateFlow()

    // Set once available via LocalCompositionProvider
    var focusManager: FocusManager? = null
    var clipboard: Clipboard? = null
    var uriHandler: UriHandler? = null

    var windowCoordinates: Rect? = null
    private var _lastPointerPosition = Offset.Zero
    val lastPointerPosition: Offset
        get() = _lastPointerPosition
    private val currentFocus = MutableStateFlow<UUID?>(null)

    private val _mode = MutableStateFlow<KeyboardActionMode>(KeyboardActionMode.Navigation)
    val mode = _mode.asStateFlow()

    private val _keyboardPrimary = MutableStateFlow(true)
    val keyboardPrimary = combine(
        _keyboardPrimary,
        RevengePrefs.settingFlow(ScPrefs.ALWAYS_SHOW_KEYBOARD_FOCUS),
        Boolean::or,
    ).stateIn(scope, SharingStarted.Eagerly, false)

    private val handlerSettings = RevengePrefs.combinedSettingFlow { lookup ->
        KeyboardActionHandlerSettings.from(lookup)
    }.stateIn(scope, SharingStarted.Eagerly,
        KeyboardActionHandlerSettings.from {
            RevengePrefs.getCachedOrDefaultValue(it)
        }
    )

    private val _currentOpenContextMenu = MutableStateFlow<UUID?>(null)
    val currentOpenContextMenu = _currentOpenContextMenu.asStateFlow()

    val currentFocusState = combine(
        currentFocus,
        mode,
        keyboardPrimary,
    ) { focused, currentMode, keyboardEnabled ->
        FocusState(
            keyboardFocus = focused.takeIf { keyboardEnabled },
            commandFocus = (currentMode as? KeyboardActionMode.Command)?.focused,
        )
    }.stateIn(scope, SharingStarted.Eagerly, FocusState())

    val needsKeyboardSearchBar = mode.map { m ->
        m is KeyboardActionMode.Search
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val searchQuery = mode.map {
        (it as? KeyboardActionMode.Search)?.query ?: ""
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val commandSuggestionsState = mode.flatMapLatest { mode ->
        (mode as? KeyboardActionMode.Command)?.suggestionsProvider?.suggestionState?.map {
            Pair(mode, it)
        } ?: flowOf(null)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val focusableTargets = ConcurrentHashMap<UUID, FocusTarget>()

    private val pendingKeyTriggersInAction = ConcurrentHashMap<KeyTrigger, Unit>()

    init {
        UiState.globalMessageBoard.onEach {
            publishMessage(it)
        }.launchIn(scope)
    }

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

    private fun ActionContext.focused() = (this as? InternalActionContext)?.focused ?: currentFocused()

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
            is InteractionAction.ContextMenu -> openContextMenu(action.focusId)
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

    private fun navigateCurrentDestination(
        destinationStateHolder: DestinationStateHolder? = currentFocused()?.destinationStateHolder,
        buildDestination: (Destination) -> Destination?
    ): Boolean {
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

    fun publishMessage(
        message: AbstractAppMessage,
    ) {
        _messageBoard.update {
            val filtered = if (message.uniqueId == null) {
                it
            } else {
                it.filter { it.uniqueId != message.uniqueId }
            }
            (filtered + message).toPersistentList()
        }
    }

    fun dismissMessage(uniqueId: String) {
        val now = System.currentTimeMillis()
        _messageBoard.update {
            it.map {
                if (it.uniqueId == uniqueId) {
                    it.copyDismissed(dismissedTimestamp = it.dismissedTimestamp ?: now)
                } else {
                    it
                }
            }.toPersistentList()
        }
    }

    fun cleanUpMessageBoard() {
        val now = System.currentTimeMillis()
        _messageBoard.update {
            if (it.isEmpty()) {
                it
            } else {
                it.mapNotNull {
                    if (it.dismissedTimestamp?.let { now > it + MESSAGE_EXPIRY_DURATION * 2 } == true) {
                        null
                    } else if (it.canAutoDismiss && now > it.timestamp + MESSAGE_EXPIRY_DURATION) {
                        it.copyDismissed(dismissedTimestamp = now)
                    } else {
                        it
                    }
                }.toPersistentList()
            }
        }
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        val trigger = event.toTrigger() ?: return false
        val focused = currentFocused()
        val contextMenu = _currentOpenContextMenu.value?.let {
            focusableTargets[it]?.actions?.findInteractionAction<InteractionAction.ContextMenu>()?.takeIf {
                it.entries.isNotEmpty()
            }
        }
        // Disallow plain keybindings of keys handled by text fields
        if (!event.isCtrlPressed && contextMenu == null &&
            focused?.role?.consumesKeyWhitelist?.let { event.key in it } == false
        ) {
            return false
        }
        return when (event.type) {
            KeyDown -> {
                val consumed = if (contextMenu != null) {
                     handleContextMenuEvent(event, contextMenu)
                } else when (val mode = mode.value) {
                    is KeyboardActionMode.Navigation -> {
                        val result = handleNavigationEvent(trigger, focused)
                        if (result is ActionResult.Failure) {
                            publishMessage(
                                AppMessage(
                                    result.message.toStringHolder(),
                                    uniqueId = "actionError",
                                    isError = true,
                                )
                            )
                        }
                        result is ActionResult.Actioned
                    }
                    is KeyboardActionMode.Search -> handleSearchEvent(trigger, focused, mode)
                    is KeyboardActionMode.Command -> handleCommandEvent(trigger, focused)
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
            val oldCommandSuggestionsProvider = (old as? KeyboardActionMode.Command)?.suggestionsProvider
            val newCommandSuggestionsProvider = (new as? KeyboardActionMode.Command)?.suggestionsProvider
            if (oldCommandSuggestionsProvider != newCommandSuggestionsProvider) {
                oldCommandSuggestionsProvider?.clear()
            }
            new
        }
    }

    private fun handleSearchEvent(
        key: KeyTrigger,
        focused: FocusTarget?,
        mode: KeyboardActionMode.Search,
    ): Boolean {
        // When navigating, prioritize other events over search events
        if (mode.navigating) {
            (handleNavigationEvent(key, focused) as? ActionResult.Actioned)?.let {
                return true
            }
        }
        return when (key.rawKey) {
            KeyMapped.Escape -> {
                updateMode { KeyboardActionMode.Navigation }
                true
            }
            KeyMapped.Enter -> {
                updateMode { mode.copy(navigating = true) }
                windowCoordinates?.let {
                    focusClosestTo(it.topCenter, allowPartial = true, role = FocusRole.LIST_ITEM)
                }
                true
            }
            KeyMapped.DirectionUp -> false // TODO cycle search history; configurable binding?
            KeyMapped.DirectionDown -> false // TODO cycle search history; configurable binding?
            else -> false
        }
    }

    private fun handleCommandEvent(
        key: KeyTrigger,
        focused: FocusTarget?,
    ): Boolean {
        if (focused?.role != FocusRole.COMMAND_BAR) {
            focusByRole(FocusRole.COMMAND_BAR)
        }
        return when (key.rawKey) {
            KeyMapped.Escape -> {
                updateMode { KeyboardActionMode.Navigation }
                true
            }
            KeyMapped.Enter -> {
                // If we have a non-null suggestion selected, consume enter and clear that
                var commandMode: KeyboardActionMode.Command? = null
                updateMode {
                    (it as? KeyboardActionMode.Command)?.let {
                        commandMode = it
                        it.copy(selectedSuggestion = null)
                    } ?: it
                }
                val selectedSuggestion = commandMode?.selectedSuggestion
                if (selectedSuggestion == null) {
                    onCommandEnter()
                } else {
                    applyCommandSuggestion(commandMode, selectedSuggestion)
                }
                true
            }
            KeyMapped.Tab -> {
                val (commandMode, suggestionsState) = commandSuggestionsState.value ?: run {
                    log.e("Tried handling command mode key while not ready via suggestions state")
                    return true
                }
                if (suggestionsState?.currentSuggestions.isNullOrEmpty()) {
                    return true
                }
                val currentSuggestionIndex = if (commandMode.selectedSuggestion == null) {
                    -1
                } else {
                    suggestionsState.currentSuggestions.indexOfFirst { it.value == commandMode.selectedSuggestion }
                }
                val nextIndex = if (key.ctrl || key.shift) {
                    currentSuggestionIndex - 1
                } else {
                    currentSuggestionIndex + 1
                }.mod(suggestionsState.currentSuggestions.size + 1) // + 1 allows clearing selection again
                val nextSuggestion = suggestionsState.currentSuggestions.getOrNull(nextIndex)?.value
                updateMode {
                    commandMode.copy(selectedSuggestion = nextSuggestion)
                }
                true
            }
            KeyMapped.DirectionUp -> false // TODO cycle search history; configurable binding?
            KeyMapped.DirectionDown -> false // TODO cycle search history; configurable binding?
            else -> false
        }
    }

    private fun handleContextMenuEvent(event: KeyEvent, contextMenu: InteractionAction.ContextMenu): Boolean {
        when (event.key) {
            Key.Escape -> dismissContextMenu(contextMenu.focusId)
            else -> {
                val action = contextMenu.entries.find { it.keyboardShortcut == event.key }
                if (action != null && action.enabled) {
                    handleAction(contextMenu.focusId, action.action, action.actionArgs)
                    if (action.autoCloseMenu) {
                        dismissContextMenu(contextMenu.focusId)
                    }
                }
            }
        }
        // Consume everything while open
        return true
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
        currentFocus.value = parent.uuid
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

    fun getActionContext(
        destination: Destination?,
        criticalActionRequiresConfirmation: Boolean = true,
    ): ActionContext = getInternalActionContext(
        focused = currentFocused(),
        criticalActionRequiresConfirmation = criticalActionRequiresConfirmation,
        keybindingConfig = UiState.keybindingsConfig.value,
        currentDestinationName = destination?.name,
    )

    fun handleAction(
        focusItem: UUID,
        action: Action,
        args: List<String>,
    ): ActionResult {
        val focused = focusableTargets[focusItem] ?: run {
            log.e("Invoked handleAction on unregistered focus item")
            currentFocused()
        }
        val context = getInternalActionContext(focused, criticalActionRequiresConfirmation = true)

        return ActionResult.chain(
            *getCurrentKeyActionHandlers(focused).map {{
                it.handleActionOrInapplicable(context, action, args)
            }}.toTypedArray()
        )
    }

    private fun getInternalActionContext(
        focused: FocusTarget?,
        criticalActionRequiresConfirmation: Boolean,
        keybindingConfig: KeybindingConfig? = UiState.keybindingsConfig.value,
        currentDestinationName: String? = focused?.destinationStateHolder?.state?.value?.destination?.name,
    ) = object : InternalActionContext {
        override fun publishMessage(message: AbstractAppMessage) =
            this@KeyboardActionHandler.publishMessage(message)
        override fun dismissMessage(uniqueId: String) =
            this@KeyboardActionHandler.dismissMessage(uniqueId)
        override fun copyToClipboard(content: String, description: ComposableStringHolder) =
            this@KeyboardActionHandler.copyToClipboard(this, content, description)
        override fun readFromClipboard(handle: suspend (ClipEntry) -> ActionResult) =
            this@KeyboardActionHandler.readFromClipboard(this, handle)
        override fun getFilesFromClipboard() = this@KeyboardActionHandler.getFilesFromClipboard()
        override fun openLinkInExternalBrowser(uri: String): ActionResult =
            this@KeyboardActionHandler.openLinkInExternalBrowser(uri)
        override fun focusByRole(role: FocusRole) =
            this@KeyboardActionHandler.focusByRole(role)
        override fun withCriticalActionConfirmation(
            prompt: ComposableStringHolder,
            confirmText: ComposableStringHolder,
            onDismiss: () -> Unit,
            action: () -> ActionResult,
        ) = this@KeyboardActionHandler.withCriticalActionConfirmation(
            context = this,
            prompt = prompt,
            confirmText = confirmText,
            onDismiss = onDismiss,
            action = action,
        )
        override suspend fun withCriticalActionConfirmationSuspend(
            scope: CoroutineScope,
            actionName: String,
            prompt: ComposableStringHolder,
            confirmText: ComposableStringHolder,
            onDismiss: () -> Unit,
            coroutineContext: CoroutineContext,
            action: suspend () -> ActionResult
        ): ActionResult {
            return if (criticalActionRequiresConfirmation) {
                withCriticalActionConfirmation(
                    prompt = prompt,
                    confirmText = confirmText,
                    onDismiss = onDismiss,
                ) {
                    launchActionAsync(
                        actionName = actionName,
                        scope = scope,
                        context = coroutineContext,
                        appMessageId = ConfirmActionAppMessage.MESSAGE_ID,
                    ) {
                        action()
                    }
                }
            } else {
                action()
            }
        }
        override val focused = focused
        override val criticalActionRequiresConfirmation = criticalActionRequiresConfirmation
        override val keybindingConfig = keybindingConfig
        override val currentDestinationName = currentDestinationName
    }

    private fun navigationItemActionHandler(
        focused: FocusTarget,
        navigationActionable: InteractionAction.NavigationAction,
    ) =
        object : KeyboardActionProvider<Action.NavigationItem> {
            override fun getPossibleActions() = Action.NavigationItem.entries.toSet()
            override fun ensureActionType(action: Action) = action as? Action.NavigationItem

            override fun handleNavigationModeEvent(
                context: ActionContext,
                key: KeyTrigger
            ): ActionResult {
                val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
                return keyConfig.navigationItem.execute(context, key, ::handleAction)
            }

            override fun handleAction(
                context: ActionContext,
                action: Action.NavigationItem,
                args: List<String>
            ): ActionResult {
                val destination = navigationActionable.buildDestination()
                return when (action) {
                    Action.NavigationItem.NavigateCurrent -> {
                        val destinationStateHolder = focused.destinationStateHolder ?: return ActionResult.Inapplicable
                        updateMode { KeyboardActionMode.Navigation }
                        navigateCurrentDestination(destination, destinationStateHolder).orActionInapplicable()
                    }
                    Action.NavigationItem.NavigateInNewWindow -> {
                        UiState.openWindow(destination, navigationActionable.initialTitle)
                        ActionResult.Success()
                    }
                }
            }
        }

    private val listActionHandler = object : KeyboardActionProvider<Action.List> {
        override fun getPossibleActions() = Action.List.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.List

        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.list.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.List,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.List.ScrollToTop -> scrollListToTop(context.focused()).orActionInapplicable()
                Action.List.ScrollToBottom -> scrollListToBottom(context.focused()).orActionInapplicable()
                Action.List.ScrollToStart -> scrollListToStart(context.focused()).orActionInapplicable()
                Action.List.ScrollToEnd -> scrollListToEnd(context.focused()).orActionInapplicable()
            }
        }
    }

    private val focusActionHandler = object : KeyboardActionProvider<Action.Focus> {
        override fun getPossibleActions() = Action.Focus.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.Focus

        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.focus.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.Focus,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.Focus.FocusUp -> moveFocus(FocusDirection.Up, context.focused())
                Action.Focus.FocusDown -> moveFocus(FocusDirection.Down, context.focused())
                Action.Focus.FocusLeft -> moveFocus(FocusDirection.Left, context.focused())
                Action.Focus.FocusRight -> moveFocus(FocusDirection.Right, context.focused())
                Action.Focus.FocusTop -> focusCurrentContainerRelative(context.focused()) { it.topCenter } // TODO keep X offset rather than assuming center
                Action.Focus.FocusCenter -> focusCurrentContainerRelative(context.focused()) { it.center } // TODO keep X offset rather than assuming center
                Action.Focus.FocusBottom -> focusCurrentContainerRelative(context.focused()) { it.bottomCenter } // TODO keep X offset rather than assuming center
                Action.Focus.FocusParent -> focusParent(context.focused())
                Action.Focus.FocusEnterContainer -> focusEnterContainer(context.focused())
                Action.Focus.OpenContextMenu -> {
                    context.focused()?.let {
                        openContextMenu(it.id)
                    } ?: false
                }
            }.orActionInapplicable()
        }
    }

    private val navigationActionHandler = object : KeyboardActionProvider<Action.Navigation> {
        override fun getPossibleActions() = Action.Navigation.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.Navigation

        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.navigation.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.Navigation,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.Navigation.NavigateCurrent -> {
                    val extraArgs = args.subList(1, args.size)
                    val destination = args[0].toDestinationOrNull(extraArgs).orActionValidationError()
                    navigateCurrentDestination(context.focused()?.destinationStateHolder) { destination }.orActionInapplicable()
                }
                Action.Navigation.NavigateInNewWindow -> {
                    val extraArgs = args.subList(1, args.size)
                    val destination = args[0].toDestinationOrNull(extraArgs).orActionValidationError()
                    UiState.openWindow(destination)
                    ActionResult.Success()
                }
                Action.Navigation.SplitHorizontal -> navigateCurrentDestination(context.focused()?.destinationStateHolder) {
                    Destination.SplitHorizontal(
                        DestinationStateHolder.forInitialDestination(it),
                        DestinationStateHolder.forInitialDestination(it),
                    )
                }.orActionInapplicable()
                Action.Navigation.SplitVertical -> navigateCurrentDestination(context.focused()?.destinationStateHolder) {
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
    }

    private val appMessageHandler = object : KeyboardActionProvider<Action.AppMessage> {
        override fun getPossibleActions() = Action.AppMessage.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.AppMessage
        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.appMessage.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.AppMessage,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.AppMessage.ClearAppMessages -> {
                    var wasEmpty = true
                    val now = System.currentTimeMillis()
                    _messageBoard.update {
                        if (it.none { it.dismissedTimestamp == null}) {
                            wasEmpty = true
                            it
                        } else {
                            wasEmpty = false
                            it.map { it.copyDismissed(dismissedTimestamp = now) }.toPersistentList()
                        }
                    }
                    if (wasEmpty) {
                        ActionResult.Inapplicable
                    } else {
                        ActionResult.Success()
                    }
                }
                Action.AppMessage.ConfirmActionAppMessage -> {
                    val message = messageBoard.value.find {
                        it is ConfirmActionAppMessage && it.dismissedTimestamp == null
                    } as? ConfirmActionAppMessage ?: return ActionResult.Inapplicable
                    message.action()
                    ActionResult.Success()
                }
            }
        }
    }

    private val globalActionHandler = object : KeyboardActionProvider<Action.Global> {
        override fun getPossibleActions() = Action.Global.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.Global

        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.global.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.Global,
            args: List<String>
        ): ActionResult {
            return when (action) {
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
                Action.Global.Command -> {
                    if (mode.value is KeyboardActionMode.Command) {
                        // CMD already active, just focus again
                        focusByRole(FocusRole.COMMAND_BAR).orActionInapplicable()
                    } else {
                        handleCommandInput(TextFieldValue("")) {
                            focusByRole(FocusRole.COMMAND_BAR)
                        }.orActionInapplicable()
                    }
                }
                Action.Global.SetSetting -> {
                    scope.launch {
                        RevengePrefs.handleSetAction(context, args)
                    }
                    ActionResult.Success()
                }
                Action.Global.ToggleSetting -> {
                    scope.launch {
                        RevengePrefs.handleToggleAction(context, args)
                    }
                    ActionResult.Success()
                }
                Action.Global.Exit -> {
                    UiState.exit(applicationScope)
                    ActionResult.Success()
                }
            }
        }
    }

    private fun getCurrentKeyActionHandlers(focused: FocusTarget?): List<KeyboardActionProvider<*>> {
        return listOfNotNull(
            appMessageHandler,
            focused?.actions?.keyActions,
            (focused?.actions?.primaryAction as? InteractionAction.NavigationAction)?.let {
                navigationItemActionHandler(focused, it)
            },
            (focused?.actions?.listActions)?.let {
                listActionHandler
            },
            focusActionHandler,
            navigationActionHandler,
            globalActionHandler,
        )
    }

    private fun handleNavigationEvent(key: KeyTrigger, focused: FocusTarget?): ActionResult {
        val keyConfig = UiState.keybindingsConfig.value ?: return ActionResult.NoMatch
        val context = getInternalActionContext(
            focused,
            criticalActionRequiresConfirmation = true,
            keybindingConfig = keyConfig,
        )

        return ActionResult.chain(
            *getCurrentKeyActionHandlers(focused).map {{
                it.handleNavigationModeEvent(context, key)
            }}.toTypedArray()
        )
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        // TODO?
        return false
    }

    fun onFocusChanged(target: UUID, state: FocusState) {
        //log.v { "Focus changed for $target to $state" }
        var lostFocusTarget: UUID? = null
        if (state.isFocused) {
            currentFocus.update {
                lostFocusTarget = it
                target
            }
        } else if (!state.hasFocus) {
            currentFocus.update {
                if (it == target) {
                    lostFocusTarget = it
                    null
                } else {
                    it
                }
            }
        }
        val newFocus = focusableTargets[target]
        if (newFocus?.focusRequester is FakeFocusRequester) {
            // Need to clear focus in case we have anything that only "fakes" our keyboard focus,
            // so e.g. textfields don't keep consuming keypresses while we still handle to navigation events
            focusManager?.clearFocus()
        }
        lostFocusTarget?.let { focusableTargets[it] }?.let(::handleLostFocus)
    }

    private fun handleLostFocus(target: FocusTarget) {
        when (target.role) {
            FocusRole.SEARCH_BAR -> {
                updateMode { mode ->
                    (mode as? KeyboardActionMode.Search)?.copy(navigating = true) ?: mode
                }
            }
            /*
            FocusRole.COMMAND_BAR -> {
                updateMode { mode ->
                    mode.takeIf { it !is KeyboardActionMode.Command } ?: KeyboardActionMode.Navigation
                }
            }
             */
            else -> {}
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
        if (_lastPointerPosition != position) {
            _lastPointerPosition = position
            _keyboardPrimary.value = false
        }
        if (handlerSettings.value.focusFollowsMouse) {
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

    fun clearSearch() {
        updateMode { KeyboardActionMode.Navigation }
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

    fun updateCommandInput(query: TextFieldValue) = handleCommandInput(query) {}

    fun applyCommandSuggestion(state: KeyboardActionMode.Command, suggestion: String) {
        val (cmd, args) = state.suggestionsProvider.commandParser.parseCommandString(state.query.text)
            ?: run {
                log.e("Failed to run autocompletion for query ${state.query.text}")
                return
            }
        val newQuery = if (args.isEmpty() && !state.query.text.endsWith(" ")) {
            "$suggestion "
        } else {
            val stableArgs = if (state.query.text.endsWith(" ") || args.isEmpty()) {
                args
            } else {
                args.subList(0, args.size - 1)
            }
            val newArgs = stableArgs + suggestion
            "$cmd ${newArgs.joinToString(separator = " ", postfix = " ")}"
        }
        updateMode {
            state.copy(
                query = TextFieldValue(newQuery, selection = TextRange(newQuery.length)),
                selectedSuggestion = null,
            )
        }
    }

    fun onCommandEnter() {
        val commandMode = (mode.value as? KeyboardActionMode.Command) ?: return
        executeCommand(commandMode)
        updateMode {
            it.takeIf { it !is KeyboardActionMode.Command } ?: KeyboardActionMode.Navigation
        }
    }

    fun dismissContextMenu(id: UUID): Boolean {
        var dismissed = false
        _currentOpenContextMenu.update {
            it?.takeIf {
                val wasOpen = it == id
                dismissed = wasOpen
                !wasOpen
            }
        }
        return dismissed
    }

    fun openContextMenu(id: UUID): Boolean {
        val focusTarget = focusableTargets[id]
        if (focusTarget == null) {
            log.e("Tried to open context menu on unregistered target $id")
            return false
        }
        focusTarget.actions?.findInteractionAction<InteractionAction.ContextMenu>()?.takeIf { it.entries.isNotEmpty() } ?: return false
        _currentOpenContextMenu.value = id
        return true
    }

    private fun executeCommand(commandMode: KeyboardActionMode.Command) {
        val focused = commandMode.focused?.let {
            focusableTargets[it]
        } ?: focusableTargets.values.find { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }
        val commandParser = CommandParser(getCurrentKeyActionHandlers(focused))
        val (mainCommand, args) = commandParser.parseCommandString(commandMode.query.text) ?: run {
            log.i("Ignoring empty command")
            return
        }
        val possibleActions = commandParser.getPossibleActions(mainCommand)
        val possibleUniqueActions = possibleActions.map {
            it.first
        }.distinct()
        val possibleUniqueActionsWithArgsChecked = possibleUniqueActions.map {
            it to checkArguments(it, args)
        }
        val possibleUniqueActionsWithValidArgs = possibleUniqueActionsWithArgsChecked.mapNotNull { (action, error) ->
            action.takeIf { error == null }
        }
        when (possibleUniqueActionsWithValidArgs.size) {
            0 -> {
                val message = when (possibleUniqueActionsWithArgsChecked.size) {
                    0 -> StringResourceHolder(Res.string.command_not_found, mainCommand.toStringHolder())
                    1 -> possibleUniqueActionsWithArgsChecked.first().second!!.message.toStringHolder()
                    else -> StringResourceHolder(Res.string.command_ambiguous_none_valid, mainCommand.toStringHolder())
                }
                publishMessage(
                    AppMessage(
                        message,
                        isError = true,
                        uniqueId = COMMAND_MESSAGE_ID,
                    )
                )
            }
            1 -> {
                val action = possibleUniqueActionsWithValidArgs.first()
                val context = getInternalActionContext(
                    focused,
                    keybindingConfig = null,
                    criticalActionRequiresConfirmation = false,
                )
                val result = try {
                    ActionResult.chain(
                        *possibleActions.filter { it.first == action }.map {{
                            it.second.handleActionOrInapplicable(context, it.first, args)
                        }}.toTypedArray()
                    )
                } catch (e: ActionValidationException) {
                    ActionResult.Failure(e.message ?: "Action validation failed")
                }
                when (result) {
                    is ActionResult.Failure -> publishMessage(
                        AppMessage(
                            result.message.toStringHolder(),
                            isError = true,
                            uniqueId = COMMAND_MESSAGE_ID,
                        )
                    )
                    is ActionResult.Success -> {}
                    ActionResult.NoMatch,
                    ActionResult.Inapplicable -> publishMessage(
                        AppMessage(
                            StringResourceHolder(Res.string.command_not_applicable, mainCommand.toStringHolder()),
                            isError = true,
                            uniqueId = COMMAND_MESSAGE_ID,
                        )
                    )
                    is ActionResult.InvalidCommand -> publishMessage(
                        AppMessage(
                            result.message.toStringHolder(),
                            isError = true,
                            uniqueId = COMMAND_MESSAGE_ID,
                        )
                    )
                }
            }
            else -> {
                log.e("Found ambiguous actions for $mainCommand: ${possibleUniqueActionsWithValidArgs.joinToString { it.name }}")
                publishMessage(
                    AppMessage(
                        StringResourceHolder(Res.string.command_ambiguous, mainCommand.toStringHolder()),
                        isError = true,
                        uniqueId = COMMAND_MESSAGE_ID,
                    )
                )
            }
        }
    }

    private fun handleCommandInput(
        query: TextFieldValue,
        handleSuccess: (KeyboardActionMode.Command) -> Unit,
    ): Boolean {
        var success: KeyboardActionMode.Command? = null
        updateMode { mode ->
            if (mode is KeyboardActionMode.Command) {
                mode.copy(query = query)
            } else {
                val focusTarget = currentFocus.value?.let { focusableTargets[it] }
                    ?: focusableTargets.values.find { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }
                KeyboardActionMode.Command(
                    query = query,
                    focused = focusTarget?.id,
                    suggestionsProvider = CommandSuggestionsProvider(
                        queryFlow = _mode.map { it as? KeyboardActionMode.Command },
                        scope = scope.childScope(Dispatchers.IO, "commandSuggestions"),
                        commandParser = CommandParser(getCurrentKeyActionHandlers(focusTarget)),
                        userIdSuggestionsProvider = focusTarget?.actions?.userIdSuggestionsProvider,
                        roomContextSuggestionsProvider = focusTarget?.actions?.roomContextSuggestionsProvider,
                    ),
                    selectedSuggestion = null,
                )
            }.also {
                success = it
            }
        }
        success?.let(handleSuccess)
        return success != null
    }

    private fun withCriticalActionConfirmation(
        context: InternalActionContext,
        prompt: ComposableStringHolder,
        confirmText: ComposableStringHolder,
        onDismiss: () -> Unit,
        action: () -> ActionResult
    ): ActionResult {
        return if (context.criticalActionRequiresConfirmation) {
            publishMessage(
                ConfirmActionAppMessage(
                    prompt,
                    confirmText = confirmText,
                    onDismiss = onDismiss,
                ) {
                    dismissMessage(ConfirmActionAppMessage.MESSAGE_ID)
                    action()
                }
            )
            ActionResult.Success(async = true, notifySuccess = false)
        } else {
            action()
        }
    }

    fun copyToClipboard(context: ActionContext, content: String, description: ComposableStringHolder): ActionResult {
        val localClipboard = clipboard ?: return ActionResult.Failure("No clipboard found")
        context.launchActionAsync("copyToClipboard", scope) {
            localClipboard.setClipEntry(
                ClipEntry(java.awt.datatransfer.StringSelection(content))
            )
            // TODO how to do string resources here
            publishMessage(
                AppMessage(
                    StringResourceHolder(Res.string.command_copied_to_clipboard, description),
                    uniqueId = "clipboard",
                )
            )
            ActionResult.Success()
        }
        return ActionResult.Success()
    }

    fun readFromClipboard(context: ActionContext, handle: suspend (ClipEntry) -> ActionResult): ActionResult {
        val localClipboard = clipboard ?: return ActionResult.Failure("No clipboard found")
        return context.launchActionAsync("readFromClipboard", scope) {
            localClipboard.getClipEntry()?.let {
                handle(it)
            } ?: ActionResult.Inapplicable
        }
    }

    fun getFilesFromClipboard(): List<File> {
        val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = systemClipboard.getContents(null) ?: return emptyList()

        return if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            @Suppress("UNCHECKED_CAST")
            contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<File> ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun openLinkInExternalBrowser(uri: String): ActionResult {
        return try {
            val localUriHandler = uriHandler ?: return ActionResult.Failure("No uri handler found")
            localUriHandler.openUri(uri)
            ActionResult.Success()
        } catch (e: Exception) {
            log.w("Failed to open URL in external browser", e)
            return ActionResult.Failure(e.message ?: e.toString())
        }
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
    sealed interface InvalidCommand : ActionResult {
        val message: String
    }

    data class Success(
        // True after launching a coroutine that may still fail, causing the action to be considered "failed" eventually later
        val async: Boolean = false,
        val notifySuccess: Boolean = async,
        override val shouldExit: Boolean = true
    ) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class Failure(val message: String, override val shouldExit: Boolean = true) : ActionResult, Actioned {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class Malformed(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class MissingParameters(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
        override fun withChainSetting(chain: Boolean) = copy(shouldExit = !chain)
    }
    data class TooManyParameters(override val message: String, override val shouldExit: Boolean = true) : ActionResult, InvalidCommand {
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
fun <T>Result<T>.toActionResult(async: Boolean = false, notifySuccess: Boolean = false) = if (isSuccess) {
    ActionResult.Success(async = async, notifySuccess = notifySuccess)
} else {
    ActionResult.Failure(exceptionOrNull()?.message ?: "Unknown failure")
}

/**
 * Try all possible bindings for a given key until the first one returns true.
 */
fun <A: Action>List<Binding<A>>.execute(
    context: ActionContext,
    key: KeyTrigger,
    block: (ActionContext, action: A, args: List<String>) -> ActionResult
): ActionResult {
    val actions = filter {
        it.trigger == key && (it.destinations.isEmpty() || it.destinations.contains(context.currentDestinationName))
    }
    var hasChainableSuccess = false
    actions.forEach { action ->
        val actionResult = try {
            action.checkArguments()?.also {
                Logger.e(it.message)
            } ?: block(context, action.action, action.args).withChainSetting(action.chain)
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

fun checkArgument(
    actionName: String,
    argDef: ActionArgument,
    argVal: String,
    context: CommandArgContext,
    lookahead: List<String>,
    validSessionIds: List<String>?,
    validSettingKeys: List<String>,
): ActionResult.InvalidCommand? {
    return when (argDef) {
        is ActionArgumentAnyOf -> {
            if (argDef.arguments.any {
                checkArgument(actionName, it, argVal, context, lookahead, validSessionIds, validSettingKeys) != null
            }) {
                null
            } else {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected ${argDef.name}; got $argVal"
                )
            }
        }
        is ActionArgumentOptional -> {
            checkArgument(actionName, argDef.argument, argVal, context, lookahead, validSessionIds, validSettingKeys)
        }
        is ActionArgumentContextBased -> {
            checkArgument(actionName, argDef.getFor(context), argVal, context, lookahead, validSessionIds, validSettingKeys)
        }
        ActionArgumentPrimitive.Reason,
        ActionArgumentPrimitive.EventType,
        ActionArgumentPrimitive.StateEventType,
        ActionArgumentPrimitive.NonEmptyStateKey,
        ActionArgumentPrimitive.Text -> null
        ActionArgumentPrimitive.SettingValue -> {
            val settingKeys = context.findAll(ActionArgumentPrimitive.SettingKey)
            if (settingKeys.isEmpty()) {
                // Ignore already broken SettingsKey
                null
            } else {
                if (settingKeys.any { sKey ->
                    val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
                        // Ignore already broken SettingsKey
                        ?: return@any true
                    // Check if this is a valid settings value
                    pref.parseType(argVal) != null
                }) {
                    null
                } else {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, not a valid settings value for ${settingKeys.joinToString()}: $argVal"
                    )
                }
            }
        }
        ActionArgumentPrimitive.Boolean -> {
            if (argVal.toBooleanStrictOrNull() == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected boolean got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.Integer -> {
            if (argVal.toIntOrNull() == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected int got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SessionIndex -> {
            val asIndex = argVal.toIntOrNull()
            if (asIndex != null) {
                if (asIndex in 0..(validSessionIds?.size ?: Integer.MAX_VALUE)) {
                    null
                } else {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, index out of range: $argVal"
                    )
                }
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SessionId,
        ActionArgumentPrimitive.UserIdInRoom,
        ActionArgumentPrimitive.UserIdNotInRoom,
        ActionArgumentPrimitive.UserId -> {
            if (validSessionIds != null && argDef == ActionArgumentPrimitive.SessionId) {
                if (!validSessionIds.contains(argVal)) {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, not an existing user login: $argVal"
                    )
                } else {
                    null
                }
            } else {
                // TODO full mxid regex checking
                if (!argVal.startsWith("@") || !argVal.contains(":")) {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected MXID got $argVal"
                    )
                } else {
                    null
                }
            }
        }
        ActionArgumentPrimitive.SpaceId,
        ActionArgumentPrimitive.RoomId -> {
            if (!argVal.startsWith("!")) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected room ID got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.EventId -> {
            if (!argVal.startsWith("$")) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected room ID got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SettingKey -> {
            if (argVal !in validSettingKeys) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid settings key: $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.NavigatableDestinationName -> {
            if (argVal.toDestinationOrNull(lookahead) == null) {
                if (argVal in ALLOWED_DESTINATION_STRINGS) {
                    ActionResult.MissingParameters(
                        "Invalid parameter for $actionName, not a valid destination: $argVal with args [${lookahead.joinToString()}]"
                    )
                } else {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, not a valid destination: $argVal"
                    )
                }
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SpaceSelectionId -> {
            if (argVal.startsWith(REAL_SPACE_ID_PREFIX) || argVal.startsWith(PSEUDO_SPACE_ID_PREFIX)) {
                null
            } else {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid space selection ID: $argVal"
                )
            }
        }
        ActionArgumentPrimitive.SpaceIndex -> {
            val asIndex = argVal.toIntOrNull()
            if (asIndex != null && asIndex >= 0) {
                null
            } else {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected non-negative integer got $argVal"
                )
            }
        }
        ActionArgumentPrimitive.Empty -> {
            if (argVal.isBlank()) {
                null
            } else {
                ActionResult.Malformed(
                    "Unexpected parameter for $actionName: $argVal"
                )
            }
        }
    }
}

fun <A : Action>Binding<A>.checkArguments(
    checkIncompleteParameters: Boolean = false,
    validSessionIds: List<String>? = UiState.currentValidSessionIds.value,
    validSettingKeys: List<String> = ScPrefs.validSettingKeys,
) = checkArguments(
    action,
    args,
    checkIncompleteParameters,
    validSessionIds,
    validSettingKeys,
)

fun checkArguments(
    action: Action,
    args: List<String>,
    checkIncompleteParameters: Boolean = false,
    validSessionIds: List<String>? = UiState.currentValidSessionIds.value,
    validSettingKeys: List<String> = ScPrefs.validSettingKeys,
): ActionResult.InvalidCommand? {
    val actionName = action.name
    val minArgSize = action.minArgsSize()
    if (args.size !in minArgSize..action.args.size) {
        val message = if (minArgSize != args.size) {
            "Invalid parameter size for $actionName, expected between $minArgSize and ${action.args.size}, got ${args.size}"
        } else {
            "Invalid parameter size for $actionName, expected ${action.args.size} got ${args.size}"
        }
        return if (args.size < minArgSize) {
            if (checkIncompleteParameters) {
                action.args.zip(args).forEachIndexed { index, (argDef, argVal) ->
                    val lookahead = args.subList(index + 1, args.size)
                    val context = action.args.zip(args.subList(0, index))
                    checkArgument(action.name, argDef, argVal, context, lookahead, validSessionIds, validSettingKeys)?.let {
                        return it
                    }
                }
            }
            ActionResult.MissingParameters(message)
        } else {
            ActionResult.TooManyParameters(message)
        }
    }
    // Optional arguments only supported to leave away at the end right now
    action.args.zip(args).forEachIndexed { index, (argDef, argVal) ->
        val lookahead = args.subList(index + 1, args.size)
        val context = action.args.zip(args.subList(0, index))
        checkArgument(action.name, argDef, argVal, context, lookahead, validSessionIds, validSettingKeys)?.let {
            return it
        }
    }
    return null
}

interface ActionContext {
    fun publishMessage(message: AbstractAppMessage)
    fun dismissMessage(uniqueId: String)
    fun copyToClipboard(content: String, description: ComposableStringHolder): ActionResult
    fun readFromClipboard(handle: suspend (ClipEntry) -> ActionResult): ActionResult
    fun getFilesFromClipboard(): List<File>
    fun openLinkInExternalBrowser(uri: String): ActionResult
    fun focusByRole(role: FocusRole): Boolean
    fun withCriticalActionConfirmation(
        prompt: ComposableStringHolder,
        confirmText: ComposableStringHolder,
        onDismiss: () -> Unit = {},
        action: () -> ActionResult,
    ): ActionResult
    suspend fun withCriticalActionConfirmationSuspend(
        scope: CoroutineScope,
        actionName: String,
        prompt: ComposableStringHolder,
        confirmText: ComposableStringHolder,
        onDismiss: () -> Unit = {},
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        action: suspend () -> ActionResult,
    ): ActionResult
    val currentDestinationName: String?
    val keybindingConfig: KeybindingConfig?
}

private interface InternalActionContext : ActionContext {
    val focused: FocusTarget?
    val criticalActionRequiresConfirmation: Boolean
}

fun ActionContext?.publishError(log: Logger, messageId: String?, error: String) {
    log.e(error)
    this?.publishMessage(
        AppMessage(
            message = error.toStringHolder(),
            uniqueId = messageId,
            isError = true
        )
    )
}

fun ActionContext.launchActionAsync(
    actionName: String,
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    appMessageId: String? = null,
    notifyProcessing: Boolean = false,
    block: suspend () -> ActionResult,
): ActionResult {
    scope.launch(context) {
        if (notifyProcessing) {
            publishMessage(
                AppMessage(
                    message = Res.string.action_processing.toStringHolder(),
                    uniqueId = appMessageId,
                    canAutoDismiss = appMessageId == null,
                )
            )
        }
        val result = block()
        if (result is ActionResult.Failure) {
            Logger.withTag("AsyncAction").e("Failed to execute $actionName: ${result.message}")
            publishMessage(
                AppMessage(result.message.toStringHolder(), isError = true, uniqueId = appMessageId)
            )
        } else if ((result as? ActionResult.Success)?.notifySuccess == true) {
            publishMessage(
                AppMessage(
                    StringResourceHolder(Res.string.action_processing_done),
                    isError = true,
                    uniqueId = appMessageId
                )
            )
        } else if (notifyProcessing && appMessageId != null) {
            dismissMessage(appMessageId)
        }
    }
    return ActionResult.Success(async = true)
}


@Composable
fun currentActionContext(): ActionContext {
    val keyHandler = LocalKeyboardActionHandler.current
    val destination = LocalDestinationState.current?.state?.collectAsState()?.value?.destination
    return remember(keyHandler, destination) {
        keyHandler.getActionContext(destination)
    }
}
