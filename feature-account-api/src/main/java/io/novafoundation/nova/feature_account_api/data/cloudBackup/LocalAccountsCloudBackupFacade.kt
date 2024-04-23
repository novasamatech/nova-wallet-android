package io.novafoundation.nova.feature_account_api.data.cloudBackup

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.localVsCloudDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategyFactory
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface LocalAccountsCloudBackupFacade {

    /**
     * Constructs full backup instance, including sensitive information
     * Should only be used when full backup instance is needed, for example when writing backup to cloud. Otherwise use [publicBackupInfoFromLocalSnapshot]
     *
     * Important note: Should only be called as the result of direct user interaction!
     * We don't want to exposure secrets to RAM until user explicitly directs app to do so
     */
    suspend fun fullBackupInfoFromLocalSnapshot(): CloudBackup

    /**
     * Constructs partial backup instance, including only the public information (addresses, metadata e.t.c)
     *
     * Can be used without direct user interaction (e.g. in background) to compare backup states between local and remote sources
     */
    suspend fun publicBackupInfoFromLocalSnapshot(): CloudBackup.PublicData

    /**
     * Creates a backup from external input. Useful for creating initial backup
     */
    suspend fun createCloudBackupFromInput(
        modificationTime: Long,
        metaAccount: MetaAccountLocal,
        chainAccounts: List<ChainAccountLocal>,
        baseSecrets: EncodableStruct<MetaAccountSecrets>,
        chainAccountSecrets: Map<String, EncodableStruct<ChainAccountSecrets>>,
        additionalSecrets: Map<String, String>
    ): CloudBackup

    /**
     * Check if it is possible to apply given [diff] to local state in non-destructive manner
     * In other words, whether it is possible to apply backup without notifying the user
     */
    suspend fun canPerformNonDestructiveApply(diff: CloudBackupDiff): Boolean

    /**
     * Applies cloud version of the backup to the local state.
     * This is a destructive action as may overwrite or delete secrets stored in the app
     *
     * Important note: Should only be called as the result of direct user interaction!
     */
    suspend fun applyBackupDiff(diff: CloudBackupDiff, cloudVersion: CloudBackup)
}

/**
 * Attempts to apply cloud backup version to current local application state in non-destructive manner
 * Will do nothing if it is not possible to apply changes in non-destructive manner
 *
 * @return whether the attempt succeeded
 */
suspend fun LocalAccountsCloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(
    cloudVersion: CloudBackup,
    diffStrategy: BackupDiffStrategyFactory
): CloudBackupDiff {
    val localSnapshot = publicBackupInfoFromLocalSnapshot()
    val diff = localSnapshot.localVsCloudDiff(cloudVersion.publicData, diffStrategy)

    return if (canPerformNonDestructiveApply(diff)) {
        applyBackupDiff(diff, cloudVersion)

        diff
    } else {
        throw CannotApplyNonDestructiveDiff(diff)
    }
}
