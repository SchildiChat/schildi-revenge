package chat.schildi.preferences

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

sealed interface AbstractScPref {
    val titleRes: StringResource

    val summaryRes: StringResource?

    val dependencies: List<ScPrefDependency>
}

sealed interface ScPref<T> : AbstractScPref {
    val sKey: String
    val defaultValue: T
    // Value when the preference is disabled via dependencies. If null, return the actual value anyway. Subclasses should default this to defaultValue.
    val disabledValue: T?

    val key: Preferences.Key<T>?

    fun ensureType(value: Any?): T?
    fun parseType(value: String): T?
}

sealed interface ScPrefContainer : AbstractScPref {
    val prefs: List<AbstractScPref>
}

data class ScPrefScreen(
    override val titleRes: StringResource,
    override val summaryRes: StringResource?,
    override val prefs: List<AbstractScPref>,
    override val dependencies: List<ScPrefDependency> = emptyList(),
) : ScPrefContainer

data class ScPrefCategory(
    override val titleRes: StringResource,
    override val summaryRes: StringResource?,
    override val prefs: List<AbstractScPref>,
    override val dependencies: List<ScPrefDependency> = emptyList(),
) : ScPrefContainer

data class ScPrefCategoryCollapsed(
    override val sKey: String,
    override val titleRes: StringResource,
    override val defaultValue: Boolean = false,
    override val summaryRes: StringResource? = null,
    override val disabledValue: Boolean = defaultValue,
    override val prefs: List<AbstractScPref>,
    override val dependencies: List<ScPrefDependency> = emptyList(),
) : ScPrefContainer, ScPref<Boolean> {
    override val key = booleanPreferencesKey(sKey)
    override fun ensureType(value: Any?): Boolean? {
        if (value !is Boolean?) {
            Logger.withTag("ScPrefCategory").e("Parse boolean failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): Boolean? = value.toBooleanStrictOrNull()
}

data class ScPrefCollection(
    override val titleRes: StringResource,
    override val prefs: List<AbstractScPref>,
    override val dependencies: List<ScPrefDependency> = emptyList(),
) : ScPrefContainer {
    override val summaryRes: StringResource? = null
}

data class ScBoolPref(
    override val sKey: String,
    override val defaultValue: Boolean,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val disabledValue: Boolean? = defaultValue,
    override val dependencies: List<ScPrefDependency> = emptyList(),
): ScPref<Boolean> {
    override val key = booleanPreferencesKey(sKey)
    override fun ensureType(value: Any?): Boolean? {
        if (value !is Boolean?) {
            Logger.withTag("ScBoolPref").e("Parse boolean failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): Boolean? = value.toBooleanStrictOrNull()
}

data class ScIntPref(
    override val sKey: String,
    override val defaultValue: Int,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val disabledValue: Int? = defaultValue,
    override val dependencies: List<ScPrefDependency> = emptyList(),
    val minValue: Int = Int.MIN_VALUE,
    val maxValue: Int = Int.MAX_VALUE,
): ScPref<Int> {
    override val key = intPreferencesKey(sKey)
    override fun ensureType(value: Any?): Int? {
        if (value !is Int?) {
            Logger.withTag("ScIntPref").e("Parse int failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): Int? = value.toIntOrNull()
}

data class ScFloatPref(
    override val sKey: String,
    override val defaultValue: Float,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val disabledValue: Float? = defaultValue,
    override val dependencies: List<ScPrefDependency> = emptyList(),
    val minValue: Float = Float.MIN_VALUE,
    val maxValue: Float = Float.MAX_VALUE,
): ScPref<Float> {
    override val key = floatPreferencesKey(sKey)
    override fun ensureType(value: Any?): Float? {
        if (value !is Float?) {
            Logger.withTag("ScIntPref").e("Parse float failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): Float? = value.toFloatOrNull()
}

sealed interface ScListPref<T>: ScPref<T> {
    val itemKeys: Array<T>
    val itemNames: StringArrayResource
    val itemSummaries: StringArrayResource?
}

data class ScStringListPref(
    override val sKey: String,
    override val defaultValue: String,
    override val itemKeys: Array<String>,
    override val itemNames: StringArrayResource,
    override val itemSummaries: StringArrayResource?,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val disabledValue: String = defaultValue,
    override val dependencies: List<ScPrefDependency> = emptyList(),
): ScListPref<String> {
    override val key = stringPreferencesKey(sKey)
    override fun ensureType(value: Any?): String? {
        if (value !is String?) {
            Logger.withTag("ScStringListPref").e("Parse string failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): String? = value.takeIf { it in itemKeys }
}

data class ScColorPref(
    override val sKey: String,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val defaultValue: Int = FOLLOW_THEME_VALUE,
    override val disabledValue: Int = defaultValue,
    override val dependencies: List<ScPrefDependency> = emptyList(),
): ScPref<Int> {
    override val key = intPreferencesKey(sKey)
    override fun ensureType(value: Any?): Int? {
        if (value !is Int) {
            Logger.withTag("ScColorPref").e("Parse Int failed of $sKey for ${value?.javaClass?.simpleName}")
            return null
        }
        return value
    }
    override fun parseType(value: String): Int? = value.toIntOrNull()

    companion object {
        // Fully transparent #000001 (less likely to be set by user then #000000)
        const val FOLLOW_THEME_VALUE = 0x00000001

        fun valueToColor(value: Int) = value.takeIf { it != FOLLOW_THEME_VALUE }?.let { Color(it) }
    }
}

data class ScActionablePref(
    val key: String,
    override val titleRes: StringResource,
    override val summaryRes: StringResource? = null,
    override val dependencies: List<ScPrefDependency> = emptyList(),
) : AbstractScPref

data class ScDisclaimerPref(
    val key: String,
    override val titleRes: StringResource,
) : AbstractScPref {
    override val summaryRes = null
    override val dependencies = emptyList<ScPrefDependency>()
}
