package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff

sealed class BackupSyncOutcome {

    object Ok : BackupSyncOutcome()

    object EmptyPassword : BackupSyncOutcome()

    object UnknownPassword : BackupSyncOutcome()

    class DestructiveDiff(val cloudBackupDiff: CloudBackupDiff, val cloudBackup: CloudBackup) : BackupSyncOutcome()

    object StorageAuthFailed : BackupSyncOutcome()

    object CorruptedBackup : BackupSyncOutcome()

    object UnknownError : BackupSyncOutcome()
}

fun BackupSyncOutcome.isError(): Boolean {
    return this != BackupSyncOutcome.Ok
}
