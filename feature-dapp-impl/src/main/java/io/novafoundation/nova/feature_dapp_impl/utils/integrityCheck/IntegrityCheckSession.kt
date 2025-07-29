package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.common.utils.sha256
import io.novafoundation.nova.common.utils.toBase64
import io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck.IntegrityCheckSession.Callback
import java.util.UUID

class IntegrityCheckSessionFactory(
    private val apiCreator: NetworkApiCreator,
    private val preferences: Preferences,
    private val integrityService: IntegrityService
) {

    fun createSession(
        baseUrl: String,
        callback: Callback
    ) = IntegrityCheckSession(
        apiCreator.create(IntegrityCheckApi::class.java, baseUrl.ensureSuffix("/")),
        preferences,
        integrityService,
        callback
    )
}

private const val PREFS_APP_INTEGRITY_ID = "PREFS_APP_INTEGRITY_ID"
private const val PREFS_ATTESTATION_SUCCEED = "PREFS_ATTESTATION_SUCCEED"

class IntegrityCheckSession(
    private val integrityCheckApi: IntegrityCheckApi,
    private val preferences: Preferences,
    private val integrityService: IntegrityService,
    private var callback: Callback?
) {

    interface Callback {
        fun sendVerificationRequest(challenge: String, appIntegrityId: String, signature: String)
    }

    suspend fun startIntegrityCheck() {
        if (isAttestationNeeded()) {
            runAttestation()
        }

        runVerifying()
    }

    suspend fun restartIntegrityCheck() {
        runAttestation()

        runVerifying()
    }

    fun removeCallback() {
        callback = null
    }

    private suspend fun runAttestation() {
        val challengeResponse = integrityCheckApi.getChallenge()
        val appIntegrityId = getAppIntegrityId()
        IntegrityCheckKeyPairGenerator.ensureKeyPairGenerated(appIntegrityId)
        val publicKey = IntegrityCheckKeyPairGenerator.getPublicKey(appIntegrityId).toBase64()

        val requestHash = createRequestHash(challengeResponse.challenge + appIntegrityId + publicKey)
        val integrityToken = integrityService.getIntegrityToken(requestHash = requestHash)

        integrityCheckApi.attest(
            AttestRequest(
                appIntegrityId = appIntegrityId,
                publicKey = publicKey,
                integrityToken = integrityToken,
                challenge = challengeResponse.challenge
            )
        )

        preferences.putBoolean(PREFS_ATTESTATION_SUCCEED, true)
    }

    private suspend fun runVerifying() {
        val challengeResponse = integrityCheckApi.getChallenge()
        val appIntegrityId = getAppIntegrityId()
        val requestHash = createRequestHash(challengeResponse.challenge + appIntegrityId)
        val signature = IntegrityCheckKeyPairGenerator.signData(appIntegrityId, requestHash.toByteArray())

        callback?.sendVerificationRequest(
            appIntegrityId = appIntegrityId,
            challenge = challengeResponse.challenge,
            signature = signature.toBase64()
        )
    }

    private fun isAttestationNeeded(): Boolean {
        val appIntegrityId = preferences.getString(PREFS_APP_INTEGRITY_ID)
        return appIntegrityId == null
            || !isAttestationSucceed()
            || !IntegrityCheckKeyPairGenerator.isKeyPairGenerated(appIntegrityId)
    }

    private fun isAttestationSucceed(): Boolean {
        return preferences.getBoolean(PREFS_ATTESTATION_SUCCEED, false)
    }

    private fun createRequestHash(value: String): String {
        return value.toByteArray()
            .sha256()
            .toBase64()
    }

    private fun getAppIntegrityId(): String {
        var id = preferences.getString(PREFS_APP_INTEGRITY_ID)
        if (id == null) {
            id = UUID.randomUUID().toString()
            preferences.putString(PREFS_APP_INTEGRITY_ID, id)
        }

        return id
    }
}
