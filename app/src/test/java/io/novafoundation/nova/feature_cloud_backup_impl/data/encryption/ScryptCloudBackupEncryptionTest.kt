package io.novafoundation.nova.feature_cloud_backup_impl.data.encryption

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_impl.data.UnencryptedBackupData
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ScryptCloudBackupEncryptionTest {

    private val encryption = ScryptCloudBackupEncryption()

    @Test
    fun shouldEncryptAndDecryptToTheSameValue() = runBlocking {
        val plaintext = "Test"
        val password = "12345"

        val encrypted = encryption.encryptBackup(UnencryptedBackupData(plaintext), password)
        assert(encrypted.isSuccess)
        val decrypted = encryption.decryptBackup(encrypted.getOrThrow(), password)
        assert(decrypted.isSuccess)
        assertEquals(plaintext, decrypted.getOrThrow().decryptedData)
    }

    @Test(expected = InvalidBackupPasswordError::class)
    fun shouldFailOnWrongPassword() {
        runBlocking {
            val plaintext = "Test"
            val password = "12345"
            val wrongPassword = "1234"

            val encrypted = encryption.encryptBackup(UnencryptedBackupData(plaintext), password)
            val decrypted = encryption.decryptBackup(encrypted.getOrThrow(), wrongPassword)

            decrypted.getOrThrow()
        }
    }
}
