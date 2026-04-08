package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.element.android.libraries.matrix.api.core.UserId

data class CopyActions(
    val accessPlaintext: (() -> String?)? = null,
    val accessPlaintextSuspend: (suspend () -> String?)? = null,
    val accessUserId: (() -> String?)? = null,
    val accessMxcUrl: (() -> String?)? = null,
    val accessFilePath: (() -> String?)? = null,
)

@Composable
fun plainTextCopyAction(access: () -> String?) = remember(access) { CopyActions(accessPlaintext = access) }

@Composable
fun plainTextCopyActionWithUserId(userId: UserId?, plaintext: () -> String?) = remember(userId, plaintext) {
    CopyActions(
        accessPlaintext = plaintext,
        accessUserId = userId?.let {{ userId.value }},
    )
}

@Composable
fun plainTextCopyActionWithMxcUrl(mxc: String?, plaintext: (() -> String?)? = null) = remember(mxc, plaintext) {
    CopyActions(
        accessPlaintext = plaintext,
        accessMxcUrl = mxc?.let {{ mxc }},
    )
}

@Composable
fun String.toCopyAction() = remember(this) { CopyActions(accessPlaintext = { this }) }
