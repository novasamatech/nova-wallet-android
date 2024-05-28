package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError

interface EncryptedCloudBackup {

    val publicData: CloudBackup.PublicData

    /**
     * @throws InvalidBackupPasswordError
     */
    suspend fun decrypt(password: String): Result<CloudBackup>
}
