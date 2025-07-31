package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

object IntegrityCheckKeyPairGenerator {

    fun isKeyPairGenerated(alias: String): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.containsAlias(alias)
    }

    fun generateKeyPair(alias: String) {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            setUserAuthenticationRequired(false)
            build()
        }

        keyPairGenerator.initialize(parameterSpec)

        keyPairGenerator.generateKeyPair()
    }

    fun getPublicKey(alias: String): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getCertificate(alias).publicKey.encoded
    }

    fun signData(alias: String, data: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(alias, null) as PrivateKey

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)

        return signature.sign()
    }

    fun verifySignature(alias: String, data: ByteArray, signatureBytes: ByteArray): Boolean {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val publicKey = keyStore.getCertificate(alias).publicKey

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initVerify(publicKey)
        signature.update(data)

        return signature.verify(signatureBytes)
    }

    fun verifySignature(publicKeyBytes: ByteArray, data: ByteArray, signatureBytes: ByteArray): Boolean {
        val keyFactory = KeyFactory.getInstance("EC")
        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initVerify(publicKey)
        signature.update(data)

        return signature.verify(signatureBytes)
    }
}

fun IntegrityCheckKeyPairGenerator.ensureKeyPairGenerated(alias: String) {
    if (!isKeyPairGenerated(alias)) {
        generateKeyPair(alias)
    }
}
