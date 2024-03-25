package io.novafoundation.nova.feature_cloud_backup_api.domain

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CreateBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CreateBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.DeleteBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError

interface CloudBackupService {

    /**
     * Checks conditions for creating initial backup
     */
    suspend fun validateCanCreateBackup(): PreCreateValidationStatus

    /**
     * For initial backup creation
     *
     * @throws CreateBackupError
     */
    suspend fun createBackup(request: CreateBackupRequest): Result<Unit>

    /**
     * Check if backup file exists in the cloud
     */
    suspend fun isCloudBackupExist(): Result<Boolean>

    /**
     * Check if user enabled cloud backup in the current application instance
     */
    suspend fun isCloudBackupActivated(): Result<Boolean>

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
