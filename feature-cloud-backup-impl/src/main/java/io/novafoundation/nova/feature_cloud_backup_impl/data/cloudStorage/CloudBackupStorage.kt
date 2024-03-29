package io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage

import io.novafoundation.nova.common.utils.InformationSize
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_impl.data.ReadyForStorageBackup

internal interface CloudBackupStorage {

    suspend fun hasEnoughFreeStorage(neededSize: InformationSize): Result<Boolean>

    suspend fun isCloudStorageServiceAvailable(): Boolean

    suspend fun isUserAuthenticated(): Boolean

    suspend fun authenticateUser(): Result<Unit>

    suspend fun checkBackupExists(): Result<Boolean>

    suspend fun writeBackup(backup: ReadyForStorageBackup): Result<Unit>

    /**
     * @throws FetchBackupError.BackupNotFound
     */
    suspend fun fetchBackup(): Result<ReadyForStorageBackup>

    suspend fun deleteBackup(): Result<Unit>
}
