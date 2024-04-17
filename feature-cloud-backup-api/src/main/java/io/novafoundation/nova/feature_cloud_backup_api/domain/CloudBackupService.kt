package io.novafoundation.nova.feature_cloud_backup_api.domain

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.DeleteBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.WriteBackupError

/**
 * Manages cloud backup storage, serialization and encryption
 *
 * The storing pipeline is the following:
 *
 * Backup -> serialize private data -> encrypt private data -> serialize public data + encrypted private
 *
 * This allows to access public data without accessing password
 */
interface CloudBackupService {

    /**
     * Current user preferences, state known for cloud backup related functionality
     */
    val session: CloudBackupSession

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
     * @throws FetchBackupError
     */
    suspend fun fetchBackup(): Result<EncryptedCloudBackup>

    /**
     * @throws DeleteBackupError
     */
    suspend fun deleteBackup(): Result<Unit>
}

/**
 * @throws FetchBackupError
 * @throws InvalidBackupPasswordError
 */
suspend fun CloudBackupService.fetchAndDecryptExistingBackup(password: String): Result<CloudBackup> {
    return fetchBackup().flatMap { it.decrypt(password) }
}

/**
 * @throws PasswordNotSaved
 * @throws FetchBackupError
 * @throws InvalidBackupPasswordError
 */
suspend fun CloudBackupService.fetchAndDecryptExistingBackupWithSavedPassword(): Result<CloudBackup> {
    return fetchBackup()
        .flatMap { encryptedBackup ->
            val password = session.getSavedPassword().getOrThrow()
            encryptedBackup.decrypt(password)
        }
}
