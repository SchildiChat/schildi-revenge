package chat.schildi.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.publishError
import chat.schildi.revenge.compose.util.ComposableStringHolder
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

private const val MESSAGE_ID = "setSetting"

interface ScPreferencesStore {
    suspend fun <T>setSetting(scPref: ScPref<T>, value: T)
    suspend fun <T>setSettingTypesafe(scPref: ScPref<T>, value: Any?)
    fun <T> settingFlow(scPref: ScPref<T>): Flow<T>
    fun <T> combinedSettingValueAndEnabledFlow(transform: ((ScPref<*>) -> Any?, (ScPref<*>) -> Boolean) -> T): Flow<T>
    fun isEnabledFlow(scPref: AbstractScPref): Flow<Boolean>
    fun <T>getCachedOrDefaultValue(scPref: ScPref<T>): T
    suspend fun reset()
    suspend fun prefetch()
    suspend fun handleSetAction(context: ActionContext?, args: List<String>): Boolean
    suspend fun handleToggleAction(context: ActionContext?, args: List<String>): Boolean

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

    override suspend fun handleSetAction(context: ActionContext?, args: List<String>): Boolean {
        if (args.size != 2) {
            context.publishError(log, MESSAGE_ID, "Invalid parameter size for SetSetting action, expected 2 got ${args.size}")
            return false
        }
        val (sKey, value) = args
        val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
        if (pref == null) {
            context.publishError(log, MESSAGE_ID, "Did not find preference for SetSetting action with key \"$sKey\"")
            return false
        }
        return handleSetActionTypesafe(context, pref, value)
    }

    private suspend fun <T>handleSetActionTypesafe(context: ActionContext?, pref: ScPref<T>, value: String): Boolean {
        val valueToSet = pref.parseType(value)
        if (valueToSet == null) {
            context.publishError(log, MESSAGE_ID, "Invalid value for SetSetting action with key \"${pref.sKey}\", value \"$value\"")
            return false
        }
        log.d("Setting setting \"${pref.sKey}\" to $value")
        setSetting(pref, valueToSet)
        context?.publishMessage(
            AppMessage(
                message = StringResourceHolder(Res.string.command_setting_set_to, pref.titleRes.toStringHolder(), valueToSet.toString().toStringHolder()),
                uniqueId = MESSAGE_ID,
            )
        )
        return true
    }

    override suspend fun handleToggleAction(context: ActionContext?, args: List<String>): Boolean {
        if (args.size != 1) {
            context.publishError(log, MESSAGE_ID, "Invalid parameter size for ToggleSetting action, expected 1 got ${args.size}")
            return false
        }
        val (sKey) = args
        val pref = ScPrefs.rootPrefs.findPreference { it.sKey == sKey }
        if (pref == null) {
            context.publishError(log, MESSAGE_ID, "Did not find preference for ToggleSetting action with key \"$sKey\"")
            return false
        }
        if (pref !is ScBoolPref) {
            context.publishError(log, MESSAGE_ID, "Tried action ToggleSetting for unsupported preference \"$sKey\"")
            return false
        }
        val toggledValue = !getSetting(pref)
        log.d("Toggling setting \"$sKey\" to $toggledValue")
        setSetting(pref, toggledValue)
        context?.publishMessage(
            AppMessage(
                message = StringResourceHolder(Res.string.command_setting_set_to, pref.titleRes.toStringHolder(), toggledValue.toString().toStringHolder()),
                uniqueId = MESSAGE_ID,
            )
        )
        return true
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
        is ScDisclaimerPref,
        is ScActionablePref -> emptyList()
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

@Composable
fun ScColorPref.userColor(): Color? = LocalScPreferencesStore.current.settingState(this).value.let { ScColorPref.valueToColor(it) }

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
