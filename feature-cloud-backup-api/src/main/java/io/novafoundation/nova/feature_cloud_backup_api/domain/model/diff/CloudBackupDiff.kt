package io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.SourcedBackupChanges.WalletsFromCloud
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategyFactory
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.addToCloud
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.addToLocal
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.modifyInCloud
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.modifyLocally
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.removeFromCloud
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.removeLocally

class CloudBackupDiff(
    val localChanges: PerSourceDiff,
    val cloudChanges: PerSourceDiff
) {

    class PerSourceDiff(
        val added: List<WalletPublicInfo>,
        val modified: List<WalletPublicInfo>,
        val removed: List<WalletPublicInfo>
    )
}

fun CloudBackupDiff.PerSourceDiff.isEmpty(): Boolean = added.isEmpty() && modified.isEmpty() && removed.isEmpty()

fun CloudBackupDiff.PerSourceDiff.isDestructive(): Boolean = removed.isNotEmpty() || modified.isNotEmpty()

fun CloudBackupDiff.PerSourceDiff.isNotDestructive(): Boolean = !isDestructive()

/**
 * @see [CloudBackup.PublicData.localVsCloudDiff]
 */
fun CloudBackup.localVsCloudDiff(
    cloudVersion: CloudBackup,
    strategyFactory: BackupDiffStrategyFactory
): CloudBackupDiff {
    return publicData.localVsCloudDiff(cloudVersion.publicData, strategyFactory)
}

/**
 * Finds the diff between local and cloud versions
 *
 * [this] - Local snapshot
 * [cloudVersion] - Cloud snapshot
 */
fun CloudBackup.PublicData.localVsCloudDiff(
    cloudVersion: CloudBackup.PublicData,
    strategyFactory: BackupDiffStrategyFactory
): CloudBackupDiff {
    val localVersion = this
    val strategy = strategyFactory(localVersion, cloudVersion)

    val localToCloudDiff = CollectionDiffer.findDiff(newItems = cloudVersion.wallets, oldItems = localVersion.wallets, forceUseNewItems = false)

    val walletsOnlyPresentInCloud = localToCloudDiff.added.asCloudWallets()
    val walletsModifiedByCloud = localToCloudDiff.updated.asCloudWallets()
    val walletsOnlyPresentLocally = localToCloudDiff.removed.asLocalWallets()

    val cloudToLocalDiff = CollectionDiffer.findDiff(newItems = localVersion.wallets, oldItems = cloudVersion.wallets, forceUseNewItems = false)
    val walletsModifiedByLocal = cloudToLocalDiff.updated.asLocalWallets()

    return CloudBackupDiff(
        localChanges = CloudBackupDiff.PerSourceDiff(
            added = strategy.addToLocal(walletsOnlyPresentInCloud),
            removed = strategy.removeLocally(walletsOnlyPresentLocally),
            modified = strategy.modifyLocally(walletsModifiedByCloud)
        ),
        cloudChanges = CloudBackupDiff.PerSourceDiff(
            added = strategy.addToCloud(walletsOnlyPresentLocally),
            removed = strategy.removeFromCloud(walletsOnlyPresentInCloud),
            modified = strategy.modifyInCloud(walletsModifiedByLocal)
        )
    )
}

private fun List<WalletPublicInfo>.asCloudWallets(): SourcedBackupChanges<WalletsFromCloud> {
    return SourcedBackupChanges(this)
}

private fun List<WalletPublicInfo>.asLocalWallets(): SourcedBackupChanges<SourcedBackupChanges.LocalWallets> {
    return SourcedBackupChanges(this)
}
