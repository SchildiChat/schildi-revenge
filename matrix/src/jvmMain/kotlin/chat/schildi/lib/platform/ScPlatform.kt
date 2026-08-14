package chat.schildi.lib.platform

import io.element.android.libraries.core.data.tryOrNull
import java.net.InetAddress

val platformDeviceName: String? = tryOrNull { InetAddress.getLocalHost().hostName }
    ?: System.getenv("HOST")
    ?: System.getenv("HOSTNAME")
    ?: System.getenv("COMPUTERNAME")
