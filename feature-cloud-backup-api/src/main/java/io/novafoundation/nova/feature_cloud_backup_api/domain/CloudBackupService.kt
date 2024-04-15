package io.novafoundation.nova.feature_cloud_backup_api.domain

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.DeleteBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.WriteBackupError
import java.util.Date
import kotlinx.coroutines.flow.Flow

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

    suspend fun setSyncingBackupEnabled(enable: Boolean)

    /**
     * @throws FetchBackupError
     */
    suspend fun fetchBackup(): Result<EncryptedCloudBackup>

    /**
     * @throws DeleteBackupError
     */
    suspend fun deleteBackup(): Result<Unit>

    /**
     * Observe last synced backup time on device
     */
    fun observeLastSyncedTime(): Flow<Date?>

    suspend fun setLastSyncedTime(date: Date)
}

suspend fun CloudBackupService.fetchAndDecryptExistingBackup(password: String): Result<CloudBackup> {
    return fetchBackup().flatMap { it.decrypt(password) }
}
