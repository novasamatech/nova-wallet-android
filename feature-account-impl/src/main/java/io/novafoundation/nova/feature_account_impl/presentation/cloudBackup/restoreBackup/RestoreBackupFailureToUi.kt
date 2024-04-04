package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.model.PasswordErrors
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupUnknownError

fun mapRestoreBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {
        is InvalidBackupPasswordError -> {
            TitleAndMessage(
                resourceManager.getString(R.string.cloud_backup_error_invalid_password_title),
                resourceManager.getString(R.string.cloud_backup_error_invalid_password_message),
            )
        }

        is CloudBackupNotFound -> handleCloudBackupNotFound(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}

fun mapDeleteBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}
