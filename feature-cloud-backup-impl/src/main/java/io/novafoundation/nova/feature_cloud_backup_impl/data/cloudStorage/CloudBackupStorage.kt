package io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage

internal interface CloudBackupStorage {

    suspend fun isCloudStorageServiceAvailable(): Boolean

    suspend fun isUserAuthenticated(): Boolean

    suspend fun authenticateUser(): Result<Unit>

    suspend fun checkBackupExists(): Result<Boolean>
}
