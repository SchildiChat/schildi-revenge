package chat.schildi.revenge.compose.components

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class ComposableStringLookupRequest(
    val strings: ImmutableList<StringResource>,
)

data class ComposableStringLookupTable(
    val stringLookup: PersistentMap<StringResource, String>
)

@Composable
fun ComposableStringLookupRequest.lookup(): ComposableStringLookupTable {
    return ComposableStringLookupTable(
        stringLookup = strings.associateWith { stringResource(it) }.toPersistentHashMap()
    )
}
