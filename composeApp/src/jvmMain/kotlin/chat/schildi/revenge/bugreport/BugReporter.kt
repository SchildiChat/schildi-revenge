/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.revenge.bugreport

import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.collectScPrefs
import chat.schildi.revenge.BuildInfo
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.config.ScAppDirs
import chat.schildi.revenge.config.keybindings.ActionArgument
import chat.schildi.revenge.glue.RevengeOkHttpClient
import chat.schildi.revenge.glue.RevengeSessionStore
import chat.schildi.revenge.glue.platformApplicationId
import chat.schildi.revenge.glue.platformOsDebugName
import chat.schildi.revenge.glue.platformVersionCode
import chat.schildi.revenge.glue.platformVersionName
import chat.schildi.revenge.preferences.RevengePrefs
import io.element.android.libraries.androidutils.file.safeDelete
import io.element.android.libraries.core.data.tryOrNull
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Whether calling [getPlatformLogContent] makes sense on this device.
 */
expect val canReadPlatformLog: Boolean
/**
 * Retrieves the logs from the OS, e.g. logcat on Android.
 *
 * @param streamWriter the stream writer
 */
expect fun getPlatformLogContent(streamWriter: OutputStreamWriter)

val RevengeBugReporter = BugReporter()

/**
 * BugReporter creates and sends the bug reports.
 */
class BugReporter(
    private val okHttpClient: () -> OkHttpClient = { RevengeOkHttpClient  },
    private val sessionStore: SessionStore = RevengeSessionStore,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
) {
    companion object {
        // filenames
        private const val PLATFORM_LOG = "platform.log"
    }

    private var currentTracingLogLevel: String? = null

    private val currentLogDirectory = File(ScAppDirs.getUserLogDir())

    suspend fun sendBugReport(
        problemDescription: String,
        reportContext: List<Pair<ActionArgument, String>> = emptyList(),
        withDevicesLogs: Boolean = true,
        canContact: Boolean = false,
        ghIssueNumber: Int? = null,
        onProgress: (percentage: Int) -> Unit = {},
    ): ActionResult {
        // enumerate files to delete
        val bugReportFiles: MutableList<File> = ArrayList()
        var response: Response? = null

        // Start at something like 1000 lines to have some 'buffer' in case unexpected lines were added
        var totalLogLines = 1000L

        return try {
            var serverError: String? = null
            withContext(Dispatchers.IO) {
                val bugDescription = buildString {
                    append(problemDescription)
                    ghIssueNumber?.let {
                        append("\n\nhttps://github.com/SchildiChat/schildi-revenge/issues/$it")
                    }
                }
                val gzippedFiles = mutableListOf<File>()
                var filesTooBig = emptyList<String>()

                if (withDevicesLogs) {
                    savePlatformLog()
                        ?.takeIf { it.length() < RageshakeConfig.MAX_LOG_CONTENT_SIZE }
                        ?.takeIf { countLogLines(it) + totalLogLines < RageshakeConfig.MAX_LOG_LINES_SIZE }
                        ?.let { logCatFile ->
                            compressFile(logCatFile).also {
                                logCatFile.safeDelete()
                            }
                        }
                        ?.let { gzippedLogcat ->
                            gzippedFiles.add(0, gzippedLogcat)
                        }
                }
                val preferredSessionId = UiState.matrixClients.value.keys.sortedWith(UiState.sessionIdComparator.first()).firstOrNull()?.value
                    ?: UiState.sessionIdOrder.first().minByOrNull { it.value }?.key
                // build the multipart request
                val builder = BugReporterMultipartBody.Builder()
                    .addFormDataPart("text", bugDescription)
                    .addFormDataPart("app", RageshakeConfig.BUG_REPORT_APP_NAME)
                    .addFormDataPart("user_id", preferredSessionId.ensureNonEmpty())
                    .addFormDataPart("sessions_active", UiState.matrixClients.value.size.toString())
                    .addFormDataPart("sessions_failed", UiState.failedSessions.value.size.toString())
                    .addFormDataPart("can_contact", canContact.toString())
                    //.addFormDataPart("device", BuildInfo.MODEL.trim())
                    .addFormDataPart("locale", Locale.getDefault().toString())
                    .addFormDataPart("sdk_sha", BuildInfo.SDK_REVISION)
                    .addFormDataPart("local_time", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .addFormDataPart("utc_time", LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME))
                    .addFormDataPart("app_id", platformApplicationId)
                    .addFormDataPart("version", platformVersionName)
                    .addFormDataPart("os", platformOsDebugName)
                    .addFormDataPart("os_name", System.getProperty("os.name"))
                    .addFormDataPart("version_code", platformVersionCode?.toString().ensureNonEmpty())

                UiState.matrixClients.value.forEach { (id, client) ->
                    builder.addFormDataPart("device_id_$id", client.deviceId.value)
                }
                reportContext.forEach { (primitive, value) ->
                    builder.addFormDataPart("report_context_${primitive.name}", value)
                }

                val reportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZ", Locale.US).format(Date())
                builder
                    .addFormDataPart("reportTime", reportTime)
                    .addFormDataPart("is_debug_build", BuildInfo.DEBUG.toString())
                    .addFormDataPart("build_type_app", BuildInfo.BUILD_TYPE)
                    .addFormDataPart("sdk_profile", BuildInfo.RUST_PROFILE)
                if (BuildInfo.DEBUG) {
                    builder.addFormDataPart("label", "debug_build")
                }
                if (canContact) {
                    builder.addFormDataPart("label", "can contact")
                }
                // Non-default SC settings
                val changedScSettings = ScPrefs.rootPrefsAllPlatforms.prefs.collectScPrefs {
                    scPreferencesStore.getCachedOrDefaultValue(it) != it.defaultValue
                }
                val scPrefsString = changedScSettings.joinToString(separator = ",") { "${it.sKey}=${scPreferencesStore.getCachedOrDefaultValue(it)}" }
                builder.addFormDataPart("sc_preferences", scPrefsString)

                if (withDevicesLogs) {
                    val files = getLogFiles().sortedByDescending { it.lastModified() }
                    val filesBySize = files.groupBy {
                        it.length() < RageshakeConfig.MAX_LOG_CONTENT_SIZE
                    }.toMutableMap()

                    filesBySize[true].orEmpty().mapNotNullTo(gzippedFiles) { file ->
                        val logLines = countLogLines(file)
                        totalLogLines += logLines

                        when {
                            totalLogLines > RageshakeConfig.MAX_LOG_LINES_SIZE -> {
                                // Add it to the list of omitted files too
                                (filesBySize.getOrPut(false) { mutableListOf() } as MutableList<File>).add(file)

                                Timber.e(
                                    "Could not upload file ${file.name} because it would exceed the max log lines size " +
                                        "($totalLogLines/${RageshakeConfig.MAX_LOG_LINES_SIZE}"
                                )

                                totalLogLines -= logLines

                                null
                            }
                            file.extension == "gz" -> file
                            else -> compressFile(file)
                        }
                    }
                    filesTooBig = filesBySize[false].orEmpty().map { it.name }
                }

                if (filesTooBig.isNotEmpty()) {
                    builder.addFormDataPart("omitted_logs", filesTooBig.toString())
                }

                currentTracingLogLevel?.let {
                    builder.addFormDataPart("tracing_log_level", it)
                }
                // add the gzipped files, don't cancel the whole upload if only some file failed to upload
                var totalUploadedSize = 0L
                var uploadedSomeLogs = false
                for (file in gzippedFiles) {
                    try {
                        val requestBody = file.asRequestBody(MimeTypes.OctetStream.toMediaTypeOrNull())
                        totalUploadedSize += requestBody.contentLength()
                        // If we are about to upload more than the max request size, stop here
                        if (totalUploadedSize > RageshakeConfig.MAX_LOG_UPLOAD_SIZE) {
                            Timber.e("Could not upload file ${file.name} because it would exceed the max request size")
                            break
                        }
                        builder.addFormDataPart("compressed-log", file.name, requestBody)
                        uploadedSomeLogs = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "## sendBugReport() : fail to attach file ${file.name}")
                    }
                }
                bugReportFiles.addAll(gzippedFiles)
                if (gzippedFiles.isNotEmpty() && !uploadedSomeLogs) {
                    serverError = "Couldn't upload any logs, please retry."
                    return@withContext
                }
                val requestBody = builder.build()
                // add a progress listener
                requestBody.setWriteListener { totalWritten, contentLength ->
                    val percentage = if (-1L != contentLength) {
                        if (totalWritten > contentLength) {
                            100
                        } else {
                            (totalWritten * 100 / contentLength).toInt()
                        }
                    } else {
                        0
                    }
                    Timber.v("## onWrite() : $percentage%")
                    onProgress(percentage)
                }
                // build the request
                val request = Request.Builder()
                    .url(RageshakeConfig.BUG_REPORT_URL)
                    .post(requestBody)
                    .build()
                var errorMessage: String? = null
                // trigger the request
                try {
                    response = okHttpClient()
                        .newCall(request)
                        .execute()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error executing the request")
                    errorMessage = e.localizedMessage
                }
                val responseCode = response?.code
                // if the upload failed, try to retrieve the reason
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    serverError = if (errorMessage != null) {
                        "Failed with error $errorMessage"
                    } else {
                        val responseBody = response?.body
                        if (responseBody == null) {
                            "Failed with error $responseCode"
                        } else {
                            try {
                                val inputStream = responseBody.byteStream()
                                val serverErrorJson = inputStream.use {
                                    it.readBytes().toString(Charsets.UTF_8)
                                }
                                try {
                                    Json.parseToJsonElement(serverErrorJson).jsonObject["error"]?.jsonPrimitive?.contentOrNull
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Timber.e(e, "Json conversion failed")
                                    "Failed with error $responseCode"
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "## sendBugReport() : failed to parse error")
                                "Failed with error $responseCode"
                            }
                        }
                    }
                }
            }
            if (serverError == null) {
                ActionResult.Success()
            } else {
                ActionResult.Failure(serverError)
            }
        } finally {
            withContext(Dispatchers.IO) {
                // delete the generated files when the bug report process has finished
                for (file in bugReportFiles) {
                    file.safeDelete()
                }
                response?.close()
            }
        }
    }

    /**
     * @return the files on the log directory.
     */
    private fun getLogFiles(): List<File> {
        return tryOrNull(
            onException = { Timber.e(it, "## getLogFiles() failed") }
        ) {
            val logDirectory = currentLogDirectory
            logDirectory.listFiles()
                ?.filter {
                    it.isFile &&
                        !it.name.endsWith(PLATFORM_LOG)
                }
        }.orEmpty()
    }

    // ==============================================================================================================
    // Logcat management
    // ==============================================================================================================

    /**
     * Save the platform log, e.g. logcat.
     *
     * @return the file if the operation succeeds
     */
    fun savePlatformLog(): File? {
        val file = File(currentLogDirectory, PLATFORM_LOG)
        if (file.exists()) {
            file.safeDelete()
        }
        return try {
            file.writer().use {
                getPlatformLogContent(it)
            }
            file
        } catch (e: Exception) {
            Timber.e(e, "## saveLogCat() : fail to write logcat")
            null
        }
    }

    private fun countLogLines(file: File): Int {
        return file.reader().useLines { it.count() }
    }
}

fun String?.ensureNonEmpty(fallback: String = "∅"): String {
    return if (isNullOrEmpty()) {
        // GitHub markdown format doesn't like empty code blocks
        fallback
    } else {
        this
    }
}
