package io.element.android.libraries.matrix.impl.notification

import io.element.android.libraries.matrix.api.core.UserId
import org.matrix.rustcomponents.sdk.NotificationEvent

internal fun NotificationEvent.senderId(): UserId =
    when (this) {
        is NotificationEvent.Timeline -> UserId(event.senderId())
        is NotificationEvent.Invite -> UserId(sender)
    }

