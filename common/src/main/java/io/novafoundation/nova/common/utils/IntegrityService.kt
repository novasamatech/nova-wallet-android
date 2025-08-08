package io.novafoundation.nova.common.utils

import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
import kotlinx.coroutines.flow.MutableStateFlow
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
    private var standardIntegrityManager: StandardIntegrityManager
) {

    private var state: MutableStateFlow<ProviderState> = MutableStateFlow(ProviderState.Preparing)

    init {
        prepareTokenProvider()
    }

    suspend fun getIntegrityToken(requestHash: String): String {
        ensureProviderIsReady()

        return try {
            requestIntegrityToken(requestHash)
        } catch (e: StandardIntegrityException) {
            if (e.statusCode == INTEGRITY_TOKEN_PROVIDER_INVALID) {
                prepareTokenProvider()
                ensureProviderIsReady()
                requestIntegrityToken(requestHash)
            } else {
                throw e
            }
        }
    }

    private suspend fun ensureProviderIsReady(retry: Int = 0) {
        if (retry >= RECEIVING_PROVIDER_MAX_RETRY) return
        if (state.value is ProviderState.Ready) return

        if (state.value is ProviderState.Preparing) {
            state.collect {
                if (it is ProviderState.Ready) return@collect
                if (it is ProviderState.ReceivingError) return@collect
            }
        }

        if (state.value is ProviderState.Ready) return
        if (state.value is ProviderState.ReceivingError) {
            prepareTokenProvider()
            ensureProviderIsReady(retry + 1)
        }
    }

    private fun prepareTokenProvider() {
        state.value = ProviderState.Preparing
        standardIntegrityManager.prepareIntegrityToken(providerRequest())
            .addOnSuccessListener { state.value = ProviderState.Ready(it) }
            .addOnFailureListener { state.value = ProviderState.ReceivingError }
            .addOnCanceledListener { state.value = ProviderState.ReceivingError }
    }

    private fun getIntegrityTokenProvider() = (state.value as? ProviderState.Ready)?.provider

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
