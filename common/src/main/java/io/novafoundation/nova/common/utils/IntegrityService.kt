package io.novafoundation.nova.common.utils

import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
import io.novafoundation.nova.common.utils.coroutines.RootScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val RECEIVING_PROVIDER_MAX_RETRY = 3

private sealed interface ProviderState {
    data object Preparing : ProviderState
    data object ReceivingError : ProviderState
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
        rootScope.launch {
            prepareProviderMutex.withLock {
                providerState = prepareTokenProvider()
            }
        }
    }

    suspend fun getIntegrityToken(requestHash: String): String {
        ensureProviderIsReady()

        return try {
            requestIntegrityToken(requestHash)
        } catch (e: StandardIntegrityException) {
            if (e.statusCode == INTEGRITY_TOKEN_PROVIDER_INVALID) {
                providerState = prepareTokenProvider()
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
                if (providerState is ProviderState.Ready) return@repeat
            }
        }
    }

    private suspend fun prepareTokenProvider() = suspendCoroutine { continuation ->
        standardIntegrityManager.prepareIntegrityToken(providerRequest())
            .addOnSuccessListener { continuation.resume(ProviderState.Ready(it)) }
            .addOnFailureListener { continuation.resume(ProviderState.ReceivingError) }
            .addOnCanceledListener { continuation.resume(ProviderState.ReceivingError) }
    }

    private fun getIntegrityTokenProvider() = (providerState as? ProviderState.Ready)?.provider

    private suspend fun requestIntegrityToken(requestHash: String): String {
        val provider = getIntegrityTokenProvider() ?: throw IllegalStateException("Token provider is not initialized")
        return suspendCoroutine { continuation ->
            provider.request(integrityTokenRequest(requestHash))
                .addOnSuccessListener { continuation.resume(it.token()) }
                .addOnFailureListener { continuation.resumeWithException(it) }
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
