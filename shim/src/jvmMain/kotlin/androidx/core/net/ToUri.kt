package androidx.core.net

import java.net.URI

fun String.toUri() = URI.create(this)
