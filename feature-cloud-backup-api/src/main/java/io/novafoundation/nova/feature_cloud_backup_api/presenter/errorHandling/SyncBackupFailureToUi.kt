package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupWrongPassword

suspend fun mapCloudBackupSyncFailed(
    resourceManager: ResourceManager,
    state: Throwable,
    onBackupNotFound: (suspend () -> Unit)? = null,
    onBackupPasswordDeprecated: (suspend () -> Unit)? = null
): TitleAndMessage? {
    return when (state) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        is CloudBackupNotFound -> {
            onBackupNotFound?.invoke()
            null
        }

        is CloudBackupWrongPassword -> {
            onBackupPasswordDeprecated?.invoke()
            null
        }

        else -> null
    }
}
