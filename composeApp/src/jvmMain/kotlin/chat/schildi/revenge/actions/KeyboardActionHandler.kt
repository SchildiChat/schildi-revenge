package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.toIntSize
import chat.schildi.revenge.preferences.RevengePrefs
import chat.schildi.revenge.preferences.SETTINGS_MESSAGE_ID
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.findPreference
import chat.schildi.lib.preferences.safeLookup
import chat.schildi.revenge.DefaultDestinationStateHolder
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationState
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.NavigationPreference
import chat.schildi.revenge.compose.components.ContextMenuActionEntry
import chat.schildi.revenge.compose.components.ContextMenuCallbackEntry
import chat.schildi.revenge.compose.components.ContextMenuEntry
import chat.schildi.revenge.compose.components.ContextMenuSubmenuEntry
import chat.schildi.revenge.compose.focus.AbstractFocusRequester
import chat.schildi.revenge.compose.focus.FakeFocusRequester
import chat.schildi.revenge.compose.focus.preferFocusChildren
import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.resources.toStringHolder
import chat.schildi.revenge.HEADLESS_WINDOW_ID
import chat.schildi.revenge.WindowId
import chat.schildi.revenge.config.keybindings.ALLOWED_DESTINATION_STRINGS
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.config.keybindings.ActionArgumentAnyOf
import chat.schildi.revenge.config.keybindings.ActionArgumentContextBased
import chat.schildi.revenge.config.keybindings.ActionArgumentOptional
import chat.schildi.revenge.config.keybindings.ActionArgumentPrimitive
import chat.schildi.revenge.config.keybindings.ActionArgumentRepeatable
import chat.schildi.revenge.config.keybindings.ActionRoomNotificationSetting
import chat.schildi.revenge.config.keybindings.AllowedComposerTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedSingleLineTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.AllowedTextFieldBindingKeys
import chat.schildi.revenge.config.keybindings.Binding
import chat.schildi.revenge.config.keybindings.CommandArgContext
import chat.schildi.revenge.config.keybindings.DestinationEnum
import chat.schildi.revenge.config.keybindings.KeyMapped
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.config.keybindings.KeybindingConfig
import chat.schildi.revenge.config.keybindings.SpaceCatchAllMode
import chat.schildi.revenge.config.keybindings.findAll
import chat.schildi.revenge.config.keybindings.maxArgsSize
import chat.schildi.revenge.config.keybindings.minArgsSize
import chat.schildi.revenge.model.account.OAuthRepo
import chat.schildi.revenge.model.verification.RevengeDeviceVerificationProvider
import chat.schildi.revenge.model.account.RevengeOAUthRepo
import chat.schildi.revenge.model.verification.ScIncomingVerificationRequest
import chat.schildi.revenge.model.verification.ScOutgoingVerificationRequest
import chat.schildi.revenge.model.joinRoomByIdOrAliasTracked
import chat.schildi.revenge.model.spaces.PSEUDO_SPACE_ID_PREFIX
import chat.schildi.revenge.model.spaces.REAL_SPACE_ID_PREFIX
import chat.schildi.revenge.model.spaces.RevengeSpaceListDataSource
import chat.schildi.revenge.notification.NotifiableRoomSubscriber
import chat.schildi.revenge.toDestination
import chat.schildi.revenge.toPrettyJson
import chat.schildi.revenge.util.ExternalViewCache
import chat.schildi.revenge.util.MimeUtil
import chat.schildi.revenge.util.matrix.MatrixLinkPatterns
import chat.schildi.revenge.util.matrix.updateAccountData
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import com.beeper.android.messageformat.MatrixToLink
import io.element.android.libraries.core.coroutine.childScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import shire.res.generated.resources.Res
import shire.res.generated.resources.action_cancel
import shire.res.generated.resources.action_logout
import shire.res.generated.resources.action_logout_session_prompt
import shire.res.generated.resources.action_processing
import shire.res.generated.resources.action_processing_done
import shire.res.generated.resources.command_ambiguous
import shire.res.generated.resources.command_ambiguous_none_valid
import shire.res.generated.resources.command_copied_content_to_clipboard
import shire.res.generated.resources.command_copied_to_clipboard
import shire.res.generated.resources.command_copy_name_full_account_data
import shire.res.generated.resources.command_external_application_launched
import shire.res.generated.resources.command_not_applicable
import shire.res.generated.resources.command_not_found
import shire.res.generated.resources.toast_room_created
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt
import kotlin.uuid.Uuid

// When focusing certain elements by role, we want to disable following mouse focus for a second to avoid
// any animations messing with the focus request
private const val MOUSE_FOCUS_PAUSE_DURATION = 1000L
private const val MOUSE_FOCUS_DEBOUNCE = 50L

val LocalKeyboardActionHandler = compositionLocalOf<KeyboardActionHandler> {
    throw IllegalArgumentException("No keyboard action handler provided")
}

private data class FocusTarget(
    val id: Uuid,
    val parent: FocusParent?,
    val role: FocusRole,
    val coordinates: Rect,
    val isFullyVisible: Boolean,
    val focusRequester: AbstractFocusRequester,
    val destinationStateHolder: DestinationStateHolder?,
    val actions: ActionProvider?,
)

enum class FocusRole(
    val consumesKeyWhitelist: List<Key>? = null,
    val consumesKeyWhitelistDuringEdit: List<Key>? = null,
    val autoRequestFocus: Boolean = false,
) {
    LIST_ITEM,
    LIST_ITEM_EDITABLE_SINGLE_LINE(consumesKeyWhitelistDuringEdit = AllowedSingleLineTextFieldBindingKeys),
    LIST_ITEM_EDITABLE_MULTI_LINE(consumesKeyWhitelistDuringEdit = AllowedTextFieldBindingKeys),
    AUX_ITEM,
    AUX_ITEM_EDITABLE_SINGLE_LINE(consumesKeyWhitelistDuringEdit = AllowedSingleLineTextFieldBindingKeys),
    AUX_ITEM_EDITABLE_MULTI_LINE(consumesKeyWhitelistDuringEdit = AllowedTextFieldBindingKeys),
    NESTED_AUX_ITEM,
    CONTEXT_MENU_ENTRY_WITH_SUBMENU,
    DESTINATION_ROOT_CONTAINER,
    NESTING_DESTINATION_ROOT_CONTAINER,
    SHADOW_ITEM, // Not meant to be keyboard-focusable but still hooking into the framework
    CONTAINER,
    CONTAINER_ITEM, // Can both like AUX_ITEM and CONTAINER
    TEXT_FIELD_SINGLE_LINE(consumesKeyWhitelist = AllowedSingleLineTextFieldBindingKeys),
    TEXT_FIELD_MULTI_LINE(consumesKeyWhitelist = AllowedTextFieldBindingKeys),
    MESSAGE_COMPOSER(autoRequestFocus = true, consumesKeyWhitelist = AllowedComposerTextFieldBindingKeys),
    SEARCH_BAR(autoRequestFocus = true), // Does not need to consume plain keys, key handler has a special mode for that
    COMMAND_BAR(autoRequestFocus = true), // Does not need to consume plain keys, key handler has a special mode for that
}

val FOCUSABLE_LIST_ITEMS = arrayOf(
    FocusRole.LIST_ITEM,
    FocusRole.LIST_ITEM_EDITABLE_SINGLE_LINE,
    FocusRole.LIST_ITEM_EDITABLE_MULTI_LINE,
)

val FOCUSABLE_SINGLE_LINE_EDIT_ITEMS = arrayOf(
    FocusRole.LIST_ITEM_EDITABLE_SINGLE_LINE,
    FocusRole.AUX_ITEM_EDITABLE_SINGLE_LINE,
    FocusRole.TEXT_FIELD_SINGLE_LINE,
)

sealed interface CommandHolder {
    val command: String
    val focused: Uuid?
    val impliedArguments: List<Pair<ActionArgumentPrimitive, String>>
}

data class ActionableSnapshot(
    val actionHandlers: List<KeyboardActionProvider<*>>,
    val destinationStateHolder: DestinationStateHolder?,
    val destinationType: DestinationEnum?,
)

data class IpcCommand(
    override val command: String,
) : CommandHolder {
    override val focused: Uuid? = null
    override val impliedArguments: List<Pair<ActionArgumentPrimitive, String>> = emptyList()
}

sealed interface KeyboardActionMode {
    data object Navigation : KeyboardActionMode
    data class Search(
        val query: String,
        val searchProvider: SearchProvider,
        val navigating: Boolean,
        val searchFocusContainer: Uuid?,
    ) : KeyboardActionMode
    data class Command(
        val query: TextFieldValue,
        // Fix the item we want to action on
        override val focused: Uuid?,
        val suggestionsProvider: CommandSuggestionsProvider,
        val selectedSuggestion: String?,
        override val impliedArguments: List<Pair<ActionArgumentPrimitive, String>>,
        val actionableSnapshot: ActionableSnapshot,
        val forSearch: Search?,
    ) : KeyboardActionMode, CommandHolder {
        override val command = query.text
    }
}

// TODO config or something
const val DEFAULT_MESSAGE_EXPIRY_DURATION = 5000L

sealed interface AbstractAppMessage {
    val message: ComposableStringHolder
    val timestamp: Long
    val uniqueId: String?
    val autoDismissDuration: Long?
    val dismissedTimestamp: Long?
    fun copyDismissed(dismissedTimestamp: Long): AbstractAppMessage
}

data class AppMessage(
    override val message: ComposableStringHolder,
    val isError: Boolean = false,
    override val timestamp: Long = System.currentTimeMillis(),
    override val uniqueId: String? = null,
    override val autoDismissDuration: Long? = DEFAULT_MESSAGE_EXPIRY_DURATION,
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
    override val autoDismissDuration: Long? = null
    override fun copyDismissed(dismissedTimestamp: Long) = copy(dismissedTimestamp = dismissedTimestamp).also {
        onDismiss()
    }
    companion object {
        const val MESSAGE_ID = "confirmAction"
    }
}

data class VerificationRequestAppMessage(
    val request: ScIncomingVerificationRequest,
    override val dismissedTimestamp: Long? = null,
) : AbstractAppMessage {
    override val message = request.message
    override val timestamp: Long = request.ts
    override val uniqueId = "verificationRequest/${request.sessionId}/${request.request.details.flowId}"
    // 2 minutes as upstream does it for verification requests
    override val autoDismissDuration = 120_000L
    override fun copyDismissed(dismissedTimestamp: Long) = copy(dismissedTimestamp = dismissedTimestamp)
}

private const val COMMAND_MESSAGE_ID = "cmd"

data class FocusState(
    val windowFocused: Boolean,
    val keyboardFocus: Uuid? = null,
    val commandFocus: Uuid? = null,
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

data class ContextMenuFocus(
    val focusId: Uuid,
    val menuId: Uuid,
    val parentMenu: ContextMenuFocus?,
) {
    fun hasMenu(menuId: Uuid): Boolean =
        menuId == this.menuId || parentMenu?.hasMenu(menuId) == true
    fun find(menuId: Uuid): ContextMenuFocus? =
        this.takeIf { menuId == this.menuId } ?: parentMenu?.find(menuId)
    fun dismiss(menuId: Uuid, dismissParents: Boolean): ContextMenuFocus? {
        val toDismiss = find(menuId)
        return if (toDismiss == null) {
            // Not found, not dismissing self
            this
        } else if (dismissParents) {
            null
        } else {
            // Dismiss up to parent menu if exists
            toDismiss.parentMenu
        }
    }
}

@OptIn(FlowPreview::class)
class KeyboardActionHandler(
    private val scope: CoroutineScope,
    private val windowId: WindowId,
    private val oAuthRepo: OAuthRepo = RevengeOAUthRepo,
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
    private val _lastPointerType = MutableStateFlow<PointerType?>(null)
    val lastPointerType = _lastPointerType.asStateFlow()

    private val currentFocus = MutableStateFlow<Uuid?>(null)
    // Text input that should prevent some keys from being used for bindings, mapped to allowed whitelist of keys.
    // If the map is empty, everything is allowed, else the intersection of keys is allowed.
    // [currentFocus] is keyboard-controllable and may disagree with compose internals in some edge cases,
    // thus the additional map to be safe.
    private val focusedKeyConsumers = MutableStateFlow<Map<Uuid, FocusRole>>(emptyMap())

    private val _isWindowFocused = MutableStateFlow(false)
    val isWindowsFocused = _isWindowFocused.asStateFlow()

    private val lastFocusedDestination = MutableStateFlow<Uuid?>(null)
    private val lastFocusByDestination = MutableStateFlow<Map<Uuid, Uuid>>(emptyMap())

    val currentFocusedNestingDestinations = currentFocus.map { focusId ->
        focusId ?: return@map persistentListOf()
        focusableTargets[focusId].findAllInParentHierarchy { it.role == FocusRole.NESTING_DESTINATION_ROOT_CONTAINER }
            .map {
                it.id
            }.toImmutableList()
    }

    private val _mode = MutableStateFlow<KeyboardActionMode>(KeyboardActionMode.Navigation)
    val mode = _mode.asStateFlow()

    private val _activeEditAble = MutableStateFlow<EditActions?>(null)
    val activeEditAbleId = _activeEditAble.map {
        it?.editId
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val _editPersistInProgress = MutableStateFlow<ImmutableSet<Any>>(persistentSetOf())
    val editPersistInProgress = _editPersistInProgress.asStateFlow()

    private val _keyboardPrimary = MutableStateFlow(false)
    val keyboardPrimary = combine(
        _keyboardPrimary,
        RevengePrefs.settingFlow(ScPrefs.ALWAYS_SHOW_KEYBOARD_FOCUS),
        Boolean::or,
    ).stateIn(scope, SharingStarted.Eagerly, false)

    private val handlerSettings = RevengePrefs.combinedSettingFlow { lookup ->
        KeyboardActionHandlerSettings.from(lookup)
    }.stateIn(
        scope, SharingStarted.Eagerly,
        KeyboardActionHandlerSettings.from {
            RevengePrefs.getCachedOrDefaultValue(it)
        }
    )

    private val mouseFocusRequests = MutableSharedFlow<FocusTarget>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _currentOpenContextMenu = MutableStateFlow<ContextMenuFocus?>(null)
    val currentOpenContextMenu = _currentOpenContextMenu.asStateFlow()

    val currentFocusState = combine(
        currentFocus,
        isWindowsFocused,
        mode,
        keyboardPrimary,
    ) { focused, windowFocused, currentMode, keyboardEnabled ->
        FocusState(
            windowFocused = windowFocused,
            keyboardFocus = focused.takeIf { keyboardEnabled },
            commandFocus = (currentMode as? KeyboardActionMode.Command)?.focused,
        )
    }.stateIn(scope, SharingStarted.Eagerly, FocusState(isWindowsFocused.value))

    /** Use for UI components that aren't destination-specific, otherwise use [searchQueryForDestination]. */
    val globalSearchQuery = mode.map {
        it.asSearchMode()?.query ?: ""
    }

    fun searchQueryForDestination(searchProvider: SearchProvider) = mode.map {
        it.asSearchMode()?.takeIf { it.searchProvider == searchProvider }?.query
    }

    fun needsKeyboardSearchBar(searchProvider: SearchProvider?) = mode.map { m ->
        m.asSearchMode()?.takeIf { searchProvider == it.searchProvider || searchProvider == null } != null
    }.stateIn(scope, SharingStarted.Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val commandSuggestionsState = mode.flatMapLatest { mode ->
        (mode as? KeyboardActionMode.Command)?.suggestionsProvider?.suggestionState?.map {
            Pair(mode, it)
        } ?: flowOf(null)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val focusableTargets = ConcurrentHashMap<Uuid, FocusTarget>()

    private val pendingKeyTriggersInAction = ConcurrentHashMap<KeyTrigger, Unit>()

    init {
        val creationTs = System.currentTimeMillis()
        UiState.globalMessageBoard.onEach {
            // Ignore replay of messages before this window was created.
            if (it.timestamp > creationTs) {
                publishMessage(it)
            }
        }.launchIn(scope)

        mouseFocusRequests.distinctUntilChanged().onEach {
            it.focusRequester.requestFocus()
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
        parentId: Uuid? = currentFocus?.parent?.uuid,
    ): Boolean {
        _keyboardPrimary.value = true
        if (parentId == null || currentFocus == null || currentFocus.coordinates.isEmpty) {
            // No clue what to do, but maybe compose internals have an idea
            log.i { "moveFocus: Fall back to FocusManager without current focus for $currentFocus" }
            return focusManager?.moveFocus(focusDirection) == true
        }
        val focusDirectionCheck: (FocusTarget) -> Boolean = when (focusDirection) {
            FocusDirection.Left -> {
                { it.coordinates.right <= currentFocus.coordinates.left }
            }

            FocusDirection.Right -> {
                { it.coordinates.left >= currentFocus.coordinates.right }
            }

            FocusDirection.Up -> {
                { it.coordinates.bottom <= currentFocus.coordinates.top }
            }

            FocusDirection.Down -> {
                { it.coordinates.top >= currentFocus.coordinates.bottom }
            }
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
        parentId: Uuid? = null,
        vararg roles: FocusRole,
    ): Boolean {
        val filtered = if (parentId == null && roles.isEmpty()) {
            focusableTargets.values
        } else {
            focusableTargets.values.filter {
                (roles.isEmpty() || it.role in roles) &&
                        (allowPartial || it.isFullyVisible) &&
                        (parentId == null || it.parent?.uuid == parentId)
            }
        }
        return filtered.minByOrNull {
            distanceToRect(it.coordinates, position)
        }?.focusRequester?.requestFocus() ?: false
    }

    fun focusByRoleUnlessAlreadyFocused(role: FocusRole): ActionResult {
        return if (currentFocused(fallbackToRoot = false)?.role == role) {
            ActionResult.Inapplicable
        } else {
            focusByRole(role).orActionFailure("No focusable target for role $role")
        }
    }

    fun focusByRole(role: FocusRole): Boolean {
        val focusRequester = findClosestByRole(role)?.focusRequester
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

    private fun findClosestByRole(
        role: FocusRole,
        focused: FocusTarget? = currentFocused(fallbackToRoot = false),
    ): FocusTarget? {
        val preferredDestination = lastFocusedDestination.value
        val candidates = if (preferredDestination != null) {
            focusableTargets.values.filter {
                it.role == role && preferredDestination == it.destination()
            }.ifEmpty {
                focusableTargets.values.filter { it.role == role }
            }
        } else {
            focusableTargets.values.filter { it.role == role }
        }
        val preferredPosition = focused?.coordinates?.center
            ?: preferredDestination?.let { lastFocusByDestination.value[it] }?.let {
                focusableTargets[it]?.coordinates?.center
            }
            ?: lastPointerPosition
        return candidates.minByOrNull {
            distanceToRect(it.coordinates, preferredPosition)
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
            val root = lastFocusedDestination.value?.let { focusableTargets[it] }
                ?: focusableTargets.values.filter { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }
                    .maxByOrNull { it.coordinates.contains(lastPointerPosition) }
            log.i { "No active focus, use destination root ${root?.id}" }
            return root
        }
        return null
    }

    private fun ActionContext.focused(fallbackToRoot: Boolean = true) =
        (this as? InternalActionContext)?.focused ?: currentFocused(fallbackToRoot)

    fun executeAction(
        action: InteractionAction,
        destinationStateHolder: DestinationStateHolder? = null
    ): Boolean {
        return when (action) {
            is InteractionAction.NavigationAction -> {
                val destination = action.buildDestination()
                when (action) {
                    is InteractionAction.Navigate -> {
                        updateMode { KeyboardActionMode.Navigation }
                        navigateAuto(
                            destination,
                            destinationStateHolder,
                        ) is ActionResult.Success
                    }

                    is InteractionAction.OpenWindow -> {
                        UiState.openWindow(destination, action.preferNewTask, action.initialTitle())
                        true
                    }
                }
            }

            is InteractionAction.Invoke -> action.invoke()
            is InteractionAction.HandleAction -> handleAction(action.focusId, action.action, action.args) is ActionResult.Success
            is InteractionAction.CopyToClipboard -> {
                val context = getActionContext(destinationStateHolder?.state?.value?.destination, destinationStateHolder)
                copyToClipboard(context, action.text, action.text.toStringHolder()) is ActionResult.Success
            }

            is InteractionAction.OpenInBrowser -> {
                this@KeyboardActionHandler.openLink(action.url) is ActionResult.Success
            }

            is InteractionAction.ContextMenu -> openContextMenu(action.focusId, action.menuId, action.parentMenuId)
        }
    }

    private fun navigateCurrentDestination(
        destination: Destination,
        destinationStateHolder: DestinationStateHolder? = null,
        invalidateHolderId: Boolean = false,
    ): ActionResult {
        val effectiveStateHolder = destinationStateHolder
            ?: currentFocused()?.destinationStateHolder
            ?: focusableTargets.values.firstNotNullOfOrNull { it.destinationStateHolder }
            ?: return ActionResult.Inapplicable
        if (!effectiveStateHolder.isNavigationDestinationApplicable(destination)) {
            return ActionResult.Inapplicable
        }
        effectiveStateHolder.navigate(
            destination,
            NavigationPreference.REPLACE,
            invalidateHolderId = invalidateHolderId,
        )
        return ActionResult.Success()
    }

    private fun navigateCurrentDestination(
        destinationStateHolder: DestinationStateHolder? = currentFocused()?.destinationStateHolder,
        invalidateHolderId: Boolean = false,
        buildDestination: (DestinationState) -> Destination?
    ): ActionResult {
        return destinationStateHolder?.state?.value?.let { destinationState ->
            buildDestination(destinationState)?.let {
                navigateCurrentDestination(
                    destination = it,
                    destinationStateHolder = destinationStateHolder,
                    invalidateHolderId = invalidateHolderId,
                )
            }
        } ?: ActionResult.Inapplicable
    }

    private fun navigateAuto(
        destination: Destination,
        destinationStateHolder: DestinationStateHolder? = null,
        invalidateHolderId: Boolean = false,
    ): ActionResult {
        val effectiveStateHolder = destinationStateHolder
            ?: currentFocused()?.destinationStateHolder
            ?: focusableTargets.values.firstNotNullOfOrNull { it.destinationStateHolder }
            ?: return ActionResult.Inapplicable
        if (!effectiveStateHolder.isNavigationDestinationApplicable(destination)) {
            return ActionResult.Inapplicable
        }
        effectiveStateHolder.navigate(
            destination,
            NavigationPreference.AUTO,
            invalidateHolderId = invalidateHolderId,
        )
        return ActionResult.Success()
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
            // That distinctBy looks unnecessary, but I got a unique lazy layout key constraint before somehow
            (filtered + message).distinctBy { it.uniqueId ?: it.timestamp }.toPersistentList()
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
                    if (it.dismissedTimestamp?.let { now > it + DEFAULT_MESSAGE_EXPIRY_DURATION * 2 } == true) {
                        null
                    } else if (it.autoDismissDuration?.let { duration -> now > it.timestamp + duration } == true) {
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
        val contextMenu = _currentOpenContextMenu.value
        val contextMenuEntries = contextMenu?.takeIf { keyboardPrimary.value }?.let {
            it.resolveMenuEntries(focusableTargets[it.focusId])
        }?.takeIf { it.isNotEmpty() }
        // Disallow plain keybindings of keys handled by text fields
        if (!event.isCtrlPressed && contextMenu == null) {
            val currentActiveEditableId = activeEditAbleId.value
            focusedKeyConsumers.value.forEach { (focusId, role) ->
                if (role.consumesKeyWhitelist != null && event.key !in role.consumesKeyWhitelist) {
                    return false
                }
                if (role.consumesKeyWhitelistDuringEdit != null && currentActiveEditableId != null) {
                    if (event.key in role.consumesKeyWhitelistDuringEdit) {
                        return@forEach
                    }
                    val editId = focusableTargets[focusId]?.actions?.editActions?.editId
                    if (editId == currentActiveEditableId) {
                        return false
                    }
                }
            }
        }
        return when (event.type) {
            KeyDown -> {
                val consumed = if (contextMenu != null) {
                    handleContextMenuEvent(event, contextMenuEntries, contextMenu.focusId, contextMenu.menuId)
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
        var focusToRestore: Uuid? = null
        _mode.update { old ->
            val new = update(old)
            val oldSearchProvider = old.asSearchMode()?.searchProvider
            if (oldSearchProvider != null && oldSearchProvider != new.asSearchMode()?.searchProvider) {
                oldSearchProvider.onSearchCleared()
            }
            val oldCommandSuggestionsProvider = (old as? KeyboardActionMode.Command)?.suggestionsProvider
            val newCommandSuggestionsProvider = (new as? KeyboardActionMode.Command)?.suggestionsProvider
            if (oldCommandSuggestionsProvider != newCommandSuggestionsProvider) {
                oldCommandSuggestionsProvider?.clear()
            }
            focusToRestore = when (new) {
                is KeyboardActionMode.Navigation -> old.impliedFocus()
                else -> null
            }
            new
        }
        if (focusToRestore != null) {
            focusableTargets[focusToRestore]?.focusRequester?.requestFocus()
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
                _keyboardPrimary.value = true
                updateMode { mode.copy(navigating = true) }
                windowCoordinates?.let {
                    focusClosestTo(it.topCenter, allowPartial = true, roles = FOCUSABLE_LIST_ITEMS)
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
                updateMode { it.asSearchMode() ?: KeyboardActionMode.Navigation }
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
                val direction = if (key.ctrl || key.shift) {
                    -1
                } else {
                    1
                }
                cycleCommandSuggestions(direction)
                true
            }

            KeyMapped.DirectionUp -> {
                cycleCommandSuggestions(-1)
                true
            }

            KeyMapped.DirectionDown -> {
                cycleCommandSuggestions(1)
                true
            }

            else -> false
        }
    }

    private fun cycleCommandSuggestions(direction: Int) {
        val (commandMode, suggestionsState) = commandSuggestionsState.value ?: run {
            log.e("Tried handling command mode key while not ready via suggestions state")
            return
        }
        if (suggestionsState?.currentSuggestions.isNullOrEmpty()) {
            return
        }
        val currentSuggestionIndex = if (commandMode.selectedSuggestion == null) {
            -1
        } else {
            suggestionsState.currentSuggestions.indexOfFirst { it.value == commandMode.selectedSuggestion }
        }
        val nextIndex = (currentSuggestionIndex + direction)
            .mod(suggestionsState.currentSuggestions.size + 1) // + 1 allows clearing selection again
        val nextSuggestion = suggestionsState.currentSuggestions.getOrNull(nextIndex)?.value
        updateMode {
            commandMode.copy(selectedSuggestion = nextSuggestion)
        }
    }

    private fun handleContextMenuEvent(
        event: KeyEvent,
        contextMenuEntries: List<ContextMenuEntry>?,
        focusId: Uuid,
        menuId: Uuid,
    ): Boolean {
        when (event.key) {
            Key.Escape -> dismissContextMenu(menuId)
            else -> {
                val action = contextMenuEntries?.find { it.keyboardShortcut == event.key }
                if (action != null) {
                    handleContextMenuEntry(focusId, menuId, action)
                }
            }
        }
        // Consume everything while open
        return true
    }

    fun handleContextMenuEntry(
        focusId: Uuid,
        menuId: Uuid,
        entry: ContextMenuEntry,
    ) {
        if (!entry.enabled) {
            return
        }
        when (entry) {
            is ContextMenuActionEntry -> {
                handleAction(focusId, entry.action, entry.actionArgs)
            }
            is ContextMenuCallbackEntry -> {
                handleContextCallbackAction(focusId, entry.action)
            }
            is ContextMenuSubmenuEntry -> {
                openContextMenu(focusId, entry.submenuId, parentMenuId = menuId)
            }
        }
        if (entry.autoCloseMenu) {
            dismissContextMenu(menuId, dismissParents = entry.dismissParentsOnAutoClose)
        }
    }

    private fun focusSearchResults(parentId: Uuid?) {
        focusClosestTo(Offset.Zero, allowPartial = true, roles = FOCUSABLE_LIST_ITEMS, parentId = parentId)
    }

    private fun focusCurrentContainerRelative(
        currentFocus: FocusTarget?,
        vararg roles: FocusRole,
        select: (Rect) -> Offset,
    ) = focusCurrentContainerRelative(parentId = currentFocus?.parent?.uuid, select = select, roles = roles)

    private fun focusCurrentContainerRelative(
        parentId: Uuid? = currentFocused()?.parent?.uuid,
        vararg roles: FocusRole,
        select: (Rect) -> Offset,
    ): Boolean {
        return windowCoordinates?.let { coordinates ->
            focusClosestTo(select(coordinates), allowPartial = false, parentId = parentId, roles = roles)
        } ?: false
    }

    private fun focusParent(focused: FocusTarget? = currentFocused()): Boolean {
        val parent = focused?.parent
        log.v { "Focus parent: $parent" }
        parent ?: return false
        currentFocus.value = parent.uuid
        focusableTargets[parent.uuid]?.let {
            onFocusChanged(parent.uuid, null, it.role)
        }
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

    private fun focusNextSplit(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        val currentDestination = focused?.destination()
        val availableDestinations = focusableTargets.values.filter {
            it.role == FocusRole.DESTINATION_ROOT_CONTAINER && it.id != currentDestination?.id
        }
        val target = if (availableDestinations.isEmpty()) {
            return false
        } else {
            // TODO logic to cycle in a certain direction?
            availableDestinations.first()
        }
        // Find best suitable focus target
        val lastFocusForDestination = lastFocusByDestination.value[target.id]?.let {
            focusableTargets[it]
        }
        return lastFocusForDestination?.focusRequester?.requestFocus() == true ||
                findVisibleListItemStart(target.id)?.focusRequester?.requestFocus() == true ||
                target.focusRequester.requestFocus()
    }

    private fun findFocusableListItems(parentId: Uuid?) = if (parentId == null) {
        focusableTargets.values.filter {
            it.role in FOCUSABLE_LIST_ITEMS
        }
    } else {
        findAllChildren(parentId) {
            it.role in FOCUSABLE_LIST_ITEMS
        }
    }

    private fun findVisibleListItemTop(parentId: Uuid?) = findFocusableListItems(parentId)
        .filter { it.actions?.listActions != null }
        .takeIf { it.isNotEmpty() }
        ?.minBy {
            it.coordinates.top
        }


    private fun findVisibleListItemBottom(parentId: Uuid?) = findFocusableListItems(parentId)
        .filter { it.actions?.listActions != null }
        .takeIf { it.isNotEmpty() }
        ?.maxBy {
            it.coordinates.bottom
        }

    private fun findVisibleListItemStart(parentId: Uuid?) = findFocusableListItems(parentId)
        .filter { it.actions?.listActions != null }
        .takeIf { it.isNotEmpty() }
        ?.maxBy {
            if (it.actions?.listActions?.isReverseList == true) {
                it.coordinates.bottom
            } else {
                -it.coordinates.top
            }
        }

    private fun findVisibleListItemEnd(parentId: Uuid?) = findFocusableListItems(parentId)
        .filter { it.actions?.listActions != null }
        .takeIf { it.isNotEmpty() }
        ?.maxBy {
            if (it.actions?.listActions?.isReverseList == true) {
                -it.coordinates.top
            } else {
                it.coordinates.bottom
            }
        }

    private fun scrollListToTop(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToTop(scope) {
            focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.topCenter }
        } ?: false
    }

    private fun scrollListToBottom(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToBottom(scope) {
            focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.bottomCenter }
        } ?: false
    }

    private fun scrollListToStart(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToStart(scope) {
            if (focused.actions.listActions.isReverseList) {
                focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.bottomCenter }
            } else {
                focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.topCenter }
            }
        } ?: false
    }

    private fun scrollListToEnd(
        focused: FocusTarget? = currentFocused(),
    ): Boolean {
        return focused?.actions?.listActions?.scrollToEnd(scope) {
            if (focused.actions.listActions.isReverseList) {
                focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.topCenter }
            } else {
                focusCurrentContainerRelative(focused, *FOCUSABLE_LIST_ITEMS) { it.bottomCenter }
            }
        } ?: false
    }

    fun getActionContext(
        destination: Destination?,
        destinationStateHolder: DestinationStateHolder?,
        criticalActionRequiresConfirmation: Boolean = true,
    ): ActionContext = getInternalActionContext(
        focused = currentFocused(),
        criticalActionRequiresConfirmation = criticalActionRequiresConfirmation,
        keybindingConfig = UiState.keybindingsConfig.value,
        currentDestinationType = destination?.destinationId,
        destinationStateHolder = destinationStateHolder,
    )

    fun handleAction(
        focusItem: Uuid,
        action: Action,
        args: List<String> = emptyList(),
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

    fun handleContextCallbackAction(
        focusItem: Uuid,
        block: suspend ActionContext.() -> ActionResult,
    ): ActionResult {
        val focused = focusableTargets[focusItem] ?: run {
            log.e("Invoked handleContextCallbackAction on unregistered focus item")
            currentFocused()
        }
        val context = getInternalActionContext(focused, criticalActionRequiresConfirmation = true)

        return context.launchActionAsync(
            "callbackAction",
            GlobalActionsScope,
        ) {
            context.block()
        }
    }

    private fun getInternalActionContext(
        focused: FocusTarget?,
        criticalActionRequiresConfirmation: Boolean,
        keybindingConfig: KeybindingConfig? = UiState.keybindingsConfig.value,
        currentDestinationType: DestinationEnum? = focused?.destinationStateHolder?.state?.value?.destination?.destinationId,
        destinationStateHolder: DestinationStateHolder? = focused?.destinationStateHolder,
        implicitArgs: CommandArgContext = getCurrentKeyActionHandlers(focused).flatMap { it.impliedArguments() }.distinct(),
        asyncCallback: ActionResultCallback? = null,
    ) = object : InternalActionContext {
        override val windowId = this@KeyboardActionHandler.windowId
        override fun publishMessage(message: AbstractAppMessage) =
            this@KeyboardActionHandler.publishMessage(message)
        override fun dismissMessage(uniqueId: String) =
            this@KeyboardActionHandler.dismissMessage(uniqueId)
        override fun copyToClipboard(content: String, description: ComposableStringHolder?) =
            this@KeyboardActionHandler.copyToClipboard(this, content, description)
        override fun viewInExternalApp(content: String, fileExtension: String) =
            this@KeyboardActionHandler.viewInExternalApp(this, content, fileExtension)
        override fun getFilesFromClipboard() = this@KeyboardActionHandler.getFilesFromClipboard()
        override fun getStringFromClipboard() = this@KeyboardActionHandler.getStringFromClipboard()
        override fun openLink(uri: String): ActionResult =
            this@KeyboardActionHandler.openLink(uri)
        override fun focusByRole(role: FocusRole) =
            this@KeyboardActionHandler.focusByRole(role)
        override fun focusByRoleUnlessAlreadyFocused(role: FocusRole) =
            this@KeyboardActionHandler.focusByRoleUnlessAlreadyFocused(role)
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
        override suspend fun onAsyncActionResult(result: ActionResult) {
            asyncCallback?.onActionResult(result)
        }
        override val focused = focused
        override val criticalActionRequiresConfirmation = criticalActionRequiresConfirmation
        override val keybindingConfig = keybindingConfig
        override val currentDestinationType = currentDestinationType
        override val destinationStateHolder = destinationStateHolder
        override val implicitArgs = implicitArgs
    }

    private fun navigationItemActionHandler(
        destinationStateHolder: DestinationStateHolder?,
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
                        destinationStateHolder ?: return ActionResult.Inapplicable
                        updateMode { KeyboardActionMode.Navigation }
                        navigateCurrentDestination(destination, destinationStateHolder)
                    }
                    Action.NavigationItem.NavigateInNewWindow -> {
                        UiState.openWindow(destination, true, navigationActionable.initialTitle())
                        ActionResult.Success()
                    }
                }
            }
        }

    private fun copyAbleActionHandler(
        copyActions: CopyActions,
    ) = object : KeyboardActionProvider<Action.CopyAble> {
        override fun getPossibleActions(): Set<Action.CopyAble> = setOfNotNull(
            Action.CopyAble.CopyPlaintext.takeIf {
                copyActions.accessPlaintext != null || copyActions.accessPlaintextSuspend != null
            },
            Action.CopyAble.ViewPlaintext.takeIf {
                copyActions.accessPlaintext != null || copyActions.accessPlaintextSuspend != null
            },
            Action.CopyAble.CopyUserId.takeIf { copyActions.accessUserId != null },
            Action.CopyAble.CopyFilePath.takeIf { copyActions.accessFilePath != null },
        )
        override fun ensureActionType(action: Action) = action as? Action.CopyAble


        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.copyAble.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.CopyAble,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.CopyAble.CopyPlaintext,
                Action.CopyAble.ViewPlaintext -> {
                    val content = copyActions.accessPlaintext?.invoke()?.takeIf(String::isNotEmpty)
                        ?: copyActions.accessPlaintextSuspend?.let { plaintext ->
                            return context.launchActionAsync(
                                if (action == Action.CopyAble.CopyPlaintext) "copyPlaintext" else "viewPlaintext",
                                scope,
                            ) {
                                val content = plaintext()?.takeIf(String::isNotEmpty) ?: return@launchActionAsync ActionResult.Inapplicable
                                if (action == Action.CopyAble.CopyPlaintext) {
                                    context.copyToClipboard(content)
                                } else {
                                    context.viewInExternalApp(content)
                                }
                            }
                        } ?: return ActionResult.Inapplicable
                    if (action == Action.CopyAble.CopyPlaintext) {
                        context.copyToClipboard(content)
                    } else {
                        context.viewInExternalApp(content)
                    }
                }
                Action.CopyAble.CopyUserId -> {
                    val content = copyActions.accessUserId?.invoke() ?: return ActionResult.Inapplicable
                    context.copyToClipboard(content)
                }
                Action.CopyAble.CopyMxcUrl -> {
                    val content = copyActions.accessMxcUrl?.invoke() ?: return ActionResult.Inapplicable
                    context.copyToClipboard(content)
                }
                Action.CopyAble.CopyFilePath -> {
                    val content = copyActions.accessFilePath?.invoke() ?: return ActionResult.Inapplicable
                    context.copyToClipboard(content)
                }
            }
        }
    }

    private fun editableActionHandler(editActions: EditActions) = when (editActions) {
        is PlaintextEditActions -> plaintextEditAbleActionHandler(editActions)
    }

    private fun plaintextEditAbleActionHandler(
        editActions: PlaintextEditActions,
    ) = object : KeyboardActionProvider<Action.PlaintextEditAble> {
        override fun getPossibleActions(): Set<Action.PlaintextEditAble> = Action.PlaintextEditAble.values().toSet()
        override fun ensureActionType(action: Action) = action as? Action.PlaintextEditAble

        override fun handleNavigationModeEvent(
            context: ActionContext,
            key: KeyTrigger
        ): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.editAble.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.PlaintextEditAble,
            args: List<String>
        ): ActionResult {
            return when (action) {
                Action.PlaintextEditAble.LaunchEdit -> {
                    setEditable(editActions)
                    ActionResult.Success()
                }
                Action.PlaintextEditAble.DiscardEdit -> {
                    if (setEditableExpecting(null, editActions.editId)) {
                        // Collapsing an editable can move the current focus out of composition
                        editActions.stableFocusId?.let {
                            focusableTargets[it]?.focusRequester?.requestFocus()
                        }
                        ActionResult.Success()
                    } else {
                        ActionResult.Inapplicable
                    }
                }
                Action.PlaintextEditAble.SaveEdit -> {
                    val editId = editActions.editId
                    val appMessageId = "saveEdit/$editId"
                    // Collapsing an editable can move the current focus out of composition
                    editActions.stableFocusId?.let {
                        focusableTargets[it]?.focusRequester?.requestFocus()
                    }
                    context.launchActionAsync(
                        "saveEdit/$editId",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        appMessageId,
                    ) {
                        _editPersistInProgress.update { (it + editId).toPersistentHashSet() }
                        try {
                            editActions.persistEdit()?.toActionResult() ?: ActionResult.Inapplicable
                        } finally {
                            _editPersistInProgress.update { (it - editId).toPersistentHashSet() }
                            setEditableExpecting(null, editId)
                        }
                    }
                }
            }
        }
    }

    private fun setEditable(editActions: EditActions?) {
        val previous = _activeEditAble.getAndUpdate { editActions }
        previous?.discardEdit()
    }

    private fun setEditableExpecting(editActions: EditActions?, expectEditId: Any?): Boolean {
        var matched = false
        val previous = _activeEditAble.getAndUpdate {
            if (it?.editId == expectEditId) {
                matched = true
                editActions
            } else {
                matched = false
                it
            }
        }
        if (matched) {
            previous?.discardEdit()
        }
        return matched
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
            _keyboardPrimary.value = true
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
                Action.Focus.FocusNextSplit -> focusNextSplit(context.focused())
                Action.Focus.FocusVisibleListTop -> findVisibleListItemTop(context.focused()?.destination()?.id)?.focusRequester?.requestFocus() == true
                Action.Focus.FocusVisibleListBottom -> findVisibleListItemBottom(context.focused()?.destination()?.id)?.focusRequester?.requestFocus() == true
                Action.Focus.FocusVisibleListStart -> findVisibleListItemStart(context.focused()?.destination()?.id)?.focusRequester?.requestFocus() == true
                Action.Focus.FocusVisibleListEnd -> findVisibleListItemEnd(context.focused()?.destination()?.id)?.focusRequester?.requestFocus() == true
                Action.Focus.FocusByRole -> {
                    val roleString = args.firstOrNull().orActionValidationError()
                    val role = tryOrNull { FocusRole.valueOf(roleString) }.orActionValidationError()
                    val allowContainer = role.preferFocusChildren()
                    val currentFocus = context.focused(fallbackToRoot = allowContainer)?.takeIf {
                        !it.role.preferFocusChildren() || allowContainer
                    }
                    if (currentFocus?.role == role) {
                        // Already focused, no-op
                        false
                    } else {
                        findClosestByRole(role, currentFocus)?.focusRequester?.requestFocus() == true
                    }
                }
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
                    context.launchActionAsync("navigateCurrent", scope) {
                        val extraArgs = args.subList(1, args.size)
                        val destination = args[0].toDestinationOrNull(extraArgs, context.implicitArgs).orActionValidationError()
                        navigateCurrentDestination(context.destinationStateHolder) { destination }
                    }
                }
                Action.Navigation.NavigateAuto -> {
                    context.launchActionAsync("navigateAuto", scope) {
                        val extraArgs = args.subList(1, args.size)
                        val destination = args[0].toDestinationOrNull(extraArgs, context.implicitArgs).orActionValidationError()
                        navigateAuto(destination, context.destinationStateHolder)
                    }
                }
                Action.Navigation.NavigateInNewWindow -> {
                    context.launchActionAsync("navigateInNewWindow", scope) {
                        val extraArgs = args.subList(1, args.size)
                        val destination =
                            args[0].toDestinationOrNull(extraArgs, context.implicitArgs).orActionValidationError()
                        UiState.openWindow(destination, true)
                        ActionResult.Success()
                    }
                }
                Action.Navigation.SplitHorizontal -> {
                    context.launchActionAsync("splitHorizontal", scope) {
                        val destination = if (args.isEmpty()) {
                            null
                        } else {
                            val extraArgs = args.subList(1, args.size)
                            args[0].toDestinationOrNull(extraArgs, context.implicitArgs).orActionValidationError()
                        }
                        navigateCurrentDestination(
                            destinationStateHolder = context.destinationStateHolder,
                            invalidateHolderId = true,
                        ) { destinationState ->
                            Destination.SplitHorizontal(
                                DefaultDestinationStateHolder(destinationState),
                                DestinationStateHolder.forInitialDestination(
                                    destination ?: destinationState.destination
                                ),
                            )
                        }
                    }
                }
                Action.Navigation.SplitVertical -> {
                    context.launchActionAsync("splitVertical", scope) {
                        val destination = if (args.isEmpty()) {
                            null
                        } else {
                            val extraArgs = args.subList(1, args.size)
                            args[0].toDestinationOrNull(extraArgs, context.implicitArgs).orActionValidationError()
                        }
                        navigateCurrentDestination(
                            destinationStateHolder = context.destinationStateHolder,
                            invalidateHolderId = true,
                        ) { destinationState ->
                            Destination.SplitVertical(
                                DefaultDestinationStateHolder(destinationState),
                                DestinationStateHolder.forInitialDestination(
                                    destination ?: destinationState.destination
                                ),
                            )
                        }
                    }
                }
                Action.Navigation.CloseWindow -> {
                    if (UiState.closeWindow(windowId)) {
                        ActionResult.Success()
                    } else {
                        ActionResult.Inapplicable
                    }
                }
                Action.Navigation.CloseWindowUnlessLast -> {
                    if (UiState.closeWindow(windowId, closeUnlessLast = true)) {
                        ActionResult.Success()
                    } else {
                        ActionResult.Inapplicable
                    }
                }
            }
        }
    }

    fun closeWindow() {
        UiState.closeWindow(windowId)
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
                        ActionResult.NoOp
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
        override fun getPossibleActions() = Action.Global.entries.toSet().let {
            if (windowId == HEADLESS_WINDOW_ID) {
                it
            } else {
                it - Action.Global.HandleOAuthResponse
            }
        }
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
                        val previousSearch = mode.value.asSearchMode()
                        if (previousSearch != null) {
                            updateMode { previousSearch }
                            focusByRole(FocusRole.SEARCH_BAR)
                            ActionResult.Success()
                        } else {
                            handleSearchUpdate("", null, null, navigating = false) {
                                focusByRole(FocusRole.SEARCH_BAR)
                            }.orActionInapplicable()
                        }
                    }
                }
                Action.Global.Command -> {
                    val currentMode = mode.value
                    val explicitFocus = context.focused(false)
                    if (currentMode is KeyboardActionMode.Command) {
                        if (explicitFocus != null && explicitFocus != currentMode.focused) {
                            handleCommandInput(
                                TextFieldValue(currentMode.command, TextRange(currentMode.command.length)),
                                explicitFocus,
                            ) {
                                focusByRole(FocusRole.COMMAND_BAR)
                            }.orActionInapplicable()
                        } else {
                            // CMD already active, just focus again
                            focusByRole(FocusRole.COMMAND_BAR).orActionInapplicable()
                        }
                    } else {
                        handleCommandInput(TextFieldValue(""), explicitFocus) {
                            focusByRole(FocusRole.COMMAND_BAR)
                        }.orActionInapplicable()
                    }
                }
                Action.Global.SetSetting -> {
                    context.launchActionAsync(
                        "setSetting",
                        scope,
                        appMessageId = SETTINGS_MESSAGE_ID,
                    ) {
                        RevengePrefs.handleSetAction(context, args)
                    }
                }
                Action.Global.ResetSetting -> {
                    context.launchActionAsync(
                        "resetSetting",
                        scope,
                        appMessageId = SETTINGS_MESSAGE_ID,
                    ) {
                        RevengePrefs.handleResetAction(context, args)
                    }
                }
                Action.Global.ToggleSetting -> {
                    context.launchActionAsync(
                        "toggleSetting",
                        scope,
                        appMessageId = SETTINGS_MESSAGE_ID,
                    ) {
                        RevengePrefs.handleToggleAction(context, args)
                    }
                }
                Action.Global.Exit -> {
                    UiState.exit()
                    ActionResult.Success()
                }
                Action.Global.SetMinimized -> {
                    val minimized = args.firstOrNull()?.toBooleanStrictOrNull() ?: true
                    UiState.setMinimized(minimized)
                }
                Action.Global.ToggleMinimized -> {
                    UiState.setMinimized(!UiState.minimizedToTray.value)
                }
                Action.Global.RecreateUi -> {
                    UiState.recreateUi()
                    ActionResult.Success()
                }
                Action.Global.RecreateWindow -> {
                    UiState.recreateWindow(windowId)
                    ActionResult.Success()
                }
                Action.Global.ClearSessionCache -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val appMessageId = "clearCache/$sessionId"
                    context.launchActionAsync(
                        "clearCache",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        appMessageId,
                        notifyProcessing = true,
                    ) {
                        log.i("Clearing session cache for $sessionId")
                        UiState.disableSession(sessionId)
                        client.clearCache()
                        log.i("Session cache cleared, restarting $sessionId")
                        UiState.enableSession(sessionId)
                        ActionResult.Success(notifySuccess = false)
                    }
                }
                Action.Global.VacuumDatabase -> {
                    val sessionId = args.firstOrNull()
                    val sessionIds = if (sessionId == null) {
                        UiState.currentValidSessionIds.value.orEmpty().takeIf { it.isNotEmpty() }
                            ?: return ActionResult.Inapplicable
                    } else {
                        listOf(sessionId)
                    }
                    context.launchActionAsync(
                        "vacuumDb",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        "vacuumDb",
                        notifyProcessing = true,
                    ) {
                        var error: ActionResult.Failure? = null
                        var success: ActionResult.Success? = null
                        var notFound = 0
                        sessionIds.forEach {
                            publishMessage(
                                AppMessage(
                                    "Vacuuming $it".toStringHolder(),
                                    uniqueId = "vacuumDb",
                                    autoDismissDuration = null,
                                )
                            )
                            val sessionId = SessionId(it)
                            val client = UiState.currentClientFor(sessionId)
                            if (client == null) {
                                notFound++
                                return@forEach
                            }
                            val result = client.performDatabaseVacuum().toActionResult()
                            if (result is ActionResult.Failure && error == null) {
                                error = result
                            } else if (result is ActionResult.Success) {
                                success = result
                            }
                        }
                        error
                            ?: if (notFound > 0) {
                                ActionResult.Failure("Unable to find $notFound/${sessionIds.size} clients")
                            } else {
                                success ?: ActionResult.Inapplicable
                            }
                    }
                }
                Action.Global.CopyGlobalAccountData,
                Action.Global.ViewGlobalAccountData -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    context.launchActionAsync(
                        if (action == Action.Global.CopyGlobalAccountData) "copyGlobalAccountData" else "viewGlobalAccountData",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        if (action == Action.Global.CopyGlobalAccountData) "copyGlobalAccountData" else "viewGlobalAccountData",
                        notifyProcessing = true,
                    ) {
                        val result = client.getGlobalAccountData()
                        if (result.isSuccess) {
                            val joined = result.getOrNull().formatEventContentDump({ it.eventType }, { it.content.toPrettyJson() })
                            if (action == Action.Global.CopyGlobalAccountData) {
                                context.copyToClipboard(
                                    joined,
                                    Res.string.command_copy_name_full_account_data.toStringHolder()
                                )
                            } else {
                                context.viewInExternalApp(joined, ".md")
                            }
                        } else {
                            result.toActionResult()
                        }
                    }
                }
                Action.Global.SetGlobalAccountData -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val eventType = args.getOrNull(1).orActionValidationError()
                    val content = args.getOrNull(2) ?: "{}"
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    context.launchActionAsync(
                        "setAccountData",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        "setAccountData/$sessionId/$eventType",
                        notifyProcessing = true,
                    ) {
                        client.setAccountData(eventType, content).toActionResult()
                    }
                }
                Action.Global.CreateRoom -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val name = args.getOrNull(1)
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val parameters = CreateRoomParameters(
                        name = name,
                        isEncrypted = true,
                        visibility = RoomVisibility.Private,
                        isDirect = false,
                        preset = RoomPreset.PRIVATE_CHAT,
                    )
                    context.launchCreateRoomAction("createRoom", client, parameters)
                }
                Action.Global.CreateUnencryptedRoom -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val name = args.getOrNull(1)
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val parameters = CreateRoomParameters(
                        name = name,
                        isEncrypted = false,
                        visibility = RoomVisibility.Private,
                        isDirect = false,
                        preset = RoomPreset.PRIVATE_CHAT,
                    )
                    context.launchCreateRoomAction("createRoom", client, parameters)
                }
                Action.Global.CreateDm -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val userId = UserId(args.getOrNull(1).orActionValidationError())
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    context.launchActionAsync(
                        "createDm",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        "createDm",
                        notifyProcessing = true,
                    ) {
                        val result = client.createDM(userId, isEncrypted = true)
                        if (result.isSuccess) {
                            publishMessage(
                                AppMessage(
                                    StringResourceHolder(Res.string.toast_room_created, result.getOrNull().toString().toStringHolder()),
                                    uniqueId = "createDm",
                                    autoDismissDuration = null,
                                )
                            )
                            ActionResult.Success()
                        } else {
                            result.toActionResult()
                        }
                    }
                }
                Action.Global.CreateSpace -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val name = args.getOrNull(1)
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val parameters = CreateRoomParameters(
                        name = name,
                        isEncrypted = false,
                        visibility = RoomVisibility.Private,
                        isDirect = false,
                        preset = RoomPreset.PRIVATE_CHAT,
                        isSpace = true,
                    )
                    context.launchCreateRoomAction("createSpace", client, parameters)
                }
                Action.Global.AutoSubscribeNotifiableRooms -> {
                    NotifiableRoomSubscriber.launch()
                    ActionResult.Success()
                }
                Action.Global.InspectFocusable -> {
                    context.copyToClipboard(context.focused().toString())
                }
                Action.Global.Join -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val roomAddress = args.getOrNull(1)
                        ?.let { RoomIdOrAlias.from(it) }
                        .orActionValidationError()
                    val via = args.getOrNull(2)?.let { CommandParser.parseSplittable(it) }.orEmpty()
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val actionId = "joinRoom/$sessionId/$roomAddress"
                    context.launchActionAsync(
                        actionId,
                        GlobalActionsScope,
                        Dispatchers.IO,
                        actionId,
                        notifyProcessing = true,
                    ) {
                        client.joinRoomByIdOrAliasTracked(roomAddress, via).toActionResult()
                    }
                }
                Action.Global.VerifyUser -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val userId = UserId(args.getOrNull(1).orActionValidationError())
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val actionId = "verifyUser/$sessionId/$userId"
                    context.launchActionAsync(
                        actionId,
                        GlobalActionsScope,
                        Dispatchers.IO,
                        actionId,
                        notifyProcessing = true,
                    ) {
                        if (RevengeDeviceVerificationProvider.setActiveRequest(
                                ScOutgoingVerificationRequest(
                                    sessionId = sessionId,
                                    ts = System.currentTimeMillis(),
                                    request = VerificationRequest.Outgoing.User(userId),
                                )
                            )
                        ) {
                            client.sessionVerificationService.requestUserVerification(userId)
                            UiState.openWindow(Destination.VerificationRequest(sessionId), false)
                            ActionResult.Success()
                        } else {
                            ActionResult.Failure("Failed to initiate request")
                        }
                    }
                }
                Action.Global.ConsumeLink -> {
                    val rawLink = args.firstOrNull().orActionValidationError()
                    val link = MatrixLinkPatterns.parseMatrixLink(rawLink)
                        .also {
                            log.e { "Consuming matrix link: $rawLink -> $it" }
                        }
                        .orActionValidationError()
                    consumeLink(link)
                    ActionResult.Success()
                }
                Action.Global.EnableImagePack -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val roomId = args.getOrNull(1).orActionValidationError()
                    val stateKey = args.getOrNull(2) ?: ""
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val actionId = "imagePack/$sessionId/$roomId/$stateKey"
                    context.launchActionAsync(
                        actionId,
                        GlobalActionsScope,
                        Dispatchers.IO,
                        actionId,
                        notifyProcessing = true,
                    ) {
                        client.updateAccountData("m.image_pack.rooms") {
                            it.orEmpty().toMutableMap().apply {
                                this["rooms"] = this["rooms"]?.jsonObject.orEmpty().toMutableMap().apply {
                                    this[roomId] = this[roomId]?.jsonObject.orEmpty().toMutableMap().apply {
                                        this[stateKey] = JsonObject(emptyMap())
                                    }.let(::JsonObject)
                                }.let(::JsonObject)
                            }.let(::JsonObject)
                        }
                    }
                }
                Action.Global.DisableImagePack -> {
                    val sessionId = SessionId(args.firstOrNull().orActionValidationError())
                    val roomId = args.getOrNull(1).orActionValidationError()
                    val stateKey = args.getOrNull(2) ?: ""
                    val client = UiState.currentClientFor(sessionId) ?: return ActionResult.Failure("Client not ready")
                    val actionId = "imagePack/$sessionId/$roomId/$stateKey"
                    context.launchActionAsync(
                        actionId,
                        GlobalActionsScope,
                        Dispatchers.IO,
                        actionId,
                        notifyProcessing = true,
                    ) {
                        client.updateAccountData("m.image_pack.rooms") {
                            it?.toMutableMap()?.apply {
                                this["rooms"]?.jsonObject?.toMutableMap()?.apply {
                                    this[roomId]?.jsonObject?.toMutableMap()?.apply {
                                        remove(stateKey)
                                    }?.let {
                                        if (it.isEmpty()) {
                                            this.remove(roomId)
                                        } else {
                                            this[roomId] = JsonObject(it)
                                        }
                                    }
                                }?.let { this["rooms"] = JsonObject(it) }
                            }?.let(::JsonObject)
                        }
                    }
                }
                Action.Global.HandleOAuthResponse -> {
                    val path = args.firstOrNull().orActionValidationError()
                    context.launchActionAsync(
                        "handleOAuthResponse/$path",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        "handleOAuthResponse",
                        notifyProcessing = true,
                    ) {
                        oAuthRepo.handleOAuthLoginCallback(path)
                    }
                }
                Action.Global.Logout -> {
                    val sessionIdString = args.firstOrNull().orActionValidationError()
                    val sessionId = SessionId(sessionIdString)
                    withCriticalActionConfirmation(
                        context as? InternalActionContext,
                        prompt = Res.string.action_logout_session_prompt.toStringHolder(sessionIdString.toStringHolder()),
                        confirmText = Res.string.action_logout.toStringHolder(),
                    ) {
                        context.launchActionAsync(
                            "logout/$sessionIdString",
                            GlobalActionsScope,
                            Dispatchers.IO,
                            "logout/$sessionIdString",
                            notifyProcessing = true,
                        ) {
                            logout(sessionId, deleteOnRestoreFailure = false)
                        }
                    }
                }
                Action.Global.LogoutOrDelete -> {
                    val sessionIdString = args.firstOrNull().orActionValidationError()
                    val sessionId = SessionId(sessionIdString)
                    withCriticalActionConfirmation(
                        context as? InternalActionContext,
                        prompt = Res.string.action_logout_session_prompt.toStringHolder(sessionIdString.toStringHolder()),
                        confirmText = Res.string.action_logout.toStringHolder(),
                    ) {
                        context.launchActionAsync(
                            "logout/$sessionIdString",
                            GlobalActionsScope,
                            Dispatchers.IO,
                            "logout/$sessionIdString",
                            notifyProcessing = true,
                        ) {
                            logout(sessionId, deleteOnRestoreFailure = true)
                        }
                    }
                }
            }
        }
    }

    suspend fun logout(sessionId: SessionId, deleteOnRestoreFailure: Boolean): ActionResult {
        val sessionCache = UiState.appGraph.sessionCache
        val sessionStore = UiState.appGraph.sessionStore
        val restoreFailed = sessionCache.getOrRestore(sessionId)
            .getOrElse {
                if (!deleteOnRestoreFailure) return it.toActionResult() else null
            }
            ?.logout(userInitiated = true, ignoreSdkError = deleteOnRestoreFailure) == null
        if (restoreFailed && deleteOnRestoreFailure) {
            log.e { "Failed to logout session for $sessionId, deleting anyway" }
            sessionStore.removeSession(sessionId.value)
        }
        return ActionResult.Success()
    }

    private fun consumeLink(link: MatrixToLink) {
        val destination = Destination.SessionSelector(link.rawUrl.toStringHolder()) { sessionId ->
            when (link) {
                is MatrixToLink.MessageLink -> link.toDestination(sessionId)
                is MatrixToLink.RoomLink -> link.toDestination(sessionId)
                is MatrixToLink.UserMention -> Result.success(link.toDestination(sessionId, null))
            }
        }
        UiState.openWindow(destination, false)
    }

    private fun ActionContext.launchCreateRoomAction(
        actionName: String,
        client: MatrixClient,
        parameters: CreateRoomParameters,
    ): ActionResult {
        return launchActionAsync(
            actionName,
            GlobalActionsScope,
            Dispatchers.IO,
            actionName,
            notifyProcessing = true,
        ) {
            val result = client.createRoom(parameters)
            if (result.isSuccess) {
                publishMessage(
                    AppMessage(
                        StringResourceHolder(Res.string.toast_room_created, result.getOrNull().toString().toStringHolder()),
                        uniqueId = actionName,
                        autoDismissDuration = null,
                    )
                )
                ActionResult.Success()
            } else {
                result.toActionResult()
            }
        }
    }

    private fun getCurrentKeyActionHandlers(focused: FocusTarget?): List<KeyboardActionProvider<*>> {
        return listOfNotNull(
            appMessageHandler,
            focused?.actions?.keyActions,
            (focused?.actions?.primaryAction as? InteractionAction.NavigationAction)?.let {
                navigationItemActionHandler(focused.destinationStateHolder, it)
            },
            (focused?.actions?.editActions ?: _activeEditAble.value)?.let {
                editableActionHandler(it)
            },
            (focused?.actions?.copyActions)?.let {
                copyAbleActionHandler(it)
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

    fun onFocusChanged(
        target: Uuid,
        state: FocusState?,
        role: FocusRole,
    ) {
        //log.v { "Focus changed for $target to $state" }
        if (role.consumesKeyWhitelist != null || role.consumesKeyWhitelistDuringEdit != null) {
            focusedKeyConsumers.update {
                if (state?.hasFocus == true) {
                    it + (target to role)
                } else {
                    it - target
                }
            }
        }
        var lostFocusTargetId: Uuid? = null
        if (state == null) {
            // Should already happen in focusParent() via key actions where we already set this...?
            if (currentFocus.value != target) {
                log.e("Tried to request focus on null state")
            }
        } else if (state.isFocused) {
            currentFocus.update {
                lostFocusTargetId = it
                target
            }
        } else if (!state.hasFocus) {
            currentFocus.update {
                if (it == target) {
                    lostFocusTargetId = it
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
        val lostFocusTarget = lostFocusTargetId?.let { focusableTargets[it] }
        lostFocusTarget?.let(::handleLostFocus)
        var newFocusedDestination: Uuid? = null
        lastFocusByDestination.update {
            it.filter {
                it.value != lostFocusTargetId
            }.let { filtered ->
                val destination = newFocus?.destination()
                if (destination == null) {
                    newFocusedDestination = null
                    filtered
                } else {
                    newFocusedDestination = destination.id
                    filtered + (destination.id to newFocus.id)
                }
            }
        }
        if (newFocusedDestination != null) {
            lastFocusedDestination.value = newFocusedDestination
        }
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
        target: Uuid,
        parent: FocusParent?,
        coordinates: LayoutCoordinates,
        focusRequester: AbstractFocusRequester,
        destinationStateHolder: DestinationStateHolder?,
        actions: ActionProvider?,
        role: FocusRole = FocusRole.LIST_ITEM,
    ) {
        val bounds = try {
            coordinates.boundsInWindow()
        } catch (e: IllegalStateException) {
            unregisterFocusTarget(target)
            return
        }
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

    fun unregisterFocusTarget(target: Uuid) {
        focusableTargets.remove(target)
        focusedKeyConsumers.update { it - target }
    }

    fun handlePointer(position: Offset, type: PointerEventType, pointerType: PointerType) {
        val previous = _lastPointerPosition
        // Don't action if nothing changed
        if (_lastPointerPosition == position) {
            return
        }

        _lastPointerType.value = pointerType

        if (type != PointerEventType.Move) {
            // Still need to track the updated position so we don't trigger focus changes that we don't want,
            // particularly while focusing composer / search / command bar
            log.d { "Not updating focus on $type via $previous -> $position" }
            _lastPointerPosition = position
            return
        }

        // Don't follow mouse focus on editing
        if (activeEditAbleId.value != null) {
            return
        }

        // For some text fields (command bar, search, composer) it can be pretty annoying losing focus by accident,
        // when the mouse just moved a little bit.
        // We need to do this check before persisting the new position so eventually moving the cursor enough still
        // triggers focus updates even if moved slowly.
        val currentFocusable = currentFocus.value?.let { focusableTargets[it] }
        if (currentFocusable?.role?.autoRequestFocus == true) {
            // Squared distance is cheaper than raw distance and sufficient for our needs
            val distanceSquared = (position - previous).getDistanceSquared()
            if (distanceSquared < 20) {
                log.d { "Not losing focus on ${currentFocusable.role} from moving pointer $previous -> $position [distanceSquare: $distanceSquared]" }
                return
            } else {
                log.d { "Losing focus on ${currentFocusable.role} from moving pointer $previous -> $position [distanceSquare: $distanceSquared]" }
            }
        }

        // Remember pointer position
        _lastPointerPosition = position
        _keyboardPrimary.value = false

        // Check if we should focus any elements below the pointer
        if (!handlerSettings.value.focusFollowsMouse) {
            return
        }

        // Find element to focus and request focus
        val focusCandidates = focusableTargets.values.filter {
            it.isFullyVisible && it.coordinates.contains(position)
        }
        val newFocusable = focusCandidates.firstNotNullOfOrNull { target ->
            target.takeIf {
                it.role != FocusRole.CONTAINER &&
                        it.role != FocusRole.NESTING_DESTINATION_ROOT_CONTAINER &&
                        it.role != FocusRole.DESTINATION_ROOT_CONTAINER &&
                        it.role != FocusRole.NESTED_AUX_ITEM
            }
        } ?: focusCandidates.firstNotNullOfOrNull { target ->
            target.takeIf {
                it.role == FocusRole.DESTINATION_ROOT_CONTAINER &&
                        (currentFocusable == null || !it.coordinates.contains(currentFocusable.coordinates.center))
            }
        }
        newFocusable?.let {
            mouseFocusRequests.tryEmit(it)
        }
    }

    fun onSearchType(
        query: String,
        searchProvider: SearchProvider?,
        searchFocusContainer: Uuid?,
    ) = handleSearchUpdate(query, searchProvider, searchFocusContainer, navigating = false) {
        it.searchProvider.onSearchEnter(it.query)
    }

    fun onSearchEnter(
        searchProvider: SearchProvider?,
        searchFocusContainer: Uuid?,
        query: String? = null,
    ) {
        handleSearchUpdate(query, searchProvider, searchFocusContainer, navigating = true) {
            it.searchProvider.onSearchEnter(it.query)
            focusSearchResults(it.searchFocusContainer)
        }
    }

    fun clearSearch() {
        updateMode { KeyboardActionMode.Navigation }
    }

    private fun handleSearchUpdate(
        query: String?,
        searchProvider: SearchProvider?,
        searchFocusContainer: Uuid?,
        navigating: Boolean,
        handleSuccess: (KeyboardActionMode.Search) -> Unit,
    ): Boolean {
        var success: KeyboardActionMode.Search? = null
        updateMode { mode ->
            if (mode is KeyboardActionMode.Search) {
                mode.copy(
                    query = query ?: mode.query,
                    navigating = navigating && (searchProvider == null || mode.searchProvider == searchProvider),
                    searchProvider = searchProvider ?: mode.searchProvider,
                    searchFocusContainer = searchFocusContainer ?: mode.searchFocusContainer,
                ).also {
                    success = it
                }
            } else {
                if (searchProvider == null) {
                    val current = currentFocused() ?: focusableTargets.values.firstNotNullOfOrNull {
                        it.takeIf { it.actions?.searchProvider != null }
                    }
                    if (current?.actions?.searchProvider != null) {
                        KeyboardActionMode.Search(
                            query = query ?: "",
                            searchProvider = current.actions.searchProvider,
                            navigating = navigating,
                            searchFocusContainer = searchFocusContainer ?: current.parent?.uuid,
                        ).also {
                            success = it
                        }
                    } else {
                        success = null
                        log.w { "Updates search but no search provider available" }
                        mode
                    }
                } else {
                    KeyboardActionMode.Search(
                        query = query ?: "",
                        searchProvider = searchProvider,
                        navigating = navigating,
                        searchFocusContainer = searchFocusContainer,
                    ).also {
                        success = it
                    }
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
        clearCommandMode()
    }

    fun clearCommandMode() {
        updateMode {
            it.takeIf { it !is KeyboardActionMode.Command } ?: it.asSearchMode() ?: KeyboardActionMode.Navigation
        }
    }

    fun dismissContextMenu(id: Uuid, dismissParents: Boolean = false): Boolean {
        var dismissed = false
        _currentOpenContextMenu.update { current ->
            current?.dismiss(id, dismissParents).also {
                dismissed = it != current
            }
        }
        return dismissed
    }

    fun openContextMenu(focusId: Uuid, menuId: Uuid = focusId, parentMenuId: Uuid? = null): Boolean {
        val focusTarget = focusableTargets[focusId]
        if (focusTarget == null) {
            log.e("Tried to open context menu on unregistered target $focusId")
            return false
        }
        focusTarget.actions?.findInteractionAction<InteractionAction.ContextMenu>()?.takeIf {
            it.entries == null || it.entries.isNotEmpty()
        } ?: return false
        if (parentMenuId != null) {
            _currentOpenContextMenu.update { current ->
                val parent = current?.find(parentMenuId)
                if (parent == null) {
                    log.e("Tried to open context menu on unexpected parent $parentMenuId via $focusId, opening as orphan")
                }
                ContextMenuFocus(
                    focusId = focusId,
                    menuId = menuId,
                    parentMenu = parent,
                )
            }
        } else {
            _currentOpenContextMenu.value = ContextMenuFocus(focusId, menuId, null)
        }
        return true
    }

    fun executeCommandFromIpc(
        command: String,
        asyncCallback: ActionResultCallback? = null,
    ) = executeCommand(IpcCommand(command), asyncCallback)

    private fun executeCommand(
        command: CommandHolder,
        asyncCallback: ActionResultCallback? = null,
    ): ActionResult {
        val actionableSnapshot = (command as? KeyboardActionMode.Command)?.actionableSnapshot
        val focused = when (command) {
            is KeyboardActionMode.Command -> command.focused?.let { focusableTargets[it] }
            is IpcCommand -> focusableTargets.values.find { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }
        }
        val actionHandlers = focused?.let { getCurrentKeyActionHandlers(it) }
            ?: actionableSnapshot?.actionHandlers
            ?: getCurrentKeyActionHandlers(null)
        val commandParser = CommandParser(actionHandlers)
        val (mainCommand, args) = commandParser.parseCommandString(command.command) ?: run {
            log.i("Ignoring empty command")
            return ActionResult.Inapplicable
        }
        val possibleActions = commandParser.getPossibleActions(mainCommand)
        val possibleUniqueActions = possibleActions.map {
            it.first
        }.distinct()
        val possibleUniqueActionsWithArgsChecked = possibleUniqueActions.map {
            it to checkArguments(it, args, command.impliedArguments)
        }
        val possibleUniqueActionsWithValidArgs = possibleUniqueActionsWithArgsChecked.mapNotNull { (action, error) ->
            action.takeIf { error == null }
        }
        return when (possibleUniqueActionsWithValidArgs.size) {
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
                ActionResult.NoMatch
            }
            1 -> {
                val action = possibleUniqueActionsWithValidArgs.first()
                val normalizedArgs = commandParser.normalizeArgs(args, action.args)
                val context = getInternalActionContext(
                    focused,
                    keybindingConfig = null,
                    criticalActionRequiresConfirmation = false,
                    currentDestinationType = focused?.destinationStateHolder?.state?.value?.destination?.destinationId
                        ?: actionableSnapshot?.destinationType,
                    destinationStateHolder = focused?.destinationStateHolder
                        ?: actionableSnapshot?.destinationStateHolder,
                    implicitArgs = command.impliedArguments,
                    asyncCallback = asyncCallback,
                )
                val result = try {
                    ActionResult.chain(
                        *possibleActions.filter { it.first == action }.map {{
                            it.second.handleActionOrInapplicable(context, it.first, normalizedArgs)
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
                    is ActionResult.NoOp -> publishMessage(
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
                    is ActionResult.Ambiguous -> publishMessage(
                        AppMessage(
                            StringResourceHolder(Res.string.command_ambiguous, mainCommand.toStringHolder()),
                            isError = true,
                            uniqueId = COMMAND_MESSAGE_ID,
                        )
                    )
                }
                result
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
                ActionResult.Ambiguous
            }
        }
    }

    private fun handleCommandInput(
        query: TextFieldValue,
        setCommandFocus: FocusTarget? = null,
        handleSuccess: (KeyboardActionMode.Command) -> Unit,
    ): Boolean {
        var success: KeyboardActionMode.Command? = null
        updateMode { mode ->
            if (mode is KeyboardActionMode.Command && (setCommandFocus == null || setCommandFocus.id == mode.focused)) {
                mode.copy(query = query)
            } else {
                val focusTarget = setCommandFocus
                    ?: currentFocus.value?.let { focusableTargets[it] }
                    ?: focusableTargets.values.find { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }
                val actionHandlers = getCurrentKeyActionHandlers(focusTarget)
                val destinationStateHolder = focusTarget?.destinationStateHolder
                KeyboardActionMode.Command(
                    query = query,
                    focused = focusTarget?.id,
                    suggestionsProvider = CommandSuggestionsProvider(
                        queryFlow = _mode.map { it as? KeyboardActionMode.Command },
                        scope = scope.childScope(Dispatchers.IO, "commandSuggestions"),
                        commandParser = CommandParser(actionHandlers),
                        userIdSuggestionsProvider = focusTarget?.actions?.userIdSuggestionsProvider,
                        roomContextSuggestionsProvider = focusTarget?.actions?.roomContextSuggestionsProvider,
                    ),
                    selectedSuggestion = null,
                    impliedArguments = actionHandlers.flatMap { it.impliedArguments() }.distinct(),
                    actionableSnapshot = ActionableSnapshot(
                        actionHandlers = actionHandlers,
                        destinationStateHolder = destinationStateHolder,
                        destinationType = destinationStateHolder?.state?.value?.destination?.destinationId,
                    ),
                    forSearch = mode.asSearchMode(),
                )
            }.also {
                success = it
            }
        }
        success?.let(handleSuccess)
        return success != null
    }

    private fun withCriticalActionConfirmation(
        context: InternalActionContext?,
        prompt: ComposableStringHolder,
        confirmText: ComposableStringHolder,
        onDismiss: () -> Unit = {},
        action: () -> ActionResult
    ): ActionResult {
        return if (context?.criticalActionRequiresConfirmation != false) {
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
            ActionResult.Success(notifySuccess = false)
        } else {
            action()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun copyToClipboard(context: ActionContext, content: String, description: ComposableStringHolder?): ActionResult {
        val localClipboard = clipboard ?: return ActionResult.Failure("No clipboard found")
        context.launchActionAsync("copyToClipboard", scope) {
            localClipboard.setClipEntry(
                platformTextClipEntry(content)
            )
            publishMessage(
                AppMessage(
                    if (description == null) {
                        StringResourceHolder(Res.string.command_copied_content_to_clipboard, content.toStringHolder())
                    } else {
                        StringResourceHolder(Res.string.command_copied_to_clipboard, description)
                    },
                    uniqueId = "clipboard",
                )
            )
            ActionResult.Success()
        }
        return ActionResult.Success()
    }

    fun viewInExternalApp(context: ActionContext, content: String, fileExtension: String = ".txt"): ActionResult {
        return context.launchActionAsync(
            "viewInExternalApp",
            scope,
            Dispatchers.IO,
        ) {
            try {
                val tempFile = ExternalViewCache.createFile(fileExtension)
                Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8)
                val result = platformOpenFile(tempFile, MimeUtil.detectMimeType(tempFile))
                if (result is ActionResult.Success) {
                    publishMessage(
                        AppMessage(
                            Res.string.command_external_application_launched.toStringHolder(),
                            uniqueId = "external-app",
                        )
                    )
                }
                result
            } catch (t: Throwable) {
                log.e("Failed to open plaintext content", t)
                ActionResult.Failure(t.message ?: "Failed to open external viewer")
            }
        }
    }

    fun getStringFromClipboard(): String? {
        val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = systemClipboard.getContents(null) ?: return null

        return if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            contents.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
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

    fun openLink(uri: String): ActionResult {
        if (uri.startsWith("matrix:")) {
            MatrixLinkPatterns.parseMatrixLink(uri)?.let {
                consumeLink(it)
                return ActionResult.Success()
            }
        }
        platformOpenUri?.let { openUri ->
            val context = getInternalActionContext(currentFocused(), criticalActionRequiresConfirmation = true)
            return context.launchActionAsync("openLink", scope) {
                runCatching { openUri(uri) }
                    .onFailure { log.w("Failed to open URL in external browser via platform", it) }
                    .toActionResult()
            }
        }
        return try {
            val localUriHandler = uriHandler ?: return ActionResult.Failure("No uri handler found")
            localUriHandler.openUri(uri)
            ActionResult.Success()
        } catch (e: Exception) {
            log.w("Failed to open URL in external browser via JVM", e)
            return ActionResult.Failure(e.message ?: e.toString())
        }
    }

    private fun FocusTarget?.findAllInParentHierarchy(condition: (FocusTarget) -> Boolean): List<FocusTarget> {
        var current = this
        return buildList {
            while (current != null) {
                if (condition(current)) {
                    add(current)
                }
                current = current.parent?.uuid?.let { focusableTargets[it] }
            }
        }
    }

    private fun FocusTarget?.findFirstInParentHierarchy(condition: (FocusTarget) -> Boolean): FocusTarget? {
        var current = this
        while (current != null) {
            if (condition(current)) {
                return current
            }
            current = current.parent?.uuid?.let { focusableTargets[it] }
        }
        return null
    }

    private fun findAllChildren(parentId: Uuid, condition: (FocusTarget) -> Boolean = { true }): List<FocusTarget> {
        return focusableTargets.values.filter { focusable ->
            focusable.findFirstInParentHierarchy {
                it.parent?.uuid == parentId && condition(it)
            } != null
        }
    }

    private fun FocusTarget.destination() = findFirstInParentHierarchy { it.role == FocusRole.DESTINATION_ROOT_CONTAINER }

    fun onWindowFocusChanged(isFocused: Boolean) {
        _isWindowFocused.value = isFocused
    }

    private fun ContextMenuFocus.resolveMenuEntries(target: FocusTarget?): List<ContextMenuEntry>? {
        target ?: return null
        val action = target.actions?.findInteractionAction<InteractionAction.ContextMenu>() ?: return null
        if (parentMenu == null) {
            return action.entries
        }
        val resolution = buildList {
            var current = this@resolveMenuEntries
            while (true) {
                add(current.menuId)
                current = current.parentMenu ?: break
            }
        }.toMutableList()
        if (resolution.last() != action.menuId) {
            log.e { "Unexpected menu ID: ${action.menuId} via ${resolution.joinToString()}" }
            // Unexpected root menu ID
            return null
        }
        resolution.removeLastOrNull()
        var entries = action.entries ?: return null
        while (true) {
            val menuId = resolution.removeLastOrNull() ?: run {
                log.e { "Failed to look up submenu in iteration" }
                return null
            }
            entries = entries.mapNotNull {
                it as? ContextMenuSubmenuEntry
            }.find {
                it.submenuId == menuId
            }?.submenu ?: return run {
                log.e { "Failed to look up submenu $menuId via ${this.menuId}, remaining=${resolution.size}; ${entries.size}" }
                null
            }
            if (menuId == this.menuId) {
                return entries
            }
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

/**
 * Try all possible bindings for a given key until the first one returns true.
 */
fun <A: Action>List<Binding<A>>.execute(
    context: ActionContext,
    key: KeyTrigger,
    block: (ActionContext, action: A, args: List<String>) -> ActionResult
): ActionResult {
    val actions = filter {
        it.trigger == key && (
                it.destinations.isEmpty() && it.notDestinations.none { context.currentDestinationType?.matches(it) == true } ||
                        it.destinations.any { context.currentDestinationType?.matches(it) == true}
                )
    }
    var hasChainableSuccess = false
    actions.forEach { action ->
        val actionResult = try {
            action.checkArguments(context.implicitArgs)?.also {
                Logger.e(it.message)
            } ?: block(context, action.action, action.args).withChainSetting(action.chain)
        } catch (e: IndexOutOfBoundsException) {
            Logger.e("Error executing action", e)
            ActionResult.Failure(e.message ?: "Exception occurred trying to execute action")
        } catch (e: ActionValidationException) {
            Logger.e("Error executing action", e)
            ActionResult.Failure(e.message ?: "Exception occurred trying to execute action")
        }
        Logger.i("Action binding $action yielded $actionResult")
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
): ActionResult.InvalidCommand? {
    return when (argDef) {
        is ActionArgumentAnyOf -> {
            if (argDef.arguments.any {
                checkArgument(actionName, it, argVal, context, lookahead, validSessionIds) == null
            }) {
                null
            } else {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected ${argDef.name}; got $argVal"
                )
            }
        }
        is ActionArgumentOptional -> {
            checkArgument(actionName, argDef.argument, argVal, context, lookahead, validSessionIds)
        }
        is ActionArgumentRepeatable -> {
            checkArgument(actionName, argDef.argument, argVal, context, lookahead, validSessionIds)
        }
        is ActionArgumentContextBased -> {
            checkArgument(actionName, argDef.getFor(context), argVal, context, lookahead, validSessionIds)
        }
        ActionArgumentPrimitive.Reason,
        ActionArgumentPrimitive.EventType,
        ActionArgumentPrimitive.StateEventType,
        ActionArgumentPrimitive.NonEmptyStateKey,
        ActionArgumentPrimitive.AccountDataType,
        ActionArgumentPrimitive.RoomAccountDataType,
        ActionArgumentPrimitive.UserName,
        ActionArgumentPrimitive.RoomName,
        ActionArgumentPrimitive.RoomTopic,
        ActionArgumentPrimitive.ServerName,
        ActionArgumentPrimitive.SpaceOrder,
        ActionArgumentPrimitive.Text,
        ActionArgumentPrimitive.Ignored -> null
        ActionArgumentPrimitive.MatrixLink -> {
            try {
                val link = com.beeper.android.messageformat.MatrixPatterns.parseMatrixUri(argVal, true)
                if (link == null) {
                    ActionResult.Malformed("Invalid matrix URI")
                } else {
                    null
                }
            } catch (e: Exception) {
                ActionResult.Malformed("Invalid matrix URI: $e")
            }
        }
        ActionArgumentPrimitive.MatrixToLink -> {
            try {
                val link = com.beeper.android.messageformat.MatrixPatterns.parseMatrixToUrl(argVal, true)
                if (link == null) {
                    ActionResult.Malformed("Invalid matrix.to URL")
                } else {
                    null
                }
            } catch (e: Exception) {
                ActionResult.Malformed("Invalid matrix.to URL: $e")
            }
        }
        ActionArgumentPrimitive.SchildiChatLegacyLink -> {
            try {
                val link = MatrixLinkPatterns.parseSchildiChatLegacyLink(argVal)
                if (link == null) {
                    ActionResult.Malformed("Invalid schildichat URI")
                } else {
                    null
                }
            } catch (e: Exception) {
                ActionResult.Malformed("Invalid schildichat URI: $e")
            }
        }
        ActionArgumentPrimitive.OAuthCallbackPath -> {
            if (argVal.startsWith("/")) {
                null
            } else {
                ActionResult.Malformed("Invalid oauth callback path")
            }
        }
        ActionArgumentPrimitive.Json -> {
            try {
                Json.parseToJsonElement(argVal)
                null
            } catch (e: Exception) {
                ActionResult.Malformed(e.message ?: e.toString())
            }
        }
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
        ActionArgumentPrimitive.PowerLevel,
        ActionArgumentPrimitive.PositiveOffset,
        ActionArgumentPrimitive.Integer -> {
            val parsed = argVal.toIntOrNull()
            if (parsed == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected int got $argVal"
                )
            } else if (argDef == ActionArgumentPrimitive.PositiveOffset && parsed < 0) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected positive offset got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.Index -> {
            val parsed = argVal.toIntOrNull()
            if (parsed == null || parsed < 0) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected non-negative int got $argVal"
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
        ActionArgumentPrimitive.ExistingDmUserId,
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
                if (!MatrixPatterns.isUserId(argVal)) {
                    ActionResult.Malformed(
                        "Invalid parameter for $actionName, expected MXID got $argVal"
                    )
                } else {
                    null
                }
            }
        }
        ActionArgumentPrimitive.SpaceId,
        ActionArgumentPrimitive.ParentSpaceId,
        ActionArgumentPrimitive.NonParentSpaceId,
        ActionArgumentPrimitive.RoomIdNotJoined,
        ActionArgumentPrimitive.RoomId -> {
            if (!MatrixPatterns.isRoomId(argVal)) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected room ID got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.RoomAliasNotJoined,
        ActionArgumentPrimitive.RoomAlias -> {
            if (!MatrixPatterns.isRoomAlias(argVal)) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected room alias got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.ThreadId,
        ActionArgumentPrimitive.EventId -> {
            if (!MatrixPatterns.isEventId(argVal)) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected room ID got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.Mxc -> {
            if (!argVal.startsWith("mxc://")) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected MXC URL got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SettingKey -> {
            if (argVal !in ScPrefs.validSettingKeys) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid settings key: $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.SettingCategory -> {
            if (argVal !in ScPrefs.validCategoryKeys) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid settings category: $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.DestinationName -> {
            if (argVal !in ALLOWED_DESTINATION_STRINGS) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid destination: $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.NavigatableDestination -> {
            if (argVal.verifyConstructableDestination(lookahead, context) == null) {
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
        ActionArgumentPrimitive.RoomNotificationSetting -> {
            if (ActionRoomNotificationSetting.tryResolve(argVal) == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid room notification setting: $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.PseudoSpaceId -> {
            if (RevengeSpaceListDataSource.isValidPseudoSpaceId(argVal)) {
                null
            } else {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, not a valid pseudo space: $argVal"
                )
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
        ActionArgumentPrimitive.SpaceCatchAllMode -> {
            if (tryOrNull { SpaceCatchAllMode.valueOf(argVal) } == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected valid SpaceCatchAllMode, got $argVal"
                )
            } else {
                null
            }
        }
        ActionArgumentPrimitive.FocusRole -> {
            if (tryOrNull { FocusRole.valueOf(argVal) } == null) {
                ActionResult.Malformed(
                    "Invalid parameter for $actionName, expected valid FocusRole, got $argVal"
                )
            } else {
                null
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
    implicitArgs: CommandArgContext,
    checkIncompleteParameters: Boolean = false,
    validSessionIds: List<String>? = UiState.currentValidSessionIds.value,
) = checkArguments(
    action,
    args,
    implicitArgs,
    checkIncompleteParameters,
    validSessionIds,
)

fun checkArguments(
    action: Action,
    args: List<String>,
    implicitArgs: CommandArgContext,
    checkIncompleteParameters: Boolean = false,
    validSessionIds: List<String>? = UiState.currentValidSessionIds.value,
): ActionResult.InvalidCommand? {
    val actionName = action.name
    val minArgSize = action.minArgsSize()
    val maxArgSize = action.maxArgsSize()
    if (args.size !in minArgSize..maxArgSize) {
        val message = if (minArgSize != args.size) {
            "Invalid parameter size for $actionName, expected between $minArgSize and ${action.args.size}, got ${args.size}"
        } else {
            "Invalid parameter size for $actionName, expected ${action.args.size} got ${args.size}"
        }
        return if (args.size < minArgSize) {
            if (checkIncompleteParameters) {
                action.args.zip(args).forEachIndexed { index, (argDef, argVal) ->
                    val lookahead = args.subList(index + 1, args.size)
                    val context = action.args.zip(args.subList(0, index)) + implicitArgs
                    checkArgument(action.name, argDef, argVal, context, lookahead, validSessionIds)?.let {
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
        val context = action.args.zip(args.subList(0, index)) + implicitArgs
        checkArgument(action.name, argDef, argVal, context, lookahead, validSessionIds)?.let {
            return it
        }
    }
    return null
}

interface ActionContext {
    val windowId: WindowId
    fun publishMessage(message: AbstractAppMessage)
    fun dismissMessage(uniqueId: String)
    fun copyToClipboard(content: String, description: ComposableStringHolder? = null): ActionResult
    fun viewInExternalApp(content: String, fileExtension: String = ".txt"): ActionResult
    fun getFilesFromClipboard(): List<File>
    fun getStringFromClipboard(): String?
    fun openLink(uri: String): ActionResult
    fun focusByRole(role: FocusRole): Boolean
    fun focusByRoleUnlessAlreadyFocused(role: FocusRole): ActionResult
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
    suspend fun onAsyncActionResult(result: ActionResult)
    val currentDestinationType: DestinationEnum?
    val destinationStateHolder: DestinationStateHolder?
    val keybindingConfig: KeybindingConfig?
    val implicitArgs: CommandArgContext
}

inline fun ActionContext.runWithMessage(
    messageId: String,
    start: ComposableStringHolder,
    end: (ActionResult) -> ComposableStringHolder,
    block: () -> ActionResult,
): ActionResult {
    publishMessage(
        AppMessage(
            message = start,
            uniqueId = messageId,
            autoDismissDuration = null,
        )
    )
    var result: ActionResult? = null
    try {
        result = block()
        return result
    } finally {
        publishMessage(
            AppMessage(
                message = end(result ?: ActionResult.Failure("Unexpected exit")),
                uniqueId = messageId,
                isError = result !is ActionResult.Success,
            )
        )
    }
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
                    autoDismissDuration = DEFAULT_MESSAGE_EXPIRY_DURATION.takeIf { appMessageId == null },
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
                    uniqueId = appMessageId
                )
            )
        } else if (notifyProcessing && appMessageId != null) {
            dismissMessage(appMessageId)
        }
        onAsyncActionResult(result)
    }
    return ActionResult.Success(async = true)
}


@Composable
fun currentActionContext(): ActionContext {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationStateHolder = LocalDestinationState.current
    val destination = destinationStateHolder?.state?.collectAsState()?.value?.destination
    return remember(keyHandler, destinationStateHolder, destination) {
        keyHandler.getActionContext(destination, destinationStateHolder)
    }
}

private fun KeyboardActionMode.asSearchMode() = when (this) {
    is KeyboardActionMode.Search -> this
    is KeyboardActionMode.Command -> forSearch
    else -> null
}

private fun KeyboardActionMode.impliedFocus() = when (this) {
    is KeyboardActionMode.Command -> this.focused
    else -> null
}
