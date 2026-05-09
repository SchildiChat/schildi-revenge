package chat.schildi.revenge.actions

fun <T> List<T>?.formatEventContentDump(
    eventType: (T) -> String,
    content: (T) -> String,
    stateKey: (T) -> String? = { null },
) = this?.joinToString("\n\n\n") {
    val header = stateKey(it)?.let { stateKey ->
        "# `${eventType(it)}` / `$stateKey`"
    } ?: "# `${eventType(it)}`"

    "$header\n\n```json\n${content(it)}\n```"
} ?: "{}"
