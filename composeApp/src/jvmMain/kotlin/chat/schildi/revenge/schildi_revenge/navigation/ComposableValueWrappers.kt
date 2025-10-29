package chat.schildi.revenge.schildi_revenge.navigation

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
    val formatArgs: ImmutableList<String> = persistentListOf(),
) : ComposableStringHolder {
    @Composable
    override fun render() = stringResource(res, *formatArgs.toTypedArray())
}
