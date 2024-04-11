package io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges.LocalWallets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges.WalletsFromCloud

/**
 * Strategy that uses modifiedAt timestamps to identify priority
 */
class SyncWithCloudStrategy(
    localTimestamp: Long,
    cloudTimestamp: Long
): BackupDiffStrategy {

    private val cloudInPriority = cloudTimestamp > localTimestamp
    private val localInPriority = localTimestamp > cloudTimestamp

    override fun shouldAddLocally(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean {
        return cloudInPriority
    }

    override fun shouldRemoveFromCloud(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean {
        return localInPriority
    }

    override fun shouldRemoveLocally(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): Boolean {
       return cloudInPriority
    }

    override fun shouldAddToCloud(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): Boolean {
       return localInPriority
    }

    override fun shouldModifyLocally(modifiedInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean {
        return cloudInPriority
    }

    override fun shouldModifyInCloud(modifiedLocally: SourcedBackupChanges<LocalWallets>): Boolean {
        return localInPriority
    }
}
