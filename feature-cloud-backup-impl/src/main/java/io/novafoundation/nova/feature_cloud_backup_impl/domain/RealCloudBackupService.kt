package io.novafoundation.nova.feature_cloud_backup_impl.domain

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CreateBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class RealCloudBackupService(
    private val cloudBackupStorage: CloudBackupStorage,
) : CloudBackupService {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus = withContext(Dispatchers.IO) {
        validateCanCreateBackupInternal()
    }

    override suspend fun createBackup(request: CreateBackupRequest): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun isCloudBackupExist(): Result<Boolean> {
        return Result.success(false)
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
        cloudBackupStorage.ensureUserAuthenticated().getOrNull() ?: return PreCreateValidationStatus.AuthenticationFailed

        val fileExists = cloudBackupStorage.checkBackupExists().getOrNull() ?: return PreCreateValidationStatus.OtherError
        if (fileExists) {
            return PreCreateValidationStatus.ExistingBackupFound
        }

        return PreCreateValidationStatus.Ok
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
