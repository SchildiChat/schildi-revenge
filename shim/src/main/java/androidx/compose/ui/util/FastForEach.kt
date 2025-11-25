package androidx.compose.ui.util

inline fun <T> Iterable<T>.fastForEach(action: (T) -> Unit) = forEach(action)
