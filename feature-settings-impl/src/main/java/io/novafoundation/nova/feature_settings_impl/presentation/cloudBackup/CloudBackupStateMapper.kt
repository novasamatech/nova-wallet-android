package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.common.utils.formatting.formatTime
import io.novafoundation.nova.feature_settings_impl.R
import java.util.Date

fun mapCloudBackupStateModel(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    syncOutcome: BackupSyncOutcome,
    lastSync: Date?
): CloudBackupStateModel {
    val (stateImage, stateImageTint, stateBackgroundColor) = mapStateImage(backupEnabled, syncOutcome)

    return CloudBackupStateModel(
        stateImg = stateImage,
        stateImageTint = stateImageTint,
        stateColorBackgroundRes = stateBackgroundColor,
        showProgress = syncingInProgress,
        title = mapCloudBackupStateTitle(resourceManager, backupEnabled, syncingInProgress, syncOutcome),
        subtitle = mapCloudBackupStateSubtitle(resourceManager, backupEnabled, lastSync),
        isClickable = mapCloudBackupClickability(backupEnabled, syncingInProgress, syncOutcome),
        problemButtonText = mapCloudBackupProblemButton(resourceManager, backupEnabled, syncingInProgress, syncOutcome)
    )
}

private fun mapStateImage(backupEnabled: Boolean, syncOutcome: BackupSyncOutcome) = when {
    !backupEnabled -> Triple(R.drawable.ic_cloud_backup_status_disabled, R.color.icon_secondary, R.color.waiting_status_background)
    syncOutcome.isError() -> Triple(R.drawable.ic_cloud_backup_status_warning, R.color.icon_warning, R.color.warning_block_background)
    else -> Triple(R.drawable.ic_cloud_backup_status_active, R.color.icon_positive, R.color.active_status_background)
}

private fun mapCloudBackupStateTitle(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    syncOutcome: BackupSyncOutcome
) = when {
    syncingInProgress -> resourceManager.getString(R.string.cloud_backup_state_syncing_title)
    !backupEnabled -> resourceManager.getString(R.string.cloud_backup_state_disabled_title)
    syncOutcome.isError() -> resourceManager.getString(R.string.cloud_backup_state_unsynced_title)
    else -> resourceManager.getString(R.string.cloud_backup_state_synced_title)
}

private fun mapCloudBackupStateSubtitle(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    lastSync: Date?
) = when {
    !backupEnabled -> resourceManager.getString(R.string.cloud_backup_settings_disabled_state_subtitle)
    lastSync != null -> resourceManager.getString(
        R.string.cloud_backup_settings_last_sync,
        lastSync.formatDateSinceEpoch(resourceManager),
        resourceManager.formatTime(lastSync)
    )

    else -> null
}

private fun mapCloudBackupClickability(
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    syncOutcome: BackupSyncOutcome
): Boolean {
    if (!backupEnabled) return false
    if (syncingInProgress) return false

    // TODO Antony: check and adapt if needed when implementing each individual state handling
    return when (syncOutcome) {
        BackupSyncOutcome.Ok,
        BackupSyncOutcome.DestructiveDiff,
        BackupSyncOutcome.UnknownPassword,
        BackupSyncOutcome.CorruptedBackup,
        BackupSyncOutcome.UnknownError -> true

        BackupSyncOutcome.OtherStorageIssue,
        BackupSyncOutcome.StorageAuthFailed -> false
    }
}

private fun mapCloudBackupProblemButton(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    syncOutcome: BackupSyncOutcome
): String? {
    if (!backupEnabled) return null
    if (syncingInProgress) return null

    return when (syncOutcome) {
        BackupSyncOutcome.Ok -> null
        BackupSyncOutcome.CorruptedBackup -> resourceManager.getString(R.string.cloud_backup_settings_backup_errors_button)
        BackupSyncOutcome.DestructiveDiff -> resourceManager.getString(R.string.cloud_backup_settings_corrupted_backup_button)
        BackupSyncOutcome.UnknownPassword -> resourceManager.getString(R.string.cloud_backup_settings_deprecated_password_button)
        BackupSyncOutcome.OtherStorageIssue -> resourceManager.getString(R.string.cloud_backup_settings_other_errors_button)
        BackupSyncOutcome.StorageAuthFailed -> resourceManager.getString(R.string.cloud_backup_settings_not_auth_button)
        BackupSyncOutcome.UnknownError -> resourceManager.getString(R.string.cloud_backup_settings_backup_errors_button)
    }
}
