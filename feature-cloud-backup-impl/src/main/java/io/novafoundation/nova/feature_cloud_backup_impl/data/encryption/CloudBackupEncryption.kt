package io.novafoundation.nova.feature_cloud_backup_impl.data.encryption

import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedPrivateData
import io.novafoundation.nova.feature_cloud_backup_impl.data.UnencryptedPrivateData
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.encrypt.xsalsa20poly1305.SecretBox
import org.bouncycastle.crypto.generators.SCrypt
import java.security.SecureRandom
import java.util.Random

interface CloudBackupEncryption {

    suspend fun encryptBackup(data: UnencryptedPrivateData, password: String): Result<EncryptedPrivateData>

    /**
     * @throws InvalidBackupPasswordError
     */
    suspend fun decryptBackup(data: EncryptedPrivateData, password: String): Result<UnencryptedPrivateData>
}

class ScryptCloudBackupEncryption : CloudBackupEncryption {

    private val random: Random = SecureRandom()

    companion object {
        private const val SCRYPT_KEY_SIZE = 32
        private const val SALT_SIZE = 32
        private const val NONCE_SIZE = 24

        private const val N = 16384
        private const val p = 1
        private const val r = 8
    }

    override suspend fun encryptBackup(data: UnencryptedPrivateData, password: String): Result<EncryptedPrivateData> {
        return runCatching {
            val salt = generateSalt()
            val encryptionKey = generateScryptKey(password.encodeToByteArray(), salt)
            val plaintext = data.unencryptedData.encodeToByteArray()

            val secretBox = SecretBox(encryptionKey)
            val nonce = secretBox.nonce(plaintext)

            val secret = secretBox.seal(nonce, plaintext)
            val encryptedData = salt + nonce + secret

            EncryptedPrivateData(encryptedData)
        }
    }

    override suspend fun decryptBackup(data: EncryptedPrivateData, password: String): Result<UnencryptedPrivateData> {
        return runCatching {
            val salt = data.encryptedData.copyBytes(from = 0, size = SALT_SIZE)
            val nonce = data.encryptedData.copyBytes(from = SALT_SIZE, size = NONCE_SIZE)
            val encryptedContent = data.encryptedData.dropBytes(SALT_SIZE + NONCE_SIZE)

            val encryptionSecret = generateScryptKey(password.encodeToByteArray(), salt)

            val secret = SecretBox(encryptionSecret).open(nonce, encryptedContent)

            if (secret.isEmpty()) {
                throw InvalidBackupPasswordError()
            }

            UnencryptedPrivateData(secret.decodeToString())
        }
    }

    private fun generateScryptKey(password: ByteArray, salt: ByteArray): ByteArray {
        return SCrypt.generate(password, salt, N, r, p, SCRYPT_KEY_SIZE)
    }

    private fun generateSalt(): ByteArray {
        return ByteArray(SALT_SIZE).also {
            random.nextBytes(it)
        }
    }
}
