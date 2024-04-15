package io.novafoundation.nova.feature_cloud_backup_impl.domain

import android.util.Log
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.mapErrorNotInstance
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.EncryptedCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.DeleteBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.WriteBackupError
import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedPrivateData
import io.novafoundation.nova.feature_cloud_backup_impl.data.ReadyForStorageBackup
import io.novafoundation.nova.feature_cloud_backup_impl.data.SerializedBackup
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.encryption.CloudBackupEncryption
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.CloudBackupPreferences
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.enableSyncWithCloud
import io.novafoundation.nova.feature_cloud_backup_impl.data.serializer.CloudBackupSerializer
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
        storage.ensureUserAuthenticated().flatMap {
            cloudBackupPreferences.enableSyncWithCloud()

            prepareBackupForSaving(request.cloudBackup, request.password)
        }
            .flatMap {
                storage.writeBackup(it)
            }.onFailure {
                Log.e("CloudBackupService", "Failed to write backup to cloud", it)
            }.mapErrorNotInstance<_, WriteBackupError> {
                WriteBackupError.Other
            }
    }

    override suspend fun isCloudBackupExist(): Result<Boolean> = withContext(Dispatchers.IO) {
        storage.ensureUserAuthenticated().flatMap {
            storage.checkBackupExists()
        }
    }

    override suspend fun isSyncWithCloudEnabled(): Boolean {
        return cloudBackupPreferences.syncWithCloudEnabled()
    }

    override suspend fun setSyncingBackupEnabled(enable: Boolean) {
        cloudBackupPreferences.setSyncWithCloudEnabled(enable)
    }

    override suspend fun fetchBackup(): Result<EncryptedCloudBackup> {
        return storage.ensureUserAuthenticated()
            .flatMap { storage.fetchBackup() }
            .flatMap { serializer.deserializePublicData(it) }
            .map { RealEncryptedCloudBackup(encryption, serializer, it) }
            .onFailure {
                Log.e("CloudBackupService", "Failed to read backup from the cloud", it)
            }.mapErrorNotInstance<_, FetchBackupError> {
                FetchBackupError.Other
            }
    }

    override suspend fun deleteBackup(): Result<Unit> {
        return storage.ensureUserAuthenticated().flatMap {
            storage.deleteBackup()
        }.onFailure {
            Log.e("CloudBackupService", "Failed to delete backup from the cloud", it)
        }.mapErrorNotInstance<_, DeleteBackupError> {
            DeleteBackupError.Other
        }
    }

    override fun observeLastSyncedTime(): Flow<Date?> {
        return cloudBackupPreferences.observeLastSyncedTime()
    }

    override suspend fun setLastSyncedTime(date: Date) {
        cloudBackupPreferences.setLastSyncedTime(date)
    }

    private suspend fun prepareBackupForSaving(backup: CloudBackup, password: String): Result<ReadyForStorageBackup> {
        return serializer.serializePrivateData(backup)
            .flatMap { unencryptedBackupData -> encryption.encryptBackup(unencryptedBackupData.privateData, password) }
            .map { SerializedBackup(backup.publicData, it) }
            .flatMap { serializer.serializePublicData(it) }
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

    private class RealEncryptedCloudBackup(
        private val encryption: CloudBackupEncryption,
        private val serializer: CloudBackupSerializer,
        private val encryptedBackup: SerializedBackup<EncryptedPrivateData>
    ) : EncryptedCloudBackup {

        override val publicData: CloudBackup.PublicData = encryptedBackup.publicData

        override suspend fun decrypt(password: String): Result<CloudBackup> {
            return encryption.decryptBackup(encryptedBackup.privateData, password).flatMap { privateData ->
                val unencryptedBackup = SerializedBackup(encryptedBackup.publicData, privateData)

                serializer.deserializePrivateData(unencryptedBackup)
            }
        }
    }
}
