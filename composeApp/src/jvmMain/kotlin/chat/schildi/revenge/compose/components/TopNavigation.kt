package chat.schildi.revenge.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.Anim
import chat.schildi.revenge.Dimens

@Composable
fun TopNavigation(content: @Composable RowScope.() -> Unit) {
    val visible = !ScPrefs.MINIMAL_MODE.value()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(Anim.DURATION)) { -it } +
                expandVertically(tween(Anim.DURATION), expandFrom = Alignment.Top),
        exit = slideOutVertically(tween(Anim.DURATION)) { -it } +
                shrinkVertically(tween(Anim.DURATION), shrinkTowards = Alignment.Top),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

@Composable
fun TopNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    WithTooltip(contentDescription, modifier) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(imageVector, contentDescription)
        }
    }
}

@Composable
fun RowScope.TopNavigationTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.padding(Dimens.windowPadding).weight(1f),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}
