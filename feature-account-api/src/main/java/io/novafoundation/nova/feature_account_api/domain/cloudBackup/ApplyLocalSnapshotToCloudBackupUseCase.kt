package io.novafoundation.nova.feature_account_api.domain.cloudBackup

interface ApplyLocalSnapshotToCloudBackupUseCase {

    suspend fun applyLocalSnapshotToCloudBackupIfSyncEnabled(): Result<Unit>
}
