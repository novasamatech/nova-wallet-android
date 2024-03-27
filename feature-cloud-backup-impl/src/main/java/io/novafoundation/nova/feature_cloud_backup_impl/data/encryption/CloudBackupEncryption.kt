package io.novafoundation.nova.feature_cloud_backup_impl.data.encryption

import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedBackupData
import io.novafoundation.nova.feature_cloud_backup_impl.data.UnencryptedBackupData

interface CloudBackupEncryption {

    suspend fun encryptBackup(data: UnencryptedBackupData, password: String): Result<EncryptedBackupData>

    suspend fun decryptBackup(data: EncryptedBackupData, password: String): Result<UnencryptedBackupData>
}

class ScryptCloudBackupEncryption: CloudBackupEncryption {

    // TODO encryption
    override suspend fun encryptBackup(data: UnencryptedBackupData, password: String): Result<EncryptedBackupData> {
        return runCatching {
            EncryptedBackupData(data.decryptedData)
        }
    }

    // TODO encryption
    override suspend fun decryptBackup(data: EncryptedBackupData, password: String): Result<UnencryptedBackupData> {
        return runCatching {
            UnencryptedBackupData(data.encryptedData)
        }
    }
}
