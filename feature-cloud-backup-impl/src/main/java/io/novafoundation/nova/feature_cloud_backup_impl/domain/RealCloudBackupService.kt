package io.novafoundation.nova.feature_cloud_backup_impl.domain

import android.util.Log
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedBackupData
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.encryption.CloudBackupEncryption
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.CloudBackupPreferences
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.enableSyncWithCloud
import io.novafoundation.nova.feature_cloud_backup_impl.data.serializer.CloudBackupSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class RealCloudBackupService(
    private val storage: CloudBackupStorage,
    private val serializer: CloudBackupSerializer,
    private val encryption: CloudBackupEncryption,
    private val cloudBackupPreferences: CloudBackupPreferences,
) : CloudBackupService {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus = withContext(Dispatchers.IO) {
        validateCanCreateBackupInternal()
    }

    override suspend fun writeBackupToCloud(request: WriteBackupRequest): Result<Unit> = withContext(Dispatchers.IO) {
        storage.ensureUserAuthenticated()
            .flatMap {
                cloudBackupPreferences.enableSyncWithCloud()

                prepareBackupForSaving(request.cloudBackup, request.password)
            }
            .flatMap {
                storage.writeBackup(it)
            }.onFailure {
                Log.w("CloudBackupService", it)
            }
    }

    override suspend fun isCloudBackupExist(): Result<Boolean> = withContext(Dispatchers.IO) {
        storage.checkBackupExists()
    }

    override suspend fun isSyncWithCloudEnabled(): Boolean {
        return cloudBackupPreferences.syncWithCloudEnabled()
    }

    override suspend fun fetchBackup(): Result<EncryptedCloudBackup> {
        return Result.success(StubEncryptedCloudBackup())
    }

    override suspend fun deleteBackup(): Result<Unit> {
        return Result.success(Unit)
    }

    private suspend fun prepareBackupForSaving(backup: CloudBackup, password: String): Result<EncryptedBackupData> {
        return serializer.serializeBackup(backup).flatMap { unencryptedBackupData ->
            encryption.encryptBackup(unencryptedBackupData, password)
        }
    }

    private suspend fun CloudBackupStorage.ensureUserAuthenticated(): Result<Unit> {
        return if (!isUserAuthenticated()) {
            authenticateUser()
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun validateCanCreateBackupInternal(): PreCreateValidationStatus {
        if (!storage.isCloudStorageServiceAvailable()) return PreCreateValidationStatus.BackupServiceUnavailable

        storage.ensureUserAuthenticated().getOrNull() ?: return PreCreateValidationStatus.AuthenticationFailed

        val fileExists = storage.checkBackupExists().getOrNull() ?: return PreCreateValidationStatus.OtherError
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
        val neededBackupSize = serializer.neededSizeForBackup()

        return storage.hasEnoughFreeStorage(neededBackupSize)
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
