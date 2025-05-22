package io.novafoundation.nova.feature_account_migration.utils.common

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_KEY_SIZE = 32 // 256 bits
private const val AES_GCM_IV_LENGTH = 12
private const val AES_GCM_TAG_LENGTH = 128

class KeyExchangeUtils {

    // EC Curve specification
    private val ecSpec = ECGenParameterSpec("secp256r1")

    fun generateEphemeralKeyPair(): KeyPair {
        val keyPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ecSpec, SecureRandom())
            generateKeyPair()
        }

        return keyPair
    }

    fun encrypt(encryptionData: ByteArray, keypair: KeyPair, publicKey: PublicKey): ByteArray {
        val sharedSecret = getSharedSecret(keypair, publicKey)
        val keySpec = deriveAESKeyFromSharedSecret(sharedSecret)

        val iv = ByteArray(AES_GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = getCypher()
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(encryptionData)
        return iv + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, keypair: KeyPair, publicKey: PublicKey): ByteArray {
        val sharedSecret = getSharedSecret(keypair, publicKey)
        val keySpec = deriveAESKeyFromSharedSecret(sharedSecret)

        val iv = encryptedData.copyOfRange(0, AES_GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(AES_GCM_IV_LENGTH, encryptedData.size)

        val cipher = getCypher()
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    fun mapPublicKeyFromBytes(publicKeyBytes: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance("EC")
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        return keyFactory.generatePublic(keySpec)
    }

    private fun getSharedSecret(keypair: KeyPair, peerPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(keypair.private)
        keyAgreement.doPhase(peerPublicKey, true)

        return keyAgreement.generateSecret()
    }

    private fun deriveAESKeyFromSharedSecret(sharedSecret: ByteArray): SecretKeySpec {
        val mac = Mac.getInstance("HmacSHA256")
        val salt = "ephemeral-salt".toByteArray()
        val keySpec = SecretKeySpec(salt, "HmacSHA256")
        mac.init(keySpec)
        val prk = mac.doFinal(sharedSecret)

        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val info = ByteArray(0) // We can set purpose of using this key and make it different for same shared secret depends on info
        val t1 = mac.doFinal(info + 0x01.toByte())
        val aesKey = t1.copyOf(AES_KEY_SIZE)

        return SecretKeySpec(aesKey, "AES")
    }

    private fun getCypher() = Cipher.getInstance("AES/GCM/NoPadding")
}

fun PublicKey.bytes(): ByteArray {
    return this.encoded
}
