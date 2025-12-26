package chat.schildi.revenge.model.account

import io.element.android.libraries.matrix.api.core.SessionId

class AccountComparator<T>(
    val sessionIdComparator: Comparator<SessionId>,
    val select: (T) -> SessionId,
) : Comparator<T> {
    override fun compare(a: T, b: T): Int {
        return sessionIdComparator.compare(select(a), select(b))
    }
}
