package io.novafoundation.nova.feature_cloud_backup_impl.data.encryption

import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedBackupData
import io.novafoundation.nova.feature_cloud_backup_impl.data.UnencryptedBackupData
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.encrypt.xsalsa20poly1305.SecretBox
import org.bouncycastle.crypto.generators.SCrypt
import java.security.SecureRandom
import java.util.Random

interface CloudBackupEncryption {

    suspend fun encryptBackup(data: UnencryptedBackupData, password: String): Result<EncryptedBackupData>

    /**
     * @throws InvalidBackupPasswordError
     */
    suspend fun decryptBackup(data: EncryptedBackupData, password: String): Result<UnencryptedBackupData>
}

class ScryptCloudBackupEncryption: CloudBackupEncryption {

    private val random: Random = SecureRandom()

    companion object {
        private const val SCRYPT_KEY_SIZE = 32
        private const val SALT_SIZE = 32
        private const val NONCE_SIZE = 24

        private const val N = 16384
        private const val p = 1
        private const val r = 8
    }


    override suspend fun encryptBackup(data: UnencryptedBackupData, password: String): Result<EncryptedBackupData> {
        return runCatching {
            val salt = generateSalt()
            val encryptionKey = generateScryptKey(password.encodeToByteArray(), salt)
            val plaintext = data.decryptedData.encodeToByteArray()

            val secretBox = SecretBox(encryptionKey)
            val nonce = secretBox.nonce(plaintext)

            val secret = secretBox.seal(nonce, plaintext)
            val encryptedData = salt + nonce + secret

            EncryptedBackupData(encryptedData)
        }
    }

    override suspend fun decryptBackup(data: EncryptedBackupData, password: String): Result<UnencryptedBackupData> {
        return runCatching {
            val salt = data.encryptedData.copyBytes(from = 0, size = SALT_SIZE)
            val nonce = data.encryptedData.copyBytes(from = SALT_SIZE, size = NONCE_SIZE)
            val encryptedContent = data.encryptedData.dropBytes(SALT_SIZE + NONCE_SIZE)

            val encryptionSecret = generateScryptKey(password.encodeToByteArray(), salt)

            val secret = SecretBox(encryptionSecret).open(nonce, encryptedContent)

            if (secret.isEmpty()) {
                throw InvalidBackupPasswordError()
            }

            UnencryptedBackupData(secret.decodeToString())
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
