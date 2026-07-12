package chat.schildi.revenge.actions

import chat.schildi.revenge.dbus.FreedesktopPortal
import chat.schildi.revenge.util.SystemInfo

internal actual val platformOpenUri: (suspend (String) -> Unit)? =
    if (SystemInfo.isLinux()) FreedesktopPortal::openUri else null
