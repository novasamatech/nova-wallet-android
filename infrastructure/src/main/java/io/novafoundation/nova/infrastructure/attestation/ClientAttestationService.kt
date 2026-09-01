package io.novafoundation.nova.infrastructure.attestation

import android.util.Base64
import io.novafoundation.nova.common.utils.IntegrityService
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

const val HEADER_CLIENT_ID = "X-Client-Id"
const val HEADER_CHALLENGE = "X-Challenge"
const val HEADER_SIGNATURE = "X-Signature"

private const val PLATFORM_ANDROID = "android"

enum class AttestationMode(val wireName: String) {
    PLAY_INTEGRITY("play_integrity"),
    SHARED_SECRET("shared_secret"),
    UNATTESTED("none")
}

/** The backend rejected this client's attestation; retrying will not help. */
class AttestationFailedException(message: String) : IOException(message)

interface ClientAttestationService {

    /**
     * Headers proving this request came from an attested client, or null when
     * attestation is disabled. Registers the client on first use.
     */
    suspend fun signedHeaders(body: ByteArray): Map<String, String>?
}

class RealClientAttestationService(
    private val api: AttestationApi,
    private val identity: AttestationIdentity,
    private val keyPairStore: AttestationKeyPairStore,
    private val integrityService: IntegrityService,
    private val appPackage: String,
    private val mode: AttestationMode,
    private val sharedSecret: String? = null
) : ClientAttestationService {

    private val registrationMutex = Mutex()

    @Volatile
    private var rejected = false

    override suspend fun signedHeaders(body: ByteArray): Map<String, String>? {
        if (mode == AttestationMode.UNATTESTED) return null

        ensureAttested()

        val clientId = identity.clientId()
        val challenge = api.challenge().challenge
        val signature = keyPairStore.sign(clientId, AttestationSigning.signingPayload(challenge, clientId, body))

        return mapOf(
            HEADER_CLIENT_ID to clientId,
            HEADER_CHALLENGE to challenge,
            HEADER_SIGNATURE to Base64.encodeToString(signature, Base64.NO_WRAP)
        )
    }

    private suspend fun ensureAttested() {
        if (rejected) throw AttestationFailedException("Attestation was rejected by the backend")
        if (identity.isAttested()) return

        registrationMutex.withLock {
            if (identity.isAttested()) return

            val clientId = identity.clientId()
            val challenge = api.challenge().challenge
            val publicKey = Base64.encodeToString(keyPairStore.publicKey(clientId), Base64.NO_WRAP)

            try {
                api.register(
                    AttestationRegisterRequest(
                        client_id = clientId,
                        public_key = publicKey,
                        platform = PLATFORM_ANDROID,
                        app_package = appPackage,
                        attestation_type = mode.wireName,
                        challenge = challenge,
                        integrity_token = integrityToken(challenge, clientId, publicKey)
                    )
                )
            } catch (exception: HttpException) {
                if (exception.code() in 400..499) {
                    rejected = true
                    throw AttestationFailedException("Attestation rejected with HTTP ${exception.code()}")
                }
                throw exception
            }

            identity.markAttested()
        }
    }

    private suspend fun integrityToken(challenge: String, clientId: String, publicKey: String): String? {
        val payload = AttestationSigning.attestationPayload(challenge, clientId, publicKey)

        return when (mode) {
            AttestationMode.PLAY_INTEGRITY -> {
                val requestHash = Base64.encodeToString(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                integrityService.getIntegrityToken(requestHash)
            }

            AttestationMode.SHARED_SECRET -> {
                val secret = requireNotNull(sharedSecret) { "SHARED_SECRET mode requires a secret" }
                Base64.encodeToString(AttestationSigning.sharedSecretToken(secret, payload), Base64.NO_WRAP)
            }

            AttestationMode.UNATTESTED -> null
        }
    }
}
