package chat.schildi.revenge

import chat.schildi.revenge.config.ScAppDirs
import chat.schildi.revenge.glue.platformVersionName
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import org.matrix.rustcomponents.sdk.TracingFileConfiguration
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val APP_LOG_PREFIX = "app"
private const val SDK_LOG_PREFIX = "sdk"
private const val LOG_FILE_SUFFIX = ".log"
private const val MAX_LOG_SIZE_BYTES = 100L * 1024L * 1024L
private const val LOG_QUEUE_CAPACITY = 1024
private const val MAX_WRITE_BATCH_SIZE = 256
private val MAX_LOG_AGE = Duration.ofDays(3)
private val crashFileTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(ZoneOffset.UTC)

private val logDirectory = File(ScAppDirs.getUserLogDir())

internal object RevengeLogFormatter : MessageStringFormatter {
    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String {
        return "${Instant.now().truncatedTo(ChronoUnit.MILLIS)} ${super.formatMessage(severity, tag, message)}"
    }
}

private val appFileLogWriter by lazy {
    RotatingFileLogWriter(
        directory = logDirectory,
        filePrefix = APP_LOG_PREFIX,
        maxAge = MAX_LOG_AGE,
        maxTotalSizeBytes = MAX_LOG_SIZE_BYTES,
    )
}

internal fun createAppFileLogWriter(): LogWriter = appFileLogWriter

internal fun flushAppFileLogs() {
    appFileLogWriter.flushPendingLogs()
}

internal fun logProcessStarted() {
    Logger.withTag("Process").i(
        "App started: version=$platformVersionName, build=${BuildInfo.BUILD_TYPE}, revision=${BuildInfo.SOURCE_REVISION}",
    )
}

internal fun logUncaughtException(thread: Thread, error: Throwable) {
    val message = "Schildi encountered a fatal error in ${thread.name}"
    runCatching { Logger.e(message, error) }

    try {
        val directory = logDirectory
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Failed to create log directory ${directory.absolutePath}")
        }

        val timestamp = crashFileTimeFormatter.format(Instant.now())
        val file = File(directory, "$APP_LOG_PREFIX.crash.$timestamp-${System.nanoTime().toString(16)}$LOG_FILE_SUFFIX")
        FileOutputStream(file, true).use { stream ->
            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                writer.write(RevengeLogFormatter.formatMessage(Severity.Error, Tag("UncaughtException"), Message(message)))
                writer.write(System.lineSeparator())
                error.printStackTrace(PrintWriter(writer))
                writer.flush()
                stream.fd.sync()
            }
        }
    } catch (failure: Throwable) {
        System.err.println("Failed to write emergency crash log")
        failure.printStackTrace(System.err)
    }

    runCatching { flushAppFileLogs() }
}

internal fun createSdkTracingFileConfiguration() = TracingFileConfiguration(
    path = logDirectory.absolutePath,
    filePrefix = SDK_LOG_PREFIX,
    fileSuffix = LOG_FILE_SUFFIX,
    maxTotalSizeBytes = MAX_LOG_SIZE_BYTES.toULong(),
    maxAgeSeconds = MAX_LOG_AGE.seconds.toULong(),
)

private class RotatingFileLogWriter(
    private val directory: File,
    private val filePrefix: String,
    private val maxAge: Duration,
    private val maxTotalSizeBytes: Long,
) : LogWriter() {
    private val periodFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH").withZone(ZoneOffset.UTC)
    private val queue = ArrayBlockingQueue<QueueItem>(LOG_QUEUE_CAPACITY)
    private val droppedLogCount = AtomicLong()
    private val disabled = AtomicBoolean()
    private var currentPeriod: String? = null
    private var output: BufferedWriter? = null
    private val worker = Thread(::runWriter, "SchildiLogWriter").apply {
        isDaemon = true
        start()
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (!disabled.get() && !queue.offer(QueueItem.Log(severity, message, tag, throwable))) {
            droppedLogCount.incrementAndGet()
        }
    }

    fun flushPendingLogs() {
        if (disabled.get()) return

        val flushed = CountDownLatch(1)
        try {
            if (queue.offer(QueueItem.Flush(flushed), 1, TimeUnit.SECONDS)) {
                flushed.await(2, TimeUnit.SECONDS)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun runWriter() {
        try {
            while (true) {
                val batch = ArrayList<QueueItem>(MAX_WRITE_BATCH_SIZE)
                batch.add(queue.take())
                queue.drainTo(batch, MAX_WRITE_BATCH_SIZE - 1)

                batch.forEach { item ->
                    when (item) {
                        is QueueItem.Log -> write(item)
                        is QueueItem.Flush -> {
                            output?.flush()
                            item.flushed.countDown()
                        }
                    }
                }
                output?.flush()
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (failure: Exception) {
            disable(failure)
        }
    }

    private fun write(item: QueueItem.Log) {
        val period = periodFormatter.format(Instant.now())
        if (period != currentPeriod) {
            rotate(period)
        }

        val droppedLogs = droppedLogCount.getAndSet(0)
        if (droppedLogs > 0) {
            writeEntry(Severity.Warn, "Dropped $droppedLogs file log entries because the writer queue was full", "FileLogWriter", null)
        }
        writeEntry(item.severity, item.message, item.tag, item.throwable)
    }

    private fun writeEntry(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val writer = checkNotNull(output)
        writer.write(RevengeLogFormatter.formatMessage(severity, Tag(tag), Message(message)))
        writer.newLine()
        throwable?.printStackTrace(PrintWriter(writer))
    }

    private fun rotate(period: String) {
        output?.close()
        output = null
        currentPeriod = null

        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Failed to create log directory ${directory.absolutePath}")
        }
        if (!directory.isDirectory) {
            throw IllegalStateException("Log path is not a directory: ${directory.absolutePath}")
        }

        cleanUpLogFiles()
        val file = File(directory, "$filePrefix.$period$LOG_FILE_SUFFIX")
        output = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), StandardCharsets.UTF_8))
        currentPeriod = period
    }

    private fun cleanUpLogFiles() {
        val files = directory.listFiles { file ->
            file.isFile && file.name.startsWith("$filePrefix.") && file.name.endsWith(LOG_FILE_SUFFIX)
        }?.sortedBy(File::lastModified).orEmpty()

        val oldestAllowed = Instant.now().minus(maxAge).toEpochMilli()
        val retainedFiles = files.filter { file ->
            file.lastModified() >= oldestAllowed || !file.delete()
        }.toMutableList()

        var totalSize = retainedFiles.sumOf(File::length)
        while (totalSize > maxTotalSizeBytes && retainedFiles.isNotEmpty()) {
            val oldest = retainedFiles.removeAt(0)
            val size = oldest.length()
            if (oldest.delete()) {
                totalSize -= size
            }
        }
    }

    private fun disable(failure: Exception) {
        runCatching { output?.close() }
        output = null
        disabled.set(true)
        System.err.println("Disabling file logging after failing to write to ${directory.absolutePath}")
        failure.printStackTrace(System.err)
    }

    private sealed interface QueueItem {
        data class Log(
            val severity: Severity,
            val message: String,
            val tag: String,
            val throwable: Throwable?,
        ) : QueueItem

        data class Flush(val flushed: CountDownLatch) : QueueItem
    }
}
