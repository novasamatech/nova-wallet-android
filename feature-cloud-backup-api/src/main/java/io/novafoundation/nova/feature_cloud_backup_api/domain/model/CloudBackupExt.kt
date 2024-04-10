package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.BackupPriorityResolutionStrategy.Priority
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

fun interface BackupPriorityResolutionStrategy {

    companion object {

        fun byModifiedAt() = BackupPriorityResolutionStrategy { local, cloud ->
            when {
                cloud.isModifiedLaterThan(local) -> Priority.CLOUD
                local.isModifiedLaterThan(cloud) -> Priority.LOCAL
                else -> Priority.EQUAL
            }
        }

        fun alwaysCloud() = BackupPriorityResolutionStrategy { _, _ ->
            Priority.CLOUD
        }
    }

    enum class Priority {
        LOCAL, CLOUD, EQUAL
    }

    fun resolvePriority(localBackup: CloudBackup.PublicData, cloudBackup: CloudBackup.PublicData): Priority
}

/**
 * @see [CloudBackup.PublicData.localVsCloudDiff]
 */
fun CloudBackup.localVsCloudDiff(
    cloudVersion: CloudBackup,
    priorityResolution: BackupPriorityResolutionStrategy = BackupPriorityResolutionStrategy.byModifiedAt()
): CloudBackupDiff {
    return publicData.localVsCloudDiff(cloudVersion.publicData, priorityResolution)
}

/**
 * Finds the diff between local and cloud versions
 *
 * [this] - Local snapshot
 * [cloudVersion] - Cloud snapshot
 */
fun CloudBackup.PublicData.localVsCloudDiff(
    cloudVersion: CloudBackup.PublicData,
    priorityResolution: BackupPriorityResolutionStrategy = BackupPriorityResolutionStrategy.byModifiedAt()
): CloudBackupDiff {
    val localVersion = this

    val priority = priorityResolution.resolvePriority(localVersion, cloudVersion)

    return when(priority) {
        Priority.LOCAL -> {
            // modified, added: local, deleted: cloud
            val cloudToLocalDiff = CollectionDiffer.findDiff(newItems = localVersion.wallets, oldItems = cloudVersion.wallets, forceUseNewItems = false)

            CloudBackupDiff(
                localChanges = CloudBackupDiff.PerSourceDiff(
                    added = emptyList(),
                    modified = emptyList(),
                    removed = emptyList()
                ),
                cloudChanges = CloudBackupDiff.PerSourceDiff(
                    added = cloudToLocalDiff.added,
                    modified = cloudToLocalDiff.updated,
                    removed = cloudToLocalDiff.removed
                )
            )
        }

        Priority.CLOUD -> {
            // modified, added: cloud, deleted: local
            val localToCloudDiff = CollectionDiffer.findDiff(newItems = cloudVersion.wallets, oldItems = localVersion.wallets, forceUseNewItems = false)

            CloudBackupDiff(
                localChanges = CloudBackupDiff.PerSourceDiff(
                    added = localToCloudDiff.added,
                    modified = localToCloudDiff.updated,
                    removed = localToCloudDiff.removed
                ),
                cloudChanges = CloudBackupDiff.PerSourceDiff(
                    added = emptyList(),
                    modified = emptyList(),
                    removed = emptyList()
                )
            )
        }
        Priority.EQUAL -> CloudBackupDiff(
            localChanges = CloudBackupDiff.PerSourceDiff(
                added = emptyList(),
                modified =  emptyList(),
                removed =  emptyList()
            ),
            cloudChanges = CloudBackupDiff.PerSourceDiff(
                added =  emptyList(),
                modified =  emptyList(),
                removed =  emptyList()
            )
        )
    }
}

private fun CloudBackup.PublicData.isModifiedLaterThan(other: CloudBackup.PublicData): Boolean {
    return modifiedAt > other.modifiedAt
}
