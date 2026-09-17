package io.novafoundation.nova.infrastructure.attestation

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val CURVE = "secp256r1"
private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

interface AttestationKeyPairStore {

    fun publicKey(alias: String): ByteArray

    fun sign(alias: String, payload: ByteArray): ByteArray

    fun delete(alias: String)
}

class RealAttestationKeyPairStore : AttestationKeyPairStore {

    override fun publicKey(alias: String): ByteArray {
        ensureGenerated(alias)

        return keyStore().getCertificate(alias).publicKey.encoded
    }

    override fun sign(alias: String, payload: ByteArray): ByteArray {
        ensureGenerated(alias)

        val privateKey = keyStore().getKey(alias, null) as PrivateKey

        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(privateKey)
            update(payload)
            sign()
        }
    }

    override fun delete(alias: String) {
        val keyStore = keyStore()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun ensureGenerated(alias: String) {
        if (keyStore().containsAlias(alias)) return

        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER).run {
            initialize(spec)
            generateKeyPair()
        }
    }

    private fun keyStore() = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
}
