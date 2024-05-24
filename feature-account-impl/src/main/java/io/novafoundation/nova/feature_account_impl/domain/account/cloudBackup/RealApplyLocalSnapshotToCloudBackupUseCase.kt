package io.novafoundation.nova.feature_account_impl.domain.account.cloudBackup

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackupWithSavedPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isEmpty
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.localVsCloudDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.setLastSyncedTimeAsNow
import io.novafoundation.nova.feature_cloud_backup_api.domain.writeCloudBackupWithSavedPassword

class RealApplyLocalSnapshotToCloudBackupUseCase(
    private val localAccountsCloudBackupFacade: LocalAccountsCloudBackupFacade,
    private val cloudBackupService: CloudBackupService
) : ApplyLocalSnapshotToCloudBackupUseCase {

    override suspend fun applyLocalSnapshotToCloudBackupIfSyncEnabled(): Result<Unit> {
        if (!cloudBackupService.session.isSyncWithCloudEnabled()) return Result.success(Unit)

        return cloudBackupService.fetchAndDecryptExistingBackupWithSavedPassword()
            .flatMap { cloudBackup ->
                val localCloudBackupSnapshot = localAccountsCloudBackupFacade.fullBackupInfoFromLocalSnapshot()
                val diff = localCloudBackupSnapshot.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

                // If there are no changes to apply we can finish it
                if (diff.cloudChanges.isEmpty()) return Result.success(Unit)

                // If we don't have destructive local changes, we can apply the diff to cloud
                if (localAccountsCloudBackupFacade.canPerformNonDestructiveApply(diff)) {
                    cloudBackupService.writeCloudBackupWithSavedPassword(localCloudBackupSnapshot)
                        .onSuccess {
                            cloudBackupService.session.setLastSyncedTimeAsNow()
                        }
                } else {
                    return Result.failure(CannotApplyNonDestructiveDiff(diff, cloudBackup))
                }
            }
    }
}
