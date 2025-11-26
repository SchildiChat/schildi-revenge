package timber.log

import co.touchlab.kermit.Logger

/**
 * Timber is Android-only, so shim it to call kermit instead, without having to rewrite imported Element X libs.
 */
val Timber = TimberShim(Logger)

/**
 * Timber is Android-only, so shim it to call kermit instead, without having to rewrite imported Element X libs.
 */
class TimberShim(val logger: Logger) {
    fun v(message: String) = logger.v(message)
    fun v(throwable: Throwable) = logger.v(throwable.toString(), throwable)
    fun v(throwable: Throwable?, message: String) = logger.v(message, throwable)
    fun v(message: String, throwable: Throwable?) = logger.v(message, throwable)
    fun d(message: String) = logger.d(message)
    fun d(throwable: Throwable) = logger.d(throwable.toString(), throwable)
    fun d(throwable: Throwable?, message: String) = logger.d(message, throwable)
    fun d(message: String, throwable: Throwable?) = logger.d(message, throwable)
    fun i(message: String) = logger.i(message)
    fun i(throwable: Throwable) = logger.i(throwable.toString(), throwable)
    fun i(throwable: Throwable?, message: String) = logger.i(message, throwable)
    fun i(message: String, throwable: Throwable?) = logger.i(message, throwable)
    fun w(message: String) = logger.w(message)
    fun w(throwable: Throwable) = logger.w(throwable.toString(), throwable)
    fun w(throwable: Throwable?, message: String) = logger.w(message, throwable)
    fun w(message: String, throwable: Throwable?) = logger.w(message, throwable)
    fun e(message: String) = logger.e(message)
    fun e(throwable: Throwable) = logger.e(throwable.toString(), throwable)
    fun e(throwable: Throwable?, message: String) = logger.e(message, throwable)
    fun e(message: String, throwable: Throwable?) = logger.e(message, throwable)
    fun tag(tag: String) = TimberShim(logger.withTag(tag))
}
