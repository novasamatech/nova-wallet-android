package io.novafoundation.nova.feature_cloud_backup_api.domain.model

interface EncryptedCloudBackup {

    suspend fun decrypt(password: String): Result<CloudBackup>
}
