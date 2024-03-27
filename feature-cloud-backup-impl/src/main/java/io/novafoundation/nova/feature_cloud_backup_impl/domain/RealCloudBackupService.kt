package io.novafoundation.nova.feature_cloud_backup_impl.domain

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CreateBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class RealCloudBackupService : CloudBackupService {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus {
        delay(1.seconds)

        return PreCreateValidationStatus.Ok
    }

    override suspend fun createBackup(request: CreateBackupRequest): Result<Unit> {
        delay(1.seconds)

        return Result.success(Unit)
    }

    override suspend fun isCloudBackupExist(): Result<Boolean> {
        return Result.success(false)
    }

    override suspend fun isCloudBackupActivated(): Result<Boolean> {
        return Result.success(false)
    }

    override suspend fun fetchBackup(): Result<EncryptedCloudBackup> {
        delay(1.seconds)

        return Result.success(StubEncryptedCloudBackup())
    }

    override suspend fun deleteBackup(): Result<Unit> {
        delay(1.seconds)

        return Result.success(Unit)
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
