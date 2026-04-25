package io.element.android.libraries.matrix.api.timeline

// Let's not do a data class, referential equality is enough for us
/*data*/ class InMemoryMediaThumbnail(
    val data: ByteArray,
    val filename: String,
    val mimeType: String,
)
