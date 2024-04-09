package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo

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

/**
 * Finds the diff between local and cloud versions
 *
 * [this] - Local snapshot
 * [cloudVersion] - Cloud snapshot
 */
fun CloudBackup.PublicData.localVsCloudDiff(cloudVersion: CloudBackup.PublicData): CloudBackupDiff {
    val localVersion = this

    val localToCloudDiff = CollectionDiffer.findDiff(newItems = cloudVersion.wallets, oldItems = localVersion.wallets, forceUseNewItems = false)

    val isCloudInPriority = cloudVersion.isModifiedLaterThan(localVersion)
    val isLocalInPriority = localVersion.isModifiedLaterThan(cloudVersion)

    val unknownWalletsInCloud = localToCloudDiff.added
    val modifiedWallets = localToCloudDiff.updated
    val nonPresentWalletInCloud = localToCloudDiff.removed

    val walletsToAddLocally = unknownWalletsInCloud.takeNonEmptyIf(isCloudInPriority)
    val walletsToRemoveFromCloud = unknownWalletsInCloud.takeNonEmptyIf(isLocalInPriority)

    val walletsToModifyLocally = modifiedWallets.takeNonEmptyIf(isCloudInPriority)
    val walletsToModifyInCloud = modifiedWallets.takeNonEmptyIf(isLocalInPriority)

    val walletToRemoveLocally = nonPresentWalletInCloud.takeNonEmptyIf(isCloudInPriority)
    val walletsToAddToCloud = nonPresentWalletInCloud.takeNonEmptyIf(isLocalInPriority)

    return CloudBackupDiff(
        localChanges = CloudBackupDiff.PerSourceDiff(
            added = walletsToAddLocally,
            modified = walletsToModifyLocally,
            removed = walletToRemoveLocally
        ),
        cloudChanges = CloudBackupDiff.PerSourceDiff(
            added = walletsToAddToCloud,
            modified = walletsToModifyInCloud,
            removed = walletsToRemoveFromCloud
        )
    )
}

private fun <T> List<T>.takeNonEmptyIf(condition: Boolean): List<T> = if (condition) this else emptyList()

private fun CloudBackup.PublicData.isModifiedLaterThan(other: CloudBackup.PublicData): Boolean {
    return modifiedAt > other.modifiedAt
}
