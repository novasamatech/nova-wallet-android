package io.novafoundation.nova.common.utils

import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.API_NOT_AVAILABLE
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.APP_NOT_INSTALLED
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.APP_UID_MISMATCH
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED
import io.novafoundation.nova.common.utils.coroutines.RootScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val RECEIVING_PROVIDER_MAX_RETRY = 3
private const val INTEGRITY_TIMEOUT_MILLIS = 100_000L

// Codes that describe the device itself (no or outdated Play Store / Play services), not a transient outage.
private val DEVICE_UNSUPPORTED_CODES = setOf(
    API_NOT_AVAILABLE,
    PLAY_STORE_NOT_FOUND,
    APP_NOT_INSTALLED,
    PLAY_SERVICES_NOT_FOUND,
    APP_UID_MISMATCH,
    PLAY_STORE_VERSION_OUTDATED,
    PLAY_SERVICES_VERSION_OUTDATED
)

/** Whether Play Integrity failed because this device cannot provide it, so a retry cannot succeed. */
fun Throwable.isIntegrityUnsupportedOnDevice(): Boolean = generateSequence(this) { it.cause }
    .any { it is StandardIntegrityException && it.statusCode in DEVICE_UNSUPPORTED_CODES }

private sealed interface ProviderState {
    data object Preparing : ProviderState
    class ReceivingError(val cause: Throwable?) : ProviderState
    class Ready(val provider: StandardIntegrityTokenProvider) : ProviderState
}

class IntegrityService(
    private val cloudProjectNumber: Long,
    private var standardIntegrityManager: StandardIntegrityManager,
    private val rootScope: RootScope
) {

    private var providerState: ProviderState = ProviderState.Preparing
    private val prepareProviderMutex = Mutex()

    init {
        rootScope.launch { ensureProviderIsReady() }
    }

    suspend fun getIntegrityToken(requestHash: String): String = withTimeout(INTEGRITY_TIMEOUT_MILLIS) {
        ensureProviderIsReady()

        try {
            requestIntegrityToken(requestHash)
        } catch (e: StandardIntegrityException) {
            if (e.statusCode == INTEGRITY_TOKEN_PROVIDER_INVALID) {
                resetProviderState()
                ensureProviderIsReady()
                requestIntegrityToken(requestHash)
            } else {
                throw e
            }
        }
    }

    private suspend fun ensureProviderIsReady() {
        if (providerState is ProviderState.Ready) return

        prepareProviderMutex.withLock {
            if (providerState is ProviderState.Ready) return@withLock

            repeat(RECEIVING_PROVIDER_MAX_RETRY) {
                providerState = prepareTokenProvider()
                if (providerState is ProviderState.Ready) return@withLock
            }
        }
    }

    private fun resetProviderState() {
        providerState = ProviderState.Preparing
    }

    private suspend fun prepareTokenProvider(): ProviderState = withTimeout(INTEGRITY_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            standardIntegrityManager.prepareIntegrityToken(providerRequest())
                .addOnSuccessListener { continuation.resume(ProviderState.Ready(it)) }
                .addOnFailureListener { continuation.resume(ProviderState.ReceivingError(it)) }
                .addOnCanceledListener { continuation.resume(ProviderState.ReceivingError(null)) }
        }
    }

    private fun getIntegrityTokenProvider() = (providerState as? ProviderState.Ready)?.provider

    private suspend fun requestIntegrityToken(requestHash: String): String {
        val provider = getIntegrityTokenProvider()
            ?: throw IllegalStateException("Token provider is not initialized", (providerState as? ProviderState.ReceivingError)?.cause)
        return suspendCancellableCoroutine { continuation ->
            provider.request(integrityTokenRequest(requestHash))
                .addOnSuccessListener { continuation.resume(it.token()) }
                .addOnFailureListener { continuation.resumeWithException(it) }
                .addOnCanceledListener { continuation.cancel(CancellationException("Integrity token request cancelled")) }
        }
    }

    private fun providerRequest() =
        StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

    private fun integrityTokenRequest(requestHash: String) =
        StandardIntegrityTokenRequest.builder()
            .setRequestHash(requestHash)
            .build()
}
