package chat.schildi.resources

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface ComposableStringHolder {
    @Composable
    fun render(): String
    suspend fun renderSuspend(): String
}

data class HardcodedStringHolder(
    val value: String,
) : ComposableStringHolder {
    @Composable
    override fun render() = value
    override suspend fun renderSuspend() = value
}

data class StringResourceHolder(
    val res: StringResource,
    val formatArgs: ImmutableList<ComposableStringHolder> = persistentListOf(),
) : ComposableStringHolder {
    constructor(res: StringResource, vararg formatArgs: ComposableStringHolder) : this(res, formatArgs.toPersistentList())
    @Composable
    override fun render() = stringResource(res, *formatArgs.map { it.render() }.toTypedArray())
    override suspend fun renderSuspend() = getString(res, *formatArgs.map { it.renderSuspend() }.toTypedArray())
}

data class PluralsResourceHolder(
    val res: PluralStringResource,
    val quantity: Int,
    val formatArgs: ImmutableList<ComposableStringHolder> = persistentListOf(),
) : ComposableStringHolder {
    constructor(res: PluralStringResource, quantity: Int, vararg formatArgs: ComposableStringHolder) : this(res, quantity, formatArgs.toPersistentList())
    @Composable
    override fun render() = pluralStringResource(res, quantity, *formatArgs.map { it.render() }.toTypedArray())
    override suspend fun renderSuspend() = getPluralString(res, quantity, *formatArgs.map { it.renderSuspend() }.toTypedArray())
}

fun String.toStringHolder() = HardcodedStringHolder(this)
fun StringResource.toStringHolder(vararg formatArgs: ComposableStringHolder) = StringResourceHolder(this, formatArgs.toPersistentList())
fun PluralStringResource.toStringHolder(quantity: Int, vararg formatArgs: ComposableStringHolder) = PluralsResourceHolder(this, quantity, formatArgs.toPersistentList())
