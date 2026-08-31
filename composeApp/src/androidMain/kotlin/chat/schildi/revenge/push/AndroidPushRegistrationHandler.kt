package chat.schildi.revenge.push

import android.app.Activity
import chat.schildi.lib.platform.platformDeviceName
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.revenge.RevengeApplication
import chat.schildi.revenge.ScCoroutines
import chat.schildi.revenge.UiState
import chat.schildi.revenge.androidWindowManager
import chat.schildi.revenge.database.push.PushRegistrationEntity
import chat.schildi.revenge.database.revengeDatabase
import chat.schildi.revenge.glue.RevengeOkHttpClient
import chat.schildi.revenge.preferences.RevengePrefs
import co.touchlab.kermit.Logger
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.pusher.SetHttpPusherData
import io.element.android.libraries.matrix.api.pusher.UnsetHttpPusherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import java.net.URI
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.uuid.Uuid

const val DEFAULT_GATEWAY = "https://up.schildi.chat/_matrix/push/v1/notify"
const val MATRIX_NOTIFY_PATH = "/_matrix/push/v1/notify"
val DEFINITE_NON_GATEWAY_CODES = setOf(401, 403, 404, 405, 406)

@OptIn(ExperimentalAtomicApi::class)
object AndroidPushRegistrationHandler {

    private val log = Logger.withTag("AndroidPushRegistration")

    private val pushAppId = RevengeApplication.instance.packageName

    private val isLaunched = AtomicBoolean(false)
    private val scope = ScCoroutines.scope(Dispatchers.IO, "AndroidPushRegistration")

    private val pushDao = revengeDatabase.pushNotificationDao()

    private val deviceName = platformDeviceName ?: "Android"

    fun launch() {
        if (!isLaunched.exchange(true)) {
            loopRegisterUnifiedPush()
        }
    }

    private fun loopRegisterUnifiedPush() {
        combine(
            RevengePrefs.settingFlow(ScPrefs.PUSH_NOTIFICATIONS),
            androidWindowManager.windows.map { it.isEmpty() }.distinctUntilChanged(),
            UiState.knownSessionIds,
            UiState.mutedAccounts,
        ) { enabled, backgrounded, sessionIds, muted ->
            if (backgrounded || muted == null) {
                return@combine
            }
            val currentSessions = sessionIds.mapNotNull { if (it in muted) null else it.value }.toSet()
            val activity = androidWindowManager.currentActivity?.get() ?: return@combine

            val currentRegistrations = pushDao.getPushRegistrations()

            if (!enabled) {
                currentRegistrations.forEach {
                    UnifiedPush.unregister(activity, it.clientSecret)
                    pushDao.deletePushRegistration(it)
                }
                return@combine
            }

            val registeredSessions = currentRegistrations.map { it.sessionId }.toSet()
            val needsRegistration = currentSessions - registeredSessions
            val newRegistrations = needsRegistration.map { sessionId ->
                PushRegistrationEntity(
                    sessionId = sessionId,
                    clientSecret = Uuid.random().toString(),
                )
            }
            val needsEndpoint = currentRegistrations.filter {
                it.sessionId in currentSessions && it.endpoint == null
            } + newRegistrations
            val needsUnregistration = currentRegistrations.filter { it.sessionId !in currentSessions }
            log.v { "Push state: registered=${registeredSessions.size}, adding=${needsRegistration.size}, removing=${needsUnregistration.size}" }
            if (needsEndpoint.isNotEmpty()) {
                log.i { "Try registering push endpoints for ${needsEndpoint.size} sessions" }
                if (tryUseCurrentOrDefaultDistributor(activity)) {
                    newRegistrations.forEach { registration ->
                        pushDao.insertPushRegistration(registration)
                    }
                    needsEndpoint.forEach { registration ->
                        log.d { "Registering push for ${registration.sessionId}, instance=${registration.clientSecret}" }
                        UnifiedPush.register(RevengeApplication.instance, instance = registration.clientSecret)
                    }
                } else {
                    log.w { "No UnifiedPush distributor selected" }
                }
            }
            if (needsUnregistration.isNotEmpty()) {
                needsUnregistration.forEach { registration ->
                    log.i { "Unregistering push for ${registration.sessionId}" }
                    UnifiedPush.unregister(RevengeApplication.instance, registration.clientSecret)
                    pushDao.deletePushRegistration(registration)
                }
            }
        }.launchIn(scope)
    }

    private suspend fun tryUseCurrentOrDefaultDistributor(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            UnifiedPush.tryUseCurrentOrDefaultDistributor(activity) { success ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.success(success))
                }
            }
        }

    fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        log.i { "Received UnifiedPush endpoint for instance=$instance: ${endpoint.url}" }
        scope.launch(Dispatchers.IO) {
            pushDao.updateEndpoint(instance, endpoint.url)
            registerHomeserverPush(instance)
        }
    }

    fun onPushUnregistered(instance: String) {
        log.i { "UnifiedPush unregistered for instance=$instance" }
        scope.launch(Dispatchers.IO) {
            val registration = pushDao.getPushRegistration(instance) ?: run {
                log.i { "Did not find UnifiedPush registration for instance=$instance that just unregistered" }
                return@launch
            }
            pushDao.deletePushRegistration(registration)
            val pushKey = registration.endpoint ?: return@launch
            val client = UiState.currentClientFor(SessionId(registration.sessionId)) ?: run {
                log.w { "Skip Matrix push unregistration for ${registration.sessionId}, no client yet" }
                return@launch
            }
            client.pushersService.unsetHttpPusher(
                UnsetHttpPusherData(
                    appId = pushAppId,
                    pushKey = pushKey,
                )
            ).onSuccess {
                log.i { "Matrix push unregistration successful for ${registration.sessionId}" }
            }.onFailure {
                log.e("Matrix push unregistration failed for ${registration.sessionId}", it)
            }
        }
    }

    // TODO kick on connectivity change too?
    private suspend fun registerHomeserverPush(instance: String) {
        val toRegister = listOfNotNull(pushDao.getPendingPushRegistration(instance))
        if (toRegister.isEmpty()) {
            return
        }
        log.d { "Handling Matrix push registration for ${toRegister.size} sessions" }
        toRegister.forEach { registration ->
            val client = UiState.currentClientFor(SessionId(registration.sessionId)) ?: run {
                log.w { "Skip Matrix push registration for ${registration.sessionId}, no client yet" }
                return@forEach
            }
            registration.endpoint ?: run {
                log.e { "Skip Matrix push registration for ${registration.sessionId}, no endpoint" }
                return@forEach
            }
            val gateway = resolveGateway(registration.endpoint, registration.gateway)
            val payload = serializeUnifiedPushDefaultPayload(registration.clientSecret)
            log.i { "Registering push for ${registration.sessionId} at ${registration.endpoint} via ${registration.gateway}" }
            client.pushersService.setHttpPusher(
                SetHttpPusherData(
                    pushKey = registration.endpoint,
                    appId = pushAppId,
                    url = gateway,
                    appDisplayName = "SchildiChat Revenge",
                    deviceDisplayName = deviceName,
                    profileTag = "mobile_",
                    lang = "en",
                    defaultPayload = payload,
                    append = false,
                )
            ).onSuccess {
                log.i { "Matrix push registration successful for ${registration.sessionId}" }
                pushDao.insertPushRegistration(
                    registration.copy(
                        gateway = gateway,
                        homeserverRegistered = true,
                    )
                )
            }.onFailure {
                log.e("Matrix push registration failed for ${registration.sessionId}", it)
            }
        }
    }

    private fun resolveGateway(endpoint: String, previousGateway: String?): String {
        val candidate = runCatching {
            val endpointUri = URI(endpoint)
            URI(endpointUri.scheme, null, endpointUri.host, endpointUri.port, MATRIX_NOTIFY_PATH, null, null).toString()
        }.getOrNull() ?: return DEFAULT_GATEWAY
        val request = Request.Builder().url(candidate).get().build()
        return runCatching {
            RevengeOkHttpClient.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val body = response.body?.string() ?: return@use DEFAULT_GATEWAY
                        val gateway = Json.parseToJsonElement(body).jsonObject["unifiedpush"]
                            ?.jsonObject?.get("gateway")?.jsonPrimitive?.content
                        candidate.takeIf { gateway == "matrix" } ?: DEFAULT_GATEWAY
                    }
                    response.code in DEFINITE_NON_GATEWAY_CODES -> DEFAULT_GATEWAY
                    else -> previousGateway ?: DEFAULT_GATEWAY
                }
            }
        }.getOrElse { previousGateway ?: DEFAULT_GATEWAY }
    }
}
