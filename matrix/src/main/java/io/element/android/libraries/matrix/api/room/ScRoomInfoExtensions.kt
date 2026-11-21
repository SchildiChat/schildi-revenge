package io.element.android.libraries.matrix.api.room

data class BridgeState(
    val stateKey: String,
    val bridgeBotUserId: String?,
    val protocol: Protocol?
) {
    data class Protocol(
        val id: String?,
        val displayName: String?,
        val avatarUrl: String?,
    )
}

fun RoomInfo.preferOpenAtBottom() = unreadCount <= 0L && notificationCount <= 0L && highlightCount <= 0L &&
        numUnreadMessages <= 0 && numUnreadNotifications <= 0 && numUnreadMentions <= 0 && !unreadCountUnderestimate
