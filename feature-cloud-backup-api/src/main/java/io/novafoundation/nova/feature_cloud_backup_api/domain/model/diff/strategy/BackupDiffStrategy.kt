package io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy


import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges.LocalWallets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges.WalletsFromCloud

typealias BackupDiffStrategyFactory = (localData: CloudBackup.PublicData, remoteData: CloudBackup.PublicData) -> BackupDiffStrategy

interface BackupDiffStrategy {

    companion object {

        fun importFromCloud(): BackupDiffStrategyFactory = { _, _ ->
            ImportFromCloudStrategy()
        }

        fun syncWithCloud(): BackupDiffStrategyFactory = { local, remote ->
            SyncWithCloudStrategy(localTimestamp = local.modifiedAt, cloudTimestamp = remote.modifiedAt)
        }

        fun overwriteLocal(): BackupDiffStrategyFactory = { _, _ ->
            OverwriteLocalStrategy()
        }
    }

    fun shouldAddLocally(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean

    fun shouldRemoveFromCloud(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean

    fun shouldRemoveLocally(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): Boolean

    fun shouldAddToCloud(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): Boolean

    fun shouldModifyLocally(modifiedInCloud: SourcedBackupChanges<WalletsFromCloud>): Boolean

    fun shouldModifyInCloud(modifiedLocally: SourcedBackupChanges<LocalWallets>): Boolean
}

fun BackupDiffStrategy.addToLocal(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): List<CloudBackup.WalletPublicInfo> {
    return walletsOnlyPresentInCloud.takeValueOrEmpty(shouldAddLocally(walletsOnlyPresentInCloud))
}

fun BackupDiffStrategy.removeFromCloud(walletsOnlyPresentInCloud: SourcedBackupChanges<WalletsFromCloud>): List<CloudBackup.WalletPublicInfo> {
    return walletsOnlyPresentInCloud.takeValueOrEmpty(shouldRemoveFromCloud(walletsOnlyPresentInCloud))
}

fun BackupDiffStrategy.removeLocally(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): List<CloudBackup.WalletPublicInfo> {
    return walletsOnlyPresentLocally.takeValueOrEmpty(shouldRemoveLocally(walletsOnlyPresentLocally))
}

fun BackupDiffStrategy.addToCloud(walletsOnlyPresentLocally: SourcedBackupChanges<LocalWallets>): List<CloudBackup.WalletPublicInfo> {
    return walletsOnlyPresentLocally.takeValueOrEmpty(shouldAddToCloud(walletsOnlyPresentLocally))
}

fun BackupDiffStrategy.modifyLocally(modifiedInCloud: SourcedBackupChanges<WalletsFromCloud>): List<CloudBackup.WalletPublicInfo> {
    return modifiedInCloud.takeValueOrEmpty(shouldModifyLocally(modifiedInCloud))
}

fun BackupDiffStrategy.modifyInCloud(modifiedLocally: SourcedBackupChanges<LocalWallets>): List<CloudBackup.WalletPublicInfo> {
    return modifiedLocally.takeValueOrEmpty(shouldModifyInCloud(modifiedLocally))
}

private fun SourcedBackupChanges<*>.takeValueOrEmpty(condition: Boolean): List<CloudBackup.WalletPublicInfo> {
    return if (condition) {
        changes
    } else {
        emptyList()
    }
}
