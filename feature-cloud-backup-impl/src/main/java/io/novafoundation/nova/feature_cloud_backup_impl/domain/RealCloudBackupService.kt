package io.novafoundation.nova.feature_cloud_backup_impl.domain

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CreateBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.serializer.CloudBackupSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class RealCloudBackupService(
    private val cloudBackupStorage: CloudBackupStorage,
    private val cloudBackupSerializer: CloudBackupSerializer
) : CloudBackupService {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus = withContext(Dispatchers.IO) {
        validateCanCreateBackupInternal()
    }

    override suspend fun createBackup(request: CreateBackupRequest): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun isCloudBackupExist(): Result<Boolean> = withContext(Dispatchers.IO) {
        cloudBackupStorage.checkBackupExists()
    }

    override suspend fun isCloudBackupActivated(): Result<Boolean> {
        return Result.success(false)
    }

    override suspend fun fetchBackup(): Result<EncryptedCloudBackup> {
        return Result.success(StubEncryptedCloudBackup())
    }

    override suspend fun deleteBackup(): Result<Unit> {
        return Result.success(Unit)
    }

    private suspend fun CloudBackupStorage.ensureUserAuthenticated(): Result<Unit> {
        return if (!isUserAuthenticated()) {
            authenticateUser()
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun validateCanCreateBackupInternal(): PreCreateValidationStatus {
        if (!cloudBackupStorage.isCloudStorageServiceAvailable()) return PreCreateValidationStatus.BackupServiceUnavailable

        cloudBackupStorage.ensureUserAuthenticated().getOrNull() ?: return PreCreateValidationStatus.AuthenticationFailed

        val fileExists = cloudBackupStorage.checkBackupExists().getOrNull() ?: return PreCreateValidationStatus.OtherError
        if (fileExists) {
            return PreCreateValidationStatus.ExistingBackupFound
        }

        val hasEnoughSize = hasEnoughSizeForBackup().getOrNull() ?: return PreCreateValidationStatus.OtherError
        if (!hasEnoughSize) {
            return PreCreateValidationStatus.NotEnoughSpace
        }

        return PreCreateValidationStatus.Ok
    }

    private suspend fun hasEnoughSizeForBackup(): Result<Boolean> {
        val neededBackupSize = cloudBackupSerializer.neededSizeForBackup()

        return cloudBackupStorage.hasEnoughFreeStorage(neededBackupSize)
    }

    private class StubEncryptedCloudBackup : EncryptedCloudBackup {

        override suspend fun decrypt(password: String): Result<CloudBackup> {
            delay(100.milliseconds)

            return Result.success(
                CloudBackup(
                    modifiedAt = System.currentTimeMillis(),
                    wallets = emptyList()
                )
            )
        }
    }
}
