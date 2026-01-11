package chat.schildi.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.actions.publishError
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.RevengeDatastoreStorage
import chat.schildi.revenge.config.ScAppDirs
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.command_setting_set_to
import java.io.File
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

const val SETTINGS_MESSAGE_ID = "setSetting"

interface ScPreferencesStore {
    suspend fun <T>setSetting(scPref: ScPref<T>, value: T)
    suspend fun <T>setSettingTypesafe(scPref: ScPref<T>, value: Any?)
    fun <T> settingFlow(scPref: ScPref<T>): Flow<T>
    fun <T> combinedSettingValueAndEnabledFlow(transform: ((ScPref<*>) -> Any?, (ScPref<*>) -> Boolean) -> T): Flow<T>
    fun isEnabledFlow(scPref: AbstractScPref): Flow<Boolean>
    fun <T>getCachedOrDefaultValue(scPref: ScPref<T>): T
    suspend fun reset()
    suspend fun prefetch()
    suspend fun handleSetAction(context: ActionContext?, args: List<String>): ActionResult
    suspend fun handleToggleAction(context: ActionContext?, args: List<String>): ActionResult
    suspend fun handleResetAction(context: ActionContext?, args: List<String>): ActionResult

    fun <T> combinedSettingFlow(transform: ((ScPref<*>) -> Any?) -> T): Flow<T> = combinedSettingValueAndEnabledFlow { getPref, _ ->
        transform(getPref)
    }

    suspend fun <T>getSetting(scPref: ScPref<T>): T = settingFlow(scPref).first()
}

fun <T>ScPref<T>.safeLookup(getPref: (ScPref<*>) -> Any?): T {
    return ensureType(getPref(this)) ?: defaultValue
}

class DefaultScPreferencesStore() : ScPreferencesStore {
    private val log = Logger.withTag("ScPrefStore")

    private val dataDir = File(ScAppDirs.getUserConfigDir()).also {
        it.mkdirs()
    }
    private val storeFile = File(dataDir, "preferences.toml")
    private val store = DataStoreFactory.create(
        RevengeDatastoreStorage(storeFile.path)
    )

    // When listening to the settings flow, we want an appropriate initial value
    private val settingsCache = mutableMapOf<String, Any>()

    override suspend fun <T>setSetting(scPref: ScPref<T>, value: T) {
        val key = scPref.key ?: return
        store.edit { prefs ->
            prefs[key] = value
        }
        cacheSetting(scPref, value)
        if (scPref.requiresWindowRecreation) {
            UiState.recreateUi()
        }
    }

    private suspend fun <T>clearSetting(scPref: ScPref<T>) {
        val key = scPref.key ?: return
        store.edit { prefs ->
            prefs.minusAssign(key)
        }
        cacheSetting(scPref, null)
    }

    override suspend fun <T>setSettingTypesafe(scPref: ScPref<T>, value: Any?) {
        val v = scPref.ensureType(value)
        if (v == null) {
            log.e("Cannot set typesafe setting for ${scPref.key}, $value")
            return
        }
        setSetting(scPref, v)
    }

    override fun <T> settingFlow(scPref: ScPref<T>): Flow<T> {
        val key = scPref.key ?: return emptyFlow()
        return store.data.map { prefs ->
            val disabledValue = scPref.disabledValue
            if (isEnabled(prefs, scPref) || disabledValue == null) {
                prefs[key] ?: scPref.defaultValue
            } else {
                disabledValue
            }
        }
    }

    override fun <T> combinedSettingValueAndEnabledFlow(transform: ((ScPref<*>) -> Any?, (ScPref<*>) -> Boolean) -> T): Flow<T> {
        return store.data.map {  prefs ->
            transform(
                { scPref ->
                    val key = scPref.key ?: return@transform scPref.defaultValue
                    val disabledValue = scPref.disabledValue
                    if (isEnabled(prefs, scPref) || disabledValue == null) {
                        prefs[key] ?: scPref.defaultValue
                    } else {
                        disabledValue
                    }
                },
                { scPref ->
                    isEnabled(prefs, scPref)
                }
            )
        }
    }

    override fun <T>getCachedOrDefaultValue(scPref: ScPref<T>): T {
        return scPref.ensureType(settingsCache[scPref.sKey]) ?: scPref.defaultValue
    }

    override fun isEnabledFlow(scPref: AbstractScPref): Flow<Boolean> {
        return store.data.map { prefs ->
            isEnabled(prefs, scPref)
        }
    }

    private fun isEnabled(prefs: Preferences, scPref: AbstractScPref): Boolean {
        return scPref.dependencies.all {
            it.fulfilledFor(prefs)
        }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
        settingsCache.clear()
    }

    override suspend fun prefetch() {
        store.data.firstOrNull()?.let { data ->
            ScPrefs.rootPrefs.forEachPreference { pref ->
                val key = pref.key ?: return@forEachPreference
                cacheSetting(pref, data[key])
            }
        }
    }

    private fun cacheSetting(scPref: ScPref<*>, value: Any?) {
        val v = scPref.ensureType(value)
        if (v == null) {
            settingsCache.remove(scPref.sKey)
        } else {
            settingsCache[scPref.sKey] = v
        }
    }

    override suspend fun handleSetAction(context: ActionContext?, args: List<String>): ActionResult {
        val sKey = args.firstOrNull().orActionValidationError()
        val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
        if (pref == null) {
            return ActionResult.Failure("Did not find preference for SetSetting action with key \"$sKey\"")
        }
        val value = args.getOrNull(1)
        return if (value == null) {
            if (pref is ScBoolPref) {
                handleSetActionTypesafe(context, pref, "true")
            } else {
                handleSetActionTypesafe(context, pref, pref.defaultValue.toString())
            }
        } else {
            handleSetActionTypesafe(context, pref, value)
        }
    }

    override suspend fun handleResetAction(context: ActionContext?, args: List<String>): ActionResult {
        return restoreSettingDefault(context, args.first())
    }

    suspend fun restoreSettingDefault(context: ActionContext?, sKey: String): ActionResult {
        val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
        if (pref == null) {
            return ActionResult.Failure("Did not find preference for SetSetting action with key \"$sKey\"")
        }
        clearSetting(pref)
        context?.publishMessage(
            AppMessage(
                message = StringResourceHolder(Res.string.command_setting_set_to, pref.titleRes.toStringHolder(), pref.defaultValue.toString().toStringHolder()),
                uniqueId = SETTINGS_MESSAGE_ID,
            )
        )
        return ActionResult.Success()
    }

    private suspend fun <T>handleSetActionTypesafe(context: ActionContext?, pref: ScPref<T>, value: String): ActionResult {
        val valueToSet = pref.parseType(value)
            ?: return ActionResult.Failure("Invalid value for SetSetting action with key \"${pref.sKey}\", value \"$value\"")
        log.d("Setting setting \"${pref.sKey}\" to $value")
        setSetting(pref, valueToSet)
        context?.publishMessage(
            AppMessage(
                message = StringResourceHolder(Res.string.command_setting_set_to, pref.titleRes.toStringHolder(), valueToSet.toString().toStringHolder()),
                uniqueId = SETTINGS_MESSAGE_ID,
            )
        )
        return ActionResult.Success()
    }

    override suspend fun handleToggleAction(context: ActionContext?, args: List<String>): ActionResult {
        if (args.isEmpty()) {
            return ActionResult.Failure("Invalid parameter size for ToggleSetting action, expected at least one")
        }
        val sKey = args.first()
        val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
        if (pref == null) {
            return ActionResult.Failure("Did not find preference for ToggleSetting action with key \"$sKey\"")
        }
        val toggleValues = args.subList(1, args.size).takeIf { it.isNotEmpty() } ?: when (pref) {
            is ScBoolPref -> listOf("true", "false")
            else -> {
                return ActionResult.Failure("Tried action ToggleSetting for non-boolean preference \"$sKey\" without providing toggle values")
            }
        }
        val currentValue = getSetting(pref).toString()
        val nextValueIndex = (toggleValues.indexOf(currentValue) + 1) % toggleValues.size
        val toggledValueString = toggleValues[nextValueIndex]
        val toggledValue = pref.parseType(toggledValueString) ?: run {
            return ActionResult.Failure("Invalid value for \"$sKey\": $toggledValueString")
        }
        log.d("Toggling setting \"$sKey\" to $toggledValue")
        setSettingTypesafe(pref, toggledValue)
        context?.publishMessage(
            AppMessage(
                message = StringResourceHolder(Res.string.command_setting_set_to, pref.titleRes.toStringHolder(), toggledValue.toString().toStringHolder()),
                uniqueId = SETTINGS_MESSAGE_ID,
            )
        )
        return ActionResult.Success()
    }
}

@Composable
fun <T>ScPreferencesStore.settingState(scPref: ScPref<T>, context: CoroutineContext = EmptyCoroutineContext): State<T> = settingFlow(scPref).collectAsState(getCachedOrDefaultValue(scPref), context)

@Composable
fun ScPreferencesStore.enabledState(scPref: AbstractScPref, context: CoroutineContext = EmptyCoroutineContext): State<Boolean> = isEnabledFlow(scPref).collectAsState(true, context)

fun List<AbstractScPref>.collectScPrefs(predicate: (ScPref<*>) -> Boolean = { true }): List<ScPref<*>> = this.flatMap { pref ->
    when (pref) {
        is ScPrefContainer -> pref.prefs.collectScPrefs(predicate).let {
            if (pref is ScPref<*>) {
                it + listOf(pref).filter(predicate)
            } else {
                it
            }
        }
        is ScPref<*> -> listOf(pref).filter(predicate)
        /*
        is ScDisclaimerPref,
        is ScActionablePref -> emptyList()
         */
    }
}

@Composable
fun <R>List<ScPref<*>>.prefValMap(v: @Composable (ScPref<*>) -> R) = associate { it.sKey to v(it) }
@Composable
fun List<ScPref<out Any>>.prefMap() = prefValMap { p -> p }

// Just something that *looks like* a CompositionLocal for compatibility with Next
data class NotExactlyACompositionLocal<T>(val current: T)
val LocalScPreferencesStore = NotExactlyACompositionLocal<ScPreferencesStore>(RevengePrefs)

@Composable
fun <T>ScPref<T>.value(): T = LocalScPreferencesStore.current.settingState(this).value

/*
@Composable
fun ScColorPref.userColor(): Color? = LocalScPreferencesStore.current.settingState(this).value.let { ScColorPref.valueToColor(it) }
*/

@Composable
fun <T>ScPref<T>.state(): State<T> = LocalScPreferencesStore.current.settingState(this)

fun ScPrefContainer.forEachPreference(block: (ScPref<*>) -> Unit) {
    prefs.forEach {
        if (it is ScPrefContainer) {
            it.forEachPreference(block)
        }
        if (it is ScPref<*>) {
            block(it)
        }
    }
}

fun ScPrefContainer.forEachPreferenceOrContainer(block: (AbstractScPref) -> Unit) {
    prefs.forEach {
        block(it)
        if (it is ScPrefContainer) {
            it.forEachPreferenceOrContainer(block)
        }
    }
}

suspend fun ScPrefContainer.forEachPreferenceSuspend(block: suspend (ScPref<*>) -> Unit) {
    prefs.forEach {
        if (it is ScPrefContainer) {
            it.forEachPreferenceSuspend(block)
        }
        if (it is ScPref<*>) {
            block(it)
        }
    }
}

fun ScPrefContainer.findPreference(condition: (ScPref<*>) -> Boolean): ScPref<*>? {
    prefs.forEach {
        if (it is ScPrefContainer) {
            it.findPreference(condition)?.let { return it }
        }
        if (it is ScPref<*>) {
            if (condition(it)) {
                return it
            }
        }
    }
    return null
}

data class ScPrefFilter(
    // Condition for normal preferences to have fulfilled.
    val predicate: (ScPref<*>) -> Boolean = { true },
    // Condition for containers before evaluating children. If true, all children will be included no matter what their
    // individual filter results would be.
    val prePredicate: (ScPrefContainer) -> Boolean = { false },
    // Condition for containers to evaluate after having their children filtered, if we still want to have
    // the container included anyway. Usually we can just drop empty containers.
    val postPredicate: (ScPrefContainer) -> Boolean = { it.prefs.isNotEmpty() },
)

fun ScPrefContainer.filteredBy(filter: ScPrefFilter): ScPrefContainer {
    return copyWithPrefs(
        prefs = prefs.filteredBy(filter),
    )
}

fun List<AbstractScPref>.filteredBy(filter: ScPrefFilter): List<AbstractScPref> {
    // First map, then filter, so we can filter out pref categories based on their filtered contents
    return mapNotNull {
        when (it) {
            is ScPref<*> -> it.takeIf { filter.predicate(it) }
            is ScPrefContainer -> {
                if (filter.prePredicate(it)) {
                    it
                } else {
                    it.copyWithPrefs(it.prefs.filteredBy(filter)).takeIf { filter.postPredicate(it) }
                }
            }
        }
    }
}
