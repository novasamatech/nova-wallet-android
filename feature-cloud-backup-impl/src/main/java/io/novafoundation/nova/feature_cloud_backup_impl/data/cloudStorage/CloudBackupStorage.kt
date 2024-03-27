package io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage

import io.novafoundation.nova.common.utils.InformationSize

internal interface CloudBackupStorage {

    suspend fun hasEnoughFreeStorage(neededSize: InformationSize): Result<Boolean>

    suspend fun isCloudStorageServiceAvailable(): Boolean

    suspend fun isUserAuthenticated(): Boolean

    suspend fun authenticateUser(): Result<Unit>

    suspend fun checkBackupExists(): Result<Boolean>
}
