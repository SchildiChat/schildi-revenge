package chat.schildi.lib.preferences

// TODO
object ScPreferencesStore {
    fun <T>getCachedOrDefaultValue(pref: ScPref<T>) = pref.defaultValue
}
