package chat.schildi.lib.preferences

data class ScPrefFilter(
    // Condition for normal preferences to have fulfilled.
    val predicate: suspend (ScPref<*>) -> Boolean = { true },
    // Condition for containers before evaluating children. If true, all children will be included no matter what their
    // individual filter results would be.
    val prePredicate: suspend (ScPrefContainer) -> Boolean = { false },
    // Condition for containers to evaluate after having their children filtered, if we still want to have
    // the container included anyway. Usually we can just drop empty containers.
    val postPredicate: suspend (ScPrefContainer) -> Boolean = { it.prefs.isNotEmpty() },
    // Whether to allow view-only prefs
    val viewOnlyPredicate: suspend (ScViewOnlyPref) -> Boolean = { true },
)

suspend fun ScPrefContainer.filteredBy(filter: ScPrefFilter): ScPrefContainer {
    return copyWithPrefs(
        prefs = prefs.filteredBy(filter),
    )
}

suspend fun List<AbstractScPref>.filteredBy(filter: ScPrefFilter): List<AbstractScPref> {
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
            is ScViewOnlyPref -> it.takeIf { filter.viewOnlyPredicate(it) }
        }
    }
}
