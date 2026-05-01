package chat.schildi.matrixsdk

data class ScTimelineFilterSettings(
    val showHiddenEvents: Boolean = false,
    val showRedactions: Boolean = true,
    val preferHideThreadedEvents: Boolean? = null,
) {
    companion object {
        val IncludeAll = ScTimelineFilterSettings(
            showHiddenEvents = true,
            showRedactions = true,
            preferHideThreadedEvents = false,
        )
    }
}
