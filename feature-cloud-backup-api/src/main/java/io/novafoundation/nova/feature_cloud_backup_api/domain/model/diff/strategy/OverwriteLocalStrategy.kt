package io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges

/**
 * Strategy that considers cloud version to be the source of truth
 */
class OverwriteLocalStrategy : BackupDiffStrategy {

    override fun shouldAddLocally(walletsOnlyPresentInCloud: SourcedBackupChanges<SourcedBackupChanges.WalletsFromCloud>): Boolean {
        return true
    }

    override fun shouldRemoveFromCloud(walletsOnlyPresentInCloud: SourcedBackupChanges<SourcedBackupChanges.WalletsFromCloud>): Boolean {
        return false
    }

    override fun shouldRemoveLocally(walletsOnlyPresentLocally: SourcedBackupChanges<SourcedBackupChanges.LocalWallets>): Boolean {
        return true
    }

    override fun shouldAddToCloud(walletsOnlyPresentLocally: SourcedBackupChanges<SourcedBackupChanges.LocalWallets>): Boolean {
       return false
    }

    override fun shouldModifyLocally(modifiedInCloud: SourcedBackupChanges<SourcedBackupChanges.WalletsFromCloud>): Boolean {
       return true
    }

    override fun shouldModifyInCloud(modifiedLocally: SourcedBackupChanges<SourcedBackupChanges.LocalWallets>): Boolean {
       return false
    }
}
