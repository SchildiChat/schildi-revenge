package chat.schildi.revenge.notification

import chat.schildi.revenge.model.verification.ScIncomingVerificationRequest

actual fun platformNotifyIncomingVerificationRequest(request: ScIncomingVerificationRequest) =
    NotificationProcessor.notifyIncomingVerificationRequest(request)
