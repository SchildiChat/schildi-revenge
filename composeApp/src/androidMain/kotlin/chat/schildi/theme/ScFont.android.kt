package chat.schildi.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
actual fun platformSupportsColorVectorFonts(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
