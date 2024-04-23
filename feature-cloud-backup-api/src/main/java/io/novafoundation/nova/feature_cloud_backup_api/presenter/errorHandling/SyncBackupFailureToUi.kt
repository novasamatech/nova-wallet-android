package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.toCustomDialogPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CorruptedBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapCloudBackupSyncFailed(
    resourceManager: ResourceManager,
    state: Throwable,
    onPasswordDeprecated: () -> Unit,
    onCorruptedBackup: () -> Unit,
    initSignIn: () -> Unit,
    onDestructiveBackupFound: (CloudBackupDiff) -> Unit,
): CustomDialogDisplayer.Payload? {
    return when (state) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager, initSignIn)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)
            .toCustomDialogPayload(resourceManager)

        is CannotApplyNonDestructiveDiff -> {
            onDestructiveBackupFound(state.cloudBackupDiff)
            null
        }

        is CorruptedBackupError -> {
            onCorruptedBackup()
            null
        }

        is InvalidBackupPasswordError, is PasswordNotSaved -> {
            onPasswordDeprecated()
            null
        }

        else -> null
    }
}
