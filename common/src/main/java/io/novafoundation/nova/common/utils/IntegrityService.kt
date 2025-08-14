package io.novafoundation.nova.common.utils

import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class IntegrityService(
    private val cloudProjectNumber: Long,
    private var standardIntegrityManager: StandardIntegrityManager
) {

    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    init {
        standardIntegrityManager.prepareIntegrityToken(providerRequest())
            .addOnSuccessListener { integrityTokenProvider = it }
    }

    suspend fun getIntegrityToken(requestHash: String): String {
        if (integrityTokenProvider == null) {
            integrityTokenProvider = prepareTokenProvider()
        }

        return try {
            requestIntegrityToken(requestHash)
        } catch (e: StandardIntegrityException) {
            if (e.statusCode == INTEGRITY_TOKEN_PROVIDER_INVALID) {
                integrityTokenProvider = prepareTokenProvider()
                requestIntegrityToken(requestHash)
            } else {
                throw e
            }
        }
    }

    private suspend fun prepareTokenProvider(): StandardIntegrityTokenProvider {
        return suspendCoroutine { continuation ->
            standardIntegrityManager.prepareIntegrityToken(providerRequest())
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    private suspend fun requestIntegrityToken(requestHash: String): String {
        val provider = integrityTokenProvider ?: throw IllegalStateException("Token provider is not initialized")
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
