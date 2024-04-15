package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError

fun mapCloudBackupSyncFailed(
    resourceManager: ResourceManager,
    state: Throwable
): TitleAndMessage? {
    return when (state) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}
