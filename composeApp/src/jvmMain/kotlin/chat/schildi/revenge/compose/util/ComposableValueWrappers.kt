package chat.schildi.revenge.compose.util

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed interface ComposableStringHolder {
    @Composable
    fun render(): String
}

data class HardcodedStringHolder(
    val value: String,
) : ComposableStringHolder {
    @Composable
    override fun render() = value
}

data class StringResourceHolder(
    val res: StringResource,
    val formatArgs: ImmutableList<ComposableStringHolder> = persistentListOf(),
) : ComposableStringHolder {
    constructor(res: StringResource, vararg formatArgs: ComposableStringHolder) : this(res, formatArgs.toPersistentList())
    @Composable
    override fun render() = stringResource(res, *formatArgs.map { it.render() }.toTypedArray())
}

fun String.toStringHolder() = HardcodedStringHolder(this)
fun StringResource.toStringHolder() = StringResourceHolder(this)
