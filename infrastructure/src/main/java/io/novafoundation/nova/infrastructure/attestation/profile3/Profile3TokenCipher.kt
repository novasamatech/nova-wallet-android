package io.novafoundation.nova.infrastructure.attestation.profile3

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface Profile3TokenCipher {
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray

    fun decrypt(encrypted: ByteArray, associatedData: ByteArray): ByteArray
}

interface Profile3TokenEncryptionKey {
    fun getOrCreate(): SecretKey

    fun existing(): SecretKey?
}

class AndroidProfile3TokenEncryptionKey : Profile3TokenEncryptionKey {

    @Synchronized
    override fun getOrCreate(): SecretKey {
        existing()?.let { return it }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).run {
            init(spec)
            generateKey()
        }
    }

    override fun existing(): SecretKey? {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return store.getKey(ALIAS, null) as? SecretKey
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "nova.attestation.profile3.token-encryption.v1"
    }
}

class AesGcmProfile3TokenCipher(private val key: Profile3TokenEncryptionKey) : Profile3TokenCipher {

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key.getOrCreate())
        cipher.updateAAD(associatedData)
        require(cipher.iv.size == NONCE_BYTES) { "Invalid token encryption nonce" }
        return cipher.iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(encrypted: ByteArray, associatedData: ByteArray): ByteArray {
        require(encrypted.size >= NONCE_BYTES + TAG_BYTES) { "Invalid encrypted token" }
        val existingKey = requireNotNull(key.existing()) { "Token encryption key unavailable" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, existingKey, GCMParameterSpec(TAG_BYTES * 8, encrypted.copyOfRange(0, NONCE_BYTES)))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(encrypted, NONCE_BYTES, encrypted.size - NONCE_BYTES)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BYTES = 16
    }
}
