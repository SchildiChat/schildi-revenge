package chat.schildi.revenge.glue

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import chat.schildi.revenge.RevengeApplication
import coil3.PlatformContext

internal actual fun applicationPlatformContext(): PlatformContext = RevengeApplication.instance

internal actual val platformApplicationId: String
    get() = RevengeApplication.instance.packageName

private val packageInfo by lazy {
    val packageManager = RevengeApplication.instance.packageManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(platformApplicationId, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(platformApplicationId, 0)
    }
}

internal actual val platformVersionName: String
    get() = packageInfo.versionName ?: "0.0.0-dev"

internal actual val platformVersionCode: Long
    get() = PackageInfoCompat.getLongVersionCode(packageInfo)
