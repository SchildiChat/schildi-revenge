package chat.schildi.revenge.util

/**
 * Assumes that all inputs are already sorted by sortKey(), to achieve a merged sorted list in O(N).
 */
inline fun <T, R : Comparable<R>, K>mergeLists(
    vararg input: List<T>,
    key: (T) -> K,
    sortKey: (T) -> R,
    descending: Boolean = true,
    mergeWithDuplicates: (T, List<T>) -> T = { it, _ -> it },
    deduplicateIndividualInputs: Boolean = false,
): List<T> = mergeLists(
    *input,
    key = key,
    chooseNext = { nextUp ->
        if (descending) nextUp.maxBy(sortKey) else nextUp.minBy(sortKey)
    },
    mergeWithDuplicates = mergeWithDuplicates,
    deduplicateIndividualInputs = deduplicateIndividualInputs,
)

/**
 * Assumes that all inputs are already sorted by comparator, to achieve a merged sorted list in O(N).
 */
inline fun <T, K>mergeLists(
    vararg input: List<T>,
    key: (T) -> K,
    comparator: Comparator<in T>,
    descending: Boolean = false,
    mergeWithDuplicates: (T, List<T>) -> T = { it, _ -> it },
    deduplicateIndividualInputs: Boolean = false,
    limit: Int = Integer.MAX_VALUE,
): List<T> = mergeLists(
    *input,
    key = key,
    chooseNext = { nextUp ->
        if (descending) nextUp.maxWith(comparator) else nextUp.minWith(comparator)
    },
    mergeWithDuplicates = mergeWithDuplicates,
    deduplicateIndividualInputs = deduplicateIndividualInputs,
    limit = limit,
)

inline fun <T, K>mergeLists(
    vararg inputs: List<T>,
    key: (T) -> K,
    chooseNext: (List<T>) -> T,
    mergeWithDuplicates: (T, List<T>) -> T = { it, _ -> it },
    deduplicateIndividualInputs: Boolean = false,
    limit: Int = Integer.MAX_VALUE,
): List<T> {
    // We know the max size of this array, so use for initialization to avoid unnecessary grow operations
    val result = ArrayList<T>(inputs.sumOf { it.size })

    val pending = inputs.map { it.toMutableList() }

    while (true) {
        val nextUp = pending.mapNotNull { it.getOrNull(0) }
        if (nextUp.isEmpty() || result.size >= limit) {
            break
        } else if (nextUp.size == 1 && !deduplicateIndividualInputs) {
            // Only one of the remaining lists is non-empty
            pending.forEach {
                result.addAll(it)
            }
            break
        }
        val toInsert = chooseNext(nextUp)
        val toRemove = key(toInsert)
        val duplicates = mutableListOf<T>()
        pending.forEach {
            // Note: this is where we de-duplicate values that are in each of the inputs.
            // That's why we re-lookup in every list rather than remembering whe list we picked
            // toInsert from in the first place.
            // Do a while rather than an if just in case any input list had duplicated already.
            while (it.getOrNull(0)?.let { key(it) } == toRemove) {
                duplicates.add(it.removeAt(0))
            }
        }
        if (duplicates.size > 1) {
            result.add(mergeWithDuplicates(toInsert, duplicates))
        } else {
            result.add(toInsert)
        }
    }

    return result
}
