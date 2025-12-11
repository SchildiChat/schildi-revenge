package chat.schildi.preferences

import androidx.datastore.preferences.core.Preferences

sealed interface ScPrefDependency {
    fun fulfilledFor(preferences: Preferences): Boolean
}

data class ScPrefEnabledDependency(
    val pref: ScPref<Boolean>,
    val expect: Boolean = true,
) : ScPrefDependency {
    private fun fulfilledFor(value: Boolean): Boolean = value == expect
    override fun fulfilledFor(preferences: Preferences): Boolean = fulfilledFor(preferences[pref.key!!] ?: pref.defaultValue)
}

data class ScPrefFulfilledForAnyDependency(
    val dependencies: List<ScPrefDependency>,
) : ScPrefDependency {
    override fun fulfilledFor(preferences: Preferences): Boolean = dependencies.any { it.fulfilledFor(preferences) }
}

data class ScPrefNotDependency(
    val dependency: ScPrefDependency,
) : ScPrefDependency {
    override fun fulfilledFor(preferences: Preferences): Boolean = !dependency.fulfilledFor(preferences)
}

fun ScPref<Boolean>.toDependency(expect: Boolean = true): ScPrefDependency = ScPrefEnabledDependency(this, expect)
fun ScPref<Boolean>.asDependencies(expect: Boolean = true): List<ScPrefDependency> = listOf(toDependency(expect))
fun ScPrefDependency.not() = ScPrefNotDependency(this)
