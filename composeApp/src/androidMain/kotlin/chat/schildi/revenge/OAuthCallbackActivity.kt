package chat.schildi.revenge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import chat.schildi.revenge.model.account.RevengeOAUthRepo
import kotlinx.coroutines.launch

class OAuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callbackUrl = intent
            .takeIf { it.action == Intent.ACTION_VIEW }
            ?.takeIf { it.data?.scheme == packageName }
            ?.dataString
        if (callbackUrl != null) {
            GlobalActionsScope.launch {
                RevengeOAUthRepo.handleOAuthLoginCallback(callbackUrl)
            }
        }

        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
        finish()
    }
}
