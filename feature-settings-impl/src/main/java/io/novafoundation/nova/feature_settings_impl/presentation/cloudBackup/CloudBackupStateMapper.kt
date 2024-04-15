package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.common.utils.formatting.formatTime
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupWrongPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CorruptedBackupError
import io.novafoundation.nova.feature_settings_impl.R
import java.util.Date

fun mapCloudBackupStateModel(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    errorState: Throwable?,
    lastSync: Date?
): CloudBackupStateModel {
    val (stateImage, stateImageTint, stateBackgroundColor) = mapStateImage(backupEnabled, errorState)

    return CloudBackupStateModel(
        stateImg = stateImage,
        stateImageTint = stateImageTint,
        stateColorBackgroundRes = stateBackgroundColor,
        showProgress = syncingInProgress,
        title = mapCloudBackupStateTitle(resourceManager, backupEnabled, syncingInProgress, errorState),
        subtitle = mapCloudBackupStateSubtitle(resourceManager, backupEnabled, lastSync),
        isClickable = mapCloudBackupClickability(backupEnabled, syncingInProgress, errorState),
        problemButtonText = mapCloudBackupProblemButton(resourceManager, backupEnabled, syncingInProgress, errorState)
    )
}

private fun mapStateImage(backupEnabled: Boolean, errorState: Throwable?) = when {
    !backupEnabled -> Triple(R.drawable.ic_cloud_backup_status_disabled, R.color.icon_secondary, R.color.waiting_status_background)
    errorState != null -> Triple(R.drawable.ic_cloud_backup_status_warning, R.color.icon_warning, R.color.warning_block_background)
    else -> Triple(R.drawable.ic_cloud_backup_status_active, R.color.icon_positive, R.color.active_status_background)
}

private fun mapCloudBackupStateTitle(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    errorState: Throwable?
) = when {
    syncingInProgress -> resourceManager.getString(R.string.cloud_backup_state_syncing_title)
    !backupEnabled -> resourceManager.getString(R.string.cloud_backup_state_disabled_title)
    errorState != null -> resourceManager.getString(R.string.cloud_backup_state_unsynced_title)
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
        lastSync.formatTime(resourceManager)
    )

    else -> null
}

private fun mapCloudBackupClickability(
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    errorState: Throwable?
): Boolean {
    if (!backupEnabled) return false
    if (syncingInProgress) return false
    if (errorState == null) return true

    return when (errorState) {
        is CloudBackupAuthFailed -> false
        is CloudBackupWrongPassword -> false
        is CorruptedBackupError -> false

        else -> true
    }
}

private fun mapCloudBackupProblemButton(
    resourceManager: ResourceManager,
    backupEnabled: Boolean,
    syncingInProgress: Boolean,
    errorState: Throwable?
): String? {
    if (!backupEnabled) return null
    if (syncingInProgress) return null
    if (errorState == null) return null

    return when (errorState) {
        is CloudBackupAuthFailed -> resourceManager.getString(R.string.cloud_backup_settings_not_auth_button)
        is CloudBackupWrongPassword -> resourceManager.getString(R.string.cloud_backup_settings_deprecated_password_button)
        is CannotApplyNonDestructiveDiff -> resourceManager.getString(R.string.cloud_backup_settings_corrupted_backup_button)

        is CloudBackupNotEnoughSpace,
        is CloudBackupUnknownError -> resourceManager.getString(R.string.cloud_backup_settings_other_errors_button)

        else -> resourceManager.getString(R.string.cloud_backup_settings_backup_errors_button)
    }
}
