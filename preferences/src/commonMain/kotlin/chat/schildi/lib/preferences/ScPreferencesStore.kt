package chat.schildi.lib.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface ScPreferencesStore {
    suspend fun <T>setSetting(scPref: ScPref<T>, value: T)
    suspend fun <T>setSettingTypesafe(scPref: ScPref<T>, value: Any?)
    fun <T> settingFlow(scPref: ScPref<T>): Flow<T>
    fun <T> combinedSettingValueAndEnabledFlow(transform: ((ScPref<*>) -> Any?, (ScPref<*>) -> Boolean) -> T): Flow<T>
    fun isEnabledFlow(scPref: AbstractScPref): Flow<Boolean>
    fun <T>getCachedOrDefaultValue(scPref: ScPref<T>): T
    suspend fun reset()
    suspend fun prefetch()

    fun <T> combinedSettingFlow(transform: ((ScPref<*>) -> Any?) -> T): Flow<T> = combinedSettingValueAndEnabledFlow { getPref, _ ->
        transform(getPref)
    }

    suspend fun <T>getSetting(scPref: ScPref<T>): T = settingFlow(scPref).first()
}

fun <T>ScPref<T>.safeLookup(getPref: (ScPref<*>) -> Any?): T {
    return ensureType(getPref(this)) ?: defaultValue
}

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
        is ScViewOnlyPref -> emptyList()
    }
}

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

fun ScPrefContainer.findPreferenceContainer(condition: (ScPrefContainer) -> Boolean): ScPrefContainer? {
    prefs.forEach { pref ->
        if (pref is ScPrefContainer) {
            if (condition(pref)) {
                return pref
            }
            pref.findPreferenceContainer(condition)?.let { return it }
        }
    }
    return null
}
