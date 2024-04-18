package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved

fun mapCloudBackupSyncFailed(
    resourceManager: ResourceManager,
    state: Throwable,
    onPasswordDeprecated: () -> Unit
): TitleAndMessage? {
    return when (state) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        is InvalidBackupPasswordError, is PasswordNotSaved -> {
            onPasswordDeprecated()
            null
        }

        else -> null
    }
}
