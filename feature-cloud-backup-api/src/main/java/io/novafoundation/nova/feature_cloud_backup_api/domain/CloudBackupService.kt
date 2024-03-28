package io.novafoundation.nova.feature_cloud_backup_api.domain

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.DeleteBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.WriteBackupError

interface CloudBackupService {

    /**
     * Checks conditions for creating initial backup
     */
    suspend fun validateCanCreateBackup(): PreCreateValidationStatus

    /**
     * Writes a backup to the cloud, overwriting already existing one
     *
     * @throws WriteBackupError
     */
    suspend fun writeBackupToCloud(request: WriteBackupRequest): Result<Unit>

    /**
     * Check if backup file exists in the cloud
     */
    suspend fun isCloudBackupExist(): Result<Boolean>

    /**
     * Check if user enabled sync with cloud backup in the current application instance
     * Enabling this means that the app should write changes of local state to backup on addition, modification or delegation of accounts
     */
    suspend fun isSyncWithCloudEnabled(): Boolean

    /**
     * @throws FetchBackupError
     */
    suspend fun fetchBackup(): Result<EncryptedCloudBackup>

    /**
     * @throws DeleteBackupError
     */
    suspend fun deleteBackup(): Result<Unit>
}

suspend fun CloudBackupService.fetchAndDecryptExistingBackup(password: String): Result<CloudBackup> {
    return fetchBackup().flatMap { it.decrypt(password) }
}
